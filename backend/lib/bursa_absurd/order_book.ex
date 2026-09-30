defmodule BursaAbsurd.OrderBook do
  @moduledoc """
  Order book untuk semua komoditas dengan pencocokan prioritas harga-waktu.

    * BUY dicocokkan dengan ASK termurah selama `ask <= harga_beli`
    * SELL dicocokkan dengan BID tertinggi selama `bid >= harga_jual`
    * Harga transaksi = harga pesanan yang sudah mengantre (resting order)
    * Sisa pesanan yang belum cocok masuk antrean (maks. 40 per sisi)

  Setiap transaksi disiarkan ke PubSub, diteruskan ke bot Erlang, dan
  dibukukan oleh COBOL lewat `BursaAbsurd.LedgerWriter`.
  """
  use GenServer

  @pubsub BursaAbsurd.PubSub
  @topic "market:lobby"
  @max_depth 40

  # ------------------------------------------------------------------- API

  def start_link(_opts), do: GenServer.start_link(__MODULE__, :ok, name: __MODULE__)

  @doc """
  Pasang pesanan. Menerima map berkunci atom atau string:
  `trader`, `symbol`, `side` ("BUY"/"SELL"), `qty` (integer), `price` (angka).
  """
  def place_order(payload), do: GenServer.call(__MODULE__, {:place_order, payload})

  def get_book(symbol), do: GenServer.call(__MODULE__, {:get_book, symbol})
  def get_leaderboard, do: GenServer.call(__MODULE__, :leaderboard)
  def recent_trades(limit \\ 20), do: GenServer.call(__MODULE__, {:recent_trades, limit})

  @doc "Kosongkan semua buku & statistik (dipakai tes)."
  def reset, do: GenServer.call(__MODULE__, :reset)

  # ------------------------------------------------------------- callbacks

  @impl true
  def init(:ok), do: {:ok, initial_state()}

  @impl true
  def handle_call({:place_order, payload}, _from, state) do
    case normalize(payload) do
      {:ok, order} ->
        seq = state.seq + 1
        order = Map.merge(order, %{id: order_id(seq), ts: System.system_time(:millisecond)})
        book = Map.fetch!(state.books, order.symbol)

        {rest_order, book, trades, seq} = match(order, book, [], seq)
        book = if rest_order.qty > 0, do: insert_resting(book, rest_order), else: book

        state =
          %{state | books: Map.put(state.books, order.symbol, book), seq: seq}
          |> apply_trades(trades)

        publish(trades, order.symbol, book)

        {:reply, {:ok, %{order: public_order(order, rest_order), trades: trades}}, state}

      {:error, reason} ->
        {:reply, {:error, reason}, state}
    end
  end

  def handle_call({:get_book, symbol}, _from, state) do
    case Map.fetch(state.books, symbol) do
      {:ok, book} -> {:reply, {:ok, snapshot(book)}, state}
      :error -> {:reply, {:error, "Komoditas absurd tidak dikenal: #{symbol}"}, state}
    end
  end

  def handle_call(:leaderboard, _from, state) do
    board =
      state.traders
      |> Enum.map(fn {name, entry} ->
        holdings =
          Enum.reduce(entry.positions, 0.0, fn {symbol, qty}, acc ->
            acc + qty * Map.get(state.last_prices, symbol, 0.0)
          end)

        %{
          trader: name,
          net_worth_delta: Float.round(entry.cash_flow + holdings, 2),
          cash_flow: Float.round(entry.cash_flow, 2),
          trades: entry.trades,
          volume: entry.volume
        }
      end)
      |> Enum.sort_by(& &1.net_worth_delta, :desc)
      |> Enum.with_index(1)
      |> Enum.map(fn {row, rank} -> Map.put(row, :rank, rank) end)

    {:reply, {:ok, board}, state}
  end

  def handle_call({:recent_trades, limit}, _from, state) do
    {:reply, {:ok, Enum.take(state.trades, limit)}, state}
  end

  def handle_call(:reset, _from, _state), do: {:reply, :ok, initial_state()}

  # ------------------------------------------------------------- pencocokan

  defp match(%{qty: 0} = order, book, trades, seq),
    do: {order, book, Enum.reverse(trades), seq}

  defp match(
         %{side: "BUY", price: price} = order,
         %{asks: [%{price: ask_price} = best | rest]} = book,
         trades,
         seq
       )
       when ask_price <= price do
    {order, best, trade, seq} = fill(order, best, seq)
    asks = if best.qty > 0, do: [best | rest], else: rest
    match(order, %{book | asks: asks}, [trade | trades], seq)
  end

  defp match(
         %{side: "SELL", price: price} = order,
         %{bids: [%{price: bid_price} = best | rest]} = book,
         trades,
         seq
       )
       when bid_price >= price do
    {order, best, trade, seq} = fill(order, best, seq)
    bids = if best.qty > 0, do: [best | rest], else: rest
    match(order, %{book | bids: bids}, [trade | trades], seq)
  end

  defp match(order, book, trades, seq), do: {order, book, Enum.reverse(trades), seq}

  defp fill(order, resting, seq) do
    qty = min(order.qty, resting.qty)
    seq = seq + 1

    {buyer, seller} =
      if order.side == "BUY",
        do: {order.trader, resting.trader},
        else: {resting.trader, order.trader}

    trade = %{
      id: trade_id(seq),
      symbol: order.symbol,
      price: resting.price,
      qty: qty,
      buyer: buyer,
      seller: seller,
      taker_side: order.side,
      ts: System.system_time(:millisecond)
    }

    {%{order | qty: order.qty - qty}, %{resting | qty: resting.qty - qty}, trade, seq}
  end

  defp insert_resting(%{bids: bids} = book, %{side: "BUY"} = order) do
    sorted =
      [order | bids]
      |> Enum.sort_by(fn o -> {-o.price, o.ts, o.id} end)
      |> Enum.take(@max_depth)

    %{book | bids: sorted}
  end

  defp insert_resting(%{asks: asks} = book, %{side: "SELL"} = order) do
    sorted =
      [order | asks]
      |> Enum.sort_by(fn o -> {o.price, o.ts, o.id} end)
      |> Enum.take(@max_depth)

    %{book | asks: sorted}
  end

  # ---------------------------------------------------------- akuntansi

  defp apply_trades(state, trades) do
    Enum.reduce(trades, state, fn t, acc ->
      value = t.qty * t.price

      traders =
        acc.traders
        |> update_trader(t.buyer, t.symbol, t.qty, -value)
        |> update_trader(t.seller, t.symbol, -t.qty, value)

      %{
        acc
        | traders: traders,
          last_prices: Map.put(acc.last_prices, t.symbol, t.price),
          trades: Enum.take([t | acc.trades], 100)
      }
    end)
  end

  defp update_trader(traders, name, symbol, qty_delta, cash_delta) do
    base = %{cash_flow: 0.0, trades: 0, volume: 0, positions: %{}}
    entry = Map.get(traders, name, base)

    entry = %{
      entry
      | cash_flow: entry.cash_flow + cash_delta,
        trades: entry.trades + 1,
        volume: entry.volume + abs(qty_delta),
        positions: Map.update(entry.positions, symbol, qty_delta, &(&1 + qty_delta))
    }

    Map.put(traders, name, entry)
  end

  # ----------------------------------------------------------- siaran & util

  defp publish(trades, symbol, book) do
    Enum.each(trades, fn trade ->
      Phoenix.PubSub.broadcast(@pubsub, @topic, {:trade_executed, trade})
      :absurd_bot_manager.notify_trade(trade)
      BursaAbsurd.LedgerWriter.record_trade(trade)
    end)

    Phoenix.PubSub.broadcast(@pubsub, @topic, {:book_updated, symbol, snapshot(book)})
  end

  defp snapshot(%{bids: bids, asks: asks}),
    do: %{bids: Enum.take(bids, 20), asks: Enum.take(asks, 20)}

  defp public_order(original, rest) do
    status =
      cond do
        rest.qty == 0 -> "FILLED"
        rest.qty < original.qty -> "PARTIAL"
        true -> "OPEN"
      end

    %{
      id: original.id,
      trader: original.trader,
      symbol: original.symbol,
      side: original.side,
      qty: original.qty,
      price: original.price,
      remaining: rest.qty,
      status: status
    }
  end

  defp initial_state do
    books = Map.new(BursaAbsurd.commodities(), fn symbol -> {symbol, %{bids: [], asks: []}} end)

    last_prices =
      Map.new(BursaAbsurd.commodity_specs(), fn {symbol, price, _vol} -> {symbol, price} end)

    %{books: books, last_prices: last_prices, traders: %{}, seq: 0, trades: []}
  end

  # "ORD-000000001" dan "TX-000000001" (TX-... pas 12 karakter, sesuai kolom COBOL)
  defp order_id(seq), do: "ORD-" <> String.pad_leading(Integer.to_string(seq), 9, "0")
  defp trade_id(seq), do: "TX-" <> String.pad_leading(Integer.to_string(seq), 9, "0")

  # ------------------------------------------------------------ validasi

  defp normalize(payload) when is_map(payload) do
    trader = fetch_field(payload, :trader)
    symbol = payload |> fetch_field(:symbol) |> upcase()
    side = payload |> fetch_field(:side) |> upcase()
    qty = fetch_field(payload, :qty)
    price = fetch_field(payload, :price)

    cond do
      not is_binary(trader) or trader == "" ->
        {:error, "trader wajib diisi"}

      String.length(trader) > 15 ->
        {:error, "nama trader maksimal 15 karakter"}

      not BursaAbsurd.valid_symbol?(symbol) ->
        {:error, "Komoditas absurd tidak dikenal: #{inspect(symbol)}"}

      side not in ["BUY", "SELL"] ->
        {:error, "side harus BUY atau SELL"}

      not (is_integer(qty) and qty > 0 and qty <= 99_999) ->
        {:error, "qty harus bilangan bulat 1..99999"}

      not (is_number(price) and price > 0 and price < 9_999_999) ->
        {:error, "price harus angka positif"}

      true ->
        {:ok,
         %{trader: trader, symbol: symbol, side: side, qty: qty, price: Float.round(price * 1.0, 2)}}
    end
  end

  defp normalize(_other), do: {:error, "payload harus berupa objek"}

  defp fetch_field(payload, key) do
    case Map.fetch(payload, key) do
      {:ok, value} -> value
      :error -> Map.get(payload, Atom.to_string(key))
    end
  end

  defp upcase(value) when is_binary(value), do: String.upcase(value)
  defp upcase(value), do: value
end

defmodule BursaAbsurdWeb.MarketChannel do
  @moduledoc """
  Channel real-time.

    * `"market:lobby"`             : semua tick harga, transaksi, badai gema, depth
    * `"market:orderbook:SYMBOL"`  : lobby + depth khusus satu komoditas

  Event ke klien : `price_update`, `trade_occurred`, `echo_storm`, `depth_updated`
  Event dari klien: `submit_order` (payload sama seperti POST /api/orders)
  """
  use Phoenix.Channel

  alias BursaAbsurd.{MarketState, OrderBook}

  @impl true
  def join("market:lobby", _payload, socket) do
    {:ok, prices} = MarketState.get_prices()
    {:ok, %{prices: prices}, socket}
  end

  def join("market:orderbook:" <> symbol, _payload, socket) do
    symbol = String.upcase(symbol)

    case OrderBook.get_book(symbol) do
      {:ok, book} ->
        # Ikut mendengarkan siaran umum supaya bisa menerima depth & transaksi.
        Phoenix.PubSub.subscribe(BursaAbsurd.PubSub, "market:lobby")
        {:ok, %{book: book}, assign(socket, :symbol, symbol)}

      {:error, reason} ->
        {:error, %{reason: reason}}
    end
  end

  def join(_topic, _payload, _socket), do: {:error, %{reason: "topik tidak dikenal"}}

  @impl true
  def handle_in("submit_order", payload, socket) do
    case OrderBook.place_order(payload) do
      {:ok, result} -> {:reply, {:ok, result}, socket}
      {:error, reason} -> {:reply, {:error, %{reason: reason}}, socket}
    end
  end

  def handle_in(_event, _payload, socket), do: {:reply, {:error, %{reason: "event tidak dikenal"}}, socket}

  @impl true
  def handle_info({:price_tick, symbol, price, storm?}, socket) do
    push(socket, "price_update", %{symbol: symbol, price: price, storm: storm?})
    {:noreply, socket}
  end

  def handle_info({:echo_storm_warning, symbol, factor}, socket) do
    push(socket, "echo_storm", %{symbol: symbol, multiplier: factor})
    {:noreply, socket}
  end

  def handle_info({:trade_executed, trade}, socket) do
    push(socket, "trade_occurred", trade)
    {:noreply, socket}
  end

  def handle_info({:book_updated, symbol, book}, socket) do
    case socket.assigns[:symbol] do
      nil -> push(socket, "depth_updated", %{symbol: symbol, book: book})
      ^symbol -> push(socket, "depth_updated", %{symbol: symbol, book: book})
      _other -> :ok
    end

    {:noreply, socket}
  end

  def handle_info(_msg, socket), do: {:noreply, socket}
end

defmodule BursaAbsurd.BotBridge do
  @moduledoc """
  Titik masuk untuk bot Erlang (`absurd_bot.erl`) ke sistem Elixir.

  Bot memanggil `submit_bot_order/4`. Harga limit ditentukan di sini dari
  harga spot ditambah selisih kecil (0-2%) ke arah agresif, sehingga pesanan
  BUY dan SELL dari bot yang berbeda cenderung saling cocok.
  """

  alias BursaAbsurd.{MarketState, OrderBook}

  def submit_bot_order(bot_id, symbol, side, qty) do
    price = current_price(symbol)
    edge = :rand.uniform() * 0.02

    limit =
      case side do
        "BUY" -> price * (1.0 + edge)
        _sell -> price * (1.0 - edge)
      end

    OrderBook.place_order(%{
      trader: bot_id,
      symbol: symbol,
      side: side,
      qty: qty,
      price: Float.round(limit, 2)
    })
  end

  defp current_price(symbol) do
    with {:ok, prices} <- MarketState.get_prices(),
         %{price: price} <- Map.get(prices, symbol) do
      price
    else
      _ -> 100.0
    end
  end
end

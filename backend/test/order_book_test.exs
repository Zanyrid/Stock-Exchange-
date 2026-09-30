defmodule BursaAbsurd.OrderBookTest do
  use ExUnit.Case, async: false

  alias BursaAbsurd.OrderBook

  setup do
    :ok = OrderBook.reset()
    :ok
  end

  test "pesanan BUY dicocokkan dengan ASK yang mengantre" do
    {:ok, res1} =
      OrderBook.place_order(%{trader: "SELLER_TEST", symbol: "GMA.GUA", side: "SELL", qty: 10, price: 150.0})

    assert res1.trades == []
    assert res1.order.status == "OPEN"

    {:ok, res2} =
      OrderBook.place_order(%{trader: "BUYER_TEST", symbol: "GMA.GUA", side: "BUY", qty: 6, price: 155.0})

    assert [trade] = res2.trades
    assert trade.qty == 6
    assert trade.price == 150.0
    assert trade.buyer == "BUYER_TEST"
    assert trade.seller == "SELLER_TEST"
    assert res2.order.status == "FILLED"

    {:ok, book} = OrderBook.get_book("GMA.GUA")
    assert [remaining_ask] = book.asks
    assert remaining_ask.qty == 4
  end

  test "pesanan yang tidak cocok mengantre di sisi yang benar" do
    {:ok, _} =
      OrderBook.place_order(%{trader: "A", symbol: "AWN.KUM", side: "BUY", qty: 3, price: 70.0})

    {:ok, res} =
      OrderBook.place_order(%{trader: "B", symbol: "AWN.KUM", side: "SELL", qty: 2, price: 90.0})

    assert res.trades == []

    {:ok, book} = OrderBook.get_book("AWN.KUM")
    assert length(book.bids) == 1
    assert length(book.asks) == 1
  end

  test "menolak pesanan tidak valid" do
    assert {:error, _} =
             OrderBook.place_order(%{trader: "X", symbol: "NGACO", side: "BUY", qty: 1, price: 10.0})

    assert {:error, _} =
             OrderBook.place_order(%{trader: "X", symbol: "GMA.GUA", side: "HOLD", qty: 1, price: 10.0})

    assert {:error, _} =
             OrderBook.place_order(%{trader: "X", symbol: "GMA.GUA", side: "BUY", qty: 0, price: 10.0})
  end

  test "menerima payload berkunci string (seperti dari JSON)" do
    assert {:ok, %{order: order}} =
             OrderBook.place_order(%{
               "trader" => "JSON_TRADER",
               "symbol" => "wkt.tnd",
               "side" => "buy",
               "qty" => 2,
               "price" => 480
             })

    assert order.symbol == "WKT.TND"
    assert order.side == "BUY"
  end

  test "leaderboard mencatat trader yang bertransaksi" do
    {:ok, _} =
      OrderBook.place_order(%{trader: "PENJUAL", symbol: "BSK.PST", side: "SELL", qty: 5, price: 300.0})

    {:ok, _} =
      OrderBook.place_order(%{trader: "PEMBELI", symbol: "BSK.PST", side: "BUY", qty: 5, price: 300.0})

    {:ok, board} = OrderBook.get_leaderboard()
    names = Enum.map(board, & &1.trader)
    assert "PENJUAL" in names
    assert "PEMBELI" in names
    assert Enum.map(board, & &1.rank) == Enum.to_list(1..length(board))
  end
end

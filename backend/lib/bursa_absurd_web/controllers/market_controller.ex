defmodule BursaAbsurdWeb.MarketController do
  use Phoenix.Controller, formats: [:json]

  alias BursaAbsurd.{MarketState, OrderBook}

  def health(conn, _params) do
    json(conn, %{status: "ok", commodities: BursaAbsurd.commodities()})
  end

  def prices(conn, _params) do
    {:ok, prices} = MarketState.get_prices()
    json(conn, %{status: "success", data: prices})
  end

  def orderbook(conn, %{"symbol" => symbol}) do
    symbol = String.upcase(symbol)

    case OrderBook.get_book(symbol) do
      {:ok, book} ->
        json(conn, %{status: "success", symbol: symbol, book: book})

      {:error, reason} ->
        conn |> put_status(:not_found) |> json(%{status: "error", message: reason})
    end
  end

  def trades(conn, _params) do
    {:ok, trades} = OrderBook.recent_trades(30)
    json(conn, %{status: "success", data: trades})
  end

  def simulate_step(conn, %{"symbol" => symbol}) do
    symbol = String.upcase(to_string(symbol))

    case MarketState.trigger_step_simulation(symbol) do
      {:ok, result} ->
        json(conn, %{status: "success", symbol: symbol, simulation: result})

      {:error, :not_found} ->
        conn
        |> put_status(:not_found)
        |> json(%{status: "error", message: "Komoditas tidak dikenal: #{symbol}"})

      {:error, reason} ->
        conn
        |> put_status(:internal_server_error)
        |> json(%{status: "error", message: "Simulasi Fortran gagal", detail: inspect(reason)})
    end
  end

  def simulate_step(conn, _params) do
    conn |> put_status(:bad_request) |> json(%{status: "error", message: "field symbol wajib diisi"})
  end

  def leaderboard(conn, _params) do
    {:ok, board} = OrderBook.get_leaderboard()
    json(conn, %{status: "success", data: board})
  end

  def list_bots(conn, _params) do
    {:ok, bots} = :absurd_bot_manager.get_all_bots()
    json(conn, %{status: "success", data: bots})
  end

  def crash_bot(conn, %{"bot_id" => bot_id}) do
    case :absurd_bot_manager.crash_bot(bot_id) do
      :ok ->
        json(conn, %{status: "success", message: "Bot #{bot_id} dipaksa crash; supervisor akan menyalakannya lagi"})

      {:error, :not_found} ->
        conn |> put_status(:not_found) |> json(%{status: "error", message: "Bot tidak ditemukan: #{bot_id}"})
    end
  end
end

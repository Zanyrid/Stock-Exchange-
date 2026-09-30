defmodule BursaAbsurdWeb.OrderController do
  use Phoenix.Controller, formats: [:json]

  alias BursaAbsurd.OrderBook

  def create(conn, params) do
    case OrderBook.place_order(params) do
      {:ok, %{order: order, trades: trades}} ->
        conn
        |> put_status(:created)
        |> json(%{status: "success", order: order, trades: trades})

      {:error, reason} ->
        conn
        |> put_status(:unprocessable_entity)
        |> json(%{status: "error", message: reason})
    end
  end
end

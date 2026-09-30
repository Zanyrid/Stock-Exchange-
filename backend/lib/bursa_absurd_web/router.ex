defmodule BursaAbsurdWeb.Router do
  use Phoenix.Router, helpers: false
  import Phoenix.Controller

  pipeline :api do
    plug :accepts, ["json"]
  end

  scope "/api", BursaAbsurdWeb do
    pipe_through :api

    get "/health", MarketController, :health
    get "/market/prices", MarketController, :prices
    get "/market/orderbook/:symbol", MarketController, :orderbook
    get "/market/trades", MarketController, :trades
    post "/market/simulate-step", MarketController, :simulate_step
    get "/market/leaderboard", MarketController, :leaderboard

    post "/orders", OrderController, :create

    get "/bots", MarketController, :list_bots
    post "/bots/:bot_id/crash", MarketController, :crash_bot

    get "/ledger/report", LedgerController, :report
    get "/ledger/audit", LedgerController, :audit
  end
end

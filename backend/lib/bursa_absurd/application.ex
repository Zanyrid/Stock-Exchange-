defmodule BursaAbsurd.Application do
  @moduledoc false
  use Application

  @impl true
  def start(_type, _args) do
    children = [
      {Phoenix.PubSub, name: BursaAbsurd.PubSub},
      BursaAbsurd.OrderBook,
      BursaAbsurd.MarketState,
      BursaAbsurd.LedgerWriter,
      # Erlang: manager dulu, baru supervisor bot. Dengan strategi rest_for_one,
      # kalau manager crash maka supervisor bot ikut dibangun ulang.
      %{id: :absurd_bot_manager, start: {:absurd_bot_manager, :start_link, []}, type: :worker},
      %{id: :absurd_bot_sup, start: {:absurd_bot_sup, :start_link, []}, type: :supervisor},
      # Tugas sekali jalan: menyalakan bot bawaan. Ikut dijalankan ulang
      # bila salah satu child sebelumnya di-restart.
      %{
        id: :spawn_default_bots,
        start: {Task, :start_link, [&spawn_default_bots/0]},
        restart: :transient
      },
      BursaAbsurdWeb.Endpoint
    ]

    Supervisor.start_link(children, strategy: :rest_for_one, name: BursaAbsurd.Supervisor)
  end

  @impl true
  def config_change(changed, _new, removed) do
    BursaAbsurdWeb.Endpoint.config_change(changed, removed)
    :ok
  end

  defp spawn_default_bots do
    if Application.get_env(:bursa_absurd, :start_bots, true) do
      :absurd_bot_manager.spawn_default_bots()
    end

    :ok
  end
end

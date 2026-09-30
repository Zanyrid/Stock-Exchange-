defmodule BursaAbsurd.MarketState do
  @moduledoc """
  Menyimpan harga spot komoditas. Secara berkala memanggil simulasi Monte Carlo
  Fortran (lewat Port) untuk menggerakkan harga, lalu menyiarkan perubahannya
  ke PubSub `"market:lobby"`.

  Simulasi dijalankan di luar proses ini (Task / proses pemanggil) supaya
  GenServer tidak macet menunggu program native.
  """
  use GenServer
  require Logger

  alias BursaAbsurd.PortRunner

  @pubsub BursaAbsurd.PubSub
  @topic "market:lobby"

  # ------------------------------------------------------------------- API

  def start_link(_opts), do: GenServer.start_link(__MODULE__, :ok, name: __MODULE__)

  def get_prices, do: GenServer.call(__MODULE__, :get_prices)

  @doc "Jalankan satu simulasi (horizon lebih panjang) untuk satu komoditas."
  def trigger_step_simulation(symbol) do
    with {:ok, prices} <- get_prices(),
         %{} = meta <- Map.get(prices, symbol) || {:error, :not_found},
         {:ok, result} <-
           PortRunner.run_fortran_simulation(symbol, meta.price, meta.vol, meta.drift, 40, 200,
             horizon: 0.05
           ) do
      GenServer.cast(__MODULE__, {:apply_sim, symbol, result})
      {:ok, result}
    end
  end

  # ------------------------------------------------------------- callbacks

  @impl true
  def init(:ok) do
    prices =
      Map.new(BursaAbsurd.commodity_specs(), fn {symbol, price, vol} ->
        {symbol, %{price: price, vol: vol, drift: 0.02, last_storm: false}}
      end)

    tick_ms = Application.get_env(:bursa_absurd, :market_tick_ms, 5000)
    if tick_ms > 0, do: :timer.send_interval(tick_ms, :market_tick)

    {:ok, prices}
  end

  @impl true
  def handle_call(:get_prices, _from, state) do
    {:reply, {:ok, state}, state}
  end

  @impl true
  def handle_cast({:apply_sim, symbol, result}, state) do
    case Map.fetch(state, symbol) do
      {:ok, meta} ->
        new_price = sanitize_price(Map.get(result, "sample_price"), meta.price)
        storm? = Map.get(result, "echo_storm") == true

        Phoenix.PubSub.broadcast(@pubsub, @topic, {:price_tick, symbol, new_price, storm?})

        if storm? do
          factor = Map.get(result, "storm_multiplier", 1.0)
          Phoenix.PubSub.broadcast(@pubsub, @topic, {:echo_storm_warning, symbol, factor})
        end

        {:noreply, Map.put(state, symbol, %{meta | price: new_price, last_storm: storm?})}

      :error ->
        {:noreply, state}
    end
  end

  @impl true
  def handle_info(:market_tick, state) do
    # Pilih satu komoditas acak, simulasikan di Task terpisah.
    {symbol, meta} = Enum.random(state)

    Task.start(fn ->
      case PortRunner.run_fortran_simulation(symbol, meta.price, meta.vol, meta.drift, 20, 50,
             horizon: 0.005
           ) do
        {:ok, result} ->
          GenServer.cast(__MODULE__, {:apply_sim, symbol, result})

        {:error, {:binary_missing, _}} ->
          # Cadangan: biner Fortran belum dikompilasi -> random walk sederhana.
          jitter = 1.0 + (:rand.uniform() - 0.5) * 0.02
          fallback = %{"sample_price" => meta.price * jitter, "echo_storm" => false}
          GenServer.cast(__MODULE__, {:apply_sim, symbol, fallback})

        {:error, reason} ->
          Logger.warning("Simulasi #{symbol} gagal: #{inspect(reason)}")
      end
    end)

    {:noreply, state}
  end

  # --------------------------------------------------------------- internal

  defp sanitize_price(price, _old) when is_number(price) and price > 0 do
    rounded = Float.round(price * 1.0, 2)
    rounded |> max(1.0) |> min(99_999.0)
  end

  defp sanitize_price(_other, old), do: old
end

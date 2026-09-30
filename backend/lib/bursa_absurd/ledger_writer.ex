defmodule BursaAbsurd.LedgerWriter do
  @moduledoc """
  Menulis transaksi ke buku besar COBOL secara berurutan (satu per satu),
  supaya tidak ada dua proses COBOL yang menulis `ledger.dat` bersamaan.
  Satu transaksi menghasilkan dua baris: sisi BUY (pembeli) dan SELL (penjual).
  """
  use GenServer
  require Logger

  alias BursaAbsurd.PortRunner

  def start_link(_opts), do: GenServer.start_link(__MODULE__, :ok, name: __MODULE__)

  def record_trade(trade), do: GenServer.cast(__MODULE__, {:record, trade})

  @impl true
  def init(:ok), do: {:ok, %{}}

  @impl true
  def handle_cast({:record, trade}, state) do
    if Application.get_env(:bursa_absurd, :ledger_enabled, true) do
      write_row(trade, trade.buyer, "BUY")
      write_row(trade, trade.seller, "SELL")
    end

    {:noreply, state}
  end

  defp write_row(trade, trader, side) do
    case PortRunner.record_cobol_ledger(trade.id, trader, trade.symbol, side, trade.qty, trade.price) do
      {:ok, _} ->
        :ok

      {:error, {:binary_missing, _}} ->
        Logger.debug("Biner COBOL belum dikompilasi; transaksi #{trade.id} tidak dibukukan")

      {:error, reason} ->
        Logger.warning("Gagal membukukan #{trade.id}: #{inspect(reason)}")
    end
  end
end

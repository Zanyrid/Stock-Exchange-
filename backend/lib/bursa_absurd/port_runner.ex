defmodule BursaAbsurd.PortRunner do
  @moduledoc """
  Jembatan Elixir -> program native lewat Erlang Port (`Port.open/2`).

    * Fortran (`monte_carlo_sim`) : simulasi Monte Carlo, keluaran JSON
    * COBOL   (`ledger_book`)     : catat transaksi, laporan, audit

  Semua fungsi mengembalikan `{:ok, hasil}` atau `{:error, alasan}`.
  Bila biner belum dikompilasi: `{:error, {:binary_missing, path}}`.
  """

  @default_timeout 15_000

  # ---------------------------------------------------------------- Fortran

  @doc """
  Jalankan simulasi Monte Carlo. Opsi: `:horizon` (tahun, bawaan 0.05),
  `:seed` (bawaan acak).
  """
  def run_fortran_simulation(symbol, price, vol, drift, steps \\ 50, sims \\ 100, opts \\ []) do
    horizon = Keyword.get(opts, :horizon, 0.05)
    seed = Keyword.get(opts, :seed, :rand.uniform(2_000_000_000))

    args = [
      "--symbol", to_string(symbol),
      "--price", fmt(price, 4),
      "--vol", fmt(vol, 4),
      "--drift", fmt(drift, 4),
      "--steps", Integer.to_string(steps),
      "--sims", Integer.to_string(sims),
      "--horizon", fmt(horizon, 4),
      "--seed", Integer.to_string(seed)
    ]

    with {:ok, output} <- run_port(fortran_bin(), args, File.cwd!()) do
      decode_json(output)
    end
  end

  # ------------------------------------------------------------------ COBOL

  @doc "Catat satu baris transaksi ke buku besar COBOL."
  def record_cobol_ledger(tx_id, trader, symbol, side, qty, price, timestamp \\ nil) do
    ts = timestamp || now_string()

    args = [
      "RECORD", tx_id, ts, trader, symbol, side,
      Integer.to_string(qty), fmt(price, 2)
    ]

    run_cobol(args)
  end

  @doc "Laporan buku besar berformat kolom tetap (teks apa adanya dari COBOL)."
  def generate_cobol_report, do: run_cobol(["REPORT"])

  @doc "Audit konsistensi buku besar; hasil berupa map hasil decode JSON."
  def audit_cobol_ledger do
    with {:ok, output} <- run_cobol(["AUDIT"]) do
      decode_json(output)
    end
  end

  # --------------------------------------------------------------- internal

  defp run_cobol(args) do
    dir = ledger_dir()
    File.mkdir_p!(dir)
    run_port(cobol_bin(), args, dir)
  end

  defp run_port(bin, args, cwd) do
    if File.exists?(bin) do
      port =
        Port.open({:spawn_executable, String.to_charlist(bin)}, [
          :binary,
          :exit_status,
          :stderr_to_stdout,
          {:args, args},
          {:cd, String.to_charlist(cwd)}
        ])

      collect(port, "")
    else
      {:error, {:binary_missing, bin}}
    end
  end

  defp collect(port, acc) do
    receive do
      {^port, {:data, data}} ->
        collect(port, acc <> data)

      {^port, {:exit_status, 0}} ->
        {:ok, acc}

      {^port, {:exit_status, code}} ->
        {:error, {:exit_status, code, acc}}
    after
      @default_timeout ->
        Port.close(port)
        {:error, :timeout}
    end
  end

  defp decode_json(output) do
    case Jason.decode(String.trim(output)) do
      {:ok, decoded} when is_map(decoded) -> {:ok, decoded}
      {:ok, _other} -> {:error, {:bad_json, output}}
      {:error, reason} -> {:error, {:bad_json, reason}}
    end
  end

  defp fmt(number, decimals), do: :erlang.float_to_binary(number * 1.0, decimals: decimals)

  defp now_string do
    NaiveDateTime.utc_now() |> NaiveDateTime.truncate(:second) |> NaiveDateTime.to_string()
  end

  defp fortran_bin, do: Application.fetch_env!(:bursa_absurd, :fortran_bin)
  defp cobol_bin, do: Application.fetch_env!(:bursa_absurd, :cobol_bin)
  defp ledger_dir, do: Application.fetch_env!(:bursa_absurd, :ledger_dir)
end

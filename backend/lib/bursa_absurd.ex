defmodule BursaAbsurd do
  @moduledoc """
  Bursa Komoditas Absurd: pasar saham fiktif untuk komoditas tak berwujud.

    * Elixir/Phoenix  - REST API, WebSocket (Channels), order book, orkestrasi
    * Erlang/OTP      - trader bot (`src/absurd_bot*.erl`) dengan self-healing
    * Fortran         - simulasi harga Monte Carlo & "Badai Gema"
    * COBOL           - buku besar transaksi & laporan kolom tetap
  """

  # {simbol, harga awal, volatilitas tahunan}
  @commodities [
    {"GMA.GUA", 150.0, 0.40},
    {"MMP.SNG", 220.0, 0.55},
    {"AWN.KUM", 80.0, 0.30},
    {"BSK.PST", 310.0, 0.25},
    {"ARM.HJN", 195.0, 0.35},
    {"KNG.DJV", 450.0, 0.60},
    {"NST.KST", 120.0, 0.20},
    {"WKT.TND", 500.0, 0.50}
  ]

  @doc "Daftar `{simbol, harga_awal, volatilitas}`."
  def commodity_specs, do: @commodities

  @doc "Daftar simbol komoditas."
  def commodities, do: Enum.map(@commodities, fn {symbol, _price, _vol} -> symbol end)

  @doc "Apakah simbol dikenal?"
  def valid_symbol?(symbol), do: symbol in commodities()
end

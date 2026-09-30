import Config

config :bursa_absurd, BursaAbsurdWeb.Endpoint,
  http: [port: 4002],
  server: false

# Saat tes: bot & tick pasar dimatikan agar hasil tes deterministik.
config :bursa_absurd,
  start_bots: false,
  market_tick_ms: 0,
  ledger_enabled: false,
  ledger_dir: Path.expand("../native/cobol/data_test", __DIR__)

config :logger, level: :warning

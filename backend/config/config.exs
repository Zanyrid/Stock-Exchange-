import Config

config :bursa_absurd, BursaAbsurdWeb.Endpoint,
  url: [host: "localhost"],
  render_errors: [formats: [json: BursaAbsurdWeb.ErrorJSON], layout: false],
  pubsub_server: BursaAbsurd.PubSub,
  secret_key_base: "CfEQ16gL3go5WIuVgV_ji_r2Bw5tw4fWNVtdOSlTtNt88F-G1QRGdt7CKTcyxZz1NdQFk8K2Spew7cibrl3aqw",
  server: true,
  http: [port: 4000]

# Lokasi program native (Fortran & COBOL), relatif terhadap folder config/.
config :bursa_absurd,
  fortran_bin: Path.expand("../native/fortran/bin/monte_carlo_sim", __DIR__),
  cobol_bin: Path.expand("../native/cobol/bin/ledger_book", __DIR__),
  ledger_dir: Path.expand("../native/cobol/data", __DIR__),
  start_bots: true,
  market_tick_ms: 5000,
  ledger_enabled: true

config :logger, :console,
  format: "$time $metadata[$level] $message\n",
  metadata: [:request_id]

config :phoenix, :json_library, Jason

import_config "#{config_env()}.exs"

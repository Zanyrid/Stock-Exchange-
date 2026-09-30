import Config

# 0.0.0.0 supaya bisa diakses dari HP / emulator Android di jaringan yang sama.
config :bursa_absurd, BursaAbsurdWeb.Endpoint,
  http: [ip: {0, 0, 0, 0}, port: 4000],
  debug_errors: true,
  check_origin: false

config :logger, :console, format: "[$level] $message\n"

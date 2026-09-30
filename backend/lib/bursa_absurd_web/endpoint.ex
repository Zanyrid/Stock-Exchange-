defmodule BursaAbsurdWeb.Endpoint do
  use Phoenix.Endpoint, otp_app: :bursa_absurd

  # WebSocket (protokol Phoenix Channels): ws://HOST:4000/socket/websocket
  socket "/socket", BursaAbsurdWeb.UserSocket,
    websocket: true,
    longpoll: false

  plug Plug.RequestId
  plug Plug.Telemetry, event_prefix: [:phoenix, :endpoint]

  plug Plug.Parsers,
    parsers: [:urlencoded, :multipart, :json],
    pass: ["*/*"],
    json_decoder: Phoenix.json_library()

  plug BursaAbsurdWeb.Router
end

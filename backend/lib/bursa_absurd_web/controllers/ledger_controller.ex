defmodule BursaAbsurdWeb.LedgerController do
  use Phoenix.Controller, formats: [:json]

  alias BursaAbsurd.PortRunner

  # Laporan COBOL apa adanya (teks kolom tetap).
  def report(conn, _params) do
    case PortRunner.generate_cobol_report() do
      {:ok, text} ->
        conn |> put_resp_content_type("text/plain") |> send_resp(200, text)

      {:error, reason} ->
        conn
        |> put_status(:internal_server_error)
        |> json(%{status: "error", message: "Laporan COBOL gagal", detail: inspect(reason)})
    end
  end

  def audit(conn, _params) do
    case PortRunner.audit_cobol_ledger() do
      {:ok, result} ->
        json(conn, %{status: "success", audit: result})

      {:error, reason} ->
        conn
        |> put_status(:internal_server_error)
        |> json(%{status: "error", message: "Audit COBOL gagal", detail: inspect(reason)})
    end
  end
end

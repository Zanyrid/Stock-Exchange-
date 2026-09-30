defmodule BursaAbsurd.PortRunnerTest do
  use ExUnit.Case, async: false

  alias BursaAbsurd.PortRunner

  test "melaporkan error yang jelas bila biner Fortran hilang" do
    original = Application.fetch_env!(:bursa_absurd, :fortran_bin)
    Application.put_env(:bursa_absurd, :fortran_bin, "/tidak/ada/monte_carlo_sim")
    on_exit(fn -> Application.put_env(:bursa_absurd, :fortran_bin, original) end)

    assert {:error, {:binary_missing, _}} =
             PortRunner.run_fortran_simulation("GMA.GUA", 100.0, 0.3, 0.05, 10, 10)
  end

  @tag :native
  test "simulasi Fortran mengembalikan JSON yang valid" do
    assert {:ok, json} = PortRunner.run_fortran_simulation("MMP.SNG", 200.0, 0.45, 0.05, 10, 20)

    assert json["symbol"] == "MMP.SNG"
    assert json["initial_price"] == 200.0
    assert is_float(json["simulated_price"])
    assert is_float(json["sample_price"])
    assert json["min_price"] <= json["max_price"]
    assert is_boolean(json["echo_storm"])
    assert length(json["path"]) == 10
  end

  @tag :native
  test "seed yang sama menghasilkan simulasi yang sama" do
    {:ok, a} = PortRunner.run_fortran_simulation("AWN.KUM", 80.0, 0.3, 0.05, 10, 20, seed: 42)
    {:ok, b} = PortRunner.run_fortran_simulation("AWN.KUM", 80.0, 0.3, 0.05, 10, 20, seed: 42)
    assert a == b
  end

  @tag :native
  test "COBOL mencatat transaksi lalu membuat laporan dan audit" do
    File.rm_rf!(Application.fetch_env!(:bursa_absurd, :ledger_dir))

    tx_id = "TX-TEST0001"
    assert {:ok, resp} = PortRunner.record_cobol_ledger(tx_id, "TRADER_COB", "AWN.KUM", "BUY", 5, 80.50)
    assert resp =~ "RECORDED"

    assert {:ok, report} = PortRunner.generate_cobol_report()
    assert report =~ "BURSA KOMODITAS ABSURD - LAPORAN BUKU BESAR"
    assert report =~ "AWN.KUM"
    assert report =~ "TRADER_COB"
    assert report =~ "402.50"

    assert {:ok, audit} = PortRunner.audit_cobol_ledger()
    assert audit["records"] == 1
    assert audit["mismatches"] == 0
  end
end

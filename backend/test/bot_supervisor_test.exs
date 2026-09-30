defmodule BursaAbsurd.BotSupervisorTest do
  use ExUnit.Case, async: false

  setup do
    on_exit(fn ->
      for pid <- :absurd_bot_sup.list_bots() do
        :supervisor.terminate_child(:absurd_bot_sup, pid)
      end
    end)

    :ok
  end

  test "bot yang crash otomatis di-restart oleh supervisor" do
    bot_id = "TEST_CRASH_BOT"
    {:ok, pid1} = :absurd_bot_sup.start_child(bot_id, :zen, 5000.0)
    assert Process.alive?(pid1)

    :absurd_bot.force_panic(pid1)
    :timer.sleep(200)

    refute Process.alive?(pid1)

    pids = :absurd_bot_sup.list_bots()

    statuses =
      for pid <- pids do
        {:ok, status} = :absurd_bot.get_status(pid)
        status
      end

    assert Enum.any?(statuses, fn s -> s.bot_id == bot_id end)
  end

  test "manager mencatat jumlah crash bot" do
    bot_id = "TEST_COUNT_BOT"
    {:ok, pid} = :absurd_bot_sup.start_child(bot_id, :zen, 5000.0)
    :timer.sleep(50)

    assert :ok = :absurd_bot_manager.crash_bot(bot_id)
    :timer.sleep(300)
    refute Process.alive?(pid)

    {:ok, bots} = :absurd_bot_manager.get_all_bots()
    bot = Enum.find(bots, fn s -> s.bot_id == bot_id end)
    assert bot.restarts == 1
  end

  test "crash_bot mengembalikan error untuk bot yang tidak ada" do
    assert {:error, :not_found} = :absurd_bot_manager.crash_bot("BOT_GAIB")
  end
end

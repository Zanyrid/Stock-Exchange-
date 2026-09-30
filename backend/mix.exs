defmodule BursaAbsurd.MixProject do
  use Mix.Project

  def project do
    [
      app: :bursa_absurd,
      version: "0.1.0",
      elixir: "~> 1.14",
      elixirc_paths: ["lib"],
      erlc_paths: ["src"],
      start_permanent: Mix.env() == :prod,
      aliases: aliases(),
      deps: deps()
    ]
  end

  def application do
    [
      extra_applications: [:logger, :runtime_tools],
      mod: {BursaAbsurd.Application, []}
    ]
  end

  defp deps do
    [
      {:phoenix, "~> 1.7.10"},
      {:phoenix_pubsub, "~> 2.1"},
      {:plug_cowboy, "~> 2.6"},
      {:jason, "~> 1.4"},
      {:cowboy, "~> 2.12.0"},
      {:cowlib, "~> 2.13.0", override: true}
    ]
  end

  defp aliases do
    [
      compile_native: ["cmd make -C native/fortran", "cmd make -C native/cobol"],
      setup: ["deps.get", "compile_native", "compile"]
    ]
  end
end

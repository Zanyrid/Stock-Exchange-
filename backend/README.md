# Bursa Komoditas Absurd (backend)

Pasar saham fiktif untuk komoditas tak berwujud (gema gua, mimpi siang, awan
kumulus, bisikan perpustakaan...). Proyek eksperimen empat bahasa:

| Bahasa | Peran | File |
|---|---|---|
| **Elixir** (Phoenix) | REST API, WebSocket, order book, orkestrasi | `lib/`, `config/`, `test/` |
| **Erlang/OTP** | Trader bot + supervisor + manager (self-healing) | `src/*.erl` |
| **Fortran** | Simulasi harga Monte Carlo + "Badai Gema" | `native/fortran/monte_carlo_sim.f90` |
| **COBOL** | Buku besar transaksi, laporan kolom tetap, audit | `native/cobol/ledger_book.cob` |

Elixir memanggil Fortran dan COBOL lewat Erlang Port (`lib/bursa_absurd/port_runner.ex`).

> Kode ini ditulis tanpa sempat dikompilasi (tidak ada Elixir/gfortran/GnuCOBOL di
> lingkungan pembuatnya). Kemungkinan ada galat kecil saat build pertama; salin
> pesan galatnya, dan perbaikannya biasanya satu-dua baris.

## 1. Install

Ubuntu/Debian (juga cocok di GitHub Codespaces):

```bash
sudo apt update
sudo apt install -y erlang elixir gfortran gnucobol build-essential
```

macOS: `brew install elixir gcc gnucobol`

## 2. Build & jalankan

```bash
make setup        # mix deps.get + kompilasi Fortran & COBOL
mix run --no-halt # server di http://localhost:4000
```

Tes:

```bash
make test         # tes yang butuh Fortran/COBOL otomatis dilewati bila biner belum ada
```

Uji program native tanpa Elixir:

```bash
./native/fortran/bin/monte_carlo_sim --symbol GMA.GUA --price 150 --vol 0.4 --steps 10 --sims 50 --seed 7

cd /tmp
/path/ke/native/cobol/bin/ledger_book RECORD TX-000000001 "2026-10-01 10:00:00" BOT_A AWN.KUM BUY 5 80.50
/path/ke/native/cobol/bin/ledger_book REPORT
```

## 3. Endpoint

| Method | Path | Fungsi |
|---|---|---|
| GET | `/api/health` | cek server |
| GET | `/api/market/prices` | harga semua komoditas |
| GET | `/api/market/orderbook/GMA.GUA` | bids & asks |
| GET | `/api/market/trades` | 30 transaksi terakhir |
| POST | `/api/market/simulate-step` | body `{"symbol":"GMA.GUA"}` -> simulasi Fortran |
| GET | `/api/market/leaderboard` | peringkat trader |
| POST | `/api/orders` | body `{"trader":"ME","symbol":"GMA.GUA","side":"BUY","qty":3,"price":155}` |
| GET | `/api/bots` | status bot Erlang (termasuk jumlah restart) |
| POST | `/api/bots/BOT_ZEN_04/crash` | paksa bot crash, lihat supervisor menyalakannya lagi |
| GET | `/api/ledger/report` | laporan COBOL (teks kolom tetap) |
| GET | `/api/ledger/audit` | audit buku besar COBOL |

Contoh:

```bash
curl localhost:4000/api/market/prices
curl -X POST localhost:4000/api/bots/BOT_ZEN_04/crash
curl localhost:4000/api/bots
curl localhost:4000/api/ledger/report
```

WebSocket memakai **protokol Phoenix Channels** (bukan WebSocket mentah):
`ws://HOST:4000/socket/websocket?vsn=2.0.0`, join topik `market:lobby`.
Event: `price_update`, `trade_occurred`, `echo_storm`, `depth_updated`.

## 4. Menghubungkan ke app Android

Di layar Settings app, isi URL REST dengan `http://IP_KOMPUTER:4000/api`
(bukan `localhost`; di emulator Android pakai `http://10.0.2.2:4000/api`).
Bentuk JSON dan protokol WebSocket di sini dirancang sendiri, jadi kode
Kotlin di app mungkin perlu disesuaikan. Cara mudahnya: beri README ini dan
contoh respons endpoint ke AI Studio, lalu minta app diselaraskan.

## 5. Struktur

```
bursa_absurd/
├── mix.exs  Makefile
├── config/            config.exs dev.exs test.exs prod.exs
├── src/               absurd_bot.erl  absurd_bot_sup.erl  absurd_bot_manager.erl
├── native/
│   ├── fortran/       monte_carlo_sim.f90  Makefile
│   └── cobol/         ledger_book.cob      Makefile
├── lib/
│   ├── bursa_absurd.ex
│   ├── bursa_absurd/  application port_runner order_book market_state
│   │                  bot_bridge ledger_writer
│   └── bursa_absurd_web/  endpoint router error_json
│                          channels/ controllers/
└── test/
```

## Catatan desain

- Bot Erlang mendaftar ke `absurd_bot_manager`, yang memantau mereka dan
  meneruskan notifikasi transaksi. Bot yang crash di-restart supervisor
  dengan kas awal (state tidak dipertahankan).
- `absurd_bot_manager:chaos_monkey(10000)` (dari `iex -S mix`) mematikan satu
  bot acak tiap 10 detik; `chaos_monkey(0)` menghentikannya.
- Badai Gema: pengali harga acak (~0.65x sampai ~1.45x) yang dinormalkan agar
  rata-ratanya tepat 1, jadi tidak membuat harga naik/turun terus.
- Buku besar COBOL ada di `native/cobol/data/ledger.dat`.

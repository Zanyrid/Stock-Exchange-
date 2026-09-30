%%%-------------------------------------------------------------------
%%% @doc Trader bot absurd: satu bot = satu proses gen_server.
%%%
%%% Bot berdagang sendiri secara berkala lewat 'Elixir.BursaAbsurd.BotBridge'.
%%% Saldo kas dan portofolio diperbarui saat manager meneruskan notifikasi
%%% transaksi (baik saat bot jadi pembeli maupun penjual).
%%% Bot yang crash akan di-restart oleh absurd_bot_sup.
%%% @end
%%%-------------------------------------------------------------------
-module(absurd_bot).
-behaviour(gen_server).

%% API
-export([start_link/3, get_status/1, force_panic/1, update_cash/2, apply_trade/2]).

%% gen_server callbacks
-export([init/1, handle_call/3, handle_cast/2, handle_info/2, terminate/2, code_change/3]).

-record(state, {
    bot_id      :: binary(),
    strategy    :: fomo | contrarian | echo_chaser | zen,
    cash        :: float(),
    portfolio   :: #{binary() => integer()},
    timer_ref   :: reference() | undefined,
    trade_count :: non_neg_integer()
}).

-define(COMMODITIES, [
    <<"GMA.GUA">>, <<"MMP.SNG">>, <<"AWN.KUM">>, <<"BSK.PST">>,
    <<"ARM.HJN">>, <<"KNG.DJV">>, <<"NST.KST">>, <<"WKT.TND">>
]).

%%%===================================================================
%%% API
%%%===================================================================

-spec start_link(binary(), atom(), number()) -> {ok, pid()} | {error, term()}.
start_link(BotId, Strategy, InitialCash) ->
    gen_server:start_link(?MODULE, [BotId, Strategy, InitialCash], []).

get_status(Pid) ->
    gen_server:call(Pid, get_status).

%% Memaksa bot crash (demo self-healing OTP).
force_panic(Pid) ->
    gen_server:cast(Pid, force_panic).

update_cash(Pid, Amount) ->
    gen_server:cast(Pid, {update_cash, Amount}).

%% Dipanggil manager saat ada transaksi yang melibatkan bot ini.
apply_trade(Pid, Trade) ->
    gen_server:cast(Pid, {trade, Trade}).

%%%===================================================================
%%% gen_server callbacks
%%%===================================================================

init([BotId, Strategy, InitialCash]) ->
    process_flag(trap_exit, true),
    Portfolio = maps:from_list([{Sym, 20} || Sym <- ?COMMODITIES]),
    Interval = 1000 + rand:uniform(2000),
    TimerRef = erlang:send_after(Interval, self(), trade_tick),
    absurd_bot_manager:register_bot(BotId, self()),
    logger:info("Trader bot aktif: ~p [~p], kas awal ~p", [BotId, Strategy, InitialCash]),
    {ok, #state{
        bot_id      = BotId,
        strategy    = Strategy,
        cash        = float(InitialCash),
        portfolio   = Portfolio,
        timer_ref   = TimerRef,
        trade_count = 0
    }}.

handle_call(get_status, _From, State) ->
    Payload = #{
        bot_id      => State#state.bot_id,
        strategy    => State#state.strategy,
        cash        => State#state.cash,
        portfolio   => State#state.portfolio,
        trade_count => State#state.trade_count
    },
    {reply, {ok, Payload}, State};
handle_call(_Request, _From, State) ->
    {reply, {error, unknown_call}, State}.

handle_cast(force_panic, State) ->
    logger:error("SIMULASI KERUSAKAN: bot ~p terjebak Paradoks Absurd!", [State#state.bot_id]),
    exit(absurd_paradox_overflow);
handle_cast({update_cash, Amount}, State) ->
    {noreply, State#state{cash = State#state.cash + Amount}};
handle_cast({trade, #{buyer := Buyer, seller := Seller, symbol := Sym,
                      qty := Qty, price := Price}}, State) ->
    {noreply, settle(Buyer, Seller, Sym, Qty, Price, State)};
handle_cast(_Msg, State) ->
    {noreply, State}.

handle_info(trade_tick, State) ->
    NewState = execute_trade_strategy(State),
    NextInterval = 1200 + rand:uniform(2500),
    NewTimer = erlang:send_after(NextInterval, self(), trade_tick),
    {noreply, NewState#state{timer_ref = NewTimer}};
handle_info({'EXIT', _Pid, normal}, State) ->
    {noreply, State};
handle_info({'EXIT', _Pid, Reason}, State) ->
    logger:warning("Bot menerima sinyal exit: ~p", [Reason]),
    {stop, Reason, State};
handle_info(_Info, State) ->
    {noreply, State}.

terminate(Reason, #state{bot_id = BotId, timer_ref = TimerRef}) ->
    _ = case TimerRef of
        undefined -> ok;
        _ -> erlang:cancel_timer(TimerRef)
    end,
    logger:info("Bot ~p berhenti. Alasan: ~p", [BotId, Reason]),
    ok.

code_change(_OldVsn, State, _Extra) ->
    {ok, State}.

%%%===================================================================
%%% Logika internal
%%%===================================================================

execute_trade_strategy(#state{bot_id = BotId, strategy = Strategy} = State) ->
    Symbol = lists:nth(rand:uniform(length(?COMMODITIES)), ?COMMODITIES),
    Qty = rand:uniform(5),
    case pick_side(Strategy) of
        hold ->
            State;
        Side ->
            submit(BotId, Symbol, side_bin(Side), Qty),
            State#state{trade_count = State#state.trade_count + 1}
    end.

pick_side(fomo)        -> buy;
pick_side(contrarian)  -> sell;
pick_side(echo_chaser) ->
    case rand:uniform(2) of
        1 -> buy;
        2 -> sell
    end;
pick_side(zen) ->
    case rand:uniform(10) of
        1 -> buy;
        2 -> sell;
        _ -> hold
    end.

side_bin(buy)  -> <<"BUY">>;
side_bin(sell) -> <<"SELL">>.

%% Kirim pesanan ke order book Elixir. Kegagalan di sisi Elixir tidak boleh
%% menjatuhkan bot (kecuali lewat force_panic), jadi semua dibungkus try.
submit(BotId, Symbol, Side, Qty) ->
    try 'Elixir.BursaAbsurd.BotBridge':submit_bot_order(BotId, Symbol, Side, Qty) of
        {ok, _Result} ->
            ok;
        {error, Reason} ->
            logger:debug("Pesanan bot ~p ditolak: ~p", [BotId, Reason]),
            ok;
        _Other ->
            ok
    catch
        Class:Error ->
            logger:warning("Bot ~p gagal mengirim pesanan: ~p:~p", [BotId, Class, Error]),
            ok
    end.

%% Perbarui kas & portofolio sesuai peran bot dalam transaksi.
settle(Buyer, Seller, Sym, Qty, Price,
       #state{bot_id = Id, cash = Cash, portfolio = Portfolio} = State) ->
    Value = Qty * Price,
    case {Id =:= Buyer, Id =:= Seller} of
        {true, false} ->
            State#state{
                cash = Cash - Value,
                portfolio = maps:update_with(Sym, fun(Q) -> Q + Qty end, Qty, Portfolio)
            };
        {false, true} ->
            State#state{
                cash = Cash + Value,
                portfolio = maps:update_with(Sym, fun(Q) -> Q - Qty end, -Qty, Portfolio)
            };
        _ ->
            State
    end.

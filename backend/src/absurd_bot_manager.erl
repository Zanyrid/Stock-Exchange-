%%%-------------------------------------------------------------------
%%% @doc Manager trader bot: penghubung antara dunia Erlang (bot) dan
%%% API Elixir (Phoenix).
%%%
%%%  * mendaftarkan bot (id -> pid) dan memantaunya dengan monitor
%%%  * menghitung berapa kali tiap bot crash & di-restart
%%%  * meneruskan notifikasi transaksi ke bot yang terlibat
%%%  * menyediakan "chaos monkey" opsional yang mematikan bot secara acak
%%% @end
%%%-------------------------------------------------------------------
-module(absurd_bot_manager).
-behaviour(gen_server).

-export([start_link/0, spawn_default_bots/0, register_bot/2, notify_trade/1,
         get_all_bots/0, crash_bot/1, chaos_monkey/1]).
-export([init/1, handle_call/3, handle_cast/2, handle_info/2, terminate/2, code_change/3]).

-define(SERVER, ?MODULE).

-define(DEFAULT_BOTS, [
    {<<"BOT_FOMO_01">>,   fomo,        10000.0},
    {<<"BOT_CONTRA_02">>, contrarian,  10000.0},
    {<<"BOT_ECHO_03">>,   echo_chaser, 12000.0},
    {<<"BOT_ZEN_04">>,    zen,          8000.0},
    {<<"BOT_FOMO_05">>,   fomo,         9000.0},
    {<<"BOT_CONTRA_06">>, contrarian,   9000.0}
]).

-record(mgr, {
    bots      = #{} :: #{binary() => {pid(), reference()}},
    crashes   = #{} :: #{binary() => non_neg_integer()},
    chaos_ms  = 0   :: non_neg_integer(),
    chaos_ref :: reference() | undefined
}).

%%%===================================================================
%%% API
%%%===================================================================

start_link() ->
    gen_server:start_link({local, ?SERVER}, ?MODULE, [], []).

%% Jalankan bot bawaan yang belum terdaftar. Dipanggil dari proses lain
%% (bukan dari dalam manager) supaya tidak deadlock.
spawn_default_bots() ->
    lists:foreach(
        fun({BotId, Strategy, Cash}) ->
            case gen_server:call(?SERVER, {lookup, BotId}) of
                {ok, _Pid} -> ok;
                error      -> absurd_bot_sup:start_child(BotId, Strategy, Cash)
            end
        end,
        ?DEFAULT_BOTS),
    ok.

register_bot(BotId, Pid) ->
    gen_server:cast(?SERVER, {register, BotId, Pid}).

%% Dipanggil order book Elixir setiap ada transaksi.
notify_trade(Trade) ->
    gen_server:cast(?SERVER, {trade, Trade}).

%% Status semua bot. Status diambil di proses pemanggil supaya manager tidak
%% ikut macet kalau ada bot yang sedang sibuk.
get_all_bots() ->
    Entries = gen_server:call(?SERVER, list_bots),
    Statuses = lists:filtermap(
        fun({_BotId, Pid, Crashes}) ->
            try absurd_bot:get_status(Pid) of
                {ok, Status} -> {true, Status#{restarts => Crashes}};
                _            -> false
            catch
                _:_ -> false
            end
        end,
        Entries),
    {ok, Statuses}.

%% Paksa satu bot crash. Supervisor akan menyalakannya lagi.
crash_bot(BotId) ->
    gen_server:call(?SERVER, {crash, BotId}).

%% Nyalakan chaos monkey: tiap IntervalMs satu bot acak dimatikan.
%% IntervalMs = 0 untuk mematikannya.
chaos_monkey(IntervalMs) when is_integer(IntervalMs), IntervalMs >= 0 ->
    gen_server:call(?SERVER, {chaos, IntervalMs}).

%%%===================================================================
%%% gen_server callbacks
%%%===================================================================

init([]) ->
    {ok, #mgr{}}.

handle_call({lookup, BotId}, _From, #mgr{bots = Bots} = State) ->
    case maps:find(BotId, Bots) of
        {ok, {Pid, _Ref}} -> {reply, {ok, Pid}, State};
        error             -> {reply, error, State}
    end;
handle_call(list_bots, _From, #mgr{bots = Bots, crashes = Crashes} = State) ->
    Entries = [{Id, Pid, maps:get(Id, Crashes, 0)} || {Id, {Pid, _Ref}} <- maps:to_list(Bots)],
    {reply, lists:sort(Entries), State};
handle_call({crash, BotId}, _From, #mgr{bots = Bots} = State) ->
    case maps:find(BotId, Bots) of
        {ok, {Pid, _Ref}} ->
            absurd_bot:force_panic(Pid),
            {reply, ok, State};
        error ->
            {reply, {error, not_found}, State}
    end;
handle_call({chaos, IntervalMs}, _From, #mgr{chaos_ref = OldRef} = State) ->
    cancel_timer(OldRef),
    case IntervalMs of
        0 ->
            {reply, ok, State#mgr{chaos_ms = 0, chaos_ref = undefined}};
        _ ->
            Ref = erlang:send_after(IntervalMs, self(), chaos_tick),
            {reply, ok, State#mgr{chaos_ms = IntervalMs, chaos_ref = Ref}}
    end;
handle_call(_Request, _From, State) ->
    {reply, {error, unknown_call}, State}.

handle_cast({register, BotId, Pid}, #mgr{bots = Bots, crashes = Crashes} = State) ->
    Ref = erlang:monitor(process, Pid),
    case maps:find(BotId, Bots) of
        {ok, {Pid, OldRef}} ->
            erlang:demonitor(OldRef, [flush]),
            {noreply, State#mgr{bots = Bots#{BotId => {Pid, Ref}}}};
        {ok, {_OldPid, OldRef}} ->
            %% Pid baru mendaftar sebelum 'DOWN' pid lama diproses:
            %% hitung sebagai satu crash, lalu buang monitor lama.
            erlang:demonitor(OldRef, [flush]),
            {noreply, State#mgr{bots = Bots#{BotId => {Pid, Ref}},
                                crashes = bump(BotId, Crashes)}};
        error ->
            {noreply, State#mgr{bots = Bots#{BotId => {Pid, Ref}}}}
    end;
handle_cast({trade, #{buyer := Buyer, seller := Seller} = Trade}, #mgr{bots = Bots} = State) ->
    lists:foreach(
        fun(Id) ->
            case maps:find(Id, Bots) of
                {ok, {Pid, _Ref}} -> absurd_bot:apply_trade(Pid, Trade);
                error             -> ok
            end
        end,
        lists:usort([Buyer, Seller])),
    {noreply, State};
handle_cast(_Msg, State) ->
    {noreply, State}.

handle_info({'DOWN', _Ref, process, Pid, Reason},
            #mgr{bots = Bots, crashes = Crashes} = State) ->
    case [Id || {Id, {P, _}} <- maps:to_list(Bots), P =:= Pid] of
        [BotId] ->
            NewCrashes = case Reason of
                shutdown -> Crashes;
                normal   -> Crashes;
                _        -> bump(BotId, Crashes)
            end,
            {noreply, State#mgr{bots = maps:remove(BotId, Bots), crashes = NewCrashes}};
        _ ->
            {noreply, State}
    end;
handle_info(chaos_tick, #mgr{chaos_ms = Ms, bots = Bots} = State) when Ms > 0 ->
    case maps:keys(Bots) of
        [] ->
            ok;
        Ids ->
            Victim = lists:nth(rand:uniform(length(Ids)), Ids),
            {Pid, _Ref} = maps:get(Victim, Bots),
            logger:warning("CHAOS MONKEY menyerang ~p", [Victim]),
            absurd_bot:force_panic(Pid)
    end,
    Ref = erlang:send_after(Ms, self(), chaos_tick),
    {noreply, State#mgr{chaos_ref = Ref}};
handle_info(_Info, State) ->
    {noreply, State}.

terminate(_Reason, _State) ->
    ok.

code_change(_OldVsn, State, _Extra) ->
    {ok, State}.

%%%===================================================================
%%% Internal
%%%===================================================================

bump(BotId, Crashes) ->
    maps:update_with(BotId, fun(N) -> N + 1 end, 1, Crashes).

cancel_timer(undefined) -> ok;
cancel_timer(Ref)       -> _ = erlang:cancel_timer(Ref), ok.

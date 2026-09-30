%%%-------------------------------------------------------------------
%%% @doc Supervisor trader bot. Bot yang crash otomatis di-restart.
%%% Strategi simple_one_for_one: semua anak memakai spesifikasi yang sama,
%%% argumen (BotId, Strategy, Cash) ditambahkan saat start_child/3.
%%% @end
%%%-------------------------------------------------------------------
-module(absurd_bot_sup).
-behaviour(supervisor).

-export([start_link/0, start_child/3, stop_bot/1, list_bots/0]).
-export([init/1]).

-define(SERVER, ?MODULE).

start_link() ->
    supervisor:start_link({local, ?SERVER}, ?MODULE, []).

start_child(BotId, Strategy, Cash) ->
    supervisor:start_child(?SERVER, [BotId, Strategy, Cash]).

%% Hentikan bot secara permanen (tidak di-restart).
stop_bot(Pid) when is_pid(Pid) ->
    supervisor:terminate_child(?SERVER, Pid).

list_bots() ->
    Children = supervisor:which_children(?SERVER),
    [Pid || {_, Pid, worker, [absurd_bot]} <- Children, is_pid(Pid)].

init([]) ->
    SupFlags = #{
        strategy  => simple_one_for_one,
        intensity => 50,   %% toleransi maksimal 50 crash ...
        period    => 5     %% ... dalam 5 detik sebelum supervisor menyerah
    },
    ChildSpec = #{
        id       => absurd_bot,
        start    => {absurd_bot, start_link, []},
        restart  => permanent,   %% selalu di-restart, apa pun alasannya
        shutdown => 2000,
        type     => worker,
        modules  => [absurd_bot]
    },
    {ok, {SupFlags, [ChildSpec]}}.

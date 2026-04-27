package org.example.space_invaders_online.game.server;

public enum ServerAnswerType {
    INIT,
    UPDATE,
    NAME_ACCEPTED,
    NAME_REJECTED,
    WIN,
    PLAYER_LIST_UPDATE,
    GAME_START,
    GAME_PAUSED,
    GAME_RESUMED,
    LEADERBOARD
}

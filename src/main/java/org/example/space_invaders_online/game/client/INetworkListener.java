package org.example.space_invaders_online.game.client;

import org.example.space_invaders_online.game.database.PlayerStats;
import org.example.space_invaders_online.game.server.DTOGameState;
import org.example.space_invaders_online.game.server.DTOPlayer;

import java.util.List;

public interface INetworkListener {
    void onInit(int playerId);

    void onGameState(DTOGameState state);

    void onNameAccepted();

    void onNameRejected(String reason);

    void onPlayerListUpdate(List<DTOPlayer> players);

    void onGameStart();

    void onGamePaused();

    void onGameResumed();

    void onWin(String winnerName);

    void onLeaderBoard(List<PlayerStats> board);

    void onDisconnected();
}

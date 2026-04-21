package org.example.space_invaders_online.game.client;

import org.example.space_invaders_online.game.database.PlayerStats;
import org.example.space_invaders_online.game.server.DTOGameState;

import java.util.List;

public interface INetworkListener {
    void onGameState(DTOGameState sate);
    void onNameAccepted();
    void onNameRejected(String reason);
    void onGameStart();
    void onGamePaused();
    void onWin(String winnerName);
    void onLeaderBoard(List<PlayerStats> board);
    void onDisconnected();
}

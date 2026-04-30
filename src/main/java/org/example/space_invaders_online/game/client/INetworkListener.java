package org.example.space_invaders_online.game.client;
import org.example.space_invaders_online.game.server.object.ServerMessage;

public interface INetworkListener {
    void onInit(ServerMessage m);
    void onGameState(ServerMessage m);
    void onNameAccepted();
    void onNameRejected();
    void onPlayerListUpdate(ServerMessage m);
    void onGameStart();
    void onGamePaused();
    void onGameResumed();
    void onWin(ServerMessage m);
    void onLeaderBoard(ServerMessage m);
    void onDisconnected();
}

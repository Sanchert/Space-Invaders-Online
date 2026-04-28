package org.example.space_invaders_online.game.sceneController;

import org.example.space_invaders_online.game.client.NetworkClient;

public class GameContext {
    private String playerName;
    private NetworkClient networkClient;

    // getters / setters
    public String getPlayerName() { return playerName; }
    public void setPlayerName(String playerName) { this.playerName = playerName; }

    public NetworkClient getNetworkClient() {
        return networkClient;
    }
    public void setNetworkClient(NetworkClient networkClient) {
        this.networkClient = networkClient;
    }
}

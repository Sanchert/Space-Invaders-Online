package org.example.space_invaders_online.game.sceneController;

import org.example.space_invaders_online.game.client.OnlineMatchClient;

public class GameContext {
    private String playerName;
    private GameMode gameMode;
    private String winner;
    private int score;
    private OnlineMatchClient onlineMatchClient;

    public enum GameMode {
        SINGLE, ONLINE
    }

    // Геттеры и сеттеры
    public String getPlayerName() { return playerName; }
    public void setPlayerName(String playerName) { this.playerName = playerName; }

    public GameMode getGameMode() { return gameMode; }
    public void setGameMode(GameMode gameMode) { this.gameMode = gameMode; }

    public String getWinner() { return winner; }
    public void setWinner(String winner) { this.winner = winner; }

    public int getScore() { return score; }
    public void setScore(int score) { this.score = score; }

    public OnlineMatchClient getOnlineMatchClient() { return onlineMatchClient; }
    public void setOnlineMatchClient(OnlineMatchClient onlineMatchClient) { this.onlineMatchClient = onlineMatchClient; }
}

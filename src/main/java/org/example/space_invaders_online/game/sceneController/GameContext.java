package org.example.space_invaders_online.game.sceneController;

public class GameContext {
    private String playerName;
    private GameMode gameMode;  // SINGLE, ONLINE
    private String winner;
    private int score;
    // другие данные

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
}

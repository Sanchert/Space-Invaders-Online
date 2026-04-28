package org.example.space_invaders_online.game.database;

import java.util.List;

public interface PlayerStatsDAO {
    PlayerStats getOrCreate(String playerName);
    void update(PlayerStats stats);
    List<PlayerStats> getLeaderboard();
}

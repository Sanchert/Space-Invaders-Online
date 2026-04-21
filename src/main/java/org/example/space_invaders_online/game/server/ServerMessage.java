package org.example.space_invaders_online.game.server;

import org.example.space_invaders_online.game.database.PlayerStats;
import java.util.List;

public class ServerMessage {
    public ServerAnswerType type;
    public DTOGameState currentGameState;
    public String args;
    public List<PlayerStats> leaderboard; // статистика
}


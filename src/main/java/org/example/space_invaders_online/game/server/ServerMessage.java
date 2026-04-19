package org.example.space_invaders_online.game.server;

import org.example.space_invaders_online.game.database.PlayerStats;
import java.util.List;

public class ServerMessage {
    private static final long serialVersionUID = 1L;

    public ServerAnswerType type;
    public int playerId;
    public SerializableGameState currentGameState;
    public String args;
    public List<SerializablePlayer> players;
    public List<PlayerStats> leaderboard;
}


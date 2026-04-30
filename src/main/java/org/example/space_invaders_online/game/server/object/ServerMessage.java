package org.example.space_invaders_online.game.server.object;

import org.example.space_invaders_online.game.database.PlayerStats;
import org.example.space_invaders_online.game.server.ServerAnswerType;
import org.example.space_invaders_online.game.server.dto.DTOGameState;
import org.example.space_invaders_online.game.server.dto.DTOPlayer;

import java.util.List;

public class ServerMessage {
    public ServerAnswerType type;
    public int playerId;
    public DTOGameState currentGameState;
    public String args;
    public List<PlayerStats> leaderboard;
    public List<DTOPlayer> players;
}

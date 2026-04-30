package org.example.space_invaders_online.game.server.dto;

import java.util.ArrayList;
import java.util.List;

public class DTOGameState {
    public List<DTOPlayer> players;
    public List<DTOBullet> bullets;
    public List<DTOTarget> targets;

    public DTOGameState() {
        players = new ArrayList<>();
        bullets = new ArrayList<>();
        targets = new ArrayList<>();
    }

    public DTOGameState(List<DTOPlayer> players,
                        List<DTOBullet> bullets,
                        List<DTOTarget> targets) {
        this.players = players;
        this.bullets = bullets;
        this.targets = targets;
    }
}

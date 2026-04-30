// src/main/java/org/example/space_invaders_online/game/server/SerializablePlayer.java
package org.example.space_invaders_online.game.server.dto;

public class DTOPlayer {

    public int objectID;
    public double pos_x;
    public double pos_y;
    public String name;
    public int shoots;
    public int score;
    public int colorID;
    public boolean isDestroyed;
    public int hits;

    public DTOPlayer() {}

    public DTOPlayer(int id, double pos_x, double pos_y, int shoots, int score, String name, int colorID, boolean isDestroyed, int hits) {
        this.objectID = id;
        this.pos_x = pos_x;
        this.pos_y = pos_y;
        this.shoots = shoots;
        this.score = score;
        this.name = name;
        this.colorID = colorID;
        this.isDestroyed = isDestroyed;
        this.hits = hits;
    }
}

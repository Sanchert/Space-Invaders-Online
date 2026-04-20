package org.example.space_invaders_online.game.server;

public class DTOTarget {
    public int objectID;
    public double pos_x;
    public double pos_y;
    public int cost;
    public boolean isDestroyed;
    public DTOTarget() {}

    public DTOTarget(int id, double pos_x, double pos_y, int cost, boolean isDestroyed) {
        this.objectID = id;
        this.pos_x = pos_x;
        this.pos_y = pos_y;
        this.cost = cost;
        this.isDestroyed = isDestroyed;
    }
}

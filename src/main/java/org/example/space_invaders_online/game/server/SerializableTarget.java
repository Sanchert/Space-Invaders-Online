package org.example.space_invaders_online.game.server;

import java.io.Serializable;

public class SerializableTarget implements Serializable {
    public int objectID;
    public double pos_x;
    public double pos_y;
    public int cost;

    public SerializableTarget() {}

    public SerializableTarget(int id, double x, double y, int cost) {
        this.objectID = id;
        this.pos_x = x;
        this.pos_y = y;
        this.cost = cost;
    }
}

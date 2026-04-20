package org.example.space_invaders_online.game.server;

public class DTOBullet {
    public int objectID;
    public int ownerId;
    public double pos_x;
    public double pos_y;

    public DTOBullet() {}

    public DTOBullet(int id, int ownerId, double pos_x, double pos_y) {
        this.objectID = id;
        this.ownerId = ownerId;
        this.pos_x = pos_x;
        this.pos_y = pos_y;
    }
}

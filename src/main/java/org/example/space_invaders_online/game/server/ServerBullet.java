package org.example.space_invaders_online.game.server;

import org.example.space_invaders_online.game.gameWorld.ServerGameObject;

public class ServerBullet extends ServerGameObject {
    private final int ownerId;
    private double speed = 6.5;

    public ServerBullet(int id, int ownerId, double pos_x, double pos_y) {
        super(id, pos_x, pos_y);
        this.ownerId = ownerId;
    }

    public void setSpeed(double speed) {
        this.speed = speed;
    }

    @Override
    public void update() {
        this.pos_x += this.speed;
        if (this.pos_x > 920) {
            destroyed = true;
        }
    }

    @Override
    public boolean collidesWith(ServerGameObject other) {
        return false;
    }

    public int getOwnerId() {
        return this.ownerId;
    }

    public DTOBullet toSerializable() {
        return new DTOBullet(objectId, ownerId, pos_x, pos_y, isDestroyed());
    }
}


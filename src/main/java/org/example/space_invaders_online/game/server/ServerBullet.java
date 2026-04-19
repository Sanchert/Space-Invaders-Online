package org.example.space_invaders_online.game.server;

import javafx.scene.shape.Shape;
import org.example.space_invaders_online.game.gameWorld.ServerGameObject;

public class ServerBullet extends ServerGameObject {
    private final int ownerId;
    private double speed = 3.5;
    // NOTE: if you need to turn the player
    // moveDirection

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
        if (this.pos_x > 800) {
            destroyed = true;
        }
    }

    public SerializableBullet serialize() {
        return new SerializableBullet(objectId, ownerId, (float)pos_x, (float)pos_y);
    }

    @Override
    public boolean collidesWith(Shape other) {
        return false;
    }

    public int getOwnerId() {
        return this.ownerId;
    }
}

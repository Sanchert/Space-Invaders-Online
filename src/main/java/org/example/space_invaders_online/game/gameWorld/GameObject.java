package org.example.space_invaders_online.game.gameWorld;

import javafx.scene.shape.Shape;

public abstract class GameObject {
    protected final int objectId;
    protected double pos_x;
    protected double pos_y;
    protected boolean destroyed = false;

    /*NOTE: can be Interface (IMonoBehaviour :D )*/
    public abstract void update();
    public abstract boolean collidesWith(Shape other);

    public GameObject(int id, double pos_x, double pos_y) {
        this.objectId = id;
        this.pos_x = pos_x;
        this.pos_y = pos_y;
    }
    public void setPosition(double pos_x, double pos_y) {
        this.pos_x = pos_x;
        this.pos_y = pos_y;
    }
    public void destroy() {
        this.destroyed = true;
    }
    public boolean isDestroyed() {
        return destroyed;
    }
}

package org.example.space_invaders_online.game.gameWorld;

public abstract class GameObject {
    protected final int objectId;
    protected double pos_x;
    protected double pos_y;
    protected boolean destroyed = false;

    public GameObject(int id) {
        this.objectId = id;
    }

    public void destroy() {
        this.destroyed = true;
    }

    public boolean isDestroyed() {
        return destroyed;
    }
}

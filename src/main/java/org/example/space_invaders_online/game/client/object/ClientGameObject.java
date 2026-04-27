package org.example.space_invaders_online.game.client.object;

import org.example.space_invaders_online.game.gameWorld.GameObject;

public abstract class ClientGameObject extends GameObject {

    public ClientGameObject(int id, double pos_x, double pos_y) {
        super(id);
        this.pos_x = pos_x;
        this.pos_y = pos_y;
    }

    public void setPosition(double pos_x, double pos_y) {
        this.pos_x = pos_x;
        this.pos_y = pos_y;
    }

    public double getPosX() {
        return pos_x;
    }

    public double getPosY() {
        return pos_y;
    }
}

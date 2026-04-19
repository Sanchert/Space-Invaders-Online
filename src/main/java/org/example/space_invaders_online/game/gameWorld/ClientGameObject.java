package org.example.space_invaders_online.game.gameWorld;

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
}

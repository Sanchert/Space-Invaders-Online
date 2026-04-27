package org.example.space_invaders_online.game.client.object;

public class ClientBullet extends ClientGameObject {

    public ClientBullet(int id, double pos_x, double pos_y) {
        super(id, pos_x, pos_y);
    }

    public void updateFromServer(double x, double y) {
        setPosition(x, y);
    }
}

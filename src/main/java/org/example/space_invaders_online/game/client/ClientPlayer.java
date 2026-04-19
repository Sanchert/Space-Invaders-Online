package org.example.space_invaders_online.game.client;

import javafx.scene.shape.Shape;
import org.example.space_invaders_online.game.gameWorld.ClientGameObject;

public class ClientPlayer extends ClientGameObject {

    private Shape sprite;
    private int shoots;
    private String name;

    public ClientPlayer(int id, double pos_x, double pos_y) {
        super(id, pos_x, pos_y);
        shoots = 10;
    }

    public void updateFromServer(double x, double y, int shoots, String name) {
        setPosition(x, y);
    }

    public void render() {
        sprite.setLayoutX(pos_x);
        sprite.setLayoutY(pos_y);
    }
}

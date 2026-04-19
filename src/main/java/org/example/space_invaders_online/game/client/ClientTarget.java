package org.example.space_invaders_online.game.client;

import javafx.scene.shape.Shape;
import org.example.space_invaders_online.game.gameWorld.ClientGameObject;

public class ClientTarget extends ClientGameObject {
    private Shape sprite;

    public ClientTarget(int id, double pos_x, double pos_y) {
        super(id, pos_x, pos_y);
    }

    public void updateFromServer(double x, double y) {
        setPosition(x, y);
        render();
    }

    public void render() {
        sprite.setLayoutX(pos_x);
        sprite.setLayoutY(pos_y);
    }
}

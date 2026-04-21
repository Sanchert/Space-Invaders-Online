package org.example.space_invaders_online.game.client;

import javafx.scene.paint.Color;
import org.example.space_invaders_online.game.gameWorld.ClientGameObject;

public class ClientPlayer extends ClientGameObject {

    private int shoots;
    private String name;
    private final Color fillColor;

    public ClientPlayer(int id, Color fillColor) {
        super(id, 0, 0);
        this.fillColor = fillColor;
        this.shoots = 10;
    }

    public Color getFillColor() {
        return fillColor;
    }

    public void updateFromServer(double x, double y, int shoots, String name) {
        setPosition(x, y);
        this.shoots = shoots;
        this.name = name;
    }

    public int getShoots() {
        return shoots;
    }

    public String getName() {
        return name;
    }
}

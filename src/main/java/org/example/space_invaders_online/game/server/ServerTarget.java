package org.example.space_invaders_online.game.server;

import javafx.scene.shape.Shape;
import org.example.space_invaders_online.game.gameWorld.ServerGameObject;
import org.example.space_invaders_online.game.client.MoveDirection;

public class ServerTarget extends ServerGameObject {
    private int cost = 1;
    private double speed = 1.0f;
    private final static double UPPER_BOUND = 330.0;
    private final static double LOWER_BOUND = 20.0;
    private MoveDirection moveDirection = MoveDirection.MOVE_DOWN;

    public ServerTarget(int id, double pos_x, double pos_y, int cost) {
        super(id, pos_x, pos_y);
        this.cost = cost;
    }

    public void setSpeed(double speed) {
        this.speed = speed;
    }

    public int getCost() {
        return this.cost;
    }

    public void setCost(int points) {
        this.cost = points;
    }
    @Override
    public void update() {
        this.move();
    }
//    TODO: let's do this!
//    @Override
// Добавить метод serialize в ServerTarget.java
public DTOTarget serialize() {
    return new DTOTarget(objectId, (float)pos_x, (float)pos_y, cost, !destroyed);
}

//    TODO: let's do this!
    @Override
    public boolean collidesWith(Shape other) {
        return false;
    }

    private void move() {
        if (this.pos_y >= UPPER_BOUND) {
            moveDirection = MoveDirection.MOVE_DOWN;
        } else if (this.pos_y <= LOWER_BOUND) {
            moveDirection = MoveDirection.MOVE_UP;
        }

        switch (moveDirection) {
            case MOVE_UP   -> pos_y += speed;
            case MOVE_DOWN -> pos_y -= speed;
        }
    }
}

package org.example.space_invaders_online.game.server.object;

import org.example.space_invaders_online.game.gameWorld.ServerGameObject;
import org.example.space_invaders_online.game.client.MoveDirection;
import org.example.space_invaders_online.game.server.dto.DTOTarget;

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

    public DTOTarget serialize() {
        return new DTOTarget(objectId, pos_x, pos_y, cost, isDestroyed());
    }

    @Override
    public boolean collidesWith(ServerGameObject other) {
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

package org.example.space_invaders_online.game.server;

import javafx.scene.shape.Shape;
import org.example.space_invaders_online.game.gameWorld.ServerGameObject;
import org.example.space_invaders_online.game.client.MoveDirection;


public class ServerPlayer extends ServerGameObject {
    private double speed;
    private int shoots;
    private double shootLag = 0;
    private final int colorId;
    private String name;
    private int score;

    private final static double UPPER_BOUND = 270.0;
    private final static double LOWER_BOUND = 30.0;

    public ServerPlayer(int id, double pos_x, double pos_y, int shoots, int colorId) {
        super(id, pos_x, pos_y);
        this.shoots = shoots;
        this.colorId = colorId;
        this.score = 0;
        this.speed = 2.0;
    }

    @Override
    public void update() {
        if (shootLag >= 0.0) {
            shootLag -= ServerTime.FIXED_DELTA_TIME;
        }
    }

    @Override
    public boolean collidesWith(Shape other) {
        return false;
    }

    public void setSpeed(double speed) {
        this.speed = speed;
    }

    public void move(MoveDirection direction) {
        switch (direction) {
            case MOVE_UP   -> pos_y = Math.max(LOWER_BOUND, pos_y - speed);
            case MOVE_DOWN -> pos_y = Math.min(UPPER_BOUND, pos_y + speed);
        }
    }

    private boolean canShoot() {
        return (shoots > 0) && (shootLag <= 0);
    }

    public void setName(String newName) {
        name = newName;
    }

    public String getName() {
        return name;
    }

    public void shoot() {
        if (canShoot()) {
            shoots--;
            shootLag = 1;
        }
    }

    public int getColorId() {
        return colorId;
    }

    public int getScore() {
        return score;
    }

    public void addScore(int additionalPoints) {
        score += additionalPoints;
    }

    public int getShootsLeft() {
        return shoots;
    }

    public void resetScore() {
        this.score = 0;
    }

    public void resetShoots() {
        this.shoots = 10;
    }

    public DTOPlayer serialize() {
        return new DTOPlayer(
                objectId, name, (float)pos_x, (float)pos_y,
                shoots, score, colorId, !destroyed
        );
    }
}

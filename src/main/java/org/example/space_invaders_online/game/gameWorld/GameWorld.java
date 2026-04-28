package org.example.space_invaders_online.game.gameWorld;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import org.example.space_invaders_online.game.client.MoveDirection;
import org.example.space_invaders_online.game.client.Request;
import org.example.space_invaders_online.game.server.*;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class GameWorld {
    private String winner = null;
    private final Server server;
    private final Map<Integer, ServerGameObject> objects = new ConcurrentHashMap<>();
    private final Map<Integer, ServerPlayer> players = new ConcurrentHashMap<>();
    private final Map<Integer, ServerBullet> bullets = new ConcurrentHashMap<>();
    private final Map<Integer, ServerTarget> targets = new ConcurrentHashMap<>();

    private final AtomicInteger nextObjectId = new AtomicInteger(1);
    private double playerStartPosY = 60.0;

    private int currentScore = 0;
    private static final int WIN_SCORE = 6;
    private static final int NEAR_TARGET_POINTS = 6;
    private static final int FAR_TARGET_POINTS = 6;

    public GameWorld(Server server) {
        this.server = server;
    }

    /**
     * Загружает матч: регистрирует игроков, затем выдаёт id мишеням выше любого id игрока.
     */
    public void loadMatch(Collection<ServerPlayer> roster) {
        objects.clear();
        players.clear();
        bullets.clear();
        targets.clear();
        winner = null;
        playerStartPosY = 60.0;

        for (ServerPlayer p : roster) {
            objects.put(p.objectId, p);
            players.put(p.objectId, p);
        }

        int maxPlayerId = players.keySet().stream().mapToInt(Integer::intValue).max().orElse(0);
        nextObjectId.set(maxPlayerId + 1);

        spawnInitialTargets();
    }

    /** Мишени справа от игроков (логическая ширина поля до ~800 по X снаряда). */
    private void spawnInitialTargets() {
        double x = 620.0;
        double y = 30.0;

        for (int i = 0; i < 3; i++) {
            ServerTarget target = new ServerTarget(nextObjectId.getAndIncrement(), x, y, NEAR_TARGET_POINTS);
            objects.put(target.objectId, target);
            targets.put(target.objectId, target);
            x += 45.0;
            y += 4.0;
        }

        x = 740.0;
        y = 30.0;
        for (int i = 0; i < 2; i++) {
            ServerTarget target = new ServerTarget(nextObjectId.getAndIncrement(), x, y, FAR_TARGET_POINTS);
            objects.put(target.objectId, target);
            targets.put(target.objectId, target);
            x += 45.0;
            y += 4.0;
        }
    }

    public int addPlayer() {
        int id = nextObjectId.getAndIncrement();
        ServerPlayer player = new ServerPlayer(id, 21f, playerStartPosY, 10, id % 4);
        objects.put(id, player);
        players.put(id, player);
        playerStartPosY += 60f;
        return id;
    }

    public void addExistingPlayer(ServerPlayer player) {
        objects.put(player.objectId, player);
        players.put(player.objectId, player);
    }

    public void handleRequest(int playerId, Request request) {
        ServerPlayer player = players.get(playerId);
        if (player == null || player.isDestroyed()) return;

        switch (request.requestType()) {
            case MOVE_UP:
                player.move(MoveDirection.MOVE_UP);
                break;
            case MOVE_DOWN:
                player.move(MoveDirection.MOVE_DOWN);
                break;
            case SHOOT:
                if (player.getShootsLeft() > 0) {
                    player.shoot();
                    ServerBullet bullet = new ServerBullet(
                            nextObjectId.getAndIncrement(),
                            playerId,
                            player.pos_x + 10.0,
                            player.pos_y + 20.0
                    );
                    objects.put(bullet.objectId, bullet);
                    bullets.put(bullet.objectId, bullet);
                }
                break;
            case SET_NAME:
                player.setName(request.args());
                break;
        }
    }

    public void update() {
        for (ServerGameObject obj : objects.values()) {
            obj.update();
        }

        checkCollisions();
        clearDestroyedObjects();
        checkWinCondition();
    }

    private void checkCollisions() {
        for (ServerBullet bullet : bullets.values()) {
            if (bullet.isDestroyed()) continue;

            for (ServerTarget target : targets.values()) {
                if (target.isDestroyed()) continue;

                if (checkCollision(bullet, target)) {
                    bullet.destroy();
                    target.destroy();

                    int points = target.getCost();

                    if (server == null) {
                        currentScore += points;
                    } else {
                        ServerPlayer shooter = players.get(bullet.getOwnerId());
                        if (shooter != null) {
                            shooter.addScore(points);
                        }
                    }
                    break;
                }
            }
        }
    }

    private boolean checkCollision(ServerBullet bullet, ServerTarget target) {
        double bulletRight = bullet.pos_x + 10;
        double bulletBottom = bullet.pos_y + 10;
        double targetRight = target.pos_x + 15;
        double targetBottom = target.pos_y + 15;

        return bullet.pos_x < targetRight &&
                bulletRight > target.pos_x &&
                bullet.pos_y < targetBottom &&
                bulletBottom > target.pos_y;
    }

    private void clearDestroyedObjects() {
        objects.values().removeIf(ServerGameObject::isDestroyed);
        bullets.values().removeIf(ServerBullet::isDestroyed);
        targets.values().removeIf(ServerTarget::isDestroyed);
    }

    private void checkWinCondition() {
        if (server == null) {
            if (currentScore >= WIN_SCORE) {
                winner = "Player";
            }
        } else {
            for (ServerPlayer player : players.values()) {
                if (player.getScore() >= WIN_SCORE) {
                    winner = player.getName();
                    break;
                }
            }
        }
    }

    public String hasWinner() {
        return winner;
    }

    public Map<Integer, ServerBullet> getBullets() {
        return bullets;
    }

    public Map<Integer, ServerTarget> getTargets() {
        return targets;
    }

    public Map<Integer, ServerPlayer> getPlayers() {
        return players;
    }
}

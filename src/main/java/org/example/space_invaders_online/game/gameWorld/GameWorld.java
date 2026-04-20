package org.example.space_invaders_online.game.gameWorld;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import org.example.space_invaders_online.game.client.MoveDirection;
import org.example.space_invaders_online.game.client.Request;
import org.example.space_invaders_online.game.server.*;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class GameWorld {
    private String winner = null;
    private final Server server; // null для одиночной игры
    private final Map<Integer, ServerGameObject> objects = new ConcurrentHashMap<>();
    private final Map<Integer, ServerPlayer> players = new ConcurrentHashMap<>();
    private final Map<Integer, ServerBullet> bullets = new ConcurrentHashMap<>();
    private final Map<Integer, ServerTarget> targets = new ConcurrentHashMap<>();

    private final AtomicInteger nextObjectId = new AtomicInteger(1);
    private double playerStartPosY = 60.0;

    // Для одиночной игры
    private int currentScore = 0;
    private static final int WIN_SCORE = 6;
    private static final int NEAR_TARGET_POINTS = 1;
    private static final int FAR_TARGET_POINTS = 2;

    public GameWorld(Server server) {
        this.server = server;
    }

    public void init() {
        playerStartPosY = 60.0;
        objects.clear();
        players.clear();
        bullets.clear();
        targets.clear();

        // Создаём мишени: ближние и дальние
        double x = 270.0;
        double y = 30.0;

        // Ближние мишени (слева)
        for (int i = 0; i < 3; i++) {
            ServerTarget target = new ServerTarget(nextObjectId.getAndIncrement(), x, y, NEAR_TARGET_POINTS);
            objects.put(target.objectId, target);
            targets.put(target.objectId, target);
            x += 45.0;
            y += 4.0;
        }

        // Дальние мишени (справа)
        x = 450.0;
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
        // Обновляем все объекты
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
                        // Одиночная игра
                        currentScore += points;
                    } else {
                        // Сетевая игра
                        ServerPlayer player = players.get(bullet.getOwnerId());
                        if (player != null) {
                            player.addScore(points);
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

    public void render(GraphicsContext gc) {
        // Отрисовка игроков
        for (ServerPlayer player : players.values()) {
            if (!player.isDestroyed()) {
                gc.setFill(getColorForId(player.getColorId()));
                gc.fillRect(player.pos_x, player.pos_y, 30, 50);
            }
        }

        // Отрисовка пуль
        for (ServerBullet bullet : bullets.values()) {
            gc.setFill(Color.YELLOW);
            gc.fillRect(bullet.pos_x, bullet.pos_y, 10, 10);
        }

        // Отрисовка мишеней
        for (ServerTarget target : targets.values()) {
            gc.setFill(Color.RED);
            gc.fillOval(target.pos_x, target.pos_y, 30, 30);
        }
    }

    private Color getColorForId(int colorId) {
        return switch (colorId) {
            case 0 -> Color.BLUE;
            case 1 -> Color.RED;
            case 2 -> Color.GREEN;
            case 3 -> Color.ORANGE;
            default -> Color.PURPLE;
        };
    }

    public Map<Integer, ServerBullet> getBullets() { return bullets; }
    public Map<Integer, ServerTarget> getTargets() { return targets; }
    public Map<Integer, ServerPlayer> getPlayers() { return players; }

    public DTOGameState serialize() {
        // Сериализация состояния игры для отправки клиентам
        return new DTOGameState(players.values().stream().toList(),
                                         bullets.values().stream().toList(),
                                         targets.values().stream().toList());
    }
}

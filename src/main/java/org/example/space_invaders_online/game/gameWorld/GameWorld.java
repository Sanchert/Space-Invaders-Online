package org.example.space_invaders_online.game.gameWorld;

import org.example.space_invaders_online.game.client.MoveDirection;
import org.example.space_invaders_online.game.client.Request;
import org.example.space_invaders_online.game.server.ServerBullet;
import org.example.space_invaders_online.game.server.ServerPlayer;
import org.example.space_invaders_online.game.server.ServerTarget;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class GameWorld {
    private String winner = null;
    public String hasWinner() {
        return winner;
    }
    private double playerStartPosY = 60.0;
    private final Map<Integer, GameObject> objects = new ConcurrentHashMap<>();
    private final Map<Integer, ServerPlayer> players = new ConcurrentHashMap<>();
    private final Map<Integer, ServerBullet> bullets = new ConcurrentHashMap<>();
    private final Map<Integer, ServerTarget> targets = new ConcurrentHashMap<>();

    private final AtomicInteger nextObjectId = new AtomicInteger(1);

    public void init() {
        playerStartPosY = 60.0;
        double x = 270.0;
        double y = 30.0;
        for (int i = 0; i < 5; i++) {
            ServerTarget target = new ServerTarget(nextObjectId.getAndIncrement(), x, y, 3);
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
        //        players.put(id, player);
        playerStartPosY += 60f;
        return id;
    }

    public void destroyPlayer(int playerId) {
        players.get(playerId).destroy();
        bullets.values().forEach(serverBullet -> {
            if (serverBullet.getOwnerId() == playerId) {
                serverBullet.destroy();
            }
        });
    }

    public void handleRequest(int playerId, Request request) {
        if (objects.containsKey(playerId)) return;

        ServerPlayer player = (ServerPlayer)objects.get(playerId);

        switch (request.requestType()) {
            case MOVE_UP -> player.move(MoveDirection.MOVE_UP);
            case MOVE_DOWN -> player.move(MoveDirection.MOVE_DOWN);
            case SHOOT -> {
                player.shoot();
                ServerBullet bullet = new ServerBullet(
                        nextObjectId.getAndIncrement(),
                        playerId,
                        player.pos_x + 10.0,
                        player.pos_y + 20.0
                );
                objects.put(bullet.objectId, bullet);
            }
            case SET_NAME -> player.setName(request.args()); //TODO: split
        }
    }

    public void update() {

    }

    private void clearDeletedObjects() {
        objects.values().removeIf(gameObject -> gameObject.destroyed);
    }

    private void checkCollisions() {

    }

    public void serialize() {

    }
}

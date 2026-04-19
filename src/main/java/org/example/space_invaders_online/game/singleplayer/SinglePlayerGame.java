package org.example.space_invaders_online.game.singleplayer;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import org.example.space_invaders_online.game.client.MoveDirection;
import org.example.space_invaders_online.game.client.Request;
import org.example.space_invaders_online.game.client.RequestType;
import org.example.space_invaders_online.game.gameWorld.GameWorld;

import java.util.concurrent.atomic.AtomicBoolean;

public class SinglePlayerGame {
    private final Canvas canvas;
    private final GraphicsContext gc;
    private GameWorld gameWorld;
    private GameThread gameThread;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicBoolean paused = new AtomicBoolean(false);

    private int playerId;
    private int score = 0;
    private int shotsLeft = 10;

    public SinglePlayerGame(Canvas canvas) {
        this.canvas = canvas;
        this.gc = canvas.getGraphicsContext2D();
        initGame();
    }

    public void initGame() {
        gameWorld = new GameWorld(null); // null для одиночной игры
        gameWorld.init();
        playerId = gameWorld.addPlayer();
        score = 0;
        shotsLeft = 10;
    }

    public void start() {
        if (gameThread != null && gameThread.isAlive()) {
            reset();
        }

        running.set(true);
        paused.set(false);
        gameThread = new GameThread();
        gameThread.start();
    }

    public void pause() {
        paused.set(true);
        if (gameThread != null) {
            gameThread.suspendThread();
        }
    }

    public void resume() {
        paused.set(false);
        if (gameThread != null) {
            gameThread.resumeThread();
        }
    }

    public void reset() {
        running.set(false);
        if (gameThread != null) {
            gameThread.interrupt();
        }
        initGame();
    }

    public void shoot() {
        if (shotsLeft > 0 && !paused.get() && running.get()) {
            shotsLeft--;
            gameWorld.handleRequest(playerId, new Request(RequestType.SHOOT, "", playerId));
        }
    }

    public void move(MoveDirection direction) {
        if (!paused.get() && running.get()) {
            gameWorld.handleRequest(playerId, new Request(
                    direction == MoveDirection.MOVE_UP ? RequestType.MOVE_UP : RequestType.MOVE_DOWN,
                    "", playerId));
        }
    }

    public void updateScore(int points) {
        this.score += points;
    }

    public int getScore() { return score; }
    public int getShotsLeft() { return shotsLeft; }

    private class GameThread extends Thread {
        private volatile boolean suspended = false;

        public void suspendThread() {
            suspended = true;
        }

        public void resumeThread() {
            suspended = false;
            synchronized (this) {
                notify();
            }
        }

        @Override
        public void run() {
            long lastTime = System.nanoTime();
            final double TICK_RATE = 60.0;
            final double TIME_PER_TICK = 1_000_000_000.0 / TICK_RATE;
            double delta = 0;

            while (running.get() && !Thread.currentThread().isInterrupted()) {
                synchronized (this) {
                    while (suspended) {
                        try {
                            wait();
                        } catch (InterruptedException e) {
                            return;
                        }
                    }
                }

                long now = System.nanoTime();
                delta += (now - lastTime) / TIME_PER_TICK;
                lastTime = now;

                while (delta >= 1) {
                    if (!suspended) {
                        gameWorld.update();
                        render();
                    }
                    delta--;
                }

                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    break;
                }
            }
        }

        private void render() {
            javafx.application.Platform.runLater(() -> {
                gc.setFill(Color.BLACK);
                gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());

                // Отрисовка мира
                gameWorld.render(gc);

                // Отрисовка HUD
                gc.setFill(Color.WHITE);
                gc.setFont(javafx.scene.text.Font.font("Courier New", 20));
                gc.fillText("Score: " + score, 10, 30);
                gc.fillText("Shots: " + shotsLeft, 10, 60);

                if (paused.get()) {
                    gc.setFill(Color.rgb(0, 0, 0, 0.7));
                    gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
                    gc.setFill(Color.WHITE);
                    gc.setFont(javafx.scene.text.Font.font("Courier New", 40));
                    gc.fillText("PAUSED", canvas.getWidth() / 2 - 80, canvas.getHeight() / 2);
                }
            });
        }
    }
}

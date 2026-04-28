package org.example.space_invaders_online.game.sceneController.controllers;

import javafx.animation.AnimationTimer;
import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import org.example.space_invaders_online.game.client.*;
import org.example.space_invaders_online.game.client.object.ClientBullet;
import org.example.space_invaders_online.game.client.object.ClientPlayer;
import org.example.space_invaders_online.game.client.object.ClientTarget;
import org.example.space_invaders_online.game.sceneController.GameContext;
import org.example.space_invaders_online.game.sceneController.ScreenManager;
import org.example.space_invaders_online.game.sceneController.ScreenType;
import org.example.space_invaders_online.game.server.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static java.util.stream.Collectors.toSet;


public class GameScreenController extends BaseController implements INetworkListener {

    @FXML private Canvas gameCanvas;
    @FXML private VBox listRows;
    @FXML private Label winOverlayLabel;
    @FXML private VBox pauseOverlay;
    @FXML private Button pauseButton;
    @FXML private Button resumeButton;
    @FXML private Button exitToMenuBtn;
    @FXML private Label pauseTitleLabel;
    @FXML private Label pauseHintLabel;

    private static final double LOGICAL_W = 900;
    private static final double LOGICAL_H = 320;
    private static final double PLAYER_W = 30, PLAYER_H = 50;
    private static final double TARGET_W = 34, TARGET_H = 34;
    private static final double BULLET_W = 18, BULLET_H = 10;

    private final Image imgPlayer   = loadImage("/images/player/player1.png");
    private final Image imgEnemy    = loadImage("/images/enemy/enemy1.png");
    private final Image imgBullet   = loadImage("/images/shoot/shoot1.png");
    private final Image imgBg       = loadImage("/images/backgrounds/bg-space.png");

    private NetworkClient networkClient;
    private GraphicsContext gc;
    private AnimationTimer renderLoop;
    private int myPlayerId;
    private boolean isRunning = true;

    private final Map<Integer, ClientPlayer> players = new ConcurrentHashMap<>();
    private final Map<Integer, ClientBullet>  bullets = new ConcurrentHashMap<>();
    private final Map<Integer, ClientTarget>  targets = new ConcurrentHashMap<>();
    private final Map<Integer, PlayerInfoPanel> hudPanels = new HashMap<>();
    private final Map<RequestType, Request> requests = new HashMap<>();

    private final LeaderboardPopup leaderboardPopup = new LeaderboardPopup();

    public GameScreenController(ScreenManager screenManager, GameContext gameContext) {
        super(screenManager, gameContext);
    }

    @FXML
    public void initialize() {
        networkClient = gameContext.getNetworkClient();
        myPlayerId    = gameContext.getMyPlayerId();
        networkClient.setListener(this);

        initRequests();

        gc = gameCanvas.getGraphicsContext2D();

        StackPane gameStack = (StackPane)gameCanvas.getParent();
        gameCanvas.widthProperty().bind(gameStack.widthProperty());
        gameCanvas.heightProperty().bind(gameStack.heightProperty());

        gameCanvas.setFocusTraversable(true);
        gameCanvas.setOnKeyPressed(this::onKeyPressed);

        winOverlayLabel.setVisible(false);
        winOverlayLabel.setManaged(false);
        pauseOverlay.setVisible(false);
        pauseOverlay.setManaged(false);

        pauseButton .setOnAction(e  -> sendAction(RequestType.PAUSE));
        resumeButton.setOnAction(e  -> sendAction(RequestType.RESUME));
        exitToMenuBtn.setOnAction(e -> onExitToMenu());

        startRenderLoop();
    }

    private void initRequests() {
        requests.put(RequestType.MOVE_UP,           new Request(RequestType.MOVE_UP,         "", myPlayerId));
        requests.put(RequestType.MOVE_DOWN,         new Request(RequestType.MOVE_DOWN,       "", myPlayerId));
        requests.put(RequestType.SHOOT,             new Request(RequestType.SHOOT,           "", myPlayerId));
        requests.put(RequestType.PAUSE,             new Request(RequestType.PAUSE,           "", myPlayerId));
        requests.put(RequestType.RESUME,            new Request(RequestType.RESUME,          "", myPlayerId));
        requests.put(RequestType.DISCONNECT,        new Request(RequestType.DISCONNECT,      "", myPlayerId));
        requests.put(RequestType.GET_LEADERBOARD,   new Request(RequestType.GET_LEADERBOARD, "", myPlayerId));
    }

    private void startRenderLoop() {
        renderLoop = new AnimationTimer() {
            @Override public void handle(long now) { renderFrame(); }
        };
        renderLoop.start();
    }

    private void onKeyPressed(KeyEvent e) {
        switch (e.getCode()) {
            case W      -> sendAction(RequestType.MOVE_UP);
            case S      -> sendAction(RequestType.MOVE_DOWN);
            case D      -> sendAction(RequestType.SHOOT);
            case ESCAPE -> {
                isRunning = !isRunning;
                if (isRunning) sendAction(RequestType.PAUSE);
                else           sendAction(RequestType.RESUME);
            }
        }
    }

    private void sendAction(RequestType type) {
        Request r = requests.get(type);
        if (r != null && networkClient.isConnected()) {
            networkClient.send(r);
        }
    }

    // == INetworkListener =================================================

    @Override public void onInit(ServerMessage m) {}
    @Override public void onNameAccepted() {}
    @Override public void onNameRejected() {}
    @Override public void onPlayerListUpdate(ServerMessage m) {}
    @Override public void onGameStart() {}

    @Override
    public void onGameState(ServerMessage m) {
        if (m.currentGameState != null) {
            updateGameState(m.currentGameState);
        }
    }

    @Override
    public void onGamePaused() {
        isRunning = false;
        pauseOverlay.setVisible(true);
        pauseOverlay.setManaged(true);
    }

    @Override
    public void onGameResumed() {
        isRunning = true;
        pauseOverlay.setVisible(false);
        pauseOverlay.setManaged(false);
    }

    @Override
    public void onWin(ServerMessage m) {
        pauseTitleLabel.setText("WINNER: " + m.args);
        pauseHintLabel.setVisible(false);
        pauseHintLabel.setManaged(false);

        pauseOverlay.setVisible(true);
        pauseOverlay.setManaged(true);
    }

    @Override
    public void onLeaderBoard(ServerMessage m) {
        leaderboardPopup.show(m.leaderboard);
    }

    @Override
    public void onDisconnected() {
        renderLoop.stop();
        networkClient.setListener(null);
        gameContext.setNetworkClient(null);
        screenManager.switchScreen(ScreenType.MAIN_MENU, gameContext);
    }

    // == Game state update ============================================
    private void updateGameState(DTOGameState state) {
        if (state == null) return;

        if (state.players != null) {
            Set<Integer> ids = state.players.stream()
                    .map(p -> p.objectID).collect(toSet());
            players.keySet().removeIf(id -> !ids.contains(id));
        }
        if (state.bullets != null) {
            Set<Integer> ids = state.bullets.stream()
                    .map(b -> b.objectID).collect(toSet());
            bullets.keySet().removeIf(id -> !ids.contains(id));
        }
        if (state.targets != null) {
            Set<Integer> ids = state.targets.stream()
                    .map(t -> t.objectID).collect(toSet());
            targets.keySet().removeIf(id -> !ids.contains(id));
        }

        // Add or update
        if (state.players != null)
            for (DTOPlayer dto : state.players)
                players.computeIfAbsent(dto.objectID, id -> new ClientPlayer(id, getColor(dto.colorID)))
                        .updateFromServer(dto.pos_x, dto.pos_y, dto.shoots, dto.name);

        if (state.bullets != null)
            for (DTOBullet dto : state.bullets)
                bullets.computeIfAbsent(dto.objectID, id -> new ClientBullet(id, dto.pos_x, dto.pos_y))
                        .updateFromServer(dto.pos_x, dto.pos_y);

        if (state.targets != null)
            for (DTOTarget dto : state.targets)
                targets.computeIfAbsent(dto.objectID, id -> new ClientTarget(id, dto.pos_x, dto.pos_y))
                        .updateFromServer(dto.pos_x, dto.pos_y);

        if(state.players != null) {
            syncHudPanels(state.players);
        }
    }

    private void syncHudPanels(List<DTOPlayer> dtos) {
        Set<Integer> active = new HashSet<>();
        for (DTOPlayer dto : dtos) {
            active.add(dto.objectID);
            String name = (dto.name != null && !dto.name.isEmpty()) ? dto.name : "Player " + dto.objectID;
            PlayerInfoPanel panel = hudPanels.computeIfAbsent(dto.objectID, id -> {
                PlayerInfoPanel p = new PlayerInfoPanel(name, dto.shoots, dto.colorID);
                listRows.getChildren().add(p);
                return p;
            });
            panel.updateName(name);
            panel.updateScore(dto.score);
            panel.updateShoots(dto.shoots);
            panel.setCurrentPlayer(dto.objectID == myPlayerId);
        }
        hudPanels.entrySet().removeIf(e -> {
            if (!active.contains(e.getKey())) {
                listRows.getChildren().remove(e.getValue());
                return true;
            }
            return false;
        });
    }

    // === Rendering ==================================

    private void renderFrame() {
        if (gc == null) {
            return;
        }

        double w = gameCanvas.getWidth();
        double h = gameCanvas.getHeight();

        if (w <= 0 || h <= 0) {
            return;
        }

        double scale = Math.min(w / LOGICAL_W, h / LOGICAL_H);
        double ox = (w - LOGICAL_W * scale) / 2;
        double oy = (h - LOGICAL_H * scale) / 2;

        gc.setFill(javafx.scene.paint.Color.BLACK);
        gc.fillRect(0, 0, w, h);
        if (imgBg != null && !imgBg.isError()) {
            gc.drawImage(imgBg, 0, 0, w, h);
        }

        for (ClientTarget t : targets.values()) drawSprite(imgEnemy,  ox + t.getPosX() * scale, oy + t.getPosY() * scale, TARGET_W * scale, TARGET_H * scale, javafx.scene.paint.Color.RED);
        for (ClientPlayer p : players.values()) drawSprite(imgPlayer, ox + p.getPosX() * scale, oy + p.getPosY() * scale, PLAYER_W * scale, PLAYER_H * scale, p.getFillColor());
        for (ClientBullet b : bullets.values()) drawSprite(imgBullet, ox + b.getPosX() * scale, oy + b.getPosY() * scale, BULLET_W * scale, BULLET_H * scale, javafx.scene.paint.Color.YELLOW);
    }

    private void drawSprite(Image img, double pos_x, double pos_y, double w, double h, javafx.scene.paint.Color color) {
        if (img != null && !img.isError()) {
            gc.drawImage(img, pos_x, pos_y, w, h);
        } else {
            gc.setFill(color);
            gc.fillRect(pos_x, pos_y, w, h);
        }
    }

    private static Image loadImage(String path) {
        var url = GameScreenController.class.getResource(path);
        return url != null ? new Image(url.toExternalForm(), false) : null;
    }

    private javafx.scene.paint.Color getColor(int id) {
        return switch (id) {
            case 0 -> javafx.scene.paint.Color.CYAN;
            case 1 -> javafx.scene.paint.Color.RED;
            case 2 -> javafx.scene.paint.Color.GREEN;
            case 3 -> javafx.scene.paint.Color.ORANGE;
            default -> javafx.scene.paint.Color.PURPLE;
        };
    }

    private void onExitToMenu() {
        sendAction(RequestType.DISCONNECT);
        renderLoop.stop();
        networkClient.disconnect(false);  // just close — server detects it via IOException
        gameContext.setNetworkClient(null);
        networkClient.setListener(null);
        hudPanels.clear();
        players.clear();
        bullets.clear();
        targets.clear();
        screenManager.switchScreen(ScreenType.MAIN_MENU, gameContext);
    }
}

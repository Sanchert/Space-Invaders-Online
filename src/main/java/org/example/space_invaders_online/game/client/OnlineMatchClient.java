package org.example.space_invaders_online.game.client;

import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import org.example.space_invaders_online.game.client.object.ClientBullet;
import org.example.space_invaders_online.game.client.object.ClientPlayer;
import org.example.space_invaders_online.game.client.object.ClientTarget;
import org.example.space_invaders_online.game.database.PlayerStats;
import org.example.space_invaders_online.game.server.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Одна TCP-сессия с сервером: лобби в {@code menu.fxml}, игровое поле в {@code game.fxml}.
 */
public class OnlineMatchClient implements INetworkListener {

    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 12345;

    private static final double LOGICAL_W = 900;
    private static final double LOGICAL_H = 320;

    /** Логические размеры отрисовки (согласованы с хитбоксами сервера). */
    private static final double PLAYER_W = 30;
    private static final double PLAYER_H = 50;
    private static final double TARGET_W = 34;
    private static final double TARGET_H = 34;
    private static final double BULLET_W = 18;
    private static final double BULLET_H = 10;

    private final Image spaceBackground;
    private final Image imgPlayer;
    private final Image imgEnemy;
    private final Image imgBullet;

    private final NetworkClient networkClient = new NetworkClient();
    private final LeaderboardPopup leaderboardPopup = new LeaderboardPopup();

    private VBox lobbyPlayerList;
    private Label lobbyStatusLabel;
    private Button lobbyReadyButton;
    private Button lobbyLeaderboardButton;
    private Runnable onNavigateToGame;
    private Runnable onNameRejectedUi;

    private Canvas gameCanvas;
    private Region canvasSizeHost;
    private Label winLabel;
    private VBox pauseOverlay;
    private VBox gameHudRows;

    private GraphicsContext gc;
    private AnimationTimer renderLoop;

    private int myPlayerId = -1;
    public void setPlayerID(int id) { myPlayerId = id; }
    private boolean isReady = false;
    private boolean isRunning = true;
    private volatile boolean userRequestedExit;
    private String pendingPlayerName;

    private final Map<Integer, ClientPlayer> remotePlayers = new ConcurrentHashMap<>();
    private final Map<Integer, ClientBullet> remoteBullets = new ConcurrentHashMap<>();
    private final Map<Integer, ClientTarget> remoteTargets = new ConcurrentHashMap<>();
    private final Map<Integer, PlayerInfoPanel> lobbyPanels = new HashMap<>();
    private final Map<Integer, PlayerInfoPanel> gameHudPanels = new HashMap<>();
    private final Map<RequestType, Request> requestHash = new ConcurrentHashMap<>();

    public OnlineMatchClient() {
        networkClient.setListener(this);
        this.spaceBackground = loadImage("/images/backgrounds/bg-space.png");
        this.imgPlayer = loadImage("/images/player/player1.png");
        this.imgEnemy = loadImage("/images/enemy/enemy1.png");
        this.imgBullet = loadImage("/images/shoot/shoot1.png");
    }

    private static Image loadImage(String classpath) {
        var url = OnlineMatchClient.class.getResource(classpath);
        return url != null ? new Image(url.toExternalForm(), false) : null;
    }
    // TODO: это говно сделало сраное копирование полей из всех контроллеров. FUCK!!!
    public void bindLobby(VBox playerListPanel,
                          Label statusLabel,
                          Button readyButton,
                          Button leaderboardButton,
                          Runnable onNavigateToGame,
                          Runnable onNameRejectedUi) {
        this.lobbyPlayerList = playerListPanel;
        this.lobbyStatusLabel = statusLabel;
        this.lobbyReadyButton = readyButton;
        this.lobbyLeaderboardButton = leaderboardButton;
        this.onNavigateToGame = onNavigateToGame;
        this.onNameRejectedUi = onNameRejectedUi;
        if (readyButton != null) {
            readyButton.setOnAction(e -> onReadyBtnClick());
        }
        if (leaderboardButton != null) {
            leaderboardButton.setOnAction(e -> onLeaderboardBtnClick());
        }
    }
    // TODO: FUCK THIS SHIT
    public void bindGame(Canvas canvas,
                         StackPane gameStack,
                         Label winLabel,
                         VBox pauseOverlay,
                         VBox gameHudRows) {
        this.gameCanvas = canvas;
        this.canvasSizeHost = gameStack;
        this.winLabel = winLabel;
        this.pauseOverlay = pauseOverlay;
        this.gameHudRows = gameHudRows;

        if (canvas == null) {
            return;
        }
        gc = canvas.getGraphicsContext2D();
        canvas.setFocusTraversable(true);
        bindCanvasSize();
        canvas.setOnKeyPressed(this::onGameKeyPressed);

        if (renderLoop == null) {
            renderLoop = new AnimationTimer() {
                @Override
                public void handle(long now) {
                    renderFrame();
                }
            };
        }
        renderLoop.start();
        Platform.runLater(canvas::requestFocus);
    }

    public void bindPauseResumeButtons(Button pauseButton, Button resumeButton) {
        if (pauseButton != null) {
            pauseButton.setOnAction(e -> onPauseBtnClick());
        }
        if (resumeButton != null) {
            resumeButton.setOnAction(e -> onResumeBtnClick());
        }
    }

    public void beginConnection(String playerName) throws IOException {
        userRequestedExit = false;
        this.pendingPlayerName = playerName;
        networkClient.connect(SERVER_HOST, SERVER_PORT);
    }

    public void submitPlayerName(String name) {
        if (!networkClient.isConnected()) {
            return;
        }
        if (myPlayerId < 0) {
            pendingPlayerName = name;
            return;
        }
        sendAction(new Request(RequestType.SET_NAME, name, myPlayerId));
        if (lobbyStatusLabel != null) {
            lobbyStatusLabel.setText("Checking name...");
            lobbyStatusLabel.setTextFill(Color.YELLOW);
        }
    }

    public boolean isConnected() {
        return networkClient.isConnected();
    }

    public void shutdownForMenuBack() {
        userRequestedExit = true;
        stopRenderLoop();
        sendAction(requestHash.get(RequestType.DISCONNECT));
        networkClient.disconnect(false);
        resetMatchStateAfterShutdown();
    }

    /** Сброс локального состояния матча (в т.ч. при «мёртвом» клиенте перед новым подключением). */
    public void resetMatchStateAfterShutdown() {
        if (gameCanvas != null) {
            gameCanvas.widthProperty().unbind();
            gameCanvas.heightProperty().unbind();
        }
        gameCanvas = null;
        canvasSizeHost = null;
        gc = null;
        winLabel = null;
        pauseOverlay = null;
        gameHudRows = null;
        myPlayerId = -1;
        isReady = false;
        isRunning = true;
        requestHash.clear();
        clearPanelMap(lobbyPanels, lobbyPlayerList);
        clearPanelMap(gameHudPanels, gameHudRows);
        remotePlayers.clear();
        remoteBullets.clear();
        remoteTargets.clear();
        if (lobbyReadyButton != null) {
            lobbyReadyButton.setText("READY");
            lobbyReadyButton.setStyle("-fx-background-color: #4CAF50;");
        }
    }

    private void clearPanelMap(Map<Integer, PlayerInfoPanel> map, VBox container) {
        if (container != null) {
            for (PlayerInfoPanel p : map.values()) {
                container.getChildren().remove(p);
            }
        }
        map.clear();
    }

    private void stopRenderLoop() {
        if (renderLoop != null) {
            renderLoop.stop();
        }
    }

    private void bindCanvasSize() {
        if (gameCanvas == null || canvasSizeHost == null) {
            return;
        }
        gameCanvas.widthProperty().bind(canvasSizeHost.widthProperty());
        gameCanvas.heightProperty().bind(canvasSizeHost.heightProperty());
    }

    private void initMyCommands() {
        requestHash.clear();
        requestHash.put(RequestType.MOVE_UP, new Request(RequestType.MOVE_UP, "", myPlayerId));
        requestHash.put(RequestType.MOVE_DOWN, new Request(RequestType.MOVE_DOWN, "", myPlayerId));
        requestHash.put(RequestType.SHOOT, new Request(RequestType.SHOOT, "", myPlayerId));
        requestHash.put(RequestType.PAUSE, new Request(RequestType.PAUSE, "", myPlayerId));
        requestHash.put(RequestType.RESUME, new Request(RequestType.RESUME, "", myPlayerId));
        requestHash.put(RequestType.DISCONNECT, new Request(RequestType.DISCONNECT, "", myPlayerId));
    }

    private void drawBackground(double imgWidth, double imgHeight) {
        gc.setFill(Color.BLACK);
        gc.fillRect(0, 0, imgWidth, imgHeight);
        if (spaceBackground == null || spaceBackground.isError()) {
            gc.setFill(Color.rgb(10, 14, 36));
            gc.fillRect(0, 0, imgWidth, imgHeight);
            return;
        }
        double realImgW = spaceBackground.getWidth();
        double realImgH = spaceBackground.getHeight();
        if (realImgW <= 0 || realImgH <= 0) {
            gc.drawImage(spaceBackground, 0, 0, imgWidth, imgHeight);
            return;
        }
        double fit = Math.min(imgWidth / realImgW, imgHeight / realImgH);
        double scale = Math.min(1.0, fit);
        double dWidth = realImgW * scale;
        double dHeight = realImgH * scale;
        double bx = (imgWidth - dWidth) / 2;
        double by = (imgHeight - dHeight) / 2;
        gc.drawImage(spaceBackground, bx, by, dWidth, dHeight);
    }

    private void drawSprite(GraphicsContext g, Image img, double x, double y, double bw, double bh, Color fallback) {
        if (img != null && !img.isError()) {
            g.drawImage(img, x, y, bw, bh);
        } else {
            g.setFill(fallback);
            g.fillRect(x, y, bw, bh);
        }
    }

    private void renderFrame() {
        if (gc == null || gameCanvas == null) {
            return;
        }
        double w = gameCanvas.getWidth();
        double h = gameCanvas.getHeight();
        if (w <= 0 || h <= 0) {
            return;
        }

        drawBackground(w, h);

        double scale = Math.min(w / LOGICAL_W, h / LOGICAL_H);
        double ox = (w - LOGICAL_W * scale) / 2;
        double oy = (h - LOGICAL_H * scale) / 2;

        gc.setStroke(Color.rgb(0, 255, 195, 0.35));
        gc.setLineWidth(1);
        gc.strokeRect(ox, oy, LOGICAL_W * scale, LOGICAL_H * scale);

        for (ClientTarget t : remoteTargets.values()) {
            double px = ox + t.getPosX() * scale;
            double py = oy + t.getPosY() * scale;
            drawSprite(gc, imgEnemy, px, py, TARGET_W * scale, TARGET_H * scale, Color.web("#ff6b6b"));
        }

        for (ClientPlayer p : remotePlayers.values()) {
            double px = ox + p.getPosX() * scale;
            double py = oy + p.getPosY() * scale;
            drawSprite(gc, imgPlayer, px, py, PLAYER_W * scale, PLAYER_H * scale, p.getFillColor());
        }

        for (ClientBullet b : remoteBullets.values()) {
            double px = ox + b.getPosX() * scale;
            double py = oy + b.getPosY() * scale;
            drawSprite(gc, imgBullet, px, py, BULLET_W * scale, BULLET_H * scale, Color.YELLOW);
        }
    }

    @Override
    public void onInit(ServerMessage msg) {
        myPlayerId = msg.playerId;
        initMyCommands();
        if (pendingPlayerName != null) {
            sendAction(new Request(RequestType.SET_NAME, pendingPlayerName, myPlayerId));
            pendingPlayerName = null;
        }
    }

    @Override
    public void onGameState(ServerMessage msg) {
        if (msg.currentGameState != null) {
            updateGameState(msg.currentGameState);
        }
    }

    @Override
    public void onNameAccepted() {
        if (lobbyStatusLabel != null) {
            lobbyStatusLabel.setText("Name accepted! Press Ready when you want to start.");
            lobbyStatusLabel.setTextFill(Color.LIGHTGREEN);
        }
    }

    @Override
    public void onNameRejected() {
        if (lobbyStatusLabel != null) {
            lobbyStatusLabel.setText("Name already taken or invalid.");
            lobbyStatusLabel.setTextFill(Color.RED);
        }
        if (onNameRejectedUi != null) {
            Platform.runLater(onNameRejectedUi);
        }
    }

    @Override
    public void onPlayerListUpdate(ServerMessage msg) {
        syncPlayerPanels(msg.players != null ? msg.players : List.of(), lobbyPlayerList, lobbyPanels);
    }

    @Override
    public void onGameStart() {
        if (winLabel != null) {
            winLabel.setVisible(false);
        }
        if (onNavigateToGame != null) {
            Platform.runLater(onNavigateToGame);
        }
        Platform.runLater(() -> {
            if (gameCanvas != null) {
                gameCanvas.requestFocus();
            }
        });
    }

    @Override
    public void onGamePaused() {
        isRunning = false;
        if (pauseOverlay != null) {
            pauseOverlay.setManaged(true);
            pauseOverlay.setVisible(true);
        }
    }

    @Override
    public void onGameResumed() {
        isRunning = true;
        if (pauseOverlay != null) {
            pauseOverlay.setVisible(false);
            pauseOverlay.setManaged(false);
        }
        if (gameCanvas != null) {
            gameCanvas.requestFocus();
        }
    }

    @Override
    public void onWin(ServerMessage msg) {
        if (winLabel != null) {
            winLabel.setText("WINNER: " + msg.args);
            winLabel.setManaged(true);
            winLabel.setVisible(true);
        }
    }

    @Override
    public void onLeaderBoard(List<PlayerStats> board) {
        leaderboardPopup.show(board);
    }

    @Override
    public void onDisconnected() {
        if (userRequestedExit) {
            return;
        }
        if (lobbyStatusLabel != null) {
            lobbyStatusLabel.setText("Connection lost");
            lobbyStatusLabel.setTextFill(Color.RED);
        }
    }

    private void syncPlayerPanels(List<DTOPlayer> players, VBox container, Map<Integer, PlayerInfoPanel> panels) {
        if (container == null || players == null) {
            return;
        }

        for (PlayerInfoPanel panel : panels.values()) {
            panel.setDisconnected(true);
        }

        Set<Integer> activeIds = new HashSet<>();
        for (DTOPlayer sp : players) {
            activeIds.add(sp.objectID);
            PlayerInfoPanel panel = panels.get(sp.objectID);
            String displayName = sp.name != null && !sp.name.isEmpty() ? sp.name : "Player " + sp.objectID;
            if (panel == null) {
                panel = new PlayerInfoPanel(sp.objectID, displayName, sp.shoots, sp.colorID);
                panels.put(sp.objectID, panel);
                container.getChildren().add(panel);
            }
            panel.updateName(displayName);
            panel.updateScore(sp.score);
            panel.updateShoots(sp.shoots);
            panel.setDisconnected(false);
            panel.setCurrentPlayer(sp.objectID == myPlayerId);
        }

        panels.entrySet().removeIf(entry -> {
            if (!activeIds.contains(entry.getKey())) {
                container.getChildren().remove(entry.getValue());
                return true;
            }
            return false;
        });
    }

    private void updateGameState(DTOGameState state) {
        cleanupRemovedObjects(state);

        if (state.players != null) {
            for (DTOPlayer sp : state.players) {

                if (remotePlayers.get(sp.objectID) != null) {
                    remotePlayers.get(sp.objectID).updateFromServer(sp.pos_x, sp.pos_y, sp.shoots, sp.name);
                } else {
                    remotePlayers.put(sp.objectID, new ClientPlayer(sp.objectID, getPlayerColor(sp.colorID)));
                }
            }
        }

        if (state.bullets != null) {
            for (DTOBullet sb : state.bullets) {
                if (remoteBullets.get(sb.objectID) == null) {
                    remoteBullets.put(sb.objectID, new ClientBullet(sb.objectID, sb.pos_x, sb.pos_y));
                }
                remoteBullets.get(sb.objectID).updateFromServer(sb.pos_x, sb.pos_y);
            }
        }

        if (state.targets != null) {
            for (DTOTarget st : state.targets) {
                if (remoteTargets.get(st.objectID) == null) {
                    remoteTargets.put(st.objectID, new ClientTarget(st.objectID, st.pos_x, st.pos_y));
                }
                remoteTargets.get(st.objectID).updateFromServer(st.pos_x, st.pos_y);
            }
        }

        if (gameHudRows != null) {
            syncPlayerPanels(state.players, gameHudRows, gameHudPanels);
        }

        if (lobbyPlayerList != null && lobbyPlayerList.isVisible()) {
            syncPlayerPanels(state.players, lobbyPlayerList, lobbyPanels);
        }
    }

    private void cleanupRemovedObjects(DTOGameState newState) {
        Set<Integer> playerIds = new HashSet<>();
        Set<Integer> bulletIds = new HashSet<>();
        Set<Integer> targetIds = new HashSet<>();

        if (newState.players != null) {
            newState.players.forEach(sp -> playerIds.add(sp.objectID));
        }
        if (newState.bullets != null) {
            newState.bullets.forEach(sb -> bulletIds.add(sb.objectID));
        }
        if (newState.targets != null) {
            newState.targets.forEach(st -> targetIds.add(st.objectID));
        }

        remotePlayers.keySet().removeIf(id -> !playerIds.contains(id));
        remoteBullets.keySet().removeIf(id -> !bulletIds.contains(id));
        remoteTargets.keySet().removeIf(id -> !targetIds.contains(id));
    }

    private Color getPlayerColor(int colorID) {
        return switch (colorID) {
            case 0 -> Color.BLUE;
            case 1 -> Color.RED;
            case 2 -> Color.GREEN;
            case 3 -> Color.ORANGE;
            default -> Color.PURPLE;
        };
    }

    private void sendAction(Request request) {
        if (!networkClient.isConnected() || request == null) {
            return;
        }
        networkClient.send(request);
    }

    private void onGameKeyPressed(KeyEvent event) {
        if (!networkClient.isConnected()) {
            return;
        }
        KeyCode code = event.getCode();
        switch (code) {
            case W -> sendAction(requestHash.get(RequestType.MOVE_UP));
            case S -> sendAction(requestHash.get(RequestType.MOVE_DOWN));
            case D -> sendAction(requestHash.get(RequestType.SHOOT));
            case ESCAPE -> {
                if (isRunning) {
                    sendAction(requestHash.get(RequestType.PAUSE));
                } else {
                    sendAction(requestHash.get(RequestType.RESUME));
                }
                isRunning = !isRunning;
            }
            default -> { }
        }
    }

    private void onReadyBtnClick() {
        if (networkClient.isConnected()) {
            isReady = !isReady;
            sendAction(new Request(RequestType.START, "", myPlayerId));
            if (lobbyReadyButton != null) {
                lobbyReadyButton.setText(isReady ? "CANCEL" : "READY");
                lobbyReadyButton.setStyle(isReady
                        ? "-fx-background-color: #ff9800;"
                        : "-fx-background-color: #4CAF50;");
            }
        }
    }

    private void onPauseBtnClick() {
        if (networkClient.isConnected()) {
            sendAction(requestHash.get(RequestType.PAUSE));
        }
    }

    private void onResumeBtnClick() {
        if (networkClient.isConnected()) {
            sendAction(requestHash.get(RequestType.RESUME));
        }
    }

    private void onLeaderboardBtnClick() {
        if (networkClient.isConnected()) {
            sendAction(new Request(RequestType.GET_LEADERBOARD, "", myPlayerId));
        }
    }
}

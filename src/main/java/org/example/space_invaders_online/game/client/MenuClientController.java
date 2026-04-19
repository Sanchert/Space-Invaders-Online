// src/main/java/org/example/space_invaders_online/game/client/GameClientController.java
package org.example.space_invaders_online.game.client;

import com.google.gson.Gson;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import org.example.space_invaders_online.game.database.PlayerStats;
import org.example.space_invaders_online.game.server.*;

import java.io.*;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class MenuClientController {

    // FXML элементы
    @FXML private AnchorPane Root;
    @FXML private Canvas gameCanvas;
    @FXML private VBox playerListPanel;
    @FXML private AnchorPane startWindow;
    @FXML private TextField textField;
    @FXML private Label warningLabel;
    @FXML private Label winLabel;
    @FXML private Button readyButton;
    @FXML private Button startBtn;
    @FXML private Button pauseBtn;
    @FXML private Button resumeBtn;
    @FXML private Button exitBtn;
    @FXML private Button leaderboardBtn;

    private GraphicsContext gc;

    // Сетевое взаимодействие
    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 12345;

    private Socket socket;
    private ObjectOutputStream oos;
    private ObjectInputStream ois;
    private volatile boolean connected = false;

    private int myPlayerId = -1;
    private volatile boolean running = true;
    private boolean isReady = false;
    private static final Gson gson = new Gson();

    private Thread senderThread;
    private Thread readerThread;

    private boolean isRunning = true;

    // Игровые объекты
    private final Map<Integer, ClientPlayer> remotePlayers = new ConcurrentHashMap<>();
    private final Map<Integer, ClientBullet> remoteBullets = new ConcurrentHashMap<>();
    private final Map<Integer, ClientTarget> remoteTargets = new ConcurrentHashMap<>();
    private final Map<Integer, PlayerInfoPanel> playerInfoPanels = new HashMap<>();

    // Управление
    private final Object actionLock = new Object();
    private String pendingAction = null;

    private Map<RequestType, Request> requestHash;

    // Отображение
    private boolean showLeaderboard = false;

    @FXML
    public void initialize() {
        gc = gameCanvas.getGraphicsContext2D();
        gameCanvas.setFocusTraversable(true);
        gameCanvas.requestFocus();

        setupEventHandlers();

        connectToServer();

        senderThread = new Thread(this::sendLoop);
        senderThread.setDaemon(true);
        senderThread.start();

        readerThread = new Thread(this::readLoop);
        readerThread.setDaemon(true);
        readerThread.start();

//        Thread renderThread = new Thread(this::renderLoop);
//        renderThread.setDaemon(true);
//        renderThread.start();
    }

    private void setupEventHandlers() {
        gameCanvas.setOnKeyPressed(this::onKeyPressed);

        if (readyButton != null) {
            readyButton.setOnAction(e -> onReadyBtnClick());
        }
        if (startBtn != null) {
            startBtn.setOnAction(e -> onStartBtnClick());
        }
        if (pauseBtn != null) {
            pauseBtn.setOnAction(e -> onPauseBtnClick());
        }
        if (resumeBtn != null) {
            resumeBtn.setOnAction(e -> onResumeBtnClick());
        }
        if (exitBtn != null) {
            exitBtn.setOnAction(e -> onExitBtnClick());
        }
        if (leaderboardBtn != null) {
            leaderboardBtn.setOnAction(e -> onLeaderboardBtnClick());
        }
    }

    private void connectToServer() {
        try {
            socket = new Socket(SERVER_HOST, SERVER_PORT);
            oos = new ObjectOutputStream(socket.getOutputStream());
            ois = new ObjectInputStream(socket.getInputStream());
            connected = true;
        } catch (IOException e) {
//            Platform.runLater(() -> showError("Cannot connect to server: " + e.getMessage()));
        }
    }

    private void readLoop() {
        try {
            while (connected) {
                String line = ois.readUTF();
                ServerMessage message = gson.fromJson(line, ServerMessage.class);
                Platform.runLater(() -> handleServerMessage(message));
            }
        } catch (IOException e) {
//            Platform.runLater(() -> showError("Connection lost: " + e.getMessage()));
        } finally {
            connected = false;
        }
    }

    private void handleServerMessage(ServerMessage message) {
        switch (message.type) {
            case INIT:
                myPlayerId = message.playerId;
                initMyCommands();
                System.out.println("[CLIENT] My ID: " + myPlayerId);
                break;

            case NAME_ACCEPTED:
                startWindow.setVisible(false);
                warningLabel.setText("Name accepted! Click READY to start");
                warningLabel.setTextFill(Color.GREEN);
                break;

            case NAME_REJECTED:
                Platform.runLater(() -> {
                    warningLabel.setText("Name already taken or invalid!");
                    warningLabel.setTextFill(Color.RED);
                    warningLabel.setVisible(true);
                    textField.setDisable(false);
                    textField.clear();
                    textField.requestFocus();
                });
                break;

            case UPDATE:
                updateGameState(message.currentGameState);
                break;

            case WIN:
                winLabel.setText("🏆 WINNER: " + message.args + " 🏆");
                winLabel.setVisible(true);
                break;

            case LEADERBOARD:
                showLeaderboardWindow(message.leaderboard);
                break;
        }
    }

    private void initMyCommands() {
        requestHash.put(RequestType.MOVE_UP,    new Request(RequestType.MOVE_UP, "", myPlayerId));
        requestHash.put(RequestType.MOVE_DOWN,  new Request(RequestType.MOVE_DOWN, "", myPlayerId));
        requestHash.put(RequestType.SHOOT,      new Request(RequestType.SHOOT, "", myPlayerId));
        requestHash.put(RequestType.PAUSE,      new Request(RequestType.PAUSE, "", myPlayerId));
        requestHash.put(RequestType.RESUME,     new Request(RequestType.RESUME, "", myPlayerId));
        requestHash.put(RequestType.DISCONNECT, new Request(RequestType.DISCONNECT, "", myPlayerId));
    }

    private void updatePlayerPanels(List<SerializablePlayer> players) {
        if (playerListPanel == null || players == null) return;

        for (PlayerInfoPanel panel : playerInfoPanels.values()) {
            panel.setDisconnected(true);
        }

        Set<Integer> activeIds = new HashSet<>();

        for (SerializablePlayer sp : players) {
            activeIds.add(sp.objectID);
            PlayerInfoPanel panel = playerInfoPanels.get(sp.objectID);

            if (panel == null) {
                panel = new PlayerInfoPanel(sp.objectID, sp.name, sp.shoots, sp.colorID);
                playerInfoPanels.put(sp.objectID, panel);
                playerListPanel.getChildren().add(panel);
            }

            panel.updateName(sp.name);
            panel.updateScore(sp.score);
            panel.updateShoots(sp.shoots);
            panel.setDisconnected(false);
            panel.setCurrentPlayer(sp.objectID == myPlayerId);
        }

        playerInfoPanels.entrySet().removeIf(entry -> {
            if (!activeIds.contains(entry.getKey())) {
                playerListPanel.getChildren().remove(entry.getValue());
                return true;
            }
            return false;
        });
    }

    private void updateGameState(SerializableGameState state) {
        cleanupRemovedObjects(state);

        if (state.players != null) {
            for (SerializablePlayer sp : state.players) {
                ClientPlayer player = remotePlayers.get(sp.objectID);
                if (player != null) {
                    player.updateFromServer(sp.pos_x, sp.pos_y, sp.shoots, sp.name);
                } else {
                    player = new ClientPlayer(sp.objectID, getPlayerColor(sp.colorID));
                    remotePlayers.put(sp.objectID, player);
                }
            }
        }

        if (state.bullets != null) {
            for (SerializableBullet sb : state.bullets) {
                ClientBullet bullet = remoteBullets.get(sb.objectID);
                if (bullet == null) {
                    bullet = new ClientBullet(sb.objectID, 0,0);
                    remoteBullets.put(sb.objectID, bullet);
                }
                bullet.updateFromServer(sb.pos_x, sb.pos_y);
            }
        }

        if (state.targets != null) {
            for (SerializableTarget st : state.targets) {
                ClientTarget target = remoteTargets.get(st.objectID);
                if (target == null) {
                    target = new ClientTarget(st.objectID, 0,0);
                    remoteTargets.put(st.objectID, target);
                }
                target.updateFromServer(st.pos_x, st.pos_y);
            }
        }

        updatePlayerPanels(state.players);
    }

    private void cleanupRemovedObjects(SerializableGameState newState) {

        Set<Integer> objects = new HashSet<>();

        if (newState.players != null) {
            newState.players.forEach(sp -> objects.add(sp.objectID));
        }
        if (newState.bullets != null) {
            newState.bullets.forEach(sb -> objects.add(sb.objectID));
        }
        if (newState.targets != null) {
            newState.targets.forEach(st -> objects.add(st.objectID));
        }

        remotePlayers.keySet().removeIf(id -> !objects.contains(id));
        remoteBullets.keySet().removeIf(id -> !objects.contains(id));
        remoteTargets.keySet().removeIf(id -> !objects.contains(id));
    }

    private void sendLoop() {
        while (running) {
            if (connected && myPlayerId != -1 && pendingAction != null) {
                String action;
                synchronized (actionLock) {
                    action = pendingAction;
                    pendingAction = null;
                }
                if (action != null) {
                    try {
                        oos.writeUTF(action);
                        oos.flush();
                    } catch (IOException e) {
                        System.out.println("[CLIENT] Send error: " + e.getMessage());
                    }
                }
            }
//            try {
//                Thread.sleep(16);
//            } catch (InterruptedException e) {
//                break;
//            }
        }
    }

    private void sendAction(Request request) {
        if (!connected) return;
        String json = gson.toJson(request);
        synchronized (actionLock) {
            pendingAction = json;
        }
    }

    @FXML
    protected void onKeyPressed(KeyEvent event) {
        if (!connected) return;
        KeyCode code = event.getCode();
        switch (code) {
            case W      -> sendAction(requestHash.get(RequestType.MOVE_UP));
            case S      -> sendAction(requestHash.get(RequestType.MOVE_DOWN));
            case D      -> sendAction(requestHash.get(RequestType.SHOOT));
            case ESCAPE -> {
                if (isRunning)
                    sendAction(requestHash.get(RequestType.PAUSE));
                else
                    sendAction(requestHash.get(RequestType.RESUME));
                isRunning = !isRunning;
            }
        }
    }

    @FXML
    protected void onEnterBtnClick() {
        String name = textField.getText().trim();

        if (name.isEmpty()) {
            warningLabel.setText("Name cannot be empty!");
            warningLabel.setVisible(true);
            return;
        }

        if (name.length() < 3) {
            warningLabel.setText("Name must be at least 3 characters!");
            warningLabel.setVisible(true);
            return;
        }

        sendAction(new Request(RequestType.SET_NAME, name, myPlayerId));
        warningLabel.setText("Checking name...");
        warningLabel.setTextFill(Color.YELLOW);
        warningLabel.setVisible(true);
        textField.setDisable(true);
    }

    @FXML
    protected void onReadyBtnClick() {
        if (connected) {
            isReady = !isReady;
            sendAction(new Request(RequestType.START, "", myPlayerId));
            if (readyButton != null) {
                readyButton.setText(isReady ? "CANCEL" : "READY");
                readyButton.setStyle(isReady ?
                        "-fx-background-color: #ff9800;" :
                        "-fx-background-color: #4CAF50;");
            }
        }
    }

    @FXML
    protected void onStartBtnClick() {
        onReadyBtnClick();
    }

    @FXML
    protected void onPauseBtnClick() {
        if (connected) {
            sendAction(requestHash.get(RequestType.PAUSE));
        }
    }

    @FXML
    protected void onResumeBtnClick() {
        if (connected) {
            sendAction(requestHash.get(RequestType.RESUME));
        }
    }

    @FXML
    protected void onLeaderboardBtnClick() {
//        if (connected) {
//            sendAction(new Request(RequestType.GET_LEADERBOARD, "", myPlayerId));
//        }
    }

    @FXML
    protected void onExitBtnClick() {
        running = false;
        connected = false;

        try {
            if (socket != null && !socket.isClosed()) {
                sendAction(requestHash.get(RequestType.DISCONNECT));
                socket.close();
            }
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }

        while (!senderThread.isInterrupted()) {
            senderThread.interrupt();
        }
        while (!readerThread.isInterrupted()) {
            readerThread.interrupt();
        }

        Platform.exit();
    }

//    private void showError(String message) {
//        Platform.runLater(() -> {
//            Label errorLabel = new Label(message);
//            errorLabel.setLayoutX(20);
//            errorLabel.setLayoutY(150);
//            errorLabel.setTextFill(Color.RED);
//            errorLabel.setStyle("-fx-font-size: 14; -fx-background-color: rgba(0,0,0,0.7); -fx-padding: 10;");
//            Root.getChildren().add(errorLabel);
//
//            new Thread(() -> {
//                try {
//                    Thread.sleep(3000);
//                    Platform.runLater(() -> Root.getChildren().remove(errorLabel));
//                } catch (InterruptedException e) {
//                    Thread.currentThread().interrupt();
//                }
//            }).start();
//        });
//    }

    private void showLeaderboardWindow(List<PlayerStats> leaderboard) {
        Platform.runLater(() -> {
            javafx.stage.Stage stage = new javafx.stage.Stage();
            stage.setTitle("Leaderboard");
            stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);

            VBox root = new VBox(10);
            root.setStyle("-fx-padding: 20; -fx-background-color: #1a1a2e;");

            Label title = new Label("🏆 LEADERBOARD 🏆");
            title.setStyle("-fx-font-size: 24; -fx-font-weight: bold; -fx-text-fill: gold;");

            // Заголовки
            javafx.scene.layout.GridPane grid = new javafx.scene.layout.GridPane();
            grid.setHgap(20);
            grid.setVgap(5);
            grid.setStyle("-fx-padding: 10;");

            String[] headers = {"#", "Player", "Wins", "Shots", "Hits", "Accuracy"};
            for (int i = 0; i < headers.length; i++) {
                Label header = new Label(headers[i]);
                header.setStyle("-fx-text-fill: #4CAF50; -fx-font-weight: bold; -fx-font-size: 14;");
                grid.add(header, i, 0);
            }

            int rank = 1;
            for (PlayerStats stats : leaderboard) {
                int row = rank;
                Label rankLabel = new Label(String.valueOf(rank++));
                rankLabel.setStyle("-fx-text-fill: #ccc;");
                Label nameLabel = new Label(stats.getPlayerName());
                nameLabel.setStyle("-fx-text-fill: #fff;");
                Label winsLabel = new Label(String.valueOf(stats.getWins()));
                winsLabel.setStyle("-fx-text-fill: gold; -fx-font-weight: bold;");
                Label shotsLabel = new Label(String.valueOf(stats.getTotalShots()));
                shotsLabel.setStyle("-fx-text-fill: #ccc;");
                Label hitsLabel = new Label(String.valueOf(stats.getTotalHits()));
                hitsLabel.setStyle("-fx-text-fill: #ccc;");
                Label accuracyLabel = new Label(String.format("%.1f%%", stats.getAccuracy()));
                accuracyLabel.setStyle("-fx-text-fill: #4CAF50;");

                grid.add(rankLabel, 0, row);
                grid.add(nameLabel, 1, row);
                grid.add(winsLabel, 2, row);
                grid.add(shotsLabel, 3, row);
                grid.add(hitsLabel, 4, row);
                grid.add(accuracyLabel, 5, row);
            }

            javafx.scene.control.ScrollPane scrollPane = new javafx.scene.control.ScrollPane(grid);
            scrollPane.setFitToWidth(true);
            scrollPane.setStyle("-fx-background: transparent;");

            Button closeBtn = new Button("Close");
            closeBtn.setStyle("-fx-background-color: #f44336; -fx-text-fill: white; -fx-font-size: 14;");
            closeBtn.setOnAction(e -> stage.close());

            root.getChildren().addAll(title, scrollPane, closeBtn);

            javafx.scene.Scene scene = new javafx.scene.Scene(root, 600, 400);
            stage.setScene(scene);
            stage.show();
        });
    }
}
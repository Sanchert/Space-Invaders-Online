package org.example.space_invaders_online.game.client;

import com.google.gson.Gson;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;
import org.example.space_invaders_online.game.server.ServerMessage;

import java.io.*;
import java.net.Socket;
import java.util.*;


public class GameClientController extends AnchorPane {
//    private static final String SERVER_HOST = "localhost";
//    private static final int SERVER_PORT = 12345;
//    @FXML
//    private VBox playerListPanel;
//
//    private final Map<Integer, PlayerInfoPanel> playerInfoPanels = new HashMap<>();
//    private enum ClientState {
//        WAITING_FOR_NAME,
//        GAME_RUNNING
//    }
//
//    private ClientState clientState = ClientState.WAITING_FOR_NAME;
//
//    @FXML
//    private AnchorPane Root;
//    @FXML
//    private AnchorPane startWindow;
//    @FXML
//    private TextField textField;
//    @FXML
//    private Label warningLabel;
//    @FXML
//    private Label winLabel;
//
//    private Socket socket;
//    private ObjectOutputStream oos;
//    private ObjectInputStream ois;
//    private volatile boolean connected = false;
//
//    private final Gson json = new Gson();
//
//    private final Map<Integer, RemotePlayer> remotePlayers = new HashMap<>();
//    private final Map<Integer, RemoteBullet> remoteBullets = new HashMap<>();
//    private final Map<Integer, RemoteTarget> remoteTargets = new HashMap<>();
//
//    private int myPlayerId = -1;
//    private volatile boolean running = true;
//
//    private boolean dPressed = false;
//
//    private final Object actionLock = new Object();
//    private String pendingAction = null;
//
//    @FXML
//    public void initialize() {
//        setFocusTraversable(true);
//        requestFocus();
//
//        connectToServer();
//
//        Thread senderThread = new Thread(this::sendLoop);
//        senderThread.start();
//    }
//
//    private void connectToServer() {
//        try {
//            socket = new Socket(SERVER_HOST, SERVER_PORT);
//            oos = new ObjectOutputStream(socket.getOutputStream());
//            ois = new ObjectInputStream(socket.getInputStream());
//            connected = true;
//            System.out.println("[CONNECTION SUCCESS] connected to server");
//            Thread readerThread = new Thread(this::readLoop);
//            readerThread.start();
//
//        } catch (IOException e) {
//            System.out.println(e.getMessage());
//            showError("[CONNECTION ERROR] can't connect to server");
//        }
//    }
//
//    private void readLoop() {
//        try {
//            while (connected) {
//                String line = ois.readUTF();
//                ServerMessage message = json.fromJson(line, ServerMessage.class);
//                Platform.runLater(() -> handleServerMessage(message));
//            }
//        } catch (IOException e) {
//            Platform.runLater(() -> showError("[CONNECTION ERROR] Connection lost"));
//        } finally {
//            connected = false;
//        }
//    }
//
//    private void handleServerMessage(ServerMessage message) {
//        System.out.println(message.type);
//        switch (message.type) {
//            case INIT:
//                myPlayerId = message.playerId;
//                updatePlayerPanels(message.currentGameState);
//                System.out.println("My ID: " + myPlayerId);
//                break;
//
//            case NAME_ACCEPTED:
//                Platform.runLater(() -> {
//                    warningLabel.setText("Name accepted! Press START to play");
//                    warningLabel.setTextFill(Color.GREEN);
//                    textField.setDisable(false);
//                });
//                startWindow.setDisable(true);
//                startWindow.setVisible(false);
//                clientState = ClientState.GAME_RUNNING;
//                break;
//
//            case NAME_REJECTED:
//                clientState = ClientState.WAITING_FOR_NAME;
//                Platform.runLater(() -> {
//                    warningLabel.setTextFill(Color.RED);
//                    warningLabel.setVisible(true);
//                    textField.setDisable(false);
//                    textField.clear();
//                    textField.requestFocus();
//                });
//                break;
//
//            case STATE_UPDATE:
//                updatePlayerPanels(message.currentGameState);
//                if (clientState == ClientState.GAME_RUNNING) {
//                    updateGameObjects(message.currentGameState);
//                }
//                break;
//            case WIN:
//                System.out.println(message.args);
//                winLabel.setText("Winner: " + message.args);
//                winLabel.setVisible(true);
//                winLabel.setDisable(false);
//                clientState = ClientState.WAITING_FOR_NAME;
//        }
//    }
//
//private void updatePlayerPanels(SerializableGameState state) {
//    if (playerListPanel == null) return;
//
//    Platform.runLater(() -> {
//        Set<Integer> currentIds = new HashSet<>();
//
//
//        for (PlayerInfoPanel panel : playerInfoPanels.values()) {
//            panel.setDisconnected(true);
//        }
//
//        for (SerializablePlayer sp : state.players) {
//            currentIds.add(sp.id);
//
//            PlayerInfoPanel panel = playerInfoPanels.get(sp.id);
//            String displayName = (sp.name != null && !sp.name.isEmpty()) ? sp.name : "Player " + sp.id;
//
//            if (panel == null) {
//                panel = new PlayerInfoPanel(sp.id, displayName, sp.shoots, sp.colorId);
//                playerInfoPanels.put(sp.id, panel);
//                playerListPanel.getChildren().add(panel);
//            } else {
//                panel.updateName(displayName);
//                panel.updateShoots(sp.shoots);
//                panel.updateScore(sp.score);
//                panel.setDisconnected(false);
//            }
//        }
//    });
//}
//
//    private void updateGameObjects(SerializableGameState state) {
//        cleanupObjects(state);
//        for (SerializablePlayer sp : state.players) {
//            RemotePlayer player = remotePlayers.get(sp.id);
//            if (player == null) {
//                Color color = getPlayerColor(sp.colorId);
//                player = new RemotePlayer(sp.id, color);
//                remotePlayers.put(sp.id, player);
//                Root.getChildren().add(player.sprite);
//            }
//            player.updateFromServer(sp.x, sp.y, sp.shoots, sp.name);
//        }
//
//        for (SerializableBullet sb : state.bullets) {
//            RemoteBullet bullet = remoteBullets.get(sb.id);
//            if (bullet == null) {
//                bullet = new RemoteBullet(sb.id);
//                remoteBullets.put(sb.id, bullet);
//                Root.getChildren().add(bullet.sprite);
//            }
//            bullet.updateFromServer(sb.x, sb.y);
//        }
//
//        for (SerializableTarget st : state.targets) {
//            if (st.alive) {
//                RemoteTarget target = remoteTargets.get(st.id);
//                if (target == null) {
//                    target = new RemoteTarget(st.id);
//                    remoteTargets.put(st.id, target);
//                    Root.getChildren().add(target.sprite);
//                }
//                target.updateFromServer(st.x, st.y);
//            }
//        }
//    }
//
//    //TODO: flag indicating whether objects have been deleted && id of deleted objects
//    private void cleanupObjects(SerializableGameState newState) {
//
//        Set<Integer> currentPlayerIds = new HashSet<>();
//        newState.players.forEach(sp -> currentPlayerIds.add(sp.id));
//
//        remotePlayers.keySet().removeIf(id -> {
//            if (!currentPlayerIds.contains(id)) {
//                RemotePlayer p = remotePlayers.get(id);
//                Root.getChildren().remove(p.sprite);
//                return true;
//            }
//            return false;
//        });
//
//        Set<Integer> currentBulletIds = new HashSet<>();
//        for (SerializableBullet sb : newState.bullets) {
//            currentBulletIds.add(sb.id);
//        }
//
//        remoteBullets.keySet().removeIf(id -> {
//            if (!currentBulletIds.contains(id)) {
//                RemoteBullet b = remoteBullets.get(id);
//                Root.getChildren().remove(b.sprite);
//                return true;
//            }
//            return false;
//        });
//
//        Set<Integer> currentTargetIds = new HashSet<>();
//        for (SerializableTarget st : newState.targets) {
//            if (st.alive) {
//                currentTargetIds.add(st.id);
//            }
//        }
//
//        remoteTargets.keySet().removeIf(id -> {
//            if (!currentTargetIds.contains(id)) {
//                RemoteTarget t = remoteTargets.get(id);
//                Root.getChildren().remove(t.sprite);
//                return true;
//            }
//            return false;
//        });
//    }
//
//    private Color getPlayerColor(int colorId) {
//        return switch (colorId) {
//            case 0 -> Color.BLUE;
//            case 1 -> Color.RED;
//            case 2 -> Color.GREEN;
//            case 3 -> Color.ORANGE;
//            default -> Color.PURPLE;
//        };
//    }
//
//    private void sendLoop() {
//        while (running) {
//            if (connected && myPlayerId != -1) {
//                String action = null;
//                synchronized (actionLock) {
//                    if (pendingAction != null) {
//                        action = pendingAction;
//                        pendingAction = null;
//                    }
//                }
//                if (action != null) {
//                    try {
//                        System.out.println(action);
//                        oos.writeUTF(action);
//                        oos.flush();
//                    } catch (IOException e) {
//                        System.out.println(e.getMessage());
//                        continue;
//                    }
//                }
//            }
//
//            try {
//                Thread.sleep(16);
//            } catch (InterruptedException e) {
//                break;
//            }
//        }
//    }
//
//    private void sendAction(Request request) {
//        if (!connected){
//            return;
//        }
//        String str = json.toJson(request);
//        synchronized (actionLock) {
//            pendingAction = str;
//        }
//    }
//
//    @FXML
//    protected void onKeyPressed(KeyEvent event) {
//        if (!connected) return;
//        KeyCode code = event.getCode();
//        if (code == KeyCode.W) {
//            sendAction(new Request(RequestType.MOVE_UP, "", myPlayerId));
//        } else if (code == KeyCode.S) {
//            sendAction(new Request(RequestType.MOVE_DOWN, "", myPlayerId));
//        } else if (code == KeyCode.D && !dPressed) {
//            dPressed = true;
//            sendAction(new Request(RequestType.SHOOT, "", myPlayerId));
//        }
//    }
//    @FXML
//    protected void onKeyReleased(KeyEvent event) {
//        if (!connected) return;
//        KeyCode code = event.getCode();
//        if (code == KeyCode.D) {
//            dPressed = false;
//        }
//    }
//    @FXML
//    protected void onEnterBtnClick() {
//        String name = textField.getText().trim();
//
//        if (name.isEmpty()) {
//            warningLabel.setText("Name cannot be empty!");
//            warningLabel.setVisible(true);
//            return;
//        }
//
//
//        sendAction(new Request(RequestType.SET_NAME, name, myPlayerId));
//        warningLabel.setText("Checking name...");
//        warningLabel.setVisible(true);
//
//        textField.setDisable(true);
//    }
//    @FXML
//    protected void onStartBtnClick() {
//        if (connected) {
//            sendAction(new Request(RequestType.START, "", myPlayerId));
//        }
//    }
//    @FXML
//    protected void onPauseBtnClick() {
//        if (connected) {
//            sendAction(new Request(RequestType.PAUSE, "", myPlayerId));
//        }
//    }
//    @FXML
//    protected void onResumeBtnClick() {
//        if (connected) {
//            sendAction(new Request(RequestType.START, "", myPlayerId));
//        }
//    }
//    @FXML
//    protected void onExitBtnClick() {
//        running = false;
//        connected = false;
//        try {
//            if (socket != null) {
//                sendAction(new Request(RequestType.DISCONNECT, "", myPlayerId));
//                socket.close();
//            }
//        } catch (IOException e) {
//            System.out.println(e.getMessage());
//        }
//        Platform.exit();
//    }
//
//    private void showError(String message) {
//        Label errorLabel = new Label(message);
//        errorLabel.setLayoutX(20);
//        errorLabel.setLayoutY(150);
//        errorLabel.setTextFill(Color.ORANGERED);
//        errorLabel.setStyle("-fx-font-size: 15;");
//        Root.getChildren().add(errorLabel);
//    }
}

//TODO: common?
class RemotePlayer {
    Shape sprite;
    int id;
    float x, y;
    int shoots;
    String name;

    public RemotePlayer(int id, Color color) {
        this.id = id;
        Rectangle rect = new Rectangle(30, 50);
        rect.setFill(color);
        rect.setStroke(Color.BLACK);
        rect.setArcHeight(5);
        rect.setArcWidth(5);
        this.sprite = rect;
    }

    public void updateFromServer(float x, float y, int shoots, String name) {
        this.x = x;
        this.y = y;
        this.shoots = shoots;
        this.name = name;
        render();
    }

    public void render() {
        sprite.setLayoutX(x);
        sprite.setLayoutY(y);
    }
}

class RemoteBullet {
    Rectangle sprite;
    int id;
    float x, y;

    public RemoteBullet(int id) {
        this.id = id;
        this.sprite = new Rectangle(10, 10);
        sprite.setFill(Color.YELLOW);
        sprite.setArcHeight(5);
        sprite.setArcWidth(5);
    }

    public void updateFromServer(float x, float y) {
        this.x = x;
        this.y = y;
        render();
    }

    public void render() {
        sprite.setLayoutX(x);
        sprite.setLayoutY(y);
    }
}

class RemoteTarget {
    Circle sprite;
    int id;
    float x, y;

    public RemoteTarget(int id) {
        this.id = id;
        this.sprite = new Circle(15);
        sprite.setFill(Color.RED);
        sprite.setStroke(Color.BLACK);
    }

    public void updateFromServer(float x, float y) {
        this.x = x;
        this.y = y;
        render();
    }

    public void render() {
        sprite.setLayoutX(x);
        sprite.setLayoutY(y);
    }
}

class PlayerInfoPanel extends AnchorPane {
    private Label nameLabel;
    private Label scoreLabel;
    private Label shootsLabel;
    private int playerId;
    private String currentName;
    private boolean disconnected = false;

    public PlayerInfoPanel(int playerId, String name, int shoots, int colorId) {
        this.playerId = playerId;

        setPrefHeight(50);
        setPrefWidth(160);
        setStyle("-fx-border-color: black; -fx-border-width: 1; -fx-padding: 5; -fx-background-color: #f0f0f0;");

        Rectangle colorRect = new Rectangle(10, 40);
        colorRect.setFill(getPlayerColor(colorId));
        colorRect.setLayoutX(5);
        colorRect.setLayoutY(5);

        nameLabel = new Label(name);
        nameLabel.setLayoutX(20);
        nameLabel.setLayoutY(5);
        nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 11;");
        nameLabel.setPrefWidth(130);

        Label scoreTitle = new Label("Score:");
        scoreTitle.setLayoutX(20);
        scoreTitle.setLayoutY(25);
        scoreTitle.setStyle("-fx-font-size: 10;");

        scoreLabel = new Label("0");
        scoreLabel.setLayoutX(60);
        scoreLabel.setLayoutY(25);
        scoreLabel.setStyle("-fx-font-size: 10; -fx-font-weight: bold;");

        Label shootsTitle = new Label("Shots:");
        shootsTitle.setLayoutX(90);
        shootsTitle.setLayoutY(25);
        shootsTitle.setStyle("-fx-font-size: 10;");

        shootsLabel = new Label(String.valueOf(shoots));
        shootsLabel.setLayoutX(130);
        shootsLabel.setLayoutY(25);
        shootsLabel.setStyle("-fx-font-size: 10; -fx-font-weight: bold;");

        getChildren().addAll(colorRect, nameLabel, scoreTitle, scoreLabel, shootsTitle, shootsLabel);
    }

    public void setDisconnected(boolean disconnected) {
        this.disconnected = disconnected;
        if (disconnected) {
            nameLabel.setText(currentName + " [disconnected]");
            nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 11; -fx-text-fill: gray;");
            this.setStyle("-fx-border-color: gray; -fx-border-width: 1; -fx-padding: 5; -fx-background-color: #e0e0e0;");
        } else {
            nameLabel.setText(currentName);
            nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 11; -fx-text-fill: black;");
            this.setStyle("-fx-border-color: black; -fx-border-width: 1; -fx-padding: 5; -fx-background-color: #f0f0f0;");
        }
    }


    public void updateName(String name) {
        this.currentName = name;
        if (disconnected) {
            nameLabel.setText(name + " [disconnected]");
        } else {
            nameLabel.setText(name);
        }
    }

    public void updateScore(int score) {
        scoreLabel.setText(String.valueOf(score));
    }
    public void updateShoots(int shoots) {
        shootsLabel.setText(String.valueOf(shoots));
    }

    private Color getPlayerColor(int colorId) {
        return switch (colorId) {
            case 0 -> Color.BLUE;
            case 1 -> Color.RED;
            case 2 -> Color.GREEN;
            case 3 -> Color.ORANGE;
            default -> Color.PURPLE;
        };
    }
}
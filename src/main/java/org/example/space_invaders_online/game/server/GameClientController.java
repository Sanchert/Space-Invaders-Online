// src/main/java/org/example/space_invaders_online/game/client/GameClientController.java
package org.example.space_invaders_online.game.client;

import com.google.gson.Gson;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import org.example.space_invaders_online.game.server.*;
import org.example.space_invaders_online.game.database.PlayerStats;

import java.io.*;
import java.net.Socket;
import java.util.*;

public class GameClientController extends AnchorPane {
    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 12345;

    @FXML
    private VBox playerListPanel;

    @FXML
    private AnchorPane Root;

    @FXML
    private AnchorPane startWindow;

    @FXML
    private TextField textField;

    @FXML
    private Label warningLabel;

    @FXML
    private Label winLabel;

    @FXML
    private Button startBtn;

    @FXML
    private Button pauseBtn;

    @FXML
    private Button resumeBtn;

    @FXML
    private Button exitBtn;

    @FXML
    private Button leaderboardBtn;

    private final Map<Integer, PlayerInfoPanel> playerInfoPanels = new HashMap<>();

    private enum ClientState {
        WAITING_FOR_NAME,
        IN_LOBBY,
        GAME_RUNNING,
        GAME_PAUSED,
        GAME_OVER,
        VIEWING_LEADERBOARD
    }

    private ClientState clientState = ClientState.WAITING_FOR_NAME;

    private Socket socket;
    private ObjectOutputStream oos;
    private ObjectInputStream ois;
    private volatile boolean connected = false;

    private final Gson json = new Gson();

    private final Map<Integer, ClientPlayer> remotePlayers = new HashMap<>();
    private final Map<Integer, ClientBullet> remoteBullets = new HashMap<>();
    private final Map<Integer, ClientTarget> remoteTargets = new HashMap<>();

    private int myPlayerId = -1;
    private volatile boolean running = true;
    private boolean isReady = false;
    private boolean isPaused = false;

    private boolean wPressed = false;
    private boolean sPressed = false;
    private boolean dPressed = false;

    private final Object actionLock = new Object();
    private String pendingAction = null;

    private LeaderboardPopup leaderboardPopup;

    @FXML
    public void initialize() {
        setFocusTraversable(true);
        requestFocus();

        connectToServer();

        Thread senderThread = new Thread(this::sendLoop);
        senderThread.setDaemon(true);
        senderThread.start();

        // Настройка обработки клавиш
        Root.setOnKeyPressed(this::onKeyPressed);
        Root.setOnKeyReleased(this::onKeyReleased);

        // Создаём попап для таблицы лидеров
        leaderboardPopup = new LeaderboardPopup();
    }

    private void connectToServer() {
        try {
            socket = new Socket(SERVER_HOST, SERVER_PORT);
            oos = new ObjectOutputStream(socket.getOutputStream());
            ois = new ObjectInputStream(socket.getInputStream());
            connected = true;
            System.out.println("[CONNECTION SUCCESS] connected to server");
            Thread readerThread = new Thread(this::readLoop);
            readerThread.setDaemon(true);
            readerThread.start();
        } catch (IOException e) {
            System.out.println(e.getMessage());
            showError("[CONNECTION ERROR] can't connect to server");
        }
    }

    private void readLoop() {
        try {
            while (connected) {
                String line = ois.readUTF();
                ServerMessage message = json.fromJson(line, ServerMessage.class);
                Platform.runLater(() -> handleServerMessage(message));
            }
        } catch (IOException e) {
            Platform.runLater(() -> showError("[CONNECTION ERROR] Connection lost"));
        } finally {
            connected = false;
        }
    }

    private void handleServerMessage(ServerMessage message) {
        System.out.println("Received: " + message.type);

        switch (message.type) {
            case INIT:
                myPlayerId = message.playerId;
                System.out.println("My ID: " + myPlayerId);
                break;

            case NAME_ACCEPTED:
                Platform.runLater(() -> {
                    warningLabel.setText("Name accepted! Press READY to play");
                    warningLabel.setTextFill(Color.GREEN);
                    textField.setDisable(true);
                    startWindow.setVisible(false);
                    clientState = ClientState.IN_LOBBY;
                });
                break;

            case NAME_REJECTED:
                clientState = ClientState.WAITING_FOR_NAME;
                Platform.runLater(() -> {
                    warningLabel.setTextFill(Color.RED);
                    warningLabel.setText("Name already taken or invalid!");
                    warningLabel.setVisible(true);
                    textField.setDisable(false);
                    textField.clear();
                    textField.requestFocus();
                });
                break;

            case PLAYER_LIST_UPDATE:
                updatePlayerPanels(message.players);
                break;

            case GAME_START:
                Platform.runLater(() -> {
                    clientState = ClientState.GAME_RUNNING;
                    isReady = false;
                    winLabel.setVisible(false);
                    startWindow.setVisible(false);
                    requestFocus();
                });
                break;

            case GAME_PAUSED:
                Platform.runLater(() -> {
                    clientState = ClientState.GAME_PAUSED;
                    showPauseOverlay();
                });
                break;

            case GAME_RESUMED:
                Platform.runLater(() -> {
                    clientState = ClientState.GAME_RUNNING;
                    hidePauseOverlay();
                });
                break;

            case STATE_UPDATE:
                if (clientState == ClientState.GAME_RUNNING) {
                    updateGameObjects(message.currentGameState);
                }
                break;

            case WIN:
                System.out.println("Winner: " + message.args);
                Platform.runLater(() -> {
                    winLabel.setText("Winner: " + message.args);
                    winLabel.setVisible(true);
                    clientState = ClientState.GAME_OVER;
                });
                break;

            case LEADERBOARD:
                showLeaderboard(message.leaderboard);
                break;
        }
    }

    private void updatePlayerPanels(List<DTOPlayer> players) {
        if (playerListPanel == null) return;

        Platform.runLater(() -> {
            Set<Integer> currentIds = new HashSet<>();

            // Помечаем всех как отключённых
            for (PlayerInfoPanel panel : playerInfoPanels.values()) {
                panel.setDisconnected(true);
            }

            if (players != null) {
                for (DTOPlayer sp : players) {
                    currentIds.add(sp.id);

                    PlayerInfoPanel panel = playerInfoPanels.get(sp.id);
                    String displayName = (sp.name != null && !sp.name.isEmpty()) ? sp.name : "Player " + sp.id;

                    if (panel == null) {
                        panel = new PlayerInfoPanel(sp.id, displayName, sp.shoots, sp.colorId);
                        playerInfoPanels.put(sp.id, panel);
                        playerListPanel.getChildren().add(panel);
                    } else {
                        panel.updateName(displayName);
                        panel.updateShoots(sp.shoots);
                        panel.updateScore(sp.score);
                        panel.setDisconnected(false);
                    }

                    // Подсвечиваем текущего игрока
                    if (sp.id == myPlayerId) {
                        panel.setCurrentPlayer(true);
                    } else {
                        panel.setCurrentPlayer(false);
                    }
                }
            }

            // Удаляем отключённых игроков
            playerInfoPanels.entrySet().removeIf(entry -> {
                if (!currentIds.contains(entry.getKey())) {
                    playerListPanel.getChildren().remove(entry.getValue());
                    return true;
                }
                return false;
            });
        });
    }

    private void updateGameObjects(DTOGameState state) {
        if (state == null) return;

        cleanupObjects(state);

        if (state.players != null) {
            for (DTOPlayer sp : state.players) {
                ClientPlayer player = remotePlayers.get(sp.id);
                if (player == null) {
                    Color color = getPlayerColor(sp.colorId);
                    player = new ClientPlayer(sp.id, color);
                    remotePlayers.put(sp.id, player);
                    Root.getChildren().add(player.sprite);
                }
                player.updateFromServer(sp.x, sp.y, sp.shoots, sp.name);
            }
        }

        if (state.bullets != null) {
            for (DTOBullet sb : state.bullets) {
                ClientBullet bullet = remoteBullets.get(sb.id);
                if (bullet == null) {
                    bullet = new ClientBullet(sb.id);
                    remoteBullets.put(sb.id, bullet);
                    Root.getChildren().add(bullet.sprite);
                }
                bullet.updateFromServer(sb.x, sb.y);
            }
        }

        if (state.targets != null) {
            for (DTOTarget st : state.targets) {
                if (st.alive) {
                    ClientTarget target = remoteTargets.get(st.id);
                    if (target == null) {
                        target = new ClientTarget(st.id);
                        remoteTargets.put(st.id, target);
                        Root.getChildren().add(target.sprite);
                    }
                    target.updateFromServer(st.x, st.y);
                }
            }
        }
    }

    private void cleanupObjects(DTOGameState newState) {
        if (newState == null) return;

        Set<Integer> currentPlayerIds = new HashSet<>();
        if (newState.players != null) {
            newState.players.forEach(sp -> currentPlayerIds.add(sp.id));
        }

        remotePlayers.keySet().removeIf(id -> {
            if (!currentPlayerIds.contains(id)) {
                ClientPlayer p = remotePlayers.get(id);
                Root.getChildren().remove(p.sprite);
                return true;
            }
            return false;
        });

        Set<Integer> currentBulletIds = new HashSet<>();
        if (newState.bullets != null) {
            for (DTOBullet sb : newState.bullets) {
                currentBulletIds.add(sb.id);
            }
        }

        remoteBullets.keySet().removeIf(id -> {
            if (!currentBulletIds.contains(id)) {
                ClientBullet b = remoteBullets.get(id);
                Root.getChildren().remove(b.sprite);
                return true;
            }
            return false;
        });

        Set<Integer> currentTargetIds = new HashSet<>();
        if (newState.targets != null) {
            for (DTOTarget st : newState.targets) {
                if (st.alive) {
                    currentTargetIds.add(st.id);
                }
            }
        }

        remoteTargets.keySet().removeIf(id -> {
            if (!currentTargetIds.contains(id)) {
                ClientTarget t = remoteTargets.get(id);
                Root.getChildren().remove(t.sprite);
                return true;
            }
            return false;
        });
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

    private void sendLoop() {
        while (running) {
            if (connected && myPlayerId != -1) {
                String action = null;
                synchronized (actionLock) {
                    if (pendingAction != null) {
                        action = pendingAction;
                        pendingAction = null;
                    }
                }
                if (action != null) {
                    try {
                        oos.writeUTF(action);
                        oos.flush();
                    } catch (IOException e) {
                        System.out.println(e.getMessage());
                    }
                }
            }

            try {
                Thread.sleep(16);
            } catch (InterruptedException e) {
                break;
            }
        }
    }

    private void sendAction(Request request) {
        if (!connected) return;
        String str = json.toJson(request);
        synchronized (actionLock) {
            pendingAction = str;
        }
    }

    @FXML
    protected void onKeyPressed(KeyEvent event) {
        if (!connected) return;

        KeyCode code = event.getCode();

        if (clientState == ClientState.GAME_RUNNING) {
            if (code == KeyCode.W && !wPressed) {
                wPressed = true;
                sendAction(new Request(RequestType.MOVE_UP, "", myPlayerId));
            } else if (code == KeyCode.S && !sPressed) {
                sPressed = true;
                sendAction(new Request(RequestType.MOVE_DOWN, "", myPlayerId));
            } else if (code == KeyCode.D && !dPressed) {
                dPressed = true;
                sendAction(new Request(RequestType.SHOOT, "", myPlayerId));
            } else if (code == KeyCode.ESCAPE) {
                sendAction(new Request(RequestType.PAUSE, "", myPlayerId));
            }
        } else if (clientState == ClientState.GAME_PAUSED && code == KeyCode.ESCAPE) {
            sendAction(new Request(RequestType.RESUME, "", myPlayerId));
        } else if (clientState == ClientState.VIEWING_LEADERBOARD && code == KeyCode.ESCAPE) {
            leaderboardPopup.hide();
            clientState = ClientState.IN_LOBBY;
        }
    }

    @FXML
    protected void onKeyReleased(KeyEvent event) {
        if (!connected) return;

        KeyCode code = event.getCode();
        if (code == KeyCode.W) {
            wPressed = false;
        } else if (code == KeyCode.S) {
            sPressed = false;
        } else if (code == KeyCode.D) {
            dPressed = false;
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
        warningLabel.setVisible(true);
        textField.setDisable(true);
    }

    @FXML
    protected void onReadyBtnClick() {
        if (connected && clientState == ClientState.IN_LOBBY) {
            isReady = !isReady;
            sendAction(new Request(RequestType.START, "", myPlayerId));
            if (isReady) {
                startBtn.setText("Cancel Ready");
            } else {
                startBtn.setText("Ready");
            }
        }
    }

    @FXML
    protected void onPauseBtnClick() {
        if (connected && clientState == ClientState.GAME_RUNNING) {
            sendAction(new Request(RequestType.PAUSE, "", myPlayerId));
        }
    }

    @FXML
    protected void onResumeBtnClick() {
        if (connected && clientState == ClientState.GAME_PAUSED) {
            sendAction(new Request(RequestType.RESUME, "", myPlayerId));
        }
    }

    @FXML
    protected void onLeaderboardBtnClick() {
        if (connected && (clientState == ClientState.IN_LOBBY || clientState == ClientState.GAME_PAUSED)) {
            if (clientState == ClientState.GAME_RUNNING) {
                sendAction(new Request(RequestType.PAUSE, "", myPlayerId));
            }
            clientState = ClientState.VIEWING_LEADERBOARD;
            sendAction(new Request(RequestType.GET_LEADERBOARD, "", myPlayerId));
        }
    }

    @FXML
    protected void onExitBtnClick() {
        running = false;
        connected = false;
        try {
            if (socket != null) {
                sendAction(new Request(RequestType.DISCONNECT, "", myPlayerId));
                socket.close();
            }
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
        Platform.exit();
    }

    private void showError(String message) {
        Platform.runLater(() -> {
            Label errorLabel = new Label(message);
            errorLabel.setLayoutX(20);
            errorLabel.setLayoutY(150);
            errorLabel.setTextFill(Color.ORANGERED);
            errorLabel.setStyle("-fx-font-size: 15;");
            Root.getChildren().add(errorLabel);

            new Thread(() -> {
                try {
                    Thread.sleep(3000);
                    Platform.runLater(() -> Root.getChildren().remove(errorLabel));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }).start();
        });
    }

    private void showPauseOverlay() {
        // Создаём и показываем оверлей паузы
        System.out.println("Game paused");
    }

    private void hidePauseOverlay() {
        System.out.println("Game resumed");
    }

    private void showLeaderboard(List<PlayerStats> leaderboard) {
        Platform.runLater(() -> leaderboardPopup.show(leaderboard));
    }
}

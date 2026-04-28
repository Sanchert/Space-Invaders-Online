package org.example.space_invaders_online.game.sceneController.controllers;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.VBox;
import org.example.space_invaders_online.game.client.*;
import org.example.space_invaders_online.game.sceneController.GameContext;
import org.example.space_invaders_online.game.sceneController.ScreenManager;
import org.example.space_invaders_online.game.sceneController.ScreenType;
import org.example.space_invaders_online.game.server.DTOPlayer;
import org.example.space_invaders_online.game.server.ServerMessage;

import java.util.*;

public class MenuScreenController extends BaseController implements INetworkListener {
    // ===== TITLE SCREEN =====
    @FXML private Label gameTitle;
    @FXML private VBox splashContent;

    // ===== MAIN MENU SCREEN =====
    @FXML private VBox mainMenuContent;
    @FXML private Button singlePlayerBtn;
    @FXML private Button onlineGameBtn;
    @FXML private Button leaderboardBtn;
    @FXML private Button optionsBtn;
    @FXML private Button exitBtn;

    // ===== NAME INPUT SCREEN ====
    @FXML private VBox nameInputContent;
    @FXML private TextField nameField;
    @FXML private Button confirmNameBtn;
    @FXML private Button backToMenuBtn;

    // ===== LOBBY SCREEN ====
    @FXML private VBox lobbyContent;
    @FXML private VBox playersList;
    @FXML private Button readyBtn;
    @FXML private Button lobbyBackBtn;

    @FXML private Label errorLabel;

    private NetworkClient networkClient;
    private boolean isReady = false;


    private final Map<Integer, PlayerInfoPanel> lobbyPanels = new HashMap<>();

    public MenuScreenController(ScreenManager screenManager, GameContext gameContext) {
        super(screenManager, gameContext);
    }

    @FXML
    public void initialize() {
        singlePlayerBtn .setOnAction(e -> onSinglePlayer());
        onlineGameBtn   .setOnAction(e -> onOnlineGame());
        optionsBtn      .setOnAction(e -> onOptions());
        exitBtn         .setOnAction(e -> onExit());
        confirmNameBtn  .setOnAction(e -> onConfirmName());
        backToMenuBtn   .setOnAction(e -> showMainMenu());
        lobbyBackBtn    .setOnAction(e -> onLobbyBack());
        readyBtn        .setOnAction(e -> onReady());
        leaderboardBtn  .setOnAction(e -> onLeaderboard());
        Platform.runLater(() -> {
            if (screenManager.getStage().getScene() != null) {
                screenManager.getStage().getScene().addEventFilter(KeyEvent.KEY_PRESSED, this::onKeyPressed);
            }
        });

        showSplash();

        networkClient = gameContext.getNetworkClient();
        networkClient.setListener(this);

        try {
            networkClient.connect("localhost", 12345);
        } catch (Exception e) {
            showError("Cannot connect to server: " + e.getMessage());
        }
    }

    private void onLeaderboard() {
        networkClient.send(new Request(RequestType.PAUSE, "", gameContext.getMyPlayerId()));
        networkClient.send(new Request(RequestType.GET_LEADERBOARD, "", gameContext.getMyPlayerId()));
    }

    private void onKeyPressed(KeyEvent event) {
        if (splashContent.isVisible()) {
            showMainMenu();
            event.consume();
        }
    }

    private void showSplash() {
        gameTitle.setVisible(true);
        gameTitle.setManaged(true);
        splashContent.setVisible(true);
        splashContent.setManaged(true);
    }

    private void showMainMenu() {
        gameTitle.setVisible(true);
        gameTitle.setManaged(true);
        mainMenuContent.setVisible(true);
        mainMenuContent.setManaged(true);

        splashContent.setVisible(false);
        splashContent.setManaged(false);

        lobbyContent.setVisible(false);
        lobbyContent.setManaged(false);

        nameInputContent.setVisible(false);
        nameInputContent.setManaged(false);
    }

    private void showNameInput() {
        gameTitle.setVisible(false);
        gameTitle.setManaged(false);
        splashContent.setVisible(false);
        splashContent.setManaged(false);

        mainMenuContent.setVisible(false);
        mainMenuContent.setManaged(false);

        nameInputContent.setVisible(true);
        nameInputContent.setManaged(true);

        nameField.clear();
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);

        nameField.requestFocus();
    }

    private void showLobby() {
        lobbyContent.setVisible(true);
        lobbyContent.setManaged(true);

        gameTitle.setVisible(false);
        splashContent.setVisible(false);
        splashContent.setManaged(false);

        mainMenuContent.setVisible(false);
        mainMenuContent.setManaged(false);

        nameInputContent.setVisible(false);
        nameInputContent.setManaged(false);
    }

    private void onLobbyBack() {
        networkClient.disconnect(false);
        networkClient.setListener(null);
        gameContext.setNetworkClient(null);
        networkClient = null;
        gameContext.setMyPlayerId(-1);

        lobbyPanels.clear();
        playersList.getChildren().clear();
        showMainMenu();
    }

    private void onSinglePlayer() {}

    private void onOnlineGame() {
        showNameInput();
    }

    private void onOptions() {}

    private void onExit() {
        Platform.exit();
        System.exit(0);
    }

    private void onConfirmName() {
        String name = nameField.getText().trim();
        if (name.length() < 2) {
            showError("Name must be at least 2 characters");
            return;
        }
        gameContext.setPlayerName(name);
        if (networkClient.isConnected() && gameContext.getMyPlayerId() != -1) {
            networkClient.send(new Request(RequestType.SET_NAME, name,
                    gameContext.getMyPlayerId()));
        }
    }

    private void onReady() {
        isReady = !isReady;
        networkClient.send(new Request(RequestType.START, "", gameContext.getMyPlayerId()));

        readyBtn.setText(isReady ? "CANCEL" : "READY");
    }

    protected void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);

        new Thread(() -> {
            try {
                Thread.sleep(3000);
                Platform.runLater(() -> {
                    errorLabel.setVisible(false);
                    errorLabel.setManaged(false);
                });
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }

    @Override
    public void onInit(ServerMessage m) {
        gameContext.setMyPlayerId(m.playerId);
        String pendingName = gameContext.getPlayerName();
        if (pendingName != null && !pendingName.isEmpty()) {
            networkClient.send(new Request(RequestType.SET_NAME, pendingName,
                    gameContext.getMyPlayerId()));
        }
    }

    @Override
    public void onNameAccepted() {
        showLobby();
    }

    @Override
    public void onNameRejected() {
        networkClient.disconnect(false);
        gameContext.setMyPlayerId(-1);
        showNameInput();
        showError("Name already taken. Try another.");
    }

    @Override
    public void onPlayerListUpdate(ServerMessage m) {
        if (m.players == null) return;
        syncPlayerPanels(m.players);
    }

    private void syncPlayerPanels(List<DTOPlayer> players) {
        for (PlayerInfoPanel panel : lobbyPanels.values()) {
            panel.setDisconnected(true);
        }

        Set<Integer> activeIds = new HashSet<>();
        for (DTOPlayer dto : players) {
            activeIds.add(dto.objectID);

            String displayName = (dto.name != null && !dto.name.isEmpty())
                    ? dto.name
                    : "Player " + dto.objectID;

            PlayerInfoPanel panel = lobbyPanels.get(dto.objectID);
            if (panel == null) {
                // New player joined — create a panel
                panel = new PlayerInfoPanel(displayName, dto.shoots, dto.colorID);
                lobbyPanels.put(dto.objectID, panel);
                playersList.getChildren().add(panel);
            }

            panel.updateName(displayName);
            panel.updateScore(dto.score);
            panel.updateShoots(dto.shoots);
            panel.setDisconnected(false);
            panel.setCurrentPlayer(dto.objectID == gameContext.getMyPlayerId());
        }

        lobbyPanels.entrySet().removeIf(entry -> {
            if (!activeIds.contains(entry.getKey())) {
                playersList.getChildren().remove(entry.getValue());
                return true;
            }
            return false;
        });
    }

    @Override
    public void onGameStart() {
        networkClient.setListener(null);
        screenManager.switchScreen(ScreenType.GAME, gameContext);
    }

    @Override
    public void onGameState(ServerMessage m) {}
    @Override
    public void onGamePaused() {

    }
    @Override
    public void onGameResumed() {

    }
    @Override
    public void onWin(ServerMessage m) {

    }
    @Override
    public void onLeaderBoard(ServerMessage m) {
        new LeaderboardPopup().show(m.leaderboard);
    }

    @Override
    public void onDisconnected() {
        gameContext.setMyPlayerId(-1);
        networkClient.setListener(null);
        gameContext.setNetworkClient(null);

        showMainMenu();
        showError("Connection lost. Server may be down.");
    }
}

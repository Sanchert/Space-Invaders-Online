package org.example.space_invaders_online.game.sceneController.controllers;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.VBox;
import org.example.space_invaders_online.game.client.*;
import org.example.space_invaders_online.game.database.PlayerStats;
import org.example.space_invaders_online.game.sceneController.GameContext;
import org.example.space_invaders_online.game.sceneController.ScreenManager;
import org.example.space_invaders_online.game.sceneController.ScreenType;
import org.example.space_invaders_online.game.server.ServerMessage;

import java.io.IOException;
import java.util.List;

public class MenuScreenController extends BaseController implements INetworkListener {
    // ===== TITLE SCREEN =====
    @FXML private Label gameTitle;
    @FXML private VBox splashContent;

    // ===== MAIN MENU SCREEN =====
    @FXML private VBox mainMenuContent;
    @FXML private Button singlePlayerBtn; //
    @FXML private Button onlineGameBtn; //
    @FXML private Button optionsBtn; //
    @FXML private Button exitBtn; //

    // ===== NAME INPUT SCREEN ====
    @FXML private VBox nameInputContent;
    @FXML private TextField nameField;
    @FXML private Button confirmNameBtn; //
    @FXML private Button backToMenuBtn; //

    // ===== LOBBY SCREEN ====
    @FXML private VBox lobbyContent;
    @FXML private VBox playersList;
    @FXML private Button readyBtn; //
    @FXML private Button lobbyBackBtn; //

    @FXML private Label errorLabel;

    private String name;
    private NetworkClient networkClient;
    private int myPlayerId = -1;

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

        Platform.runLater(() -> {
            if (screenManager.getStage().getScene() != null) {
                screenManager.getStage().getScene().addEventFilter(KeyEvent.KEY_PRESSED, this::onKeyPressed);
            }
        });

        showSplash();

        networkClient = gameContext.getNetworkClient();
        networkClient.setListener(this);
    }

    private void onKeyPressed(KeyEvent event) {
        if (splashContent.isVisible()) {
            showMainMenu();
            event.consume();
        }
    }

    private void showSplash() {
        gameTitle.setVisible(true);

        splashContent.setVisible(true);
        splashContent.setManaged(true);
    }

    private void showMainMenu() {
        gameTitle.setVisible(true);

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
        OnlineMatchClient client = gameContext.getOnlineMatchClient();
        if (client != null) {
            client.shutdownForMenuBack();
            gameContext.setOnlineMatchClient(null);
        }
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
        // if mode == ONLINE ... bla-bla
        if (networkClient == null) {
            showError("network error");
        }
        networkClient.connect("localhost", 12345);
        name = nameField.getText().trim();

        if (name.isEmpty()) {
            showError("Please enter your name");
            return;
        }

        if (name.length() < 2) {
            showError("Name must be at least 2 characters");
            return;
        }

        gameContext.setPlayerName(name);
        // wait until receive myPlayerId from onInit (answer on connection)
        while (myPlayerId == -1) {}
        // try set name
        networkClient.send(new Request(RequestType.SET_NAME, name, myPlayerId));

        showLobby();
    }

    private void onReady() {
        // TODO: while(!allPlayersReady) { wait; }
        startOnlineSession(name);
    }

    private void startOnlineSession(String name) {
        try {
            OnlineMatchClient client = new OnlineMatchClient();
            gameContext.setOnlineMatchClient(client);
            client.bindLobby(
                    playersList,
                    lobbyStatusLabel,
                    readyBtn,
                    lobbyLeaderboardBtn,
                    () -> screenManager.switchScreen(ScreenType.GAME, gameContext),
                    () -> Platform.runLater(() -> {
                        hideLobby();
                        showNameInput();
                    })
            );
            client.beginConnection(name);
            showLobby();
        } catch (IOException e) {
            gameContext.setOnlineMatchClient(null);
            showError("Cannot connect to server: " + e.getMessage());
        }
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
        myPlayerId = m.playerId;

    }

    @Override
    public void onGameState(ServerMessage m) {

    }

    @Override
    public void onNameAccepted() {

    }

    @Override
    public void onNameRejected() {

    }

    @Override
    public void onPlayerListUpdate(ServerMessage m) {

    }

    @Override
    public void onGameStart() {

    }

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
    public void onLeaderBoard(List<PlayerStats> board) {

    }

    @Override
    public void onDisconnected() {

    }
}

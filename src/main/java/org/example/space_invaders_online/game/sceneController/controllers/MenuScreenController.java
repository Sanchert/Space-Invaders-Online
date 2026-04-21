package org.example.space_invaders_online.game.sceneController.controllers;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.example.space_invaders_online.game.client.OnlineMatchClient;
import org.example.space_invaders_online.game.sceneController.GameContext;
import org.example.space_invaders_online.game.sceneController.ScreenManager;
import org.example.space_invaders_online.game.sceneController.ScreenType;

import java.io.IOException;

public class MenuScreenController extends BaseController {

    @FXML private StackPane rootPane;
    @FXML private StackPane contentContainer;
    @FXML private VBox splashContent;
    @FXML private VBox mainMenuContent;
    @FXML private VBox nameInputContent;

    @FXML private Label gameTitle;
    @FXML private Label pressAnyKeyLabel;
    @FXML private TextField nameField;
    @FXML private Label errorLabel;

    @FXML private Button singlePlayerBtn;
    @FXML private Button onlineGameBtn;
    @FXML private Button optionsBtn;
    @FXML private Button exitBtn;
    @FXML private Button confirmNameBtn;
    @FXML private Button backToMenuBtn;

    @FXML private VBox lobbyContent;
    @FXML private Label lobbyStatusLabel;
    @FXML private VBox playersList;
    @FXML private Button readyBtn;
    @FXML private Button lobbyLeaderboardBtn;
    @FXML private Button lobbyBackBtn;

    public MenuScreenController(ScreenManager screenManager, GameContext gameContext) {
        super(screenManager, gameContext);
    }

    @FXML
    public void initialize() {
        singlePlayerBtn.setDisable(true);
        singlePlayerBtn.setVisible(false);
        singlePlayerBtn.setManaged(false);

        singlePlayerBtn.setOnAction(e -> onSinglePlayer());
        onlineGameBtn.setOnAction(e -> onOnlineGame());
        optionsBtn.setOnAction(e -> onOptions());
        exitBtn.setOnAction(e -> onExit());
        confirmNameBtn.setOnAction(e -> onConfirmName());
        backToMenuBtn.setOnAction(e -> showMainMenu());

        lobbyBackBtn.setOnAction(e -> onLobbyBack());

        showSplash();

        Platform.runLater(() -> {
            if (screenManager.getStage().getScene() != null) {
                screenManager.getStage().getScene().addEventFilter(KeyEvent.KEY_PRESSED, this::onKeyPressed);
            }
        });
    }

    private void onKeyPressed(KeyEvent event) {
        if (splashContent.isVisible()) {
            showMainMenu();
            event.consume();
        }
    }

    private void showSplash() {
        hideLobby();
        gameTitle.setVisible(true);
        gameTitle.setManaged(true);

        splashContent.setVisible(true);
        splashContent.setManaged(true);

        mainMenuContent.setVisible(false);
        mainMenuContent.setManaged(false);

        nameInputContent.setVisible(false);
        nameInputContent.setManaged(false);
    }

    private void showMainMenu() {
        hideLobby();
        gameTitle.setVisible(true);
        gameTitle.setManaged(true);

        splashContent.setVisible(false);
        splashContent.setManaged(false);

        mainMenuContent.setVisible(true);
        mainMenuContent.setManaged(true);

        nameInputContent.setVisible(false);
        nameInputContent.setManaged(false);
    }

    private void showNameInput() {
        hideLobby();
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
        contentContainer.setVisible(false);
        contentContainer.setManaged(false);

        lobbyContent.setVisible(true);
        lobbyContent.setManaged(true);
    }

    private void hideLobby() {
        lobbyContent.setVisible(false);
        lobbyContent.setManaged(false);

        contentContainer.setVisible(true);
        contentContainer.setManaged(true);
    }

    private void onLobbyBack() {
        OnlineMatchClient client = gameContext.getOnlineMatchClient();
        if (client != null) {
            client.shutdownForMenuBack();
            gameContext.setOnlineMatchClient(null);
        }
        playersList.getChildren().clear();
        hideLobby();
        showMainMenu();
    }

    private void onSinglePlayer() {
        gameContext.setGameMode(GameContext.GameMode.SINGLE);
        showNameInput();
    }

    private void onOnlineGame() {
        gameContext.setGameMode(GameContext.GameMode.ONLINE);
        showNameInput();
    }

    private void onOptions() {
        System.out.println("Options - пока не реализовано");
    }

    private void onExit() {
        OnlineMatchClient client = gameContext.getOnlineMatchClient();
        if (client != null) {
            client.shutdownForMenuBack();
            gameContext.setOnlineMatchClient(null);
        }
        Platform.exit();
        System.exit(0);
    }

    private void onConfirmName() {
        String name = nameField.getText().trim();

        if (name.isEmpty()) {
            showError("Please enter your name");
            return;
        }

        if (name.length() < 3) {
            showError("Name must be at least 3 characters");
            return;
        }

        gameContext.setPlayerName(name);

        if (gameContext.getGameMode() == GameContext.GameMode.SINGLE) {
            showError("Single player is not available yet");
            return;
        }

        OnlineMatchClient existing = gameContext.getOnlineMatchClient();
        if (existing != null && existing.isConnected()) {
            existing.submitPlayerName(name);
            showLobby();
            if (lobbyStatusLabel != null) {
                lobbyStatusLabel.setText("Checking name...");
            }
            return;
        }

        if (existing != null) {
            existing.shutdownForMenuBack();
            gameContext.setOnlineMatchClient(null);
        }

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
            if (lobbyStatusLabel != null) {
                lobbyStatusLabel.setText("Connecting...");
            }
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
}

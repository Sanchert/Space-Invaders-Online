package org.example.space_invaders_online.game.sceneController.controllers;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.VBox;
import org.example.space_invaders_online.game.sceneController.GameContext;
import org.example.space_invaders_online.game.sceneController.ScreenManager;
import org.example.space_invaders_online.game.sceneController.ScreenType;

public class MenuScreenController extends BaseController {
    @FXML private VBox splashContent;          // Только текст "press any button"
    @FXML private VBox mainMenuContent;        // Кнопки
    @FXML private VBox nameInputContent;       // Ввод имени

    @FXML private Label gameTitle;     // Заголовок (всегда виден в SPLASH/MAIN)

    @FXML private Label     pressAnyKeyLabel;
    @FXML private TextField nameField;
    @FXML private Label     errorLabel;

    @FXML private Button singlePlayerBtn;
    @FXML private Button onlineGameBtn;
    @FXML private Button optionsBtn;
    @FXML private Button exitBtn;
    @FXML private Button confirmNameBtn;
    @FXML private Button backToMenuBtn;

    public MenuScreenController(ScreenManager screenManager, GameContext gameContext) {
        super(screenManager, gameContext);
    }

    @FXML
    public void initialize() {
        // === Настройка кнопок ===
        singlePlayerBtn.setOnAction(e -> onSinglePlayer());
        onlineGameBtn.setOnAction(e -> onOnlineGame());
        optionsBtn.setOnAction(e -> onOptions());
        exitBtn.setOnAction(e -> onExit());
        confirmNameBtn.setOnAction(e -> onConfirmName());
        backToMenuBtn.setOnAction(e -> showMainMenu());

        showSplash();

        Platform.runLater(() -> {
            screenManager.getStage().getScene().addEventFilter(KeyEvent.KEY_PRESSED, this::onKeyPressed);
        });
    }

    private void onKeyPressed(KeyEvent event) {
        if (splashContent.isVisible()) {
            showMainMenu();
            event.consume();
        }
    }

    private void showSplash() {
        // Заголовок всегда виден в SPLASH и MAIN MENU
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
            screenManager.switchScreen(ScreenType.GAME, gameContext);
        } else {
            screenManager.switchScreen(ScreenType.LOBBY, gameContext);
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

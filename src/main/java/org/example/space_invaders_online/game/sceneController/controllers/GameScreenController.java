package org.example.space_invaders_online.game.sceneController.controllers;

import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.example.space_invaders_online.game.client.OnlineMatchClient;
import org.example.space_invaders_online.game.sceneController.GameContext;
import org.example.space_invaders_online.game.sceneController.ScreenManager;
import org.example.space_invaders_online.game.sceneController.ScreenType;

/**
 * Игровой экран ({@code game.fxml}): холст, HUD справа, пауза. Сеть — {@link OnlineMatchClient}.
 */
public class GameScreenController extends BaseController {

    @FXML private HBox gameRootHBox;
    @FXML private StackPane gameStack;
    @FXML private Canvas gameCanvas;
    @FXML private Label winOverlayLabel;
    @FXML private Button resumeButton;
    @FXML private Button pauseButton;
    @FXML private Button exitToMenuBtn;
    @FXML private VBox pauseOverlay;
    @FXML private VBox gameHudRows;

    public GameScreenController(ScreenManager screenManager, GameContext gameContext) {
        super(screenManager, gameContext);
    }

    @FXML
    public void initialize() {
        OnlineMatchClient match = gameContext.getOnlineMatchClient();
        if (match == null) {
            screenManager.switchScreen(ScreenType.MAIN_MENU, gameContext);
            return;
        }

        if (winOverlayLabel != null) {
            winOverlayLabel.setVisible(false);
            winOverlayLabel.setManaged(false);
        }
        if (pauseOverlay != null) {
            pauseOverlay.setVisible(false);
            pauseOverlay.setManaged(false);
        }

        match.bindGame(gameCanvas, gameStack, winOverlayLabel, pauseOverlay, gameHudRows);
        match.bindPauseResumeButtons(pauseButton, resumeButton);

        if (exitToMenuBtn != null) {
            exitToMenuBtn.setOnAction(e -> {
                match.shutdownForMenuBack();
                gameContext.setOnlineMatchClient(null);
                screenManager.switchScreen(ScreenType.MAIN_MENU, gameContext);
            });
        }
    }
}

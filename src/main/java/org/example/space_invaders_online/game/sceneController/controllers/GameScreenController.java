package org.example.space_invaders_online.game.sceneController.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.Button;
import javafx.scene.layout.AnchorPane;
import org.example.space_invaders_online.game.client.MenuClientController;
import org.example.space_invaders_online.game.sceneController.GameContext;
import org.example.space_invaders_online.game.sceneController.ScreenManager;
import org.example.space_invaders_online.game.singleplayer.SinglePlayerGame;

public class GameScreenController extends BaseController{

    @FXML private Canvas gameCanvas;
    @FXML private Button resumeButton;
    @FXML private Button pauseButton;

    private MenuClientController multiplayerGame;

    public GameScreenController(ScreenManager screenManager, GameContext gameContext) {
        super(screenManager, gameContext);
    }

    @FXML
    public void initialize() {
        setupMultiplayer();

        resumeButton.setOnAction(e -> onResumeClick());
        pauseButton.setOnAction(e -> onPauseClick());
    }

    private void setupMultiplayer() {
        // Загрузка мультиплеерного клиента
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/example/space_invaders_online/SpaceInvadersOnline.fxml"));
            AnchorPane root = loader.load();
            multiplayerGame = loader.getController();

            // Встраиваем в текущую сцену
            gameCanvas.getScene().setRoot(root);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void onPauseClick() {
        multiplayerGame.onPauseBtnClick();
    }

    private void onResumeClick() {
        multiplayerGame.onResumeBtnClick();
    }
}

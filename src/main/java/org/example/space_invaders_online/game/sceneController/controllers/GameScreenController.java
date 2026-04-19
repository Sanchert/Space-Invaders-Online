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

    private SinglePlayerGame singlePlayerGame;
    private MenuClientController multiplayerGame;
    private boolean isMultiplayer;

    public GameScreenController(ScreenManager screenManager, GameContext gameContext) {
        super(screenManager, gameContext);
        this.isMultiplayer = gameContext.getGameMode() == GameContext.GameMode.ONLINE;
    }

    @FXML
    public void initialize() {
        if (isMultiplayer) {
            setupMultiplayer();
        } else {
            setupSinglePlayer();
        }

        resumeButton.setOnAction(e -> onResumeClick());
        pauseButton.setOnAction(e -> onPauseClick());
    }

    private void setupSinglePlayer() {
        singlePlayerGame = new SinglePlayerGame(gameCanvas);
        singlePlayerGame.start();

        // Настройка обработки клавиш
        gameCanvas.setFocusTraversable(true);
        gameCanvas.requestFocus();
        gameCanvas.setOnKeyPressed(e -> {
            switch (e.getCode()) {
                case W -> singlePlayerGame.move(org.example.space_invaders_online.game.client.MoveDirection.MOVE_UP);
                case S -> singlePlayerGame.move(org.example.space_invaders_online.game.client.MoveDirection.MOVE_DOWN);
                case D -> singlePlayerGame.shoot();
                case ESCAPE -> singlePlayerGame.pause();
            }
        });
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
        if (isMultiplayer && multiplayerGame != null) {
            multiplayerGame.onPauseBtnClick();
        } else if (singlePlayerGame != null) {
            singlePlayerGame.pause();
        }
    }

    private void onResumeClick() {
        if (isMultiplayer && multiplayerGame != null) {
            multiplayerGame.onResumeBtnClick();
        } else if (singlePlayerGame != null) {
            singlePlayerGame.resume();
        }
    }
}

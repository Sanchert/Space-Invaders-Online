package org.example.space_invaders_online.game.sceneController.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import org.example.space_invaders_online.game.sceneController.GameContext;
import org.example.space_invaders_online.game.sceneController.ScreenManager;

public class GameScreenController extends BaseController{

    @FXML private Button resumeButton;
    @FXML private Button pauseButton;

    public GameScreenController(ScreenManager screenManager, GameContext gameContext) {
        super(screenManager, gameContext);
    }

    @FXML public void initialize() {
        resumeButton.setOnAction(e -> onResumeClick());
        pauseButton.setOnAction(e -> onPauseClick());
    }

    private void onPauseClick() {

    }

    private void onResumeClick() {

    }
}

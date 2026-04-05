package org.example.space_invaders_online.game.sceneController.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import org.example.space_invaders_online.game.sceneController.GameContext;
import org.example.space_invaders_online.game.sceneController.ScreenManager;
import org.example.space_invaders_online.game.sceneController.ScreenType;

public class LobbyScreenController extends BaseController {

    @FXML private Button readyBtn;
    @FXML private Button backBtn;

    @FXML private VBox playersList;

    public LobbyScreenController(ScreenManager screenManager, GameContext gameContext) {
        super(screenManager, gameContext);
    }

    @FXML
    public void initialize() {
        readyBtn.setOnAction(e-> readyBtnPressed());
        backBtn.setOnAction(e-> backBtnPressed());
    }

    private void readyBtnPressed() {

    }

    private void backBtnPressed() {
        screenManager.switchScreen(ScreenType.MAIN_MENU);
    }
}

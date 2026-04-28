package org.example.space_invaders_online.game.client;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.example.space_invaders_online.game.sceneController.GameContext;
import org.example.space_invaders_online.game.sceneController.ScreenManager;
import org.example.space_invaders_online.game.sceneController.ScreenType;

import java.io.IOException;

public class GameApplication extends Application {
    private ScreenManager screenManager;
    @Override
    public void start(Stage stage) {
        GameContext ctx = new GameContext();
        screenManager = new ScreenManager(stage, ctx);
        screenManager.switchScreen(ScreenType.MAIN_MENU);
    }

    static void main(String[] args) {
        launch(args);
    }
}

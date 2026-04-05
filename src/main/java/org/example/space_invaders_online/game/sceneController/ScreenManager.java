package org.example.space_invaders_online.game.sceneController;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

import javafx.application.Platform;

public class ScreenManager {

    private final Stage stage;
    private GameContext gameContext;

    public ScreenManager(Stage stage) {
        this.stage = stage;
        this.gameContext = new GameContext();

        stage.setOnCloseRequest(e -> {
            Platform.exit();
            System.exit(0);
        });
    }

    public void switchScreen(ScreenType type) {
        switchScreen(type, gameContext);
    }

    public void switchScreen(ScreenType type, GameContext context) {
        try {
            String fxmlPath = getFxmlPath(type);
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));

            loader.setControllerFactory(controllerClass -> {
                try {
                    return controllerClass.getConstructor(
                            ScreenManager.class,
                            GameContext.class
                    ).newInstance(this, context);
                } catch (Exception e) {
                    try {
                        return controllerClass.getDeclaredConstructor().newInstance();
                    } catch (Exception ex) {
                        throw new RuntimeException(ex);
                    }
                }
            });

            Parent root = loader.load();
            Scene scene = new Scene(root, 1920, 1080);

            stage.setScene(scene);
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to load screen: " + type, e);
        }
    }

    private String getFxmlPath(ScreenType type) {
        return switch (type) {
            case MAIN_MENU -> "/fxml/menu.fxml";
            case LOBBY -> "/fxml/lobby.fxml";
            case GAME -> "/fxml/game.fxml";
            case VICTORY -> "/fxml/victory.fxml";
            default -> "/fxml/menu.fxml";
        };
    }

    public Stage getStage() {
        return stage;
    }

    public GameContext getGameContext() {
        return gameContext;
    }
}
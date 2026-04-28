package org.example.space_invaders_online.game.sceneController;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Screen;
import javafx.stage.Stage;

import java.io.IOException;

public class ScreenManager {

    private final Stage stage;
    private final GameContext gameContext;

    public ScreenManager(Stage stage, GameContext gctx) {
        this.stage = stage;
        this.gameContext = gctx;

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
            Rectangle2D visualBounds = Screen.getPrimary().getVisualBounds();
            double sceneW = Math.min(1920, visualBounds.getWidth());
            double sceneH = Math.min(1080, visualBounds.getHeight());
            Scene scene = new Scene(root, sceneW, sceneH);

            stage.setScene(scene);
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to load screen: " + type, e);
        }
    }

    private String getFxmlPath(ScreenType type) {
        return switch (type) {
            case MAIN_MENU  -> "/fxml/menu.fxml";
            case GAME       -> "/fxml/game.fxml";
        };
    }

    public Stage getStage() {
        return stage;
    }

    public GameContext getGameContext() {
        return gameContext;
    }
}
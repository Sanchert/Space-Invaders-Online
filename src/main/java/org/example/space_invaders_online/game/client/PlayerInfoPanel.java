package org.example.space_invaders_online.game.client;

import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

public class PlayerInfoPanel extends VBox {
    private final Label nameLabel;
    private final Label scoreLabel;
    private final Label shootsLabel;
    private final int colorId;
    private String currentName;

    public PlayerInfoPanel(String name, int shoots, int colorId) {
        this.colorId = colorId;
        this.currentName = name;

        setStyle("-fx-padding: 5; -fx-border-color: #333; -fx-border-width: 1; -fx-background-color: #2c2c2c;");
        setPrefWidth(160);

        nameLabel = new Label(name);
        nameLabel.setStyle("-fx-text-fill: " + getColorHex(colorId) + "; -fx-font-weight: bold;");

        scoreLabel = new Label("Score: 0");
        scoreLabel.setStyle("-fx-text-fill: #ccc; -fx-font-size: 11;");

        shootsLabel = new Label("Shots: " + shoots);
        shootsLabel.setStyle("-fx-text-fill: #ccc; -fx-font-size: 11;");

        getChildren().addAll(nameLabel, scoreLabel, shootsLabel);
    }

    public void setDisconnected(boolean disconnected) {
        if (disconnected) {
            nameLabel.setText(currentName + " [DC]");
            nameLabel.setStyle("-fx-text-fill: gray;");
            setStyle("-fx-padding: 5; -fx-border-color: #666; -fx-background-color: #1a1a1a;");
        } else {
            nameLabel.setText(currentName);
            nameLabel.setStyle("-fx-text-fill: " + getColorHex(colorId) + "; -fx-font-weight: bold;");
            setStyle("-fx-padding: 5; -fx-border-color: #333; -fx-border-width: 1; -fx-background-color: #2c2c2c;");
        }
    }

    public void setCurrentPlayer(boolean isCurrent) {
        if (isCurrent) {
            setStyle("-fx-padding: 5; -fx-border-color: #4CAF50; -fx-border-width: 2; -fx-background-color: #1a3a1a;");
        } else if (!getStyle().contains("#4CAF50")) {
            setStyle("-fx-padding: 5; -fx-border-color: #333; -fx-border-width: 1; -fx-background-color: #2c2c2c;");
        }
    }

    public void updateName(String name) {
        this.currentName = name;
        nameLabel.setText(name);
    }

    public void updateScore(int score) {
        scoreLabel.setText("Score: " + score);
    }

    public void updateShoots(int shoots) {
        shootsLabel.setText("Shots: " + shoots);
    }

    private String getColorHex(int colorId) {
        return switch (colorId) {
            case 0 -> "#00ffff";
            case 1 -> "#ff4444";
            case 2 -> "#44ff44";
            case 3 -> "#ffaa44";
            default -> "#ff44ff";
        };
    }
}

package org.example.space_invaders_online.game.client;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.example.space_invaders_online.game.database.PlayerStats;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;

public class LeaderboardPopup {

    public void show(List<PlayerStats> leaderboard) {
        var url = LeaderboardPopup.class.getResource("/fxml/leaderboard-popup.fxml");
        if (url == null) {
            throw new IllegalStateException("Resource not found: /fxml/leaderboard-popup.fxml");
        }
        try {
            FXMLLoader loader = new FXMLLoader(url);
            Parent root = loader.load();
            LeaderboardPopupController controller = loader.getController();
            controller.fillLeaderboard(leaderboard);

            Stage stage = new Stage();
            stage.setTitle("Leaderboard");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root, 600, 400));
            stage.show();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to load leaderboard popup", e);
        }
    }
}

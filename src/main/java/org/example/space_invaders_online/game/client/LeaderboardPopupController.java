package org.example.space_invaders_online.game.client;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import org.example.space_invaders_online.game.database.PlayerStats;

import java.util.List;

public class LeaderboardPopupController {

    @FXML
    private GridPane leaderboardGrid;

    @FXML
    private void onClose(ActionEvent event) {
        Node source = (Node) event.getSource();
        source.getScene().getWindow().hide();
    }

    public void fillLeaderboard(List<PlayerStats> leaderboard) {
        leaderboardGrid.getChildren().clear();

        String[] headers = {"#", "Player", "Wins", "Shots", "Hits", "Accuracy"};
        for (int i = 0; i < headers.length; i++) {
            Label header = new Label(headers[i]);
            header.getStyleClass().add("leaderboard-popup-header");
            leaderboardGrid.add(header, i, 0);
        }

        if (leaderboard == null) {
            return;
        }
        int rank = 1;
        for (PlayerStats stats : leaderboard) {
            int row = rank;
            Label rankLabel = new Label(String.valueOf(rank++));
            rankLabel.getStyleClass().add("leaderboard-popup-rank");
            Label nameLabel = new Label(stats.getPlayerName());
            nameLabel.getStyleClass().add("leaderboard-popup-name");
            Label winsLabel = new Label(String.valueOf(stats.getWins()));
            winsLabel.getStyleClass().add("leaderboard-popup-wins");
            Label shotsLabel = new Label(String.valueOf(stats.getTotalShots()));
            shotsLabel.getStyleClass().add("leaderboard-popup-muted");
            Label hitsLabel = new Label(String.valueOf(stats.getTotalHits()));
            hitsLabel.getStyleClass().add("leaderboard-popup-muted");
            Label accuracyLabel = new Label(String.format("%.1f%%", stats.getAccuracy()));
            accuracyLabel.getStyleClass().add("leaderboard-popup-accuracy");

            leaderboardGrid.add(rankLabel, 0, row);
            leaderboardGrid.add(nameLabel, 1, row);
            leaderboardGrid.add(winsLabel, 2, row);
            leaderboardGrid.add(shotsLabel, 3, row);
            leaderboardGrid.add(hitsLabel, 4, row);
            leaderboardGrid.add(accuracyLabel, 5, row);
        }
    }
}

module org.example.space_invaders_online {
    requires javafx.controls;
    requires javafx.fxml;
    requires com.google.gson;
    requires javafx.graphics;
    requires java.desktop;
    requires javafx.media;

    opens org.example.space_invaders_online to javafx.fxml;
    exports org.example.space_invaders_online;
    exports org.example.space_invaders_online.game.server;
    opens org.example.space_invaders_online.game.server to javafx.fxml;
    exports org.example.space_invaders_online.game.utils;
    opens org.example.space_invaders_online.game.utils to javafx.fxml;
    exports org.example.space_invaders_online.game.gameWorld;
    opens org.example.space_invaders_online.game.gameWorld to javafx.fxml;
    exports org.example.space_invaders_online.game.client;
    opens org.example.space_invaders_online.game.client to javafx.fxml;
    exports org.example.space_invaders_online.game.sceneController.controllers;
    opens org.example.space_invaders_online.game.sceneController.controllers to javafx.fxml;
}
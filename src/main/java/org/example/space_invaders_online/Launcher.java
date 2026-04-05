package org.example.space_invaders_online;

import javafx.application.Application;
import org.example.space_invaders_online.game.client.GameApplication;

public class Launcher {
    public static void main(String[] args) {
        Application.launch(GameApplication.class, args);
    }
}

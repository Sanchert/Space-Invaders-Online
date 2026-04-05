package org.example.space_invaders_online.game.sceneController.controllers;

import org.example.space_invaders_online.game.sceneController.GameContext;
import org.example.space_invaders_online.game.sceneController.ScreenManager;

public abstract class BaseController {

    protected ScreenManager screenManager;
    protected GameContext gameContext;

    public BaseController(ScreenManager screenManager, GameContext gameContext) {
        this.screenManager = screenManager;
        this.gameContext = gameContext;
    }

    // Общие методы для всех контроллеров
    protected void showError(String message) {
        // Показать уведомление об ошибке
    }


    public void onEnter(GameContext context) {
    }

}

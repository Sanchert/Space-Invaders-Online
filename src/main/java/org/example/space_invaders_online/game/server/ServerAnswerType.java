package org.example.space_invaders_online.game.server;
// Типы сообщений
public enum ServerAnswerType {
    INIT,           // Первоначальная инициализация
    STATE_UPDATE,
    NAME_ACCEPTED,// Обновление состояния
    NAME_REJECTED,
    WIN
}

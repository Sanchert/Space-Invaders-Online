package org.example.space_invaders_online.game.server;

//import org.example.space_invaders_online.SerializableGameState;

public class ServerMessage {
    public ServerAnswerType type;
    public int playerId;                            // ID этого клиента (только для INIT)
//    public SerializableGameState currentGameState;  // Полное состояние мира
    public String args;
//    public ServerMessage(int playerId, ServerAnswerType type, SerializableGameState state) {
//        this.playerId = playerId;
//        this.currentGameState = state;
//        this.type = type;
//    }
//
//    public ServerMessage(int playerId, ServerAnswerType type, SerializableGameState state, String args) {
//        this.playerId = playerId;
//        this.currentGameState = state;
//        this.type = type;
//        this.args = args;
//    }
}

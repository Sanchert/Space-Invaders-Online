package org.example.space_invaders_online.game.server;
import java.io.Serializable;
import java.util.Collection;
import java.util.List;

public class SerializableGameState implements Serializable {
    public List<SerializablePlayer> players;
    public List<SerializableBullet> bullets;
    public List<SerializableTarget> targets;

    public SerializableGameState() {}

    public SerializableGameState(List<SerializablePlayer> players,
                                 List<SerializableBullet> bullets,
                                 List<SerializableTarget> targets) {
        this.players = players;
        this.bullets = bullets;
        this.targets = targets;
    }
}

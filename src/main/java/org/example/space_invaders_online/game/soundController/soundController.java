package org.example.space_invaders_online.game.soundController;

import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class soundController {
    private static Map<soundType, Media> mediaMap = new HashMap<>();
    private static Map<soundType, MediaPlayer> playerMap = new HashMap<>();
    private static double volume = 75.0;

    public static void loadTrack(soundType name, String path) {
        if (!mediaMap.containsKey(name)) {
            Media media = new Media(new File(path).toURI().toString());
            mediaMap.put(name, media);
        }
    }

    public static void play(soundType name) {
        Media media = mediaMap.get(name);
        if (media != null) {
            MediaPlayer player = new MediaPlayer(media);
            playerMap.put(name, player);
            player.play();
        }
    }

    public static void pause(soundType name) {
        MediaPlayer player = playerMap.get(name);
        if (player != null) {
            MediaPlayer.Status status = player.getStatus();
            if (status == MediaPlayer.Status.PLAYING) {
                player.pause();
            } else if (status == MediaPlayer.Status.PAUSED) {
                player.play(); // Можно добавить возможность возобновления, если нужно
            }
        }
    }

    public static void unload(soundType name) {
        MediaPlayer player = playerMap.remove(name);
        if (player != null) {
            player.dispose(); // Освобождает ресурсы MediaPlayer
        }
        // Если хотите полностью удалить трек из памяти:
        mediaMap.remove(name);
    }

    public static void stop(soundType name) {
        MediaPlayer player = playerMap.get(name);
        if (player != null) {
            player.stop();
        }
    }

    public static void setVolume(double volume) {
        soundController.volume = volume;
    }
}
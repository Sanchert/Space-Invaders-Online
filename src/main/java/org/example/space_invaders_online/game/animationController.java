package org.example.space_invaders_online.game;

import javafx.scene.image.Image;

class animation {
    public enum AnimationState {
        RUN,
        STOP
    }

    private final double animationLength;
    private double currentTime = 0.0;
    private final Image[] frames;
    private int activeImage;
    private AnimationState state;

    public animation(Image[] frames, double animationLength) {
        this.frames = frames;
        this.activeImage = 0;
        this.animationLength = animationLength;
        this.state = AnimationState.STOP;
    }

    public void update(double dT) {
        if (state != AnimationState.RUN) {
            return;
        }

        currentTime += dT;
        double timePerFrame = animationLength / frames.length;

        if (currentTime >= timePerFrame) {
            activeImage++;
            currentTime -= timePerFrame; // важно: вычитаем, а не обнуляем!

            // зацикливание анимации
            if (activeImage >= frames.length) {
                activeImage = 0;
                // опционально: остановить анимацию при завершении
                // state = AnimState.STOP;
            }
        }
    }

    public void play() {
        this.state = AnimationState.RUN;
    }

    public Image getCurrentFrame() {
        return frames[activeImage];
    }

    public void stop() {
        this.state = AnimationState.STOP;
    }

    public void reset() {
        this.activeImage = 0;
        this.currentTime = 0.0;
    }
}

public class animationController {
    private animation[] animations;


}

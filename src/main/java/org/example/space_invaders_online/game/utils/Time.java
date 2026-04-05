package org.example.space_invaders_online.game.utils;

public class Time {
    // Константы
    public static final double FIXED_DELTA_TIME = 1.0 / 60.0; // Фиксированный шаг для FixedUpdate

    // Текущие значения времени
    private static double deltaTime = 0.0;           // Переменный deltaTime для Update
    private static double fixedDeltaTime = FIXED_DELTA_TIME; // Фиксированный для FixedUpdate
    private static double time = 0.0;                 // Общее время игры
    private static double unscaledTime = 0.0;          // Время без учета scale
    private static double timeScale = 1.0;             // Множитель времени

    // Для интерполяции
    private static double interpolationFactor = 0.0;

    // Фреймовые счетчики
    private static int frameCount = 0;
    private static int fixedFrameCount = 0;

    // Геттеры (только для чтения)
    public static double getDeltaTime() {
        return deltaTime * timeScale;
    }
    public static double getFixedDeltaTime() {
        return fixedDeltaTime * timeScale;
    }
    public static double getUnscaledDeltaTime() {
        return deltaTime;
    }
    public static double getTime() {
        return time;
    }
    public static double getUnscaledTime() {
        return unscaledTime;
    }
    public static double getInterpolationFactor() {
        return interpolationFactor;
    }
    public static int getFrameCount() {
        return frameCount;
    }
    public static int getFixedFrameCount() {
        return fixedFrameCount;
    }

    // Управление временем
    public static void setTimeScale(double scale) {
        timeScale = Math.max(0, scale);
    }

    public static double getTimeScale() {
        return timeScale;
    }

    // Методы для обновления времени (вызываются из gameLoop)
    public static void update(double rawDeltaTime) {
        unscaledTime += rawDeltaTime;
        time += rawDeltaTime * timeScale;
        deltaTime = rawDeltaTime;
        frameCount++;
    }

    public static void fixedUpdate() {
        fixedFrameCount++;
        // fixedDeltaTime остается константным
    }

    public static void setInterpolationFactor(double factor) {
        interpolationFactor = factor;
    }

    // Утилиты для работы со временем
    public static double secondsToFixedSteps(double seconds) {
        return seconds / FIXED_DELTA_TIME;
    }

    public static double fixedStepsToSeconds(int steps) {
        return steps * FIXED_DELTA_TIME;
    }

    // Таймеры (удобно для создания задержек)
    public static class Timer {
        private final double duration;
        private double elapsed;
        private final boolean unscaled;

        public Timer(double duration, boolean unscaled) {
            this.duration = duration;
            this.unscaled = unscaled;
            this.elapsed = 0;
        }

        public boolean tick() {
            elapsed += unscaled ? getUnscaledDeltaTime() : getDeltaTime();
            return elapsed >= duration;
        }

        public void reset() {
            elapsed = 0;
        }
        public double getProgress() {
            return Math.min(1.0, elapsed / duration);
        }
        public boolean isFinished() {
            return elapsed >= duration;
        }
    }
}

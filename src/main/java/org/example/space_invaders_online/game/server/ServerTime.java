package org.example.space_invaders_online.game.server;

public class ServerTime {
    // Константы
    public static final double TICK_RATE = 60.0; // фиксированных тиков в секунду
    public static final double FIXED_DELTA_TIME = 1.0 / TICK_RATE; // ~0.01666 сек

    // Текущее состояние времени
    private static double accumulatedTime = 0.0;  // накопленное время для fixedUpdate
    private static double gameTime = 0.0;         // общее игровое время
    private static double lastTimestamp = 0.0;    // последний timestamp для расчета

    // Состояние сервера
    private static boolean isPaused = false;
    private static double pauseStartTime = 0.0;
    private static double totalPausedTime = 0.0;

    // Счетчики
    private static long tickCount = 0;
    private static long lastTickTime = System.nanoTime();

    // Геттеры
    public static double getGameTime() {
        return isPaused ? pauseStartTime - totalPausedTime : gameTime - totalPausedTime;
    }

    public static double getFixedDeltaTime() { return FIXED_DELTA_TIME; }
    public static long getTickCount() { return tickCount; }
    public static boolean isPaused() { return isPaused; }

    // Основной метод для gameLoop
    public static int calculateTicksToProcess(long currentNanoTime) {
        double currentTime = currentNanoTime / 1_000_000_000.0;
        double deltaTime = currentTime - lastTimestamp;
        lastTimestamp = currentTime;

        if (isPaused) {
            pauseStartTime = currentTime;
            return 0;
        }

        // Восстанавливаемся после паузы
        if (pauseStartTime > 0) {
            totalPausedTime += currentTime - pauseStartTime;
            pauseStartTime = 0;
        }

        // Накапливаем время
        accumulatedTime += deltaTime;

        // Считаем, сколько fixedUpdate нужно выполнить
        int ticksToProcess = (int)(accumulatedTime / FIXED_DELTA_TIME);
        accumulatedTime -= ticksToProcess * FIXED_DELTA_TIME;

        // Обновляем игровое время
        gameTime += ticksToProcess * FIXED_DELTA_TIME;
        tickCount += ticksToProcess;
        lastTickTime = currentNanoTime;

        return ticksToProcess;
    }

    // Для ручного управления паузой
    public static void setPaused(boolean paused) {
        if (isPaused == paused) return;

        double currentTime = System.nanoTime() / 1_000_000_000.0;

        if (paused) {
            pauseStartTime = currentTime;
        } else {
            totalPausedTime += currentTime - pauseStartTime;
            pauseStartTime = 0;
        }

        isPaused = paused;
    }

    // Сброс времени (например, при рестарте игры)
    public static void reset() {
        accumulatedTime = 0;
        gameTime = 0;
        tickCount = 0;
        isPaused = false;
        pauseStartTime = 0;
        totalPausedTime = 0;
        lastTimestamp = System.nanoTime() / 1_000_000_000.0;
    }
}

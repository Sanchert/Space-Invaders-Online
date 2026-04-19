package org.example.space_invaders_online.game.server;


import com.google.gson.Gson;
import org.example.space_invaders_online.game.gameWorld.GameWorld;
import org.example.space_invaders_online.game.client.Request;
import org.example.space_invaders_online.game.client.RequestType;

import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
/*
 * [CLIENT] Хранит все объекты, отсылает запросы на изменение, получает ответы с результатами изменения, отрисовывает.
 * Клиент при создании запрашивает соединение. Сервер выдаёт id и текущее свое состояние. Клиент обновляет состояние.
 * Клиент отправляет сообщения с типом действия и меткой-id (кого и как сервер должен изменить). Получает ответ, рисует.
 * Отрисовка происходит постоянно, обновленное состояние "вклинивается".
 *
 * [SERVER] Хранит только метаданные и изменяет их, по запросам. Отправляет ответы. Хранит пользователей.
 * Отдельно крутится нить с добавлением новых соединений (пока возможно) - не мешает обновлять игру (можно начать игру с
 * меньше чем 4 игроками)
 * Отдельно крутится обновление состояния игры - главный поток
 * Сервер накапливает обновления (применяет результаты запросов) и отсылает все накопленное спустя определенный интервал времени.
 * Сервер на время сессии хранит список всех подключенных игроков. При запросе на обновление, он
 * производит вычисления и отправляет результат всем игрокам (синхронизация?).
 *
 * [COMMON IDEA]
 * 1) Игрок жмет на кнопку
 * 2) передается сообщение серверу
 * 3) сервер рассылает новое состояние всем участникам
 * 4) все игроки разом отрисовывают новое состояние
 *
 * Клиент постоянно рендерит, что есть. Результат он получает от расчетов сервера [через свою нить?]
 *
 * Каждый клиент постоянно выполняет update. В отдельной нити читает сообщения с сервера, данные
 * из которых подаются в основной поток.
 *
 * У клиента 3 нити: update (главный), чтение, запись.
 *
 * [REQUEST TYPES]
 * 1) Двинуть вверх [MOVE_UP]    : сервер высылает всем игрокам обновленные координаты
 * 2) Двинуть вниз  [MOVE_DOWN]  : -//-
 * 3) Выстрелить    [SHOOT]      : сервер рассылает команду на создание объекта 'пуля'
 * 4) Сменить скин  [CHANGE_SKIN]: сервер рассылает новое значение цвета игрока
 *
 * [ANSWER TYPES]
 * 1) состояние мира:
 *      координаты всех объектов
 *      число игроков
 *      число мишеней
 *      число пуль
 *
 * [GAME OBJECTS]
 * Игроки
 * 1) клиент отсылает запрос на создает своего игрока (получить id)
 * 2) сервер вычисляет координаты нового игрока и рассылает их всем
 * 3) все игроки создают нового игрока в заданных координатах
 *
 * Мишени
 * 1) создаются у каждого игрока при подключении
 * 2) постоянно получают новую координату с сервера
 * 3) при пропадании пули, сервер рассылает сообщение об уничтожении пули
 * 4) каждый клиент уничтожает пулю
 *
 * Пули
 * 1) клиент отсылает запрос на создание пули (нажатие кнопки)
 * 2) сервер создает координаты и высылает их всем игрокам. Owner-id пули для обработки принадлежности попадания.
 * 3) все игроки создают пулю в полученных координатах
 * 4) сервер при столкновении пули рассылает сообщение об уничтожении объекта
 * 5) все клиенты уничтожают пулю
 *
 * */

public class Server {
    private static final int PORT = 12345;
    private static final int MAX_PLAYERS = 4;

    private ServerSocket serverSocket;
    private final Map<Integer, ClientHandler> clients = new ConcurrentHashMap<>();
    private final Map<Integer, ServerPlayer> gamePlayers = new ConcurrentHashMap<>();
    private final Map<Integer, Boolean> readyStatus = new ConcurrentHashMap<>();
    private final Map<Integer, Boolean> pauseRequest = new ConcurrentHashMap<>();

    private final AtomicInteger nextPlayerId = new AtomicInteger(1);
    private final Set<String> usedNames = ConcurrentHashMap.newKeySet();

    private ServerState state = ServerState.WAITING;
    private GameWorld gameWorld;
    private Thread gameThread;
    private volatile boolean gameRunning = false;

    private final Gson gson = new Gson();

    public void start() {
        try {
            serverSocket = new ServerSocket(PORT);
            System.out.println("[SERVER] Started on port " + PORT);

            // Thread для принятия подключений
            new Thread(this::acceptClients).start();

        } catch (IOException e) {
            System.err.println("[SERVER] Failed to start: " + e.getMessage());
        }
    }

    private void acceptClients() {
        while (true) {
            try {
                if (clients.size() >= MAX_PLAYERS && state != ServerState.RUNNING) {
                    Thread.sleep(1000);
                    continue;
                }

                Socket socket = serverSocket.accept();
                int playerId = nextPlayerId.getAndIncrement();

                ClientHandler handler = new ClientHandler(socket, playerId, this);
                clients.put(playerId, handler);
                readyStatus.put(playerId, false);
                pauseRequest.put(playerId, false);

                new Thread(handler).start();

                System.out.println("[SERVER] Player " + playerId + " connected");

                // Отправляем INIT сообщение
                sendInitMessage(playerId);

            } catch (Exception e) {
                if (!serverSocket.isClosed()) {
                    System.err.println("[SERVER] Error accepting connection: " + e.getMessage());
                }
                break;
            }
        }
    }

    private void sendInitMessage(int playerId) {
        ServerMessage message = new ServerMessage();
        message.type = ServerAnswerType.INIT;
        message.playerId = playerId;

        try {
            ClientHandler handler = clients.get(playerId);
            if (handler != null) {
                handler.sendMessage(gson.toJson(message));
            }
        } catch (IOException e) {
            System.err.println("[SERVER] Failed to send INIT: " + e.getMessage());
        }
    }

    public void handleClientRequest(int playerId, Request request) {
        switch (request.requestType()) {
            case SET_NAME:
                handleSetName(playerId, request.args());
                break;

            case START:
                handleReady(playerId, true);
                break;

            case PAUSE:
                handlePauseRequest(playerId, true);
                break;

            case RESUME:
                handlePauseRequest(playerId, false);
                break;

            case SHOOT:
                if (state == ServerState.RUNNING && gameWorld != null) {
                    gameWorld.handleRequest(playerId, request);
                    DatabaseManager.getInstance().recordShot(
                            gamePlayers.get(playerId).getName(), false); // hit будет обновлён позже
                }
                break;

            case MOVE_UP:
            case MOVE_DOWN:
                if (state == ServerState.RUNNING && gameWorld != null) {
                    gameWorld.handleRequest(playerId, request);
                }
                break;

            case GET_LEADERBOARD:
                sendLeaderboard(playerId);
                break;

            case DISCONNECT:
                removeClient(playerId);
                break;
        }
    }

    private void handleSetName(int playerId, String name) {
        if (name == null || name.trim().isEmpty()) {
            sendNameResponse(playerId, false);
            return;
        }

        name = name.trim();

        if (usedNames.contains(name)) {
            sendNameResponse(playerId, false);
            return;
        }

        usedNames.add(name);

        ServerPlayer player = new ServerPlayer(playerId, 21f, 60f + (clients.size() - 1) * 60f, 10, playerId % 4);
        player.setName(name);
        gamePlayers.put(playerId, player);

        sendNameResponse(playerId, true);
        broadcastPlayerList();
    }

    private void sendNameResponse(int playerId, boolean accepted) {
        ServerMessage message = new ServerMessage();
        message.type = accepted ? ServerAnswerType.NAME_ACCEPTED : ServerAnswerType.NAME_REJECTED;

        try {
            ClientHandler handler = clients.get(playerId);
            if (handler != null) {
                handler.sendMessage(gson.toJson(message));
            }
        } catch (IOException e) {
            System.err.println("[SERVER] Failed to send name response: " + e.getMessage());
        }
    }

    private void handleReady(int playerId, boolean ready) {
        readyStatus.put(playerId, ready);
        broadcastPlayerList();

        // Проверяем, все ли готовы
        if (state == ServerState.WAITING && allPlayersReady()) {
            startGame();
        }
    }

    private void handlePauseRequest(int playerId, boolean requestingPause) {
        pauseRequest.put(playerId, requestingPause);

        if (requestingPause) {
            // Проверяем, все ли запросили паузу
            if (allPlayersPaused()) {
                pauseGame();
            }
        } else {
            // Кто-то снял запрос паузы - возобновляем
            if (state == ServerState.PAUSED) {
                resumeGame();
            }
        }
    }

    private boolean allPlayersReady() {
        if (clients.size() < 2) return false; // Минимум 2 игрока для начала
        return readyStatus.values().stream().allMatch(ready -> ready);
    }

    private boolean allPlayersPaused() {
        return pauseRequest.values().stream().allMatch(paused -> paused);
    }

    private void startGame() {
        state = ServerState.RUNNING;
        gameWorld = new GameWorld(this);
        gameWorld.init();

        // Добавляем игроков в мир
        for (ServerPlayer player : gamePlayers.values()) {
            gameWorld.addExistingPlayer(player);
        }

        gameRunning = true;
        gameThread = new Thread(this::gameLoop);
        gameThread.start();

        broadcastGameStart();
    }

    private void gameLoop() {
        long lastTime = System.nanoTime();
        final double TICK_RATE = 60.0;
        final double TIME_PER_TICK = 1_000_000_000.0 / TICK_RATE;
        double delta = 0;

        while (gameRunning) {
            long now = System.nanoTime();
            delta += (now - lastTime) / TIME_PER_TICK;
            lastTime = now;

            while (delta >= 1 && gameRunning) {
                if (state == ServerState.RUNNING) {
                    gameWorld.update();
                    checkWinCondition();
                    broadcastGameState();
                }
                delta--;
            }

            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    private void pauseGame() {
        state = ServerState.PAUSED;
        // Приостанавливаем поток игры
        if (gameThread != null) {
            gameThread.suspend(); // Важно: используем suspend для соответствия требованию
        }
        broadcastGamePaused();
    }

    private void resumeGame() {
        if (gameThread != null) {
            gameThread.resume(); // Возобновляем поток
        }
        state = ServerState.RUNNING;
        broadcastGameResumed();
    }

    private void checkWinCondition() {
        if (gameWorld == null) return;

        String winner = gameWorld.hasWinner();
        if (winner != null) {
            endGame(winner);
        }
    }

    private void endGame(String winnerName) {
        gameRunning = false;
        state = ServerState.GAME_OVER;

        // Записываем победу в БД
        DatabaseManager.getInstance().recordWin(winnerName);

        // Сообщаем всем о победителе
        ServerMessage message = new ServerMessage();
        message.type = ServerAnswerType.WIN;
        message.args = winnerName;

        String jsonMessage = gson.toJson(message);
        for (ClientHandler client : clients.values()) {
            try {
                client.sendMessage(jsonMessage);
            } catch (IOException e) {
                System.err.println("[SERVER] Failed to send win message: " + e.getMessage());
            }
        }

        // Сбрасываем состояние для следующей игры
        resetForNextGame();
    }

    private void resetForNextGame() {
        state = ServerState.WAITING;

        // Сбрасываем статусы готовности
        for (Integer playerId : readyStatus.keySet()) {
            readyStatus.put(playerId, false);
            pauseRequest.put(playerId, false);
        }

        // Сбрасываем счёт игроков
        for (ServerPlayer player : gamePlayers.values()) {
            player.resetScore();
            player.resetShoots();
        }

        broadcastPlayerList();
    }

    private void broadcastGameState() {
        if (gameWorld == null) return;

        // Конвертируем в сериализуемые объекты
        List<SerializablePlayer> serializablePlayers = new ArrayList<>();
        for (ServerPlayer player : gameWorld.getPlayers().values()) {
            if (!player.isDestroyed()) {
                serializablePlayers.add(player.toSerializable());
            }
        }

        List<SerializableBullet> serializableBullets = new ArrayList<>();
        for (ServerBullet bullet : gameWorld.getBullets().values()) {
            if (!bullet.isDestroyed()) {
                serializableBullets.add(bullet.toSerializable());
            }
        }

        List<SerializableTarget> serializableTargets = new ArrayList<>();
        for (ServerTarget target : gameWorld.getTargets().values()) {
            if (!target.isDestroyed()) {
                serializableTargets.add(target.toSerializable());
            }
        }

        SerializableGameState state = new SerializableGameState(
                serializablePlayers,
                serializableBullets,
                serializableTargets
        );

        ServerMessage message = new ServerMessage();
        message.type = ServerAnswerType.STATE_UPDATE;
        message.currentGameState = state;

        String jsonMessage = gson.toJson(message);

        for (ClientHandler client : clients.values()) {
            try {
                client.sendMessage(jsonMessage);
            } catch (IOException e) {
                System.err.println("[SERVER] Failed to send state: " + e.getMessage());
            }
        }
    }

    private void broadcastPlayerList() {
        // Отправляем обновлённый список игроков
        ServerMessage message = new ServerMessage();
        message.type = ServerAnswerType.PLAYER_LIST_UPDATE;
        message.players = new ArrayList<>(gamePlayers.values());

        String jsonMessage = gson.toJson(message);

        for (ClientHandler client : clients.values()) {
            try {
                client.sendMessage(jsonMessage);
            } catch (IOException e) {
                System.err.println("[SERVER] Failed to send player list: " + e.getMessage());
            }
        }
    }

    private void broadcastGameStart() {
        ServerMessage message = new ServerMessage();
        message.type = ServerAnswerType.GAME_START;

        String jsonMessage = gson.toJson(message);

        for (ClientHandler client : clients.values()) {
            try {
                client.sendMessage(jsonMessage);
            } catch (IOException e) {
                System.err.println("[SERVER] Failed to send game start: " + e.getMessage());
            }
        }
    }

    private void broadcastGamePaused() {
        ServerMessage message = new ServerMessage();
        message.type = ServerAnswerType.GAME_PAUSED;

        String jsonMessage = gson.toJson(message);

        for (ClientHandler client : clients.values()) {
            try {
                client.sendMessage(jsonMessage);
            } catch (IOException e) {
                System.err.println("[SERVER] Failed to send pause: " + e.getMessage());
            }
        }
    }

    private void broadcastGameResumed() {
        ServerMessage message = new ServerMessage();
        message.type = ServerAnswerType.GAME_RESUMED;

        String jsonMessage = gson.toJson(message);

        for (ClientHandler client : clients.values()) {
            try {
                client.sendMessage(jsonMessage);
            } catch (IOException e) {
                System.err.println("[SERVER] Failed to send resume: " + e.getMessage());
            }
        }
    }

    private SerializableGameState serializeGameState() {
        List<SerializablePlayer> serializedPlayers = new ArrayList<>();
        for (ServerPlayer player : gamePlayers.values()) {
            if (!player.isDestroyed()) {
                serializedPlayers.add(player.serialize());
            }
        }

        List<SerializableBullet> serializedBullets = new ArrayList<>();
        for (ServerBullet bullet : gameWorld.getBullets().values()) {
            if (!bullet.isDestroyed()) {
                serializedBullets.add(bullet.serialize());
            }
        }

        List<SerializableTarget> serializedTargets = new ArrayList<>();
        for (ServerTarget target : gameWorld.getTargets().values()) {
            if (!target.isDestroyed()) {
                serializedTargets.add(target.serialize());
            }
        }

        return new SerializableGameState(serializedPlayers, serializedBullets, serializedTargets);
    }

    private void sendLeaderboard(int playerId) {
        List<PlayerStats> leaderboard = DatabaseManager.getInstance().getLeaderboard();

        ServerMessage message = new ServerMessage();
        message.type = ServerAnswerType.LEADERBOARD;
        message.leaderboard = leaderboard;

        try {
            ClientHandler handler = clients.get(playerId);
            if (handler != null) {
                handler.sendMessage(gson.toJson(message));
            }
        } catch (IOException e) {
            System.err.println("[SERVER] Failed to send leaderboard: " + e.getMessage());
        }
    }

    public void removeClient(int playerId) {
        ClientHandler handler = clients.remove(playerId);
        if (handler != null) {
            ServerPlayer player = gamePlayers.remove(playerId);
            if (player != null && player.getName() != null) {
                usedNames.remove(player.getName());
            }
        }
        readyStatus.remove(playerId);
        pauseRequest.remove(playerId);

        broadcastPlayerList();
        System.out.println("[SERVER] Player " + playerId + " disconnected");
    }

    public void stop() {
        gameRunning = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            System.err.println("[SERVER] Error stopping: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        Server server = new Server();
        server.start();

        // Добавляем shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            DatabaseManager.getInstance().shutdown();
            server.stop();
        }));
    }
}


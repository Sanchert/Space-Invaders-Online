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
    private static final InetAddress IP = null;
    private ServerSocket serverSocket;

    private final Map<Integer, ClientHandler> clients = new ConcurrentHashMap<>();
    private final Map<Integer, Boolean> clientPauseStates = new ConcurrentHashMap<>();
    private final Map<Integer, String> playerNames = new ConcurrentHashMap<>();
    private final GameWorld gameWorld = new GameWorld();

    private final Gson gson = new Gson();

    private volatile ServerState serverState = ServerState.WAITING;

    private static final double STATE_BROADCAST_RATE = 20.0; // 20 обновлений в секунду
    private double broadcastAccumulator = 0;

    public void start() {
        try {
            serverSocket = new ServerSocket(PORT, MAX_PLAYERS, IP); System.out.println("[SERVER] server started on " + serverSocket.getInetAddress() + ":" + PORT);

            gameWorld.init();

            Thread acceptor = new Thread(() -> {
                while (clients.size() < MAX_PLAYERS) {
                    try {
                        //System.out.println("[SERVER] waiting for new client...");
                        Socket clientSocket = serverSocket.accept();
                        int clientId = gameWorld.addPlayer();

                        ClientHandler handler = new ClientHandler(clientSocket, clientId, this);
                        clients.put(clientId, handler);
                        clientPauseStates.put(clientId, true);
                        // System.out.println("[SERVER] подключился клиент [" + clientId + "]: " + clientSocket.getPort());
                        new Thread(handler).start();

                        sendInitialState(handler, clientId);
                        updateServerState();
                    } catch (IOException e) {
                        System.out.println("[SERVER ERR] " + e.getMessage());
                    }
                }
                System.out.println("[SERVER] Acceptor closed.");
            });
            acceptor.start();
            gameLoop();
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    private void gameLoop() {
        long lastTime = System.nanoTime();


        while (serverState != ServerState.EXIT) {
            long currentTime = System.nanoTime();
            int ticksToProcess = ServerTime.calculateTicksToProcess(currentTime);

            if (serverState == ServerState.RUNNING) {

                for (int i = 0; i < ticksToProcess; i++) {
                    processGameTick();
                }

                // Отправляем обновления состояния с пониженной частотой
                broadcastAccumulator += ticksToProcess * ServerTime.FIXED_DELTA_TIME;
                if (broadcastAccumulator >= 1.0 / STATE_BROADCAST_RATE) {
                    broadcastAccumulator = 0;
                    broadcastGameState();
                }
//                gameWorld.update();
//                if (gameWorld.hasWinner() != null) {
//                    broadcastEndState(gameWorld.hasWinner());
//                    serverState = ServerState.WAITING;
//                    continue;
//                }
//                broadcastGameState();
            }
        }
    }

    private void processGameTick() {
        // Обновляем мир с фиксированным deltaTime
        gameWorld.update();

        // Обрабатываем накопленные запросы
        processPendingRequests();
    }

    // TODO:
    private void processPendingRequests() {
        // Здесь можно обрабатывать очередь запросов от клиентов
        // Например, применять их в фиксированные моменты времени
    }

    private void sendInitialState(ClientHandler handler, int playerId) throws IOException {
//        handler.sendMessage(gson.toJson(new ServerMessage(playerId, ServerAnswerType.INIT, gameWorld.serialize())));
    }


    private void broadcastEndState(String winner) {
        clients.values().forEach(clientHandler -> {
//            try {
//                clientHandler.sendMessage(gson.toJson(new ServerMessage(clientHandler.getPlayerId(), ServerAnswerType.WIN, gameWorld.serialize(), winner)));
//            } catch (IOException e) {
//                System.out.println(e.getMessage());
//            }
        });
    }
    private void broadcastGameState() {
        clients.values().forEach(clientHandler -> {
//            try {
//                clientHandler.sendMessage(gson.toJson(new ServerMessage(clientHandler.getPlayerId(), ServerAnswerType.STATE_UPDATE, gameWorld.serialize())));
//            } catch (IOException e) {
//                System.out.println(e.getMessage());
//            }
        });
    }

    public void handleClientRequest(int playerId, Request request) {
        RequestType type = request.requestType();

        if (type == RequestType.PAUSE) {
            clientPauseStates.put(playerId, true);
            updateServerState();
            System.out.println("[SERVER] Player " + playerId + " paused the game");
            return;
        }

        if (type == RequestType.START) {
            clientPauseStates.put(playerId, false);
            updateServerState();
            System.out.println("[SERVER] Player " + playerId + " resumed the game");
            return;
        }

        if (type == RequestType.SET_NAME) {

            String newName = request.args();
            boolean accepted = true;
            System.out.println("Get name:" + newName);

            if (newName == null || newName.trim().isEmpty()) {
                System.out.println("[SERVER] Player " + playerId + " tried to set empty name");
                accepted = false;
            }

            if (playerNames.containsValue(newName)) {
                System.out.println("[SERVER] Player " + playerId + " tried to use existing name: " + newName);
                accepted = false;
            }

            gameWorld.handleRequest(playerId, request);
            playerNames.put(playerId, newName);
            System.out.println("[SERVER] Player " + playerId + " set name to: " + newName);

            sendNameResponse(playerId, accepted);
            if (accepted) {
                broadcastGameState();
            }
            return;
        }

        if (type == RequestType.DISCONNECT) {
            gameWorld.handleRequest(playerId, request);
            if (clients.isEmpty()) {
                stop();
            }
            return;
        }

        if (serverState == ServerState.RUNNING) {
            gameWorld.handleRequest(playerId, request);
        } else {
            System.out.println("[SERVER] Ignoring " + type + " from player " + playerId + " - game is paused");
        }
    }
    private void sendNameResponse(int playerId, boolean accepted) {
        ClientHandler handler = clients.get(playerId);
        if (handler != null) {
//            try {
                ServerAnswerType type = accepted ? ServerAnswerType.NAME_ACCEPTED : ServerAnswerType.NAME_REJECTED;
//                ServerMessage response = new ServerMessage(playerId, type, null);
//                handler.sendMessage(gson.toJson(response));
//            } catch (IOException e) {
//                System.out.println("[SERVER] Failed to send name response to player " + playerId);
//            }
        }
    }
    private void updateServerState() {
        if (clients.isEmpty()) {
            serverState = ServerState.WAITING;
            return;
        }

        boolean anyPaused = clientPauseStates.values().stream().anyMatch(paused -> paused);

        if (anyPaused) {
            serverState = ServerState.WAITING;
            System.out.println("[SERVER] waiting");
        } else {
            serverState = ServerState.RUNNING;
            System.out.println("[SERVER] running");
        }
    }
    public void removeClient(int playerId) {
        clients.remove(playerId);
        clientPauseStates.remove(playerId);
        playerNames.remove(playerId);
//        gameWorld.removePlayer(playerId);
        updateServerState();
        broadcastGameState();
    }
    public void stop() {
        serverState = ServerState.EXIT;
        try {
            serverSocket.close();
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }
    static void main() {
        new Server().start();
    }
}


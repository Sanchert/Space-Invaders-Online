package org.example.space_invaders_online.game.server;

import com.google.gson.Gson;
import org.example.space_invaders_online.game.client.Request;
import org.example.space_invaders_online.game.database.DatabaseManager;
import org.example.space_invaders_online.game.database.PlayerStats;
import org.example.space_invaders_online.game.gameWorld.GameWorld;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static java.lang.Thread.sleep;

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
    private volatile boolean gameRunning = false;

    private final Gson gson = new Gson();

    public void start() {
        try {
            serverSocket = new ServerSocket(PORT);
            System.out.println("[SERVER] Started on port " + PORT);

            new Thread(this::acceptClients).start();

        } catch (IOException e) {
            System.err.println("[SERVER] Failed to start: " + e.getMessage());
        }
    }

    private void acceptClients() {
        while (true) {
            try {

                if (clients.size() >= MAX_PLAYERS && state != ServerState.RUNNING) {
                    wait(5000);
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
            case SET_NAME        -> handleSetName(playerId, request.args());
            case START           -> handleReadyToggle(playerId);
            case PAUSE           -> handlePauseRequest(playerId, true);
            case RESUME          -> handlePauseRequest(playerId, false);
            case GET_LEADERBOARD -> sendLeaderboard(playerId);
            case DISCONNECT      -> removeClient(playerId);
            case SHOOT,
                 MOVE_UP,
                 MOVE_DOWN       -> {
                if (state == ServerState.RUNNING && gameWorld != null) {
                    gameWorld.handleRequest(playerId, request);
                }
            }
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
        int slot = gamePlayers.size();
        ServerPlayer player = new ServerPlayer(playerId, 21f, 60f + slot * 60f, 10, playerId % 4);
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

    private void handleReadyToggle(int playerId) {
        boolean next = !Boolean.TRUE.equals(readyStatus.get(playerId));
        readyStatus.put(playerId, next);
        broadcastPlayerList();

        if (state == ServerState.WAITING && allPlayersReady()) {
            startGame();
        }
    }

    private void handlePauseRequest(int playerId, boolean requestingPause) {
        pauseRequest.put(playerId, requestingPause);

        if (requestingPause) {
            if (allPlayersPaused()) {
                pauseGame();
            }
        } else {
            if (state == ServerState.PAUSED && !allPlayersPaused()) {
                resumeGame();
            }
        }
    }

    private boolean allPlayersReady() {
        if (gamePlayers.size() < 2) return false;
        // Only check players who have completed name registration
        for (Integer id : gamePlayers.keySet()) {
            if (!Boolean.TRUE.equals(readyStatus.get(id))) return false;
        }
        return true;
    }

    private boolean allPlayersPaused() {
        if (gamePlayers.isEmpty()) return false;
        for (Integer id : gamePlayers.keySet()) {
            if (!Boolean.TRUE.equals(pauseRequest.get(id))) return false;
        }
        return true;
    }

    private void startGame() {
        state = ServerState.RUNNING;
        gameWorld = new GameWorld(this);
        gameWorld.loadMatch(gamePlayers.values());

        gameRunning = true;
        Thread gameThread = new Thread(this::gameLoop);
        gameThread.start();

        broadcastGameStart();
    }

    private void gameLoop() {
        ServerTime.reset();

        while (gameRunning) {
            long now = System.nanoTime();
            int ticks = ServerTime.calculateTicksToProcess(now);

            for (int i = 0; i < ticks && gameRunning; i++) {
                if (state == ServerState.RUNNING) {
                    gameWorld.update();
                    checkWinCondition();
                    broadcastGameState();
                }
            }

            try {
                sleep(1);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    private void pauseGame() {
        state = ServerState.PAUSED;
        gameLoop();
        ServerTime.setPaused(true);
        broadcastGamePaused();
    }

    private void resumeGame() {
        state = ServerState.RUNNING;
        ServerTime.setPaused(false);
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

        DatabaseManager db = DatabaseManager.getInstance();
        gamePlayers.values().forEach(sp -> {
            String name = sp.getName();
            if (name == null || name.isEmpty()) {
                return;
            }
            PlayerStats stats = db.getOrCreatePlayer(name);
            if (name.equals(winnerName)) {
                stats.addWin();
            }
            db.updatePlayerStats(stats);
        });

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

        List<DTOPlayer> serializablePlayers = new ArrayList<>();
        for (ServerPlayer player : gameWorld.getPlayers().values()) {
            serializablePlayers.add(player.serialize());
        }

        List<DTOBullet> serializableBullets = new ArrayList<>();
        for (ServerBullet bullet : gameWorld.getBullets().values()) {
            serializableBullets.add(bullet.toSerializable());
        }

        List<DTOTarget> serializableTargets = new ArrayList<>();
        for (ServerTarget target : gameWorld.getTargets().values()) {
            serializableTargets.add(target.serialize());
        }

        DTOGameState state = new DTOGameState(
                serializablePlayers,
                serializableBullets,
                serializableTargets
        );

        ServerMessage message = new ServerMessage();
        message.type = ServerAnswerType.UPDATE;
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
        message.players = new ArrayList<>();
        for (ServerPlayer p : gamePlayers.values()) {
            message.players.add(p.serialize());
        }

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

        if (state == ServerState.PAUSED && !gamePlayers.isEmpty()) {
            for (Integer id : gamePlayers.keySet()) {
                pauseRequest.put(id, false);
            }
            resumeGame();
        }

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

    public void recordHit(int playerId) {
        ServerPlayer p = gamePlayers.get(playerId);
        if (p != null && p.getName() != null)
            DatabaseManager.getInstance().recordHit(p.getName());
    }

    public void recordShot(int playerId) {
        ServerPlayer p = gamePlayers.get(playerId);
        if (p != null && p.getName() != null)
            DatabaseManager.getInstance().recordShot(p.getName());
    }

    public static void main(String[] args) {
        Server server = new Server();
        server.start();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            DatabaseManager.getInstance().shutdown();
            server.stop();
        }));
    }
}


package org.example.space_invaders_online.game.server;

import com.google.gson.Gson;
import org.example.space_invaders_online.game.client.Request;
import org.example.space_invaders_online.game.database.DatabaseManager;
import org.example.space_invaders_online.game.database.PlayerStats;
import org.example.space_invaders_online.game.gameWorld.GameWorld;
import org.example.space_invaders_online.game.server.dto.DTOBullet;
import org.example.space_invaders_online.game.server.dto.DTOGameState;
import org.example.space_invaders_online.game.server.dto.DTOPlayer;
import org.example.space_invaders_online.game.server.dto.DTOTarget;
import org.example.space_invaders_online.game.server.object.ServerBullet;
import org.example.space_invaders_online.game.server.object.ServerMessage;
import org.example.space_invaders_online.game.server.object.ServerPlayer;
import org.example.space_invaders_online.game.server.object.ServerTarget;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class Server {
    private static final int PORT = 12345;
    private static final int MAX_PLAYERS = 4;

    private static final String OBSERVER_PREFIX = "OBSERVER_";
    private static final String WATCHER_PREFIX  = "WATCHER_";
    private final Set<Integer> observers = ConcurrentHashMap.newKeySet();

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

    public void handleClientRequest (int playerId, Request request) {
        switch (request.requestType()) {
            case SET_NAME        -> handleSetName(playerId, request.args());
            case START           -> { if (!observers.contains(playerId)) handleReadyToggle(playerId); }
            case PAUSE           -> { if (!observers.contains(playerId)) handlePauseRequest(playerId, true);  }
            case RESUME          -> { if (!observers.contains(playerId)) handlePauseRequest(playerId, false); }
            case GET_LEADERBOARD -> sendLeaderboard(playerId);
            case DISCONNECT      -> removeClient(playerId);
            case SHOOT,
                 MOVE_UP,
                 MOVE_DOWN       -> {
                if (!observers.contains(playerId)
                        && state == ServerState.RUNNING
                        && gameWorld != null) {
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

        if (name.startsWith(OBSERVER_PREFIX) || name.startsWith(WATCHER_PREFIX)) {
            observers.add(playerId);
            sendNameResponse(playerId, true);
            System.out.println("[SERVER] Player " + playerId + " joined as observer: " + name);
            if (state == ServerState.RUNNING) {
                sendCurrentGameState(playerId);
            } else {
                broadcastPlayerListTo(playerId);
            }
            return;
        }

        if (gamePlayers.size() >= MAX_PLAYERS) {
            clients.remove(playerId);
            sendNameResponse(playerId, false);
            return;
        }
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
        sendTo(playerId, message);
    }

    private void handleReadyToggle(int playerId) {
        boolean next = !Boolean.TRUE.equals(readyStatus.get(playerId));
        readyStatus.put(playerId, next);
        broadcastPlayerList();
        if (state == ServerState.WAITING && allPlayersReady()) startGame();
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
        for (Integer id : gamePlayers.keySet()) {
            if (!Boolean.TRUE.equals(readyStatus.get(id))) return false;
        }
        return true;
    }

    private boolean allPlayersPaused() {
        if (gamePlayers.isEmpty()){
            return false;
        }
        for (Integer id : gamePlayers.keySet()) {
            if (!Boolean.TRUE.equals(pauseRequest.get(id))) {
                return false;
            }
        }
        return true;
    }

    private void startGame() {
        state     = ServerState.RUNNING;
        gameWorld = new GameWorld(this);
        gameWorld.loadMatch(gamePlayers.values());
        gameRunning = true;

        new Thread(this::gameLoop).start();
        broadcastGameStart();
    }

    private void gameLoop() {
        ServerTime.reset();
        while (gameRunning) {
            long now   = System.nanoTime();
            int  ticks = ServerTime.calculateTicksToProcess(now);
            for (int i = 0; i < ticks && gameRunning; i++) {
                if (state == ServerState.RUNNING) {
                    gameWorld.update();
                    String winner = gameWorld.hasWinner();
                    if (winner != null) { endGame(winner); return; }
                    broadcastGameState();
                }
            }
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    private void pauseGame()  {
        state = ServerState.PAUSED;
        ServerTime.setPaused(true);
        broadcastGamePaused();
    }

    private void resumeGame() {
        state = ServerState.RUNNING;
        ServerTime.setPaused(false);
        broadcastGameResumed();
    }

    public void endGame(String winnerName) {
        gameRunning = false;
        DatabaseManager db = DatabaseManager.getInstance();
        gamePlayers.values().forEach(sp -> {
            String n = sp.getName();
            if (n == null || n.isEmpty()) return;
            db.getOrCreate(n);
            if (n.equals(winnerName)) db.recordWin(n);
        });

        ServerMessage msg = new ServerMessage();
        msg.type = ServerAnswerType.WIN;
        msg.args = winnerName;
        broadcast(msg);

        resetForNextGame();
    }

    private void resetForNextGame() {
        state     = ServerState.WAITING;
        gameWorld = null;
        gamePlayers.clear();
        usedNames.clear();
        for (Integer id : clients.keySet()) {
            readyStatus.put(id, false);
            pauseRequest.put(id, false);
        }
        broadcastPlayerList();
    }

    private void broadcast(ServerMessage msg) {
        String json = gson.toJson(msg);
        for (ClientHandler ch : clients.values()) {
            try { ch.sendMessage(json); } catch (IOException e) {
                System.err.println("[SERVER] broadcast error: " + e.getMessage());
            }
        }
    }

    private void broadcastGameState() {
        if (gameWorld == null) return;

        List<DTOPlayer> serPlayers = new ArrayList<>();
        for (ServerPlayer p : gameWorld.getPlayers().values())  serPlayers.add(p.serialize());
        List<DTOBullet> serBullets = new ArrayList<>();
        for (ServerBullet b : gameWorld.getBullets().values())  serBullets.add(b.toSerializable());
        List<DTOTarget> serTargets = new ArrayList<>();
        for (ServerTarget t : gameWorld.getTargets().values())  serTargets.add(t.serialize());

        DTOGameState gs  = new DTOGameState(serPlayers, serBullets, serTargets);
        ServerMessage msg = new ServerMessage();
        msg.type             = ServerAnswerType.UPDATE;
        msg.currentGameState = gs;
        broadcast(msg);
    }

    private void sendCurrentGameState(int playerId) {
        if (gameWorld == null) {
            return;
        }
        List<DTOPlayer> servPlayers = new ArrayList<>();
        for (ServerPlayer p : gameWorld.getPlayers().values()) {
            servPlayers.add(p.serialize());
        }
        DTOGameState gs = new DTOGameState(servPlayers, new ArrayList<>(), new ArrayList<>());
        ServerMessage msg = new ServerMessage();
        msg.type = ServerAnswerType.UPDATE;
        msg.currentGameState = gs;
        sendTo(playerId, msg);
    }

    private void broadcastPlayerList() {
        ServerMessage msg = new ServerMessage();
        msg.type    = ServerAnswerType.PLAYER_LIST_UPDATE;
        msg.players = new ArrayList<>();
        for (ServerPlayer p : gamePlayers.values()) msg.players.add(p.serialize());
        broadcast(msg);
    }

    private void broadcastPlayerListTo(int playerId) {
        ServerMessage msg = new ServerMessage();
        msg.type    = ServerAnswerType.PLAYER_LIST_UPDATE;
        msg.players = new ArrayList<>();
        for (ServerPlayer p : gamePlayers.values()) msg.players.add(p.serialize());
        sendTo(playerId, msg);
    }

    private void broadcastGameStart() {
        ServerMessage m = new ServerMessage();
        m.type = ServerAnswerType.GAME_START;
        broadcast(m);
    }

    private void broadcastGamePaused() {
        ServerMessage m = new ServerMessage();
        m.type = ServerAnswerType.GAME_PAUSED;
        broadcast(m);
    }

    private void broadcastGameResumed() {
        ServerMessage m = new ServerMessage();
        m.type = ServerAnswerType.GAME_RESUMED;
        broadcast(m);
    }

    private void sendLeaderboard(int playerId) {
        List<PlayerStats> lb = DatabaseManager.getInstance().getLeaderboard();
        ServerMessage msg = new ServerMessage();
        msg.type = ServerAnswerType.LEADERBOARD;
        msg.leaderboard = lb;
        sendTo(playerId, msg);
    }

    private void sendTo(int playerId, ServerMessage msg) {
        ClientHandler handler = clients.get(playerId);
        if (handler == null) return;
        try {
            handler.sendMessage(gson.toJson(msg));
        } catch (IOException e) {
            System.err.println("[SERVER] sendTo " + playerId + " failed: " + e.getMessage());
        }
    }

    public void removeClient(int playerId) {
        clients.remove(playerId);
        observers.remove(playerId);
        ServerPlayer player = gamePlayers.remove(playerId);
        if (player != null && player.getName() != null) {
            usedNames.remove(player.getName());
        }
        readyStatus.remove(playerId);
        pauseRequest.remove(playerId);

        if (state == ServerState.PAUSED && !gamePlayers.isEmpty()) {
            for (Integer id : gamePlayers.keySet()) pauseRequest.put(id, false);
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
        if (p != null && p.getName() != null) DatabaseManager.getInstance().recordHit(p.getName());
    }

    public void recordShot(int playerId) {
        ServerPlayer p = gamePlayers.get(playerId);
        if (p != null && p.getName() != null) DatabaseManager.getInstance().recordShot(p.getName());
    }

    static void main() {
        Server server = new Server();
        server.start();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            DatabaseManager.getInstance().shutdown();
            server.stop();
        }));
    }
}

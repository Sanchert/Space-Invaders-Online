package org.example.space_invaders_online.game.client;

import com.google.gson.Gson;
import javafx.application.Platform;
import org.example.space_invaders_online.game.server.ServerMessage;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;

public class NetworkClient {
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private INetworkListener listener;
    private final Gson gson = new Gson();
    private volatile boolean connected;
    private Thread readerThread;
    private final AtomicBoolean suppressDisconnectCallback = new AtomicBoolean(false);

    public void connect(String host, int port) throws IOException {
        disconnect(true);
        socket = new Socket(host, port);
        out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(
                new BufferedOutputStream(socket.getOutputStream()),
                StandardCharsets.UTF_8)), true);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
        connected = true;

        readerThread = new Thread(this::readLoop, "network-client-reader");
        readerThread.setDaemon(true);
        readerThread.start();
    }

    public void setListener(INetworkListener l) {
        this.listener = l;
    }

    public void send(Request request) {
        if (!connected || out == null) {
            return;
        }
        out.println(gson.toJson(request));
    }

    public boolean isConnected() {
        return connected && socket != null && !socket.isClosed();
    }

    /**
     * @param notifyListener if false, {@link INetworkListener#onDisconnected()} is not invoked when the reader stops
     */
    public void disconnect(boolean notifyListener) {
        suppressDisconnectCallback.set(!notifyListener);
        connected = false;
        if (out != null) {
            out.close();
            out = null;
        }
        try {
            if (in != null) {
                in.close();
            }
        } catch (IOException ignored) {
        }
        in = null;
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException ignored) {
        }
        socket = null;
    }

    public void disconnect() {
        disconnect(true);
    }

    private void readLoop() {
        try {
            while (connected && in != null) {
                String line = in.readLine();
                if (line == null) {
                    break;
                }
                ServerMessage message = gson.fromJson(line, ServerMessage.class);
                if (message == null || message.type == null) {
                    continue;
                }
                dispatch(message);
            }
        } catch (IOException e) {
            System.out.println("[CLIENT] Read error: " + e.getMessage());
        } finally {
            connected = false;
            if (!suppressDisconnectCallback.getAndSet(false) && listener != null) {
                Platform.runLater(listener::onDisconnected);
            }
        }
    }

    private void dispatch(ServerMessage message) {
        INetworkListener l = listener;
        if (l == null) {
            return;
        }
        Platform.runLater(() -> {
            switch (message.type) {
                case INIT -> l.onInit(message.playerId);
                case UPDATE -> l.onGameState(message.currentGameState);
                case NAME_ACCEPTED -> l.onNameAccepted();
                case NAME_REJECTED -> l.onNameRejected(message.args != null ? message.args : "");
                case PLAYER_LIST_UPDATE -> l.onPlayerListUpdate(message.players);
                case GAME_START -> l.onGameStart();
                case GAME_PAUSED -> l.onGamePaused();
                case GAME_RESUMED -> l.onGameResumed();
                case WIN -> l.onWin(message.args != null ? message.args : "");
                case LEADERBOARD -> l.onLeaderBoard(message.leaderboard);
                default -> { }
            }
        });
    }
}

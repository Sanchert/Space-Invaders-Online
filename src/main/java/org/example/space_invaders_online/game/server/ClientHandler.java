package org.example.space_invaders_online.game.server;

import com.google.gson.Gson;
import org.example.space_invaders_online.game.client.Request;

import java.io.*;
import java.net.Socket;

public class ClientHandler implements Runnable {
    private final Socket socket;
    private final int playerId;
    private final Server server;
    private volatile boolean connected = true;
    private final PrintWriter out;
    private final BufferedReader in;
    private final Gson json = new Gson();

    public ClientHandler(Socket socket, int playerId, Server server) throws IOException {
        this.socket = socket;
        this.playerId = playerId;
        this.server = server;

        this.out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())));
        this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    }

    public final int getPlayerId() {
        return this.playerId;
    }

    @Override
    public void run() {
        try {
            while (connected) {
                String inputLine = in.readLine();
                if (inputLine == null) {
                    break;
                }
                Request request = json.fromJson(inputLine, Request.class);
                server.handleClientRequest(playerId, request);
            }
        } catch (EOFException e) {
            System.out.println("[SERVER] client " + playerId + " closed connection.");
        } catch (IOException e) {
            System.out.println("[SERVER] client " + playerId + " disconnected: " + e.getMessage());
        } finally {
            connected = false;
            server.removeClient(playerId);
            try {
                socket.close();
            } catch (IOException e) {
                System.out.println("[SERVER] " + e.getMessage());
            }
        }
    }

    public void sendMessage(String message) throws IOException {
        if (connected && out != null) {
            out.println(message);
            out.flush();
        }
    }
}

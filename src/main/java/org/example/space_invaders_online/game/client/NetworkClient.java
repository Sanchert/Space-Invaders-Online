package org.example.space_invaders_online.game.client;

import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.net.Socket;

public class NetworkClient {
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private INetworkListener listener;
    private final Gson gson = new Gson();

    public void connect(String host, int port ) {

    }

    public void setListener(INetworkListener l) {
        this.listener = l;
    }

    public void send(Request request) {

    }

    public void disconnect() {

    }
}

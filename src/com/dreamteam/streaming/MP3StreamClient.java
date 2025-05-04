package com.dreamteam.streaming;

import javazoom.jl.player.advanced.AdvancedPlayer;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.net.Socket;

/**
 * Client di streaming MP3.
 * Si connette al server e riproduce l'MP3 ricevuto via socket.
 */
public class MP3StreamClient {
    private static final String SERVER_IP = "localhost";
    private static final int SERVER_PORT = 5000;

    public static void main(String[] args) {
        try (Socket socket = new Socket(SERVER_IP, SERVER_PORT);
             InputStream in = new BufferedInputStream(socket.getInputStream())) {

            System.out.println("[Client] Connesso al server " + SERVER_IP);

            AdvancedPlayer player = new AdvancedPlayer(in);
            player.play();

            System.out.println("[Client] Riproduzione completata");
        } catch (Exception e) {
            System.err.println("[Client] Errore durante la riproduzione: " + e.getMessage());
        }
    }
}

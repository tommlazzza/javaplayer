package com.dreamteam.streaming;

import javazoom.jl.player.advanced.AdvancedPlayer;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Server di streaming MP3.
 * Legge un file MP3 e lo invia ai client connessi tramite socket.
 */
public class MP3StreamServer {
    private static final int PORT = 5000;
    private static final String MP3_FILE_PATH = "songs/FENTANYL.mp3";

    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = new ServerSocket(PORT);
        System.out.println("[Server] In ascolto sulla porta " + PORT);

        while (true) {
            Socket clientSocket = serverSocket.accept();
            System.out.println("[Server] Client connesso: " + clientSocket.getInetAddress());

            Thread clientThread = new Thread(() -> {
                try (BufferedInputStream fileStream = new BufferedInputStream(new FileInputStream(MP3_FILE_PATH));
                     BufferedOutputStream out = new BufferedOutputStream(clientSocket.getOutputStream())) {

                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    while ((bytesRead = fileStream.read(buffer)) != -1) {
                        out.write(buffer, 0, bytesRead);
                        out.flush();
                    }

                    System.out.println("[Server] Streaming completato per " + clientSocket.getInetAddress());
                } catch (IOException e) {
                    System.err.println("[Server] Errore streaming: " + e.getMessage());
                } finally {
                    try { clientSocket.close(); } catch (IOException ignored) {}
                }
            });

            clientThread.start();
        }
    }
}
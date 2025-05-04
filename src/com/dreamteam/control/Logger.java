package com.dreamteam.control;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public abstract class Logger {
    private static File logFile;
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final DateTimeFormatter FILE_NAME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");

    /**
     * Inizializza un nuovo file di log con nome univoco.
     */
    public static void initLog() {
        String timestamp = LocalDateTime.now().format(FILE_NAME_FORMATTER);
        logFile = new File("logs/log_" + timestamp + ".txt");

        File parentDir = logFile.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs();
        }

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(logFile, true))) {
            writer.write("[Logger inizializzato alle " + LocalTime.now().format(TIME_FORMATTER) + "]");
            writer.newLine();
        } catch (IOException e) {
            System.err.println("Errore durante l'inizializzazione del log: " + e.getMessage());
        }
    }

    /**
     * Scrive un messaggio nel file di log con timestamp.
     *
     * @param message Il messaggio da scrivere nel log.
     */
    public static void writeLog(String message) {
        if (logFile == null) initLog(); // fallback in caso non inizializzato

        String time = LocalTime.now().format(TIME_FORMATTER);
        String logEntry = "[" + time + "] " + message;

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(logFile, true))) {
            writer.write(logEntry);
            writer.newLine();
        } catch (IOException e) {
            System.err.println("Errore durante la scrittura del log: " + e.getMessage());
        }
    }
}

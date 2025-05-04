package com.dreamteam.control;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public abstract class Logger {
    private static final String LOG_FILE_PATH = "logs/log.txt";
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");

    /**
     * Inizializza il file di log.
     * Crea la directory e il file di log se non esistono, oppure lo svuota se già presente.
     * Scrive un’intestazione iniziale con l’ora corrente.
     */
    public static void initLog() 
    {
        File logFile = new File(LOG_FILE_PATH);

        File parentDir = logFile.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs();
        }

        // Crea o svuota il file all'avvio
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(logFile, false))) {
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
        File logFile = new File(LOG_FILE_PATH);

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

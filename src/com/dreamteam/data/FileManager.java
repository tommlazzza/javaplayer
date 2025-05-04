package com.dreamteam.data;

import com.dreamteam.control.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;

public class FileManager {
    public static Map<String, Integer> loadCounts(File file) {
        Map<String, Integer> map = new LinkedHashMap<>();
        if (file.exists()) {
            try {
                List<String> lines = Files.readAllLines(file.toPath());
                for (String line : lines) {
                    if (!line.trim().isEmpty() && line.contains("=")) {
                        String[] parts = line.split("=", 2);
                        map.put(parts[0].trim(), Integer.parseInt(parts[1].trim()));
                    }
                }
            } catch (IOException | NumberFormatException e) {
                Logger.writeLog("Errore nel caricamento dei dati da " + file.getName() + ": " + e.getMessage());
            }
        }
        return map;
    }

    public static void saveCounts(Map<String, Integer> map, File file) {
        List<String> lines = new LinkedList<>();
        for (Map.Entry<String, Integer> entry : map.entrySet()) {
            lines.add(entry.getKey() + "=" + entry.getValue());
        }
        try {
            Files.write(file.toPath(), lines);
        } catch (IOException e) {
            Logger.writeLog("Errore nel salvataggio dei dati su " + file.getName() + ": " + e.getMessage());
        }
    }

    public static boolean copyFile(File source, File dest) {
        try {
            Files.copy(source.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
            return true;
        } catch (IOException e) {
            Logger.writeLog("Errore nel copiare file: " + e.getMessage());
            return false;
        }
    }

    public static void deleteFolder(File folder) {
        if (folder.exists() && folder.isDirectory()) {
            for (File file : folder.listFiles()) file.delete();
            folder.delete();
        }
    }
}

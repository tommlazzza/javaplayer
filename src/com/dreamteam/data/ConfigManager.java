package com.dreamteam.data;

import com.dreamteam.control.Logger;
import com.dreamteam.control.Mode;
import com.dreamteam.languages.Languages;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Properties;

public class ConfigManager {
    public static final String CONFIG_PATH = "config.properties";
    public static final String THEME_CONFIG_PATH = "resources/theme.config";
    public static final String PLAYBACK_MODE_CONFIG_PATH = "resources/playbackMode.config";

    public static Languages loadLanguageFromConfig() {
        try {
            File configFile = new File(CONFIG_PATH);
            if (configFile.exists()) {
                Properties config = new Properties();
                config.load(new FileInputStream(configFile));
                String lang = config.getProperty("language");
                if (lang != null) {
                    Logger.writeLog("Lingua caricata: " + Languages.valueOf(lang));
                    return Languages.valueOf(lang);
                }
            }
        } catch (Exception e) {
            Logger.writeLog("Errore nel caricamento del file di lingua");
        }
        Logger.writeLog("Lingua caricata: " + Languages.ITALIANO);
        return Languages.ITALIANO;
    }

    public static boolean loadThemeFromConfig() {
        try {
            File config = new File(THEME_CONFIG_PATH);
            if (config.exists()) {
                String value = Files.readString(config.toPath()).trim();
                Logger.writeLog("Tema caricato dal file config");
                return value.equalsIgnoreCase("dark");
            }
        } catch (IOException e) {}
        return false;
    }

    public static Mode loadModeFromConfig() {
        File config = new File(PLAYBACK_MODE_CONFIG_PATH);
        try {
            String value = Files.readString(config.toPath()).trim();
            Logger.writeLog("Modalità di riproduzione caricata dal file config");
            return Mode.valueOf(value);
        } catch (IOException | IllegalArgumentException e) {
            return null;
        }
    }

    public static void saveTheme(boolean isDarkMode) {
        try {
            File config = new File(THEME_CONFIG_PATH);
            Files.writeString(config.toPath(), isDarkMode ? "dark" : "light");
        } catch (IOException e) {
            Logger.writeLog("Errore nel salvataggio del tema: " + e.getMessage());
        }
    }

    public static void savePlaybackMode(Mode mode) {
        File config = new File(PLAYBACK_MODE_CONFIG_PATH);
        try {
            Files.writeString(config.toPath(), mode.name());
        } catch (IOException e) {
            Logger.writeLog("Errore nel salvataggio della modalità di riproduzione: " + e.getMessage());
        }
    }
}

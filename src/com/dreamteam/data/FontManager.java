package com.dreamteam.data;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.Enumeration;

public class FontManager {

    public static void setGlobalFont(String fontPath, float size) {
        try {
            // Carica il font personalizzato
            Font customFont = Font.createFont(Font.TRUETYPE_FONT, new File(fontPath)).deriveFont(size);

            // Applica il font a tutti i componenti UIManager
            Enumeration<Object> keys = UIManager.getDefaults().keys();
            while (keys.hasMoreElements()) {
                Object key = keys.nextElement();
                Object value = UIManager.get(key);
                if (value instanceof Font) {
                    UIManager.put(key, customFont);
                }
            }

        } catch (FontFormatException | IOException e) {
            System.err.println("Errore caricamento font: " + e.getMessage());
        }
    }
}
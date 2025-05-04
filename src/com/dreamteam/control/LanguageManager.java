package com.dreamteam.control;

import java.util.Locale;
import java.util.ResourceBundle;

public abstract class LanguageManager {
    private static ResourceBundle bundle;

    /**
     * Carica il file di lingua corrispondente alla lingua specificata.
     * Usa un {@link ResourceBundle} localizzato in base al valore dell'enum {@link Languages}.
     *
     * @param lang La lingua da caricare.
     */
    public static void load(Languages lang) {
        Locale locale;
        switch (lang) {
            case ENGLISH:
                locale = Locale.ENGLISH;
                break;
            case GIAPPONESE:
                locale = Locale.JAPANESE;
                break;
            case FRANCESE:
            	locale = Locale.FRENCH;
            	break;
            case TEDESCO:
            	locale = Locale.GERMAN;
            	break;
            case COREANO:
            	locale = Locale.KOREAN;
            	break;
            case CINESE:
            	locale = Locale.TRADITIONAL_CHINESE;
            	break;
            default:
                locale = Locale.ITALIAN;
        }
        bundle = ResourceBundle.getBundle("com.dreamteam.languages.lang", locale);
    }

    /**
     * Restituisce la stringa localizzata associata alla chiave specificata.
     *
     * @param key La chiave della stringa nel file di lingua.
     * @return La stringa tradotta corrispondente alla chiave.
     */
    public static String get(String key) {
        return bundle.getString(key);
    }
}
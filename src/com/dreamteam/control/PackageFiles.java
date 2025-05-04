package com.dreamteam.control;

import java.io.File;

import com.dreamteam.model.Song;

public class PackageFiles {
    private final String path = "songs/";

    /**
     * Costruttore di default.
     * Inizializza il percorso della cartella contenente i file MP3.
     */
    public PackageFiles() {}

    /**
     * Restituisce un array di oggetti {@link Song} rappresentanti i file MP3 nella cartella "songs/".
     * Ogni nome di file viene convertito in un titolo leggibile (sostituendo underscore e rimuovendo l'estensione).
     *
     * @return Array di canzoni trovate nella directory, oppure vuoto se non valida o nessun file trovato.
     */
    public Song[] getSongs() {
        File packageDir = new File(path);

        if (!packageDir.exists() || !packageDir.isDirectory()) {
            System.out.println("Directory 'songs/' non trovata o non valida");
            return new Song[0];
        }

        String[] files = packageDir.list((dir, name) -> name.toLowerCase().endsWith(".mp3"));
        if (files == null) return new Song[0];

        Song[] songs = new Song[files.length];

        for (int i = 0; i < files.length; i++) {
            songs[i] = new Song(files[i].replace('_', ' ').replace(".mp3", " "), files[i]);
        }

        return songs;
    }

    /**
     * Cerca e restituisce una canzone all'interno della cartella "songs/" in base al titolo.
     *
     * @param title Il titolo della canzone da cercare.
     * @return La canzone corrispondente, oppure null se non trovata.
     */
    public Song getSongByTitle(String title) {
        Song[] songs = getSongs();

        for (Song song : songs) {
            if (song.getTitle().trim().equalsIgnoreCase(title.trim())) {
                return song;
            }
        }

        return null;
    }
}

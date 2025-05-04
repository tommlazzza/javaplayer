package com.dreamteam.tools.model;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.dreamteam.model.Playlist;
import com.dreamteam.model.Song;

/**
 * La classe {@code PlaylistCreatorTool} rappresenta il modello logico dello strumento
 * di creazione delle playlist nell'applicazione Sonora.
 * <p>
 * Gestisce i dati temporanei inseriti dall'utente (nome playlist, copertina e brani)
 * prima che vengano convertiti in un oggetto {@link Playlist} salvabile e persistente.
 * </p>
 * 
 * <p>Le canzoni vengono gestite come file {@link File}, da cui vengono derivati 
 * i titoli. I percorsi salvati sono assoluti.</p>
 * 
 * @author DreamTeam
 * @see com.dreamteam.model.Playlist
 * @see com.dreamteam.model.Song
 */
public class PlaylistCreatorTool {

    private String playlistName;
    private String coverImagePath;
    private List<File> songFiles;

    /**
     * Crea una nuova istanza di {@code PlaylistCreatorTool}.
     * Inizializza la lista interna dei file MP3.
     */
    public PlaylistCreatorTool() {
        songFiles = new ArrayList<>();
    }

    /**
     * Imposta il nome della playlist.
     *
     * @param name Nome da assegnare alla playlist.
     */
    public void setPlaylistName(String name) {
        this.playlistName = name;
    }

    /**
     * Restituisce il nome corrente della playlist.
     *
     * @return Nome della playlist.
     */
    public String getPlaylistName() {
        return playlistName;
    }

    /**
     * Imposta il percorso della copertina selezionata.
     *
     * @param path Percorso assoluto dell'immagine.
     */
    public void setCoverImagePath(String path) {
        this.coverImagePath = path;
    }

    /**
     * Restituisce il percorso della copertina selezionata.
     *
     * @return Percorso della copertina.
     */
    public String getCoverImagePath() {
        return coverImagePath;
    }

    /**
     * Aggiunge un singolo file MP3 alla lista dei brani,
     * evitando duplicati.
     *
     * @param file File MP3 da aggiungere.
     */
    public void addSongFile(File file) {
        if (!songFiles.contains(file)) {
            songFiles.add(file);
        }
    }

    /**
     * Aggiunge più file MP3 alla lista dei brani.
     *
     * @param files Array di file da aggiungere.
     */
    public void addSongFiles(File[] files) {
        for (File f : files) {
            addSongFile(f);
        }
    }

    /**
     * Rimuove un file dalla lista dei brani.
     *
     * @param file File da rimuovere.
     */
    public void removeSongFile(File file) {
        songFiles.remove(file);
    }

    /**
     * Restituisce la lista dei file MP3 attualmente selezionati.
     *
     * @return Lista di file.
     */
    public List<File> getSongFiles() {
        return songFiles;
    }

    /**
     * Converte lo stato corrente del tool in un oggetto {@link Playlist}.
     * I titoli dei brani sono dedotti dai nomi dei file, e i percorsi sono assoluti.
     *
     * @return Oggetto {@link Playlist} costruito.
     */
    public Playlist toPlaylist() {
        Playlist pl = new Playlist();
        pl.setName(playlistName);
        if (coverImagePath != null) {
            pl.setCoverImage(coverImagePath);
        }

        for (File f : songFiles) {
            String title = f.getName().replaceAll("\\.mp3$", "").replace('_', ' ').trim();
            pl.addSong(new Song(title, f.getAbsolutePath()));
        }

        return pl;
    }
}

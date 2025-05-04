package com.dreamteam.model;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.dreamteam.data.PackageFilesManager;

public class Playlist implements Serializable {
    private static final long serialVersionUID = 1L;
    
    public final static String DEFAULT_IMAGE_PATH = "img/default_cover.jpg";

    private String name;

    private List<Song> songs;
    private List<Song> originalOrder;
    private List<Song> customOrder;
    private String coverImagePath; // Percorso copertina

    /**
     * Costruttore di default. Inizializza una playlist vuota con immagine di copertina predefinita.
     */
    public Playlist() {
        songs = new ArrayList<>();
        originalOrder = new ArrayList<>();
        customOrder = new ArrayList<>();
        coverImagePath = DEFAULT_IMAGE_PATH; // Percorso di default
    }

    /**
     * Aggiunge una canzone alla playlist cercandola tra quelle disponibili nel package.
     * Se trovata, la aggiunge e ordina l'elenco per titolo (case insensitive).
     *
     * @param displayTitle Il titolo della canzone da aggiungere (formattato visivamente).
     */
    public void addSong(String title) {
        Song s = new Song(title, title.replace("_", " ") + ".mp3");
        songs.add(s);
        originalOrder.add(s);
    }

    public void addSong(Song song)
    {
    	songs.add(song);
    	originalOrder.add(song);
    }

    /**
     * Rimuove una canzone dalla playlist in base al titolo visualizzato.
     *
     * @param displayTitle Il titolo della canzone da rimuovere.
     */
    public void removeSong(String displayTitle) {
        String trimmedDisplayTitle = displayTitle.trim();
        songs.removeIf(song -> song.getTitle().trim().replace('_', ' ').equalsIgnoreCase(trimmedDisplayTitle));
        originalOrder.removeIf(song -> song.getTitle().trim().replace('_', ' ').equalsIgnoreCase(trimmedDisplayTitle));
    }

    /**
     * Restituisce l'oggetto Song corrispondente al titolo dato, se presente nella playlist.
     *
     * @param title Il titolo della canzone da cercare.
     * @return La canzone corrispondente, oppure null se non trovata.
     */
    public Song getSong(String title) {
        String trimmedTitle = title.trim();
        for (Song song : songs) {
            String songTitle = song.getTitle().trim().replace('_', ' ');
            if (songTitle.equalsIgnoreCase(trimmedTitle)) {
                return song;
            }
        }
        return null;
    }
    /*
    public Song getSong(String title) {
        String trimmedTitle = title.trim();
        for (Song song : songs) {
            String songTitle = song.getTitle().trim().replace('_', ' ');
            if (songTitle.equalsIgnoreCase(trimmedTitle)) {
                return song;
            }
        }
        return null;
    }
    */

    /**
     * Restituisce un array con i titoli di tutte le canzoni nella playlist.
     *
     * @return Array di titoli delle canzoni.
     */
    public String[] getSongTitles() {
        return songs.stream().map(Song::getTitle).toArray(String[]::new);
    }
    /*
    public String[] getSongTitles() {
        String[] names = new String[songs.size()];
        for (int i = 0; i < songs.size(); i++) {
            names[i] = songs.get(i).getTitle().trim().replace('_', ' ');
        }
        return names;
    }
    */

    /**
     * Restituisce un array con tutti i titoli dei file MP3 presenti nella cartella "songs".
     *
     * @return Array di titoli di canzoni disponibili nel file system.
     */
    public String[] getAllSongs() {
        File songsFolder = new File("songs");
        File[] files = songsFolder.listFiles((dir, name) -> name.toLowerCase().endsWith(".mp3"));

        if (files == null || files.length == 0) return new String[0];

        String[] titles = new String[files.length];
        for (int i = 0; i < files.length; i++) {
            String filename = files[i].getName();
            titles[i] = filename.substring(0, filename.length() - 4).replace('_', ' ').trim();
        }
        return titles;
    }

    /**
     * Restituisce la lista di oggetti Song presenti nella playlist.
     *
     * @return Lista di canzoni.
     */
    public List<Song> getSongs() {
        return songs;
    }
    
    /**
     * Imposta la lista di canzoni per questa playlist e la ordina alfabeticamente per titolo.
     *
     * @param songs Lista di canzoni da impostare.
     */
    public void setSongs(ArrayList<Song> songs) {
        this.songs = songs;
        this.songs.sort(Comparator.comparing(Song::getTitle, String.CASE_INSENSITIVE_ORDER));
    }

    /**
     * Restituisce il percorso dell'immagine di copertina della playlist.
     *
     * @return Percorso della copertina.
     */
    public String getCoverImage() {
        return coverImagePath;
    }

    /**
     * Imposta il percorso dell'immagine di copertina della playlist.
     *
     * @param path Percorso della nuova immagine di copertina.
     */
    public void setCoverImage(String path) {
        this.coverImagePath = path;
    }
    
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
    
    public List getOriginalOrder()
    {
    	return originalOrder;
    }
    
    public void setOriginalOrder(List order)
    {
    	originalOrder = order;
    }
    
    /**
     * Ordina le canzoni alfabeticamente.
     */
    public void sortSongsAlphabetically() {
        songs.sort(Comparator.comparing(Song::getTitle, String.CASE_INSENSITIVE_ORDER));
    }

    /**
     * Ripristina l'ordine originale di inserimento.
     */
    public void sortSongsOriginalOrder() {
        songs = new ArrayList<>(originalOrder);
    }
    
    /**
     * Imposta l'ordine personalizzato (titoli) e lo applica a songs.
     */
    public void setCustomOrder(List<Song> order) {
        this.customOrder = new ArrayList<>(order);
        applyCustomOrder();
    }
    
    /** Ordina songs secondo customOrder */
    private void applyCustomOrder() {
        this.songs = customOrder;
    }
}

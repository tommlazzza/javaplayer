package com.dreamteam.data;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import com.dreamteam.control.Logger;
import com.dreamteam.model.Playlist;
import com.dreamteam.model.Song;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

public abstract class PlaylistDataManager {
    private static final String FILE_PATH = "resources/playlists.json";
    private static final String BASE_DIR = "resources/playlists";
    private final static String PLAYLIST_ALL_ICON = "resources/playlist.png";
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    /**
     * Salva una singola playlist nella sua cartella dedicata.
     * @param name Nome della playlist (usato anche per la cartella).
     * @param playlist Oggetto playlist da salvare.
     */
    public static void savePlaylist(String name, Playlist playlist) {
        File dir = new File(BASE_DIR, name);
        if (!dir.exists()) dir.mkdirs();

        File jsonFile = new File(dir, "data.json");
        try (FileWriter writer = new FileWriter(jsonFile)) {
            gson.toJson(playlist, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Salva le playlist in cartelle.
     */
    public static void savePlaylists(Map<String, Playlist> playlists) {
        File baseDir = new File(BASE_DIR);
        if (!baseDir.exists()) baseDir.mkdirs();

        for (Map.Entry<String, Playlist> entry : playlists.entrySet()) {
            String name = entry.getKey();

            // Salta la playlist "Tutti i brani"
            if (!name.equals(LanguageManager.get("playlist.all"))) {
                Playlist playlist = entry.getValue();

                // Crea cartella per la playlist
                File playlistDir = new File(baseDir, name);
                if (!playlistDir.exists()) playlistDir.mkdirs();

                // Salva JSON della playlist
                File jsonFile = new File(playlistDir, "data.json");
                try (FileWriter writer = new FileWriter(jsonFile)) {
                    gson.toJson(playlist, writer);
                } catch (IOException e) {
                    Logger.writeLog("Errore nel salvataggio JSON per playlist " + name + ": " + e.getMessage());
                }

                // Copia e rinomina copertina (opzionale)
                if (playlist.getCoverImage() != null) {
                    File coverSrc = new File(playlist.getCoverImage());
                    if (coverSrc.exists()) {
                        File coverDest = new File(playlistDir, "cover.jpg");
                        try {
                            Files.copy(coverSrc.toPath(), coverDest.toPath(), StandardCopyOption.REPLACE_EXISTING);
                            playlist.setCoverImage(coverDest.getPath()); // aggiorna percorso
                        } catch (IOException e) {
                            Logger.writeLog("Errore nel salvare la cover per " + name + ": " + e.getMessage());
                        }
                    }
                }
            }
        }
    }
    
    /**
     * Salva le playlist in formato JSON.
     */
    public static void savePlaylistsJSON(Map<String, Playlist> playlists) {
        try {
            File dir = new File("data");
            if (!dir.exists()) dir.mkdirs();

            try (FileWriter writer = new FileWriter(FILE_PATH)) {
                gson.toJson(playlists, writer);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Carica le playlist da un file JSON.
     */
    public static Map<String, Playlist> loadPlaylists() {
        File file = new File(FILE_PATH);
        if (!file.exists()) return new HashMap<>();

        try (FileReader reader = new FileReader(file)) {
        	Map<String, Playlist> playlists = gson.fromJson(reader,
        		    new TypeToken<Map<String, Playlist>>() {}.getType());

            // fallback: imposta name se mancante
        	for (Map.Entry<String, Playlist> e : playlists.entrySet()) {
        	    Playlist pl = e.getValue();
        	    
        	    if (pl.getName() == null || pl.getName().isBlank()) {
        	        pl.setName(e.getKey());
        	    }
        	}

            return playlists;

        } catch (IOException e) {
            e.printStackTrace();
            return new HashMap<>();
        }
    }
    
    /**
     * Carica tutte le playlist presenti nelle cartelle sotto /resources/playlists.
     * @return Mappa delle playlist caricate (nome -> oggetto Playlist).
     */
    public static Map<String, Playlist> loadPlaylistsFromFolders() {
        Map<String, Playlist> playlists = new LinkedHashMap<>();

        File baseDir = new File(BASE_DIR);
        if (!baseDir.exists()) baseDir.mkdirs();

        File[] dirs = baseDir.listFiles(File::isDirectory);
        if (dirs == null) return playlists;

        for (File dir : dirs) {
            File jsonFile = new File(dir, "data.json");
            if (jsonFile.exists()) {
                try (FileReader reader = new FileReader(jsonFile)) {
                    Playlist pl = gson.fromJson(reader, Playlist.class);
                    if (pl != null) {
                        playlists.put(pl.getName(), pl);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return playlists;
    }
    
    /**
     * Elimina fisicamente una playlist e la sua cartella.
     * @param name Nome della playlist da eliminare.
     */
    public static void deletePlaylist(String name) {
        File playlistDir = new File(BASE_DIR, name);
        if (playlistDir.exists()) {
            for (File file : playlistDir.listFiles()) file.delete();
            playlistDir.delete();
        }
    }
    
    /**
     * Esporta una playlist
     * 
     * @param playlist
     * @param destDir
     * @throws IOException
     */
    public static void exportPlaylist(Playlist playlist, File destDir) throws IOException {
        if (!destDir.exists()) destDir.mkdirs();

        File jsonFile = new File(destDir, "data.json");
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        try (FileWriter writer = new FileWriter(jsonFile)) {
            gson.toJson(playlist, writer);
        }

        // Copia i file MP3 associati
        for (String title : playlist.getSongTitles()) {
            Song song = playlist.getSong(title);
            if (song != null) {
                File original = new File(song.getPath());
                File target = new File(destDir, original.getName());
                if (!target.exists()) {
                    Files.copy(original.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }

        // Copia copertina se esiste
        if (playlist.getCoverImage() != null) {
            File cover = new File(playlist.getCoverImage());
            if (cover.exists()) {
                File dest = new File(destDir, "cover.jpg");
                Files.copy(cover.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }

    /**
     * Importa una playlist 
     * 
     * @param jsonFile
     * @return
     * @throws IOException
     */
    public static Playlist importPlaylist(File jsonFile) throws IOException {
        Gson gson = new Gson();
        try (FileReader reader = new FileReader(jsonFile)) {
            Playlist pl = gson.fromJson(reader, Playlist.class);

            File dir = jsonFile.getParentFile();

            for (String title : pl.getSongTitles()) {
                Song song = pl.getSong(title);
                if (song != null) {
                    File songFile = new File(dir, new File(song.getPath()).getName());
                    File dest = new File("resources/playlists/" + songFile.getName());
                    if (!dest.exists() && songFile.exists()) {
                        Files.copy(songFile.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    }
                    song.setPath(dest.getPath());
                }
            }

            // Copia la cover se presente
            File coverFile = new File(dir, "cover.jpg");
            if (coverFile.exists()) {
                File destCover = new File("resources/playlists/" + pl.getName() + "/cover.jpg");
                destCover.getParentFile().mkdirs();
                Files.copy(coverFile.toPath(), destCover.toPath(), StandardCopyOption.REPLACE_EXISTING);
                pl.setCoverImage(destCover.getPath());
            }

            savePlaylist(pl.getName(), pl);
            return pl;
        }
    }
    
    public static Playlist creaPlaylistTuttiIBrani() {
        Playlist tutti = new Playlist();
        LinkedHashMap<String, Song> unici = new LinkedHashMap<>();

        // Carica brani da tutte le playlist esistenti
        Map<String, Playlist> playlists = loadPlaylistsFromFolders();
        for (Playlist pl : playlists.values()) {
            if (pl.getName().equals(LanguageManager.get("playlist.all"))) continue;

            for (String title : pl.getSongTitles()) {
                Song s = pl.getSong(title);
                if (s != null && new File(s.getPath()).exists()) {
                    unici.putIfAbsent(title, s);
                }
            }
        }

        // Carica tutti i file MP3 presenti nelle sottocartelle di resources/playlists
        File baseDir = new File("resources/playlists");
        if (baseDir.exists()) {
            caricaMP3Ricorsivamente(baseDir, unici);
        }

        // Aggiungi tutti i brani alla playlist "Tutti i Brani"
        unici.entrySet().stream()
            .sorted(Map.Entry.comparingByKey(String.CASE_INSENSITIVE_ORDER))
            .forEach(entry -> tutti.addSong(entry.getValue()));

        tutti.setName(LanguageManager.get("playlist.all"));
        tutti.setCoverImage(PLAYLIST_ALL_ICON);
        
        return tutti;
    }
    
    private static void caricaMP3Ricorsivamente(File dir, Map<String, Song> unici) {
        File[] files = dir.listFiles();
        if (files == null) return;

        for (File file : files) {
            if (file.isDirectory()) {
                caricaMP3Ricorsivamente(file, unici);
            } else if (file.getName().toLowerCase().endsWith(".mp3")) {
                String title = file.getName().substring(0, file.getName().length() - 4)
                    .replace('_', ' ').trim();
                if (!unici.containsKey(title)) {
                    Song s = new Song(title, file.getPath());
                    unici.put(title, s);
                }
            }
        }
    }
}
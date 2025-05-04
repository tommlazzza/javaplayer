package com.dreamteam.model;

import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

public class SongAPI {
    private String databasePath;
    
    public SongAPI(String dbPath) {
        this.databasePath = dbPath;
    }
    
    public ArrayList<Song> importSongs() {
        ArrayList<Song> songs = new ArrayList<>();
        
        try {
            File dbDir = new File(databasePath);
            if (!dbDir.exists() || !dbDir.isDirectory()) {
                System.out.println("Database directory not found: " + databasePath);
                return songs;
            }
            
            // Cerca nella collezione "songs" di SleekDB
            File songsCollection = new File(dbDir, "songs");
            File dataDir = new File(songsCollection, "data");
            
            if (dataDir.exists() && dataDir.isDirectory()) {
                File[] dataFiles = dataDir.listFiles((dir, name) -> name.endsWith(".json"));
                
                if (dataFiles != null) {
                    for (File dataFile : dataFiles) {
                        JSONParser parser = new JSONParser();
                        JSONObject songData = (JSONObject) parser.parse(new FileReader(dataFile));
                        
                        String title = (String) songData.get("title");
                        String filename = (String) songData.get("filename");
                        
                        if (title != null && filename != null) {
                            songs.add(new Song(title, filename));
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return songs;
    }
}
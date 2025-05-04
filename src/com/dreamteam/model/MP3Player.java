package com.dreamteam.model;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import javazoom.jl.player.advanced.AdvancedPlayer;

public class MP3Player {
    private AdvancedPlayer player;
    private FileInputStream fileInputStream;
    private Song currentSong;
    private long pausedPosition;
    private long fileSize;
    private boolean isPaused;
    private Thread playbackThread;

    /**
     * Costruttore del player MP3.
     * Inizializza i campi con valori di default e imposta lo stato su "pausato".
     */
    public MP3Player() {
        fileSize = 0;
        pausedPosition = 0;
        isPaused = true;
    }

    /**
     * Riproduce una canzone da capo.
     * Ferma eventuali riproduzioni in corso, avvia un nuovo thread e riproduce la canzone specificata.
     *
     * @param song La canzone da riprodurre.
     */
    public void play(Song song) {
        stop();

        currentSong = song;
        pausedPosition = 0;
        isPaused = false;

        playbackThread = new Thread(() -> {
            try {
                File file = new File(song.getPath());
                fileSize = file.length();
                fileInputStream = new FileInputStream(file);
                player = new AdvancedPlayer(fileInputStream);

                player.play();

            } 
            catch (Exception e) {} 
            finally {
                player = null;
            }
        });

        playbackThread.start();
    }

    /**
     * Riprende la riproduzione da dove era stata interrotta (messa in pausa).
     */
    public void resume() {
        if (!isPaused || currentSong == null) return;

        isPaused = false;

        Thread thread = new Thread(() -> {
            try {
                File file = new File(currentSong.getPath());
                fileInputStream = new FileInputStream(file);
                fileInputStream.skipNBytes(pausedPosition);

                AdvancedPlayer resumedPlayer = new AdvancedPlayer(fileInputStream);
                player = resumedPlayer;

                resumedPlayer.play();

                player = null;

            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        playbackThread = thread;
        thread.start();
    }

    /**
     * Mette in pausa la riproduzione corrente, salvando la posizione attuale nel file.
     */
    public void pause() {
        synchronized (this) {
            if (player != null && !isPaused) {
                try {
                    pausedPosition = fileSize - fileInputStream.available();
                    isPaused = true;
                    player.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Ferma completamente la riproduzione corrente, chiude il file e il player,
     * interrompe il thread attivo se necessario.
     */
    public void stop() {
        Thread current = Thread.currentThread();
        Thread oldThread = playbackThread;

        playbackThread = null;

        if (player != null) {
            player.close();
            player = null;
        }

        if (fileInputStream != null) {
            try {
                fileInputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                fileInputStream = null;
            }
        }

        isPaused = true;

        if (oldThread != null && oldThread.isAlive() && oldThread != current) {
            oldThread.interrupt();
            try {
                oldThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Salta a una posizione percentuale all'interno della traccia.
     * Chiude il player corrente e ne avvia uno nuovo dalla posizione specificata.
     *
     * @param percentage La percentuale di avanzamento (0–100) a cui saltare.
     */
    public void seekToPercentage(int percentage) {
        if (currentSong == null || percentage < 0 || percentage > 100) return;

        stop();

        Thread thread = new Thread(() -> {
            try {
                File file = new File(currentSong.getPath());
                FileInputStream localStream = new FileInputStream(file);
                long skipBytes = (long) ((file.length() / 100.0) * percentage);
                localStream.skipNBytes(skipBytes);

                AdvancedPlayer newPlayer = new AdvancedPlayer(localStream);
                
                synchronized (this) {
                    fileInputStream = localStream;
                    player = newPlayer;
                }

                newPlayer.play();

                synchronized (this) {
                    player = null;
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        synchronized (this) {
            playbackThread = thread;
        }

        playbackThread.start();
        isPaused = false;
    }

    /**
     * Restituisce la percentuale attuale di avanzamento nella canzone.
     *
     * @return Percentuale della canzone già riprodotta.
     */
    public int getCurrentPercentage() throws Exception
    {
        if (fileInputStream == null || currentSong == null) throw new Exception("empty");

        try 
        {
            int available = fileInputStream.available(); 
            int bytesRead = (int) (fileSize - available);
            
            return (int) ((bytesRead * 100L) / fileSize);
        } 
        catch (IOException e) { throw new Exception("empty"); }
    }

    /**
     * Verifica se la canzone è attualmente in riproduzione (thread attivo).
     *
     * @return true se il player è attivo, false altrimenti.
     */
    public boolean isPlaying() {
        return playbackThread != null && playbackThread.isAlive();
    }

	/**
	 * Restituisce la posizione memorizzata nel file al momento della pausa.
	 *
	 * @return Numero di byte letti prima della pausa.
	 */
    public long getPausedPosition() {
        return pausedPosition;
    }

    /**
     * Imposta manualmente lo stato "pausato" del player.
     *
     * @param input true per segnare come in pausa, false altrimenti.
     */
    public void setIsPaused(boolean input) {
        this.isPaused = input;
    }
    
    public boolean isPaused()
    {
    	return isPaused;
    }
    
    /**
     * Restituisce la canzone attualmente in riproduzione.
     *
     * @return Oggetto Song attualmente in riproduzione.
     */
    public Song getCurrentSong()
    {
    	return this.currentSong;
    }
}
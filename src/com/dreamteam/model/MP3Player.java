package com.dreamteam.model;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;

import com.dreamteam.control.Logger;
import com.dreamteam.control.PlaybackManager;
import com.dreamteam.view.Panel;

import javazoom.jl.player.advanced.AdvancedPlayer;

public class MP3Player {
    private AdvancedPlayer player;
    private FileInputStream fileInputStream;
    private Song currentSong;
    private long pausedPosition;
    private long fileSize;
    private boolean isPaused;
    private Thread playbackThread;
    private volatile int currentPercentage;
    private final Object lock;
    private Panel panel;
    private Semaphore semaphore;
    private AtomicBoolean isPlaying = new AtomicBoolean(false);
    private final AtomicBoolean advancing = new AtomicBoolean(false);
    
    /**
     * Costruttore del player MP3.
     * Inizializza i campi con valori di default e imposta lo stato su "pausato".
     */
    public MP3Player() {
    	lock = new Object();
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
    	if (isPlaying.get()) {
    	    return; // già in riproduzione
    	}
    	isPlaying.set(true);
    	
    	Logger.writeLog("MP3Player: play() avviato – " + currentSong.getTitle());
    	
        synchronized(lock)
        {
        	stop();
            currentSong = song;
            pausedPosition = 0;
            isPaused = false;

            Logger.writeLog("MP3Player: inizio riproduzione di \"" + song.getTitle() + "\"");

            playbackThread = new Thread(() -> {
                try {
                    File file = new File(song.getPath());
                    if (!file.exists()) {
                        Logger.writeLog("MP3Player: file non trovato - " + file.getAbsolutePath());
                        return;
                    }

                    fileSize = file.length();
                    fileInputStream = new FileInputStream(file);
                    player = new AdvancedPlayer(fileInputStream);

                    isPlaying.set(true);
                    
                    Logger.writeLog("MP3Player: player creato, avvio riproduzione");

                    // Thread per aggiornare la percentuale
                    Thread monitor = new Thread(() -> {
                        while (!isPaused && player != null) {
                            try {
                                int available = fileInputStream.available();
                                int bytesRead = (int) (fileSize - available);
                                currentPercentage = (int) ((bytesRead * 100L) / fileSize);
                                Thread.sleep(500);
                            } catch (Exception ignored) {
                                break;
                            }
                        }
                    });
                    monitor.start();

                    player.play();

                    Logger.writeLog("MP3Player: riproduzione completata");
                } catch (Exception e) {
                    Logger.writeLog("MP3Player: errore durante play() - " + e.getMessage());
                } finally {
                    Logger.writeLog("MP3Player: riproduzione completata");
                    player = null;
                    fileInputStream = null;
                    
                    /* Rilascio il playbackSemaphore DOPO aver avviato il player
                    SwingUtilities.invokeLater(() -> {
                        if (panel.getController() != null) {
                            panel.getController().getPlaybackSemaphore().release();
                        }
                    });
                    */
                    isPlaying.set(false);
                    
                    Logger.writeLog("MP3Player: riproduzione completata – " + currentSong.getTitle());
                    Logger.writeLog("MP3Player: isPlaying = " + isPlaying.get());

                }
            });

            playbackThread.start();
            Logger.writeLog("MP3Player: thread playback avviato");
        }
    }
    
    /**
     * Riprende la riproduzione da dove era stata interrotta (messa in pausa).
     */
    public void resume() {
        if (!isPaused || currentSong == null) return;

        isPaused = false;
        isPlaying.set(true);

        Thread thread = new Thread(() -> {
            try {
                File file = new File(currentSong.getPath());
                fileInputStream = new FileInputStream(file);
                fileInputStream.skipNBytes(pausedPosition);
                fileSize = file.length();

                player = new AdvancedPlayer(fileInputStream);

                Logger.writeLog("MP3Player: ripresa riproduzione di \"" + currentSong.getTitle() + "\"");

                if (semaphore != null) {
                    semaphore.release();
                    Logger.writeLog("MP3Player: mutex rilasciato dopo resume()");
                }
                
                Thread monitor = new Thread(() -> {
                    while (!isPaused && player != null) {
                        try {
                            int available = fileInputStream.available();
                            int bytesRead = (int) (fileSize - available);
                            currentPercentage = (int) ((bytesRead * 100L) / fileSize);
                            Thread.sleep(500);
                        } catch (Exception ignored) {
                            break;
                        }
                    }
                });
                monitor.start();

                player.play();

                Logger.writeLog("MP3Player: riproduzione completata – " + currentSong.getTitle());
            } catch (Exception e) {
                Logger.writeLog("MP3Player resume(): errore – " + e.getMessage());
            } finally {
                isPlaying.set(false);
                Logger.writeLog("MP3Player: isPlaying = false");
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
                	Logger.writeLog(e.getMessage());
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
            	Logger.writeLog(e.getMessage());
            } finally {
                fileInputStream = null;
            }
        }

        isPaused = true;
        currentPercentage = 0;

        if (oldThread != null && oldThread.isAlive() && oldThread != current) {
            oldThread.interrupt();
            try {
                oldThread.join();
            } catch (InterruptedException e) {
            	Logger.writeLog(e.getMessage());
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
            	Logger.writeLog(e.getMessage());
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
    public int getCurrentPercentage() {
        return currentPercentage;
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
    
    public void setPanel(Panel panel)
    {
    	this.panel = panel;
    }
    
    public void setSemaphore(Semaphore s) {
        this.semaphore = s;
    }
    
    public AtomicBoolean getIsPlaying()
    {
    	return this.isPlaying;
    }
    
    public void setIsPlaying(boolean value)
    {
    	isPlaying = new AtomicBoolean(value);

    	Logger.writeLog("MP3Player: isPlaying impostato a " + isPlaying.get());

    }

	public void setCurrentSong(Song selected) {
		currentSong = selected;
	}
}
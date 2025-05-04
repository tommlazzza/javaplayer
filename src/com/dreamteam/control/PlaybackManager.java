package com.dreamteam.control;

import java.util.concurrent.Semaphore;

import javax.swing.SwingUtilities;
import javax.swing.event.ChangeListener;

import com.dreamteam.model.MP3Player;
import com.dreamteam.view.Panel;

/**
 * La classe {@code PlaybackManager} è un gestore della riproduzione audio sincronizzato con l'interfaccia utente.
 * Si occupa di monitorare lo stato di avanzamento della canzone attualmente in esecuzione
 * e di aggiornare lo slider grafico in tempo reale.
 * <p>
 * Utilizza un {@link Semaphore} per sincronizzare correttamente l'accesso concorrente alla riproduzione,
 * garantendo che un solo thread di monitoraggio sia attivo per volta.
 * </p>
 * <p>
 * Al termine della canzone, notifica il {@link Controller} per avviare automaticamente la traccia successiva,
 * rispettando la modalità di riproduzione corrente.
 * </p>
 *
 * @author DreamTeam
 */
public class PlaybackManager implements Runnable {
    private final Panel panel;
    private final MP3Player player;
    private static Semaphore mutex;
    private Thread thread;
    private volatile boolean running;

    /**
     * Costruttore della classe {@code PlaybackManager}.
     *
     * @param panel  Il pannello grafico principale.
     * @param mutex  Il semaforo per la sincronizzazione della riproduzione.
     */
    public PlaybackManager(Panel panel, Semaphore mutex) {
        this.panel = panel;
        this.player = null;
        this.mutex = mutex;
        this.running = true;
        this.thread = new Thread(this);
        this.thread.setName("PlaybackManager");
        this.thread.start();
    }

    /**
     * Ferma il thread di monitoraggio e interrompe la sua esecuzione.
     */
    public void stop() {
        running = false;
        thread.interrupt();
    }

    /**
     * Interrompe il thread corrente e ne crea uno nuovo per riprendere il monitoraggio.
     */
    public void interruptAndRestart() {
        thread.interrupt();
        try {
            thread.join(); // attende la fine del vecchio thread
        } catch (InterruptedException e) {
            Logger.writeLog("PlaybackManager: errore in join - " + e.getMessage());
        }

        thread = new Thread(this);
        thread.setName("PlaybackManager");
        thread.start();
        Logger.writeLog("PlaybackManager: nuovo thread avviato dopo interruzione");
    }

    /**
     * Metodo eseguito dal thread. Monitora continuamente la riproduzione e aggiorna lo slider
     * sull'interfaccia. Al termine della canzone, avvia automaticamente quella successiva.
     */
    @Override
    public void run() {
        while (running) {
            try {
                mutex.acquire();
                Logger.writeLog("PlaybackManager: mutex acquisito, inizio monitoraggio slider");

                while (running && panel.getPlayer().isPlaying()) {
                    try {
                        int perc = panel.getPlayer().getCurrentPercentage();
                        Logger.writeLog("PlaybackManager: percentuale attuale = " + perc);
                        SwingUtilities.invokeLater(() -> {
                            ChangeListener[] listeners = panel.getSlider().getChangeListeners();
                            for (ChangeListener l : listeners) panel.getSlider().removeChangeListener(l);

                            panel.getSlider().setValue(perc);

                            for (ChangeListener l : listeners) panel.getSlider().addChangeListener(l);
                        });

                        //if (perc >= 99) break;
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        Logger.writeLog("PlaybackManager: interrotto durante sleep - esco dal ciclo di monitoraggio");
                        break;
                    } catch (Exception e) {
                        Logger.writeLog("PlaybackManager: eccezione durante aggiornamento slider - " + e.getMessage());
                        break;
                    }
                }

                if (running && !panel.getPlayer().isPaused() && panel.getPlayer().isPlaying()) {
                    
                }
                
                SwingUtilities.invokeLater(() -> {
                    Controller controller = panel.getController();
                    
                    if (controller != null) {
                        Logger.writeLog("PlaybackManager: canzone finita, chiamo nextSong()");
                        Logger.writeLog("PlaybackManager: isPlaying prima = " + panel.getPlayer().getIsPlaying().get());

                        // SOLO se isPlaying è false
                        if (!panel.getPlayer().getIsPlaying().get() && !panel.getPlayer().isPaused() && !panel.getController().isSuppressComboBoxPlayback()) {
                            panel.getPlayer().setIsPlaying(false); // lo resettiamo prima
                            controller.nextSong();
                            Logger.writeLog("PlaybackManager: nextSong() chiamato");
                        }
                    }
                });
            } catch (InterruptedException e) {
                Logger.writeLog("PlaybackManager: interrotto in acquire - attesa riproduzione");
            }
        }

        Logger.writeLog("PlaybackManager: thread terminato");
    }
    
    /**
     * Rilascia manualmente il {@link Semaphore} associato alla riproduzione,
     * utile in caso di stop forzato o sincronizzazione.
     */
    public static void release()
    {
    	try {
			mutex.release();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
}
package com.dreamteam.control;

import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

import javax.swing.JOptionPane;

import com.dreamteam.data.PlaylistDataManager;
import com.dreamteam.model.MP3Player;
import com.dreamteam.model.Song;
import com.dreamteam.view.Panel;
import com.dreamteam.view.Window;

public class WindowController implements WindowListener
{
	private Window window;
	private Panel panel;
	
	/**
	 * Costruttore del WindowController.
	 * Inizializza i riferimenti alla finestra principale e al pannello.
	 *
	 * @param window La finestra principale dell'applicazione.
	 * @param panel Il pannello principale mostrato nella finestra.
	 */
	public WindowController(Window window, Panel panel)
	{
		this.window = window;
		this.panel = panel;
	}
	
	/**
	 * Metodo chiamato quando si tenta di chiudere la finestra.
	 * Mostra una finestra di conferma prima di chiudere.
	 *
	 * @param e L'evento associato alla chiusura della finestra.
	 */
    @Override
	public void windowClosing(WindowEvent e) 
	{
    	PlaylistDataManager.savePlaylists(panel.getPlaylists());

        int scelta = JOptionPane.showConfirmDialog(panel, "Sicuro di voler uscire?", "Conferma uscita", JOptionPane.YES_NO_OPTION);

        if (scelta == JOptionPane.YES_OPTION) {
            salvaDimensioneEFinestra();
            
            Properties config = new Properties();
            File configFile = new File("config.properties");

            try {
                if (configFile.exists()) {
                    config.load(new FileInputStream(configFile));
                }

                MP3Player player = panel.getPlayer();
                Song current = player.getCurrentSong();

                if (current != null) {
                    config.setProperty("last.song.title", current.getTitle());
                    config.setProperty("last.song.path", current.getPath());
                    config.setProperty("last.song.position", String.valueOf(player.getPausedPosition()));
                }

                String playlistName = panel.getPlaylistList().getSelectedValue();
                if (playlistName != null) {
                    config.setProperty("last.playlist", playlistName);
                }

                config.store(new FileOutputStream(configFile), null);
            } catch (IOException ex) {
                ex.printStackTrace();
            }

            window.setDefaultCloseOperation(Window.EXIT_ON_CLOSE);
        } else {
            window.setDefaultCloseOperation(Window.DO_NOTHING_ON_CLOSE);
        }
	}
    
    /**
     * Salva dimensione, posizione e stato (massimizzata o meno) della finestra nel file config.properties.
     */
    private void salvaDimensioneEFinestra() {
        try {
            File configFile = new File("config.properties");
            Properties props = new Properties();

            if (configFile.exists()) {
                props.load(new FileInputStream(configFile));
            }

            props.setProperty("window.x", String.valueOf(window.getX()));
            props.setProperty("window.y", String.valueOf(window.getY()));
            props.setProperty("window.width", String.valueOf(window.getWidth()));
            props.setProperty("window.height", String.valueOf(window.getHeight()));

            boolean isMaximized = (window.getExtendedState() & java.awt.Frame.MAXIMIZED_BOTH) != 0;
            props.setProperty("window.maximized", String.valueOf(isMaximized));

            props.store(new java.io.FileOutputStream(configFile), null);
        } catch (Exception ex) {
            Logger.writeLog("Errore nel salvataggio posizione/dimensione finestra: " + ex.getMessage());
        }
    }

	@Override
	public void windowOpened(WindowEvent e) {}
	
	@Override
	public void windowClosed(WindowEvent e) {}

	@Override
	public void windowIconified(WindowEvent e) {}

	@Override
	public void windowDeiconified(WindowEvent e) {}

	@Override
	public void windowActivated(WindowEvent e) {}

	@Override
	public void windowDeactivated(WindowEvent e) {}
}

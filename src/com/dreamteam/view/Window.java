package com.dreamteam.view;

import java.awt.Dimension;
import java.awt.Frame;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import javax.imageio.ImageIO;
import javax.swing.JFrame;

import com.dreamteam.control.Logger;
import com.dreamteam.control.WindowController;
import com.dreamteam.data.ConfigManager;
import com.dreamteam.languages.Languages;
import com.dreamteam.model.MP3Player;
import com.dreamteam.model.Playlist;

@SuppressWarnings("serial")
public class Window extends JFrame
{
	private final String TITLE = "Sonora";
	private final File iconFile = new File("resources/icons/sonora.png");
	
	private final int WIDTH = 1920;
	private final int HEIGHT = 1080;
	
	private WindowController windowController;
	private Panel panel;
	private Playlist playlist;
	private MP3Player player;

	public Window() 
	{
		Logger.writeLog("Window creata");
		playlist = new Playlist();
		player = new MP3Player();
		panel = new Panel(playlist, player, this);
		player.setPanel(panel);
		
		windowController = new WindowController(this, panel);
		
		Languages lang = ConfigManager.loadLanguageFromConfig();
		panel.updateLanguage(lang);
		
		try {
		    File configFile = new File("config.properties");
		    if (configFile.exists()) {
		        Properties props = new Properties();
		        props.load(new FileInputStream(configFile));

		        int x = Integer.parseInt(props.getProperty("window.x", "100"));
		        int y = Integer.parseInt(props.getProperty("window.y", "100"));
		        int width = Integer.parseInt(props.getProperty("window.width"));
		        int height = Integer.parseInt(props.getProperty("window.height"));
		        boolean maximized = Boolean.parseBoolean(props.getProperty("window.maximized", "false"));

		        setBounds(x, y, width, height);
		        if (maximized) setExtendedState(Frame.MAXIMIZED_BOTH);
		    } else {
		        setSize(WIDTH, HEIGHT);
		        setLocationRelativeTo(null); // centro schermo
		    }
		} catch (Exception ex) {
		    Logger.writeLog("Errore nel caricamento dimensione/posizione finestra: " + ex.getMessage());
		    setSize(WIDTH, HEIGHT);
		    setLocationRelativeTo(null);
		}
		
		setContentPane(panel);
		setTitle(TITLE);
		try {
			setIconImage(ImageIO.read(iconFile));
		} catch (IOException e) {
			Logger.writeLog(e.getMessage());
		}
		setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		setResizable(true);
		addWindowListener(windowController);
		
		setMinimumSize(new Dimension(720, 480));
		setSize(WIDTH, HEIGHT);
		setLocationRelativeTo(null);
		
		setVisible(true);
	}
	
	// getters //
	
	public Panel getPanel()
	{
		return this.panel;
	}
}
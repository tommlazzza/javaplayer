package com.dreamteam.view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.FontMetrics;
import java.awt.Image;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;

import com.dreamteam.control.Controller;
import com.dreamteam.control.Logger;
import com.dreamteam.control.PlaylistRenderer;
import com.dreamteam.data.ConfigManager;
import com.dreamteam.data.LanguageManager;
import com.dreamteam.data.PlaylistDataManager;
import com.dreamteam.languages.Languages;
import com.dreamteam.model.MP3Player;
import com.dreamteam.model.Playlist;
import com.dreamteam.model.Song;

/**
 * Classe principale della GUI dell'applicazione Sonora.
 * <p>
 * Questo pannello gestisce l'interfaccia utente principale, inclusi:
 * <ul>
 *   <li>La visualizzazione delle playlist con immagini di copertina.</li>
 *   <li>La lista delle canzoni selezionabili e filtrabili.</li>
 *   <li>I controlli di riproduzione: play, pausa, avanti, indietro, modalità di riproduzione.</li>
 *   <li>Un campo ricerca per filtrare rapidamente i brani.</li>
 *   <li>Supporto per modalità dark/light e multilingua.</li>
 * </ul>
 * Il pannello comunica con il {@link com.dreamteam.control.Controller} per gestire la logica e 
 * con {@link com.dreamteam.model.MP3Player} per la riproduzione audio.
 * 
 * @author DreamTeam
 * @version 2.0
 */
@SuppressWarnings({ "serial" })
public class Panel extends JPanel {
	public final static int ICON_SIZE = 18;
	private final static File ICONS_PATH = new File("resources/icons/");
	private final static File PREVIOUS_LIGHT_ICON = new File(ICONS_PATH, "previous_light.png");
	private final static File PREVIOUS_DARK_ICON = new File(ICONS_PATH, "previous_dark.png");
	private final static File PAUSE_LIGHT_ICON = new File(ICONS_PATH, "pause_light.png");
	private final static File PAUSE_DARK_ICON = new File(ICONS_PATH, "pause_dark.png");
	private final static File PLAY_LIGHT_ICON = new File(ICONS_PATH, "play_light.png");
	private final static File PLAY_DARK_ICON = new File(ICONS_PATH, "play_dark.png");
	private final static File NEXT_LIGHT_ICON = new File(ICONS_PATH, "next_light.png");
	private final static File NEXT_DARK_ICON = new File(ICONS_PATH, "next_dark.png");
	private final static File SEQUENZIALE_LIGHT_ICON = new File(ICONS_PATH, "sequenziale_light.png");
	private final static File SEQUENZIALE_DARK_ICON = new File(ICONS_PATH, "sequenziale_dark.png");
	private final static File SHUFFLE_LIGHT_ICON = new File(ICONS_PATH, "shuffle_light.png");
	private final static File SHUFFLE_DARK_ICON = new File(ICONS_PATH, "shuffle_dark.png");
	private final static File RIPETI_LIGHT_ICON = new File(ICONS_PATH, "ripeti_light.png");
	private final static File RIPETI_DARK_ICON = new File(ICONS_PATH, "ripeti_dark.png");
	private final static File QUEUE_LIGHT_ICON = new File(ICONS_PATH, "queue_light.png");
	private final static File QUEUE_DARK_ICON = new File(ICONS_PATH, "queue_dark.png");
	
	private Window window;
	private Playlist playlist;
	private MP3Player player;
	private Controller controller;
	private PlaylistRenderer playlistRenderer;
	
	private final Languages[] LANGUAGES = Languages.values();

	private JPanel 	panel_1, 
					panel_3, 
					leftPanel, 
					imagePanel, 
					buttonPanel, 
					centerPanel;
	private JSlider slider;
	private JScrollPane scrollPane, playlistScrollPane;
	private JButton rewind_button, 
					play_pause_btn, 
					skip_button, 
					toggleModeButton,
					stopButton;
	private JMenuBar menuBar;
	private JMenu playlistMenu, 
					songMenu, 
					viewMenu, 
					playbackMenu, 
					playbackModeMenu,
					languageMenu, 
					helpMenu, 
					sonoraMenu;
	private JMenuItem createPlaylistItem, 
					deletePlaylistItem, 
					renamePlaylistItem, 
					coverMenuItem, 
					exportPlaylistItem, 
					importPlaylistItem,
					addSongItem, 
					removeSongItem, 
					importSongItem, 
					removeMP3Item, 
					exportSingleMP3Item,
					changeThemeItem,
					previousSongItem, 
					playPauseItem, 
					nextSongItem, 
					openQueueItem, 
					manageQueueItem, 
					addToQueueItem, 
					sequenzialeItem, 
					casualeItem, 
					ripetiItem, 
					esciItem, 
					aggiungiAllaCodaTopItem, 
					rimuoviCanzoneItem, 
					aggiungiAllaCodaItem, 
					cercaComando, 
					informazioniComando, 
					informazioniSonora,
					sortPlaylistItem,
					topSongsItem,
					topPlaylistItem,
					playlistConstructor,
					customOrderItem;
	private JMenuItem[] languagesItem;
	private JLabel coverImageLabel, currentSongLabel, playlistTitleLabel;
	private JList<String> comboBox, playlistList;
	private DefaultListModel<String> comboBoxModel;
	private JTextField searchField;
	
	private JPopupMenu popupMenu;

	private Map<String, Playlist> playlists;
	private DefaultComboBoxModel<String> playlistListModel;
	
	private boolean isDarkMode = false;
	
    /**
     * Costruttore della classe Panel.
     * <p>
     * Inizializza l'interfaccia utente, carica le playlist salvate,
     * applica la lingua e il tema salvati in configurazione,
     * e imposta tutti i componenti grafici necessari.
     *
     * <p>
     * Le playlist vengono caricate da:
     * <ul>
     *   <li>Cartelle locali in <code>resources/playlists</code>.</li>
     *   <li>Una playlist speciale "Tutti i brani" generata automaticamente.</li>
     * </ul>
     *
     * <p>
     * Vengono inoltre inizializzati:
     * <ul>
     *   <li>I menu dell'applicazione (File, Playlist, Riproduzione, ecc.).</li>
     *   <li>I pulsanti di controllo (Play, Pausa, Avanti, Indietro, ecc.).</li>
     *   <li>I componenti per la lista di brani e playlist.</li>
     *   <li>La modalità di riproduzione e la lingua corrente.</li>
     * </ul>
     *
     * @param playlist La playlist iniziale da visualizzare e gestire.
     * @param player L'istanza del lettore MP3 usata per la riproduzione audio.
     * @param window La finestra principale che contiene questo pannello.
     */
	public Panel(Playlist playlist, MP3Player player, Window window) 
	{
		this.window = window;
		
		Languages langs = ConfigManager.loadLanguageFromConfig();
		LanguageManager.load(langs);
		isDarkMode = ConfigManager.loadThemeFromConfig();
		
		this.controller = new Controller(this);
		this.player = player;
		playlistRenderer = new PlaylistRenderer(this);
		
		setFocusable(true);
		requestFocusInWindow();
		addKeyListener(controller);
		
		controller.setPlaybackMode(ConfigManager.loadModeFromConfig());

		playlists = new LinkedHashMap<>();
		playlists.putAll(PlaylistDataManager.loadPlaylistsFromFolders());
		//playlists.putAll(PlaylistDataManager.loadPlaylists());
		
		playlists = playlists.entrySet()
			    .stream()
			    .sorted(Map.Entry.comparingByKey())
			    .collect(Collectors.toMap(
			        Map.Entry::getKey,
			        Map.Entry::getValue,
			        (e1, e2) -> e1,
			        LinkedHashMap::new
			    ));
		
		playlists.put(LanguageManager.get("playlist.all"), PlaylistDataManager.creaPlaylistTuttiIBrani());
		
		this.playlist = playlists.values().iterator().next();
		
		playlistListModel = new DefaultComboBoxModel<>();
		for (String name : playlists.keySet()) {
			playlistListModel.addElement(name);
		}

		setBackground(new Color(192, 192, 192));
		setLayout(new BorderLayout());

		// Menù
		menuBar = new JMenuBar();
		
		sonoraMenu = new JMenu(LanguageManager.get("menu.sonora"));
		
		informazioniSonora = new JMenuItem(LanguageManager.get("item.infoSonora"));
		esciItem = new JMenuItem(LanguageManager.get("item.exit"));
		
		informazioniSonora.addActionListener(controller);
		esciItem.addActionListener(controller);
		
		sonoraMenu.add(informazioniSonora);
		sonoraMenu.add(esciItem);
		
		menuBar.add(sonoraMenu);
		
		songMenu = new JMenu(LanguageManager.get("menu.file"));
		
		importSongItem = new JMenuItem(LanguageManager.get("item.import"));
		removeMP3Item = new JMenuItem(LanguageManager.get("item.remove"));
		exportSingleMP3Item = new JMenuItem(LanguageManager.get("item.export"));
		
		songMenu.add(importSongItem);
		songMenu.add(removeMP3Item);
		songMenu.add(exportSingleMP3Item);
		
		importSongItem.addActionListener(controller);
		removeMP3Item.addActionListener(controller);
		exportSingleMP3Item.addActionListener(controller);
		
		menuBar.add(songMenu);

		playlistMenu = new JMenu(LanguageManager.get("menu.playlist"));
		
		playlistConstructor = new JMenuItem(LanguageManager.get("playlist.constructor"));
		createPlaylistItem = new JMenuItem(LanguageManager.get("item.createPlaylist"));
		deletePlaylistItem = new JMenuItem(LanguageManager.get("item.deletePlaylist"));
		exportPlaylistItem = new JMenuItem(LanguageManager.get("item.renamePlaylist"));
		importPlaylistItem = new JMenuItem(LanguageManager.get("item.importPlaylist"));
		renamePlaylistItem = new JMenuItem(LanguageManager.get("item.renamePlaylist"));
		coverMenuItem = new JMenuItem(LanguageManager.get("item.coverPlaylist"));
		addSongItem = new JMenuItem(LanguageManager.get("item.addToPlaylist"));
		removeSongItem = new JMenuItem(LanguageManager.get("item.removeFromPlaylist"));
		sortPlaylistItem = new JMenuItem(LanguageManager.get("playlist.sort"));
		topPlaylistItem = new JMenuItem(LanguageManager.get("item.topPlaylist"));
		

		playlistMenu.add(playlistConstructor);
		playlistMenu.addSeparator();
		playlistMenu.add(createPlaylistItem);
		playlistMenu.add(deletePlaylistItem);
		playlistMenu.add(exportPlaylistItem);
		playlistMenu.add(importPlaylistItem);
		playlistMenu.addSeparator();
		playlistMenu.add(renamePlaylistItem);
		playlistMenu.add(coverMenuItem);
		playlistMenu.add(sortPlaylistItem); 
		playlistMenu.addSeparator();
		playlistMenu.add(addSongItem);
		playlistMenu.add(removeSongItem);
		playlistMenu.addSeparator();
		playlistMenu.add(topPlaylistItem);

		playlistConstructor.addActionListener(controller);
		createPlaylistItem.addActionListener(controller);
		deletePlaylistItem.addActionListener(controller);
		renamePlaylistItem.addActionListener(controller);
		coverMenuItem.addActionListener(controller);
		addSongItem.addActionListener(controller);
		removeSongItem.addActionListener(controller);
		exportPlaylistItem.addActionListener(controller);
		importPlaylistItem.addActionListener(controller);
		sortPlaylistItem.addActionListener(controller);
		topPlaylistItem.addActionListener(controller);
		
		menuBar.add(playlistMenu);
		
		playbackMenu = new JMenu(LanguageManager.get("menu.playback"));

		previousSongItem = new JMenuItem(LanguageManager.get("item.previousSong"));
		playPauseItem = new JMenuItem(LanguageManager.get("item.playPause"));
		nextSongItem = new JMenuItem(LanguageManager.get("item.nextSong"));
		openQueueItem = new JMenuItem(LanguageManager.get("item.openQueue"));
		manageQueueItem = new JMenuItem(LanguageManager.get("item.manageQueue"));
		playbackModeMenu = new JMenu(LanguageManager.get("item.changePlaybackMode"));
		topSongsItem = new JMenuItem(LanguageManager.get("item.topSongs"));
		
		addToQueueItem = new JMenuItem(LanguageManager.get("item.addToQueue"));
		addToQueueItem.addActionListener(controller);

		sequenzialeItem = new JMenuItem(LanguageManager.get("item.sequenziale"));
		casualeItem = new JMenuItem(LanguageManager.get("item.casuale"));
		ripetiItem = new JMenuItem(LanguageManager.get("item.ripeti"));

		playbackModeMenu.add(sequenzialeItem);
		playbackModeMenu.add(casualeItem);
		playbackModeMenu.add(ripetiItem);

		playbackMenu.add(previousSongItem);
		playbackMenu.add(playPauseItem);
		playbackMenu.add(nextSongItem);
		playbackMenu.addSeparator();
		playbackMenu.add(addToQueueItem);
		playbackMenu.add(openQueueItem);
		playbackMenu.add(manageQueueItem);
		playbackMenu.addSeparator();
		playbackMenu.add(playbackModeMenu);
		playbackMenu.addSeparator();
		playbackMenu.add(topSongsItem);
		
		previousSongItem.addActionListener(controller);
		playPauseItem.addActionListener(controller);
		nextSongItem.addActionListener(controller);
		openQueueItem.addActionListener(controller);
		manageQueueItem.addActionListener(controller);
		sequenzialeItem.addActionListener(controller);
		casualeItem.addActionListener(controller);
		ripetiItem.addActionListener(controller);
		topSongsItem.addActionListener(controller);

		menuBar.add(playbackMenu);
		
		viewMenu = new JMenu(LanguageManager.get("menu.view"));
		
		changeThemeItem = new JMenuItem(LanguageManager.get("theme.change"));
		viewMenu.add(changeThemeItem);
		add(menuBar, BorderLayout.NORTH);
		changeThemeItem.addActionListener(controller);
		
		languageMenu = new JMenu(LanguageManager.get("menu.language"));
		
		languagesItem = new JMenuItem[LANGUAGES.length];
		for (int i = 0; i < LANGUAGES.length; i++) {
		    Languages lang = LANGUAGES[i];
		    languagesItem[i] = new JMenuItem(lang.getDisplayName());
		    languagesItem[i].setActionCommand(lang.name()); // importante per riconoscerlo dopo
		    languagesItem[i].addActionListener(controller);
		    languageMenu.add(languagesItem[i]);
		}
		
		viewMenu.add(languageMenu);
		
		menuBar.add(viewMenu);
		
		helpMenu = new JMenu(LanguageManager.get("menu.help"));
		
		cercaComando = new JMenuItem(LanguageManager.get("item.cercaComando"));
		informazioniComando = new JMenuItem(LanguageManager.get("item.infoComando"));
		
		cercaComando.addActionListener(controller);
		informazioniComando.addActionListener(controller);
		
		helpMenu.add(cercaComando);
		helpMenu.add(informazioniComando);
		
		menuBar.add(helpMenu);

		// Lista playlist con renderer custom
		leftPanel = new JPanel(new BorderLayout());
		playlistList = new JList<>(playlistListModel);
		playlistList.setCellRenderer(playlistRenderer);
		playlistList.setSelectedIndex(0);
		playlistList.addListSelectionListener(controller);
		
		playlistList.setFixedCellHeight(-1);
		playlistList.setPrototypeCellValue(null);

		playlistScrollPane = new JScrollPane(playlistList); // <- salvato riferimento
		playlistScrollPane.setPreferredSize(new Dimension(calculateMaxPlaylistWidth(), 0)); // <- larghezza dinamica
		leftPanel.add(playlistScrollPane, BorderLayout.CENTER);
		add(leftPanel, BorderLayout.WEST);
		playlistList.addMouseListener(controller);

		// Centro layout con immagine, campo ricerca e lista canzoni
		centerPanel = new JPanel();
		centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
		add(centerPanel, BorderLayout.CENTER);

		// Campo ricerca
		searchField = new JTextField();
		searchField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 25));
		searchField.getDocument().addDocumentListener(controller);
		centerPanel.add(searchField);

		// Copertina
		imagePanel = new JPanel();
		imagePanel.setMaximumSize(new Dimension(300, 180));
		imagePanel.setPreferredSize(new Dimension(300, 180));
		imagePanel.setLayout(new BoxLayout(imagePanel, BoxLayout.Y_AXIS));
		imagePanel.setAlignmentX(Component.CENTER_ALIGNMENT);
		
		coverImageLabel = new JLabel();
		coverImageLabel.setPreferredSize(new Dimension(150, 150));
		coverImageLabel.setAlignmentX(Component.CENTER_ALIGNMENT); // centra la copertina
		imagePanel.add(coverImageLabel);
		
		imagePanel.add(Box.createVerticalStrut(10)); // spazio
		
		playlistTitleLabel = new JLabel();
		playlistTitleLabel.setAlignmentX(Component.CENTER_ALIGNMENT); // centra il titolo
		playlistTitleLabel.setHorizontalAlignment(JLabel.CENTER); // centra il testo
		imagePanel.add(playlistTitleLabel);
		centerPanel.add(imagePanel);

		// Lista canzoni
		panel_1 = new JPanel(new BorderLayout());
		comboBoxModel = new DefaultListModel<>();
		comboBox = new JList<>(comboBoxModel);
		comboBox.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		comboBox.addListSelectionListener(controller);
		
		// Menu tasto destro sulla lista canzoni
		popupMenu = new JPopupMenu();
		aggiungiAllaCodaItem = new JMenuItem(LanguageManager.get("item.addToQueue"));
		aggiungiAllaCodaItem.addActionListener(controller);
		popupMenu.add(aggiungiAllaCodaItem);
		
		//Aggiungi in cima alla coda
		aggiungiAllaCodaTopItem = new JMenuItem(LanguageManager.get("item.addToQueueTop"));
		aggiungiAllaCodaTopItem.addActionListener(controller);
		popupMenu.add(aggiungiAllaCodaTopItem);

		// NUOVA OPZIONE: "Rimuovi canzone"
		rimuoviCanzoneItem = new JMenuItem(LanguageManager.get("item.removeSong"));
		rimuoviCanzoneItem.addActionListener(controller);
		popupMenu.add(rimuoviCanzoneItem);

		comboBox.addMouseListener(controller);
		
		scrollPane = new JScrollPane(comboBox);
		panel_1.add(scrollPane, BorderLayout.CENTER);
		centerPanel.add(panel_1);

		// Controlli audio
		panel_3 = new JPanel();
		panel_3.setLayout(new BoxLayout(panel_3, BoxLayout.Y_AXIS));
		panel_3.setPreferredSize(new Dimension(0, 90));
		add(panel_3, BorderLayout.SOUTH);

		currentSongLabel = new JLabel(LanguageManager.get("label.noSong"));
		currentSongLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
		panel_3.add(currentSongLabel);
		panel_3.add(Box.createVerticalStrut(5));
		
		slider = new JSlider();
		slider.setMaximumSize(new Dimension(600, 20));
		slider.setAlignmentX(Component.CENTER_ALIGNMENT);
		slider.setValue(0);
		slider.addChangeListener(controller);
		
		panel_3.add(Box.createVerticalStrut(10));
		panel_3.add(slider);
		panel_3.add(Box.createVerticalStrut(10));

		buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
		
		rewind_button = new JButton();
		play_pause_btn = new JButton();
		skip_button = new JButton();
		toggleModeButton = new JButton();
		stopButton = new JButton();
		
		rewind_button.setBorderPainted(false);
		play_pause_btn.setBorderPainted(false);
		skip_button.setBorderPainted(false);
		toggleModeButton.setBorderPainted(false);
		stopButton.setBorderPainted(false);
		
		rewind_button.setFocusPainted(false);
		skip_button.setFocusPainted(false);
		play_pause_btn.setFocusPainted(false);
		toggleModeButton.setFocusPainted(false);
		stopButton.setFocusPainted(false);

		rewind_button.addActionListener(controller);
		play_pause_btn.addActionListener(controller);
		skip_button.addActionListener(controller);
		toggleModeButton.addActionListener(controller);
		stopButton.addActionListener(controller);
		
		buttonPanel.add(stopButton);
		buttonPanel.add(rewind_button);
		buttonPanel.add(play_pause_btn);
		buttonPanel.add(skip_button);
		buttonPanel.add(toggleModeButton);
		panel_3.add(buttonPanel);
		
		refreshComboBox();
		refreshCoverImage();
		refreshPlaylistTitle();
		
		updateLanguage(langs);
		applyTheme();
		
		Logger.writeLog("Pannello creato");
	}

	/**
     * Ricalcola dinamicamente la larghezza massima della lista delle playlist,
     * considerando la lunghezza del titolo più lungo.
     *
     * @return la larghezza consigliata per il contenitore delle playlist.
     */
	private int calculateMaxPlaylistWidth() {
		FontMetrics fm = playlistList.getFontMetrics(playlistList.getFont());
		int maxWidth = 0;

		for (int i = 0; i < playlistListModel.getSize(); i++) {
			String title = playlistListModel.getElementAt(i);
			int width = fm.stringWidth(title);
			if (width > maxWidth) {
				maxWidth = width;
			}
		}

		return maxWidth + 70; // spazio extra per margini e icona
	}

	/**
     * Aggiorna la lista delle canzoni nella playlist selezionata.
     * Cancella e ricarica tutti i titoli nel modello della comboBox.
     */
	public void refreshComboBox() {
	    if (controller != null) controller.setSuppressComboBoxPlayback(true);

	    comboBoxModel.clear();
	    if (playlist != null) {
	        for (String title : playlist.getSongTitles()) {
	            comboBoxModel.addElement(title);
	        }
	    }

	    Logger.writeLog("Lista canzoni aggiornata");

	    if (controller != null) controller.setSuppressComboBoxPlayback(false);
	}
	
	/**
     * Carica e visualizza l'immagine di copertina associata alla playlist corrente.
     * Se il file non esiste o è invalido, mostra un messaggio di errore nella label.
     */
	public void refreshCoverImage() {
		if (playlist != null && playlist.getCoverImage() != null) {
			String path = playlist.getCoverImage();
			File imgFile = new File(path);
			if (imgFile.exists()) {
				try {
					ImageIcon icon = new ImageIcon(imgFile.getAbsolutePath());
					Image image = icon.getImage();
					if (image.getWidth(null) > 0 && image.getHeight(null) > 0) {
						Image scaledImage = image.getScaledInstance(150, 150, Image.SCALE_SMOOTH);
						coverImageLabel.setIcon(new ImageIcon(scaledImage));
						coverImageLabel.setText("");
					} else {
						coverImageLabel.setIcon(null);
						coverImageLabel.setText(LanguageManager.get("label.no.valid.image"));
					}
				} catch (Exception e) {
					coverImageLabel.setIcon(null);
					coverImageLabel.setText(LanguageManager.get("error.image.load"));
				}
			} else {
				coverImageLabel.setIcon(null);
				coverImageLabel.setText(LanguageManager.get("image.not.found"));
			}
		} else {
			coverImageLabel.setIcon(null);
			coverImageLabel.setText(LanguageManager.get("no.cover"));
		}
		
		Logger.writeLog("Icone playlist caricate");
	}
	
	/**
     * Aggiorna l'etichetta del titolo della playlist corrente sotto la copertina.
     * Se il nome non è definito, mostra un messaggio standard.
     */
	public void refreshPlaylistTitle() {
		if (playlist != null && playlist.getName() != null) {
			playlistTitleLabel.setText(playlist.getName());
		} else {
			playlistTitleLabel.setText(LanguageManager.get("label.noPlaylist"));
		}
	}
	
	/**
     * Ricarica le icone dei pulsanti principali (play, pausa, avanti, ecc.)
     * in base al tema attuale (dark/light) e allo stato di riproduzione.
     */
	public void refreshIcons()
	{
		try
	    {
	    	Image img;
	    	
	    	img = ImageIO.read(isDarkMode ? PREVIOUS_DARK_ICON : PREVIOUS_LIGHT_ICON).getScaledInstance(ICON_SIZE, ICON_SIZE, Image.SCALE_DEFAULT);
	    	rewind_button.setIcon(new ImageIcon(img));
	    	
	    	if(player.isPlaying())
	    	{
	    		img = ImageIO.read(isDarkMode ? PAUSE_DARK_ICON : PAUSE_LIGHT_ICON).getScaledInstance(ICON_SIZE, ICON_SIZE, Image.SCALE_DEFAULT);
	    	}
	    	else
	    	{
	    		img = ImageIO.read(isDarkMode ? PLAY_DARK_ICON : PLAY_LIGHT_ICON).getScaledInstance(ICON_SIZE, ICON_SIZE, Image.SCALE_DEFAULT);
	    	}
	    	play_pause_btn.setIcon(new ImageIcon(img));
	    	
	    	img = ImageIO.read(isDarkMode ? NEXT_DARK_ICON : NEXT_LIGHT_ICON).getScaledInstance(ICON_SIZE, ICON_SIZE, Image.SCALE_DEFAULT);
	    	skip_button.setIcon(new ImageIcon(img));
	    	
	    	switch(controller.getPlaybackMode())
	    	{
	    		case SEQUENZIALE:
	    			img = ImageIO.read(isDarkMode ? SEQUENZIALE_DARK_ICON : SEQUENZIALE_LIGHT_ICON).getScaledInstance(ICON_SIZE, ICON_SIZE, Image.SCALE_DEFAULT);
	    			break;
	    			
	    		case CASUALE:
	    			img = ImageIO.read(isDarkMode ? SHUFFLE_DARK_ICON : SHUFFLE_LIGHT_ICON).getScaledInstance(ICON_SIZE, ICON_SIZE, Image.SCALE_DEFAULT);
	    			break;
	    			
	    		case RIPETI:
	    			img = ImageIO.read(isDarkMode ? RIPETI_DARK_ICON : RIPETI_LIGHT_ICON).getScaledInstance(ICON_SIZE, ICON_SIZE, Image.SCALE_DEFAULT);
	    			break;
	    	}
	    	toggleModeButton.setIcon(new ImageIcon(img));
	    	
	    	img = ImageIO.read(isDarkMode ? QUEUE_DARK_ICON : QUEUE_LIGHT_ICON).getScaledInstance(ICON_SIZE, ICON_SIZE, Image.SCALE_DEFAULT);
	    	stopButton.setIcon(new ImageIcon(img));
	    	
	    	Logger.writeLog("Icone aggiornate");
	    }
	    catch (Exception e)
		{
	    	Logger.writeLog("Errore nel caricamento icone");
	    }
	}
	
	/**
     * Applica la lingua selezionata a tutti i testi dell'interfaccia.
     * Aggiorna le etichette dei menu, pulsanti, messaggi e altre componenti.
     * 
     * @param selectedLang La lingua scelta da applicare.
     */
	public void updateLanguage(Languages selectedLang) 
	{
	    // Carica il bundle della lingua selezionata
	    LanguageManager.load(selectedLang);

	    // Salva lingua su config.properties
	    try {
	        Properties config = new Properties();
	        File configFile = new File("config.properties");
	        if (configFile.exists()) {
	            config.load(new FileInputStream(configFile));
	        }
	        config.setProperty("language", selectedLang.name());
	        config.store(new FileOutputStream(configFile), null);
	    } catch (Exception e) {
	        Logger.writeLog(e.getMessage());
	    }

	    // Menu principali
	    sonoraMenu.setText(LanguageManager.get("menu.sonora"));
	    songMenu.setText(LanguageManager.get("menu.file"));
	    playlistMenu.setText(LanguageManager.get("menu.playlist"));
	    playbackMenu.setText(LanguageManager.get("menu.playback"));
	    viewMenu.setText(LanguageManager.get("menu.view"));
	    languageMenu.setText(LanguageManager.get("menu.language"));
	    helpMenu.setText(LanguageManager.get("menu.help"));
	    
	    // Sonora menu
	    informazioniSonora.setText(LanguageManager.get("item.infoSonora"));
	    esciItem.setText(LanguageManager.get("item.exit"));

	    // MenuItem canzoni
	    importSongItem.setText(LanguageManager.get("item.import"));
	    removeMP3Item.setText(LanguageManager.get("item.remove"));
	    exportSingleMP3Item.setText(LanguageManager.get("item.export"));

	    // MenuItem playlist
	    createPlaylistItem.setText(LanguageManager.get("item.createPlaylist"));
	    deletePlaylistItem.setText(LanguageManager.get("item.deletePlaylist"));
	    renamePlaylistItem.setText(LanguageManager.get("item.renamePlaylist"));
	    coverMenuItem.setText(LanguageManager.get("item.coverPlaylist"));
	    addSongItem.setText(LanguageManager.get("item.addToPlaylist"));
	    removeSongItem.setText(LanguageManager.get("item.removeFromPlaylist"));
	    exportPlaylistItem.setText(LanguageManager.get("item.exportPlaylist"));
	    importPlaylistItem.setText(LanguageManager.get("item.importPlaylist"));

	    // Tema
	    changeThemeItem.setText(LanguageManager.get("theme.change"));

	    // Playback
	    previousSongItem.setText(LanguageManager.get("item.previousSong"));
	    playPauseItem.setText(LanguageManager.get("item.playPause"));
	    nextSongItem.setText(LanguageManager.get("item.nextSong"));
	    openQueueItem.setText(LanguageManager.get("item.openQueue"));
	    manageQueueItem.setText(LanguageManager.get("item.manageQueue"));
	    addToQueueItem.setText(LanguageManager.get("item.addToQueue"));

	    // Modalità di riproduzione
	    playbackModeMenu.setText(LanguageManager.get("item.changePlaybackMode"));
	    sequenzialeItem.setText(LanguageManager.get("item.sequenziale"));
	    casualeItem.setText(LanguageManager.get("item.casuale"));
	    ripetiItem.setText(LanguageManager.get("item.ripeti"));
	    
	    // Menu aiuto
	    cercaComando.setText(LanguageManager.get("item.cercaComando"));
	    informazioniComando.setText(LanguageManager.get("item.infoComando"));
	    
	    // Menu tasto destro
	    aggiungiAllaCodaItem.setText(LanguageManager.get("item.addToQueue"));
	    rimuoviCanzoneItem.setText(LanguageManager.get("item.removeSong"));
	    aggiungiAllaCodaTopItem.setText(LanguageManager.get("item.addToQueueTop"));
	    
	    // Menu lingua dinamico
	    for (int i = 0; i < LANGUAGES.length; i++) {
	        languagesItem[i].setText(LANGUAGES[i].getDisplayName());
	    }

	    // Etichetta canzone corrente
	    if (player.isPlaying() && comboBox.getSelectedValue() != null) {
	        currentSongLabel.setText(comboBox.getSelectedValue());
	    } else {
	        currentSongLabel.setText(LanguageManager.get("label.noSong"));
	    }

	    // Forza aggiornamento grafico
	    repaint();
	    revalidate();
	    Logger.writeLog("Repaint e Revalidate fatto");
	    
	    Logger.writeLog("Lingua aggiornata");
	}

	/**
     * Filtra dinamicamente la lista delle canzoni nella comboBox in base al
     * testo digitato dall'utente nel campo di ricerca.
     */
	public void filterSongs() {
		String text = searchField.getText().toLowerCase();
		comboBoxModel.clear();
		if (playlist != null) {
			for (String title : playlist.getSongTitles()) {
				if (title.toLowerCase().contains(text))
					comboBoxModel.addElement(title);
			}
		}
		
		Logger.writeLog("Canzoni filtrate");
	}
	
	/**
     * Cambia il tema dell'interfaccia tra chiaro e scuro e lo applica immediatamente.
     */
	public void toggleTheme() {
		isDarkMode = !isDarkMode;
		applyTheme();
		
		Logger.writeLog("Tema cambiato");
	}

	/**
     * Applica il tema attuale (chiaro/scuro) a tutti i componenti del pannello.
     * Colora background, testi, slider, pulsanti e menu in base al tema selezionato.
     */
	private void applyTheme() {
	    Color bg = isDarkMode ? new Color(28, 28, 30) : new Color(250, 250, 250);
	    Color fg = isDarkMode ? new Color(240, 240, 240) : new Color(30, 30, 30);
	    Color secondaryBg = isDarkMode ? new Color(44, 44, 46) : new Color(235, 235, 235);
	    Color accent = new Color(120, 120, 120);

	    setBackground(bg);
	    
	    //cursore
	    
	    slider.setCursor(new Cursor(Cursor.HAND_CURSOR));
	    playlistList.setCursor(slider.getCursor());

	    // MENU BAR e voci ricorsive
	    menuBar.setBackground(bg);
	    menuBar.setForeground(fg);
	    for (int i = 0; i < menuBar.getMenuCount(); i++) {
	        JMenu menu = menuBar.getMenu(i);
	        if (menu != null) {
	            applyMenuThemeRecursive(menu, bg, fg);
	        }
	    }
	    
	    // CAMBIA LE ICONE IN BASE AL TEMA
	    refreshIcons();

	    // Pannelli
	    centerPanel.setBackground(secondaryBg);
	    panel_1.setBackground(secondaryBg);
	    panel_3.setBackground(secondaryBg);
	    imagePanel.setBackground(secondaryBg);
	    leftPanel.setBackground(secondaryBg);
	    buttonPanel.setBackground(secondaryBg);
	    
	    slider.setBackground(secondaryBg);
	    slider.setForeground(fg);

	    // Liste
	    comboBox.setBackground(bg);
	    comboBox.setForeground(fg);
	    playlistList.setBackground(secondaryBg);
	    playlistList.setForeground(fg);

	    scrollPane.setBackground(bg);
	    scrollPane.getViewport().setBackground(bg);
	    playlistScrollPane.setBackground(secondaryBg);
	    playlistScrollPane.getViewport().setBackground(secondaryBg);

	    // Campo ricerca
	    searchField.setBackground(bg);
	    searchField.setForeground(fg);
	    searchField.setCaretColor(fg);

	    // Slider
	    slider.setBackground(secondaryBg);
	    slider.setForeground(accent);

	    // Pulsanti
	    JButton[] buttons = {
	        rewind_button, play_pause_btn, skip_button,
	        toggleModeButton, stopButton
	    };
	    for (JButton btn : buttons) {
	        btn.setBackground(secondaryBg);
	        btn.setForeground(fg);
	        btn.setCursor(slider.getCursor());
	    }

	    // Cover e label
	    coverImageLabel.setBackground(bg);
	    coverImageLabel.setForeground(fg);
	    currentSongLabel.setForeground(fg);
	    currentSongLabel.setBackground(bg);

	    // Popup menu
	    popupMenu.setBackground(secondaryBg);
	    popupMenu.setForeground(fg);
	    for (Component comp : popupMenu.getComponents()) {
	        if (comp instanceof JMenuItem item) {
	            item.setBackground(secondaryBg);
	            item.setForeground(fg);
	        }
	    }
	    
	    playlistTitleLabel.setForeground(fg);
	    playlistTitleLabel.setBackground(bg);

	    playlistList.setCellRenderer(playlistRenderer);
	    repaint();
	    revalidate();
	    
	    Logger.writeLog("Tema applicato");
	}
	
	/**
     * Applica ricorsivamente il tema a un menu e a tutte le sue voci e sottomenu.
     * 
     * @param menu Il menu da aggiornare.
     * @param bg Il colore di sfondo da applicare.
     * @param fg Il colore del testo da applicare.
     */
	private void applyMenuThemeRecursive(JMenu menu, Color bg, Color fg) {
	    menu.setBackground(bg);
	    menu.setForeground(fg);
	    menu.setOpaque(true); // fondamentale

	    for (int i = 0; i < menu.getItemCount(); i++) {
	        JMenuItem item = menu.getItem(i);
	        if (item != null) {
	            item.setBackground(bg);
	            item.setForeground(fg);
	            item.setOpaque(true);

	            if (item instanceof JMenu subMenu) {
	                applyMenuThemeRecursive(subMenu, bg, fg); // ricorsione per sottomenu
	            }
	        }
	    }
	}

	/**
     * Aggiunge tutti i file MP3 trovati ricorsivamente all'interno di una directory
     * alla playlist specificata.
     * 
     * @param playlist La playlist di destinazione.
     * @param files Array di file o cartelle da esplorare.
     */
	private void addMp3Recursively(Playlist playlist, File[] files) {
	    for (File file : files) {
	        if (file.isDirectory()) {
	            File[] inner = file.listFiles();
	            if (inner != null) {
	                addMp3Recursively(playlist, inner); // ricorsione
	            }
	        } else if (file.getName().toLowerCase().endsWith(".mp3")) {
	            String title = file.getName().substring(0, file.getName().length() - 4).replace('_', ' ').trim();
	            Song song = new Song(title, file.getPath());
	            playlist.addSong(song);
	        }
	    }
	}

	/**
     * Aggiorna l'etichetta inferiore con il titolo della canzone corrente in riproduzione.
     * 
     * @param songTitle Il titolo della canzone da mostrare.
     */
	public void setCurrentSongLabel(String songTitle) {
		if (songTitle == null || songTitle.isEmpty()) {
			currentSongLabel.setText(LanguageManager.get("label.noSong"));
		} else {
			currentSongLabel.setText(songTitle);
		}
	}

	// GETTERS E SETTERS
	
	public JList<String> getComboBox() { return comboBox; }
	public JSlider getSlider() { return slider; }
	public JButton getRewindButton() { return rewind_button; }
	public JButton getSkipButton() { return skip_button; }
	public JButton getPlayPauseButton() { return play_pause_btn; }
	public JButton getToggleModeButton() { return toggleModeButton; }
	public JButton getStopButton() { return stopButton; }
	public MP3Player getPlayer() { return player; }
	public Playlist getPlaylist() { return playlist; }
	public void setPlaylist(Playlist p) { this.playlist = p; refreshComboBox(); refreshCoverImage(); refreshPlaylistTitle(); }
	public Map<String, Playlist> getPlaylists() { return playlists; }
	public JList<String> getPlaylistList() { return playlistList; }
	public JScrollPane getScrollPane() { return scrollPane; }
	public JScrollPane getPlaylistScrollPane() { return playlistScrollPane; }
	public DefaultComboBoxModel<String> getPlaylistListModel() { return playlistListModel; }
	public JMenuBar getJMenuBar() { return menuBar; }
	public JMenuItem getCreatePlaylistItem() { return createPlaylistItem; }
	public JMenuItem getDeletePlaylistItem() { return deletePlaylistItem; }
	public JMenuItem getRenamePlaylistItem() { return renamePlaylistItem; }
	public JMenuItem getCoverMenuItem() { return coverMenuItem; }
	public JMenuItem getAddSongItem() { return addSongItem; }
	public JMenuItem getRemoveSongItem() { return removeSongItem; }
	public JMenuItem getImportSongItem() { return importSongItem; }
	public JMenuItem getRemoveMP3Item() { return removeMP3Item; }
	public JMenuItem getChangeThemeItem() { return changeThemeItem; }
	public JMenuItem getPreviousSongItem() { return previousSongItem; }
	public JMenuItem getPlayPauseItem() { return playPauseItem; }
	public JMenuItem getNextSongItem() { return nextSongItem; }
	public JMenuItem getOpenQueueItem() { return openQueueItem; }
	public JMenuItem getManageQueueItem() { return manageQueueItem; }
	public JMenuItem getSequenzialeItem() { return sequenzialeItem; }
	public JMenuItem getCasualeItem() { return casualeItem; }
	public JMenuItem getRipetiItem() { return ripetiItem; }
	public JMenuItem getAddToQueueItem() { return addToQueueItem; }
	public JMenuItem getExportSingleMP3Item() { return exportSingleMP3Item; }
	public JMenuItem getExportPlaylistItem() { return exportPlaylistItem; }
	public JMenuItem getImportPlaylistItem() { return importPlaylistItem; }
	public JMenuItem getSortPlaylistItem() { return sortPlaylistItem; }
	public JMenuItem[] getLanguagesMenuItem() { return languagesItem; }
	public JPopupMenu getPopupMenu() { return popupMenu; }
	public JMenuItem getRimuoviCanzoneItem() { return rimuoviCanzoneItem; }
	public JMenuItem getAggiungiAllaCodaItem() { return aggiungiAllaCodaItem; }
	public JMenuItem getAggiungiAllaCodaTopItem() { return aggiungiAllaCodaTopItem; }
	public JMenuItem getCercaComando() { return cercaComando; }
	public JMenuItem getInformazioniComando() { return informazioniComando; }
	public JMenuItem getInformazioniSonora() { return informazioniSonora; }
	public JMenuItem getEsci() { return esciItem; }
	public JMenuItem getTopSongsItem() { return topSongsItem; }
	public JMenuItem getTopPlaylistItem() { return topPlaylistItem; }
	public JMenuItem getPlaylistConstructor() { return playlistConstructor; }
	
	public Controller getController() { return controller; }
	public Window getWindow() { return window; }
	
	public boolean isDarkMode() { return isDarkMode; }
}
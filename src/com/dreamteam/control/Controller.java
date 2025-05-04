package com.dreamteam.control;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Frame;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.Semaphore;

import javax.swing.AbstractAction;
import javax.swing.DefaultListModel;
import javax.swing.DropMode;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.TransferHandler;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import com.dreamteam.data.PlaylistDataManager;
import com.dreamteam.model.MP3Player;
import com.dreamteam.model.Playlist;
import com.dreamteam.model.Song;
import com.dreamteam.view.Panel;

/**
 * Classe Controller principale dell'applicazione Sonora.
 * 
 * <p>Questa classe implementa il controller centrale del pattern MVC, gestendo l'interazione tra
 * l'interfaccia utente {@link com.dreamteam.view.Panel} e la logica applicativa. Si occupa della gestione
 * degli eventi generati dall'utente (clic, tastiera, slider, menu, ecc.) e coordina l'aggiornamento dell'interfaccia
 * e del player {@link com.dreamteam.model.MP3Player}.</p>
 * 
 * <h3>Responsabilità principali:</h3>
 * <ul>
 *   <li>Controllo della riproduzione audio (play, pausa, stop, next, previous).</li>
 *   <li>Gestione delle playlist: creazione, eliminazione, rinomina, importazione/esportazione, modifica.</li>
 *   <li>Gestione della coda di riproduzione personalizzata (FIFO e top insert).</li>
 *   <li>Persistenza delle configurazioni: tema, lingua, modalità di riproduzione, numero ascolti.</li>
 *   <li>Aggiornamento dello slider di avanzamento tramite un thread separato.</li>
 *   <li>Supporto per comandi da tastiera, drag & drop, e menu contestuali.</li>
 *   <li>Modalità di riproduzione: sequenziale, casuale ponderato, ripeti.</li>
 *   <li>Interfaccia multilingua e supporto per il cambio tema dinamico.</li>
 * </ul>
 * 
 * <h3>Thread:</h3>
 * La classe implementa {@code Runnable} e gestisce in modo asincrono lo stato dello slider
 * di avanzamento brano e la transizione automatica al brano successivo.
 * 
 * <h3>Note:</h3>
 * Ogni modifica all'interfaccia utente è eseguita nel thread EDT tramite {@link SwingUtilities#invokeLater}.
 * 
 * @author DreamTeam
 * @version 2.2
 * @see com.dreamteam.view.Panel
 * @see com.dreamteam.model.MP3Player
 * @see com.dreamteam.model.Playlist
 * @see com.dreamteam.data.PlaylistDataManager
 */
public class Controller implements ActionListener, ChangeListener, DocumentListener, ListSelectionListener, MouseListener, KeyListener, Runnable 
{
    private final String NAME = "Controller";
    
    public final static String THEME_CONFIG_PATH = "resources/theme.config";
    public final static String PLAYBACK_MODE_CONFIG_PATH = "resources/playbackMode.config";
    public final static String CONFIG_PATH = "config.properties";

    private Panel panel;
    private Thread thread;
    private Semaphore mutex;
    private Random random;

    private Mode playbackMode = Mode.SEQUENZIALE;
    
    //private Stack<String> playedSongs;
    private Queue<String> codaPersonalizzata;
    private String currentlyPlayingTitle;

    /**
     * Costruttore principale.
     * <p>Inizializza i componenti principali, avvia il thread per il controllo dello slider
     * e associa i key binding (es. spazio per play/pausa).</p>
     *
     * @param panel Il pannello principale dell'interfaccia utente.
     */
    public Controller(Panel panel) 
    {
        this.panel = panel;
        mutex = new Semaphore(0);
        random = new Random();

        thread = new Thread(this);
        thread.setName(NAME);
        thread.start();
        
        panel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("SPACE"), "playPause");
        panel.getActionMap().put("playPause", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                togglePlayPause();
            }
        });
        
        //playedSongs =  new Stack<>();
        codaPersonalizzata = new LinkedList<>();
        currentlyPlayingTitle = null;
    }
    
    /**
     * Carica la lingua salvata nel file di configurazione.
     *
     * @return La lingua caricata, o ITALIANO se non specificata.
     */
    public static Languages loadLanguageFromConfig() 
	{
	    try 
	    {
	        File configFile = new File(CONFIG_PATH);
	        
	        if (configFile.exists()) 
	        {
	            Properties config = new Properties();
	            config.load(new FileInputStream(configFile));
	            
	            String lang = config.getProperty("language");
	            
	            if (lang != null) 
	            {
	            	Logger.writeLog("Lingua caricata: " + Languages.valueOf(lang));
	                return Languages.valueOf(lang);
	            }
	        }
	    } 
	    catch (Exception e) 
	    {
	    	Logger.writeLog("Errore nel caricamento del file di lingua");
	    }
	    
	    Logger.writeLog("Lingua caricata: " + Languages.ITALIANO);
	    
	    return Languages.ITALIANO; // default
	}
    
    /**
     * Carica il tema dal file di configurazione.
     *
     * @return true se il tema è "dark", false altrimenti.
     */
    public static boolean loadThemeFromConfig() {
	    try {
	        File config = new File(THEME_CONFIG_PATH);
	        if (config.exists()) {
	            String value = Files.readString(config.toPath()).trim();
	            Logger.writeLog("Tema caricato dal file config");
	            return value.equalsIgnoreCase("dark");
	        }
	    } catch (IOException e) {}
	    return false;
	}
    
    /**
     * Carica la modalità di riproduzione dal file di configurazione.
     *
     * @return La modalità di riproduzione caricata, o null se non valida.
     */
    public static Mode loadModeFromConfig()
	{
		File config = new File(PLAYBACK_MODE_CONFIG_PATH);
    	
		String value = "";
		try {
			value = Files.readString(config.toPath()).trim();
		} catch (IOException e) {}
    	
		Logger.writeLog("Modalità di riproduzione caricata dal file config");
    	switch(value)
    	{
    		case "SEQUENZIALE":
    			return Mode.SEQUENZIALE;
			case "RIPETI":
	    			return Mode.RIPETI;
			case "CASUALE":
	    			return Mode.CASUALE;
    	}
		return null;
	}
    
    /**
     * Carica il numero di riproduzioni da file.
     *
     * @return Una mappa contenente il numero di riproduzioni per ciascuna canzone.
     */
    private Map<String, Integer> caricaNumeroRiproduzioni() {
        Map<String, Integer> riproduzioni = new LinkedHashMap<>();
        File file = new File("resources/plays.count");

        if (file.exists()) {
            try {
                List<String> lines = Files.readAllLines(file.toPath());
                for (String line : lines) {
                    if (!line.trim().isEmpty() && line.contains("=")) {
                        String[] parts = line.split("=", 2);
                        riproduzioni.put(parts[0].trim(), Integer.parseInt(parts[1].trim()));
                    }
                }
            } catch (IOException | NumberFormatException e) {
                Logger.writeLog("Errore nel caricamento delle riproduzioni: " + e.getMessage());
            }
        }

        return riproduzioni;
    }
    
    /**
     * Carica il numero di ascolti per ogni playlist dal file "playlist_plays.count".
     * <p>Il formato atteso del file è una serie di righe "nomePlaylist=numero". Le righe non valide vengono ignorate.
     * In caso di errore di parsing o lettura, viene registrato nel log.</p>
     *
     * @return una mappa contenente per ogni playlist il numero totale di ascolti.
     */
    private Map<String, Integer> caricaAscoltiPlaylist() {
        Map<String, Integer> ascolti = new LinkedHashMap<>();
        File file = new File("resources/playlist_plays.count");

        if (file.exists()) {
            try {
                List<String> lines = Files.readAllLines(file.toPath());
                for (String line : lines) {
                    if (!line.trim().isEmpty() && line.contains("=")) {
                        String[] parts = line.split("=", 2);
                        ascolti.put(parts[0].trim(), Integer.parseInt(parts[1].trim()));
                    }
                }
            } catch (IOException | NumberFormatException e) {
                Logger.writeLog("Errore lettura ascolti playlist: " + e.getMessage());
            }
        }

        return ascolti;
    }

    /**
     * Salva il numero di ascolti delle playlist nel file "playlist_plays.count".
     * <p>Ogni entry della mappa viene salvata nel formato "nomePlaylist=numeroAscolti".</p>
     * In caso di errore in scrittura, viene generato un messaggio nel log.
     *
     * @param ascolti mappa contenente per ogni playlist il numero totale di ascolti.
     */
    private void salvaAscoltiPlaylist(Map<String, Integer> ascolti) {
        File file = new File("resources/playlist_plays.count");
        List<String> lines = new LinkedList<>();
        for (Map.Entry<String, Integer> entry : ascolti.entrySet()) {
            lines.add(entry.getKey() + "=" + entry.getValue());
        }
        try {
            Files.write(file.toPath(), lines);
        } catch (IOException e) {
            Logger.writeLog("Errore salvataggio ascolti playlist: " + e.getMessage());
        }
    }

    /**
     * Gestisce tutti gli eventi provenienti da bottoni, menu e altre azioni.
     *
     * @param e L'evento generato dall'utente.
     */
    @Override
    public void actionPerformed(ActionEvent e) 
    {
        Object source = e.getSource();

        if (source == panel.getCreatePlaylistItem()) 
        {
        	String name = JOptionPane.showInputDialog(panel, LanguageManager.get("playlist.name"));

            if (name != null && !name.isEmpty()) 
            {
                JFileChooser fileChooser = new JFileChooser();
                fileChooser.setDialogTitle(LanguageManager.get("playlist.cover.request"));
                int result = fileChooser.showOpenDialog(panel);
                File coverImage = (result == JFileChooser.APPROVE_OPTION) ? fileChooser.getSelectedFile() : null;

                Playlist newP = new Playlist();
                newP.setName(name);

                if (coverImage != null && coverImage.exists()) {
                    try {
                        File playlistDir = new File("resources/playlists/" + name);
                        if (!playlistDir.exists()) playlistDir.mkdirs();

                        File coverDest = new File(playlistDir, "cover.jpg");
                        Files.copy(coverImage.toPath(), coverDest.toPath(), StandardCopyOption.REPLACE_EXISTING);
                        newP.setCoverImage(coverDest.getPath());
                    } catch (IOException ex) {
                        Logger.writeLog("Errore copia immagine copertina: " + ex.getMessage());
                    }
                }

                if (!panel.getPlaylists().containsKey(name)) 
                {
                    panel.getPlaylists().put(name, newP);
                    panel.getPlaylistListModel().addElement(name);
                    panel.getPlaylistList().setSelectedValue(name, true);
                    PlaylistDataManager.savePlaylists(panel.getPlaylists());
                }
            }
        } 
        else if (source == panel.getDeletePlaylistItem()) 
        {
            String selected = panel.getPlaylistList().getSelectedValue();
            if (selected != null && !selected.equals(LanguageManager.get("playlist.all"))) 
            {
                int choice = JOptionPane.showConfirmDialog(
                        panel, 
                        LanguageManager.get("playlist.delete.confirm") + selected + "?", 
                        LanguageManager.get("popup.confirm"), 
                        JOptionPane.YES_NO_OPTION);

                if (choice == JOptionPane.YES_OPTION) 
                {
                    panel.getPlaylists().remove(selected);
                    panel.getPlaylistListModel().removeElement(selected);

                    // Elimina anche la cartella della playlist
                    File playlistDir = new File("resources/playlists/" + selected);
                    if (playlistDir.exists() && playlistDir.isDirectory()) {
                        for (File file : playlistDir.listFiles()) file.delete();
                        playlistDir.delete();
                    }

                    PlaylistDataManager.savePlaylists(panel.getPlaylists());
                }
            }
            else {
                JOptionPane.showMessageDialog(panel, LanguageManager.get("playlist.all.readonly"));
            }
        }
        else if (source == panel.getRenamePlaylistItem()) 
        {
            String selected = panel.getPlaylistList().getSelectedValue();
            if (selected != null) 
            {
                String newName = JOptionPane.showInputDialog(panel, LanguageManager.get("playlist.name.new"), selected);
                if (newName != null && !newName.trim().isEmpty()) 
                {
                    Playlist pl = panel.getPlaylists().remove(selected);
                    pl.setName(newName);

                    // Rinomina la cartella fisica
                    File oldDir = new File("resources/playlists/" + selected);
                    File newDir = new File("resources/playlists/" + newName);
                    if (oldDir.exists()) oldDir.renameTo(newDir);

                    panel.getPlaylistListModel().removeElement(selected);
                    panel.getPlaylists().put(newName, pl);
                    panel.getPlaylistListModel().addElement(newName);
                    PlaylistDataManager.savePlaylists(panel.getPlaylists());
                }
            }
        }
        else if (source == panel.getSortPlaylistItem()) 
        {
            String[] options = {
                LanguageManager.get("sort.alphabetical"),
                LanguageManager.get("sort.original")
            };
            
            int choice = JOptionPane.showOptionDialog(
                panel,
                LanguageManager.get("playlist.sort.question"),
                LanguageManager.get("playlist.sort"),
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                options,
                options[0]
            );
            
            if (choice == 0) { // Ordine alfabetico
                panel.getPlaylist().sortSongsAlphabetically();
                panel.refreshComboBox();
                PlaylistDataManager.savePlaylists(panel.getPlaylists());
            } else if (choice == 1) { // Ordine originale
                panel.getPlaylist().sortSongsOriginalOrder();
                panel.refreshComboBox();
                PlaylistDataManager.savePlaylists(panel.getPlaylists());
            }
        }

        else if (source == panel.getCoverMenuItem()) 
        {
            String selected = panel.getPlaylistList().getSelectedValue();
            if (selected != null) 
            {
                JFileChooser fileChooser = new JFileChooser();
                int result = fileChooser.showOpenDialog(panel);
                if (result == JFileChooser.APPROVE_OPTION) 
                {
                    File selectedFile = fileChooser.getSelectedFile();
                    File destDir = new File("img/");
                    if (!destDir.exists()) destDir.mkdirs();

                    Playlist pl = panel.getPlaylists().get(selected);

                    // Rimuovi immagine precedente se presente
                    if (pl.getCoverImage() != null && !pl.getCoverImage().equals(Playlist.DEFAULT_IMAGE_PATH)) {
                        File oldFile = new File(pl.getCoverImage());
                        if (oldFile.exists()) oldFile.delete();
                    }

                    File destFile = new File(destDir, selectedFile.getName());
                    try 
                    {
                        Files.copy(selectedFile.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                        pl.setCoverImage(destFile.getPath());
                        PlaylistDataManager.savePlaylists(panel.getPlaylists());
                        panel.setPlaylist(pl);
                        panel.getPlaylistList().repaint();
                    } 
                    catch (IOException ex) 
                    {
                        JOptionPane.showMessageDialog(
                        		panel, 
                        		LanguageManager.get("popup.error.cover.import"), 
                        		LanguageManager.get("popup.error"), 
                        		JOptionPane.ERROR_MESSAGE);
                    }
                }
            }
        }
        else if (source == panel.getAddSongItem()) 
        {
            File songsDir = new File("resources/playlists/");
            List<File> files = new ArrayList<>();

            try {
                Files.walk(songsDir.toPath())
                     .filter(path -> path.toString().toLowerCase().endsWith(".mp3"))
                     .forEach(path -> files.add(path.toFile()));
            } catch (IOException ex) {
                Logger.writeLog("Errore durante la scansione dei file MP3: " + ex.getMessage());
            }

            // Prendi playlist corrente e titoli già presenti
            String selected = panel.getPlaylistList().getSelectedValue();
            Playlist target = panel.getPlaylists().get(selected);
            List<String> currentTitles = Arrays.asList(target.getSongTitles());

            // Mappa per evitare duplicati per titolo
            Map<String, Song> songMap = new LinkedHashMap<>();

            for (File f : files) {
                String title = f.getName().substring(0, f.getName().length() - 4).replace('_', ' ').trim();

                if (!songMap.containsKey(title) && !currentTitles.contains(title)) {
                    songMap.put(title, new Song(title, f.getPath()));
                }
            }

            List<Song> uniqueSongs = new ArrayList<>(songMap.values());

            String[] allSongTitles = uniqueSongs.stream()
                .map(Song::getTitle)
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toArray(String[]::new);

            // Crea finestra
            JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(panel), LanguageManager.get("playlist.add.songs"), true);
            dialog.setLayout(new BorderLayout());
            dialog.setSize(400, 400);
            dialog.setLocationRelativeTo(panel);

            JTextField searchField = new JTextField();
            DefaultListModel<String> listModel = new DefaultListModel<>();
            JList<String> songList = new JList<>(listModel);
            songList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
            JScrollPane scrollPane = new JScrollPane(songList);

            for (String s : allSongTitles) listModel.addElement(s);

            searchField.getDocument().addDocumentListener(new DocumentListener() {
                public void insertUpdate(DocumentEvent e) { filter(); }
                public void removeUpdate(DocumentEvent e) { filter(); }
                public void changedUpdate(DocumentEvent e) { filter(); }

                private void filter() {
                    String text = searchField.getText().toLowerCase();
                    listModel.clear();
                    for (String song : allSongTitles) {
                        if (song.toLowerCase().contains(text)) {
                            listModel.addElement(song);
                        }
                    }
                }
            });

            JButton addButton = new JButton(LanguageManager.get("label.add"));
            addButton.addActionListener(ev -> {
                List<String> selectedTitles = songList.getSelectedValuesList();
                if (selectedTitles.isEmpty()) return;

                int added = 0;
                for (String title : selectedTitles) {
                    Song songToAdd = songMap.get(title);
                    if (songToAdd != null) {
                        target.addSong(songToAdd);
                        added++;
                    }
                }

                if (added > 0) {
                    panel.setPlaylist(target);
                    PlaylistDataManager.savePlaylists(panel.getPlaylists());
                }
                dialog.dispose();
            });

            dialog.add(searchField, BorderLayout.NORTH);
            dialog.add(scrollPane, BorderLayout.CENTER);
            dialog.add(addButton, BorderLayout.SOUTH);

            dialog.setVisible(true);
        }
        else if (e.getSource() == panel.getRemoveMP3Item()) 
        {
            JFileChooser fileChooser = new JFileChooser(new File("resources/playlists/"));
            
            fileChooser.setDialogTitle(LanguageManager.get("popup.select.mp3.remove"));
            
            int result = fileChooser.showOpenDialog(panel);
            
            if (result == JFileChooser.APPROVE_OPTION) 
            {
                File selectedFile = fileChooser.getSelectedFile();
                String fileName = selectedFile.getName();
                String titleToRemove = fileName.substring(0, fileName.length() - 4).replace('_', ' ').trim();

                int choice = JOptionPane.showConfirmDialog(
                    panel,
                    LanguageManager.get("popup.confirm.delete") + fileName + LanguageManager.get("popup.confirm.delete2"),
                    LanguageManager.get("popup.delete.confirm"),
                    JOptionPane.YES_NO_OPTION
                );

                if (choice == JOptionPane.YES_OPTION) 
                {
                    boolean deleted = selectedFile.delete();
                    
                    if (deleted) 
                    {
                        for (Playlist pl : panel.getPlaylists().values()) 
                        {
                            pl.removeSong(titleToRemove);
                        }

                        PlaylistDataManager.savePlaylists(panel.getPlaylists());

                        String selected = panel.getPlaylistList().getSelectedValue();
                        
                        if (selected != null && panel.getPlaylists().containsKey(selected)) 
                        {
                            panel.setPlaylist(panel.getPlaylists().get(selected));
                        }

                        JOptionPane.showMessageDialog(
                        		panel, 
                        		LanguageManager.get("popup.deleted.file"));
                    } 
                    else 
                    {
                        JOptionPane.showMessageDialog(
                        		panel, 
                        		LanguageManager.get("popup.error.file.delete"), 
                        		LanguageManager.get("popup.error"), 
                        		JOptionPane.ERROR_MESSAGE);
                    }
                }
            }
        } 
        else if (source == panel.getRemoveSongItem()) 
        {
            String selectedSong = panel.getComboBox().getSelectedValue();
            
            if (selectedSong != null) 
            {
                panel.getPlaylist().removeSong(selectedSong);
                
                PlaylistDataManager.savePlaylists(panel.getPlaylists());
                
                panel.refreshComboBox();
                
                stopPlayback();
            }

        } 
        else if (source == panel.getImportSongItem()) 
        {
            JFileChooser fileChooser = new JFileChooser();
            
            int result = fileChooser.showOpenDialog(panel);
            
            if (result == JFileChooser.APPROVE_OPTION) 
            {
                File selectedFile = fileChooser.getSelectedFile();
                
                if (!selectedFile.getName().toLowerCase().endsWith(".mp3")) 
                {
                    JOptionPane.showMessageDialog(
                    		panel, 
                    		LanguageManager.get("popup.select.mp3"), 
                    		LanguageManager.get("popup.error"), 
                    		JOptionPane.ERROR_MESSAGE);
                    return;
                }
                
                File destinationDir = new File("resources/playlists");
                
                if (!destinationDir.exists()) destinationDir.mkdirs();
                
                File destinationFile = new File(destinationDir, selectedFile.getName());
                
                Logger.writeLog("Destination file: " + destinationFile);
                
                try 
                {
                    Files.copy(selectedFile.toPath(), destinationFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    
                    // Ottieni il nome del file senza estensione
                    String title = selectedFile.getName().substring(0, selectedFile.getName().length() - 4).replace('_', ' ').trim();
                    
                    Logger.writeLog("Song title: " + title);
                    
                    // Aggiungilo alla playlist corrente se non già presente
                    Playlist currentPlaylist = panel.getPlaylist();
                    if (!Arrays.asList(currentPlaylist.getSongTitles()).contains(title)) {
                    	currentPlaylist.addSong(title);
                        PlaylistDataManager.savePlaylists(panel.getPlaylists());
                        panel.refreshComboBox();
                    }

                    // Se il titolo non è già nella coda (comboBox), aggiungilo
                    DefaultListModel<String> model = (DefaultListModel<String>) panel.getComboBox().getModel();
                    if (!model.contains(title)) {
                        model.addElement(title);
                    }

                    JOptionPane.showMessageDialog(panel, LanguageManager.get("popup.song.import.success"));

                } 
                catch (IOException ex) 
                {
                    JOptionPane.showMessageDialog(
                    		panel, 
                    		LanguageManager.get("popup.file.import.error"), 
                    		LanguageManager.get("popup.error"), 
                    		JOptionPane.ERROR_MESSAGE);
                }
            }
        } 
        else if(source == panel.getChangeThemeItem())
        {
            panel.toggleTheme();
            salvaTema(panel.isDarkMode());
            Logger.writeLog(LanguageManager.get("theme.changed"));
        }
        else if (source == panel.getTopSongsItem()) {
            mostraTopBrani();
        }
        else if (source == panel.getTopPlaylistItem())
        {
        	mostraTopPlaylist();
        }
        else if(source == panel.getStopButton())
        {
        	//mostraCoda();
        	mostraGestioneCoda();
        }
        else if (source == panel.getPreviousSongItem()) {
            previousSong();
        }
        else if (source == panel.getPlayPauseItem()) {
            panel.getPlayPauseButton().doClick();
        }
        else if (source == panel.getNextSongItem()) {
            nextSong();
        }
        else if (source == panel.getOpenQueueItem()) {
            mostraCoda();
        }
        else if (source == panel.getManageQueueItem()) {
            mostraGestioneCoda();
        }
        else if (source == panel.getAddToQueueItem()) {
            mostraAggiungiInCoda();
        }
        else if (source == panel.getExportSingleMP3Item()) {
            esportaSingoloMP3();
        } else if (source == panel.getExportPlaylistItem()) {
            esportaPlaylist();
        } else if (source == panel.getImportPlaylistItem()) {
            importaPlaylist();
        }
        else if (source == panel.getSequenzialeItem()) {
            playbackMode = Mode.SEQUENZIALE;
            //panel.getToggleModeButton().setText("[]");
            panel.refreshIcons();
        }
        else if (source == panel.getCasualeItem()) {
            playbackMode = Mode.CASUALE;
            //panel.getToggleModeButton().setText("@@");
            panel.refreshIcons();
        }
        else if (source == panel.getRipetiItem()) {
            playbackMode = Mode.RIPETI;
            //panel.getToggleModeButton().setText("[]1");
            panel.refreshIcons();
        }
        else if (Arrays.asList(panel.getLanguagesMenuItem()).contains(source)) 
        {
            int index = Arrays.asList(panel.getLanguagesMenuItem()).indexOf(source);
            
            Languages selectedLang = Languages.values()[index];
            
            switch(selectedLang)
            {
	            case ITALIANO:
	                panel.updateLanguage(Languages.ITALIANO);
	                break;
	                
	            case ENGLISH:
	                panel.updateLanguage(Languages.ENGLISH);
	                break;
	                
	            case GIAPPONESE:
	                panel.updateLanguage(Languages.GIAPPONESE);
	                break;
	                
	            case TEDESCO:
	            	panel.updateLanguage(Languages.TEDESCO);
	            	break;
	            	
	            case FRANCESE:
	            	panel.updateLanguage(Languages.FRANCESE);
	            	break;
	            
	            case CINESE:
	            	panel.updateLanguage(Languages.CINESE);
	            	break;
	            	
	            case COREANO:
	            	panel.updateLanguage(Languages.COREANO);
	            	break;
            }
        }
        else if (source == panel.getCercaComando())
        {
        	mostraRicercaComandi();
        }
        else if (source == panel.getInformazioniComando())
        {
        	mostraInformazioniComandi();
        }
        else if (source == panel.getInformazioniSonora())
        {
        	JOptionPane.showMessageDialog(panel, LanguageManager.get("message.sonora"));
        }
        else if (source == panel.getEsci())
        {
        	panel.getWindow().dispatchEvent(new WindowEvent(panel.getWindow(), WindowEvent.WINDOW_CLOSING));
        }
        else if (source == panel.getPlayPauseButton()) 
        {
        	togglePlayPause();        
        }
        else if (source == panel.getSkipButton()) 
        {
            nextSong();
        } 
        else if (source == panel.getRewindButton()) 
        {
            try 
            {
            	previousSong();
            } 
            catch (Exception ignored) {}

        }
        else if (source == panel.getRimuoviCanzoneItem())
        {
        	String selectedSong = panel.getComboBox().getSelectedValue();
		    if (selectedSong != null) {
		        rimuoviCanzoneDaPlaylist(selectedSong);
		    }
        }
        else if (source == panel.getAggiungiAllaCodaItem())
        {
        	String selectedSong = panel.getComboBox().getSelectedValue();
		    if (selectedSong != null) {
		        aggiungiAllaCoda(selectedSong);
		    }
        }
        else if (source == panel.getAggiungiAllaCodaTopItem())
        {
        	String selectedSong = panel.getComboBox().getSelectedValue();
		    if (selectedSong != null) {
		    	aggiungiAllaCodaTop(selectedSong);
		    }
        }
        else if (source == panel.getToggleModeButton()) 
        {
            switch (playbackMode)
            {
                case SEQUENZIALE:
                    playbackMode = Mode.CASUALE;
                    //panel.getToggleModeButton().setText("@@");
                    break;
                    
                case CASUALE:
                    playbackMode = Mode.RIPETI;
                    //panel.getToggleModeButton().setText("[]1");
                    break;
                    
                case RIPETI:
                    playbackMode = Mode.SEQUENZIALE;
                    //panel.getToggleModeButton().setText("[]");
                    break;
            }
            
            salvaRiproduzione();
            
            panel.refreshIcons();
        }
    }

    /**
     * Gestisce il cambiamento dello slider di avanzamento.
     *
     * @param e Evento di cambiamento del valore.
     */
    @Override
    public void stateChanged(ChangeEvent e) 
    {
        if (Thread.currentThread().getName().equals(NAME)) return;

        JSlider slider = panel.getSlider();
        
        if (e.getSource() == slider && !slider.getValueIsAdjusting()) 
        {
            stopPlayback();
            panel.getPlayer().seekToPercentage(slider.getValue());
            
            mutex.release();
        }
    }

    /**
     * Gestisce la selezione delle liste (playlist o canzoni).
     *
     * @param e L'evento di selezione.
     */
    @Override
    public void valueChanged(ListSelectionEvent e) 
    {
        if (!e.getValueIsAdjusting()) 
        {
            if (e.getSource() == panel.getPlaylistList()) 
            {
                String selected = panel.getPlaylistList().getSelectedValue();
                Playlist selectedPlaylist = panel.getPlaylists().get(selected);
                
                if (selectedPlaylist != null) 
                {
                    panel.setPlaylist(selectedPlaylist);
                    panel.refreshCoverImage();
                    panel.refreshPlaylistTitle();
                }
            } 
            else if (e.getSource() == panel.getComboBox()) 
            {
                playSelectedSong();
            }
        }
    }
    
    /**
     * Ferma la riproduzione corrente svuotando i permit e chiamando stop() sul player.
     */
    private void stopPlayback() 
    {
        mutex.drainPermits();
        panel.getPlayer().stop();
    }

    /**
     * Avvia o mette in pausa la riproduzione a seconda dello stato del player.
     */
    private void togglePlayPause() {
        MP3Player player = panel.getPlayer();

        //if (panel.getPlayPauseButton().getText().equals(">")) {
        if (!player.isPlaying()) {
            if (player.getPausedPosition() == 0) {
            	if (panel.getComboBox().getSelectedIndex() == -1 && panel.getComboBox().getModel().getSize() > 0) {
                    panel.getComboBox().setSelectedIndex(0);
                }
            	
                playSelectedSong();
            } else {
                player.resume();
            }
            //panel.getPlayPauseButton().setText("||");
            mutex.release();
        } else {
            player.pause();
            //panel.getPlayPauseButton().setText(">");
        }
        
        panel.refreshIcons();
    }

    /**
     * Riproduce la canzone attualmente selezionata nella comboBox.
     */
    private void playSelectedSong() 
    {
        String selected = panel.getComboBox().getSelectedValue();
        
        if (!Arrays.asList(panel.getPlaylist().getSongTitles()).contains(selected)) {
    	    Logger.writeLog("Errore: canzone selezionata non esiste nella playlist corrente");
    	    return;
    	}

        if (selected != null) 
        {
            MP3Player player = panel.getPlayer();

            stopPlayback();
            
            /*
            if (currentlyPlayingTitle != null) {
                playedSongs.push(currentlyPlayingTitle); // salva nello storico
            }
            */
            
            System.out.println(selected);

            player.play(panel.getPlaylist().getSong(selected));

            panel.getComboBox().setSelectedValue(selected, true);
            //panel.getPlayPauseButton().setText("||");
            panel.refreshIcons();

            currentlyPlayingTitle = selected; // salva la canzone attuale

            mutex.release();
        }
        
        panel.setCurrentSongLabel(currentlyPlayingTitle);
        
        Map<String, Integer> riproduzioni = caricaNumeroRiproduzioni();
        riproduzioni.put(selected, riproduzioni.getOrDefault(selected, 0) + 1);
        salvaNumeroRiproduzioni(riproduzioni);
        
        Map<String, Integer> ascoltiPlaylist = caricaAscoltiPlaylist();
        String playlistName = panel.getPlaylist().getName();
        ascoltiPlaylist.put(playlistName, ascoltiPlaylist.getOrDefault(playlistName, 0) + 1);
        salvaAscoltiPlaylist(ascoltiPlaylist);

    }
    
    /**
     * Restituisce il titolo di una canzone scelto casualmente tra quelli della playlist corrente,
     * utilizzando una probabilità ponderata inversamente proporzionale al numero di riproduzioni.
     * <p>
     * Le canzoni con meno riproduzioni hanno una probabilità maggiore di essere selezionate,
     * in modo da bilanciare la varietà durante la riproduzione in modalità CASUALE.
     * <br>
     * Funzionalità dell'algoritmo:
     * <ul>
     * <li>Recupera tutte le canzoni nella playlist corrente.</li>
     * <li>Carica il numero di riproduzioni per ciascuna canzone da un file.</li>
     * <li>Trova la canzone con il numero massimo di riproduzioni e salva questo valore come maxRiproduzioni.</li>
     * <li>Per ogni canzone, calcola il suo peso inverso: <code>peso = maxRiproduzioni - numeroRiproduzioniCanzone</code>
     * <br>Quindi più riproduzioni ha più il peso è basso e meno copie in lista ci sono.</li>
     * <li>Crea una lista "ponderata" (pool) in cui ogni canzone appare tante volte quanto il suo peso.</li>
     * <li>Pesca un titolo casuale dalla pool usando random.</li>
     * <ul><br>
     *
     * @return Il titolo della canzone selezionata, oppure {@code null} se la playlist è vuota.
     */
    private String getBranoCasualePonderato() {
        JList<String> list = panel.getComboBox();
        Map<String, Integer> riproduzioni = caricaNumeroRiproduzioni();

        if (list.getModel().getSize() == 0) return null;

        List<String> pool = new LinkedList<>();
        int maxRiproduzioni = 1;

        // Calcola il massimo numero di riproduzioni tra tutte le canzoni
        for (int i = 0; i < list.getModel().getSize(); i++) {
            String title = list.getModel().getElementAt(i);
            int riproduzioniCanzone = riproduzioni.getOrDefault(title, 0);
            if (riproduzioniCanzone + 1 > maxRiproduzioni) maxRiproduzioni = riproduzioniCanzone + 1;
        }

        // Costruisci una lista ponderata
        for (int i = 0; i < list.getModel().getSize(); i++) {
            String title = list.getModel().getElementAt(i);
            int riproduzioniCanzone = riproduzioni.getOrDefault(title, 0);
            int weight = maxRiproduzioni - riproduzioniCanzone;

            for (int j = 0; j < weight; j++) {
                pool.add(title);
            }
        }

        if (pool.isEmpty()) return list.getModel().getElementAt(0); // fallback

        return pool.get(random.nextInt(pool.size()));
    }


	/**
	 * Riproduce la canzone successiva secondo la modalità attuale o la coda personalizzata.
	 */
    private void nextSong() 
    {
        JList<String> list = panel.getComboBox();

        String nextTitle = null;

        // PRIMA: Controlla la coda personalizzata
        if (!codaPersonalizzata.isEmpty()) {
            nextTitle = codaPersonalizzata.poll();
        } else {
            // TROVA indice della canzone attualmente in riproduzione
            int currentIndex = -1;
            for (int i = 0; i < list.getModel().getSize(); i++) {
                if (list.getModel().getElementAt(i).equals(currentlyPlayingTitle)) {
                    currentIndex = i;
                    break;
                }
            }

            if (currentIndex == -1) {
                currentIndex = list.getSelectedIndex(); // fallback
            }

            switch (playbackMode) 
            {
                case CASUALE:
                    nextTitle = getBranoCasualePonderato();
                    break;

                case SEQUENZIALE:
                    int nextIndex = (currentIndex < list.getModel().getSize() - 1) ? currentIndex + 1 : 0;
                    nextTitle = list.getModel().getElementAt(nextIndex);
                    break;

                case RIPETI:
                    nextTitle = currentlyPlayingTitle;
                    break;
            }
        }

        if (nextTitle == null) return;

        // AGGIORNA GUI E RIPRODUZIONE
        stopPlayback();									// blocca la precedente (drainPermits incluso)

        for(int i = 0; i < list.getModel().getSize(); i++)
        {
        	String s = nextTitle;
        	
        	if(s.equalsIgnoreCase(list.getModel().getElementAt(i)))
        	{
        		list.setSelectedIndex(i);
        		break;
        	}
        }
        
        currentlyPlayingTitle = nextTitle;               // aggiorna la canzone in riproduzione
        
        var song = panel.getPlaylist().getSong(nextTitle);

        if (song == null) {
        	Logger.writeLog(LanguageManager.get("popup.song.title.notfound") + nextTitle + "'");
            Logger.writeLog(LanguageManager.get("popup.song.available"));
            for (String titolo : panel.getPlaylist().getSongTitles()) {
            	Logger.writeLog(" - '" + titolo + "'");
            }
            return;
        }
        
        Logger.writeLog(LanguageManager.get("label.play") + nextTitle);

        SwingUtilities.invokeLater(() -> {
            panel.getPlayer().play(song); // parte il nuovo thread già gestito internamente
            panel.refreshIcons();
            panel.setCurrentSongLabel(currentlyPlayingTitle);
            mutex.release(); // rilascia dopo aggiornamento GUI
        });
    }
    
    /**
     * Torna alla canzone precedente nella lista, oppure all'ultima se si è all'inizio.
     */
    private void previousSong() 
    {
        try {
			if(panel.getPlayer().getCurrentPercentage() <= 4)
			{
				JList<String> list = panel.getComboBox();

			    int currentIndex = -1;
			    for (int i = 0; i < list.getModel().getSize(); i++) {
			        if (list.getModel().getElementAt(i).equals(currentlyPlayingTitle)) {
			            currentIndex = i;
			            break;
			        }
			    }

			    if (currentIndex == -1) currentIndex = list.getSelectedIndex(); // fallback di sicurezza

			    int prevIndex = (currentIndex > 0) ? currentIndex - 1 : list.getModel().getSize() - 1;

			    list.setSelectedIndex(prevIndex);
			    currentlyPlayingTitle = list.getModel().getElementAt(prevIndex); // aggiorna titolo in riproduzione
			    
			    panel.setCurrentSongLabel(currentlyPlayingTitle);
			}
			else
			{
				stopPlayback();
			    panel.getPlayer().seekToPercentage(0);
			    mutex.release();
			}
        } catch (Exception e) {
			Logger.writeLog(e.getMessage());
		}
    	
    	/*
    	try {
			if(panel.getPlayer().getCurrentPercentage() <= 4)
			{
				if (!playedSongs.isEmpty()) {
					JList<String> list = panel.getComboBox();
			        String previous = playedSongs.pop();
			        if (previous != null) {
			        	list.setSelectedValue(previous, true);

			            playSelectedSong();
			            panel.setCurrentSongLabel(previous);
			        }
			    } else {
			    	stopPlayback();
			        panel.getPlayer().seekToPercentage(0);
			        mutex.release();
			    }
			}
			else
			{
				stopPlayback();
			    panel.getPlayer().seekToPercentage(0);
			    mutex.release();
			}
		} catch (Exception e) {
			Logger.writeLog(e.getMessage());
		}
		*/
    }
    
    /**
     * Mostra una finestra con la coda attuale di canzoni in attesa.
     */
    private void mostraCoda() {
        if (codaPersonalizzata.isEmpty()) {
            JOptionPane.showMessageDialog(panel, LanguageManager.get("queue.empty"), LanguageManager.get("queue.queue"), JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        StringBuilder sb = new StringBuilder(LanguageManager.get("queue.show") + ":\n");
        for (String song : codaPersonalizzata) {
            sb.append("- ").append(song).append("\n");
        }

        JTextArea textArea = new JTextArea(sb.toString());
        textArea.setEditable(false);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);

        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setPreferredSize(new Dimension(300, 200));

        JOptionPane.showMessageDialog(panel, scrollPane, LanguageManager.get("queue.show"), JOptionPane.INFORMATION_MESSAGE);
    }
    

	/**
	 * Mostra una finestra per gestire e riordinare la coda personalizzata.
	 */
    private void mostraGestioneCoda() {
        try
        {
        	if (codaPersonalizzata.isEmpty()) {
                JOptionPane.showMessageDialog(panel, LanguageManager.get("queue.empty"), LanguageManager.get("queue.manage"), JOptionPane.INFORMATION_MESSAGE);
                return;
            }

            JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(panel), LanguageManager.get("queue.manage"), true);
            dialog.setLayout(new BorderLayout());
            dialog.setSize(400, 300);
            dialog.setLocationRelativeTo(panel);

            DefaultListModel<String> codaModel = new DefaultListModel<>();
            codaPersonalizzata.forEach(codaModel::addElement);

            JList<String> codaList = new JList<>(codaModel);
            codaList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            codaList.setDragEnabled(true);
            codaList.setDropMode(DropMode.INSERT);
            codaList.setTransferHandler(new TransferHandler() {
                private int fromIndex = -1;

                @Override
                protected Transferable createTransferable(JComponent c) {
                    fromIndex = codaList.getSelectedIndex();
                    return new StringSelection(codaList.getSelectedValue());
                }

                @Override
                public int getSourceActions(JComponent c) {
                    return MOVE;
                }

                @Override
                public boolean canImport(TransferHandler.TransferSupport support) {
                    return support.isDataFlavorSupported(DataFlavor.stringFlavor);
                }

                @Override
                public boolean importData(TransferHandler.TransferSupport support) {
                    try {
                        int toIndex = ((JList.DropLocation) support.getDropLocation()).getIndex();
                        String dragged = (String) support.getTransferable().getTransferData(DataFlavor.stringFlavor);

                        if (fromIndex >= 0 && toIndex >= 0 && fromIndex != toIndex) {
                            // Rimuovi prima l'elemento
                            codaModel.remove(fromIndex);

                            // Se l'hai trascinato in fondo (oltre la fine), rimuove dalla coda
                            if (toIndex >= codaModel.getSize()) {
                                //toIndex = codaModel.getSize();
                            	codaModel.remove(toIndex);
                            }

                            codaModel.add(toIndex, dragged);
                            return true;
                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    return false;
                }
            });

            JScrollPane scrollPane = new JScrollPane(codaList);
            dialog.add(scrollPane, BorderLayout.CENTER);

            JButton salvaButton = new JButton(LanguageManager.get("label.save"));
            salvaButton.addActionListener(e -> {
                // Aggiorna la vera coda
                codaPersonalizzata.clear();
                for (int i = 0; i < codaModel.size(); i++) {
                    codaPersonalizzata.add(codaModel.getElementAt(i));
                }
                dialog.dispose();
            });

            dialog.add(salvaButton, BorderLayout.SOUTH);
            dialog.setVisible(true);
        }
        catch(ArrayIndexOutOfBoundsException e)
        {
        	Logger.writeLog(e.getMessage());
        }
    }
    
    /**
     * Mostra una finestra per aggiungere più canzoni alla coda personalizzata.
     */
    private void mostraAggiungiInCoda() {
        String[] allSongs = panel.getPlaylist().getAllSongs();
        Arrays.sort(allSongs, String.CASE_INSENSITIVE_ORDER);

        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(panel), LanguageManager.get("queue.add"), true);
        dialog.setLayout(new BorderLayout());
        dialog.setSize(400, 400);
        dialog.setLocationRelativeTo(panel);

        JTextField searchField = new JTextField();
        DefaultListModel<String> listModel = new DefaultListModel<>();
        JList<String> songList = new JList<>(listModel);
        songList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        JScrollPane scrollPane = new JScrollPane(songList);

        for (String s : allSongs) listModel.addElement(s);

        searchField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { filter(); }
            public void removeUpdate(DocumentEvent e) { filter(); }
            public void changedUpdate(DocumentEvent e) { filter(); }

            private void filter() {
                String text = searchField.getText().toLowerCase();
                listModel.clear();
                for (String song : allSongs) {
                    if (song.toLowerCase().contains(text)) {
                        listModel.addElement(song);
                    }
                }
            }
        });

        JButton addButton = new JButton(LanguageManager.get("queue.add"));
        addButton.addActionListener(ev -> {
            List<String> selectedSongs = songList.getSelectedValuesList();
            if (selectedSongs.isEmpty()) return;

            for (String song : selectedSongs) {
                if (!codaPersonalizzata.contains(song)) {
                    codaPersonalizzata.add(song);
                }
            }

            dialog.dispose();
        });

        dialog.add(searchField, BorderLayout.NORTH);
        dialog.add(scrollPane, BorderLayout.CENTER);
        dialog.add(addButton, BorderLayout.SOUTH);

        dialog.setVisible(true);
        
        JOptionPane.showMessageDialog(panel, LanguageManager.get("queue.added.multiple"));
    }
    
    /**
     * Mostra i 10 brani più ascoltati con un grafico
     */
    private void mostraTopBrani() {
        Map<String, Integer> riproduzioni = caricaNumeroRiproduzioni();

        List<Map.Entry<String, Integer>> top = new ArrayList<>(riproduzioni.entrySet());
        top.sort((a, b) -> b.getValue() - a.getValue()); // ordinati decrescente

        int maxAscolti = top.isEmpty() ? 1 : top.get(0).getValue();
        int count = 0;

        StringBuilder sb = new StringBuilder(LanguageManager.get("label.topSongs") + ":\n\n");

        for (Map.Entry<String, Integer> entry : top) {
            String title = entry.getKey();
            int ascolti = entry.getValue();

            int barraLength = (int) ((ascolti / (double) maxAscolti) * 20); // max 20 caratteri
            String barra = "█".repeat(barraLength);

            sb.append(String.format("%2d. %-20s %s %d %s\n", ++count, title, barra, ascolti, LanguageManager.get("label.plays")));

            if (count >= 10) break; // top 10
        }

        JTextArea area = new JTextArea(sb.toString());
        area.setEditable(false);
        area.setFont(new Font("Monospaced", Font.PLAIN, 12)); // font a larghezza fissa
        area.setLineWrap(false); // meglio disattivare il wrap

        JScrollPane scroll = new JScrollPane(area);
        scroll.setPreferredSize(new Dimension(500, 300));

        JOptionPane.showMessageDialog(panel, scroll, LanguageManager.get("item.topSongs"), JOptionPane.INFORMATION_MESSAGE);
    }
    
    private void mostraTopPlaylist() {
        Map<String, Integer> ascolti = caricaAscoltiPlaylist();
        List<Map.Entry<String, Integer>> top = new ArrayList<>(ascolti.entrySet());
        top.sort((a, b) -> b.getValue() - a.getValue());

        int max = top.isEmpty() ? 1 : top.get(0).getValue();
        int count = 0;

        StringBuilder sb = new StringBuilder(LanguageManager.get("item.topPlaylist") + ":\n\n");

        for (Map.Entry<String, Integer> entry : top) {
            String name = entry.getKey();
            int val = entry.getValue();
            String bar = "█".repeat((int)((val / (double)max) * 20));
            sb.append(String.format("%2d. %-20s %s %d " + LanguageManager.get("label.plays") + "\n", ++count, name, bar, val));
            if (count >= 10) break;
        }

        JTextArea area = new JTextArea(sb.toString());
        area.setEditable(false);
        area.setFont(new Font("Monospaced", Font.PLAIN, 12));
        area.setLineWrap(false);

        JScrollPane scroll = new JScrollPane(area);
        scroll.setPreferredSize(new Dimension(500, 300));

        JOptionPane.showMessageDialog(panel, scroll, LanguageManager.get("item.topPlaylist"), JOptionPane.INFORMATION_MESSAGE);
    }

    
    /**
     * Aggiunge una canzone alla coda di riproduzione, se non già presente.
     *
     * @param titolo Il titolo della canzone da aggiungere.
     */
    public void aggiungiAllaCoda(String titolo) {
        if (!codaPersonalizzata.contains(titolo)) {
            codaPersonalizzata.add(titolo);
            Logger.writeLog(LanguageManager.get("queue.added") + titolo);
        }
    }
    
    /**
     * Aggiunge una canzone in cima alla coda di riproduzione.
     *
     * @param titolo Il titolo della canzone da posizionare in testa alla coda.
     */
    public void aggiungiAllaCodaTop(String titolo) {
    	Queue<String> coda = new LinkedList<>();
    	
    	if (!codaPersonalizzata.contains(titolo)) {
    		coda.add(titolo);
    		
    		for(String s : codaPersonalizzata)
    		{
    			coda.add(s);
    		}
    		
            codaPersonalizzata = coda;
            Logger.writeLog(LanguageManager.get("queue.added") + titolo);
        }
    }
    
    /**
     * Esporta un singolo file MP3 in una directory scelta dall'utente.
     */
    private void esportaSingoloMP3() {
        JFileChooser chooser = new JFileChooser(new File("resources/playlists/"));
        chooser.setDialogTitle(LanguageManager.get("popup.choose.mp3"));
        int result = chooser.showOpenDialog(panel);
        if (result == JFileChooser.APPROVE_OPTION) {
            File source = chooser.getSelectedFile();

            JFileChooser destChooser = new JFileChooser();
            destChooser.setDialogTitle(LanguageManager.get("popup.choose.save"));
            destChooser.setSelectedFile(new File(source.getName()));
            result = destChooser.showSaveDialog(panel);

            if (result == JFileChooser.APPROVE_OPTION) {
                try {
                    Files.copy(source.toPath(), destChooser.getSelectedFile().toPath(), StandardCopyOption.REPLACE_EXISTING);
                    JOptionPane.showMessageDialog(panel, LanguageManager.get("popup.export.mp3"));
                } catch (IOException ex) {
                    JOptionPane.showMessageDialog(panel, LanguageManager.get("popup.error.export"), LanguageManager.get("popup.error"), JOptionPane.ERROR_MESSAGE);
                }
            }
        }
    }

    /**
     * Esporta una playlist e tutti i file MP3 associati in una cartella.
     */
    private void esportaPlaylist() {
        String selected = panel.getPlaylistList().getSelectedValue();
        if (selected == null) {
            JOptionPane.showMessageDialog(panel, LanguageManager.get("popup.select.playlist"));
            return;
        }

        Playlist pl = panel.getPlaylists().get(selected);

        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle(LanguageManager.get("popup.choose.folder"));
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

        if (chooser.showOpenDialog(panel) == JFileChooser.APPROVE_OPTION) {
            File destDir = chooser.getSelectedFile();
            File exportFolder = new File(destDir, pl.getName());

            try {
                PlaylistDataManager.exportPlaylist(pl, exportFolder);
                JOptionPane.showMessageDialog(panel, LanguageManager.get("popup.export.success"));
            } catch (IOException e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(panel, LanguageManager.get("popup.error.export"), LanguageManager.get("popup.error"), JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /**
     * Importa una playlist da un file `.playlist.json` e copia i relativi MP3 se non presenti.
     */
    private void importaPlaylist() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle(LanguageManager.get("popup.choose.json"));

        int result = chooser.showOpenDialog(panel);
        if (result == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();

            try {
                Playlist imported = PlaylistDataManager.importPlaylist(file);
                String name = imported.getName();

                if (!panel.getPlaylists().containsKey(name)) {
                    panel.getPlaylists().put(name, imported);
                    panel.getPlaylistListModel().addElement(name);
                    PlaylistDataManager.savePlaylists(panel.getPlaylists());
                    JOptionPane.showMessageDialog(panel, LanguageManager.get("popup.import.success"));
                } else {
                    JOptionPane.showMessageDialog(panel, LanguageManager.get("popup.import.duplicate"));
                }

            } catch (IOException e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(panel, LanguageManager.get("popup.error.import"), LanguageManager.get("popup.error"), JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    
    /**
     * Salva la configurazione del tema attuale (dark/light) su file.
     *
     * @param isDarkMode true se il tema è scuro, false se chiaro.
     */
    private void salvaTema(boolean isDarkMode) {
        try {
            File config = new File("resources/theme.config");
            Files.writeString(config.toPath(), isDarkMode ? "dark" : "light");
        } catch (IOException e) {
        	Logger.writeLog("Errore nel salvataggio del tema: " + e.getMessage());
        }
    }
    
    /**
     * Salva la modalità di riproduzione attuale nel file di configurazione.
     */
    private void salvaRiproduzione()
    {
    	File config = new File("resources/playbackMode.config");
    	
    	String r = "";
    	
    	switch(playbackMode)
    	{
    		case SEQUENZIALE:
    			r = "SEQUENZIALE";
    			break;
    			
    		case RIPETI:
    			r = "RIPETI";
    			break;
    			
    		case CASUALE:
    			r = "CASUALE";
    			break;
    	}
    	
    	try 
    	{
			Files.writeString(config.toPath(), r);
		} 
    	catch (IOException e) {}
    }
    
    /**
     * Salva il numero di riproduzioni su file.
     *
     * @param riproduzioni Mappa contenente il numero di riproduzioni per ciascuna canzone.
     */
    private void salvaNumeroRiproduzioni(Map<String, Integer> riproduzioni) {
        File file = new File("resources/plays.count");
        List<String> lines = new LinkedList<>();
        for (Map.Entry<String, Integer> entry : riproduzioni.entrySet()) {
            lines.add(entry.getKey() + "=" + entry.getValue());
        }
        try {
            Files.write(file.toPath(), lines);
        } catch (IOException e) {
            Logger.writeLog("Errore nel salvataggio delle riproduzioni: " + e.getMessage());
        }
    }

    
    public void setPlaybackMode(Mode mode)
    {
    	playbackMode = mode;
    }
    
    /**
     * Rimuove una canzone dalla playlist attualmente selezionata.
     *
     * @param title Il titolo della canzone da rimuovere.
     */
    public void rimuoviCanzoneDaPlaylist(String title) {
        Playlist playlist = panel.getPlaylist();
        if (playlist != null) {
            playlist.removeSong(title);
            panel.refreshComboBox();
            PlaylistDataManager.savePlaylists(panel.getPlaylists());
        }
    }
    
    /**
     * Mostra una finestra per cercare i comandi del menu.
     */
    private void mostraRicercaComandi() {
        Map<String, JMenuItem> comandi = new LinkedHashMap<>();

        var menuBar = panel.getJMenuBar();

        for (int i = 0; i < menuBar.getMenuCount(); i++) {
            JMenu menu = menuBar.getMenu(i);
            if (menu == null) continue;
            String nomeMenu = menu.getText();

            for (int j = 0; j < menu.getItemCount(); j++) {
                JMenuItem item = menu.getItem(j);
                if (item != null && item.getText() != null && !item.getText().isBlank()) {
                    String nomeComando = item.getText();
                    String fullLabel = nomeComando + " [" + nomeMenu + "]";
                    comandi.put(fullLabel, item);
                }
            }
        }

        JDialog dialog = new JDialog(panel.getWindow(), LanguageManager.get("item.cercaComando"), true);
        dialog.setLayout(new BorderLayout());
        dialog.setSize(500, 400);
        dialog.setLocationRelativeTo(panel);

        JTextField searchBar = new JTextField();
        DefaultListModel<String> listModel = new DefaultListModel<>();
        JList<String> list = new JList<>(listModel);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JScrollPane scrollPane = new JScrollPane(list);

        Runnable updateList = () -> {
            String query = searchBar.getText().toLowerCase();
            listModel.clear();
            for (String key : comandi.keySet()) {
                if (key.toLowerCase().contains(query)) {
                    listModel.addElement(key);
                }
            }
            if (listModel.isEmpty()) {
                listModel.addElement(LanguageManager.get("label.noResults"));
            }
        };

        searchBar.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { updateList.run(); }
            public void removeUpdate(DocumentEvent e) { updateList.run(); }
            public void changedUpdate(DocumentEvent e) { updateList.run(); }
        });

        list.addMouseListener(new MouseListener() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && !list.isSelectionEmpty()) {
                    String selected = list.getSelectedValue();
                    JMenuItem item = comandi.get(selected);
                    if (item != null) {
                        dialog.dispose();
                        item.doClick();
                    }
                }
            }
            public void mousePressed(MouseEvent e) {}
            public void mouseReleased(MouseEvent e) {}
            public void mouseEntered(MouseEvent e) {}
            public void mouseExited(MouseEvent e) {}
        });

        dialog.add(searchBar, BorderLayout.NORTH);
        dialog.add(scrollPane, BorderLayout.CENTER);
        updateList.run(); // iniziale

        dialog.setVisible(true);
    }

    
    /**
     * Genera la mappa dei comandi dinamici (nome comando → menu di appartenenza).
     *
     * @return Mappa dei comandi.
     */
    private Map<String, String> generaMappaComandiDinamici() {
        Map<String, String> comandi = new LinkedHashMap<>(); // Ordine d'inserimento

        var menuBar = panel.getJMenuBar();

        for (int i = 0; i < menuBar.getMenuCount(); i++) {
            JMenu menu = menuBar.getMenu(i);
            if (menu == null) continue;
            String nomeMenu = menu.getText();

            for (int j = 0; j < menu.getItemCount(); j++) {
                JMenuItem item = menu.getItem(j);
                if (item != null && item.getText() != null && !item.getText().isBlank()) {
                    String nomeItem = item.getText();
                    comandi.put(nomeItem, nomeMenu + " > " + nomeItem);
                }
            }
        }

        return comandi;
    }

    /**
     * Mostra una finestra con le descrizioni dei comandi disponibili.
     */
    private void mostraInformazioniComandi() {
        Languages lang = loadLanguageFromConfig();
        String langCode = switch (lang) {
            case ITALIANO -> "it";
            case ENGLISH -> "en";
            case GIAPPONESE -> "ja";
            case TEDESCO -> "de";
            case FRANCESE -> "fr";
            case CINESE -> "zh";
            case COREANO -> "ko";
        };

        // Percorso nel classpath
        String resourcePath = "/com/dreamteam/languages/help_" + langCode + ".properties";

        InputStream input = getClass().getResourceAsStream(resourcePath);
        if (input == null) {
            JOptionPane.showMessageDialog(panel, LanguageManager.get("help.notfound"), "Errore", JOptionPane.ERROR_MESSAGE);
            return;
        }

        Properties helpTexts = new Properties();
        try {
            helpTexts.load(input);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(panel, LanguageManager.get("help.error"), "Errore", JOptionPane.ERROR_MESSAGE);
            return;
        }

        StringBuilder sb = new StringBuilder();
        for (String key : helpTexts.stringPropertyNames()) {
            sb.append("• ").append(key).append(": ")
              .append(helpTexts.getProperty(key)).append("\n\n");
        }

        JTextArea area = new JTextArea(sb.toString());
        area.setEditable(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);

        JScrollPane scrollPane = new JScrollPane(area);
        scrollPane.setPreferredSize(new Dimension(500, 400));

        JOptionPane.showMessageDialog(panel, scrollPane, LanguageManager.get("menu.help"), JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     * Esegue il thread che aggiorna continuamente la posizione dello slider e avvia la prossima canzone.
     */
    @Override
    public void run() 
    {
    	Logger.writeLog("Thread slider partito");
    	
        while (true) 
        {
            try 
            {
                mutex.acquire();
                
                while (panel.getPlayer().isPlaying()) {
                    try {
                        int perc = panel.getPlayer().getCurrentPercentage();
                        panel.getSlider().setValue(perc);

                        if (perc >= 99) {
                            break;
                        }

                        Thread.sleep(1000);
                    } catch (Exception ignored) {}
                }

                if(!panel.getPlayer().isPaused())
                {
                	nextSong(); 
                }
            } 
            catch (InterruptedException ignored) {
            	Logger.writeLog(ignored.getMessage());
            }
        }
    }

	@Override
	public void keyTyped(KeyEvent e) {}

	/**
	 * Eseguito quando viene premuto un tasto.
	 *
	 * @param e Evento del tasto premuto.
	 */
	@Override
	public void keyPressed(KeyEvent e) 
	{
		switch(e.getKeyCode())
		{
			case KeyEvent.VK_SPACE:
				if (panel.getPlayPauseButton().getText().equals(">")) 
	            {
	                if (!panel.getPlayer().isPlaying() && panel.getPlayer().getPausedPosition() == 0) 
	                {
	                    playSelectedSong();
	                } 
	                else 
	                {
	                    panel.getPlayer().resume();
	                }
	                
	                //panel.getPlayPauseButton().setText("||");
	                mutex.release();
	            } 
	            else 
	            {
	                panel.getPlayer().pause();
	                //panel.getPlayPauseButton().setText(">");
	            }
				panel.refreshIcons();
				break;
				
			default:
				break;
		}
	}

	@Override
	public void keyReleased(KeyEvent e) {}

	@Override
	public void mouseClicked(MouseEvent e) {}

	/**
	 * Metodo chiamato quando il mouse viene premuto.
	 *
	 * @param e Evento del mouse.
	 */
	@Override
	public void mousePressed(MouseEvent e) {
		if (SwingUtilities.isRightMouseButton(e)) {
            int index = panel.getComboBox().locationToIndex(e.getPoint());
            if (index != -1) {
                // Rimuovi temporaneamente il ListSelectionListener
                ListSelectionListener[] listeners = panel.getComboBox().getListSelectionListeners();
                for (ListSelectionListener l : listeners) {
                	panel.getComboBox().removeListSelectionListener(l);
                }

                panel.getComboBox().setSelectedIndex(index); // seleziona manualmente
                panel.getPopupMenu().show(panel.getComboBox(), e.getX(), e.getY());

                // Riattacca i listener dopo un piccolo delay (oppure subito dopo il popup)
                SwingUtilities.invokeLater(() -> {
                    for (ListSelectionListener l : listeners) {
                    	panel.getComboBox().addListSelectionListener(l);
                    }
                });
            }
        }
		
	}

	@Override
	public void mouseReleased(MouseEvent e) {}

	@Override
	public void mouseEntered(MouseEvent e) {}

	@Override
	public void mouseExited(MouseEvent e) {}

	/**
	 * Chiamato quando viene modificato il contenuto del campo ricerca.
	 */
	@Override
	public void insertUpdate(DocumentEvent e) {panel.filterSongs();}

	/**
	 * Chiamato quando viene modificato il contenuto del campo ricerca.
	 */
	@Override
	public void removeUpdate(DocumentEvent e) {panel.filterSongs();}

	/**
	 * Chiamato quando viene modificato il contenuto del campo ricerca.
	 */
	@Override
	public void changedUpdate(DocumentEvent e) {panel.filterSongs();}
	
	public Mode getPlaybackMode() { return playbackMode; }
}
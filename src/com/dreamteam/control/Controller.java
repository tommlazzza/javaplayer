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
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
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
import javax.swing.JPopupMenu;
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

import com.dreamteam.data.ConfigManager;
import com.dreamteam.data.FileManager;
import com.dreamteam.data.LanguageManager;
import com.dreamteam.data.PlaylistDataManager;
import com.dreamteam.languages.Languages;
import com.dreamteam.model.MP3Player;
import com.dreamteam.model.Playlist;
import com.dreamteam.model.Song;
import com.dreamteam.tools.model.PlaylistCreatorApp;
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
public class Controller implements ActionListener, ChangeListener, DocumentListener, ListSelectionListener, MouseListener, KeyListener 
{
    private final String NAME = "Controller";
    
    public final static String THEME_CONFIG_PATH = "resources/theme.config";
    public final static String PLAYBACK_MODE_CONFIG_PATH = "resources/playbackMode.config";
    public final static String CONFIG_PATH = "config.properties";
    
    private final Object songChangeLock;
    private volatile boolean changingSong;
    private volatile boolean playingSong;
    private boolean suppressPlaylistSelection;
    private boolean suppressComboBoxPlayback;
    private Playlist playbackPlaylist;

    private Panel panel;
    
    private PlaybackManager playbackManager;
    private final Semaphore playbackSemaphore;
    private Random random;

    private Mode playbackMode = Mode.SEQUENZIALE;
    
    //private Stack<String> playedSongs;
    private QueueManager codaManager;
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
    	songChangeLock = new Object();
    	changingSong = false;
    	playingSong = false;
    	suppressPlaylistSelection = false;
    	suppressComboBoxPlayback = false;
    	
        this.panel = panel;
        playbackSemaphore = new Semaphore(0);
        random = new Random();
        
        panel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("SPACE"), "playPause");
        panel.getActionMap().put("playPause", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                togglePlayPause();
            }
        });
        
        //playedSongs =  new Stack<>();
        this.codaManager = new QueueManager();
        currentlyPlayingTitle = null;
        
        this.playbackManager = new PlaybackManager(panel, playbackSemaphore);
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
        else if (source == panel.getPlaylistConstructor())
        {
        	PlaylistCreatorApp.main(null);
        }
        else if (source == panel.getDeletePlaylistItem()) {
            String selected = panel.getPlaylistList().getSelectedValue();
            if (selected != null && !selected.equals(LanguageManager.get("playlist.all"))) {
                int choice = JOptionPane.showConfirmDialog(
                    panel,
                    LanguageManager.get("playlist.delete.confirm") + selected + "?",
                    LanguageManager.get("popup.confirm"),
                    JOptionPane.YES_NO_OPTION
                );

                if (choice == JOptionPane.YES_OPTION) {
                    //blocca ogni play involontario
                    suppressComboBoxPlayback = true;

                    // determina se stiamo riproducendo la playlist selezionata
                    boolean wasPlayingThis = playbackPlaylist != null
                        && playbackPlaylist.getName().equals(selected);

                    // rimuovi la playlist dai dati e dalla JList
                    panel.getPlaylists().remove(selected);
                    panel.getPlaylistListModel().removeElement(selected);

                    // se era visualizzata, torna a "Tutti i brani"
                    String all = LanguageManager.get("playlist.all");
                    if (panel.getPlaylists().containsKey(all)
                        && panel.getPlaylist().getName().equals(selected)) {
                        Playlist tutti = panel.getPlaylists().get(all);
                        panel.setPlaylist(tutti);
                        panel.getPlaylistList().setSelectedValue(all, true);
                        panel.refreshCoverImage();
                        panel.refreshPlaylistTitle();
                        panel.refreshComboBox();
                    }

                    // ferma la riproduzione solo se era quella cancellata
                    if (wasPlayingThis) {
                        stopPlayback();
                        playbackPlaylist = null;
                        currentlyPlayingTitle = null;

                        // azzera UI
                        panel.setCurrentSongLabel("");
                        panel.getSlider().setValue(0);
                        panel.getPlayPauseButton().setEnabled(false);
                        panel.refreshIcons();
                    }

                    // sposta tutti gli MP3 nella cartella principale e poi rimuovi la playlist
                    File dir = new File("resources/playlists/" + selected);
                    File root = new File("resources/playlists");
                    if (dir.exists() && dir.isDirectory()) {
                        // sposto prima tutti i .mp3 nella root
                        for (File f : dir.listFiles()) {
                            if (f.isFile() && f.getName().toLowerCase().endsWith(".mp3")) {
                                Path src = f.toPath();
                                Path dst = root.toPath().resolve(f.getName());
                                try {
                                    Files.move(src, dst, StandardCopyOption.REPLACE_EXISTING);
                                } catch (IOException ex) {
                                    Logger.writeLog("Errore spostando " + f.getName() + ": " + ex.getMessage());
                                }
                            }
                        }
                        // poi pulisco eventuali file residui (es. cover.jpg, data.json) e rimuovo la cartella
                        for (File f : dir.listFiles()) {
                            try { f.delete(); } catch (Exception ignore) {}
                        }
                        dir.delete();
                    }

                    PlaylistDataManager.savePlaylists(panel.getPlaylists());

                    // svuota la selezione nella ComboBox
                    panel.getComboBox().setSelectedIndex(-1);

                    // riabilita il play automatico
                    suppressComboBoxPlayback = false;

                    // se stavi ascoltando quella playlist, ora seleziona e avvia la prima canzone di "Tutti i brani"
                    if (wasPlayingThis && panel.getComboBox().getModel().getSize() > 0) {
                        panel.getComboBox().setSelectedIndex(0);
                        panel.getPlayPauseButton().setEnabled(true);
                        playSelectedSong();
                    }
                }
            } else {
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

                    // Rinominazione cartella
                    File oldDir = new File("resources/playlists/" + selected);
                    File newDir = new File("resources/playlists/" + newName);
                    if (oldDir.exists()) {
                        boolean renamed = oldDir.renameTo(newDir);
                        if (renamed && pl.getCoverImage() != null) {
                            // Se l'immagine era nella vecchia cartella, aggiorna il percorso
                            File oldCover = new File(pl.getCoverImage());
                            if (oldCover.getParentFile().getName().equals(selected)) {
                                File newCover = new File(newDir, "cover.jpg");
                                pl.setCoverImage(newCover.getPath());
                            }
                        }
                    }

                    // Salva la playlist aggiornata nel nuovo percorso (aggiorna anche data.json)
                    try {
                        PlaylistDataManager.exportPlaylist(pl, newDir);
                    } catch (IOException e1) {
                        Logger.writeLog("Errore nel salvataggio dopo rinomina: " + e1.getMessage());
                    }

                    panel.getPlaylistListModel().removeElement(selected);
                    panel.getPlaylists().put(newName, pl);
                    panel.getPlaylistListModel().addElement(newName);

                    PlaylistDataManager.savePlaylists(panel.getPlaylists());

                    // Aggiorna GUI
                    panel.getPlaylistList().setSelectedValue(newName, true);
                    panel.setPlaylist(pl);
                    panel.refreshCoverImage();
                    panel.refreshPlaylistTitle();
                    panel.refreshComboBox();
                }
            }
        }
        else if (source == panel.getSortPlaylistItem()) 
        {
            String[] options = {
                LanguageManager.get("sort.alphabetical"),
                LanguageManager.get("sort.original"),
                LanguageManager.get("sort.custom")      // nuovo
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
            
            if (choice == 0) { 
                // Ordine alfabetico
                panel.getPlaylist().sortSongsAlphabetically();
                panel.refreshComboBox();
                PlaylistDataManager.savePlaylists(panel.getPlaylists());
                
            } else if (choice == 1) { 
                // Ordine originale
                panel.getPlaylist().sortSongsOriginalOrder();
                panel.refreshComboBox();
                PlaylistDataManager.savePlaylists(panel.getPlaylists());
                
            } else if (choice == 2) {
                // Riordino personalizzato
                Playlist pl = panel.getPlaylist();

                JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(panel),
                                             LanguageManager.get("sort.custom"),
                                             true);
                dialog.setLayout(new BorderLayout());
                dialog.setSize(400, 400);
                dialog.setLocationRelativeTo(panel);

                // modello con i titoli correnti
                DefaultListModel<String> listModel = new DefaultListModel<>();
                for (String title : pl.getSongTitles()) {
                    listModel.addElement(title);
                }

                JList<String> songList = new JList<>(listModel);
                songList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
                songList.setDragEnabled(true);
                songList.setDropMode(DropMode.INSERT);
                songList.setTransferHandler(new TransferHandler() {
                    private int fromIndex = -1;

                    @Override
                    public int getSourceActions(JComponent c) {
                        return MOVE;
                    }

                    @Override
                    protected Transferable createTransferable(JComponent c) {
                        fromIndex = songList.getSelectedIndex();
                        return new StringSelection(songList.getSelectedValue());
                    }

                    @Override
                    public boolean canImport(TransferSupport support) {
                        return support.isDataFlavorSupported(DataFlavor.stringFlavor);
                    }

                    @Override
                    public boolean importData(TransferSupport support) {
                        try {
                            int toIndex = ((JList.DropLocation) support.getDropLocation()).getIndex();
                            String value = (String) support.getTransferable()
                                                         .getTransferData(DataFlavor.stringFlavor);
                            if (fromIndex < 0 || toIndex < 0 || fromIndex == toIndex) return false;

                            DefaultListModel<String> model = (DefaultListModel<String>) songList.getModel();
                            String moved = model.get(fromIndex);
                            model.remove(fromIndex);
                            // se toIndex > size, clampa all’ultimo
                            if (toIndex > model.getSize()) toIndex = model.getSize();
                            model.add(toIndex, moved);
                            return true;
                        } catch (Exception ex) {
                            Logger.writeLog("Error reordering: " + ex.getMessage());
                            return false;
                        }
                    }
                });

                dialog.add(new JScrollPane(songList), BorderLayout.CENTER);

                JButton saveBtn = new JButton(LanguageManager.get("label.save"));
                saveBtn.addActionListener(ev -> {
                    // ricostruisci List<Song> in base ai titoli nell'ordine scelto
                    List<Song> newOrderSongs = new ArrayList<>();
                    for (int i = 0; i < listModel.size(); i++) {
                        String title = listModel.getElementAt(i);
                        Song s = pl.getSong(title);
                        if (s != null) {
                            newOrderSongs.add(s);
                        }
                    }
                    // applica e salva
                    pl.setCustomOrder(newOrderSongs);
                    PlaylistDataManager.savePlaylists(panel.getPlaylists());
                    panel.refreshComboBox();
                    dialog.dispose();
                });
                dialog.add(saveBtn, BorderLayout.SOUTH);

                dialog.setVisible(true);
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
	    else if (source == panel.getAddSongItem()) { // AGGIUNTA CANZONI ALLA PLAYLIST
	        // 1) cartella root da cui partire
	        File songsRoot = new File("resources/playlists");
	        List<File> files = new ArrayList<>();
	
	        try {
	            Files.walk(songsRoot.toPath())
	                 .filter(p -> p.toString().toLowerCase().endsWith(".mp3"))
	                 .forEach(p -> files.add(p.toFile()));
	        } catch (IOException ex) {
	            Logger.writeLog("Errore scansione MP3: " + ex.getMessage());
	        }
	
	        // 2) playlist corrente e titoli già presenti
	        String plName = panel.getPlaylistList().getSelectedValue();
	        Playlist target = panel.getPlaylists().get(plName);
	        List<String> existing = Arrays.asList(target.getSongTitles());
	
	        // 3) costruisco mappa titolo→Song con percorso RELATIVO
	        Map<String,Song> songMap = new LinkedHashMap<>();
	        Path projectRoot = Paths.get("").toAbsolutePath();
	        for (File f : files) {
	            String title = f.getName()
	                            .substring(0, f.getName().length() - 4)
	                            .replace('_', ' ')
	                            .trim();
	            if (!songMap.containsKey(title) && !existing.contains(title)) {
	                Path abs = f.toPath().toAbsolutePath();
	                Path rel = projectRoot.relativize(abs);
	                String relPath = rel.toString().replace("\\","/");
	                songMap.put(title, new Song(title, relPath));
	            }
	        }
	
	        // 4) lista di titoli da mostrare
	        String[] allTitles = songMap.keySet().stream()
	                                    .sorted(String.CASE_INSENSITIVE_ORDER)
	                                    .toArray(String[]::new);
	
	        // 5) dialog di selezione
	        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(panel),
	                                     LanguageManager.get("playlist.add.songs"), true);
	        dialog.setLayout(new BorderLayout());
	        dialog.setSize(400, 400);
	        dialog.setLocationRelativeTo(panel);
	
	        JTextField search = new JTextField();
	        DefaultListModel<String> model = new DefaultListModel<>();
	        JList<String> list = new JList<>(model);
	        list.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
	        JScrollPane scroll = new JScrollPane(list);
	
	        // popolo
	        for (String t : allTitles) model.addElement(t);
	
	        // filtro dinamico
	        search.getDocument().addDocumentListener(new DocumentListener() {
	            void filter() {
	                String q = search.getText().toLowerCase();
	                model.clear();
	                for (String t : allTitles) {
	                    if (t.toLowerCase().contains(q)) model.addElement(t);
	                }
	            }
	            public void insertUpdate(DocumentEvent e) { filter(); }
	            public void removeUpdate(DocumentEvent e) { filter(); }
	            public void changedUpdate(DocumentEvent e) { filter(); }
	        });
	
	        JButton addBtn = new JButton(LanguageManager.get("label.add"));
	        addBtn.addActionListener(ev -> {
	            for (String t : list.getSelectedValuesList()) {
	                Song s = songMap.get(t);
	                if (s != null) target.addSong(s);
	            }
	            panel.setPlaylist(target);
	            PlaylistDataManager.savePlaylists(panel.getPlaylists());
	            panel.refreshComboBox();
	            dialog.dispose();
	        });
	
	        dialog.add(search, BorderLayout.NORTH);
	        dialog.add(scroll, BorderLayout.CENTER);
	        dialog.add(addBtn, BorderLayout.SOUTH);
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

        } else if (source == panel.getImportSongItem()) {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle(LanguageManager.get("popup.select.mp3"));
            int result = fileChooser.showOpenDialog(panel);
            if (result != JFileChooser.APPROVE_OPTION) return;

            File selectedFile = fileChooser.getSelectedFile();
            if (!selectedFile.getName().toLowerCase().endsWith(".mp3")) {
                JOptionPane.showMessageDialog(panel,
                    LanguageManager.get("popup.select.mp3"),
                    LanguageManager.get("popup.error"),
                    JOptionPane.ERROR_MESSAGE);
                return;
            }

            String fileName = selectedFile.getName();
            String title = fileName.substring(0, fileName.length() - 4)
                                 .replace('_',' ').trim();

            Playlist current = panel.getPlaylist();

            if (title.contains(".") || title.equalsIgnoreCase(current.getName())) {
                JOptionPane.showMessageDialog(panel,
                    LanguageManager.get("popup.file.import.error"),
                    LanguageManager.get("popup.error"),
                    JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Se il titolo non è già presente
            if (!Arrays.asList(current.getSongTitles()).contains(title)) {
                // Path di destinazione
                File targetDir = current.getName().equalsIgnoreCase("Tutti i brani")
                                 ? new File("resources/playlists/")
                                 : new File("resources/playlists/" + current.getName());

                if (!targetDir.exists()) targetDir.mkdirs();

                // Nuovo file di destinazione
                File destFile = new File(targetDir, selectedFile.getName());

                try {
                    Files.copy(selectedFile.toPath(), destFile.toPath(),
                               StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException ex) {
                    Logger.writeLog("Errore copia MP3: " + ex.getMessage());
                    JOptionPane.showMessageDialog(panel,
                        LanguageManager.get("popup.copy.error"),
                        LanguageManager.get("popup.error"),
                        JOptionPane.ERROR_MESSAGE);
                    return;
                }

                // Percorso relativo al progetto
                Path projectRoot = Paths.get("").toAbsolutePath();
                Path rel = projectRoot.relativize(destFile.getAbsoluteFile().toPath());
                String relPath = rel.toString().replace("\\", "/");

                current.addSong(new Song(title, relPath));
                PlaylistDataManager.savePlaylists(panel.getPlaylists());
                panel.refreshComboBox();

                // plays.count per il brano
                File playsFile = new File("resources/plays.count");
                Map<String,Integer> plays = FileManager.loadCounts(playsFile);
                plays.putIfAbsent(title, 0);
                FileManager.saveCounts(plays, playsFile);
            }

            JOptionPane.showMessageDialog(panel,
                LanguageManager.get("popup.song.import.success"));
        }
        else if(source == panel.getChangeThemeItem())
        {
            panel.toggleTheme();
            ConfigManager.saveTheme(panel.isDarkMode());
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
            panel.getPlayer().setPanel(panel);
            panel.getPlayer().setSemaphore(playbackSemaphore);
            panel.getPlayer().seekToPercentage(slider.getValue());
            
            playbackSemaphore.release();
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
            	if (suppressPlaylistSelection) {
                    suppressPlaylistSelection = false;
                    return;
                }
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
            	if (suppressComboBoxPlayback) return;
                playSelectedSong();
            }
        }
    }
    
    /**
     * Ferma la riproduzione corrente svuotando i permit e chiamando stop() sul player.
     */
    private void stopPlayback() 
    {
        playbackSemaphore.drainPermits();
        panel.getPlayer().setPanel(panel);
        panel.getPlayer().setSemaphore(playbackSemaphore);
        panel.getPlayer().stop();
        //playbackManager.interruptAndRestart();
    }

    /**
     * Avvia o mette in pausa la riproduzione a seconda dello stato del player.
     */
    private void togglePlayPause() {
        MP3Player player = panel.getPlayer();
        panel.getPlayer().setPanel(panel);
        panel.getPlayer().setSemaphore(playbackSemaphore);

        if (!player.isPlaying()) {
            // Se non stiamo già suonando
            if (player.getPausedPosition() == 0) {
                // Caso “play da zero”
                if (!suppressComboBoxPlayback) {
                    // Se non è selezionata alcuna canzone, prendi la prima
                    if (panel.getComboBox().getSelectedIndex() == -1
                        && panel.getComboBox().getModel().getSize() > 0) {
                        panel.getComboBox().setSelectedIndex(0);
                    }
                    playSelectedSong();
                }
            } else {
                // Caso “riprendi da pausa”
                player.resume();
            }
        } else {
            // Caso “metto in pausa”
            player.pause();
        }

        panel.refreshIcons();
    }

    /**
     * Riproduce la canzone attualmente selezionata nella comboBox.
     */
    private void playSelectedSong() 
    {
    	if (playingSong) return;
        playingSong = true;
        
    	try
    	{
    		String selected = panel.getComboBox().getSelectedValue();
            
            if (selected == null) {
                Logger.writeLog("ATTENZIONE: nessuna canzone selezionata nella comboBox");
                return;
            }

            if (!Arrays.asList(panel.getPlaylist().getSongTitles()).contains(selected)) {
                Logger.writeLog("Errore: canzone selezionata non esiste nella playlist corrente");
                return;
            }
            
            if (!Arrays.asList(panel.getPlaylist().getSongTitles()).contains(selected)) {
        	    Logger.writeLog("Errore: canzone selezionata non esiste nella playlist corrente");
        	    return;
        	}

            if (selected != null) 
            {
                MP3Player player = panel.getPlayer();
                panel.getPlayer().setPanel(panel);
                panel.getPlayer().setSemaphore(playbackSemaphore);

                stopPlayback();
                
                /*
                if (currentlyPlayingTitle != null) {
                    playedSongs.push(currentlyPlayingTitle); // salva nello storico
                }
                */

                Logger.writeLog(selected);

                panel.getPlayer().setCurrentSong(panel.getPlaylist().getSong(selected));
   
                player.play(panel.getPlaylist().getSong(selected));

                panel.getComboBox().setSelectedValue(selected, true);
                //panel.getPlayPauseButton().setText("||");
                panel.refreshIcons();

                currentlyPlayingTitle = selected; // salva la canzone attuale
                playbackPlaylist = panel.getPlaylist(); // salvo la playlist
                
                playbackSemaphore.release();
            }
            
            panel.setCurrentSongLabel(currentlyPlayingTitle);
            
            File playsFile = new File("resources/plays.count");
            Map<String,Integer> riproduzioni = FileManager.loadCounts(playsFile);
            riproduzioni.put(selected, riproduzioni.getOrDefault(selected, 0) + 1);
            FileManager.saveCounts(riproduzioni, playsFile);

            String plName = playbackPlaylist.getName();
            File plPlaysFile = new File("resources/playlist_plays.count");
            Map<String,Integer> ascolti = FileManager.loadCounts(plPlaysFile);
            ascolti.put(plName, ascolti.getOrDefault(plName, 0) + 1);
            FileManager.saveCounts(ascolti, plPlaysFile);
    	}
    	finally
    	{
    		playingSong = false;
    	}
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
        if (playbackPlaylist == null || playbackPlaylist.getSongTitles().length == 0)
            return null;

        String[] titles = playbackPlaylist.getSongTitles();
        Map<String, Integer> riproduzioni = FileManager.loadCounts(new File("resources/plays.count"));

        List<String> pool = new LinkedList<>();
        int maxRiproduzioni = 1;

        // Trova il massimo numero di riproduzioni
        for (String title : titles) {
            int ascolti = riproduzioni.getOrDefault(title, 0);
            if (ascolti + 1 > maxRiproduzioni) {
                maxRiproduzioni = ascolti + 1;
            }
        }

        // Costruisce la lista ponderata
        for (String title : titles) {
            int ascolti = riproduzioni.getOrDefault(title, 0);
            int weight = maxRiproduzioni - ascolti;

            for (int i = 0; i < weight; i++) {
                pool.add(title);
            }
        }

        if (pool.isEmpty()) return titles[0]; // fallback sicuro

        return pool.get(random.nextInt(pool.size()));
    }

	/**
	 * Riproduce la canzone successiva secondo la modalità attuale o la coda personalizzata.
	 */    
    public void nextSong() {
        Logger.writeLog("Controller: nextSong() chiamato");

        if (changingSong) return;
        changingSong = true;

        try {
            if (playbackPlaylist == null || playbackPlaylist.getSongTitles().length == 0) {
                Logger.writeLog("Nessuna playlist di riproduzione attiva o vuota.");
                return;
            }

            String[] titles = playbackPlaylist.getSongTitles();
            String nextTitle = null;

            // 1. Controlla la coda
            if (!codaManager.isEmpty()) {
                nextTitle = codaManager.poll();
            } else {
                // 2. Trova l'indice della canzone corrente
                int currentIndex = -1;
                for (int i = 0; i < titles.length; i++) {
                    if (titles[i].equals(currentlyPlayingTitle)) {
                        currentIndex = i;
                        break;
                    }
                }

                switch (playbackMode) {
                    case CASUALE:
                        nextTitle = getBranoCasualePonderato();
                        break;
                    case SEQUENZIALE:
                        int nextIndex = (currentIndex < titles.length - 1 && currentIndex >= 0) ? currentIndex + 1 : 0;
                        nextTitle = titles[nextIndex];
                        break;
                    case RIPETI:
                        nextTitle = currentlyPlayingTitle;
                        break;
                }
            }

            if (nextTitle == null) return;

            stopPlayback();
            playbackSemaphore.drainPermits();

            currentlyPlayingTitle = nextTitle;
            Song song = playbackPlaylist.getSong(nextTitle);

            if (song == null) {
                Logger.writeLog(LanguageManager.get("popup.song.title.notfound") + nextTitle + "'");
                Logger.writeLog(LanguageManager.get("popup.song.available"));
                for (String titolo : playbackPlaylist.getSongTitles()) {
                    Logger.writeLog(" - '" + titolo + "'");
                }
                return;
            }

            Logger.writeLog(LanguageManager.get("label.play") + nextTitle);

            panel.getPlayer().setPanel(panel);
            panel.getPlayer().setSemaphore(playbackSemaphore);
            panel.getPlayer().play(song);
            
            File playsFile = new File("resources/plays.count");
            Map<String,Integer> riproduzioni = FileManager.loadCounts(playsFile);
            riproduzioni.put(song.getTitle(), riproduzioni.getOrDefault(song, 0) + 1);
            FileManager.saveCounts(riproduzioni, playsFile);

            String plName = playbackPlaylist.getName();
            File plPlaysFile = new File("resources/playlist_plays.count");
            Map<String,Integer> ascolti = FileManager.loadCounts(plPlaysFile);
            ascolti.put(plName, ascolti.getOrDefault(plName, 0) + 1);
            FileManager.saveCounts(ascolti, plPlaysFile);

            SwingUtilities.invokeLater(() -> {
                panel.refreshIcons();
                panel.setCurrentSongLabel(currentlyPlayingTitle);
                // seleziono la stringa del titolo, non l'oggetto Song
                panel.getComboBox().setSelectedValue(currentlyPlayingTitle, true);
                // faccio scrollare la lista in modo che la selezione sia ben visibile
                int idx = panel.getComboBox().getSelectedIndex();
                if(idx >= 0) {
                    panel.getComboBox().ensureIndexIsVisible(idx);
                }
            });

            playbackSemaphore.release();
        } finally {
            changingSong = false;
        }
    }
    
    /**
     * Torna alla canzone precedente nella lista, oppure all'ultima se si è all'inizio.
     */
    private void previousSong() {
        try {
            if (panel.getPlayer().getCurrentPercentage() <= 4) {
                if (playbackPlaylist == null || playbackPlaylist.getSongTitles().length == 0) {
                    Logger.writeLog("Nessuna playlist di riproduzione attiva o vuota.");
                    return;
                }

                String[] titles = playbackPlaylist.getSongTitles();

                int currentIndex = -1;
                for (int i = 0; i < titles.length; i++) {
                    if (titles[i].equals(currentlyPlayingTitle)) {
                        currentIndex = i;
                        break;
                    }
                }

                if (currentIndex == -1) currentIndex = 0;

                int prevIndex = (currentIndex > 0) ? currentIndex - 1 : titles.length - 1;

                currentlyPlayingTitle = titles[prevIndex];
                panel.setCurrentSongLabel(currentlyPlayingTitle);

                // Riproduci la canzone precedente
                stopPlayback();
                Song song = playbackPlaylist.getSong(currentlyPlayingTitle);
                panel.getPlayer().setPanel(panel);
                panel.getPlayer().setSemaphore(playbackSemaphore);
                panel.getPlayer().play(song);
                playbackSemaphore.release();
            } else {
                stopPlayback();
                panel.getPlayer().setPanel(panel);
                panel.getPlayer().setSemaphore(playbackSemaphore);
                panel.getPlayer().seekToPercentage(0);
                playbackSemaphore.release();
            }
        } catch (Exception e) {
            Logger.writeLog(e.getMessage());
        }
    }
    
    /**
     * Mostra una finestra con la coda attuale di canzoni in attesa.
     */
    private void mostraCoda() {
        if (codaManager.isEmpty()) {
            JOptionPane.showMessageDialog(panel, LanguageManager.get("queue.empty"), LanguageManager.get("queue.queue"), JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        StringBuilder sb = new StringBuilder(LanguageManager.get("queue.show") + ":\n");
        for (String song : codaManager.asList()) {
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
        	if (codaManager.isEmpty()) {
                JOptionPane.showMessageDialog(panel, LanguageManager.get("queue.empty"), LanguageManager.get("queue.manage"), JOptionPane.INFORMATION_MESSAGE);
                return;
            }

            JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(panel), LanguageManager.get("queue.manage"), true);
            dialog.setLayout(new BorderLayout());
            dialog.setSize(400, 300);
            dialog.setLocationRelativeTo(panel);

            DefaultListModel<String> codaModel = new DefaultListModel<>();
            for (String song : codaManager.asList()) {
                codaModel.addElement(song);
            }

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
                        Logger.writeLog(e.getMessage());
                    }
                    return false;
                }
            });

            JScrollPane scrollPane = new JScrollPane(codaList);
            dialog.add(scrollPane, BorderLayout.CENTER);

            JButton salvaButton = new JButton(LanguageManager.get("label.save"));
            salvaButton.addActionListener(e -> {
                // Aggiorna la vera coda
            	List<String> nuovaCoda = new ArrayList<>();
            	for (int i = 0; i < codaModel.size(); i++) {
            	    nuovaCoda.add(codaModel.getElementAt(i));
            	}
            	codaManager.replaceAll(nuovaCoda);

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
        // 1) prendo direttamente i titoli dal playlist corrente
        Playlist current = panel.getPlaylist();
        String[] allSongs = current.getSongTitles();
        Arrays.sort(allSongs, String.CASE_INSENSITIVE_ORDER);

        // 2) costruisco la dialog
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(panel),
                                     LanguageManager.get("queue.add"), 
                                     true);
        dialog.setLayout(new BorderLayout());
        dialog.setSize(400, 400);
        dialog.setLocationRelativeTo(panel);

        // 3) lista e filtro
        JTextField searchField = new JTextField();
        DefaultListModel<String> listModel = new DefaultListModel<>();
        JList<String> songList = new JList<>(listModel);
        songList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        JScrollPane scrollPane = new JScrollPane(songList);

        // riempio subito la lista con tutti i titoli
        for (String s : allSongs) {
            listModel.addElement(s);
        }

        // filtro dinamico
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

        // 4) bottone “Aggiungi”
        JButton addButton = new JButton(LanguageManager.get("queue.add"));
        addButton.addActionListener(ev -> {
            List<String> selectedSongs = songList.getSelectedValuesList();
            if (!selectedSongs.isEmpty()) {
                for (String song : selectedSongs) {
                    if (!codaManager.contains(song)) {
                        codaManager.add(song);
                    }
                }
                dialog.dispose();
                JOptionPane.showMessageDialog(panel,
                    LanguageManager.get("queue.added.multiple"),
                    LanguageManager.get("queue.add"),
                    JOptionPane.INFORMATION_MESSAGE);
            }
        });

        // 5) compongo la dialog e la mostro
        dialog.add(searchField, BorderLayout.NORTH);
        dialog.add(scrollPane, BorderLayout.CENTER);
        dialog.add(addButton, BorderLayout.SOUTH);

        dialog.setVisible(true);
    }
    
    /**
     * Mostra i 10 brani più ascoltati con un grafico
     */
    private void mostraTopBrani() {
    	Map<String, Integer> riproduzioni = FileManager.loadCounts(new File("resources/plays.count"));

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
    	Map<String, Integer> ascolti = FileManager.loadCounts(new File("resources/playlist_plays.count"));
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
        if (!codaManager.contains(titolo)) {
        	codaManager.add(titolo);
            Logger.writeLog(LanguageManager.get("queue.added") + titolo);
            JOptionPane.showMessageDialog(panel, LanguageManager.get("queue.added") + titolo);
        }
    }
    
    /**
     * Aggiunge una canzone in cima alla coda di riproduzione.
     *
     * @param titolo Il titolo della canzone da posizionare in testa alla coda.
     */
    public void aggiungiAllaCodaTop(String titolo) {
        if (!codaManager.contains(titolo)) {
            List<String> attuale = codaManager.asList();
            attuale.add(0, titolo); // inserisce in testa
            codaManager.replaceAll(attuale);
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
                Logger.writeLog(e.getMessage());
                JOptionPane.showMessageDialog(panel, LanguageManager.get("popup.error.export"), LanguageManager.get("popup.error"), JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /**
     * Importa una playlist da un file `.playlist.json` e copia i relativi MP3 nella struttura corretta.
     */
    private void importaPlaylist() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle(LanguageManager.get("popup.choose.json"));

        int result = chooser.showOpenDialog(panel);
        if (result == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();

            try {
                // 1) import della playlist
                Playlist imported = PlaylistDataManager.importPlaylist(file);
                String name = imported.getName();

                // 2) controllo titoli non validi
                for (String title : imported.getSongTitles()) {
                    if (title.contains(".")) {
                        JOptionPane.showMessageDialog(panel,
                            "Il brano \"" + title + "\" contiene un punto (.) nel titolo e non può essere importato.\n" +
                            "Rinomina il file o modifica il titolo prima di procedere.",
                            "Importazione annullata",
                            JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                }

                // 3) verifica duplicato
                if (panel.getPlaylists().containsKey(name)) {
                    JOptionPane.showMessageDialog(panel, LanguageManager.get("popup.import.duplicate"));
                    return;
                }

                // 4) aggiungo la playlist all'app
                panel.getPlaylists().put(name, imported);
                panel.getPlaylistListModel().addElement(name);
                PlaylistDataManager.savePlaylists(panel.getPlaylists());

                // ────────────── inizializzo i contatori ──────────────

                // 5) per ogni canzone, assicuro un contatore in plays.count
                File playsFile = new File("resources/plays.count");
                Map<String, Integer> plays = FileManager.loadCounts(playsFile);
                for (String title : imported.getSongTitles()) {
                    plays.putIfAbsent(title, 0);
                }
                FileManager.saveCounts(plays, playsFile);

                // 6) per la playlist appena importata, inizializzo in playlist_plays.count
                File plPlaysFile = new File("resources/playlist_plays.count");
                Map<String, Integer> plPlays = FileManager.loadCounts(plPlaysFile);
                plPlays.putIfAbsent(name, 0);
                FileManager.saveCounts(plPlays, plPlaysFile);

                // ───────────────────────────────────────────────────────

                JOptionPane.showMessageDialog(panel, LanguageManager.get("popup.import.success"));
            } catch (IOException e) {
                Logger.writeLog(e.getMessage());
                JOptionPane.showMessageDialog(panel,
                    LanguageManager.get("popup.error.import") + "\n" + e.getMessage(),
                    LanguageManager.get("popup.error"),
                    JOptionPane.ERROR_MESSAGE);
            }
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
     * Mostra una finestra con le descrizioni dei comandi disponibili.
     */
    private void mostraInformazioniComandi() {
        Languages lang = ConfigManager.loadLanguageFromConfig();
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
	                	panel.getPlayer().setPanel(panel);
	                	panel.getPlayer().setSemaphore(playbackSemaphore);
	                    panel.getPlayer().resume();
	                }
	                
	                //panel.getPlayPauseButton().setText("||");
	                playbackSemaphore.release();
	            } 
	            else 
	            {
	            	panel.getPlayer().setPanel(panel);
	            	panel.getPlayer().setSemaphore(playbackSemaphore);
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

	        // MENU CONTESTUALE CANZONI
	        if (e.getSource() == panel.getComboBox()) {
	            int index = panel.getComboBox().locationToIndex(e.getPoint());
	            if (index != -1) {
	                ListSelectionListener[] listeners = panel.getComboBox().getListSelectionListeners();
	                for (ListSelectionListener l : listeners) {
	                    panel.getComboBox().removeListSelectionListener(l);
	                }

	                panel.getComboBox().setSelectedIndex(index);
	                mostraMenuCanzoni(e.getX(), e.getY());

	                SwingUtilities.invokeLater(() -> {
	                    for (ListSelectionListener l : listeners) {
	                        panel.getComboBox().addListSelectionListener(l);
	                    }
	                });
	            }
	        }

	        // MENU CONTESTUALE PLAYLIST
	        else if (e.getSource() == panel.getPlaylistList()) {
	        	suppressPlaylistSelection = true;
	            int index = panel.getPlaylistList().locationToIndex(e.getPoint());
	            if (index != -1) {
	                panel.getPlaylistList().setSelectedIndex(index); // Solo per mostrare highlight nel menu
	                mostraMenuPlaylist(e.getX(), e.getY());
	            }
	        }
	    }
	}
	
	private void mostraMenuPlaylist(int x, int y) {
	    String selected = panel.getPlaylistList().getSelectedValue();
	    if (selected == null || selected.equals(LanguageManager.get("playlist.all"))) return;

	    JPopupMenu popup = new JPopupMenu();

	    JMenuItem aggiungiCanzoni = new JMenuItem(LanguageManager.get("playlist.add.songs"));
	    aggiungiCanzoni.addActionListener(e -> panel.getAddSongItem().doClick());
	    popup.add(aggiungiCanzoni);

	    JMenuItem rinomina = new JMenuItem(LanguageManager.get("item.renamePlaylist"));
	    rinomina.addActionListener(e -> panel.getRenamePlaylistItem().doClick());

	    JMenuItem cambiaCopertina = new JMenuItem(LanguageManager.get("item.coverPlaylist"));
	    cambiaCopertina.addActionListener(e -> panel.getCoverMenuItem().doClick());

	    JMenuItem esporta = new JMenuItem(LanguageManager.get("item.exportPlaylist"));
	    esporta.addActionListener(e -> panel.getExportPlaylistItem().doClick());

	    JMenuItem elimina = new JMenuItem(LanguageManager.get("item.deletePlaylist"));
	    elimina.addActionListener(e -> panel.getDeletePlaylistItem().doClick());

	    popup.add(rinomina);
	    popup.add(cambiaCopertina);
	    popup.add(esporta);
	    popup.addSeparator();
	    popup.add(elimina);

	    popup.show(panel.getPlaylistList(), x, y);
	}

	private void mostraMenuCanzoni(int x, int y) {
	    JPopupMenu popup = new JPopupMenu();

	    JMenuItem aggiungi = new JMenuItem(LanguageManager.get("queue.add"));
	    aggiungi.addActionListener(e -> panel.getAggiungiAllaCodaItem().doClick());

	    JMenuItem aggiungiTop = new JMenuItem(LanguageManager.get("item.addToQueueTop"));
	    aggiungiTop.addActionListener(e -> panel.getAggiungiAllaCodaTopItem().doClick());

	    JMenuItem rimuovi = new JMenuItem(LanguageManager.get("item.removeFromPlaylist"));
	    rimuovi.addActionListener(e -> panel.getRimuoviCanzoneItem().doClick());

	    JMenuItem esporta = new JMenuItem(LanguageManager.get("item.export"));
	    esporta.addActionListener(e -> panel.getExportSingleMP3Item().doClick());

	    popup.add(aggiungi);
	    popup.add(aggiungiTop);
	    popup.add(rimuovi);
	    popup.addSeparator();
	    popup.add(esporta);

	    popup.show(panel.getComboBox(), x, y);
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
	
	public Semaphore getPlaybackSemaphore() { return playbackSemaphore; }
	
	public void setSuppressComboBoxPlayback(boolean value) {
	    this.suppressComboBoxPlayback = value;
	}
	public boolean isSuppressComboBoxPlayback() {
	    return this.suppressComboBoxPlayback;
	}
}
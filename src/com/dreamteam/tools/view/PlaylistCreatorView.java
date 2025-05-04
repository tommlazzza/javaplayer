package com.dreamteam.tools.view;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.DefaultListModel;
import javax.swing.DropMode;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.TransferHandler;
import javax.swing.filechooser.FileNameExtensionFilter;

import com.dreamteam.control.Logger;
import com.dreamteam.data.LanguageManager;

/**
 * La classe {@code PlaylistCreatorView} rappresenta l'interfaccia grafica per la creazione di una nuova playlist MP3
 * all'interno dell'applicazione Sonora. Permette all'utente di:
 * <ul>
 *     <li>Inserire un nome per la playlist</li>
 *     <li>Selezionare un'immagine di copertina</li>
 *     <li>Aggiungere, rimuovere e riordinare i file MP3 tramite drag & drop</li>
 *     <li>Salvare la playlist</li>
 * </ul>
 * <p>
 * Il layout è costruito con {@link BorderLayout} e {@link GridLayout}, e integra
 * un {@link JList} personalizzabile con supporto al trascinamento interno.
 * </p>
 * 
 * @author DreamTeam
 */
@SuppressWarnings("serial")
public class PlaylistCreatorView extends JFrame {

    public JTextField nameField = new JTextField();
    public JTextField coverPathField = new JTextField();
    public DefaultListModel<File> songListModel = new DefaultListModel<>();
    public JList<File> songList = new JList<>(songListModel);
    public JButton chooseCoverButton = new JButton(LanguageManager.get("label.choose"));
    public JButton addSongButton = new JButton(LanguageManager.get("item.import"));
    public JButton removeSongButton = new JButton(LanguageManager.get("label.remove"));
    public JButton saveButton = new JButton(LanguageManager.get("playlist.save"));

    public PlaylistCreatorView() {
        setTitle("Sonora Constructor");
        setSize(500, 400);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel panel = new JPanel(new BorderLayout(10, 10));
        JPanel form = new JPanel(new GridLayout(3, 1, 10, 10));

        JPanel namePanel = new JPanel(new BorderLayout());
        namePanel.add(new JLabel(LanguageManager.get("playlist.name")), BorderLayout.WEST);
        namePanel.add(nameField, BorderLayout.CENTER);
        form.add(namePanel);

        JPanel coverPanel = new JPanel(new BorderLayout());
        coverPanel.add(new JLabel(LanguageManager.get("playlist.cover")), BorderLayout.WEST);
        coverPanel.add(coverPathField, BorderLayout.CENTER);
        coverPanel.add(chooseCoverButton, BorderLayout.EAST);
        form.add(coverPanel);

        JPanel songsPanel = new JPanel(new BorderLayout());
        songList.setDragEnabled(true);
        songList.setDropMode(DropMode.INSERT);
        songList.setTransferHandler(new TransferHandler() {
            private int fromIndex = -1;

            @Override
            protected Transferable createTransferable(JComponent c) {
                fromIndex = songList.getSelectedIndex();
                return new StringSelection(songList.getSelectedValue().getAbsolutePath());
            }

            @Override
            public int getSourceActions(JComponent c) {
                return MOVE;
            }

            @Override
            public boolean canImport(TransferSupport support) {
                return support.isDataFlavorSupported(DataFlavor.stringFlavor);
            }

            @Override
            public boolean importData(TransferSupport support) {
                try {
                    int toIndex = ((JList.DropLocation) support.getDropLocation()).getIndex();
                    String path = (String) support.getTransferable().getTransferData(DataFlavor.stringFlavor);
                    File dragged = new File(path);

                    if (fromIndex >= 0 && toIndex >= 0 && fromIndex != toIndex) {
                        songListModel.remove(fromIndex);
                        if (toIndex >= songListModel.getSize()) {
                            songListModel.addElement(dragged);
                        } else {
                            songListModel.add(toIndex, dragged);
                        }
                        return true;
                    }
                } catch (Exception e) {
                	Logger.writeLog(e.getMessage());
                }
                return false;
            }
        });

        songsPanel.add(new JScrollPane(songList), BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new GridLayout(1, 2));
        buttonPanel.add(addSongButton);
        buttonPanel.add(removeSongButton);
        songsPanel.add(buttonPanel, BorderLayout.SOUTH);

        panel.add(form, BorderLayout.NORTH);
        panel.add(songsPanel, BorderLayout.CENTER);
        panel.add(saveButton, BorderLayout.SOUTH);
        add(panel);
    }

    /**
     * Mostra un messaggio di dialogo all'utente.
     *
     * @param message Il messaggio da visualizzare.
     * @param title   Il titolo della finestra di dialogo.
     * @param type    Il tipo di messaggio (es. JOptionPane.INFORMATION_MESSAGE).
     */
    public void showMessage(String message, String title, int type) {
        JOptionPane.showMessageDialog(this, message, title, type);
    }

    /**
     * Apre un {@link JFileChooser} per selezionare un file o directory.
     *
     * @param title            Il titolo della finestra.
     * @param directoriesOnly  Se true, mostra solo directory.
     * @param multi            Se true, abilita la selezione multipla.
     * @param extensions       Estensioni dei file accettati (es. "mp3", "jpg").
     * @return Il file selezionato o null se annullato.
     */
    public File chooseFile(String title, boolean directoriesOnly, boolean multi, String... extensions) {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle(title);
        chooser.setMultiSelectionEnabled(multi);
        if (directoriesOnly) {
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        } else if (extensions.length > 0) {
            chooser.setFileFilter(new FileNameExtensionFilter("File", extensions));
        }

        int result = chooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            return chooser.getSelectedFile();
        }
        return null;
    }

    /**
     * Apre un {@link JFileChooser} per selezionare più file.
     *
     * @param title       Il titolo della finestra.
     * @param extensions  Estensioni dei file accettati.
     * @return Array di file selezionati, vuoto se annullato.
     */
    public File[] chooseMultipleFiles(String title, String... extensions) {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle(title);
        chooser.setMultiSelectionEnabled(true);
        chooser.setFileFilter(new FileNameExtensionFilter("File", extensions));

        int result = chooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            return chooser.getSelectedFiles();
        }
        return new File[0];
    }

    /**
     * Registra un listener per la selezione della copertina.
     *
     * @param l ActionListener da associare al pulsante "Scegli copertina".
     */
    public void addChooseCoverListener(ActionListener l) {
        chooseCoverButton.addActionListener(l);
    }

    /**
     * Registra un listener per l'aggiunta di nuovi brani.
     *
     * @param l ActionListener da associare al pulsante "Aggiungi brano".
     */
    public void addAddSongListener(ActionListener l) {
        addSongButton.addActionListener(l);
    }

    /**
     * Registra un listener per la rimozione di brani.
     *
     * @param l ActionListener da associare al pulsante "Rimuovi brano".
     */
    public void addRemoveSongListener(ActionListener l) {
        removeSongButton.addActionListener(l);
    }

    /**
     * Registra un listener per il salvataggio della playlist.
     *
     * @param l ActionListener da associare al pulsante "Salva playlist".
     */
    public void addSaveListener(ActionListener l) {
        saveButton.addActionListener(l);
    }
}
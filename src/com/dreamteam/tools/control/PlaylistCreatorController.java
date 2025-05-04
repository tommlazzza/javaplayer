package com.dreamteam.tools.control;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import javax.swing.DefaultListModel;
import javax.swing.JOptionPane;
import javax.swing.JComponent;
import javax.swing.TransferHandler;

import com.dreamteam.control.Logger;
import com.dreamteam.data.LanguageManager;
import com.dreamteam.model.Playlist;
import com.dreamteam.model.Song;
import com.dreamteam.tools.model.PlaylistCreatorTool;
import com.dreamteam.tools.view.PlaylistCreatorView;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * Controller dell'interfaccia "Playlist Creator".
 * <p>
 * Gestisce l'interazione tra {@link PlaylistCreatorTool} (modello) e {@link PlaylistCreatorView} (vista),
 * secondo il pattern MVC. Coordina l'aggiunta, la rimozione, il riordinamento e il salvataggio dei brani
 * in una playlist, inclusa la selezione della copertina.
 * </p>
 *
 * <p>
 * Le operazioni principali includono:
 * <ul>
 *     <li>Scelta della copertina</li>
 *     <li>Aggiunta e rimozione file MP3</li>
 *     <li>Drag & drop per riordinare i brani</li>
 *     <li>Salvataggio della playlist in una cartella dedicata</li>
 * </ul>
 * </p>
 *
 * <p>
 * Alla conferma del salvataggio, vengono copiati fisicamente i file nella cartella della playlist e
 * salvati in formato JSON.
 * </p>
 *
 * @author DreamTeam
 * @see PlaylistCreatorTool
 * @see PlaylistCreatorView
 */
public class PlaylistCreatorController {

    private final PlaylistCreatorTool model;
    private final PlaylistCreatorView view;

    /**
     * Costruisce il controller e collega i listener della vista al modello.
     *
     * @param model Il modello che gestisce i dati temporanei della playlist.
     * @param view  L'interfaccia grafica dell'utente.
     */
    public PlaylistCreatorController(PlaylistCreatorTool model, PlaylistCreatorView view) {
        this.model = model;
        this.view = view;

        // Scegli copertina
        view.addChooseCoverListener(e -> {
            File file = view.chooseFile(LanguageManager.get("playlist.cover.request"), false, false, "jpg", "png");
            if (file != null) {
                model.setCoverImagePath(file.getAbsolutePath());
                view.coverPathField.setText(file.getAbsolutePath());
            }
        });

        // Aggiungi MP3
        view.addAddSongListener(e -> {
            File[] files = view.chooseMultipleFiles(LanguageManager.get("item.import"), "mp3");
            model.addSongFiles(files);
            for (File file : files) {
                if (!view.songListModel.contains(file)) {
                    view.songListModel.addElement(file);
                }
            }
        });

        // Rimuovi MP3 selezionato
        view.addRemoveSongListener(e -> {
            File selected = view.songList.getSelectedValue();
            if (selected != null) {
                model.removeSongFile(selected);
                view.songListModel.removeElement(selected);
            }
        });

        // Drag & Drop
        view.songList.setDragEnabled(true);
        view.songList.setTransferHandler(new TransferHandler() {
            private int fromIndex = -1;

            @Override
            public int getSourceActions(JComponent c) {
                return MOVE;
            }

            @Override
            protected Transferable createTransferable(JComponent c) {
                fromIndex = view.songList.getSelectedIndex();
                return new Transferable() {
                    @Override public DataFlavor[] getTransferDataFlavors() {
                        return new DataFlavor[]{DataFlavor.stringFlavor};
                    }
                    @Override public boolean isDataFlavorSupported(DataFlavor flavor) {
                        return flavor.equals(DataFlavor.stringFlavor);
                    }
                    @Override public Object getTransferData(DataFlavor flavor) {
                        return view.songList.getSelectedValue().getAbsolutePath();
                    }
                };
            }

            @Override
            public boolean canImport(TransferSupport support) {
                return support.isDrop() && support.isDataFlavorSupported(DataFlavor.stringFlavor);
            }

            @Override
            public boolean importData(TransferSupport support) {
                if (!canImport(support)) return false;

                try {
                    int toIndex = view.songList.locationToIndex(support.getDropLocation().getDropPoint());
                    if (fromIndex == -1 || toIndex == -1 || fromIndex == toIndex) return false;

                    File moved = view.songListModel.getElementAt(fromIndex);
                    view.songListModel.removeElementAt(fromIndex);
                    view.songListModel.add(toIndex, moved);

                    // aggiorna l'ordine nel modello
                    model.getSongFiles().clear();
                    for (int i = 0; i < view.songListModel.getSize(); i++) {
                        model.getSongFiles().add(view.songListModel.getElementAt(i));
                    }
                    return true;
                } catch (Exception ex) {
                    return false;
                }
            }
        });

        // Salva
        view.addSaveListener(e -> {
            String name = view.nameField.getText().trim();
            if (name.isBlank() || view.songListModel.isEmpty()) {
                view.showMessage("Inserisci un nome e almeno un brano!", "Errore", JOptionPane.ERROR_MESSAGE);
                return;
            }

            model.setPlaylistName(name);
            Playlist playlist = model.toPlaylist();

            File saveDir = view.chooseFile("Scegli cartella di salvataggio", true, false);
            if (saveDir == null) return;

            File playlistDir = new File(saveDir, name);
            if (!playlistDir.exists()) playlistDir.mkdirs();

            for (Song song : playlist.getSongs()) {
                try {
                    File source = new File(song.getPath());
                    Path destPath = playlistDir.toPath().resolve(source.getName());
                    Files.copy(source.toPath(), destPath, StandardCopyOption.REPLACE_EXISTING);
                    song.setPath(destPath.toAbsolutePath().toUri().getPath());
                } catch (IOException ex) {
                	Logger.writeLog(ex.getMessage());
                    view.showMessage("Errore nel copiare il brano:\n" + song.getTitle() + "\n" + ex.getMessage(),
                            "Errore", JOptionPane.ERROR_MESSAGE);
                    return;
                }
            }

            if (playlist.getCoverImage() != null) {
                try {
                    File cover = new File(playlist.getCoverImage());
                    Path destCoverPath = playlistDir.toPath().resolve("cover.jpg").normalize();
                    Files.copy(cover.toPath(), destCoverPath, StandardCopyOption.REPLACE_EXISTING);
                    playlist.setCoverImage(destCoverPath.toAbsolutePath().toString());
                } catch (IOException ex) {
                	Logger.writeLog(ex.getMessage());
                    view.showMessage("Errore nella copia della copertina:\n" + ex.getMessage(),
                            "Errore", JOptionPane.ERROR_MESSAGE);
                    return;
                }
            }

            try {
                File jsonFile = new File(playlistDir, "data.json");
                Gson gson = new GsonBuilder().setPrettyPrinting().create();
                try (FileWriter writer = new FileWriter(jsonFile)) {
                    gson.toJson(playlist, writer);
                }
            } catch (IOException ex) {
            	Logger.writeLog(ex.getMessage());
                view.showMessage("Errore nel salvataggio JSON:\n" + ex.getMessage(),
                        "Errore", JOptionPane.ERROR_MESSAGE);
                return;
            }

            view.showMessage("Playlist salvata correttamente in:\n" + playlistDir.getAbsolutePath(),
                    "Successo", JOptionPane.INFORMATION_MESSAGE);
        });
    }
}

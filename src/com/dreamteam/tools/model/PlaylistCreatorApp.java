package com.dreamteam.tools.model;

import com.dreamteam.tools.view.PlaylistCreatorView;
import com.dreamteam.tools.control.PlaylistCreatorController;

import javax.swing.*;

/**
 * Punto di ingresso principale per lo strumento "Sonora Playlist Creator".
 * <p>
 * Questa classe avvia l'interfaccia grafica dell'editor playlist in modo sicuro nel thread EDT
 * (Event Dispatch Thread), istanziando il modello {@link PlaylistCreatorTool}, la vista
 * {@link PlaylistCreatorView} e il controller {@link PlaylistCreatorController}.
 * </p>
 *
 * <p>Viene rispettato il pattern MVC per garantire separazione tra interfaccia, logica
 * e gestione dei dati.</p>
 *
 * @author DreamTeam
 * @see PlaylistCreatorTool
 * @see PlaylistCreatorView
 * @see PlaylistCreatorController
 */
public class PlaylistCreatorApp {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            PlaylistCreatorTool model = new PlaylistCreatorTool();
            PlaylistCreatorView view = new PlaylistCreatorView();
            new PlaylistCreatorController(model, view);
            view.setVisible(true);
        });
    }
}
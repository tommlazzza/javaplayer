package com.dreamteam.control;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Image;
import java.io.File;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListCellRenderer;

import com.dreamteam.data.LanguageManager;
import com.dreamteam.model.Playlist;
import com.dreamteam.view.Panel;

public class PlaylistRenderer extends JPanel implements ListCellRenderer<String> {
	private JLabel imageLabel;
	private JLabel nameLabel;
	private JLabel countLabel;
	private JPanel textPanel;
	private Panel panel;

	/**
	 * Costruttore del renderer personalizzato per le playlist.
	 * Inizializza i componenti grafici (immagine, nome, numero di tracce) e li imposta in un layout orizzontale.
	 *
	 * @param panel Il pannello principale che contiene le informazioni sulle playlist e il tema attivo.
	 */
	public PlaylistRenderer(Panel panel) {
		this.panel = panel;

		imageLabel = new JLabel();
		nameLabel = new JLabel();
		countLabel = new JLabel();

		setLayout(new BorderLayout(5, 5));
		imageLabel.setPreferredSize(new Dimension(40, 40));
		imageLabel.setBorder(BorderFactory.createLineBorder(new Color(180, 180, 180)));

		textPanel = new JPanel();
		textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
		textPanel.add(nameLabel);
		textPanel.add(countLabel);

		add(imageLabel, BorderLayout.WEST);
		add(textPanel, BorderLayout.CENTER);
	}

	/**
	 * Metodo invocato per ogni elemento della lista delle playlist.
	 * Imposta nome, numero di tracce, immagine di copertina e colori in base al tema attuale.
	 *
	 * @param list La JList su cui viene eseguito il rendering.
	 * @param value Il nome della playlist corrente.
	 * @param index L'indice dell'elemento nella lista.
	 * @param isSelected true se l'elemento è selezionato.
	 * @param cellHasFocus true se l'elemento ha il focus.
	 * @return Il componente da usare per visualizzare l'elemento.
	 */
	@Override
	public Component getListCellRendererComponent(JList<? extends String> list, String value, int index, boolean isSelected, boolean cellHasFocus) {
	    Playlist pl = panel.getPlaylists().get(value);
	    nameLabel.setText(value);
	    countLabel.setText((pl != null ? pl.getSongTitles().length : 0) + " " + LanguageManager.get("label.tracks"));

	    // Copertina
	    if (pl != null && pl.getCoverImage() != null) {
	        File imgFile = new File(pl.getCoverImage());
	        if (imgFile.exists()) {
	            ImageIcon icon = new ImageIcon(imgFile.getAbsolutePath());
	            Image image = icon.getImage().getScaledInstance(40, 40, Image.SCALE_SMOOTH);
	            imageLabel.setIcon(new ImageIcon(image));
	        } else {
	            imageLabel.setIcon(null);
	        }
	    } else {
	        imageLabel.setIcon(null);
	    }

	    Color bg = panel.isDarkMode() ? new Color(44, 44, 46) : new Color(235, 235, 235);
	    Color fg = panel.isDarkMode() ? new Color(240, 240, 240) : new Color(30, 30, 30);

	    textPanel.setBackground(bg);
	    textPanel.setForeground(fg);
	    nameLabel.setBackground(bg);
	    nameLabel.setForeground(fg);
	    countLabel.setBackground(bg);
	    countLabel.setForeground(fg);
	    imageLabel.setBackground(bg);
	    imageLabel.setForeground(bg);

	    return this;
	}
}
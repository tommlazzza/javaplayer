package com.dreamteam.model;

import java.text.DecimalFormat;

public class Track
{
	private final DecimalFormat df = new DecimalFormat("0.00");
	
	private String title;
	private String author;
	private String album;
	private String genre;
	private int length;
	private boolean isPlaying;
	private String trackPath;
	
	/**
	 * Costruttore di default. Inizializza tutti i campi della traccia con valori vuoti o predefiniti.
	 */
	public Track()
	{
		this.title = "";
		this.author = "";
		this.album = "";
		this.genre = "";
		this.length = 0;
		this.isPlaying = false;
		this.trackPath = "";
	}
	
	/**
	 * Costruttore completo. Inizializza tutti i campi della traccia.
	 *
	 * @param title Titolo della traccia.
	 * @param author Autore della traccia.
	 * @param album Album di appartenenza.
	 * @param genre Genere musicale.
	 * @param length Lunghezza della traccia in secondi.
	 * @param trackName Nome del file (senza estensione), salvato nella cartella mp3.
	 */
	public Track(String title, String author, String album, String genre, int length, String trackName)
	{
		this.title = title;
		this.author = author;
		this.album = album;
		this.genre = genre;
		this.length = length;
		this.isPlaying = false;
		this.trackPath = "mp3/" + trackName + ".mp3";
	}
	
	/**
	 * Imposta il titolo della traccia.
	 * @param title Titolo da impostare.
	 */
	public void setTitle(String title)
	{
		this.title = title;
	}
	
	/**
	 * Imposta l'autore della traccia.
	 * @param author Autore da impostare.
	 */
	public void setAuthor(String author)
	{
		this.author = author;
	}
	
	/**
	 * Imposta l'album della traccia.
	 * @param album Album da impostare.
	 */
	public void setAlbum(String album)
	{
		this.album = album;
	}
	
	/**
	 * Imposta il genere musicale della traccia.
	 * @param genre Genere da impostare.
	 */
	public void setGenre(String genre)
	{
		this.genre = genre;
	}
	
	/**
	 * Imposta la lunghezza della traccia in secondi.
	 * @param length Lunghezza in secondi.
	 */
	public void setLength(int length)
	{
		this.length = length;
	}
	
	/**
	 * Imposta lo stato di riproduzione della traccia.
	 * @param isPlaying true se la traccia è in riproduzione, false altrimenti.
	 */
	public void setPlaying(boolean isPlaying)
	{
		this.isPlaying = isPlaying;
	}
	

	/**
	 * Imposta il percorso del file della traccia.
	 * @param trackPath Percorso del file mp3.
	 */
	public void setTrackPath(String trackPath)
	{
		this.trackPath = trackPath;
	}
	
	/** @return Il titolo della traccia. */
	public String getTitle()
	{
		return this.title;
	}
	
	/** @return L'autore della traccia. */
	public String getAuthor()
	{
		return this.author;
	}
	
	/** @return L'album della traccia. */
	public String getAlbum()
	{
		return this.album;
	}
	
	/** @return Il genere della traccia. */
	public String getGenre()
	{
		return this.genre;
	}
	
	/** @return La durata della traccia in secondi. */
	public int getLength()
	{
		return this.length;
	}
	
	/** @return true se la traccia è in riproduzione, false altrimenti. */
	public boolean getPlaying()
	{
		return this.isPlaying;
	}
	
	/** @return Il percorso del file mp3 della traccia. */
	public String getTrackPath()
	{
		return this.trackPath;
	}
	
	/**
	 * Converte la durata in secondi in minuti decimali (es. 2.30).
	 *
	 * @return La durata della traccia in minuti decimali.
	 */
	private double secToMin()
	{
		double value = 0.00;
		int temp = this.length;
		
		while(temp > 0)
		{
			if(temp >= 60)
			{
				temp -= 60;
				value++;
			}
			else
			{
				value += ((double) temp / 100);
				temp -= temp;
			}
		}
		
		return value;
	}
	
	/**
	 * Restituisce una rappresentazione testuale della traccia con tutte le sue informazioni.
	 *
	 * @return Stringa formattata contenente titolo, autore, album, genere e durata.
	 */
	@Override
	public String toString()
	{
		return 
				"Titolo: " + this.title + "\n" +
				"Autore: " + this.author + "\n" +
				"Album: " + this.album + "\n" +
				"Genere: " + this.genre + "\n" +
				"Durata: " + df.format(secToMin()) + "\n"
				;
	}
}

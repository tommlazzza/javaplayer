package com.dreamteam.model;

import java.io.Serializable;

public class Song implements Serializable
{
	private static final long serialVersionUID = 1L;
	
	private String title;
	private String path;
	private String author;
	
	/**
	 * Costruttore della classe Song.
	 * Inizializza il titolo e il percorso del file a partire dal nome del file.
	 *
	 * @param title Titolo della canzone.
	 * @param filename Nome del file (es. "titolo.mp3").
	 */
	public Song(String title, String filename)
	{
		this.title = title;
		this.path = filename;
	}
	
	public Song() {}
	
	/**
	 * Restituisce il titolo della canzone.
	 *
	 * @return Il titolo della canzone.
	 */
	public String getTitle()
	{
		return this.title;
	}
	
	public String getAuthor() {
	    return author;
	}

	public void setAuthor(String author) {
	    this.author = author;
	}
	
	/**
	 * Restituisce il percorso del file, con formattazione corretta per l'accesso a risorse.
	 * Sostituisce "." con "/" e rimuove il prefisso "mp3" dal path.
	 *
	 * @return Il percorso del file formattato.
	 */
	public String getPath()
	{
		return this.path.replace('.', '/').replace("/mp3", ".mp3");
	}
	
	public void setPath(String path)
	{
		this.path = path;	
	}
	
	/**
	 * Restituisce una rappresentazione testuale della canzone,
	 * contenente titolo e percorso.
	 *
	 * @return Stringa formattata con titolo e path.
	 */
	@Override
	public String toString()
	{
		return
				"Title: " + title + " Path: " + path + "\n";
	}
}

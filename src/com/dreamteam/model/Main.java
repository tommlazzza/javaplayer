package com.dreamteam.model;

import javax.swing.SwingUtilities;

import com.dreamteam.control.Logger;
import com.dreamteam.data.FontManager;
import com.dreamteam.view.Window;

public class Main 
{
	public static void main(String[] args) 
	{
		Logger.initLog();
		
		SwingUtilities.invokeLater(() -> {
            FontManager.setGlobalFont("resources/fonts/Roboto-Regular.ttf", 14f);
            Window window = new Window();
        });
	}
}
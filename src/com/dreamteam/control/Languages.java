package com.dreamteam.control;

public enum Languages 
{
    ITALIANO("Italiano"),
    ENGLISH("English"),
	FRANCESE("Français"),
	TEDESCO("Deutsch"),
	CINESE("繁體中文"),
	GIAPPONESE("日本語"),
	COREANO("한국인");

    private final String displayName;

    Languages(String displayName) 
    {
        this.displayName = displayName;
    }

    public String getDisplayName() 
    {
        return displayName;
    }

    @Override
    public String toString() 
    {
        return displayName;
    }
}

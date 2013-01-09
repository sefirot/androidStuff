package com.applang.berichtsheft.ui.components;

import java.awt.event.KeyListener;

public interface TextComponent
{
	public void addKeyListener(KeyListener l);
	public void setText(String t);
	public String getText();
	public boolean spellcheck();
}

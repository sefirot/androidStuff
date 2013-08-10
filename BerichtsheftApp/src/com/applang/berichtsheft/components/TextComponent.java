package com.applang.berichtsheft.components;

import java.awt.event.KeyListener;

public interface TextComponent
{
	public void addKeyListener(KeyListener l);
	public void setText(String t);
	public String getText();
	public boolean isDirty();
	public void setDirty(boolean dirty);
	public void spellcheck();
	public void setSelection(int start, int end);
	public void setSelectedText(String text);
	public String getSelectedText();
}

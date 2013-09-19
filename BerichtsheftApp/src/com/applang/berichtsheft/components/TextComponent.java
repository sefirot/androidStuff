package com.applang.berichtsheft.components;

import java.awt.event.KeyListener;

public interface TextComponent extends DataComponent
{
	public void addKeyListener(KeyListener l);
	public void setText(String t);
	public String getText();
	public void setSelection(int start, int end);
	public void setSelectedText(String text);
	public String getSelectedText();
}

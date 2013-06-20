package com.applang.berichtsheft.ui.components;

import java.awt.event.KeyListener;

import javax.swing.JTextArea;

import com.applang.SwingUtil;

public class TextArea implements TextComponent
{
	public TextArea() {
		super();
		textArea.setLineWrap(true);
		textArea.setWrapStyleWord(true);
		textArea.setTabSize(4);
		SwingUtil.addTabKeyForwarding(textArea);
	}

	public JTextArea textArea = new JTextArea();
	
	@Override
	public void addKeyListener(KeyListener l) {
		textArea.addKeyListener(l);
	}
	
	@Override
	public void setText(String t) {
		textArea.setText(t);
	}

	@Override
	public String getText() {
		return textArea.getText();
	}
    
	boolean dirty = false;
	
	public boolean isDirty() {
		return dirty;
	}

	public void setDirty(boolean dirty) {
		this.dirty = dirty;
	}

	@Override
	public void spellcheck() {
	}
}
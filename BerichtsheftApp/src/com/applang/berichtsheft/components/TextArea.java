package com.applang.berichtsheft.components;

import java.awt.event.KeyListener;

import javax.swing.JTextArea;

import static com.applang.Util.*;
import static com.applang.SwingUtil.*;

public class TextArea implements TextComponent
{
	public TextArea() {
		super();
		textArea.setLineWrap(true);
		textArea.setWrapStyleWord(true);
		textArea.setTabSize(4);
		addTabKeyForwarding(textArea);
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

	@Override
	public void setSelection(int start, int end) {
		textArea.setSelectionStart(start);
		textArea.setSelectionEnd(end);
		textArea.requestFocus();
	}

	@Override
	public void setSelectedText(String text) {
		int start = textArea.getSelectionStart();
		textArea.replaceRange(text, start, textArea.getSelectionEnd());
		setSelection(start, start + (notNullOrEmpty(text) ? 0 : text.length()));
	}

	@Override
	public String getSelectedText() {
		return textArea.getSelectedText();
	}
}

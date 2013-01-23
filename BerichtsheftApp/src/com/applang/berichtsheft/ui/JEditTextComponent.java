package com.applang.berichtsheft.ui;

import java.awt.event.KeyListener;

import com.applang.berichtsheft.ui.components.TextComponent;

import cswilly.jeditPlugins.spell.SpellCheckPlugin;

import org.gjt.sp.jedit.View;

public class JEditTextComponent implements TextComponent
{
	public JEditTextComponent(View view) {
		this.view = view;
	}

	public View view;
	
	public boolean hasTextArea() {
		return view != null && view.getTextArea() != null;
	}
	
	@Override
	public void addKeyListener(KeyListener l) {
	}
	
	@Override
	public void setText(String t) {
		if (hasTextArea())
			view.getTextArea().setText(t);
	}

	@Override
	public String getText() {
		if (hasTextArea())
			return view.getTextArea().getText();
		else
			return null;
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
		if (hasTextArea()) {
			SpellCheckPlugin.setBufferLanguage(view, view.getBuffer());
			SpellCheckPlugin.checkBuffer(view, view.getBuffer());
		}
	}
}

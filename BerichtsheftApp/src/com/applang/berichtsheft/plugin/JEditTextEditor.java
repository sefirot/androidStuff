package com.applang.berichtsheft.plugin;

import static com.applang.SwingUtil.*;
import static com.applang.Util.*;

import java.awt.event.KeyListener;

import android.app.Activity;

import com.applang.berichtsheft.components.TextComponent;

import cswilly.jeditPlugins.spell.SpellCheckPlugin;

import org.gjt.sp.jedit.Buffer;
import org.gjt.sp.jedit.View;
import org.gjt.sp.jedit.textarea.JEditTextArea;
import org.gjt.sp.jedit.textarea.Selection;

public class JEditTextEditor implements TextComponent
{
	public JEditTextEditor(View vw) {
		setView(vw);
		if (getView() != null) {
			messRedirection = new Function<String>() {
				public String apply(Object... params) {
					String message = param("", 0, params);
					getView().getStatus().setMessageAndClear(message);
					return message;
				}
			};
		}
	}

	private void setView(View vw) {
		Activity.frame = vw;
	}

	private View getView() {
		return Activity.frame instanceof View ? (View) Activity.frame : null;
	}
	
	public boolean hasTextArea() {
		View view = getView();
		return view != null && view.getTextArea() != null;
	}
	
	public JEditTextArea getTextArea() {
		return hasTextArea() ? getView().getTextArea() : null;
	}
	
	@Override
	public void addKeyListener(KeyListener l) {
	}
	
	@Override
	public void setText(String t) {
		if (hasTextArea())
			getView().getTextArea().setText(t);
	}

	@Override
	public String getText() {
		if (hasTextArea())
			return getView().getTextArea().getText();
		else
			return null;
	}

	@Override
	public void spellcheck() {
		if (hasTextArea()) {
			View view = getView();
			Buffer buffer = view.getBuffer();
			SpellCheckPlugin.setBufferLanguage(view, buffer);
			SpellCheckPlugin.checkBuffer(view, buffer);
		}
	}

	@Override
	public void setSelection(int start, int end) {
		if (hasTextArea()) {
			getView().getTextArea().setSelection(new Selection.Range(start, end));
		}
	}

	@Override
	public void setSelectedText(String text) {
		if (hasTextArea()) {
			JEditTextArea textArea = getView().getTextArea();
			int start = textArea.getCaretPosition();
			textArea.setSelectedText(text);
			if (notNullOrEmpty(text))
				setSelection(start, start + text.length());
		}
	}

	@Override
	public String getSelectedText() {
		if (hasTextArea()) {
			return getView().getTextArea().getSelectedText();
		}
		return null;
	}
}

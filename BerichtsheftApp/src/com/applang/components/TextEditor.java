package com.applang.components;

import java.awt.event.ActionEvent;
import java.awt.event.MouseListener;
import java.lang.reflect.Field;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JPopupMenu;
import javax.swing.JTextArea;
import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoManager;

import android.util.Log;

import com.applang.berichtsheft.plugin.BerichtsheftPlugin;
import com.inet.jortho.PopupListener;
import com.inet.jortho.SpellChecker;

import static com.applang.SwingUtil.*;

public class TextEditor extends JTextArea
{
    private static final String TAG = TextEditor.class.getSimpleName();

    public TextEditor() {
		setLineWrap(true);
		setWrapStyleWord(true);
		setTabSize(4);
    }
	
    public TextEditor(int rows, int columns) {
    	super(rows, columns);
		setTabSize(4);
    }

	class UndoAction extends AbstractAction {
		public UndoAction() {
			super("Undo");
			setEnabled(false);
		}
		
		public void actionPerformed(ActionEvent ev) {
			try {
				undo.undo();
			} catch (CannotUndoException e) {
				BerichtsheftPlugin.consoleMessage("texteditor.no-undo.message", e.getMessage());
			}
			updateUndoState();
			redoAction.updateRedoState();
		}
		
		protected void updateUndoState() {
			if (undo.canUndo()) {
				putValue(Action.NAME, undo.getUndoPresentationName());
			} else {
				putValue(Action.NAME, "Undo");
			}
			setEnabled(undo.canUndo());
		}
	}
	
	class RedoAction extends AbstractAction {
		public RedoAction() {
			super("Redo");
			setEnabled(false);
		}
		
		public void actionPerformed(ActionEvent ev) {
			try {
				undo.redo();
			} catch (CannotRedoException e) {
				BerichtsheftPlugin.consoleMessage("texteditor.no-redo.message", e.getMessage());
			}
			updateRedoState();
			undoAction.updateUndoState();
		}
		
		protected void updateRedoState() {
			if (undo.canRedo()) {
				putValue(Action.NAME, undo.getRedoPresentationName());
			} else {
				putValue(Action.NAME, "Redo");
			}
			setEnabled(undo.canRedo());
		}
	}
	
	protected UndoAction undoAction = new UndoAction();
	protected RedoAction redoAction = new RedoAction();
	public UndoManager undo = new UndoManager();
	
	public void installUndoRedo() {
		getDocument().addUndoableEditListener(new UndoableEditListener() {
			public void undoableEditHappened(UndoableEditEvent e) {
				undo.addEdit(e.getEdit());
				undoAction.updateUndoState();
				redoAction.updateRedoState();
			}
		});
		boolean menuInstalled = false;
		for (MouseListener listener : getMouseListeners()) {
			if (listener instanceof PopupListener) {
				try {
					Field f = listener.getClass().getDeclaredField("menu");
					f.setAccessible(true);
					JPopupMenu menu = (JPopupMenu) f.get(listener);
					menu.insert(undoAction, 0);
					menu.insert(redoAction, 1);
					menu.insert(new JPopupMenu.Separator(), 2);
					menuInstalled = true;
				} catch (Exception e) {
					Log.e(TAG, "installUndoRedo", e);
				}
			}
		}
		if (!menuInstalled) {
			JPopupMenu menu = (JPopupMenu) new JPopupMenu();
			menu.insert(undoAction, 0);
			menu.insert(redoAction, 1);
			addMouseListener(new PopupAdapter(menu));
		}
	}
	
	public void installSpellChecker() {
		SpellChecker.register(this);
		installUndoRedo();
	}
	
	public void uninstallSpellChecker() {
		SpellChecker.unregister(this);
	}
}
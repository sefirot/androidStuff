package com.applang.components;

import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseListener;
import java.lang.reflect.Field;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JDialog;
import javax.swing.JPopupMenu;
import javax.swing.JTextArea;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;
import javax.swing.text.JTextComponent;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoManager;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.EditText;

import com.inet.jortho.PopupListener;

import com.applang.UserContext;
import com.applang.berichtsheft.BerichtsheftActivity;

import static com.applang.Util.*;
import static com.applang.SwingUtil.*;
import static com.applang.PluginUtils.*;

public class TextEdit extends EditText
{
    private static final String TAG = TextEdit.class.getSimpleName();

    public TextEdit() {
    	super(new JTextArea());
    	setHorizontallyScrolling((JTextArea) getComponent(), false);
		setContext(BerichtsheftActivity.getInstance());
    }
	
    public TextEdit(int rows, int columns) {
    	super(new JTextArea(rows, columns));
    	setHorizontallyScrolling((JTextArea) getComponent(), true);
		setContext(BerichtsheftActivity.getInstance());
    }

	public TextEdit(Context context) {
		super(context);
	}

	public TextEdit(Context context, AttributeSet attrs) {
		super(context, attrs);
		if (hasFeature("spellchecking"))
			installSpellChecker(getTextComponent());
		if (hasFeature("togglable")) {
			setTextToggle(new TextToggle(this));
			textToggle.createBufferedTextArea(
					attributeSet.getAttributeValue("mode"), 
					attributeSet.getAttributeValue("modeFile"));
			installToggle(getTextComponent());
		}
    }

    @Override
    protected void create() {
    	super.create();
    	if (getComponent() instanceof JTextArea) {
        	setHorizontallyScrolling((JTextArea) getComponent(), false);
		}
	}

	private void setHorizontallyScrolling(JTextArea textArea, boolean whether) {
    	textArea.setLineWrap(!whether);
    	textArea.setWrapStyleWord(!whether);
    	textArea.setTabSize(4);
	}

	private TextToggle textToggle = null;
	
	public void setTextToggle(TextToggle textToggle) {
		this.textToggle = textToggle;
	}
	
	public TextToggle getTextToggle() {
		return textToggle;
	}
	
	public void showScriptArea(boolean hide) {
		if (textToggle != null) {
			if (textToggle.scriptDialog() == null)
				textToggle.showScriptArea(getParent().getComponent(), getComponent(), 
						"Script", 
						Behavior.HIDDEN);
			textToggle.scriptDialog().setVisible(!hide);
		}
	}
	
	public String getScript() {
		if (textToggle != null) 
			return textToggle.getTextArea().getText();
		else
			return null;
	}
	
	public void setScript(String script) {
		if (textToggle != null)
			textToggle.getTextArea().setText(script);
	}
	
    @Override
	public void setText(String text) {
		super.setText(textToggle != null ? 
				UserContext.toPlainText(text) : 
				text);
	}
    
    @Override
	protected void finalize() throws Throwable {
		if ("spellchecking".equals(feature))
			uninstallSpellChecker();
		super.finalize();
	}

	class UndoAction extends AbstractAction
	{
		public UndoAction() {
			super("Undo");
			setEnabled(false);
		}
		
		public void actionPerformed(ActionEvent ev) {
			try {
				undoManager.undo();
			} 
			catch (CannotUndoException e) {
				message("textedit.no-undo.message", e.getMessage());
			}
			updateUndoState();
			redoAction.updateRedoState();
		}
		
		protected void updateUndoState() {
			if (undoManager.canUndo()) {
				putValue(Action.NAME, undoManager.getUndoPresentationName());
			} else {
				putValue(Action.NAME, "Undo");
			}
			setEnabled(undoManager.canUndo());
		}
	}
	
	class RedoAction extends AbstractAction
	{
		public RedoAction() {
			super("Redo");
			setEnabled(false);
		}
		
		public void actionPerformed(ActionEvent ev) {
			try {
				undoManager.redo();
			} 
			catch (CannotRedoException e) {
				message("textedit.no-redo.message", e.getMessage());
			}
			updateRedoState();
			undoAction.updateUndoState();
		}
		
		protected void updateRedoState() {
			if (undoManager.canRedo()) {
				putValue(Action.NAME, undoManager.getRedoPresentationName());
			} else {
				putValue(Action.NAME, "Redo");
			}
			setEnabled(undoManager.canRedo());
		}
	}
	
	protected UndoAction undoAction = new UndoAction();
	protected RedoAction redoAction = new RedoAction();
	
	public UndoManager undoManager = new UndoManager();
	
	private void installUndoRedo(JTextComponent jtc) {
		jtc.getDocument().addUndoableEditListener(new UndoableEditListener() {
			public void undoableEditHappened(UndoableEditEvent e) {
				undoManager.addEdit(e.getEdit());
				undoAction.updateUndoState();
				redoAction.updateRedoState();
			}
		});
		installContextMenu(jtc, undoAction, redoAction);
	}
	
	private JPopupMenu popupMenu(JTextComponent jtc) {
		for (MouseListener listener : jtc.getMouseListeners()) {
			if (listener instanceof PopupListener) {
				try {
					Field f = listener.getClass().getDeclaredField("menu");
					f.setAccessible(true);
					return (JPopupMenu) f.get(listener);
				} catch (Exception e) {
					Log.e(TAG, "popupMenu", e);
				}
			}
		}
		return null;
	}

	private void insertMenuItems(JPopupMenu menu, Action...actions) {
		int index = 0;
		for (Action action : actions)
			menu.insert(action, index++);
	}

	private void installContextMenu(JTextComponent jtc, Action...actions) {
		JPopupMenu menu = popupMenu(jtc);
		if (menu != null) {
			insertMenuItems(menu, actions);
			menu.insert(new JPopupMenu.Separator(), actions.length);
		}
		else {
			menu = (JPopupMenu) new JPopupMenu();
			insertMenuItems(menu, actions);
			jtc.addMouseListener(new PopupAdapter(menu));
		}
	}
	
	public void installUndoRedo() {
		installUndoRedo(getTextComponent());
	}
	
	private void installSpellChecker(JTextComponent jtc) {
		spellChecking(jtc, true);
		installUndoRedo(jtc);
	}
	
	public void installSpellChecker() {
		installSpellChecker(getTextComponent());
	}
	
	public void uninstallSpellChecker() {
		spellChecking(getTextComponent(), false);
	}
	
	private void installToggle(JTextComponent jtc) {
		JPopupMenu menu = popupMenu(jtc);
		if (menu != null) {
			menu.insert(new JPopupMenu.Separator(), 0);
		}
		else {
			menu = (JPopupMenu) new JPopupMenu();
			jtc.addMouseListener(new PopupAdapter(menu));
		}
		final JCheckBoxMenuItem menuItem = new JCheckBoxMenuItem(toggleType.name(2));
		menuItem.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent ev) {
				boolean hide = ev.getStateChange() == ItemEvent.DESELECTED;
				showScriptArea(hide);
			}
		});
		menu.insert(menuItem, 0);
		menu.addPopupMenuListener(new PopupMenuAdapter() {
			@Override
			public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
				JDialog dialog = getTextToggle().scriptDialog();
				boolean visible = dialog != null && dialog.isVisible();
				menuItem.setSelected(visible);
			}
		});
	}
	
	private CustomActionType toggleType = 
		new CustomActionType() {
			public int index() {
				return 0;
			}
			public String resourceName() {
				return "textedit.action-toggle";
			}
			public String iconName() {
				SharedPreferences prefs = getContext().getSharedPreferences();
				return prefs.getString(resourceName() + ".icon", null);
			}
			public String name(int state) {
				SharedPreferences prefs = getContext().getSharedPreferences();
				return prefs.getString(resourceName().concat(".label") + "." + state, "");
			}
			public String description() {
				SharedPreferences prefs = getContext().getSharedPreferences();
				return prefs.getString(resourceName().concat(".label"), "");
			}
		};
		
	public Job<Boolean> toggler = new Job<Boolean>() {
		public void perform(Boolean isText, Object[] parms) throws Exception {
	    	String script = getScript();
	    	textToggle.toggle(isText, null);
	    	if (isText) {
	    		setText(script);
	    	}
		}
	};
		
/*	public CustomAction toggleAction = 
		new CustomAction(toggleType) {
			{
				putValue(NAME, toggleType.name(2));
			}
			@Override
			protected void action_Performed(ActionEvent ae) {
				switch (getType().index()) {
				case 0:
					toggle(this, toggler);
					break;
				}
			}
		};
*/
}

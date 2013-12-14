package com.applang.components;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.lang.reflect.Field;
import java.util.Properties;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;
import javax.swing.text.Segment;
import javax.swing.text.Highlighter.Highlight;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoManager;

import org.gjt.sp.jedit.IPropertyManager;
import org.gjt.sp.jedit.JEditBeanShellAction;
import org.gjt.sp.jedit.Mode;
import org.gjt.sp.jedit.View;
import org.gjt.sp.jedit.buffer.BufferAdapter;
import org.gjt.sp.jedit.buffer.JEditBuffer;
import org.gjt.sp.jedit.syntax.ParserRuleSet;
import org.gjt.sp.jedit.syntax.Token;
import org.gjt.sp.jedit.syntax.TokenHandler;
import org.gjt.sp.jedit.syntax.TokenMarker;
import org.gjt.sp.jedit.syntax.TokenMarker.LineContext;
import org.gjt.sp.jedit.textarea.JEditEmbeddedTextArea;
import org.gjt.sp.jedit.textarea.Selection;
import org.gjt.sp.jedit.textarea.StandaloneTextArea;
import org.gjt.sp.jedit.textarea.TextArea;
import org.gjt.sp.util.Log;

import android.app.Activity;

import com.applang.SwingUtil.PopupAdapter;
import com.applang.Util.Function;
import com.applang.berichtsheft.plugin.BerichtsheftPlugin;
import com.inet.jortho.PopupListener;
import com.inet.jortho.SpellChecker;

import static com.applang.Util.*;
import static com.applang.Util2.*;
import static com.applang.SwingUtil.*;

class Text_Editor extends JTextArea
{
    public Text_Editor() {
		setLineWrap(true);
		setWrapStyleWord(true);
		setTabSize(4);
    }
	
    public Text_Editor(int rows, int columns) {
    	super(rows, columns);
		setTabSize(4);
    }

	class UndoAction extends AbstractAction {
		public UndoAction() {
			super("Undo");
			setEnabled(false);
		}
		
		public void actionPerformed(ActionEvent e) {
			try {
				undo.undo();
			} catch (CannotUndoException ex) {
				BerichtsheftPlugin.consoleMessage("Unable to undo: " + ex);
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
		
		public void actionPerformed(ActionEvent e) {
			try {
				undo.redo();
			} catch (CannotRedoException ex) {
				BerichtsheftPlugin.consoleMessage("Unable to redo: " + ex);
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
		for(MouseListener listener : getMouseListeners()){
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
					Log.log(Log.ERROR, TextEditor.class, e);
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

public class TextEditor extends Text_Editor implements ITextComponent
{
	public TextEditor() {
	}

    public TextEditor(int rows, int columns) {
    	super(rows, columns);
    }

	public TextEditor(View view) {
		setView(view);
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

	View view = null;

	private View getView() {
		return view;
	}
	
	private void setView(View vw) {
		view = vw;
		if (Activity.frame == null)
			Activity.frame = view;
	}
	
	public boolean hasJEditTextArea() {
		View view = getView();
		return view != null && view.getTextArea() != null;
	}
	
	private TextArea[] textAreas = {null,null};

	public void setTextArea(TextArea textArea) {
		textAreas[0] = textArea;
	}
	
	public TextArea getTextArea() {
		return hasJEditTextArea() ? getView().getTextArea() : textAreas[0];
	}
	
	public TextArea getTextArea2() {
		return textAreas[1];
	}
	
	JScrollPane scrollPane = null;
	
	@Override
	public Component getUIComponent() {
		return textAreas[0] != null ? textAreas[0] : (scrollPane == null ? scrollPane = new JScrollPane(this) : scrollPane);
	}

	public void toggle(boolean unbuffered) {
		Component target = getUIComponent();
		Container container = target.getParent();
		if (container != null) {
			container.remove(target);
			if (unbuffered) {
				textAreas[1] = textAreas[0];
				setTextArea(null);
			} else if (textAreas[1] != null) {
				setTextArea(textAreas[1]);
				textAreas[1] = null;
			}
			target = getUIComponent();
			if (container.getLayout() instanceof BorderLayout)
				container.add(target, BorderLayout.CENTER);
			else
				container.add(target);
			container.validate();
			container.repaint();
		}
    }

	public TextEditor createBufferedTextArea(String modeName, String modeFileName) {
		boolean useEmbedded = false;
		if (useEmbedded && BerichtsheftPlugin.insideJEdit()) {
			textAreas[0] = new JEditEmbeddedTextArea() {
				{
					setRightClickPopupEnabled(true);
				}
				@Override
				public void createPopupMenu(MouseEvent evt) {
					popup = new JPopupMenu();
					TextEditor.this.createPopupMenu(this, popup);
				}
			};
		}
		else {
			final Properties props = new Properties();
			String keymapDirName = pathCombine(BerichtsheftPlugin.getSettingsDirectory(), "keymaps");
			String keymapFileName = pathCombine(keymapDirName, "imported_keys.props");
			if (!fileExists(keymapFileName))
				keymapFileName = pathCombine(keymapDirName, "jedit_keys.props");
			props.putAll(BerichtsheftPlugin.loadProperties(keymapFileName));
			props.putAll(BerichtsheftPlugin.loadProperties("/org/gjt/sp/jedit/jedit.props"));
			props.setProperty("buffer.folding", "explicit");
			props.setProperty("buffer.tabSize", "4");
			props.setProperty("buffer.indentSize", "4");
			props.setProperty("buffer.wrap", "soft");
			props.setProperty("buffer.maxLineLen", "0");
			String antiAlias = BerichtsheftPlugin.getProperty("view.antiAlias", "standard");
			props.setProperty("view.antiAlias", antiAlias);
			IPropertyManager propsMan = new IPropertyManager() {
				public String getProperty(String name)
				{
					String property = props.getProperty(name);
					return property;
				}
			};
			textAreas[0] = new StandaloneTextArea(propsMan) {
				{
					setRightClickPopupEnabled(true);
				}
				@Override
				public void createPopupMenu(MouseEvent evt) {
					popup = new JPopupMenu();
					TextEditor.this.createPopupMenu(this, popup);
				}
			};
		}
		if (notNullOrEmpty(modeName)) {
			EditBuffer buffer = new EditBuffer();
			TokenMarker tokenMarker = new TokenMarker();
			tokenMarker.addRuleSet(new ParserRuleSet("text","MAIN"));
			buffer.setTokenMarker(tokenMarker);
			textAreas[0].setBuffer(buffer);
			final Mode mode = new Mode(modeName);
			modeFileName = BerichtsheftPlugin.getSettingsDirectory() + modeFileName;
			mode.setProperty("file", modeFileName);
			mode.loadIfNecessary();
			buffer.setMode(mode);
		}
		Dimension size = textAreas[0].getPreferredSize();
		textAreas[0].setPreferredSize(new Dimension(size.width, size.height / 2));
		updateUI();
		return this;
	}
	
	private JMenuItem addMenuItem(final TextArea ta, JPopupMenu popup, String action, String label)
	{
		final JEditBeanShellAction shellAction = ta.getActionContext().getAction(action);
		if (shellAction == null)
			return null ;
		JMenuItem item = new JMenuItem();
		item.setAction(new AbstractAction(label)
		{
			public void actionPerformed(ActionEvent e) {
				shellAction.invoke(ta);
			}
		});
		popup.add(item);
		return item;
	}

	private void createPopupMenu(TextArea ta, JPopupMenu popup)
	{
		addMenuItem(ta, popup, "undo", "Undo");
		addMenuItem(ta, popup, "redo", "Redo");
		popup.addSeparator();
		addMenuItem(ta, popup, "cut", "Cut");
		addMenuItem(ta, popup, "copy", "Copy");
		addMenuItem(ta, popup, "paste", "Paste");
	}
	
	public class EditBuffer extends JEditBuffer
	{
		@Override
		protected TokenMarker.LineContext markTokens(Segment seg, 
				TokenMarker.LineContext prevContext,
				final TokenHandler _tokenHandler) 
		{
			TokenMarker.LineContext context = tokenMarker.markTokens(prevContext, new TokenHandler() {
				public void handleToken(Segment seg, byte id, int offset, int length, LineContext context) {
					Highlight[] hilites = getHighlighter().getHighlights();
					if (spellchecking && id == 0) {
						int offset2 = offset + length, len;
						for (int i = 0; i < hilites.length; i++) {
							Highlight hilite = hilites[i];
							int start = hilite.getStartOffset();
							int end = hilite.getEndOffset();
							if (start < offset2 && end > offset) {
//								println("token (%d,%d) '%s' hilite %d : (%d,%d) '%s'", 
//										offset, length, seg.subSequence(offset, offset2), 
//										1+i, start, end, getText().substring(start, end));
								if (start <= offset) {
									len = Math.min(length, end - offset);
									_tokenHandler.handleToken(seg, 
											Token.INVALID, 
											offset, len, context);
									offset += len;
								}
								else {
									len = start - offset;
									_tokenHandler.handleToken(seg, 
											id, 
											offset, len, context);
									offset += len;
									len = Math.min(offset2, end) - start;
									_tokenHandler.handleToken(seg, 
											Token.INVALID, 
											start, len, context);
									offset += len;
								}
							}
						}
						length = offset2 - offset;
					}
					_tokenHandler.handleToken(seg, id, offset, length, context);
				}
				public void setLineContext(LineContext lineContext) {
					_tokenHandler.setLineContext(lineContext);
				}
			}, seg);
			return context;
		}
		
	}
	
	private boolean spellchecking = false;
	
	@Deprecated
	public void spellcheck() {
		TextArea textArea = getTextArea();
		if (textArea != null) {
	        SpellChecker.register(this);
			super.setText(textArea.getText());
			spellchecking = true;
			textArea.setText(super.getText());
			spellchecking = false;
	        SpellChecker.unregister(this);
		}
	}
	
	@Override
	public void setText(String text) {
		TextArea textArea = getTextArea();
		if (textArea != null)
			textArea.setText(text);
		else
			super.setText(text);
	}

	@Override
	public String getText() {
		TextArea textArea = getTextArea();
		if (textArea != null)
			return textArea.getText();
		else
			return super.getText();
	}

	@Override
	public void setSelection(int start, int end) {
		TextArea textArea = getTextArea();
		if (textArea != null) {
			textArea.setSelection(new Selection.Range(start, end));
			textArea.requestFocus();
		}
		else {
			super.setSelectionStart(start);
			super.setSelectionEnd(end);
			super.requestFocus();
		}
	}

	@Override
	public void setSelectedText(String text) {
		TextArea textArea = getTextArea();
		if (textArea != null) {
			int start = textArea.getCaretPosition();
			textArea.setSelectedText(text);
			if (notNullOrEmpty(text))
				setSelection(start, start + text.length());
		}
		else {
			int start = super.getSelectionStart();
			super.replaceRange(text, start, super.getSelectionEnd());
			setSelection(start, start + (notNullOrEmpty(text) ? 0 : text.length()));
		}
	}

	@Override
	public String getSelectedText() {
		TextArea textArea = getTextArea();
		if (textArea != null)
			return textArea.getSelectedText();
		else
			return super.getSelectedText();
	}

	@Override
	public void addKeyListener(KeyListener l) {
		TextArea textArea = getTextArea();
		if (textArea != null)
			textArea.addKeyListener(l);
		else
			super.addKeyListener(l);
	}
	
	private Job<ITextComponent> onTextChanged = null;
	
	private void update() {
		try {
			onTextChanged.perform(TextEditor.this, objects());
		} catch (Exception e) {
			Log.log(Log.ERROR, TextEditor.class, e);
		}
	}

	public void setOnTextChanged(final Job<ITextComponent> onTextChanged) {
		this.onTextChanged = onTextChanged;
		TextArea textArea = getTextArea();
		if (textArea != null) {
			textArea.getBuffer().addBufferListener(new BufferAdapter() {
				@Override
				public void transactionComplete(JEditBuffer buffer) {
				}
				@Override
				public void bufferLoaded(JEditBuffer buffer) {
				}
				@Override
				public void preContentRemoved(JEditBuffer buffer, int startLine,
						int offset, int numLines, int length) {
				}
				@Override
				public void preContentInserted(JEditBuffer buffer, int startLine,
						int offset, int numLines, int length) {
				}
				@Override
				public void foldLevelChanged(JEditBuffer buffer, int startLine, int endLine) {
				}
				@Override
				public void foldHandlerChanged(JEditBuffer buffer) {
				}
				@Override
				public void contentRemoved(JEditBuffer buffer, int startLine, int offset, int numLines, int length) {
					update();
				}
				@Override
				public void contentInserted(JEditBuffer buffer, int startLine, int offset, int numLines, int length) {
					update();
				}
			});
		}
		else {
			super.getDocument().addDocumentListener(new DocumentListener() {
				public void removeUpdate(DocumentEvent e) {
					update();
				}
				public void insertUpdate(DocumentEvent e) {
					update();
				}
				public void changedUpdate(DocumentEvent e) {
					update();
				}
			});
		}
	}
}

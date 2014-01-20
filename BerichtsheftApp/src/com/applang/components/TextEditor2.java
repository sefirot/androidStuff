package com.applang.components;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Properties;

import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.WindowConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.Segment;
import javax.swing.text.Highlighter.Highlight;

import org.gjt.sp.jedit.IPropertyManager;
import org.gjt.sp.jedit.JEditBeanShellAction;
import org.gjt.sp.jedit.Mode;
import org.gjt.sp.jedit.View;
import org.gjt.sp.jedit.buffer.BufferAdapter;
import org.gjt.sp.jedit.buffer.JEditBuffer;
import org.gjt.sp.jedit.syntax.ModeProvider;
import org.gjt.sp.jedit.syntax.ParserRuleSet;
import org.gjt.sp.jedit.syntax.Token;
import org.gjt.sp.jedit.syntax.TokenHandler;
import org.gjt.sp.jedit.syntax.TokenMarker;
import org.gjt.sp.jedit.syntax.TokenMarker.LineContext;
import org.gjt.sp.jedit.textarea.JEditEmbeddedTextArea;
import org.gjt.sp.jedit.textarea.Selection;
import org.gjt.sp.jedit.textarea.StandaloneTextArea;
import org.gjt.sp.jedit.textarea.TextArea;

import android.util.Log;

import com.applang.Util.Constraint;
import com.applang.berichtsheft.plugin.BerichtsheftPlugin;
import com.inet.jortho.SpellChecker;

import static com.applang.Util.*;
import static com.applang.Util2.*;
import static com.applang.SwingUtil.*;

public class TextEditor2 extends DoubleFeature implements ITextComponent
{
	public TextEditor2() {
		setTextEditor(new TextEditor());
	}

	public TextEditor2(int rows, int columns) {
		setTextEditor(new TextEditor(rows, columns));
    }

	public TextEditor2(View view) {
		this();
		setView(view);
	}

	private void setTextEditor(TextEditor textEditor) {
		addNamePart(textEditor, FOCUS);
		setWidget(new JScrollPane(textEditor));
	}

	public TextEditor getTextEditor() {
		return findFirstComponent(getWidget(), FOCUS, Constraint.AMONG);
	}
	
	private View view = null;

	public View getView() {
		return view;
	}
	
	private void setView(View vw) {
		view = vw;
	}
	
	public boolean hasView() {
		View view = getView();
		return view != null && view.getTextArea() != null;
	}
	
	@Override
	public TextArea getTextArea() {
		return hasView() ? getView().getTextArea() : super.getTextArea();
	}
	
	public TextEditor2 createBufferedTextArea(String modeName, String modeFileName) {
		TextArea textArea;
		boolean useEmbedded = false;
		if (useEmbedded && BerichtsheftPlugin.insideJEdit()) {
			textArea = new JEditEmbeddedTextArea() {
				{
					setRightClickPopupEnabled(true);
				}
				@Override
				public void createPopupMenu(MouseEvent evt) {
					popup = new JPopupMenu();
					TextEditor2.this.createPopupMenu(this, popup);
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
			initBufferProperties(props);
			String antiAlias = BerichtsheftPlugin.getProperty("view.antiAlias", "standard");
			props.setProperty("view.antiAlias", antiAlias);
			props.setProperty("view.gutter.borderWidth", "0");
			IPropertyManager propsMan = new IPropertyManager() {
				public String getProperty(String name)
				{
					String property = props.getProperty(name);
					return property;
				}
			};
			textArea = new StandaloneTextArea(propsMan) {
				{
					setRightClickPopupEnabled(true);
				}
				@Override
				public void createPopupMenu(MouseEvent evt) {
					popup = new JPopupMenu();
					TextEditor2.this.createPopupMenu(this, popup);
				}
			};
		}
		if (notNullOrEmpty(modeName)) {
			EditBuffer buffer = new EditBuffer();
			TokenMarker tokenMarker = new TokenMarker();
			tokenMarker.addRuleSet(new ParserRuleSet("text","MAIN"));
			buffer.setTokenMarker(tokenMarker);
			textArea.setBuffer(buffer);
			Mode mode = new Mode(modeName);
			modeFileName = BerichtsheftPlugin.getSettingsDirectory() + modeFileName;
			mode.setProperty("file", modeFileName);
			ModeProvider.instance.addMode(mode);	//	mode.loadIfNecessary();
			buffer.setMode(mode);
		}
		Dimension size = textArea.getPreferredSize();
		textArea.setPreferredSize(new Dimension(size.width, size.height / 2));
		addNamePart(textArea, FOCUS);
		setTextArea(textArea);
		getTextEditor().updateUI();
		return this;
	}

	public static void initBufferProperties(final Properties props) {
		props.setProperty("buffer.folding", "explicit");
		props.setProperty("buffer.tabSize", "4");
		props.setProperty("buffer.indentSize", "4");
		props.setProperty("buffer.wrap", "soft");
		props.setProperty("buffer.maxLineLen", "0");
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
					TextEditor textEditor = getTextEditor();
					if (textEditor != null && spellchecking && id == 0) {
						Highlight[] hilites = textEditor.getHighlighter().getHighlights();
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
		TextEditor textEditor = getTextEditor();
		if (textEditor != null && textArea != null) {
	        SpellChecker.register(textEditor);
	        textEditor.setText(textArea.getText());
			spellchecking = true;
			textArea.setText(textEditor.getText());
			spellchecking = false;
	        SpellChecker.unregister(textEditor);
		}
	}
	
	@Override
	public void setText(String text) {
		TextArea textArea = getTextArea();
		if (textArea != null)
			textArea.setText(text);
		else {
			TextEditor textEditor = getTextEditor();
			if (textEditor != null)
				textEditor.setText(text);
			else
				Log.w(TAG, "setText not possible");
		}
	}

	@Override
	public String getText() {
		TextArea textArea = getTextArea();
		if (textArea != null)
			return textArea.getText();
		else {
			TextEditor textEditor = getTextEditor();
			if (textEditor != null)
				return textEditor.getText();
			Log.w(TAG, "getText not possible, returning null");
			return null;
		}
	}

	@Override
	public void setSelection(int start, int end) {
		TextArea textArea = getTextArea();
		if (textArea != null) {
			textArea.setSelection(new Selection.Range(start, end));
			textArea.requestFocus();
		} else {
			TextEditor textEditor = getTextEditor();
			if (textEditor != null) {
				textEditor.setSelectionStart(start);
				textEditor.setSelectionEnd(end);
				textEditor.requestFocus();
			}
			else
				Log.w(TAG, "setSelection not possible");
		}
	}

	@Override
	public void setSelectedText(String text) {
		TextArea textArea = getTextArea();
		if (textArea != null) {
			int start = textArea.getSelection()[0].getStart();
			textArea.setSelectedText(text);
			if (notNullOrEmpty(text))
				setSelection(start, start + text.length());
		} else {
			TextEditor textEditor = getTextEditor();
			if (textEditor != null) {
				int start = textEditor.getSelectionStart();
				textEditor.replaceRange(text, start, textEditor.getSelectionEnd());
				setSelection(start, start + (notNullOrEmpty(text) ? 0 : text.length()));
			}
			else
				Log.w(TAG, "setSelectedText not possible");
		}
	}

	@Override
	public String getSelectedText() {
		TextArea textArea = getTextArea();
		if (textArea != null)
			return textArea.getSelectedText();
		else {
			TextEditor textEditor = getTextEditor();
			if (textEditor != null)
				return textEditor.getSelectedText();
			Log.w(TAG, "getSelectedText not possible, returning null");
			return null;
		}
	}
	
	private Job<ITextComponent> onTextChanged = null;
	
	private void update() {
		try {
			if (onTextChanged != null)
				onTextChanged.perform(this, objects());
		} catch (Exception e) {
			Log.e(TAG, "update", e);
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
		} else {
			TextEditor textEditor = getTextEditor();
			if (textEditor != null) {
				textEditor.getDocument().addDocumentListener(
					new DocumentListener() {
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
			else
				Log.w(TAG, "setOnTextChanged not possible");
		}
	}

	@Override
	public void addKeyListener(KeyListener keyListener) {
		TextArea textArea = getTextArea();
		if (textArea != null)
			textArea.addKeyListener(keyListener);
		else {
			JComponent widget = getWidget();
			if (widget != null)
				widget.addKeyListener(keyListener);
			else
				Log.w(TAG, "addKeyListener not possible");
		}
	}

	public static void main(String...args) throws Exception {
		JFrame frame = new JFrame();
		TextEditor2 textEditor2 = new TextEditor2();
		TextArea textArea = textEditor2.createBufferedTextArea("xml", "/modes/xml.xml").getTextArea();
		textArea.getPainter().addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				println("mouseClicked", identity(e.getSource()));
			}
		});
		printContainer("textArea", textArea, _null());
		frame.getContentPane().add(textArea);
		frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		frame.pack();
		frame.setVisible(true);
	}
}

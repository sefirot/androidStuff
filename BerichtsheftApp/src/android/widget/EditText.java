package android.widget;

import java.awt.Component;
import java.awt.TextComponent;

import javax.swing.JComponent;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.AbstractDocument;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.DocumentFilter;
import javax.swing.text.JTextComponent;
import javax.swing.text.PlainDocument;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;

import static com.applang.Util.*;
import static com.applang.SwingUtil.*;

public class EditText extends TextView
{
	public EditText(Context context) {
    	super(context, null);
    }

    public EditText(Context context, AttributeSet attrs) {
    	super(context, attrs);
    }

    public EditText(Component component) {
		super(component);
	}

	@Override
    protected void create() {
		if (attributeSet == null)
    		inputType = "textMultiLine";
    	if (isMultiLine()) {
    		JTextComponent textArea = new JTextArea();
    		setComponent(textArea);
    	}
    	else {
    		JTextField textField = new JTextField();
    		if (inputType != null && inputType.startsWith("number")) {
    			PlainDocument doc = (PlainDocument) textField.getDocument();
    			doc.setDocumentFilter(new NumericFilter(inputType));
    		}
			setMaximumDimension(textField, 100);
    		setComponent(textField);
    	}
	}

    public JTextComponent getTextComponent() {
    	return (JTextComponent) getTaggedComponent();
    }
	
    @Override
	public void setText(String text) {
		getTextComponent().setText(text);
	}

    @Override
	public String getText() {
		return getTextComponent().getText();
	}

	public void setSelection(int start, int stop) {
		JTextComponent textArea = getTextComponent();
		textArea.setSelectionStart(start);
		textArea.setSelectionEnd(stop);
	}

    /**
     * Appends the given text to the end of the document.  Does nothing if
     * the model is null or the string is null or empty.
     *
     * @param str the text to insert
     * @see #insert
     */
    public void append(String str) {
        Document doc = getTextComponent().getDocument();
        if (doc != null) {
            try {
                doc.insertString(doc.getLength(), str, null);
            } 
            catch (BadLocationException e) {
            }
        }
    }

    /**
     * Replaces text from the indicated start to end position with the
     * new text specified.  Does nothing if the model is null.  Simply
     * does a delete if the new string is null or empty.
     *
     * @param str the text to use as the replacement
     * @param start the start position >= 0
     * @param end the end position >= start
     * @exception IllegalArgumentException  if part of the range is an
     *  invalid position in the model
     * @see #insert
     * @see #replaceRange
     */
    public void replaceRange(String str, int start, int end) {
        if (end < start) {
            throw new IllegalArgumentException("end before start");
        }
        Document doc = getTextComponent().getDocument();
        if (doc != null) {
            try {
                if (doc instanceof AbstractDocument) {
                    ((AbstractDocument)doc).replace(start, end - start, str, null);
                }
                else {
                    doc.remove(start, end - start);
                    doc.insertString(start, str, null);
                }
            } 
            catch (BadLocationException e) {
                throw new IllegalArgumentException(e.getMessage());
            }
        }
    }

    /**
     * Inserts the specified text at the specified position.  Does nothing
     * if the model is null or if the text is null or empty.
     *
     * @param str the text to insert
     * @param pos the position at which to insert >= 0
     * @exception IllegalArgumentException  if pos is an
     *  invalid position in the model
     * @see TextComponent#setText
     * @see #replaceRange
     */
    public void insert(String str, int pos) {
        Document doc = getTextComponent().getDocument();
        if (doc != null) {
            try {
                doc.insertString(pos, str, null);
            } 
            catch (BadLocationException e) {
                throw new IllegalArgumentException(e.getMessage());
            }
        }
    }
	
	private Job<JComponent> onTextChanged = null;
	
	private void textChanged() {
		try {
			if (onTextChanged != null)
				onTextChanged.perform(getTextComponent(), objects());
		} catch (Exception e) {
			Log.e(TAG, "textChanged", e);
		}
	}

	public void setOnTextChanged(final Job<JComponent> onTextChanged) {
		this.onTextChanged = onTextChanged;
		JTextComponent textComponent = getTextComponent();
		if (textComponent != null) {
			textComponent.getDocument().addDocumentListener(
				new DocumentListener() {
					public void removeUpdate(DocumentEvent e) {
						textChanged();
					}

					public void insertUpdate(DocumentEvent e) {
						textChanged();
					}

					public void changedUpdate(DocumentEvent e) {
						textChanged();
					}
				});
		}
		else
			Log.w(TAG, "setOnTextChanged not possible");
	}
	
    @Override
	public void applyAttributes() {
		if (attributeSet != null) {
			String value = attributeSet.getAttributeResourceItem("readOnly");
	    	if (notNullOrEmpty(value))
	    		getTextComponent().setEditable(!"true".equals(value));
		}
		super.applyAttributes();
	}
	
	class NumericFilter extends DocumentFilter
	{
		private String inputType;
		
		public NumericFilter(String inputType) {
			this.inputType = inputType;
		}
		
		public boolean test(String text, String inputType) {
			try {
				if (nullOrEmpty(text) || "-".equals(text) || "+".equals(text))
					return true;
				else if (inputType.equals("numberDecimal")) {
					Double.parseDouble(text);
				}
				else {
					Long.parseLong(text);
				}
				return true;
			} catch (NumberFormatException e) {
				EditText.this.message("edittext.numeric-test.message");
				return false;
			}
		}
		
		@Override
		public void insertString(FilterBypass fb, 
				int offset, String string, 
				javax.swing.text.AttributeSet attrs) throws BadLocationException
		{
			if (testInContext(fb, offset, null, string, 
					new Function<StringBuilder>() {
				public StringBuilder apply(Object... params) {
					StringBuilder sb = (StringBuilder) params[0];
					Integer offset = (Integer) params[1];
					sb.insert(offset, (String) params[3]);
					return sb;
				}
			}))
			{
				super.insertString(fb, offset, string, attrs);
			}
		}
		
		@Override
		public void replace(FilterBypass fb, 
				int offset, int length, String text, 
				javax.swing.text.AttributeSet attrs) throws BadLocationException 
		{
			if (testInContext(fb, offset, length, text, 
					new Function<StringBuilder>() {
				public StringBuilder apply(Object... params) {
					StringBuilder sb = (StringBuilder) params[0];
					Integer offset = (Integer) params[1];
					sb.replace(offset, offset + (Integer) params[2], (String) params[3]);
					return sb;
				}
			}))
			{
				super.replace(fb, offset, length, text, attrs);
			}
		}
		
		@Override
		public void remove(FilterBypass fb, int offset, int length) throws BadLocationException {
			if (testInContext(fb, offset, length, null, 
					new Function<StringBuilder>() {
				public StringBuilder apply(Object... params) {
					StringBuilder sb = (StringBuilder) params[0];
					Integer offset = (Integer) params[1];
					sb.delete(offset, offset + (Integer) params[2]);
					return sb;
				}
			}))
			{
				super.remove(fb, offset, length);
			}
		}
		
		private boolean testInContext(FilterBypass fb, 
				Integer offset, Integer length, String text, 
				Function<StringBuilder> oper) throws BadLocationException
		{
			Document doc = fb.getDocument();
			StringBuilder sb = new StringBuilder();
			sb.append(doc.getText(0, doc.getLength()));
			sb = oper.apply(sb, offset, length, text);
			return test(sb.toString(), inputType);
		}
	}

}
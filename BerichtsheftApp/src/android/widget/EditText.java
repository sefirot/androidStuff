package android.widget;

import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.DocumentFilter;
import javax.swing.text.JTextComponent;
import javax.swing.text.PlainDocument;

import android.content.Context;
import android.util.AttributeSet;

import static com.applang.Util.*;
import static com.applang.SwingUtil.*;

public class EditText extends TextView {

    public EditText(Context context, AttributeSet attrs) {
    	super(context, attrs);
    	if (attrs == null)
    		inputType = "textMultiLine";
    }

    @Override
    protected void create(Object... params) {
    	if ("textMultiLine".equals(inputType)) {
    		JTextArea textArea = new JTextArea();
    		textArea.setEditable(true);
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
    	if (attributeSet != null) {
			String defaultValue = attributeSet.getAttributeValue("android:text");
			if (notNullOrEmpty(defaultValue))
				setText(defaultValue);
		}
	}
    
    public JTextComponent getTextArea() {
    	return (JTextComponent) getTextComponent();
    }
	
    @Override
	public void setText(String text) {
		getTextArea().setText(text);
	}

    @Override
	public String getText() {
		return getTextArea().getText();
	}
	
    @Override
	public void append (CharSequence text) {
		String t = getTextArea().getText();
		setText(t + text.toString());
	}

	public void setSelection(int start, int stop) {
		JTextComponent textArea = getTextArea();
		textArea.setSelectionStart(start);
		textArea.setSelectionEnd(stop);
	}

	public void setHorizontallyScrolling(boolean whether) {
		JTextArea textArea = (JTextArea) getTextArea();
		textArea.setLineWrap(!whether);
		textArea.setWrapStyleWord(!whether);
	}

}

class NumericFilter extends DocumentFilter
{
	private String inputType;
	
	public NumericFilter(String inputType) {
		this.inputType = inputType;
	}

	public static boolean test(String text, String inputType) {
		try {
			if (nullOrEmpty(text))
				return true;
			else if (inputType.equals("numberDecimal")) {
				Double.parseDouble(text);
			}
			else {
				Long.parseLong(text);
			}
			return true;
		} catch (NumberFormatException e) {
			message("no way, NOT a valid number");
			return false;
		}
	}
	
	@Override
	public void insertString(FilterBypass fb, int offset, String string, javax.swing.text.AttributeSet attrs) throws BadLocationException {
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
	public void replace(FilterBypass fb, int offset, int length, String text, javax.swing.text.AttributeSet attrs) throws BadLocationException {
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
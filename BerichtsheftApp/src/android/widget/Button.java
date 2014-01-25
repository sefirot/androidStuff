package android.widget;

import static com.applang.Util.NEWLINE_REGEX;
import static com.applang.Util.enclose;

import javax.swing.JButton;

import android.content.Context;
import android.util.AttributeSet;

public class Button extends TextView
{
	public Button(Context context) {
		super(context);
	}

	public Button(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

    protected void create(Object... params) {
    	JButton button = new JButton();
		setComponent(button);
	}
    
    public JButton getButton() {
    	return (JButton) getTextComponent();
    }
	
	public void setText(String text) {
    	if (isMultiLine()) {
    		text = enclose("<html>", text.replaceAll(NEWLINE_REGEX, "<br>"), "</html>");
    	}
    	getButton().setText(text);
	}

	public String getText() {
		return getButton().getText();
	}

}

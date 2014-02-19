package android.widget;

import java.awt.Insets;

import javax.swing.JButton;

import android.content.Context;
import android.util.AttributeSet;

import static com.applang.Util.*;
import static com.applang.SwingUtil.*;

public class Button extends TextView
{
	public Button(Context context) {
		super(context);
	}

	public Button(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	@Override
    protected void create() {
    	JButton button = new JButton();
		setComponent(button);
		button.setMargin(new Insets(0,0,0,0));
	}
    
    public JButton getButton() {
    	return (JButton) getTaggedComponent();
    }
	
	public void setText(String text) {
    	if (isMultiLine()) {
    		text = enclose("<html>", text.replaceAll(NEWLINE_REGEX, "<br>"), "</html>");
    	}
    	JButton btn = getButton();
		btn.setText(text);
    	adjustButtonSize(btn);
	}

	public String getText() {
		return getButton().getText();
	}

}

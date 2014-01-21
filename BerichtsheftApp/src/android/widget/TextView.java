package android.widget;

import static com.applang.Util.*;
import static com.applang.SwingUtil.*;

import java.awt.Color;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.text.JTextComponent;

import android.content.Context;
import android.text.method.MovementMethod;
import android.text.method.ScrollingMovementMethod;
import android.util.AttributeSet;
import android.view.View;

public class TextView extends View
{
    public TextView(Context context, AttributeSet attrs) {
		super(context, attrs);
    }

    protected void create(Object... params) {
		JLabel label = new JLabel();
		setComponent(label);
    	if (attributeSet != null) {
	    	String defaultValue = attributeSet.getAttributeValue("android:text");
	    	if (notNullOrEmpty(defaultValue))
	    		setText(defaultValue);
    	}
	}
    
    public JComponent getTextComponent() {
    	JComponent component = (JComponent) getComponent();
		if (component instanceof JScrollPane)
			return (JComponent) findFirstComponent(component, tag, Constraint.AMONG);
		else
			return component;
    }
    
    public JLabel getLabel() {
    	return (JLabel) getTextComponent();
    }

	public void setMovementMethod(MovementMethod movementMethod) {
		if (movementMethod instanceof ScrollingMovementMethod) {
			JComponent component = (JComponent) getComponent();
			if (component instanceof JTextComponent && !(component instanceof JTextField)) {
				setComponent(new JScrollPane(component));
			}
		}
	}
	
	public void setText(String text) {
    	if ("textMultiLine".equals(inputType)) {
    		text = enclose("<html>", text.replaceAll(NEWLINE_REGEX, "<br>"), "</html>");
    	}
		getLabel().setText(text);
	}

	public String getText() {
		return getLabel().getText();
	}
	
	public void append (CharSequence text) {
		String t = getLabel().getText();
		setText(t + text.toString());
	}
	
	public void append(CharSequence text, int start, int end) {
		append(text.subSequence(start, end));
	}

	public void setTextColor(int color) {
		getComponent().setForeground(new Color(color));
	}

	public void setGravity(int center) {
		// TODO Auto-generated method stub
		
	}

}

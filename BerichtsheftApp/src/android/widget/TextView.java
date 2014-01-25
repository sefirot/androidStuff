package android.widget;

import java.awt.Color;
import java.awt.Component;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;

import android.content.Context;
import android.content.res.Resources;
import android.text.method.MovementMethod;
import android.text.method.ScrollingMovementMethod;
import android.util.AttributeSet;
import android.view.View;

import static com.applang.Util.*;

public class TextView extends View
{
	public TextView(Context context) {
		this(context, null);
    }

    public TextView(Context context, AttributeSet attrs) {
		super(context, attrs);
		if (isMultiLine())
			setMovementMethod(new ScrollingMovementMethod());
    }

    protected void create(Object... params) {
		if (attributeSet == null)
    		inputType = "textMultiLine";
		JLabel label = new JLabel();
		setComponent(label);
	}
    
    public JComponent getTextComponent() {
    	JComponent component = taggedComponent();
		if (component instanceof JScrollPane)
			return taggedComponent();
		else
			return component;
    }
    
    public JLabel getLabel() {
    	return (JLabel) getTextComponent();
    }

	public void setMovementMethod(MovementMethod movementMethod) {
		if (movementMethod instanceof ScrollingMovementMethod) {
			JComponent component = (JComponent) getComponent();
			if (!(component instanceof JScrollPane) && !(component instanceof JTextField)) {
				setComponent(new JScrollPane(component));
			}
		}
	}

	public boolean isMultiLine() {
		return "textMultiLine".equals(inputType);
	}
	
	public void setText(String text) {
    	if (isMultiLine() && !text.substring(0, Math.min(text.length(), 10)).toLowerCase().startsWith("<html>")) {
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
		Component component = taggedComponent();
		if (component != null) {
			Color clr = new Color(
					android.graphics.Color.red(color), 
					android.graphics.Color.green(color), 
					android.graphics.Color.blue(color));
			component.setForeground(clr);
		}
	}

	public void setGravity(int center) {
		// TODO Auto-generated method stub
		
	}
	
    @Override
	public void applyAttributes() {
		if (attributeSet != null) {
			String value = attributeSet.getAttributeValue("android:text");
	    	if (notNullOrEmpty(value))
	    		setText(value);
	    	value = attributeSet.getAttributeValue("android:textColor");
	    	if (notNullOrEmpty(value)) {
				int color= Resources.colorValue(getContext(), value);
				setTextColor(color);
			}
		}
		super.applyAttributes();
	}

}

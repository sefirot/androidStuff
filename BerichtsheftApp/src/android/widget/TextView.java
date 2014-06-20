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

    public TextView(Component component) {
		super(component);
		if (isMultiLine())
			setMovementMethod(new ScrollingMovementMethod());
	}

	@Override
	protected void create() {
		if (attributeSet == null)
    		inputType = "textMultiLine";
		JLabel label = new JLabel();
		setComponent(label);
	}
    
    public JComponent getTaggedComponent() {
    	return taggedComponent();
    }
    
    public JLabel getLabel() {
    	return (JLabel) getTaggedComponent();
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
    	if (isMultiLine()) {
    		text = htmlize(text);
    	}
		getLabel().setText(text);
	}

	public String getText() {
		String text = getLabel().getText();
    	if (isMultiLine())
    		return deHtmlize(text);
    	else
    		return text;
	}
	
	public void append(CharSequence text) {
		String t = getText();
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
	    		setText(Resources.textValue(getContext(), value));
	    	value = attributeSet.getAttributeValue("android:textColor");
	    	if (notNullOrEmpty(value)) {
				int color= Resources.colorValue(getContext(), value);
				setTextColor(color);
			}
		}
		super.applyAttributes();
	}

}

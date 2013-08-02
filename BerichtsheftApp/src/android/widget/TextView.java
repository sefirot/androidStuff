package android.widget;

import static com.applang.Util.*;
import static com.applang.SwingUtil.*;

import java.awt.Color;
import java.awt.Container;

import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import android.content.Context;
import android.text.method.MovementMethod;
import android.text.method.ScrollingMovementMethod;
import android.view.View;

public class TextView extends View {

    public TextView(Context context, Object...params) {
		super(context);
		JTextArea textArea = new JTextArea();
		textArea.setName("textArea");
		setComponent(textArea);
		if (paramBoolean(false, 0, params))
			setMovementMethod(new ScrollingMovementMethod());
		setId(0);
		getTextArea().setEditable(false);
    }
    
    public JTextArea getTextArea() {
		Container component = getComponent();
		if (component instanceof JScrollPane)
			return (JTextArea) findComponent(component, "textArea");
		else
			return (JTextArea) component;
    }

	public void setMovementMethod(MovementMethod movementMethod) {
		if (movementMethod instanceof ScrollingMovementMethod) {
			JComponent component = getComponent();
			if (!(component instanceof JScrollPane)) {
				setComponent(new JScrollPane(component));
			}
		}
	}

	public void setHorizontallyScrolling(boolean whether) {
		JTextArea textArea = getTextArea();
		textArea.setLineWrap(!whether);
		textArea.setWrapStyleWord(!whether);
	}
	
	public void setText(String text) {
		getTextArea().setText(text);
	}

	public String getText() {
		return getTextArea().getText();
	}
	
	public void append (CharSequence text) {
		String t = getTextArea().getText();
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

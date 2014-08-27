package com.applang.components;

import java.awt.Component;
import java.awt.Container;
import java.io.StringWriter;
import java.io.Writer;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;

import org.gjt.sp.jedit.Buffer;
import org.gjt.sp.jedit.EditPane;
import org.gjt.sp.jedit.textarea.TextArea;

import android.util.Log;

import static com.applang.Util.*;
import static com.applang.Util2.*;
import static com.applang.SwingUtil.*;

public class DoubleFeature
{
    protected static final String TAG = DoubleFeature.class.getSimpleName();

	private JComponent widget = null;
	
	public void setWidget(JComponent widget) {
		this.widget = widget;
	}

	public JComponent getWidget() {
		return widget;
	}

	public DoubleFeature(TextArea textArea) {
		setTextArea(textArea);
	}

    @Override
	public String toString() {
		Writer writer = write(new StringWriter(), identity(this));
		String t = "";
		for (int i = 0; i < textAreas.length; i++) {
			t += textAreas[i] == null ? "-" : "+";
		}
		writer = write_assoc(writer, "textAreas", t, 1);
		writer = write_assoc(writer, "widget", 
				functionValueOrElse("null", 
					new Function<String>() {
						public String apply(Object...params) {
							return widget.getClass().getSimpleName();
						}
					}
				), 
				1);
		Container container = getFeature().getParent();
		if (container instanceof EditPane) {
			EditPane editPane = (EditPane)container;
			Buffer buffer = editPane.getBuffer();
			if (buffer != null)
				writer = write_assoc(writer, "buffer", buffer, 1);
		}
		return writer.toString();
	}
	
	protected TextArea[] textAreas = {null,null};

	public void setTextArea(TextArea textArea) {
		textAreas[0] = textArea;
	}
	
	public TextArea getTextArea() {
		return textAreas[0];
	}
	
	public TextArea getTextArea2() {
		return textAreas[1];
	}
	
	public Component getFeature() {
		return textAreas[0] != null ? textAreas[0] : widget;
	}
	
	public void addFeatureTo(Container container) {
		addCenterComponent(getFeature(), container);
		container.validate();
		container.repaint();
	}
	
	protected boolean isolate(Object...params) {
		Component target = getFeature();
		Container container = target.getParent();
		if (container == null) {
			Log.w(TAG, String.format("%s cannot be isolated", identity(target)));
			return false;
		}
		container.remove(target);
		if (params.length > 0)
			params[0] = container;
		return true;
	}
	
	protected void integrate(Container container, boolean featured) {
		if (container != null) {
			if (featured) {
				if (textAreas[0] != null) {
					textAreas[1] = textAreas[0];
					setTextArea(null);
				}
			}
			else {
				if (textAreas[1] != null) {
					setTextArea(textAreas[1]);
					textAreas[1] = null;
				}
			}
			addFeatureTo(container);
			container = null;
		}
	}

	public void toggle(boolean featured, Job<Container> inIsolation, Object...args) {
		Object[] params = {null};
		if (isolate(params)) {
			Container container = (Container) params[0];
			if (inIsolation != null)
				try {
					inIsolation.perform(container, args);
				} catch (Exception e) {
					Log.e(TAG, "toggle", e);
				}
			integrate(container, featured);
		}
    }

	public static final String FOCUS = "focus";
	public static final String REQUEST = "request";
	
	public static Component[] focusRequestComponents(Container container) {
		final Component[] focused = findComponents(container, new Predicate<Component>() {
			public boolean apply(Component c) {
				String name = stringValueOf(c.getName());
				return check(name, Constraint.AMONG, DoubleFeature.FOCUS) || 
						check(name, Constraint.AMONG, DoubleFeature.REQUEST);
			}
		});
		no_println("focusRequest", identity(asList(focused).toArray()));
		return focused;
	}

	public void requestFocus() {
		TextArea textArea = getTextArea();
		if (textArea != null)
			textArea.requestFocus();
		else if (widget != null) {
			Component component = findFirstComponent(widget, FOCUS, Constraint.AMONG);
			if (component != null) {
				component.requestFocusInWindow();
				diag_println(DIAG_OFF, "focus", 
						identity(component), 
						identity(SwingUtilities.getAncestorOfClass(EditPane.class, component)));
			}
		}
	}
}

package com.applang.components;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.event.InputEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;

import org.gjt.sp.jedit.Buffer;
import org.gjt.sp.jedit.EditBus;
import org.gjt.sp.jedit.EditPane;
import org.gjt.sp.jedit.EditPlugin;
import org.gjt.sp.jedit.View;
import org.gjt.sp.jedit.jEdit;
import org.gjt.sp.jedit.EditBus.EBHandler;
import org.gjt.sp.jedit.bufferset.BufferSet;
import org.gjt.sp.jedit.bufferset.BufferSetManager;
import org.gjt.sp.jedit.msg.BufferChanging;
import org.gjt.sp.jedit.msg.BufferUpdate;
import org.gjt.sp.jedit.msg.EditPaneUpdate;
import org.gjt.sp.jedit.msg.ViewUpdate;
import org.gjt.sp.jedit.textarea.Gutter;
import org.gjt.sp.jedit.visitors.JEditVisitorAdapter;
import org.gjt.sp.util.Log;

import static com.applang.SwingUtil.*;
import static com.applang.Util.*;
import static com.applang.Util2.*;
import static com.applang.PluginUtils.*;

/**
 * The DoubleFeature plugin
 * 
 */
public class DoubleFeaturePlugin extends EditPlugin 
{
    protected static DoubleFeaturePlugin self = null;

	@Override
	public void start() {
		self = this;
		EditBus.addToBus(self);
	}

	@Override
	public void stop() {
		EditBus.removeFromBus(self);
	}
	
	protected View view = null;
	
	@EBHandler
	public void handleViewUpdate(ViewUpdate msg)
	{
		view = msg.getView();
		if (msg.getWhat() == ViewUpdate.EDIT_PANE_CHANGED) {
			updateGutters(view, view.getEditPane());
		}
	}
	
	protected HashMap<EditPane, DoubleFeature> doubleFeatures = 
		new HashMap<EditPane, DoubleFeature>();
	
	@EBHandler
	public void handleEditPaneUpdate(EditPaneUpdate msg)
	{
		EditPane editPane = msg.getEditPane();
		if (msg.getWhat() == EditPaneUpdate.CREATED) {
			registerEditPane(editPane);
		}
		else if (msg.getWhat() == EditPaneUpdate.DESTROYED) {
			DoubleFeature doubleFeature = doubleFeatures.get(editPane);
			doubleFeatures.remove(editPane);
			no_println("unregistered", doubleFeature);
			editPane.removeAncestorListener(ancestorListener);
		}
	}
	
	public DoubleFeature registerEditPane(EditPane editPane) {
		DoubleFeature doubleFeature = doubleFeatures.get(editPane);
		if (doubleFeature == null) {
			doubleFeature = new DoubleFeature(editPane.getTextArea());
			doubleFeatures.put(editPane, doubleFeature);
			no_println("registered", doubleFeature);
			editPane.addAncestorListener(ancestorListener);
		}
		return doubleFeature;
	}
	
	public static DoubleFeature registerPane(EditPane editPane) {
		return self.registerEditPane(editPane);
	}
	
	AncestorListener ancestorListener = new AncestorListener() {
		public void ancestorRemoved(AncestorEvent event) {
		}
		public void ancestorMoved(AncestorEvent event) {
			EditPane editPane = (EditPane) event.getSource();
			for (Object pane : doubleFeatures.keySet()) {
				DoubleFeature doubleFeature = doubleFeatures.get(pane);
				if (editPane.equals(pane) && doubleFeature.focused)
					focusRequest((EditPane) editPane);
			}
		}
		public void ancestorAdded(AncestorEvent event) {
		}
	};
	
	@EBHandler
	public void handleBufferChanging(final BufferChanging msg)
	{
		EditPane pane = msg.getEditPane();
		if (msg.getWhat() == EditPaneUpdate.BUFFER_CHANGING) {
			final DoubleFeature doubleFeature = registerEditPane(pane);
			final Buffer buffer = msg.getBuffer();
			if (pendingBuffers.contains(buffer)) {
				pendingFeatures.put(doubleFeature, buffer);
				no_println("pending", doubleFeature);
				if (fileExists(buffer.getPath()))
					return;
			}
			if (featuredBuffers.containsKey(buffer)) {
				doubleFeature.toggle(true, new Job<Container>() {
					public void perform(Container c, Object[] parms) throws Exception {
						doFeature(doubleFeature, buffer, msg);
					}
				});
				diag_println(DIAG_OFF, "featured", identity(pane), doubleFeature);
				focusRequest(pane);
			}
			else {
				doubleFeature.toggle(false, null);
				diag_println(DIAG_OFF, "reduced", identity(pane), doubleFeature);
			}
			Container parent = pane.getParent();
			if (parent != null)
				printContainer(identity(parent), parent, _null());
		}
	};
	
	protected static class FeatureBufferChanging extends BufferChanging
	{
		public InputEvent inputEvent;
		
		public FeatureBufferChanging(Component component, Buffer newBuffer, InputEvent inputEvent) {
			super((EditPane) SwingUtilities.getAncestorOfClass(EditPane.class, component), newBuffer);
			this.inputEvent = inputEvent;
		}
	}
	
	@EBHandler
	public void handleFeatureBufferChanging(FeatureBufferChanging msg)
	{
		if (msg.getWhat() == EditPaneUpdate.BUFFER_CHANGING) {
			handleBufferChanging(msg);
		}
	}
	
	private void focusRequest(final EditPane focusPane) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				setEditPane(focusPane);
				DoubleFeature doubleFeature = doubleFeatures.get(focusPane);
				if (doubleFeature != null)
					doubleFeature.requestFocus();
				updateGutters(view, focusPane);
				String s = "";
				for (Object pane : doubleFeatures.keySet()) {
					boolean focused = focusPane.equals(pane);
					doubleFeature = doubleFeatures.get(pane);
					doubleFeature.focused = focused;
					s += enclose(focused ? "(" : "", identity(pane), focused ? ")" : "", " ");
				}
				diag_println(DIAG_OFF, "focused", s);
			}
		});
	}
	
	public static void diag_visit() {
		JEditVisitorAdapter visitor = new JEditVisitorAdapter() {
			@Override
			public void visit(final EditPane editPane) {
				findComponents(editPane, new Predicate<Component>() {
					public boolean apply(Component c) {
						if (c.hasFocus()) {
							diag_println(DIAG_OFF, "hasFocus", identity(c), identity(editPane));
							return true;
						}
						return false;
					}
				});
			}
		};
		jEdit.visit(visitor);
	}
	
	protected Border getGutterBorder(EditPane editPane, String fieldName) {
		Gutter gutter = editPane.getTextArea().getGutter();
		Border border = getPrivateField(Gutter.class, gutter, fieldName);
		return border == null ? BorderFactory.createEmptyBorder() : border;
	}
	
	protected boolean setGutterBorder(Component component, Border border) {
		if (component instanceof FeatureContainer) {
			FeatureContainer fc = (FeatureContainer) component;
			fc.getGutter().setBorder(border);
			return true;
		}
		else
			return false;
	}
	
	protected void updateGutters(View view, EditPane focusPane)
	{
		EditPane[] editPanes = view.getEditPanes();
		for(int i = 0; i < editPanes.length; i++) {
			EditPane editPane = editPanes[i];
			final Border border = getGutterBorder(editPane, 
					editPane.equals(focusPane) ? "focusBorder" : "noFocusBorder");
			if (featuredBuffers.containsKey(editPane.getBuffer())) {
				findFirstComponent(editPane, new Predicate<Component>() {
					public boolean apply(Component c) {
						return setGutterBorder(c, border);
					}
				});
			}
			else {
				editPane.getTextArea().getGutter().setBorder(border);
			}
		}
	}
	
	private Hashtable<DoubleFeature,Buffer> pendingFeatures = new Hashtable<DoubleFeature,Buffer>();
	private HashSet<Buffer> pendingBuffers = new HashSet<Buffer>();
	private boolean noFeature = false;
	
	@EBHandler
	public void handleBufferUpdate(BufferUpdate msg)
	{
		final Buffer buffer = msg.getBuffer();
		if (msg.getWhat() == BufferUpdate.CREATED) {
			pendingBuffers.add(buffer);
		}
		else if (msg.getWhat() == BufferUpdate.LOADED) {
			pendingBuffers.remove(buffer);
			if (!noFeature) {
				String feature = buffer.getStringProperty(FEATURE);
				if (notNullOrEmpty(feature) && !featuredBuffers.containsKey(buffer)) {
					addFeature(buffer, feature);
				}
				EditPane editPane = null;
				if (pendingFeatures.containsValue(buffer)) {
					for (DoubleFeature doubleFeature : pendingFeatures.keySet()) 
						if (buffer.equals(pendingFeatures.get(doubleFeature))) {
							pendingFeatures.remove(doubleFeature);
							for (Map.Entry<EditPane,DoubleFeature> entry : doubleFeatures.entrySet())
								if (entry.getValue().equals(doubleFeature))
									editPane = entry.getKey();
						}
				}
				else if (featuredBuffers.containsKey(buffer)) {
					EditPane[] editPanes = getEditPanesFor(buffer);
					if (editPanes.length > 0)
						editPane = editPanes[0];
				}
				if (editPane != null) {
					EditBus.send(new BufferChanging(editPane, buffer));
				}
			}
		}
		else if (msg.getWhat() == BufferUpdate.CLOSED) {
			if (featuredBuffers.containsKey(buffer)) 
				removeFeature(buffer);
		}
	}
	
	protected void bufferChange(Buffer buffer) {
		EditPane[] editPanes = getEditPanesFor(buffer);
		if (editPanes.length > 0)
			EditBus.send(new BufferChanging(editPanes[0], buffer));
	}

	public static class FeatureContainer extends JComponent implements MouseListener
	{
		public FeatureContainer(Buffer buffer) {
			setLayout(new BorderLayout());
			this.buffer = buffer;
			addMouseListener(this);
			String format = getProperty("doublefeature.dummy-tooltip.message", "'%s'");
			setToolTipText(String.format(format, buffer));
			addGutter();
		}
		
		Buffer buffer;
		
		public void mouseClicked(MouseEvent ev) {
			EditBus.send(new FeatureBufferChanging((Component)ev.getSource(), buffer, ev));
		}
		public void mousePressed(MouseEvent e) {
		}
		public void mouseReleased(MouseEvent e) {
		}
		public void mouseEntered(MouseEvent e) {
		}
		public void mouseExited(MouseEvent e) {
		}
		
		void addGutter() {
			JPanel gutter = new JPanel();
			gutter.setName("gutter");
			add(gutter, BorderLayout.WEST);
		}

		public JComponent getGutter() {
			return findFirstComponent(this, "gutter");
		}
		
		@Override
		public String toString() {
			Writer writer = write(new StringWriter(), identity(this));
			return writer.toString();
		}
	}
	
	private EditPane getEditPaneByDescendant(MouseEvent e) {
		return (EditPane)SwingUtilities.getAncestorOfClass(
				EditPane.class, 
				(Component)e.getSource());
	}
	
	private MouseListener focusRequestListener = new MouseAdapter() {
		@Override
		public void mouseClicked(MouseEvent e) {
			EditPane editPane = getEditPaneByDescendant(e);
			if (editPane != null)
				focusRequest(editPane);
		}
	};
	
	protected void installFocusClickListener(Container container, boolean...install) {
		Component component = findFirstComponent(container, DoubleFeature.FOCUS, Constraint.AMONG);
		if (component != null) {
			if (param(true, 0, install))
				component.addMouseListener(focusRequestListener);
			else
				component.removeMouseListener(focusRequestListener);
		}
	}
	
	private Hashtable<Buffer,JComponent> featuredBuffers = new Hashtable<Buffer,JComponent>();
	
	protected JComponent constructFeature(final Buffer buffer, String feature, JComponent container, Object...params) throws IOException {
		installFocusClickListener(container);
		return container;
	}
	
	protected void deconstructFeature(String feature, JComponent container) {
		installFocusClickListener(container, false);
	}
	
	protected boolean addFeature(Buffer buffer, String feature, Object...params) {
		JComponent container = new FeatureContainer(buffer);
		try {
			container = constructFeature(buffer, feature, container, 
					arrayappend(objects(focusRequestListener), params));
		} catch (Exception e) {
			Log.log(Log.ERROR, getClass().getName() + ".addFeature", e);
		}
		if (container == null)
			container = new FeatureContainer(buffer);
		buffer.setStringProperty(FEATURE, feature);
		featuredBuffers.put(buffer, container);
		diag_println(DIAG_OFF, "addFeature", buffer);
		return true;
	}
	
	protected void removeFeature(Buffer buffer) {
		JComponent container = featuredBuffers.get(buffer);
		String feature = buffer.getStringProperty(FEATURE);
		deconstructFeature(feature, container);
		buffer.setStringProperty(FEATURE, "");
		featuredBuffers.remove(buffer);
		diag_println(DIAG_OFF, "removeFeature", buffer);
	}
	
	protected JComponent featuredWidget(JComponent widget, BufferChanging msg) {
		return widget;
	}
	
	private void doFeature(DoubleFeature doubleFeature, Buffer buffer, BufferChanging msg) {
		JComponent widget = featuredWidget(featuredBuffers.get(buffer), msg);
		Container container = widget.getParent();
		if (container instanceof EditPane) {
			container.remove(widget);
			DoubleFeature df = doubleFeatures.get(container);
			if (df != null) {
				df.setWidget(new FeatureContainer(buffer));
				df.addFeatureTo(container);
			}
		}
		doubleFeature.setWidget(widget);
	}
	
	public Buffer newFeatureBuffer(String feature, Object...params) {
		BufferSetManager bufferSetManager = jEdit.getBufferSetManager();
		EditPane editPane = jEdit.getActiveView().getEditPane();
		Buffer buffer = editPane.getBuffer();
		Buffer newBuffer = createFeatureBuffer();
		addFeature(newBuffer, feature, params);
		for (BufferSet bufferSet : bufferSetManager.getOwners(buffer)) {
			View[] views = jEdit.getViews();
			for (View view : views) {
				EditPane[] editPanes = view.getEditPanes();
				for (EditPane pane : editPanes) {
					if (pane.getBufferSet() == bufferSet) {
						bufferSetManager.addBuffer(pane, newBuffer);
						if (pane.equals(editPane))
							pane.setBuffer(newBuffer, false);
					}
				}
			}
		}
		return newBuffer;
	}
	
	public static void spellcheckBuffer(Buffer buffer) {
		if (!self.featuredBuffers.containsKey(buffer)) {
			if (self.addFeature(buffer, "spellcheck"))
				self.bufferChange(buffer);
		}
	}

}

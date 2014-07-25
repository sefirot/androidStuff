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
import java.util.HashSet;
import java.util.Hashtable;

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

import android.util.Log;
import static com.applang.SwingUtil.*;
import static com.applang.Util.*;
import static com.applang.Util2.*;
import static com.applang.PluginUtils.*;

import com.applang.Util.Constraint;
import com.applang.components.DoubleFeature;

/**
 * The DoubleFeature plugin
 * 
 */
public class DoubleFeaturePlugin extends EditPlugin 
{
    private static final String TAG = DoubleFeaturePlugin.class.getSimpleName();

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
	
	protected BidiMultiMap doubleFeatures = bmap(3);
	
	@EBHandler
	public void handleEditPaneUpdate(EditPaneUpdate msg)
	{
		EditPane editPane = msg.getEditPane();
		if (msg.getWhat() == EditPaneUpdate.CREATED) {
			registerEditPane(editPane);
		}
		else if (msg.getWhat() == EditPaneUpdate.DESTROYED) {
			DoubleFeature doubleFeature = doubleFeatures.getValue(editPane);
			doubleFeatures.removeKey(editPane);
			no_println("unregistered", doubleFeature);
			editPane.removeAncestorListener(ancestorListener);
		}
	}
	
	public DoubleFeature registerEditPane(EditPane editPane) {
		DoubleFeature doubleFeature = doubleFeatures.getValue(editPane);
		if (doubleFeature == null) {
			doubleFeature = new DoubleFeature(editPane.getTextArea());
			doubleFeatures.add(editPane, doubleFeature, false);
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
			for (Object pane : doubleFeatures.getKeys()) {
				boolean focused = doubleFeatures.getValue(pane, 2);
				if (editPane.equals(pane) && focused)
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
			if (magicBuffers.containsKey(buffer)) {
				doubleFeature.toggle(true, new Job<Container>() {
					public void perform(Container c, Object[] parms) throws Exception {
						doMagic(doubleFeature, buffer, msg);
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
	
	protected static class MagicBufferChanging extends BufferChanging
	{
		public InputEvent inputEvent;
		
		public MagicBufferChanging(Component component, Buffer newBuffer, InputEvent inputEvent) {
			super((EditPane) SwingUtilities.getAncestorOfClass(EditPane.class, component), newBuffer);
			this.inputEvent = inputEvent;
		}
	}
	
	@EBHandler
	public void handleMagicBufferChanging(MagicBufferChanging msg)
	{
		if (msg.getWhat() == EditPaneUpdate.BUFFER_CHANGING) {
			handleBufferChanging(msg);
		}
	}
	
	private void focusRequest(final EditPane focusPane) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				setEditPane(focusPane);
				DoubleFeature doubleFeature = doubleFeatures.getValue(focusPane);
				if (doubleFeature != null)
					doubleFeature.requestFocus();
				updateGutters(view, focusPane);
				String s = "";
				for (Object pane : doubleFeatures.getKeys()) {
					boolean focused = focusPane.equals(pane);
					doubleFeatures.putValue(pane, focused, 2);
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
		if (component instanceof MagicContainer) {
			MagicContainer mc = (MagicContainer) component;
			mc.getGutter().setBorder(border);
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
			if (magicBuffers.containsKey(editPane.getBuffer())) {
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
	private boolean noMagic = false;
	
	@EBHandler
	public void handleBufferUpdate(BufferUpdate msg)
	{
		final Buffer buffer = msg.getBuffer();
		if (msg.getWhat() == BufferUpdate.CREATED) {
			pendingBuffers.add(buffer);
		}
		else if (msg.getWhat() == BufferUpdate.LOADED) {
			pendingBuffers.remove(buffer);
			if (!noMagic) {
				String magic = buffer.getStringProperty(MAGIC);
				if (notNullOrEmpty(magic) && !magicBuffers.containsKey(buffer)) {
					addMagic(buffer, magic);
				}
				EditPane editPane = null;
				if (pendingFeatures.containsValue(buffer)) {
					for (DoubleFeature doubleFeature : pendingFeatures.keySet()) 
						if (buffer.equals(pendingFeatures.get(doubleFeature))) {
							pendingFeatures.remove(doubleFeature);
							editPane = (EditPane) doubleFeatures.getKey(doubleFeature);
						}
				}
				else if (magicBuffers.containsKey(buffer)) {
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
			if (magicBuffers.containsKey(buffer)) 
				removeMagic(buffer);
		}
	}
	
	protected void bufferChange(Buffer buffer) {
		EditPane[] editPanes = getEditPanesFor(buffer);
		if (editPanes.length > 0)
			EditBus.send(new BufferChanging(editPanes[0], buffer));
	}

	public static class DummyContainer extends JComponent implements MouseListener
	{
		public DummyContainer(Buffer buffer) {
			this.buffer = buffer;
			addMouseListener(this);
			String format = getProperty("doublefeature.dummy-tooltip.message", "'%s'");
			setToolTipText(String.format(format, buffer));
		}
		
		Buffer buffer;
		
		public void mouseClicked(MouseEvent ev) {
			EditBus.send(new MagicBufferChanging((Component)ev.getSource(), buffer, ev));
		}
		public void mousePressed(MouseEvent e) {
		}
		public void mouseReleased(MouseEvent e) {
		}
		public void mouseEntered(MouseEvent e) {
		}
		public void mouseExited(MouseEvent e) {
		}
	}
	
	private EditPane getEditPaneByDescendant(MouseEvent e) {
		return (EditPane)SwingUtilities.getAncestorOfClass(
				EditPane.class, 
				(Component)e.getSource());
	}
	
	protected MouseListener focusClickListener = new MouseAdapter() {
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
				component.addMouseListener(focusClickListener);
			else
				component.removeMouseListener(focusClickListener);
		}
	}

	public static class MagicContainer extends JPanel
	{
		public MagicContainer() {
			super(new BorderLayout());
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
	
	private Hashtable<Buffer,JComponent> magicBuffers = new Hashtable<Buffer,JComponent>();
	
	protected JComponent constructMagicContainer(final Buffer buffer, String magic, Object...params) throws IOException {
		MagicContainer container = new MagicContainer();
		installFocusClickListener(container);
		return container;
	}
	
	protected void deconstructMagicContainer(String magic, JComponent container) {
		installFocusClickListener(container, false);
	}
	
	protected boolean addMagic(Buffer buffer, String magic, Object...params) {
		JComponent container = null;
		try {
			container = constructMagicContainer(buffer, magic, params);
		} catch (Exception e) {
			Log.e(TAG, "addMagic", e);
		}
		if (container == null)
			container = new DummyContainer(buffer);
		buffer.setStringProperty(MAGIC, magic);
		magicBuffers.put(buffer, container);
		diag_println(DIAG_OFF, "addMagic", buffer);
		return true;
	}
	
	protected void removeMagic(Buffer buffer) {
		JComponent container = magicBuffers.get(buffer);
		String magic = buffer.getStringProperty(MAGIC);
		deconstructMagicContainer(magic, container);
		buffer.setStringProperty(MAGIC, "");
		magicBuffers.remove(buffer);
		diag_println(DIAG_OFF, "removeMagic", buffer);
	}
	
	protected JComponent trueMagicWidget(JComponent widget, BufferChanging msg) {
		return widget;
	}
	
	private void doMagic(DoubleFeature doubleFeature, Buffer buffer, BufferChanging msg) {
		JComponent widget = trueMagicWidget(magicBuffers.get(buffer), msg);
		Container parent = widget.getParent();
		if (parent instanceof EditPane) {
			parent.remove(widget);
			DoubleFeature df = doubleFeatures.getValue(parent);
			if (df != null) {
				df.setWidget(new DummyContainer(buffer));
				df.addUIComponentTo(parent);
			}
		}
		doubleFeature.setWidget(widget);
	}
	
	public Buffer newMagicBuffer(String magic, Object...params) {
		BufferSetManager bufferSetManager = jEdit.getBufferSetManager();
		EditPane editPane = jEdit.getActiveView().getEditPane();
		Buffer buffer = editPane.getBuffer();
		Buffer newBuffer = createMagicBuffer();
		addMagic(newBuffer, magic, params);
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
		if (!self.magicBuffers.containsKey(buffer)) {
			if (self.addMagic(buffer, "spellcheck"))
				self.bufferChange(buffer);
		}
	}

}

package com.applang.berichtsheft.plugin;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.StringReader;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Properties;
import java.util.Set;

import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;
import javax.swing.filechooser.FileFilter;

import org.gjt.sp.jedit.BeanShell;
import org.gjt.sp.jedit.Buffer;
import org.gjt.sp.jedit.EditAction;
import org.gjt.sp.jedit.EditBus;
import org.gjt.sp.jedit.EditPane;
import org.gjt.sp.jedit.EditPlugin;
import org.gjt.sp.jedit.GUIUtilities;
import org.gjt.sp.jedit.MiscUtilities;
import org.gjt.sp.jedit.View;
import org.gjt.sp.jedit.jEdit;
import org.gjt.sp.jedit.EditBus.EBHandler;
import org.gjt.sp.jedit.bufferset.BufferSet;
import org.gjt.sp.jedit.bufferset.BufferSetManager;
import org.gjt.sp.jedit.gui.DockableWindowManager;
import org.gjt.sp.jedit.gui.RolloverButton;
import org.gjt.sp.jedit.io.VFS;
import org.gjt.sp.jedit.io.VFSManager;
import org.gjt.sp.jedit.msg.BufferChanging;
import org.gjt.sp.jedit.msg.BufferUpdate;
import org.gjt.sp.jedit.msg.EditPaneUpdate;
import org.gjt.sp.jedit.msg.ViewUpdate;
import org.gjt.sp.jedit.textarea.Gutter;
import org.gjt.sp.jedit.visitors.JEditVisitorAdapter;
import org.gjt.sp.util.IOUtilities;

import android.app.Activity;
import android.util.Log;
import android.widget.Toast;

import com.applang.Util.Constraint;
import com.applang.berichtsheft.BerichtsheftActivity;
import com.applang.components.AndroidBridge;
import com.applang.components.DataConfiguration;
import com.applang.components.DataManager;
import com.applang.components.DataManager.DataPanel;
import com.applang.components.DataView;
import com.applang.components.DoubleFeature;
import com.applang.components.TextEditor2;
import com.applang.components.ManagerBase;
import com.applang.components.TextEditor;
import com.inet.jortho.FileUserDictionary;
import com.inet.jortho.SpellChecker;

import static com.applang.Util.*;
import static com.applang.Util2.*;
import static com.applang.SwingUtil.*;

/**
 * The Berichtsheft Plugin
 * 
 */
public class BerichtsheftPlugin extends EditPlugin
{
    private static final String TAG = BerichtsheftPlugin.class.getSimpleName();

	public static final String NAME = "berichtsheft";
	public static final String OPTION_PREFIX = join(".", "options", NAME, "");
	public static final String MENU = "plugin.com.applang.berichtsheft.plugin.BerichtsheftPlugin.menu";
	public static final String CMD_PATH = "/com/applang/berichtsheft/plugin/bsh/";
	
	public static BerichtsheftPlugin self = null;
	
	@Override
	public void start() {
		self = this;
		loadSettings();
		setupSpellChecker(System.getProperty("settings.dir"));
		BeanShell.getNameSpace().addCommandPath(CMD_PATH, getClass());
		BerichtsheftToolBar.init();
		EditBus.addToBus(self);
//		checkAvailabilityOfTools();
	}

	@Override
	public void stop() {
		EditBus.removeFromBus(self);
		BeanShell.getNameSpace().removeCommandPath(CMD_PATH, getClass());
		BerichtsheftToolBar.remove();
		Settings.save();
	}
	
	private View view = null;
	
	@EBHandler
	public void handleViewUpdate(ViewUpdate msg)
	{
		view = msg.getView();
		if (msg.getWhat() == ViewUpdate.CREATED) {
			BerichtsheftToolBar.create(view);
			Activity.frame = view;
		}
		else if (msg.getWhat() == ViewUpdate.EDIT_PANE_CHANGED) {
			updateGutters(view.getEditPane());
		}
		else if (msg.getWhat() == ViewUpdate.CLOSED) {
			BerichtsheftToolBar.remove(view);
			view = null;
		}
	}
	
	@EBHandler
	public void handleEditPaneUpdate(EditPaneUpdate msg)
	{
		EditPane editPane = msg.getEditPane();
		if (msg.getWhat() == EditPaneUpdate.CREATED) {
			registerEditPane(editPane);
		}
		else if (msg.getWhat() == EditPaneUpdate.DESTROYED) {
			DoubleFeature doubleFeature = (DoubleFeature) doubleFeatures.getValue(editPane);
			doubleFeatures.removeKey(editPane);
			no_println("unregistered", doubleFeature);
			editPane.removeAncestorListener(ancestorListener);
		}
	}
	
	private BidiMultiMap doubleFeatures = bmap(3);

	private DoubleFeature registerEditPane(EditPane editPane) {
		DoubleFeature doubleFeature = (DoubleFeature) doubleFeatures.getValue(editPane);
		if (doubleFeature == null) {
			doubleFeature = new DoubleFeature();
			doubleFeature.setTextArea(editPane.getTextArea());
			doubleFeatures.add(editPane, doubleFeature, false);
			no_println("registered", doubleFeature);
			editPane.addAncestorListener(ancestorListener);
		}
		return doubleFeature;
	}
	
	AncestorListener ancestorListener = new AncestorListener() {
		public void ancestorRemoved(AncestorEvent event) {
		}
		public void ancestorMoved(AncestorEvent event) {
			EditPane editPane = (EditPane) event.getSource();
			for (Object pane : doubleFeatures.getKeys()) {
				boolean focused = (Boolean) doubleFeatures.getValue(pane, 2);
				if (editPane.equals(pane) && focused)
					focusRequest((EditPane) editPane);
			}
		}
		public void ancestorAdded(AncestorEvent event) {
		}
	};
	
	@EBHandler
	public void handleBufferChanging(BufferChanging msg)
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
						doMagic(doubleFeature, buffer);
					}
				});
				debug_println(null, "featured", identity(pane), doubleFeature);
				focusRequest(pane);
			}
			else {
				doubleFeature.toggle(false, null);
				debug_println(null, "reduced", identity(pane), doubleFeature);
			}
			Container parent = pane.getParent();
			if (parent != null)
				printContainer(identity(parent), parent, _null());
		}
	};
	
	class MagicBufferChanging extends BufferChanging
	{
		public MagicBufferChanging(Component component, Buffer newBuffer) {
			super((EditPane) SwingUtilities.getAncestorOfClass(EditPane.class, component), newBuffer);
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
				DoubleFeature doubleFeature = (DoubleFeature) doubleFeatures.getValue(focusPane);
				if (doubleFeature != null)
					doubleFeature.requestFocus();
				updateGutters(focusPane);
				String s = "";
				for (Object pane : doubleFeatures.getKeys()) {
					boolean focused = focusPane.equals(pane);
					doubleFeatures.putValue(pane, focused, 2);
					s += enclose(focused ? "(" : "", identity(pane), focused ? ")" : "", " ");
				}
				debug_println(null, "focused", s);
			}
		});
	}
	
	public static void debug_hasFocus() {
		JEditVisitorAdapter visitor = new JEditVisitorAdapter() {
			@Override
			public void visit(final EditPane editPane) {
				findComponents(editPane, new Predicate<Component>() {
					public boolean apply(Component c) {
						if (c.hasFocus()) {
							debug_println(null, "hasFocus", identity(c), identity(editPane));
							return true;
						}
						return false;
					}
				});
			}
		};
		jEdit.visit(visitor);
	}
	
	private Border getBorder(EditPane editPane, String fieldName) {
		try {
			Gutter gutter = editPane.getTextArea().getGutter();
			Field field = Gutter.class.getDeclaredField(fieldName);
			field.setAccessible(true);
			return (Border) field.get(gutter);
		} 
		catch (Exception e) {
			Log.e(TAG, "getBorder", e);
			return BorderFactory.createEmptyBorder();
		}
	}
	
	private void updateGutters(EditPane focusPane)
	{
		EditPane[] editPanes = view.getEditPanes();
		for(int i = 0; i < editPanes.length; i++) {
			EditPane editPane = editPanes[i];
			final Border border = getBorder(editPane, editPane.equals(focusPane) ? "focusBorder" : "noFocusBorder");
			if (magicBuffers.containsKey(editPane.getBuffer())) {
				findFirstComponent(editPane, new Predicate<Component>() {
					public boolean apply(Component c) {
						if (c instanceof DataPanel) {
							DataPanel dp = (DataPanel) c;
							DataManager dm = dp.getDataManager();
							dm.getGutter(dp).setBorder(border);
							return true;
						}
						return false;
					}
				});
			}
			else {
				editPane.getTextArea().getGutter().setBorder(border);
			}
		}
	}

	private void setEditPane(EditPane editPane) {
		View view = editPane.getView();
		try {
			Class<?> cl = Class.forName("org.gjt.sp.jedit.View");
			Method method = cl.getDeclaredMethod("setEditPane", EditPane.class);
			method.setAccessible(true);
			method.invoke(view, editPane);
		} 
		catch (Exception e) {
			Log.e(TAG, "setEditPane", e);
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
	
	@SuppressWarnings("unchecked")
	private EditPane[] getEditPanesFor(final Buffer buffer) {
		try {
			JEditVisitorAdapter visitor = new JEditVisitorAdapter() {
				public Set<EditPane> editPanes = new HashSet<EditPane>();
				
				@Override
				public void visit(EditPane editPane) {
					if (buffer.equals(editPane.getBuffer()))
						editPanes.add(editPane);
				}
			};
			jEdit.visit(visitor);
			return ((Set<EditPane>) visitor.getClass().getField("editPanes").get(visitor))
					.toArray(new EditPane[0]);
		} 
		catch (Exception e) {
			return null;
		}
	}
	
	private void bufferChange(Buffer buffer) {
		EditPane[] editPanes = getEditPanesFor(buffer);
		if (editPanes.length > 0)
			EditBus.send(new BufferChanging(editPanes[0], buffer));
	}

	private EditPane getEditPaneByDescendant(MouseEvent e) {
		return (EditPane)SwingUtilities.getAncestorOfClass(EditPane.class, (Component)e.getSource());
	}
	
	public class DummyWidget extends JComponent implements MouseListener
	{
		public DummyWidget(Buffer buffer) {
			this.buffer = buffer;
			addMouseListener(this);
			String format = getProperty("doublefeature.dummy-tooltip.message", "'%s'");
			setToolTipText(String.format(format, buffer));
		}
		
		Buffer buffer;
		
		public void mouseClicked(MouseEvent e) {
			boolean controlKeyPressed = (e.getModifiers() & ActionEvent.CTRL_MASK) == ActionEvent.CTRL_MASK;
			JComponent widget = magicBuffers.get(buffer);
			if (widget instanceof DataPanel) {
				DataManager dm = ((DataPanel)widget).getDataManager();
				dm.setIndex(controlKeyPressed ? 1 : 0);
			}
			EditBus.send(new MagicBufferChanging((Component)e.getSource(), buffer));
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
	
	private MouseListener clickListener = new MouseAdapter() {
		@Override
		public void mouseClicked(MouseEvent e) {
			EditPane editPane = getEditPaneByDescendant(e);
			if (editPane != null)
				focusRequest(editPane);
		}
	};

	private Hashtable<Buffer,JComponent> magicBuffers = new Hashtable<Buffer,JComponent>();
	
	private boolean addMagic(final Buffer buffer, String magic, Object...params) {
		JComponent container = new DummyWidget(buffer);
		if (magic.startsWith("data")) {
			try {
				buffer.setAutoReloadDialog(false);
				buffer.setAutoReload(true);
				Properties props = param(new Properties(), 0, params);
				String text = buffer.getText();
				props.load(new StringReader(text));
				DataManager dm = new DataManager(view, props, buffer.getName(), clickListener);
				container = dm.getUIComponent(0);
			} catch (Exception e) {
				Log.e(TAG, "addMagic", e);
			}
		}
		else {
			if (magic.startsWith("spell")) {
				TextEditor textEditor = new TextEditor();
				addNamePart(textEditor, DoubleFeature.FOCUS);
				textEditor.installSpellChecker();
				textEditor.setText(buffer.getText());
				container = new JPanel(new BorderLayout());
				addCenterComponent(new JScrollPane(textEditor), container);
				if (magic.equals("spellcheck")) {
					final JToolBar bar = northToolBar(container, BorderLayout.SOUTH);
					bar.add(new ManagerBase<Object>() {
						{
							installButton(bar, 
									ACCEPT_BUTTON_KEY, 
									new ActionListener() {
										public void actionPerformed(ActionEvent e) {
											EditPane[] editPanes = getEditPanesFor(buffer);
											if (editPanes.length > 0) {
												DoubleFeature df = (DoubleFeature) doubleFeatures.getValue(editPanes[0]);
												TextEditor textEditor = (TextEditor) 
													findFirstComponent(df.getWidget(), 
															DoubleFeature.FOCUS, Constraint.AMONG);
												df.getTextArea2().setText(textEditor.getText());
											}
											removeMagic(buffer);
											bufferChange(buffer);
										}
									});
							installButton(bar, 
									REJECT_BUTTON_KEY, 
									new ActionListener() {
										public void actionPerformed(ActionEvent e) {
											removeMagic(buffer);
											bufferChange(buffer);
										}
									});
						}
					});
				}
			} 
			else {
				container = jEdit.getActiveView().getEditPane().getTextArea();
				addNamePart(container, DoubleFeature.FOCUS);
			}
			Component component = findFirstComponent(container, DoubleFeature.FOCUS, Constraint.AMONG);
			if (component != null) 
				component.addMouseListener(clickListener);
		}
		buffer.setStringProperty(MAGIC, magic);
		magicBuffers.put(buffer, container);
		debug_println(null, "addMagic", buffer);
		return true;
	}
	
	private void removeMagic(Buffer buffer) {
		JComponent container = magicBuffers.get(buffer);
		String magic = buffer.getStringProperty(MAGIC);
		if (!magic.startsWith("data")) {
			Component component = findFirstComponent(container, DoubleFeature.FOCUS, Constraint.AMONG);
			if (magic.startsWith("spell")) {
				TextEditor textEditor = (TextEditor) component;
				textEditor.uninstallSpellChecker();
			}
			if (component != null) 
				component.removeMouseListener(clickListener);
			removeNamePart(container, DoubleFeature.FOCUS);
		}
		buffer.setStringProperty(MAGIC, "");
		magicBuffers.remove(buffer);
		debug_println(null, "removeMagic", buffer);
	}
	
	private void doMagic(DoubleFeature doubleFeature, Buffer buffer) {
		JComponent widget = magicBuffers.get(buffer);
		if (widget instanceof DataPanel) {
			DataManager dm = ((DataPanel)widget).getDataManager();
			int index = dm.getIndex();
			widget = dm.getUIComponent(index);
		}
		Container parent = widget.getParent();
		if (parent instanceof EditPane) {
			parent.remove(widget);
			DoubleFeature df = (DoubleFeature) doubleFeatures.getValue(parent);
			if (df != null) {
				df.setWidget(new DummyWidget(buffer));
				df.addUIComponentTo(parent);
			}
		}
		doubleFeature.setWidget(widget);
	}
	
	private static final String MAGIC_TITLE = "Magic-";
	public static final String MAGIC = "magic";

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

	private Buffer createMagicBuffer() {
		int magicCount = getNextMagicBufferId();
		View view = jEdit.getActiveView();
		String parent = null;
		if (view != null) {
			Buffer buffer = view.getBuffer();
			parent = buffer.getDirectory();
		}
		if (parent == null) {
			parent = System.getProperty("user.home");
		}
		VFS vfs = VFSManager.getVFSForPath(parent);
		if ((vfs.getCapabilities() & VFS.WRITE_CAP) == 0) {
			// cannot write on that VFS, creating untitled buffer in home directory
			parent = System.getProperty("user.home");
		}
		Buffer buffer = jEdit.openTemporary(view, "/tmp", MAGIC_TITLE + magicCount,true, null);
		jEdit.commitTemporary(buffer);
		return buffer;
	}
	
	private int getNextMagicBufferId() {
		int magicTitledCount = 0;
		Buffer buffer = jEdit.getFirstBuffer();
		while(buffer != null) {
			if(buffer.getName().startsWith(MAGIC_TITLE)) {
				try {
					magicTitledCount = Math.max(
							magicTitledCount,
							Integer.parseInt(buffer.getName()
									.substring(MAGIC_TITLE.length())));
				}
				catch(NumberFormatException nf) {}
			}
			buffer = buffer.getNext();
		}
		return magicTitledCount + 1;
	}

	@SuppressWarnings("unused")
	private void parseBufferLocalProperties(Buffer buffer) {
		try {
			Class<?> cl = Class.forName("org.gjt.sp.jedit.buffer.JEditBuffer");
			Method method = cl.getDeclaredMethod("parseBufferLocalProperties");
			method.setAccessible(true);
			method.invoke(buffer);
		} 
		catch (Exception e) {
			Log.e(TAG, "parseBufferLocalProperties", e);
		}
	}

	public static Properties loadProperties(String fileName) {
		Properties props = new Properties();
		if (notNullOrEmpty(fileName)) {
			File file = new File(fileName);
			InputStream in = null;
			try {
				if (file.isFile()) {
					in = new FileInputStream(file);
				} else {
					in = BerichtsheftPlugin.class.getResourceAsStream(fileName);
				}
				props.load(in);
			} catch (Exception e) {
				Log.e(TAG, "loadProperties", e);
			} finally {
				IOUtilities.closeQuietly((Closeable) in);
			}
		}
		return props;
	}

	public static void loadSettings() {
		String settingsDir = jEdit.getSettingsDirectory();
		if (settingsDir == null) 
			settingsDir = tempPath();
		File dir = fileOf(settingsDir, "plugins", NAME);
		dir.mkdirs();
		String path = MiscUtilities.constructPath(settingsDir, "console");
		path = MiscUtilities.constructPath(path, "commando");
		File file = new File(path);
		if (!file.exists())
			file.mkdirs();
		BerichtsheftToolBar.userCommandDirectory = path;
		path = dir.getPath();
		System.setProperty("settings.dir", path);
		debug_println("load.settings.dir", path);
		Settings.load();
		path = pathCombine(settingsDir, "jars", "sqlite4java");
		System.setProperty("sqlite4java.library.path", path);
	}
	
	public static void setupSpellChecker(String path) {
		if (!fileExists(new File(path, "dictionaries.cnf")))
			return;
		SpellChecker.setUserDictionaryProvider( new FileUserDictionary(path) );
		try {
			SpellChecker.registerDictionaries( new URL("file", null, path + "/"), null );
		} catch (MalformedURLException e) {
			Log.e(TAG, "setupSpellChecker", e);
		}
	}
	
	public static void spellcheckSelection(View view) {
		final TextEditor2 jEditor = getJEditor();
		String text = jEditor.getSelectedText();
		if (nullOrEmpty(text)) {
			consoleMessage("berichtsheft.no-text-selection.message");
			return; 
		}
		final TextEditor2 textEditor2 = new TextEditor2();
		textEditor2.setText(text);
		Job<Void> takeThis = new Job<Void>() {
			public void perform(Void t, Object[] params) throws Exception {
				String text = textEditor2.getText();
				jEditor.setSelectedText(text);
			}
		};
		textEditor2.getTextEditor().installSpellChecker();
		Component component = textEditor2.getUIComponent();
		component.setPreferredSize(new Dimension(400,300));
		new JEditOptionDialog(view, 
				getProperty("berichtsheft.spellcheck-selection.title"), 
				"", 
				component, 
				JOptionPane.OK_CANCEL_OPTION,
				Behavior.MODAL, 
				getProperty("manager.action-SPELLCHECK.icon"), 
				takeThis);
		textEditor2.getTextEditor().uninstallSpellChecker();
	}
	
	public static void spellcheckBuffer(Buffer buffer) {
		if (!self.magicBuffers.containsKey(buffer)) {
			if (self.addMagic(buffer, "spellcheck"))
				self.bufferChange(buffer);
		}
	}
		
	public static void installDataManager(EditPane editPane) {
		final DoubleFeature doubleFeature = (DoubleFeature) self.registerEditPane(editPane);
		if (doubleFeature != null) {
			Properties props = new Properties();
			if (DataConfiguration.dataProperties(editPane.getView(), props)) {
				int magicCount = getNextMagicTempId();
				String path = DataManager.propsToFile(props, MAGIC_TITLE + magicCount);
				jEdit.openFile(editPane, path);
			}
		}
	}
	
	private static int getNextMagicTempId() {
		int magicTitledCount = 0;
		for (String name : new File("/tmp").list()) 
			if (name.startsWith(MAGIC_TITLE)) 
				try {
					magicTitledCount = Math.max(
							magicTitledCount,
							Integer.parseInt(name
									.substring(MAGIC_TITLE.length())));
				}
				catch(NumberFormatException nf) {}
		return magicTitledCount + 1;
	}

	//	NOTE	leaves standard-out going to log
	public static <T> T suppressJEditErrorLog(Function<T> func, Object...params) {
		try {
			org.gjt.sp.util.Log.init(true,org.gjt.sp.util.Log.ERROR + 1);
			return func.apply(params);
		} 
		finally {
			org.gjt.sp.util.Log.init(false,org.gjt.sp.util.Log.WARNING);
		}
	}
	
	public static void jEditDebugLogging() {
		org.gjt.sp.util.Log.init(true,org.gjt.sp.util.Log.DEBUG);
		org.gjt.sp.jedit.Debug.TOKEN_MARKER_DEBUG = true;
		org.gjt.sp.jedit.Debug.CHUNK_CACHE_DEBUG = true;
	}
	
	public static String getSettingsDirectory() {
		if (!insideJEdit()) {
			return pathCombine(relativePath(), ".jedit");
		}
		return jEdit.getSettingsDirectory();
	}
	
	public static String getOptionProperty(String name) {
		return getProperty(BerichtsheftPlugin.OPTION_PREFIX + name);
	}
	
	public static void setOptionProperty(String name, String value) {
		setProperty(BerichtsheftPlugin.OPTION_PREFIX + name, value);
	}
	
	// NOTE used in scripts
	public static String getProperty(String name) {
		return getProperty(name, null);
	}
	
	public static String getProperty(String name, String defaultValue) {
		String prop;
		if (insideJEdit()) 
			prop = jEdit.getProperty(name, defaultValue);
		else
			prop = properties.getProperty(name, defaultValue);
		if (nullOrEmpty(prop) && (name.endsWith("_COMMAND") || name.endsWith("_SDK"))) 
			prop = getSetting(name, "");
		return prop;
	}
	
	public static Properties properties = null;
	static {
		if (!insideJEdit()) {
			String fileName = pathCombine(relativePath(), "BerichtsheftPlugin.props");
			if (!fileExists(fileName))
				fileName = absolutePathOf(fileName, Constraint.END);
			properties = loadProperties(fileName);
		}
		messageRedirection();
	}
	
	public static boolean insideJEdit() {
		try {
			jEdit.getProperty("tip.show");
			return true;
		} catch (NullPointerException ex) {
			return false;
		}
	}
	
	// NOTE used in scripts
	public static void setProperty(String name, String value) {
		if (insideJEdit())
			jEdit.setProperty(name, value);
		else
			properties.setProperty(name, value);
	}
    
	public static void saveSettings() {
		if (insideJEdit())
			jEdit.saveSettings();
	}
    
	public static void setStatusMessage(String msg) {
		View view = jEdit.getActiveView();
		if (view != null)
			view.getStatus().setMessageAndClear(msg);
		else
			Toast.makeText(BerichtsheftActivity.getInstance(), msg, Toast.LENGTH_LONG).show(true);
	}
    
	public static void messageRedirection() {
		messRedirection = new Function<String>() {
			public String apply(Object... params) {
				String message = param("", 0, params);
				setStatusMessage(message);
				return message;
			}
		};
	}
    
	public static void consoleMessage(String key, Object...params) {
		String format = getProperty(key);
		String msg = notNullOrEmpty(format) ? 
				String.format(format, params) : 
				"<<< message format missing >>>" + com.applang.Util.toString(params);
		BerichtsheftShell.print(msg, NEWLINE);
	}
	
	public static TextEditor2 getJEditor() {
		return new TextEditor2(jEdit.getActiveView());
	}
	
	public static JComponent getDockable(View view, String name, boolean add) {
		DockableWindowManager wm = view.getDockableWindowManager();
		if (add)
			wm.addDockableWindow(name);
		return wm.getDockableWindow(name);
	}
	
	public static void showDockable(View view, String name) {
		DockableWindowManager wm = view.getDockableWindowManager();
		wm.showDockableWindow(name);
	}

	// NOTE used in scripts
	public static void invokeAction(View view, String actionName) {
		EditAction action = jEdit.getAction(actionName);
		if (action != null)
			action.invoke(view);
	}
	
	// NOTE used in scripts
	public static void checkAvailabilityOfTools() {
		BerichtsheftShell.print("Welcome...", NEWLINE);
		String[] tools = {"AWK_COMMAND", "ADB_COMMAND", "SQLITE_COMMAND"};
		for (int i = 0; i < tools.length; i++) {
			debug_println(i, tools[i]);
			String cmd = getCommand(tools[i]);
			debug_println(i, cmd);
			if (!fileExists(cmd)) {
				if (nullOrEmpty(runShellScript("which", "which " + cmd)))
					consoleMessage("berichtsheft.tool-missing.message", cmd);
			}
			else if (tools[i].startsWith("ADB")) {
				String msg = AndroidBridge.adbRestart();
				BerichtsheftShell.print(msg, NEWLINE);
			}
		}
	}

	public static String getCommand(String path) {
		String cmd = getProperty(path); 
		debug_println(path, cmd);
		if (!cmd.startsWith(PATH_SEP) && cmd.contains(PATH_SEP)) {
			String sdk = getProperty("ANDROID_SDK");
			debug_println("ANDROID_SDK", sdk);
			cmd = pathCombine(System.getProperty("user.home"), sdk, cmd);
		}
		return cmd;
	}

	public static Function<File> fileChooser(final View view) {
		return new Function<File>() {
			public File apply(Object... params) {
				boolean toOpen = param(true, 0, params);
				Container parent = param(null, 1, params);
				String title = param(null, 2, params);
				File file = param(null, 3, params);
				String fileName = file == null ? null : file.getPath();
				FileFilter filter = param(null, 4, params);
				String[] paths = view != null ? 
						GUIUtilities.showVFSFileDialog(view, fileName,
								toOpen ? JFileChooser.OPEN_DIALOG : JFileChooser.SAVE_DIALOG, 
								false) : 
						chooseFileNames(toOpen, parent, title, fileName, filter);
				if (isAvailable(0, paths)) 
					return new File(paths[0]);
				else
					return null;
			}
		};
	}

	//	NOTE	used in scripts
	public static String inquireDbFileName(View view, String fileName) {
		File dbFile = DataView.chooseDb(BerichtsheftPlugin.fileChooser(view), true, fileName, true);
    	if (dbFile != null) {
    		return dbFile.getPath();
    	}
    	return null;
	}

	public static String sqliteScript(String db, String statement) {
		String cmd = getCommand("SQLITE_COMMAND"); 
		if (notNullOrEmpty(db))
			cmd += " " + db;
		cmd += " <<<\"" + statement + "\"";
		return cmd;
	}

	public static File getTempFile(String name) {
		return tempFile(name, NAME);
	}
    		
 	// NOTE used in scripts
	public static ImageIcon loadIcon(String path) {
		try {
			if (path.startsWith("/"))
				return iconFrom(path);
			else if (!insideJEdit()) {
				path = pathCombine("/org/gjt/sp/jedit/icons/themes/tango", path);
				return iconFrom(path);
			}
			else
				return (ImageIcon) GUIUtilities.loadIcon(path);
		} catch (Exception e) {
			path = "/org/gjt/sp/jedit/icons/themes/tango/22x22/emblems/emblem-unreadable.png";
			URL url = BerichtsheftPlugin.class.getResource(path);
			return  new ImageIcon(url);
		}
	}

	public static AbstractButton makeCustomButton(CustomAction customAction, boolean flip) {
		AbstractButton btn;
		CustomActionType type = customAction.getType();
		if (type != null) {
			btn = makeCustomButton(type.resourceName(), null, flip);
			btn.setEnabled(true);
		}
		else {
			btn = new RolloverButton();
			String name = customAction.getValue(Action.NAME).toString();
			btn.setName(name);
			btn.setText(name);
		}
		btn.setAction(customAction);
		return btn;
	}

	public static AbstractButton makeCustomButton(String name, ActionListener listener, boolean flip) {
		String toolTip = getProperty(name.concat(".label"));
		ImageIcon icon = loadIcon(getProperty(name + ".icon"));
		if (flip && icon != null) {
			Image img = icon.getImage();
			BufferedImage bimg = horizontalflip(img);
			icon = new ImageIcon(bimg);
		}
		AbstractButton b = new RolloverButton(icon);
		if (listener != null) {
			b.addActionListener(listener);
			b.setEnabled(true);
		} else {
			b.setEnabled(false);
		}
		b.setToolTipText(toolTip);
		return b;
	}

	private static DataView dataView = null;
	
	public static DataView getDataView() {
		if (dataView == null)
			dataView = new DataView();
		return dataView;
	}
}

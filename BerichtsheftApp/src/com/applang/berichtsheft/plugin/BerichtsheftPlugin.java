package com.applang.berichtsheft.plugin;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Properties;

import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
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
import org.gjt.sp.jedit.visitors.JEditVisitorAdapter;
import org.gjt.sp.util.IOUtilities;

import android.util.Log;

import com.applang.Util.Job;
import com.applang.components.AndroidBridge;
import com.applang.components.DataView;
import com.applang.components.DoubleFeature;
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
	
	@Override
	public void start() {
		loadSettings();
		setupSpellChecker(System.getProperty("settings.dir"));
		BeanShell.getNameSpace().addCommandPath(CMD_PATH, getClass());
		
		BerichtsheftToolBar.init();
		
		EditBus.addToBus(this);
		
//		checkAvailabilityOfTools();
	}

	@Override
	public void stop() {
		EditBus.removeFromBus(this);
		
		BeanShell.getNameSpace().removeCommandPath(CMD_PATH, getClass());
		BerichtsheftToolBar.remove();
		
		Settings.save();
	}
	
	@EBHandler
	public void handleViewUpdate(ViewUpdate msg)
	{
		if (msg.getWhat() == ViewUpdate.CREATED)
		{
			View v = msg.getView();
			BerichtsheftToolBar.create(v);
		}
		if (msg.getWhat() == ViewUpdate.CLOSED) {
			View v = msg.getView();
			BerichtsheftToolBar.remove(v);
		}
	}
	
	@EBHandler
	public void handleBufferChanging(BufferChanging msg)
	{
		EditPane editPane = msg.getEditPane();
		if (msg.getWhat() == EditPaneUpdate.BUFFER_CHANGING) {
			final DoubleFeature doubleFeature = registerEditPane(editPane);
			final Buffer buffer = msg.getBuffer();
			if (magicBuffers.containsKey(buffer)) {
				doubleFeature.toggle(true, new Job<Void>() {
					public void perform(Void t, Object[] parms) throws Exception {
						doMagic(doubleFeature, buffer);
					}
				});
				debug_out("featured", doubleFeature);
			}
			else if (pendingBuffers.contains(buffer)) {
				pendingFeatures.put(doubleFeature, buffer);
				debug_out("pending", doubleFeature);
				return;
			}
			else {
				doubleFeature.toggle(false, null);
				debug_out("reduced", doubleFeature);
			}
			printContainer("editPane", editPane);
			focusRequest(editPane);
		}
	}
	
	private void focusRequest(EditPane editPane) {
		if (editPane.getView().getEditPane() == editPane) {
			final DoubleFeature doubleFeature = (DoubleFeature) editPanes.getValue(editPane);
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					doubleFeature.requestFocus();
				}
			});
		}
	}
	
	private Hashtable<DoubleFeature,Buffer> pendingFeatures = new Hashtable<DoubleFeature,Buffer>();
	private HashSet<Buffer> pendingBuffers = new HashSet<Buffer>();
	private EditPane editPane;
	
	@EBHandler
	public void handleBufferUpdate(BufferUpdate msg)
	{
		final Buffer buffer = msg.getBuffer();
		if (msg.getWhat() == BufferUpdate.CREATED) {
			pendingBuffers.add(buffer);
		}
		else if (msg.getWhat() == BufferUpdate.LOADED) {
			pendingBuffers.remove(buffer);
			String magic = buffer.getStringProperty("magic");
			if (notNullOrEmpty(magic)) {
				addMagic(buffer);
			}
			editPane = null;
			if (pendingFeatures.containsValue(buffer)) {
				for (DoubleFeature doubleFeature : pendingFeatures.keySet()) 
					if (buffer.equals(pendingFeatures.get(doubleFeature))) {
						pendingFeatures.remove(doubleFeature);
						editPane = (EditPane) editPanes.getKey(doubleFeature);
					}
			}
			else if (magicBuffers.containsKey(buffer)) {
				jEdit.visit(new JEditVisitorAdapter()
				{
					@Override
					public void visit(EditPane editPane) {
						if (buffer.equals(editPane.getBuffer()))
							BerichtsheftPlugin.this.editPane = editPane;
					}
				});
			}
			if (editPane != null)
				EditBus.send(new BufferChanging(editPane, buffer));
		}
		else if (msg.getWhat() == BufferUpdate.CLOSED) {
			if (magicBuffers.containsKey(buffer)) 
				removeMagic(buffer);
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
			DoubleFeature doubleFeature = (DoubleFeature) editPanes.getValue(editPane);
			editPanes.removeKey(editPane);
			debug_out("unregistered", doubleFeature);
		}
	}
	
	private BidiMultiMap editPanes = bmap(2);

	private DoubleFeature registerEditPane(EditPane editPane) {
		DoubleFeature doubleFeature = (DoubleFeature) editPanes.getValue(editPane);
		if (doubleFeature == null) {
			doubleFeature = new DoubleFeature();
			doubleFeature.setTextArea(editPane.getTextArea());
			editPanes.add(editPane, doubleFeature);
			debug_out("registered", doubleFeature);
		}
		return doubleFeature;
	}
	
	private EditPane getEditPaneFromDescendant(Component comp) {
		while(!(comp instanceof EditPane)) {
			if(comp == null)
				return null;
			comp = comp.getParent();
		}
		return (EditPane)comp;
	}
	
	private class DummyWidget extends JComponent implements MouseListener
	{
		public DummyWidget(Buffer buffer) {
			this.buffer = buffer;
			addMouseListener(this);
		}
		
		Buffer buffer;
		
		public void mouseClicked(MouseEvent e) {
			Component source = (Component)e.getSource();
			EditPane editPane = getEditPaneFromDescendant(source);
			if (editPane != null) {
				EditBus.send(new BufferChanging(editPane, buffer));
			}
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

	private Hashtable<Buffer,JComponent> magicBuffers = new Hashtable<Buffer,JComponent>();
	
	private void addMagic(Buffer buffer) {
		String magic = buffer.getStringProperty("magic");
		if ("spell".equals(magic)) {
			TextEditor textEditor = new TextEditor();
			textEditor.installSpellChecker();
			magicBuffers.put(buffer, textEditor);
		}
		else
			magicBuffers.put(buffer, new DummyWidget(buffer));
		debug_out("addMagic", buffer);
	}
	
	private void removeMagic(Buffer buffer) {
		String magic = buffer.getStringProperty("magic");
		if ("spell".equals(magic)) {
			TextEditor textEditor = (TextEditor) magicBuffers.get(buffer);
			textEditor.uninstallSpellChecker();
		}
		magicBuffers.remove(buffer);
		debug_out("removeMagic", buffer);
	}
	
	private void doMagic(DoubleFeature doubleFeature, Buffer buffer) {
		JComponent widget = magicBuffers.get(buffer);
		Container container = widget.getParent();
		if (container != null) {
			container.remove(widget);
			addCenterComponent(new DummyWidget(buffer), container);
		}
		doubleFeature.setWidget(widget);
		TextEditor textEditor = doubleFeature.getTextEditor();
		if (textEditor != null) {
			textEditor.setText(buffer.getText());
		}
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
	
	private static final String MAGIC_TITLE = "Magic-";

	public void installMagicBuffer() {
		BufferSetManager bufferSetManager = jEdit.getBufferSetManager();
		Buffer currentBuffer = jEdit.getActiveView().getEditPane().getBuffer();
		for (BufferSet bufferSet : bufferSetManager.getOwners(currentBuffer)) {
			Buffer newBuffer = createMagicBuffer();
			View[] views = jEdit.getViews();
			for (View view : views) {
				EditPane[] editPanes = view.getEditPanes();
				for (EditPane editPane : editPanes) {
					if (editPane.getBufferSet() == bufferSet) {
						bufferSetManager.addBuffer(editPane, newBuffer);
						addMagic(newBuffer);
						editPane.setBuffer(newBuffer, false);
					}
				}
			}
		}
	}

	private Buffer createMagicBuffer() {
		int magicCount = getNextMagicBufferId();

		View view = jEdit.getActiveView();
		String parent = null;

		if (view != null)
		{
			Buffer buffer = view.getBuffer();
			parent = buffer.getDirectory();
		}
		if (parent == null)
		{
			parent = System.getProperty("user.home");
		}
		VFS vfs = VFSManager.getVFSForPath(parent);
		if ((vfs.getCapabilities() & VFS.WRITE_CAP) == 0)
		{
			// cannot write on that VFS, creating untitled buffer in home directory
			parent = System.getProperty("user.home");
		}
		Buffer newEmptyBuffer = jEdit.openTemporary(view, "/tmp",
							    MAGIC_TITLE + magicCount,true, null);
		jEdit.commitTemporary(newEmptyBuffer);
		return newEmptyBuffer;
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
		final DoubleFeature jEditor = getJEditor();
		String text = jEditor.getSelectedText();
		if (nullOrEmpty(text)) {
			consoleMessage("berichtsheft.no-text-selection.message");
			return; 
		}
		final DoubleFeature doubleFeature = new DoubleFeature();
		doubleFeature.setText(text);
		Job<Void> takeThis = new Job<Void>() {
			public void perform(Void t, Object[] params) throws Exception {
				String text = doubleFeature.getText();
				jEditor.setSelectedText(text);
			}
		};
		doubleFeature.getTextEditor().installSpellChecker();
		Component component = doubleFeature.getUIComponent();
		component.setPreferredSize(new Dimension(400,300));
		new JEditOptionDialog(view, 
				getProperty("berichtsheft.spellcheck-selection.title"), 
				"", 
				component, 
				JOptionPane.OK_CANCEL_OPTION,
				Behavior.MODAL, 
				getProperty("manager.action-SPELLCHECK.icon"), 
				takeThis);
		doubleFeature.getTextEditor().uninstallSpellChecker();
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
			prop = props.getProperty(name, defaultValue);
		if (nullOrEmpty(prop) && (name.endsWith("_COMMAND") || name.endsWith("_SDK"))) 
			prop = getSetting(name, "");
		return prop;
	}
	
	public static Properties props = null;
	static {
		if (!insideJEdit()) {
			String fileName = pathCombine(relativePath(), "BerichtsheftPlugin.props");
			if (!fileExists(fileName))
				fileName = absolutePathOf(fileName, -1);
			props = loadProperties(fileName);
		}
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
			props.setProperty(name, value);
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
			message(msg);
	}
    
	public static void consoleMessage(String key, Object...params) {
		String format = getProperty(key);
		String msg = notNullOrEmpty(format) ? 
				String.format(format, params) : 
				"<<< message format missing >>>" + com.applang.Util.toString(params);
		BerichtsheftShell.print(msg, NEWLINE);
	}
	
	public static DoubleFeature getJEditor() {
		return new DoubleFeature(jEdit.getActiveView());
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
			String cmd = getProperty(tools[i]);
			if (!fileExists(new File(cmd)))
				consoleMessage("berichtsheft.tool-missing.message", cmd);
			else if (tools[i].startsWith("ADB")) {
				cmd = AndroidBridge.adbRestart();
				BerichtsheftShell.print(cmd, NEWLINE);
			}
		}
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

	public static String getSqliteCommand() {
		String cmd = getProperty("SQLITE_COMMAND"); 
		if (!cmd.startsWith("/")) {
			String sdk = getProperty("ANDROID_SDK");
			cmd = pathCombine(System.getProperty("user.home"), sdk, cmd);
		}
		return cmd;
	}

	public static String sqliteScript(String db, String statement) {
		String cmd = getSqliteCommand(); 
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
	
	public static DataView getDataView() {
		if (dataView == null)
			dataView = new DataView();
		return dataView;
	}

	private static DataView dataView = null;
}

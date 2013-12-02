package com.applang.berichtsheft.plugin;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Properties;

import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileFilter;

import org.gjt.sp.jedit.BeanShell;
import org.gjt.sp.jedit.EditAction;
import org.gjt.sp.jedit.EditBus;
import org.gjt.sp.jedit.EditPlugin;
import org.gjt.sp.jedit.GUIUtilities;
import org.gjt.sp.jedit.MiscUtilities;
import org.gjt.sp.jedit.View;
import org.gjt.sp.jedit.jEdit;
import org.gjt.sp.jedit.EditBus.EBHandler;
import org.gjt.sp.jedit.gui.DockableWindowManager;
import org.gjt.sp.jedit.gui.RolloverButton;
import org.gjt.sp.jedit.msg.ViewUpdate;
import org.gjt.sp.util.IOUtilities;
import org.gjt.sp.util.Log;

import com.applang.components.AndroidBridge;
import com.applang.components.DataView;
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
public class BerichtsheftPlugin extends EditPlugin {
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
	public void handleViewUpdate(ViewUpdate vmsg)
	{
		if (vmsg.getWhat() == ViewUpdate.CREATED)
		{
			View v = vmsg.getView();
			BerichtsheftToolBar.create(v);
		}
		if (vmsg.getWhat() == ViewUpdate.CLOSED) {
			View v = vmsg.getView();
			BerichtsheftToolBar.remove(v);
		}
	}

	public static Properties loadProperties(String fileName)
	{
		Properties props = new Properties();
		File file = new File(fileName);
	
		InputStream in = null;
		try
		{
			if (file.isFile())
			{
				in = new FileInputStream(file);
			}
			else
			{
				in = BerichtsheftPlugin.class.getResourceAsStream(fileName);
			}
			props.load(in);
		}
		catch (IOException e)
		{
			Log.log(Log.ERROR, BerichtsheftPlugin.class, e);
		}
		finally
		{
			IOUtilities.closeQuietly((Closeable)in);
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
			Log.log(Log.ERROR, BerichtsheftPlugin.class, e);
		}
	}
	
	public static void spellcheckSelection(View view) {
		final TextEditor jEditor = getJEditor();
		String text = jEditor.getSelectedText();
		if (nullOrEmpty(text)) {
			consoleMessage("berichtsheft.no-text-selection.message");
			return; 
		}
		final TextEditor textEditor = new TextEditor();
		textEditor.setText(text);
		Job<Void> takeThis = new Job<Void>() {
			public void perform(Void t, Object[] params) throws Exception {
				String text = textEditor.getText();
				jEditor.setSelectedText(text);
			}
		};
		textEditor.installSpellChecker();
		Component component = textEditor.getUIComponent();
		component.setPreferredSize(new Dimension(400,300));
		new JEditOptionDialog(view, 
				getProperty("berichtsheft.spellcheck-selection.title"), 
				"", 
				component, 
				JOptionPane.OK_CANCEL_OPTION,
				Behavior.MODAL, 
				getProperty("manager.action-SPELLCHECK.icon"), 
				takeThis);
		textEditor.uninstallSpellChecker();
	}
	
	public static <T> T suppressErrorLog(Function<T> func, Object...params) {
		try {
			Log.init(true,Log.ERROR + 1);
			return func.apply(params);
		} 
		finally {
			Log.init(true,Log.WARNING);
		}
	}
	
	public static void logDebug() {
		Log.init(true,Log.DEBUG);
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
		try {
			jEdit.getProperty("tip.show");
		} catch (NullPointerException ex) {
			try {
				String fileName = pathCombine(relativePath(), "BerichtsheftPlugin.props");
				props = loadProperties(fileName);
			} catch (Exception e) {}
		}
	}
	public static boolean insideJEdit() {
		return props == null;
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
				"<<< message missing >>>";
		BerichtsheftShell.print(msg, NEWLINE);
	}
	
	public static TextEditor getJEditor() {
		return new TextEditor(jEdit.getActiveView());
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
	
	public static DataView dataView = new DataView();
}

package com.applang.berichtsheft.plugin;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.Properties;

import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JToolBar;
import javax.swing.border.Border;
import javax.swing.filechooser.FileFilter;
import javax.swing.text.JTextComponent;

import org.gjt.sp.jedit.BeanShell;
import org.gjt.sp.jedit.Buffer;
import org.gjt.sp.jedit.EditPane;
import org.gjt.sp.jedit.GUIUtilities;
import org.gjt.sp.jedit.MiscUtilities;
import org.gjt.sp.jedit.View;
import org.gjt.sp.jedit.jEdit;
import org.gjt.sp.jedit.EditBus.EBHandler;
import org.gjt.sp.jedit.gui.DockableWindowManager;
import org.gjt.sp.jedit.msg.BufferChanging;
import org.gjt.sp.jedit.msg.ViewUpdate;

import android.app.Activity;
import android.app.AlertDialog;
import android.util.Log;

import com.applang.Util.Constraint;
import com.applang.components.AndroidBridge;
import com.applang.components.DataConfiguration;
import com.applang.components.DataManager;
import com.applang.components.DataView;
import com.applang.components.DoubleFeaturePlugin;
import com.applang.components.ManagerBase;
import com.applang.components.TextEdit;
import com.applang.components.TextToggle;

import static com.applang.Util.*;
import static com.applang.Util2.*;
import static com.applang.SwingUtil.*;
import static com.applang.PluginUtils.*;

/**
 * The Berichtsheft Plugin
 * 
 */
public class BerichtsheftPlugin extends DoubleFeaturePlugin
{
    public static final String TAG = BerichtsheftPlugin.class.getSimpleName();

	public static final String NAME = "berichtsheft";
	public static final String OPTION_PREFIX = join(".", "options", NAME, "");
	public static final String MENU = "plugin.com.applang.berichtsheft.plugin.BerichtsheftPlugin.menu";
	public static final String CMD_PATH = "/com/applang/berichtsheft/plugin/bsh/";
	
	@Override
	public void start() {
		super.start();
		loadSettings();
		setupSpellChecker(System.getProperty("settings.dir"));
		BeanShell.getNameSpace().addCommandPath(CMD_PATH, getClass());
		BerichtsheftToolBar.init();
//		checkAvailabilityOfTools();
	}

	@Override
	public void stop() {
		super.stop();
		BeanShell.getNameSpace().removeCommandPath(CMD_PATH, getClass());
		BerichtsheftToolBar.remove();
		Settings.save();
	}
	
	@EBHandler
	@Override
	public void handleViewUpdate(ViewUpdate msg)
	{
		super.handleViewUpdate(msg);
		view = msg.getView();
		if (msg.getWhat() == ViewUpdate.CREATED) {
			BerichtsheftToolBar.create(view);
			Activity.frame = view;
		}
		else if (msg.getWhat() == ViewUpdate.CLOSED) {
			BerichtsheftToolBar.remove(view);
			view = null;
		}
	}
	
	@Override
	protected boolean setGutterBorder(Component component, Border border) {
		if (component instanceof DataManager.DataPanel) {
			DataManager.DataPanel dp = (DataManager.DataPanel) component;
			dp.getGutter().setBorder(border);
			return true;
		}
		else
			return super.setGutterBorder(component, border);
	}
	
	@Override
	protected JComponent featuredWidget(JComponent widget, BufferChanging msg) {
		if (widget instanceof DataManager.DataPanel) {
			DataManager dm = ((DataManager.DataPanel)widget).getDataManager();
			if (msg instanceof FeatureBufferChanging) {
				InputEvent ev = ((FeatureBufferChanging)msg).inputEvent;
				dm.setIndex(isCtrlKeyHeld(ev) ? 1 : 0);
			}
			widget = dm.getPane(dm.getIndex());
		}
		return widget;
	}

	@Override
	protected JComponent constructFeature(final Buffer buffer, String feature, JComponent container, Object...params) throws IOException {
		container = super.constructFeature(buffer, feature, container, params);
		if (feature.startsWith("data")) {
			buffer.setAutoReloadDialog(false);
			buffer.setAutoReload(true);
			Properties props = param(new Properties(), 1, params);
			String text = buffer.getText();
			props.load(new StringReader(text));
			MouseListener focusRequestListener = param(null, 0, params);
			DataManager dm = new DataManager(view, props, buffer.getName(), focusRequestListener);
			container = dm.getPane(0);
		}
		else {
			if (feature.startsWith("spell")) {
				TextEdit textEdit = new TextEdit();
				addNamePart(textEdit.getTextComponent(), DoubleFeature.FOCUS);
				textEdit.installSpellChecker();
				textEdit.setText(buffer.getText());
				container = new JPanel(new BorderLayout());
				addCenterComponent(textEdit.getComponent(), container);
				if (feature.equals("spellcheck")) {
					final JToolBar bar = northToolBar(container, BorderLayout.SOUTH);
					bar.add(new ManagerBase<Object>() {
						{
							createButton(bar, 
									ACCEPT_BUTTON_KEY, 
									new ActionListener() {
										public void actionPerformed(ActionEvent e) {
											EditPane[] editPanes = getEditPanesFor(buffer);
											if (editPanes.length > 0) {
												DoubleFeature df = doubleFeatures.get(editPanes[0]);
												JTextComponent textComponent = (JTextComponent) 
													findFirstComponent(df.getWidget(), 
															DoubleFeature.FOCUS, Constraint.AMONG);
												df.getTextArea2().setText(textComponent.getText());
											}
											removeFeature(buffer);
											bufferChange(buffer);
										}
									});
							createButton(bar, 
									REJECT_BUTTON_KEY, 
									new ActionListener() {
										public void actionPerformed(ActionEvent e) {
											removeFeature(buffer);
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
			installFocusClickListener(container);
		}
		return container;
	}
	
	@Override
	protected void deconstructFeature(String feature, JComponent container) {
		if (!feature.startsWith("data")) {
			installFocusClickListener(container, false);
			removeNamePart(container, DoubleFeature.FOCUS);
		}
	}
	
	public static void installDataManager(EditPane editPane) {
		final DoubleFeature doubleFeature = registerPane(editPane);
		if (doubleFeature != null) {
			Properties props = new Properties();
			if (DataConfiguration.dataProperties(editPane.getView(), props)) {
				String path = featureFile(getNextFeatureTemp(), "data-manage", props);
				jEdit.openFile(editPane, path);
			}
		}
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
		Settings.load(pathCombine(path, NAME + ".properties"));
		path = pathCombine(settingsDir, "jars", "sqlite4java");
		System.setProperty("sqlite4java.library.path", path);
	}
	
	// NOTE used in scripts
	public static void spellcheckSelection(View view) {
		final TextToggle jEditor = getJEditor();
		String text = jEditor.getSelectedText();
		if (nullOrEmpty(text)) {
			consoleMessage("berichtsheft.no-text-selection.message");
			return; 
		}
		final TextToggle textToggle = new TextToggle();
		textToggle.setText(text);
		Job<Void> takeThis = new Job<Void>() {
			public void perform(Void t, Object[] params) throws Exception {
				String text = textToggle.getText();
				jEditor.setSelectedText(text);
			}
		};
		textToggle.getTextEdit().installSpellChecker();
		Component component = textToggle.getUIComponent();
		component.setPreferredSize(new Dimension(400,300));
		new AlertDialog(view, 
				getProperty("berichtsheft.spellcheck-selection.title"), 
				"", 
				component, 
				JOptionPane.OK_CANCEL_OPTION,
				Behavior.MODAL, 
				loadIcon("manager.action-SPELLCHECK.icon"), 
				takeThis)
			.open();
		textToggle.getTextEdit().uninstallSpellChecker();
	}
	
	public static String getOptionProperty(String name) {
		return getProperty(BerichtsheftPlugin.OPTION_PREFIX + name);
	}
	
	public static void setOptionProperty(String name, String value) {
		setProperty(BerichtsheftPlugin.OPTION_PREFIX + name, value);
	}

	public static void print(Object... params) {
		if (insideJEdit())
			BerichtsheftShell.print(params);
		else
			com.applang.Util2.print(params);
	}
    
	public static void consoleMessage(String key, Object...params) {
		String format = getProperty(key);
		String msg = notNullOrEmpty(format) ? 
				String.format(format, params) : 
				String.format("<<< message text missing for key '%s'>>>", key) + com.applang.Util.toString(params);
		print(msg, NEWLINE);
	}
	
	public static TextToggle getJEditor() {
		return new TextToggle(jEdit.getActiveView());
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
	public static void checkAvailabilityOfTools() {
		print("Welcome...", NEWLINE);
		print("settings.dir", System.getProperty("settings.dir"), NEWLINE);
		print("jedit.settings.dir", getSettingsDirectory(), NEWLINE);
		print("temp.dir", tempPath(), NEWLINE);
		String[] tools = {"AWK_COMMAND", "ADB_COMMAND", "SQLITE_COMMAND"};
		for (int i = 0; i < tools.length; i++) {
			no_println(i, tools[i]);
			String cmd = getCommand(tools[i]);
			no_println(i, cmd);
			if (!fileExists(cmd)) {
				if (nullOrEmpty(runShellScript("which", "which " + cmd)))
					consoleMessage("berichtsheft.tool-missing.message", cmd);
			}
			else if (tools[i].startsWith("ADB")) {
				String msg = AndroidBridge.adbRestart();
				print(msg, NEWLINE);
			}
		}
	}

	public static String getCommand(String path) {
		String cmd = getProperty(path); 
		no_println(path, cmd);
		if (!cmd.startsWith(PATH_SEP) && cmd.contains(PATH_SEP)) {
			String sdk = getProperty("ANDROID_SDK");
			no_println("ANDROID_SDK", sdk);
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

	public static String jedit_test_settings() {
		String subDirName = BerichtsheftPlugin.NAME;
		File jarsDir = tempDir(true, subDirName, "settings", "jars");
		try {
			makeLinks(jarsDir, ".jedit/jars", 
					"BerichtsheftPlugin.jar",
					"sqlite4java.jar",
					"Console.jar",
					"ProjectViewer.jar",
					"InfoViewer.jar",
					"ErrorList.jar",
					"CommonControls.jar",
					"kappalayout.jar");
			makeLinks(jarsDir, ".jedit/jars", "sqlite4java");
			File settingsDir = tempDir(false, subDirName, "settings");
			makeLinks(settingsDir, ".jedit", "keymaps");
			makeLinks(settingsDir, ".jedit", "macros");
			makeLinks(settingsDir, ".jedit", "modes");
			settingsDir = tempDir(false, subDirName, "settings", "plugins");
			makeLinks(settingsDir, ".jedit/plugins", "berichtsheft");
			File commandoDir = tempDir(false, subDirName, "settings", "console");
			makeLinks(commandoDir, ".jedit/console", "commando");
			copyFile(
					new File(tempDir(false, subDirName, "settings", "plugins", "berichtsheft"), "jedit.properties"), 
					new File(tempDir(false, subDirName, "settings"), "properties"));
		} catch (Exception e) {
			Log.e(TAG, "test_jedit_settings", e);
		}
		return jarsDir.getParent();
	}
    		
 	private static DataView dataView = null;
	
	public static DataView getDataView() {
		if (dataView == null)
			dataView = new DataView();
		return dataView;
	}
}

package com.applang.berichtsheft.plugin;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;

import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;

import org.apache.commons.lang.StringUtils;
import org.gjt.sp.jedit.BeanShell;
import org.gjt.sp.jedit.EditAction;
import org.gjt.sp.jedit.EditBus;
import org.gjt.sp.jedit.EditPlugin;
import org.gjt.sp.jedit.MiscUtilities;
import org.gjt.sp.jedit.View;
import org.gjt.sp.jedit.jEdit;
import org.gjt.sp.jedit.EditBus.EBHandler;
import org.gjt.sp.jedit.msg.ViewUpdate;

import com.applang.SwingUtil.Modality;
import com.applang.Util.Function;
import com.applang.Util.Job;
import com.applang.Util2.Task;

import static com.applang.Util.*;
import static com.applang.Util2.*;
import static com.applang.SwingUtil.*;

/**
 * The Berichtsheft plugin
 * 
 */
public class BerichtsheftPlugin extends EditPlugin {
	public static final String NAME = "berichtsheft";
	public static final String OPTION_PREFIX = join(".", "options", NAME, "");
	public static final String MENU = "plugin.com.applang.berichtsheft.plugin.BerichtsheftPlugin.menu";
	public static final String CMD_PATH = "/com/applang/berichtsheft/plugin/bsh/";
	
	public static String consoleDirectory;
	public static String userCommandDirectory;
	
	public static JEditTextEditor getTextEditor() {
		return new JEditTextEditor(jEdit.getActiveView());
	}

	public static void invokeAction(View view, String actionName) {
		EditAction action = jEdit.getAction(actionName);
		if (action != null)
			action.invoke(view);
	}

	public static File getTempFile(String name) {
		return tempFile(name, NAME);
	}

	public static String repeat(String string, int times) {
		return StringUtils.repeat(string, times);
	}
	
	public static String awkScript(String script) {
		String awk = jEdit.getProperty("AWK_COMMAND");
		return awk + " " + enclose("'", script);
	}
	
	public static String adbScript(String device, String script) {
		String adb = jEdit.getProperty("ADB_COMMAND");
		String cmd = "#!/bin/bash\n" + adb;
		if (notNullOrEmpty(device))
			cmd += " -s " + device;
		cmd += " " + script;
		return cmd;
	}
	
	public static String runShellScript(String name, String script) {
		String fileName = pathCombine(new String[]{tempPath(), name});
		String path = fileName + ".sh";
		contentsToFile(new File(path), script + " &gt; \"" + fileName + "\"");
		Process proc = null;
		try {
			proc = Runtime.getRuntime().exec( "chmod a+x " + path + " ; " + path);
			proc.waitFor();
		} catch (Exception e) {
			if (proc != null)
				proc.destroy();
		}
		return contentsFromFile(new File(fileName)).trim();
	}
	
	public static String[] deviceInfo(String device, boolean onlyDirs, String dir) {
		String script, response;
		String[] array;
		if (notNullOrEmpty(device)) {
			script = "gsub(/[ \\t\\r\\n\\f\\v\\b]+$/, \"\", $NF) ;";
			script += " m = match($0, /d/) ;";
			script += " if (m == 1) print $NF\"/\"";
			if (!onlyDirs)
				script += " ; else print $NF";
			script = BerichtsheftPlugin.awkScript("{" + script + "}");
			script = BerichtsheftPlugin.adbScript(device, "shell ls -l " + dir + " | " + script);
			response = runShellScript("ls", script);
			ValList list = split(response, NEWLINE_REGEX);
			list.add(0, "." + BerichtsheftPlugin.repeat(" ", 30));
			MatchResult[] mr = findAllIn(dir, Pattern.compile("/"));
			if (mr.length > 2)
				list.add(0, "..");
			array = toStrings(list);
		}
		else {
			script = BerichtsheftPlugin.awkScript("NR &gt; 1 {print $1}");
			script = BerichtsheftPlugin.adbScript(null, "devices | " + script);
			response = runShellScript("devices", script);
			array = toStrings(split(response, NEWLINE_REGEX));
		}
		return array;
	}
	
	public static String promptList(String title, String contents) {
		String[] array = toStrings(split(contents, NEWLINE_REGEX));
		return com.applang.berichtsheft.BerichtsheftApp.prompt(3, "", title, array);
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static String chooseFileFromSdcard(View view, boolean onlyDirs, String dir) {
		String contents = deviceInfo(null, false, null);
		String[] devices = toStrings(split(contents, NEWLINE_REGEX));
		if (!isAvailable(0, devices) || !notNullOrEmpty(devices[0]))
			return null;
		
		String title = "Android file";
		String[] labels = strings("Device", null);
		final JList[] lists = new JList[]{new JList(devices), new JList()};
		JPanel panel = new JPanel();
		panel.setLayout(new GridLayout(1, 2, 2, 2));
		for (int i = 0; i < 2; i++) {
			JPanel pnl = new JPanel(new BorderLayout());
			pnl.add(new JLabel(labels[i]), BorderLayout.NORTH);
			lists[i].setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			lists[i].addMouseListener(new MouseAdapter() {
				public void mouseClicked(MouseEvent ev) {
					JList list = (JList) ev.getSource();
					Object item = list.getSelectedValue();
					boolean files = list.equals(lists[1]);
					if (files) {
						
					}
					else if (item != null) {
						String response = deviceInfo(item.toString(), false, "/sdcard/");
					}
					final int which = list.getSelectedIndex();
					list.setSelectedIndex(which);
					switch (ev.getClickCount()) {
					case 1:
						new Task<Void>(null, new Job<Void>() {
							public void perform(Void t,	Object[] params) throws Exception {
								listener.onClick(dialog, which);
							}
						}, 500).execute();
						break;
					case 2:
						listener.onClick(dialog, -which - 1);
						break;
					}
				}
			});
			pnl.add(new JScrollPane(lists[i]), BorderLayout.CENTER);
			panel.add(pnl);
		}
		dialogResult = showOptionDialog(view, 
			objects(panel), 
			title, 
			JOptionPane.OK_CANCEL_OPTION,
			JOptionPane.PLAIN_MESSAGE,
    		null,
    		null, null
		);
		if (JOptionPane.OK_OPTION == dialogResult) {
			 list.getSelectedValue();
		}
		if (device != null) {
			if (!notNullOrEmpty(dir))
				dir = "/sdcard/";
			else if (!dir.endsWith("/"))
				dir += "/";
			do {
				response = promptFiles(device, onlyDirs, dir, response);
				boolean doubleClick = dialogResult > -1;
				if (response == null)
					break;
				else if (response.equals("..")) {
					String regex = "[^/]+/$";
					if (!doubleClick)
						response = findFirstIn(dir, Pattern.compile(regex)).group();
					dir = dir.replaceFirst(regex, "");
				}
				else if (response.trim().equals(".")) {
					response = "";
				}
				else if (response.endsWith("/")) {
					dir += response;
					response = "";
				}
				if (doubleClick) {
					response = dir + response;
					break;
				}
			} while (true);
			if (notNullOrEmpty(response)) {
				return response;
			}
		}
		return null;
	}
	
	@Override
	public void start() {
		BeanShell.getNameSpace().addCommandPath(CMD_PATH, getClass());
		String settingsDir = jEdit.getSettingsDirectory();
		if (settingsDir != null) {
			consoleDirectory = MiscUtilities.constructPath(settingsDir, "console");
			userCommandDirectory = MiscUtilities.constructPath(consoleDirectory, "commando");
			File file = new File(userCommandDirectory);
			if (!file.exists())
				file.mkdirs();
		}
		else 
			settingsDir = tempPath();
		File dir = fileOf(settingsDir, "plugins", NAME);
		dir.mkdirs();
		System.setProperty("settings.dir", dir.getPath());
		Settings.load();
		
		BerichtsheftToolBar.init();
		
		EditBus.addToBus(this);
	}
	
	@Override
	public void stop() {
		EditBus.removeFromBus(this);
		
		BeanShell.getNameSpace().removeCommandPath(CMD_PATH, getClass());
		BerichtsheftToolBar.remove();
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
}

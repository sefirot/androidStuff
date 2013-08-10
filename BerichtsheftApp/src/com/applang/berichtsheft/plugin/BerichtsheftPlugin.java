package com.applang.berichtsheft.plugin;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.StringWriter;
import java.io.Writer;
import java.util.List;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListModel;
import javax.swing.ListSelectionModel;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;

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

	public static void loadSettings() {
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
	}
	
	public static String getProperty(String name)
	{
		if (underTest) {
			if ("AWK_COMMAND".equals(name))
				return getSetting(name, "awk");
			else if ("ADB_COMMAND".equals(name))
				return getSetting(name, "/home/lotharla/android-sdk-linux/platform-tools/adb");
			else
				return getSetting(name, "");
		}
		return jEdit.getProperty(name);
	}
	
	public static void setProperty(String name, String value)
	{
		if (underTest) {
			putSetting(name, value);
			return;
		}
		jEdit.setProperty(name, value);
	}
	
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

	public static String awkCommand(String part) {
		String cmd = getProperty("AWK_COMMAND"); 
		return cmd + " " + enclose("'", part);
	}
	
	public static String adbScript(String device, String part) {
		String cmd = getProperty("ADB_COMMAND"); 
		if (notNullOrEmpty(device))
			cmd += " -s " + device;
		cmd += " " + part;
		return cmd;
	}
	
	public static Object[] deviceInfo(String device, Object...params) {
		String dir = paramString(null, 0, params);
		boolean onlyDirs = param(false, 1, params);
		String script, response;
		Object[] array;
		if (dir != null) {
			script = "gsub(/[ \\t\\r\\n\\f\\v\\b]+$/, \"\", $NF) ;";
			script += " m = match($0, /d/) ;";
			script += " if (m == 1) print $NF\"/\"";
			if (!onlyDirs)
				script += " ; else print $NF";
			script = BerichtsheftPlugin.awkCommand("{" + script + "}");
			script = BerichtsheftPlugin.adbScript(device, "shell ls -l \"" + dir + "\" | " + script);
			response = runShellScript("ls", script);
			ValList list = split(response, NEWLINE_REGEX);
			list.add(0, "." + BerichtsheftPlugin.repeat(" ", 30));
			MatchResult[] mr = findAllIn(dir, Pattern.compile("/"));
			if (mr.length > 2)
				list.add(0, "..");
			array = sorted(list).toArray();
		}
		else {
			script = BerichtsheftPlugin.awkCommand("NR > 1 {print $1}");
			script = BerichtsheftPlugin.adbScript(null, "devices | " + script);
			response = runShellScript("dev", script);
			array = split(response, NEWLINE_REGEX).toArray();
		}
		return array;
	}
	
	public static ValList splitAndroidFileName(String androidFileName) {
		return split(androidFileName, "\\|");
	}
	
	public static String joinAndroidFileName(Object...parts) {
		return strip(false, join("|", parts), "|");
	}
	
	public static String buildAdbCommand(String oper, String androidFileName, String fileName) {
		if (oper.endsWith("mkdir")) 
			oper = "shell mkdir";
		else if (oper.endsWith("rmdir")) 
			oper = "shell rmdir";
		else if (oper.endsWith("rm")) 
			oper = "shell rm";
		else if (oper.endsWith("-r")) 
			oper = "shell rm -r";
		Object device = "";
		ValList parts = BerichtsheftPlugin.splitAndroidFileName(androidFileName);
		if (parts.size() > 1) {
			device = parts.get(0);
			parts.set(0, oper);
		}
		else {
			parts.add(0, oper);
		}
		parts.set(1, enclose("\"", parts.get(1).toString()));
		fileName = enclose("\"", fileName);
		if ("push".equals(oper))
			parts.add(1, fileName);
		else if ("pull".equals(oper))
			parts.add(2, fileName);
		String cmd = join(" ", parts.toArray()) + " 2>&1";
		return BerichtsheftPlugin.adbScript(device.toString(), cmd);
	}
	
	@SuppressWarnings("rawtypes")
	public static boolean deviceOperation(String oper, String device, String dir, String name, Function<List> fileLister) {
		boolean isDirectory = name.endsWith("/");
		if ("mkdir".equals(oper)) {
			if (!isDirectory) {
				message("need name of a directory here (trailing '/')");
				return false;
			}
		}
		else if ("rm".equals(oper)) {
			if (isDirectory) 
				oper = "rm -r";
		}
		String cmd = buildAdbCommand(oper, joinAndroidFileName(device, dir + name), "");
		String response = runShellScript("cmd", cmd);
		List files = fileLister.apply(deviceInfo(device, dir));
		if (oper.startsWith("mk") && files.contains(name))
			response = null;
		else if (oper.startsWith("rm") && !files.contains(name)) 
			response = null;
		if (notNullOrEmpty(response))
			message(response);
		return response == null;
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static String chooseFileFromSdcard(final View view, final boolean onlyDirs, final String androidFileName) {
		final Object[] devices = deviceInfo(null);
		if (!isAvailable(0, devices) || !notNullOrEmpty(devices[0]))
			return null;
		
		final String title = "Android file";
		final String sd = "/sdcard/";
		final JTextField itemField = new JTextField();
		final JLabel[] labels = new JLabel[]{new JLabel("Device"), new JLabel("")};
		final JList[] lists = new JList[]{new JList(devices), new JList()};
		final Function<List> fileLister = new Function<List>() {
			public List apply(Object... params) {
				List<Object> list = list(params);
				DefaultListModel model = defaultListModel(list); 
				lists[1].setModel(model);
				return list;
			}
		};
		final Function<Boolean> highLiter = new Function<Boolean>() {
			public Boolean apply(Object... params) {
				Object item = param(null, 0, params);
				JList list = param(null, 1, params);
				ListModel model = list.getModel();
				for (int i = 0; i < model.getSize(); i++)
					if (model.getElementAt(i).equals(item)) {
						list.setSelectedIndex(i);
						list.ensureIndexIsVisible(i);
						return true;
					}
				return false;
			}
		};
		AncestorListener ancestorListener = new AncestorListener() {
			public void ancestorRemoved(AncestorEvent event) {
			}
			public void ancestorMoved(AncestorEvent event) {
			}
			public void ancestorAdded(AncestorEvent event) {
				if (androidFileName.contains("|")) {
					ValList parts = splitAndroidFileName(androidFileName);
					String device = parts.get(0).toString();
					File file = new File(parts.get(1).toString());
					int index = list(devices).indexOf(device);
					if (index > -1) {
						lists[0].setSelectedValue(devices[index], true);
						String dir = file.getParent() + "/";
						labels[1].setText(dir);
						fileLister.apply(deviceInfo(device, dir, onlyDirs));
						String name = file.getName();
						if (!highLiter.apply(name, lists[1]))
							highLiter.apply(name + "/", lists[1]);
						itemField.setText(name);
					}
				}
			}
		};
		final JPanel panel = new JPanel() {
			@Override
			public void add(Component comp, Object constraints) {
				GridBagConstraints gbc = (GridBagConstraints) constraints;
				Writer writer = format(new StringWriter(), "[");
				writer = formatAssociation(writer, "gridx", gbc.gridx, 0);
				writer = formatAssociation(writer, "gridy", gbc.gridy, 1);
				writer = formatAssociation(writer, "gridwidth", gbc.gridwidth, 1);
				writer = formatAssociation(writer, "gridheight", gbc.gridheight, 1);
				writer = formatAssociation(writer, "anchor", gbc.anchor, 1);
				noprintln(format(writer, "]").toString());
				super.add(comp, constraints);
			}
		};
		MouseAdapter clickListener = new MouseAdapter() {
			public void mouseClicked(MouseEvent ev) {
				boolean doubleClick = ev.getClickCount() == 2;
				JList list = (JList) ev.getSource();
				String dir = null, device = null;
				String item = (String) list.getSelectedValue();
				if (list.equals(lists[1])) {
					dir = labels[1].getText();
					if (doubleClick) {
						if (item.equals("..")) {
							String regex = "[^/]+/$";
							item = findFirstIn(dir, Pattern.compile(regex))
									.group();
							dir = dir.replaceFirst(regex, "");
						} else if (item.endsWith("/")) {
							dir += item;
							item = null;
						}
					}
					device = stringValueOf(lists[0].getSelectedValue());
					if (item != null && item.trim().equals("."))
						item = null;
				}
				else if (item != null) {
					dir = sd;
					device = item;
					item = "";
				}
				labels[1].setText(dir);
				fileLister.apply(deviceInfo(device, dir, onlyDirs));
				highLiter.apply(item, list);
				itemField.setText(item);
			}
		};
		panel.setLayout(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();
		for (int i = 0; i < lists.length; i++) {
			gbc.fill = GridBagConstraints.HORIZONTAL;
			gbc.gridx = i;
			gbc.gridy = 0;
			gbc.gridheight = 1;
			switch (i) {
			case 0:
				panel.add(itemField, gbc);
				gbc.gridy += 1;
				break;
			case 1:
				JPanel p = new JPanel();
				JButton[] buttons = new JButton[2];
				buttons[0] = new JButton("add");
				buttons[0].addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						String name = stringValueOf(itemField.getText());
						if (name.length() > 0) {
							String dir = stringValueOf(labels[1].getText());
							String device = stringValueOf(lists[0].getSelectedValue());
							if (deviceOperation("mkdir", device, dir, name, fileLister))
								highLiter.apply(name, lists[1]);
						}
					}
				});
				p.add(buttons[0]);
				buttons[1] = new JButton("remove");
				buttons[1].addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						String name = stringValueOf(itemField.getText());
						if (name.length() > 0) {
							String dir = stringValueOf(labels[1].getText());
							String quest = "Do you really want to remove '%s'";
							if (name.endsWith("/"))
								quest += "\nand all files within this directory";
							if (question(String.format(quest, dir + name))) {
								String device = stringValueOf(lists[0].getSelectedValue());
								if (deviceOperation("rm", device, dir, name, fileLister))
									itemField.setText("");
							}
						}
					}
				});
				p.add(buttons[1]);
				panel.add(p, gbc);
				gbc.gridy += 1;
				break;
			}
			panel.add(labels[i], gbc);
			gbc.fill = GridBagConstraints.BOTH;
			gbc.gridy += 1;
			gbc.gridheight = 1;
			lists[i].setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			lists[i].addMouseListener(clickListener);
			panel.add(new JScrollPane(lists[i]), gbc);
		}
		int dialogResult = showResizableDialog(panel, ancestorListener, new Function<Integer>() {
			public Integer apply(Object...params) {
				return showOptionDialog(view, params[0], title, 
						JOptionPane.OK_CANCEL_OPTION + Modality.MODAL, 
						JOptionPane.PLAIN_MESSAGE, 
						null, 
						null, null);
			}
		});
		if (JOptionPane.OK_OPTION == dialogResult) {
			String device = stringValueOf(lists[0].getSelectedValue());
			String path = stringValueOf(labels[1].getText());
			String name = stringValueOf(lists[1].getSelectedValue());
			return joinAndroidFileName(device, path + name);
		}
		return null;
	}

	@Override
	public void start() {
		loadSettings();
		BeanShell.getNameSpace().addCommandPath(CMD_PATH, getClass());
		
		BerichtsheftToolBar.init();
		
		EditBus.addToBus(this);
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
}

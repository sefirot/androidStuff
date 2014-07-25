package com.applang.components;

import static com.applang.Util.*;
import static com.applang.Util2.*;
import static com.applang.SwingUtil.*;
import static com.applang.PluginUtils.*;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
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
import org.gjt.sp.jedit.View;

import com.applang.berichtsheft.BerichtsheftApp;
import com.applang.berichtsheft.plugin.BerichtsheftPlugin;

public class AndroidBridge
{
	private static final int behavior = Behavior.MODAL;

	public static void main(String...args) {
		BerichtsheftApp.loadSettings();
		println(chooseFileFromSdcard(null, false, null));
		System.exit(0);
	}
	
	// NOTE used in scripts
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static String chooseFileFromSdcard(final View view, final boolean onlyDirs, final String androidFileName) {
		final Object[] devices = deviceInfo(null);
		if (notAvailable(0, devices) || nullOrEmpty(devices[0])) {
			BerichtsheftPlugin.consoleMessage("berichtsheft.android-devices.message");
			return null;
		}
		final String title = "Android file";
		final String sd = "/sdcard/";
		final JTextField itemField = new JTextField();
		final JLabel[] labels = new JLabel[]{new JLabel("Device"), new JLabel("")};
		final JList[] lists = new JList[]{new JList(devices), new JList()};
		final Function<List> fileLister = new Function<List>() {
			public List apply(Object... params) {
				List<Object> list = asList(params);
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
				if (notNullOrEmpty(androidFileName) && androidFileName.contains("|")) {
					ValList parts = splitAndroidFileName(androidFileName);
					String device = parts.get(0).toString();
					File file = new File(parts.get(1).toString());
					int index = asList(devices).indexOf(device);
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
				Writer writer = write(null, "[");
				writer = write_assoc(writer, "gridx", gbc.gridx);
				writer = write_assoc(writer, "gridy", gbc.gridy, 1);
				writer = write_assoc(writer, "gridwidth", gbc.gridwidth, 1);
				writer = write_assoc(writer, "gridheight", gbc.gridheight, 1);
				writer = write_assoc(writer, "anchor", gbc.anchor, 1);
				no_println(write(writer, "]").toString());
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
				JScrollPane scrollPane = new JScrollPane((Component) params[0]);
				return showOptionDialog(view, scrollPane, title, 
						JOptionPane.OK_CANCEL_OPTION + behavior, 
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

	@SuppressWarnings("rawtypes")
	public static boolean deviceOperation(String oper, String device, String dir, String name, Function<List> fileLister) {
		boolean isDirectory = name.endsWith("/");
		if ("mkdir".equals(oper)) {
			if (!isDirectory) {
				message(getProperty("berichtsheft.android-sdcard-directory.message"));
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

	public static Object[] deviceInfo(String device, Object...params) {
		String dir = param_String(null, 0, params);
		boolean onlyDirs = param(false, 1, params);
		String script, response;
		Object[] array;
		if (dir != null) {
			script = "gsub(/[ \\t\\r\\n\\f\\v\\b]+$/, \"\", $NF) ;";
			script += " m = match($0, /d/) ;";
			script += " if (m == 1) print $NF\"/\"";
			if (!onlyDirs)
				script += " ; else print $NF";
			script = awkCommand("{" + script + "}");
			script = adbScript(device, "shell ls -l \"" + dir + "\" | " + script);
			response = runShellScript("ls", script);
			ValList list = split(response, NEWLINE_REGEX);
			list.add(0, "." + repeat(" ", 30));
			MatchResult[] mr = findAllIn(dir, Pattern.compile("/"));
			if (mr.length > 2)
				list.add(0, "..");
			array = sortedSet(list).toArray();
		}
		else {
			script = awkCommand("NR > 1 {print $1}");
			script = adbScript(null, "devices | " + script);
			response = runShellScript("dev", script);
			array = split(response, NEWLINE_REGEX).toArray();
		}
		return array;
	}

	public static ValList splitAndroidFileName(String androidFileName) {
		ValList parts = split(androidFileName, GLUE_REGEX, 2);
		if (parts.size() < 2)
			parts.add(0, "");
		return parts;
	}

	public static String joinAndroidFileName(Object...parts) {
		return strip(Constraint.END, join(GLUE, parts), GLUE);
	}

	public static String getAdbCommand() {
		return BerichtsheftPlugin.getCommand("ADB_COMMAND");
	}

	public static String adbScript(String device, String part) {
		String cmd = getAdbCommand(); 
		if (notNullOrEmpty(device))
			cmd += " -s " + device;
		cmd += " " + part;
		return cmd;
	}

	public static String adbRestart() {
		String[] commands = strings(
				adbScript(null, "kill-server"), 
				adbScript(null, "start-server"), 
				adbScript(null, "version") 
		);
		return runShellScript("cmd", join(NEWLINE, commands));
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
		else if (!oper.startsWith("pu")) 
			oper = "version";
		Object device = null;
		ValList parts = splitAndroidFileName(androidFileName);
		if (parts.size() > 1) {
			device = parts.get(0);
			parts.set(0, oper);
		}
		else {
			parts.sizeAtLeast(2);
			parts.set(0, oper);
		}
		parts.set(1, enclose("\"", stringValueOf(parts.get(1))));
		fileName = enclose("\"", fileName);
		if ("push".equals(oper))
			parts.add(1, fileName);
		else if ("pull".equals(oper))
			parts.add(2, fileName);
		String cmd = join(" ", parts.toArray());
		return adbScript(stringValueOf(device), cmd);
	}

	public static String awkCommand(String part) {
		String cmd = getProperty("AWK_COMMAND"); 
		return cmd + " " + enclose("'", part);
	}

	public static String repeat(String string, int times) {
		return StringUtils.repeat(string, times);
	}

}

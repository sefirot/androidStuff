package com.applang.berichtsheft.plugin;

import static com.applang.Util.*;
import static com.applang.Util2.*;
import static com.applang.SwingUtil.*;
import static com.applang.PluginUtils.*;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.gjt.sp.jedit.AbstractOptionPane;
import org.gjt.sp.jedit.GUIUtilities;
import org.gjt.sp.jedit.gui.FontSelector;

import com.applang.berichtsheft.BerichtsheftApp;

public class BerichtsheftOptionPane extends AbstractOptionPane implements ActionListener
{
	public static ValMap separators, decorations;
	static {
		separators = vmap();
		separators.put("none", strings("",""));
		separators.put("newline", strings(NEWLINE, NEWLINE_REGEX));
		separators.put("tab", strings(TAB, TAB_REGEX));
		separators.put("whitespace", strings(NEWLINE, WHITESPACE_REGEX));
		decorations = vmap();
		decorations.put("none", strings("",""));
		decorations.put("fold", FOLD_MARKER);
	}
	
	private FontSelector fontSelector;
	private JTextField[] commands = new JTextField[3];
	private JCheckBox showToolbar;
	@SuppressWarnings("rawtypes")
	private JComboBox fieldSeparatorSelector, fieldDecorationSelector;
	@SuppressWarnings("rawtypes")
	private JComboBox recordSeparatorSelector, recordDecorationSelector;
	
	public BerichtsheftOptionPane() {
		super(BerichtsheftPlugin.NAME);
		
		String[] strings = strings("AWK", "ADB", "SQLITE");
		for (int i = 0; i < commands.length; i++) {
			commands[i] = new JTextField(
					getProperty(strings[i] + "_COMMAND"));
			commands[i].setName(strings[i]);
		}
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public void _init() {
		if (!underTest) {
			fontSelector = new FontSelector(makeFont());
			addComponent(BerichtsheftPlugin.getOptionProperty("choose-font"),
					fontSelector);
			addComponent(Box.createVerticalStrut(10));
		}
		JPanel panel = new JPanel();
		panel.setLayout(new GridLayout(commands.length + 1, 2, 2, 2) );
		
		for (int i = 0; i < commands.length; i++) {
			String cmd = commands[i].getName();
			JPanel pathPanel = new JPanel(new BorderLayout(0, 0));
			if ("ADB".equals(cmd)) {
				final JTextField sdk = new JTextField(
						pathCombine(System.getProperty("user.home"), 
								getProperty("ANDROID_SDK")));
				pathPanel.add(sdk, BorderLayout.CENTER);
				JButton pick = new JButton(BerichtsheftPlugin.getOptionProperty("choose"));
				pick.addActionListener(new ActionListener( ) {
					public void actionPerformed(ActionEvent e) {
						String dirName = sdk.getText();
						String[] paths = underTest ? 
								chooseDirectoryNames(null, "", dirName) : 
								GUIUtilities.showVFSFileDialog(null, dirName, 
										org.gjt.sp.jedit.browser.VFSBrowser.CHOOSE_DIRECTORY_DIALOG, false);
						if (isAvailable(0, paths)) {
							sdk.setText(paths[0]);
							for (String cmd : strings("ADB", "SQLITE")) {
								JTextField tf = findFirstComponent(BerichtsheftOptionPane.this, cmd);
								if (tf != null) {
									String t = pathCombine(paths[0], getSetting(cmd + "_COMMAND", ""));
									tf.setText(t);
								}
							}
						}
					}
				});
				pathPanel.add(pick, BorderLayout.EAST);
				panel.add(new JLabel( BerichtsheftPlugin.getOptionProperty("sdk") ));
				panel.add( pathPanel );
				pathPanel = new JPanel(new BorderLayout(0, 0));
			}
			pathPanel.add(commands[i], BorderLayout.CENTER);
			JButton pickPath = new JButton(BerichtsheftPlugin.getOptionProperty("choose"));
			pickPath.setActionCommand(cmd);
			pickPath.addActionListener(this);
			pathPanel.add(pickPath, BorderLayout.EAST);
			panel.add(new JLabel( BerichtsheftPlugin.getOptionProperty(cmd.toLowerCase()) ));
			panel.add( pathPanel );
		}
		
		addComponent(surroundingBox(panel, BerichtsheftPlugin.getOptionProperty("tools.title")));
		
		addComponent( Box.createVerticalStrut(10) );
		
		panel = new JPanel();
		panel.setLayout(new GridLayout(4, 2, 2, 2) );
		
		fieldSeparatorSelector = new JComboBox(separators.keySet().toArray());
		fieldSeparatorSelector.setSelectedItem(
				BerichtsheftPlugin.getOptionProperty("field-separator"));
		panel.add(new JLabel( BerichtsheftPlugin.getOptionProperty("field-separator.title") ));
		panel.add( fieldSeparatorSelector );
		
		fieldDecorationSelector = new JComboBox(decorations.keySet().toArray());
		fieldDecorationSelector.setSelectedItem(
				BerichtsheftPlugin.getOptionProperty("field-decoration"));
		panel.add(new JLabel( BerichtsheftPlugin.getOptionProperty("field-decoration.title") ));
		panel.add( fieldDecorationSelector );
		
		recordSeparatorSelector = new JComboBox(separators.keySet().toArray());
		recordSeparatorSelector.setSelectedItem(
				BerichtsheftPlugin.getOptionProperty("record-separator"));
		panel.add(new JLabel( BerichtsheftPlugin.getOptionProperty("record-separator.title") ));
		panel.add( recordSeparatorSelector );
		
		recordDecorationSelector = new JComboBox(decorations.keySet().toArray());
		recordDecorationSelector.setSelectedItem(
				BerichtsheftPlugin.getOptionProperty("record-decoration"));
		panel.add(new JLabel( BerichtsheftPlugin.getOptionProperty("record-decoration.title") ));
		panel.add( recordDecorationSelector );
			
		addComponent(surroundingBox(panel, BerichtsheftPlugin.getOptionProperty("transport.title")));
		
		addComponent( Box.createVerticalStrut(10) );
		
		showToolbar = new JCheckBox(BerichtsheftPlugin.getOptionProperty("show-toolbar.title"), 
				"true".equals(BerichtsheftPlugin.getOptionProperty("show-toolbar")));
		addComponent(showToolbar);
	}
	
	@Override
	public void _save() {
		if (!underTest) {
			Font font = fontSelector.getFont();
			BerichtsheftPlugin.setOptionProperty("font", font.getFamily());
			BerichtsheftPlugin.setOptionProperty("fontsize", String.valueOf(font.getSize()));
			BerichtsheftPlugin.setOptionProperty("fontstyle", String.valueOf(font.getStyle()));
		}
		for (int i = 0; i < commands.length; i++) 
			setProperty(commands[i].getName() + "_COMMAND", commands[i].getText());
		BerichtsheftPlugin.setOptionProperty("field-separator", fieldSeparatorSelector.getSelectedItem().toString());
		BerichtsheftPlugin.setOptionProperty("field-decoration", fieldDecorationSelector.getSelectedItem().toString());
		BerichtsheftPlugin.setOptionProperty("record-separator", recordSeparatorSelector.getSelectedItem().toString());
		BerichtsheftPlugin.setOptionProperty("record-decoration", recordDecorationSelector.getSelectedItem().toString());
		BerichtsheftPlugin.setOptionProperty("show-toolbar", String.valueOf(showToolbar.isSelected()));
		saveSettings();
		BerichtsheftToolBar.init();
	}

	// end AbstractOptionPane implementation

	// begin ActionListener implementation
	public void actionPerformed(ActionEvent evt) {
		JTextField cmd = findFirstComponent(this, evt.getActionCommand());
		if (cmd != null) {
			String fileName = cmd.getText();
			String[] paths = underTest ? 
					chooseFileNames(true, null, "", fileName) : 
					GUIUtilities.showVFSFileDialog(null, fileName, JFileChooser.OPEN_DIALOG, false);
			if (isAvailable(0, paths)) {
				cmd.setText(paths[0]);
			}
		}
	}

	// helper method to get Font from plugin properties
	static public Font makeFont() {
		int style, size;
		String family = BerichtsheftPlugin.getOptionProperty("font");
		try {
			size = Integer.parseInt(BerichtsheftPlugin.getOptionProperty("fontsize"));
		} catch (NumberFormatException nf) {
			size = 14;
		}
		try {
			style = Integer.parseInt(BerichtsheftPlugin.getOptionProperty("fontstyle"));
		} catch (NumberFormatException nf) {
			style = Font.PLAIN;
		}
		return new Font(family, style, size);
	}

	public static void main(String...args) {
		BerichtsheftApp.loadSettings();
    	underTest = param("true", 0, args).equals("true");
		int modality = Behavior.MODAL;
		if (underTest)
			modality |= Behavior.EXIT_ON_CLOSE;
		BerichtsheftOptionPane opane = new BerichtsheftOptionPane();
		opane._init();
		showOptionDialog(null, 
				opane, 
				"Berichtsheft options", 
				JOptionPane.DEFAULT_OPTION | modality, 
				JOptionPane.PLAIN_MESSAGE, 
				null, 
				null, null);
	}
}

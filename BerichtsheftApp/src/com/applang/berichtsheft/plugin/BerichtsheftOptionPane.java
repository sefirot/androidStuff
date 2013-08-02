package com.applang.berichtsheft.plugin;

import static com.applang.Util.*;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.gjt.sp.jedit.AbstractOptionPane;
import org.gjt.sp.jedit.GUIUtilities;
import org.gjt.sp.jedit.jEdit;
import org.gjt.sp.jedit.gui.FontSelector;

public class BerichtsheftOptionPane extends AbstractOptionPane implements ActionListener
{
	public static ValMap separators, decorations;
	static {
		separators = new ValMap();
		separators.put("none", strings("",""));
		separators.put("newline", strings(NEWLINE, NEWLINE_REGEX));
		separators.put("tab", strings(TAB, TAB_REGEX));
		decorations = new ValMap();
		decorations.put("none", strings("","","",""));
		decorations.put("fold", arrayappend(FOLD_MARKER, FOLD_MARKER_REGEX));
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
		
		String[] strings = strings("AWK", "SQLITE", "ADB");
		for (int i = 0; i < commands.length; i++) {
			commands[i] = new JTextField(
					jEdit.getProperty(strings[i] + "_COMMAND"));
			commands[i].setName(strings[i]);
		}
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public void _init() {
		fontSelector = new FontSelector(makeFont());
		addComponent(jEdit.getProperty(BerichtsheftPlugin.OPTION_PREFIX + "choose-font"), fontSelector);
		
		addComponent( Box.createVerticalStrut(10) );

		JPanel panel = new JPanel();
		panel.setLayout(new GridLayout(commands.length, 2, 2, 2) );
		
		for (int i = 0; i < commands.length; i++) {
			String cmd = commands[i].getName();
			JButton pickPath = new JButton(
					jEdit.getProperty(BerichtsheftPlugin.OPTION_PREFIX + "choose"));
			pickPath.setActionCommand(cmd);
			pickPath.addActionListener(this);
			JPanel pathPanel = new JPanel(new BorderLayout(0, 0));
			pathPanel.add(commands[i], BorderLayout.CENTER);
			pathPanel.add(pickPath, BorderLayout.EAST);
			panel.add(new JLabel( jEdit.getProperty(BerichtsheftPlugin.OPTION_PREFIX + cmd.toLowerCase()) ));
			panel.add( pathPanel );
		}
		
		addComponent(surroundingBox(panel, "tools.title"));
		
		addComponent( Box.createVerticalStrut(10) );
		
		panel = new JPanel();
		panel.setLayout(new GridLayout(4, 2, 2, 2) );
		
		fieldSeparatorSelector = new JComboBox(separators.keySet().toArray());
		fieldSeparatorSelector.setSelectedItem(
				jEdit.getProperty(BerichtsheftPlugin.OPTION_PREFIX + "field-separator"));
		panel.add(new JLabel( jEdit.getProperty(BerichtsheftPlugin.OPTION_PREFIX + "field-separator.title") ));
		panel.add( fieldSeparatorSelector );
		
		fieldDecorationSelector = new JComboBox(decorations.keySet().toArray());
		fieldDecorationSelector.setSelectedItem(
				jEdit.getProperty(BerichtsheftPlugin.OPTION_PREFIX + "field-decoration"));
		panel.add(new JLabel( jEdit.getProperty(BerichtsheftPlugin.OPTION_PREFIX + "field-decoration.title") ));
		panel.add( fieldDecorationSelector );
		
		recordSeparatorSelector = new JComboBox(separators.keySet().toArray());
		recordSeparatorSelector.setSelectedItem(
				jEdit.getProperty(BerichtsheftPlugin.OPTION_PREFIX + "record-separator"));
		panel.add(new JLabel( jEdit.getProperty(BerichtsheftPlugin.OPTION_PREFIX + "record-separator.title") ));
		panel.add( recordSeparatorSelector );
		
		recordDecorationSelector = new JComboBox(decorations.keySet().toArray());
		recordDecorationSelector.setSelectedItem(
				jEdit.getProperty(BerichtsheftPlugin.OPTION_PREFIX + "record-decoration"));
		panel.add(new JLabel( jEdit.getProperty(BerichtsheftPlugin.OPTION_PREFIX + "record-decoration.title") ));
		panel.add( recordDecorationSelector );
			
		addComponent(surroundingBox(panel, "transport.title"));
		
		addComponent( Box.createVerticalStrut(10) );
		
		showToolbar = new JCheckBox(jEdit.getProperty(BerichtsheftPlugin.OPTION_PREFIX + "show-toolbar.title"), 
				jEdit.getBooleanProperty(BerichtsheftPlugin.OPTION_PREFIX + "show-toolbar"));
		addComponent(showToolbar);
	}
	
	private JPanel surroundingBox(JPanel content, String titleName) {
		JPanel box = new JPanel();
		box.setBorder(
			BorderFactory.createTitledBorder(
				BorderFactory.createEtchedBorder(),
				jEdit.getProperty(BerichtsheftPlugin.OPTION_PREFIX + titleName)
			)
		);
		box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS) );
		box.add( content );
		box.add( Box.createVerticalStrut(10) );
		return box;
	}

	@Override
	public void _save() {
		Font font = fontSelector.getFont();
		jEdit.setProperty(BerichtsheftPlugin.OPTION_PREFIX + "font", font.getFamily());
		for (int i = 0; i < commands.length; i++) 
			jEdit.setProperty(commands[i].getName() + "_COMMAND", commands[i].getText());
		jEdit.setProperty(BerichtsheftPlugin.OPTION_PREFIX + "fontsize", String.valueOf(font.getSize()));
		jEdit.setProperty(BerichtsheftPlugin.OPTION_PREFIX + "fontstyle", String.valueOf(font.getStyle()));
		jEdit.setProperty(BerichtsheftPlugin.OPTION_PREFIX + "field-separator", fieldSeparatorSelector.getSelectedItem().toString());
		jEdit.setProperty(BerichtsheftPlugin.OPTION_PREFIX + "field-decoration", fieldDecorationSelector.getSelectedItem().toString());
		jEdit.setProperty(BerichtsheftPlugin.OPTION_PREFIX + "record-separator", recordSeparatorSelector.getSelectedItem().toString());
		jEdit.setProperty(BerichtsheftPlugin.OPTION_PREFIX + "record-decoration", recordDecorationSelector.getSelectedItem().toString());
		jEdit.saveSettings();
		jEdit.setProperty(BerichtsheftPlugin.OPTION_PREFIX + "show-toolbar", String.valueOf(showToolbar.isSelected()));
		BerichtsheftToolBar.init();
	}

	// end AbstractOptionPane implementation

	// begin ActionListener implementation
	public void actionPerformed(ActionEvent evt) {
		String[] paths = GUIUtilities.showVFSFileDialog(null, null, JFileChooser.OPEN_DIALOG, false);
		if (paths != null) {
			for (int i = 0; i < commands.length; i++) {
				if (commands[i].getName().equals(evt.getActionCommand()))
					commands[i].setText(paths[0]);
			}
		}
	}

	// helper method to get Font from plugin properties
	static public Font makeFont() {
		int style, size;
		String family = jEdit.getProperty(BerichtsheftPlugin.OPTION_PREFIX
				+ "font");
		try {
			size = Integer
					.parseInt(jEdit
							.getProperty(BerichtsheftPlugin.OPTION_PREFIX
									+ "fontsize"));
		} catch (NumberFormatException nf) {
			size = 14;
		}
		try {
			style = Integer
					.parseInt(jEdit
							.getProperty(BerichtsheftPlugin.OPTION_PREFIX
									+ "fontstyle"));
		} catch (NumberFormatException nf) {
			style = Font.PLAIN;
		}
		return new Font(family, style, size);
	}

}

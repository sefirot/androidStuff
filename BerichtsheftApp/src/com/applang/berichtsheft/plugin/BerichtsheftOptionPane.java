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
		addComponent(BerichtsheftPlugin.getOptionProperty("choose-font"), fontSelector);
		
		addComponent( Box.createVerticalStrut(10) );

		JPanel panel = new JPanel();
		panel.setLayout(new GridLayout(commands.length, 2, 2, 2) );
		
		for (int i = 0; i < commands.length; i++) {
			String cmd = commands[i].getName();
			JButton pickPath = new JButton(BerichtsheftPlugin.getOptionProperty("choose"));
			pickPath.setActionCommand(cmd);
			pickPath.addActionListener(this);
			JPanel pathPanel = new JPanel(new BorderLayout(0, 0));
			pathPanel.add(commands[i], BorderLayout.CENTER);
			pathPanel.add(pickPath, BorderLayout.EAST);
			panel.add(new JLabel( BerichtsheftPlugin.getOptionProperty(cmd.toLowerCase()) ));
			panel.add( pathPanel );
		}
		
		addComponent(surroundingBox(panel, "tools.title"));
		
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
			
		addComponent(surroundingBox(panel, "transport.title"));
		
		addComponent( Box.createVerticalStrut(10) );
		
		showToolbar = new JCheckBox(BerichtsheftPlugin.getOptionProperty("show-toolbar.title"), 
				"true".equals(BerichtsheftPlugin.getOptionProperty("show-toolbar")));
		addComponent(showToolbar);
	}
	
	private JPanel surroundingBox(JPanel content, String titleName) {
		JPanel box = new JPanel();
		box.setBorder(
			BorderFactory.createTitledBorder(
				BorderFactory.createEtchedBorder(),
				BerichtsheftPlugin.getOptionProperty(titleName)
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
		BerichtsheftPlugin.setOptionProperty("font", font.getFamily());
		for (int i = 0; i < commands.length; i++) 
			jEdit.setProperty(commands[i].getName() + "_COMMAND", commands[i].getText());
		BerichtsheftPlugin.setOptionProperty("fontsize", String.valueOf(font.getSize()));
		BerichtsheftPlugin.setOptionProperty("fontstyle", String.valueOf(font.getStyle()));
		BerichtsheftPlugin.setOptionProperty("field-separator", fieldSeparatorSelector.getSelectedItem().toString());
		BerichtsheftPlugin.setOptionProperty("field-decoration", fieldDecorationSelector.getSelectedItem().toString());
		BerichtsheftPlugin.setOptionProperty("record-separator", recordSeparatorSelector.getSelectedItem().toString());
		BerichtsheftPlugin.setOptionProperty("record-decoration", recordDecorationSelector.getSelectedItem().toString());
		jEdit.saveSettings();
		BerichtsheftPlugin.setOptionProperty("show-toolbar", String.valueOf(showToolbar.isSelected()));
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

}

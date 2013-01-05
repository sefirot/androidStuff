package com.applang.berichtsheft.ui;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.gjt.sp.jedit.AbstractOptionPane;
import org.gjt.sp.jedit.GUIUtilities;
import org.gjt.sp.jedit.jEdit;
import org.gjt.sp.jedit.gui.FontSelector;

public class BerichtsheftOptionPane extends AbstractOptionPane implements
		ActionListener {
	private JCheckBox showToolbar;

	private JTextField pathName;

	private FontSelector font;

	public BerichtsheftOptionPane() {
		super(BerichtsheftPlugin.NAME);
	}

	public void _init() {
		showToolbar = new JCheckBox(jEdit.getProperty(BerichtsheftPlugin.OPTION_PREFIX + "show-toolbar.title"), 
				jEdit.getBooleanProperty(BerichtsheftPlugin.OPTION_PREFIX + "show-toolbar"));
		addComponent(showToolbar);

		pathName = new JTextField(jEdit
				.getProperty(BerichtsheftPlugin.OPTION_PREFIX + "filepath"));
		JButton pickPath = new JButton(jEdit
				.getProperty(BerichtsheftPlugin.OPTION_PREFIX + "choose-file"));
		pickPath.addActionListener(this);

		JPanel pathPanel = new JPanel(new BorderLayout(0, 0));
		pathPanel.add(pathName, BorderLayout.CENTER);
		pathPanel.add(pickPath, BorderLayout.EAST);

		addComponent(jEdit.getProperty(BerichtsheftPlugin.OPTION_PREFIX
				+ "file"), pathPanel);

		font = new FontSelector(makeFont());
		addComponent(jEdit.getProperty(BerichtsheftPlugin.OPTION_PREFIX
				+ "choose-font"), font);
	}

	public void _save() {
		jEdit.setProperty(BerichtsheftPlugin.OPTION_PREFIX + "filepath",
				pathName.getText());
		Font _font = font.getFont();
		jEdit.setProperty(BerichtsheftPlugin.OPTION_PREFIX + "font", _font.getFamily());
		jEdit.setProperty(BerichtsheftPlugin.OPTION_PREFIX + "fontsize", String.valueOf(_font.getSize()));
		jEdit.setProperty(BerichtsheftPlugin.OPTION_PREFIX + "fontstyle", String.valueOf(_font.getStyle()));
		jEdit.setProperty(BerichtsheftPlugin.OPTION_PREFIX + "show-toolbar",
				String.valueOf(showToolbar.isSelected()));
		jEdit.saveSettings();
		BerichtsheftToolBar.init();
	}

	// end AbstractOptionPane implementation

	// begin ActionListener implementation
	public void actionPerformed(ActionEvent evt) {
		String[] paths = GUIUtilities.showVFSFileDialog(null, null,
				JFileChooser.OPEN_DIALOG, false);
		if (paths != null) {
			pathName.setText(paths[0]);
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

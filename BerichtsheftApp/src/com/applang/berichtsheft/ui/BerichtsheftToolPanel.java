package com.applang.berichtsheft.ui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.AbstractButton;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.gjt.sp.jedit.GUIUtilities;
import org.gjt.sp.jedit.jEdit;
import org.gjt.sp.jedit.gui.RolloverButton;

//import com.applang.berichtsheft.ui.components.NotePicker;

public class BerichtsheftToolPanel extends JPanel {
	private Berichtsheft pad;

	private JLabel label = new JLabel();
//	private NotePicker notePicker;

	public BerichtsheftToolPanel(Berichtsheft qnpad) {
		setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
		pad = qnpad;

		Box labelBox = new Box(BoxLayout.Y_AXIS);
		labelBox.add(Box.createGlue());

		String title = jEdit.getProperty(BerichtsheftPlugin.OPTION_PREFIX + "choose-file.title");
//		notePicker = new NotePicker(pad.textArea, pad.getFilename(), title);
		propertiesChanged();

		labelBox.add(label);
//		labelBox.add(notePicker);
		labelBox.add(Box.createGlue());

		add(labelBox);

		add(Box.createGlue());

		add(makeCustomButton("berichtsheft.choose-file", new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				BerichtsheftToolPanel.this.pad.chooseFile();
			}
		}));
		add(makeCustomButton("berichtsheft.save-file", new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				BerichtsheftToolPanel.this.pad.saveFile();
			}
		}));
		add(makeCustomButton("berichtsheft.copy-to-buffer",
				new ActionListener() {
					public void actionPerformed(ActionEvent evt) {
						BerichtsheftToolPanel.this.pad.copyToBuffer();
					}
				}));
	}

	void propertiesChanged() {
		String dbName = pad.getFilename();
		label.setText(dbName);
//		if (notePicker.openConnection(dbName)) {
//			String text = notePicker.next();
//			pad.textArea.setText(text);
//		}
		boolean show_filepath = jEdit.getProperty(BerichtsheftPlugin.OPTION_PREFIX + "show-filepath").equals("true");
		label.setVisible(show_filepath);
	}

	private AbstractButton makeCustomButton(String name, ActionListener listener) {
		String toolTip = jEdit.getProperty(name.concat(".label"));
		AbstractButton b = new RolloverButton(GUIUtilities.loadIcon(jEdit
				.getProperty(name + ".icon")));
		if (listener != null) {
			b.addActionListener(listener);
			b.setEnabled(true);
		} else {
			b.setEnabled(false);
		}
		b.setToolTipText(toolTip);
		return b;
	}

}

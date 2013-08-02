package com.applang.berichtsheft.plugin;

import static com.applang.Util2.*;

import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;

import javax.swing.AbstractButton;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.gjt.sp.jedit.GUIUtilities;
import org.gjt.sp.jedit.jEdit;
import org.gjt.sp.jedit.gui.RolloverButton;

public class BerichtsheftToolPanel extends JPanel {
	private BerichtsheftDockable dockable;

	private JLabel label = new JLabel();

	public BerichtsheftToolPanel(BerichtsheftDockable dockable) {
		setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
		this.dockable = dockable;

		Box labelBox = new Box(BoxLayout.Y_AXIS);
		labelBox.add(Box.createGlue());

		propertiesChanged();

		labelBox.add(label);
		labelBox.add(Box.createGlue());

		add(labelBox);

		add(Box.createGlue());

		add(makeCustomButton("berichtsheft.choose-uri", new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				BerichtsheftToolPanel.this.dockable.chooseUri();
			}
		}, false));
		add(makeCustomButton("berichtsheft.update-uri", new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				BerichtsheftToolPanel.this.dockable.updateUri();
			}
		}, false));
		add(makeCustomButton("berichtsheft.transport-to-buffer", new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				BerichtsheftToolPanel.this.dockable.transportToBuffer();
			}
		}, false));
		add(makeCustomButton("berichtsheft.transport-from-buffer", new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				BerichtsheftToolPanel.this.dockable.transportFromBuffer();
			}
		}, true));
	}

	void propertiesChanged() {
		String dbName = dockable.getUriString();
		label.setText(dbName);
		boolean show = jEdit.getProperty(BerichtsheftPlugin.OPTION_PREFIX + "show-uri").equals("true");
		label.setVisible(show);
	}

	private AbstractButton makeCustomButton(String name, ActionListener listener, boolean flip) {
		String toolTip = jEdit.getProperty(name.concat(".label"));
		ImageIcon icon = (ImageIcon) GUIUtilities.loadIcon(jEdit.getProperty(name + ".icon"));
		if (flip) {
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
}

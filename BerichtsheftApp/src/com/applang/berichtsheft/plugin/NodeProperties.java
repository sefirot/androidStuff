package com.applang.berichtsheft.plugin;

import static com.applang.Util.*;

import android.net.Uri;

import com.applang.berichtsheft.BerichtsheftActivity;
import com.applang.berichtsheft.BerichtsheftApp;
import com.applang.components.DataView;

import java.awt.BorderLayout;
import java.awt.Component;
import java.io.File;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import projectviewer.gui.NodePropertyProvider;
import projectviewer.vpt.VPTNode;

public class NodeProperties implements NodePropertyProvider {

	@Override
	public boolean isNodeSupported(VPTNode node) {
		if (node.isFile()) {
//			File file = new File(node.getNodePath());
//			if (isSQLite(file)) {
//				node.setAllowsChildren(true);
//				node.insert(new projectviewer.vpt.VPTFile("table"), 0);
//			}
			return true;
		}
		return false;
	}

	@Override
	public String getTitle() {
		return "more info";
	}

	@Override
	public Component getComponent(VPTNode node) {
		JPanel panel = new JPanel(new BorderLayout());
		String path = node.getNodePath();
		JTextField tf = new JTextField(path);
		tf.setEditable(false);
		tf.setBorder(BorderFactory.createEmptyBorder());
		panel.add(tf, BorderLayout.NORTH);
		File file = new File(path);
		if (isSQLite(file)) {
			panel.add(new JLabel("SQLite database"));
			panel.add(DataView.dbTablesComponent(
					BerichtsheftActivity.getInstance(), 
					Uri.fromFile(file), 
					null), BorderLayout.CENTER);
		}
		return panel;
	}

}

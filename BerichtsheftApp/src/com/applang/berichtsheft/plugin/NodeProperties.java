package com.applang.berichtsheft.plugin;

import static com.applang.Util.*;

import android.net.Uri;

import com.applang.berichtsheft.BerichtsheftApp;
import com.applang.berichtsheft.components.DataView;

import java.awt.Component;
import java.io.File;

import javax.swing.JPanel;

import projectviewer.gui.NodePropertyProvider;
import projectviewer.vpt.VPTNode;

public class NodeProperties implements NodePropertyProvider {

	@Override
	public boolean isNodeSupported(VPTNode node) {
		if (node.isFile()) {
			File file = new File(node.getNodePath());
			if (isSQLite(file)) {
//				node.setAllowsChildren(true);
//				node.insert(new projectviewer.vpt.VPTFile("table"), 0);
				return true;
			}
		}
		return false;
	}

	@Override
	public String getTitle() {
		return "SQLite database";
	}

	@Override
	public Component getComponent(VPTNode node) {
		JPanel panel = new JPanel();
		File file = new File(node.getNodePath());
		panel.add(DataView.dbTablesComponent(BerichtsheftApp.getActivity(), Uri.fromFile(file), null));
		return panel;
	}

}

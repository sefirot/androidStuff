package com.applang.berichtsheft.components;

import static com.applang.SwingUtil.*;
import static com.applang.Util.*;

import java.awt.Component;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.AbstractButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextField;

import com.applang.berichtsheft.plugin.BerichtsheftPlugin;

@SuppressWarnings("rawtypes")
public abstract class ManagerBase<T extends Object> extends JComponent
{
	protected JComboBox[] comboBoxes;
	
	public JTextField comboEdit(int index) {
		return com.applang.SwingUtil.comboEdit(comboBoxes[index]);
	}
	
	public JLabel labelFor(Component c, String text, float alignmentX) {
		JLabel label = new JLabel(text);
		label.setName(text + "_Label");
		label.setAlignmentX(alignmentX);
		label.setLabelFor(c);
		return label;
	}
	
	private AbstractButton[] abuttons = new AbstractButton[2];
	
	protected boolean saveThis(boolean exists, Object item) {
		if (!exists || isDirty()) {
			setDirty(false);
			if (notNullOrEmpty(item)) {
				String string = exists ? "Save changes to '%s'" : "Save '%s'";
				return question(String.format(string, item), null,
						JOptionPane.YES_NO_OPTION);
			}
		}
		return false;
	}

	protected Boolean save(Object item, boolean refresh) {
		boolean exists = select(item) != null;
		if (saveThis(exists, item)) {
			if (exists) {
				updateItem(true, item);
				BerichtsheftPlugin.consoleMessage("berichtsheft.add.message.1", item);
			}
			else
				if (!addItem(refresh, item))
					BerichtsheftPlugin.consoleMessage("berichtsheft.add.message.2", item);
				else
					BerichtsheftPlugin.consoleMessage("berichtsheft.add.message.3", item);
			return exists;
		}
		return null;
	}
	
	protected boolean deleteThis(Object item) {
		String string = String.format("Delete '%s'", item);
		if (notNullOrEmpty(item) && question(string, null, JOptionPane.YES_NO_OPTION)) {
			setDirty(false);
			return true;
		}
		return false;
	}

	protected Object getItem() {
		return comboEdit(0).getText();
	}
	
	protected void installAddRemove(Container container, final String itemName) {
		container.add(BerichtsheftPlugin.makeCustomButton("berichtsheft.add", new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				Object item = getItem();
				save(item, true);
			}
		}, false));
		container.add(BerichtsheftPlugin.makeCustomButton("berichtsheft.remove", new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				Object item = getItem();
				if (select(item) == null) {
					BerichtsheftPlugin.consoleMessage("berichtsheft.remove.message.1", itemName);
					return;
				}
				if (deleteThis(item))
					if (!removeItem(item))
						BerichtsheftPlugin.consoleMessage("berichtsheft.remove.message.2", itemName);
					else
						BerichtsheftPlugin.consoleMessage("berichtsheft.remove.message.3", itemName, item);
			}
		}, false));
	}
	
	protected void installUpdate(Container container) {
		container.add(abuttons[0] = BerichtsheftPlugin.makeCustomButton("berichtsheft.update-text", 
				new ActionListener() {
					public void actionPerformed(ActionEvent evt) {
						updateItem(true);
					}
				}, 
				false));
		container.add(abuttons[1] = BerichtsheftPlugin.makeCustomButton("berichtsheft.erase-text", 
				new ActionListener() {
					public void actionPerformed(ActionEvent evt) {
						updateItem(false);
					}
				}, 
				false));
	}
	
	protected boolean isDirty() {
		return abuttons[0].isEnabled();
	}

	protected void setDirty(boolean dirty) {
		if (changeAllowed) 
			for (AbstractButton button : abuttons) 
				button.setEnabled(dirty);
	}
	
	protected boolean changeAllowed = false;

	protected void updateText(TextComponent textComponent, String text) {
		try {
			changeAllowed = false;
			textComponent.setText(text);
		} finally {
			changeAllowed = true;
		}
		setDirty(false);
	}

	protected abstract T select(Object...args);
	
	protected abstract void updateItem(boolean update, Object...args);
	
	protected abstract boolean addItem(boolean refresh, Object item);
	
	protected abstract boolean removeItem(Object item);
}

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

import org.w3c.dom.Element;

import com.applang.berichtsheft.plugin.BerichtsheftPlugin;

@SuppressWarnings("rawtypes")
public abstract class ManagerBase extends JComponent
{
	protected JComboBox[] comboBoxes;
	
	public JTextField comboEdit(int index) {
		return com.applang.SwingUtil.comboEdit(comboBoxes[index]);
	}
	
	public JLabel labelFor(Component c, String text, float alignmentX) {
		JLabel label = new JLabel(text);
		label.setAlignmentX(alignmentX);
		label.setLabelFor(c);
		return label;
	}
	
	private AbstractButton[] abuttons = new AbstractButton[2];
	
	protected boolean saveThis(String item) {
		if (isDirty() && notNullOrEmpty(item)) {
			setDirty(false);
			return question(String.format("Save changes to '%s'", item), null, JOptionPane.YES_NO_OPTION);
		}
		return false;
	}
	
	protected boolean deleteThis(String item) {
		if (notNullOrEmpty(item) && question(String.format("Delete '%s'", item), null, JOptionPane.YES_NO_OPTION)) {
			setDirty(false);
			return true;
		}
		return false;
	}
	
	protected void installAddRemove(Container container, final String itemName) {
		container.add(BerichtsheftPlugin.makeCustomButton("berichtsheft.add", new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				String item = comboEdit(0).getText();
				if (select(item) != null) {
					BerichtsheftPlugin.consoleMessage("berichtsheft.add-" + itemName + ".message.1");
					return;
				}
				if (!addItem(true, item))
					BerichtsheftPlugin.consoleMessage("berichtsheft.add-" + itemName + ".message.2");
				else
					BerichtsheftPlugin.consoleMessage("berichtsheft.add-" + itemName + ".message.3", item);
			}
		}, false));
		container.add(BerichtsheftPlugin.makeCustomButton("berichtsheft.remove", new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				String item = comboEdit(0).getText();
				if (select(item) == null) {
					BerichtsheftPlugin.consoleMessage("berichtsheft.remove-" + itemName + ".message.1");
					return;
				}
				if (!removeItem(item))
					BerichtsheftPlugin.consoleMessage("berichtsheft.remove-" + itemName + ".message.2");
				else
					BerichtsheftPlugin.consoleMessage("berichtsheft.remove-" + itemName + ".message.3", item);
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
		if (updateAllowed) 
			for (AbstractButton button : abuttons) 
				button.setEnabled(dirty);
	}
	
	protected boolean updateAllowed = true;

	protected void updateText(TextComponent textComponent, String text) {
		try {
			updateAllowed = false;
			textComponent.setText(text);
		} finally {
			updateAllowed = true;
		}
		setDirty(false);
	}

	protected void saveChanges(String item) {
		if (saveThis(item)) {
			if (select(item) != null)
				updateItem(true, item);
			else
				addItem(false, item);
		}
	}

	protected abstract Element select(String...params);
	
	protected abstract void updateItem(boolean update, Object...params);
	
	protected abstract boolean addItem(boolean refresh, String item);
	
	protected abstract boolean removeItem(String item);
}

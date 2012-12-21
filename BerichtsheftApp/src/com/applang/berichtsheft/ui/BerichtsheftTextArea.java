package com.applang.berichtsheft.ui;

import static ui.Util.*;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;

import javax.swing.JTextArea;
import javax.swing.KeyStroke;

import ui.Source;
import ui.Util.Settings;

public class BerichtsheftTextArea extends JTextArea {
	public BerichtsheftTextArea() {
		super();
		setLineWrap(true);
		setWrapStyleWord(true);
		setTabSize(4);
		setPreferredSize(new Dimension(100, 100));
	}
	
	public void createPopupMenu() {
		final String key = "Input";
		final Source src = Source.fileFilters.get(key);
        this.addMouseListener(newPopupAdapter(
        	new Object[] {"Load " + key.toLowerCase() + " from ...", new ActionListener() {
		        	public void actionPerformed(ActionEvent ae) {
		        		src.loadFile(BerichtsheftTextArea.this);
		        	}
		        }, "load", "Load contents into this pane"}, 
	        new Object[] {"Save " + key.toLowerCase() + " as ...", new ActionListener() {
		        	public void actionPerformed(ActionEvent ae) {
		        		src.saveFile(BerichtsheftTextArea.this);
		        	}
		        }, "save", "Save the contents of this pane"}, 
	        new Object[] {"-"}, 
	        new Object[] {"Change font ...", new ActionListener() {
		        	public void actionPerformed(ActionEvent ae) {
		        		if (changeFont(BerichtsheftTextArea.this))
						{
							Settings.put("font." + key.toLowerCase(), BerichtsheftTextArea.this.getFont());
						}
		        	}
		        }, "change", "Change the font for this pane", changeFont(null)}, 
	        new Object[] {"-"}, 
	        new Object[] {"Search...", this, "SEARCH", "Search the contents", true, 
		        KeyStroke.getKeyStroke(KeyEvent.VK_F, ActionEvent.CTRL_MASK)}
        ));
    }
}

package com.applang.berichtsheft.plugin;

import java.awt.BorderLayout;
import java.awt.Dimension;

import javax.swing.JPanel;

import org.gjt.sp.jedit.EBComponent;
import org.gjt.sp.jedit.EBMessage;
import org.gjt.sp.jedit.EditBus;
import org.gjt.sp.jedit.View;
import org.gjt.sp.jedit.gui.DefaultFocusComponent;
import org.gjt.sp.jedit.gui.DockableWindowManager;
import org.gjt.sp.jedit.msg.PropertiesChanged;

import com.applang.components.NotePicker;
import com.applang.components.TextToggle;

import static com.applang.SwingUtil.*;

public class NoteDockable extends JPanel implements EBComponent, DefaultFocusComponent
{
	private boolean floating;
	private TextToggle textToggle;
	private NotePicker notePicker;

	public NoteDockable(View view, String position) {
		super(new BorderLayout());
		this.floating = position.equals(DockableWindowManager.FLOATING);
		if (floating)
			this.setPreferredSize(new Dimension(500, 250));
		southStatusBar(this);
		textToggle = new TextToggle();
		textToggle.getTextEdit().installSpellChecker();
		add(textToggle.getUIComponent(), BorderLayout.CENTER);
		notePicker = new NotePicker(BerichtsheftPlugin.getDataView(), textToggle, view);
		add(notePicker, BorderLayout.NORTH);
	}

	@Override
	public void focusOnDefaultComponent() {
		textToggle.requestFocus();
	}

    @Override
	public void addNotify() {
		super.addNotify();
		EditBus.addToBus(this);
		notePicker.start();
	}
     
    @Override
	public void removeNotify() {
		super.removeNotify();
		EditBus.removeFromBus(this);
		notePicker.finish();
	}
    
	@Override
	public void handleMessage(EBMessage message) {
		if (message instanceof PropertiesChanged) {
			propertiesChanged();
		}
	}

	void propertiesChanged() {
		notePicker.refresh();
	}
	
}

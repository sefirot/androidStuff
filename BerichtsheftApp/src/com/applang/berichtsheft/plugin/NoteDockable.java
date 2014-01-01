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
import com.applang.components.TextEditor2;

import static com.applang.SwingUtil.*;

public class NoteDockable extends JPanel implements EBComponent, DefaultFocusComponent
{
	private boolean floating;
	private TextEditor2 textEditor2;
	private NotePicker notePicker;

	public NoteDockable(View view, String position) {
		super(new BorderLayout());
		this.floating = position.equals(DockableWindowManager.FLOATING);
		if (floating)
			this.setPreferredSize(new Dimension(500, 250));
		southStatusBar(this);
		textEditor2 = new TextEditor2();
		textEditor2.getTextEditor().installSpellChecker();
		add(textEditor2.getUIComponent(), BorderLayout.CENTER);
		notePicker = new NotePicker(BerichtsheftPlugin.getDataView(), textEditor2, view);
		add(notePicker, BorderLayout.NORTH);
	}

	@Override
	public void focusOnDefaultComponent() {
		textEditor2.requestFocus();
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

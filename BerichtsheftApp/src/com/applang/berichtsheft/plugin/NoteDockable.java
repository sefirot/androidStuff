package com.applang.berichtsheft.plugin;

import static com.applang.SwingUtil.*;

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

import com.applang.berichtsheft.components.NotePicker;
import com.applang.berichtsheft.components.TextEditor;

public class NoteDockable extends JPanel implements EBComponent, DefaultFocusComponent
{
	private View view;
	private boolean floating;
	private TextEditor textEditor;
	private NotePicker notePicker;

	public NoteDockable(View view, String position) {
		super(new BorderLayout());
		this.view = view;
		this.floating = position.equals(DockableWindowManager.FLOATING);
		if (floating)
			this.setPreferredSize(new Dimension(500, 250));
		southStatusBar(this);
		textEditor = new TextEditor();
		textEditor.installSpellChecker();
		add(textEditor.getUIComponent(), BorderLayout.CENTER);
		notePicker = new NotePicker(textEditor);
		add(notePicker, BorderLayout.NORTH);
		propertiesChanged();
	}

	@Override
	public void focusOnDefaultComponent() {
		textEditor.requestFocus();
	}

    @Override
	public void addNotify() {
		super.addNotify();
		EditBus.addToBus(this);
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

	private void propertiesChanged() {
		// TODO Auto-generated method stub
		
	}
	
}

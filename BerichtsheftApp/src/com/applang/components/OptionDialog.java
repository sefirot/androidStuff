package com.applang.components;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;

import org.gjt.sp.jedit.GUIUtilities;
import org.gjt.sp.jedit.View;

import com.applang.berichtsheft.plugin.BerichtsheftPlugin;

import android.app.Dialog;
import android.util.Log;

import static com.applang.Util.*;
import static com.applang.SwingUtil.*;

public class OptionDialog extends Dialog	
{
    private static final String TAG = OptionDialog.class.getSimpleName();

	public OptionDialog(View view, 
			String title, String caption, Object message, 
			int optionType, int behavior, 
			String iconPath, 
			Job<Void> followUp, Object...params)
	{
		super((Frame)view, Behavior.MODAL);
		setTitle(title);
		this.optionType = optionType;
		this.options = defaultOptions(optionType);
		this.followUp = followUp;
		this.params = params;
		Icon icon = optionType == JOptionPane.DEFAULT_OPTION ? 
				UIManager.getIcon("OptionPane.informationIcon") :
				UIManager.getIcon("OptionPane.questionIcon");
		if (notNullOrEmpty(iconPath))
			icon = BerichtsheftPlugin.loadIcon(iconPath);
		init(caption, icon, message);
	}
	
	int optionType = JOptionPane.OK_CANCEL_OPTION;
	ValList options;
	Job<Void> followUp;
	Object[] params;

	private void init(String caption, Icon icon, Object message)
	{
		JPanel content = new JPanel(new BorderLayout(12,12));
		content.setBorder(new EmptyBorder(12,12,12,12));
		setContentPane(content);

		Box iconBox = new Box(BoxLayout.Y_AXIS);
		iconBox.add(new JLabel(icon));
		iconBox.add(Box.createGlue());
		content.add(BorderLayout.WEST,iconBox);

		JPanel centerPanel = new JPanel(new BorderLayout(6,6));
		centerPanel.add(BorderLayout.NORTH,
			GUIUtilities.createMultilineLabel(caption));
		if (message instanceof Component) 
			centerPanel.add(BorderLayout.CENTER, (Component)message);
		else {
			JTextArea textArea = new JTextArea(10, 80);
			textArea.setText(stringValueOf(message));
			textArea.setLineWrap(true);
			textArea.setCaretPosition(0);
			centerPanel.add(BorderLayout.CENTER, new JScrollPane(textArea));
		}
		content.add(BorderLayout.CENTER,centerPanel);
		
		Box box = new Box(BoxLayout.X_AXIS);
		JButton btn = null;
		for (int i = 0; i < options.size() - 1; i++) {
			btn = addButton(i, box);
		}
		box.add(Box.createGlue());
		content.add(BorderLayout.SOUTH,box);

		switch (optionType) {
		case 5:
			dialogResult = 4;
			break;

		default:
			dialogResult = JOptionPane.CANCEL_OPTION;
			break;
		}

		if (btn != null)
			getRootPane().setDefaultButton(btn);

		pack();
		setLocationRelativeTo(getParent());
		setVisible(true);
	}
	
	private JButton addButton(int index, Box box) {
		box.add(Box.createGlue());
		JButton btn = new JButton(stringValueOf(options.get(index)));
		btn.addActionListener(new ActionHandler());
		box.add(btn);
		return btn;
	}

	@Override
	public void ok()
	{
		dialogResult = JOptionPane.OK_OPTION;
		if (followUp != null)
			try {
				followUp.perform(null, params);
			} catch (Exception e) {
				Log.e(TAG, "ok", e);
			}
		cancel();
	}

	@Override
	public void cancel()
	{
		dispose();
	}

	class ActionHandler implements ActionListener
	{
		public void actionPerformed(ActionEvent evt)
		{
			String cmd = evt.getActionCommand();
			if (stringValueOf(options.get(0)).equals(cmd)) {
				ok();
				return;
			}
			dialogResult = options.indexOf(cmd);
			if (optionType == 5 && dialogResult == 1) {
				ok();
				dialogResult = 1;
				return;
			}
			cancel();
		}
	}

	public int getResult() {
		return dialogResult;
	}
}

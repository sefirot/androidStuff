package com.applang.berichtsheft.plugin;

import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

import javax.swing.BoxLayout;
import javax.swing.JToolBar;

import org.gjt.sp.jedit.ActionSet;
import org.gjt.sp.jedit.EditAction;
import org.gjt.sp.jedit.EditBus;
import org.gjt.sp.jedit.View;
import org.gjt.sp.jedit.jEdit;
import org.gjt.sp.jedit.EditBus.EBHandler;
import org.gjt.sp.jedit.msg.DynamicMenuChanged;

import console.ConsolePlugin;
import console.commando.CommandoButton;
import console.commando.CommandoCommand;
import console.commando.CommandoDialog;

import com.applang.components.ActionPanel;

import static com.applang.Util.*;

public class BerichtsheftToolBar extends JToolBar
{
	public static void init() {
		remove();
		View views[]  = jEdit.getViews();

		for (int i=0; i<views.length; ++i) {
			create(views[i]);
		}
	}

	public static BerichtsheftToolBar create(View view) {
		BerichtsheftToolBar tb = null;
		if ("true".equals(BerichtsheftPlugin.getOptionProperty("show-toolbar"))) {
			tb = new BerichtsheftToolBar(view);
			view.addToolBar(tb);
			smToolBarMap.put(view, tb);
		}
		return tb;
	}

	public static void remove() {
		if (commands != null) {
			jEdit.removeActionSet(commands);
			commands.removeAllActions();
		}
		Iterator<View> itr = smToolBarMap.keySet().iterator();
		while (itr.hasNext())
		{
			View v = itr.next();
			if (v == null) continue;
			BerichtsheftToolBar tb = smToolBarMap.get(v);
			if (tb != null) {
				v.removeToolBar(tb);
			}
		}
		smToolBarMap.clear();
	}

	public static void remove(View v) {
		BerichtsheftToolBar tb = smToolBarMap.get(v);
		if (tb != null) {
			v.removeToolBar(tb);
			smToolBarMap.remove(v);
			tb.updateButtons(true);
		}
	}
	
	public static ActionSet commands = new ActionSet("Plugin: Berichtsheft - Commando Commands");

	static void scanDirectory(String directory)
	{
		if (directory != null)
		{
			File[] files = new File(directory).listFiles();
			if (files != null)
			{
				for (int i = 0; i < files.length; i++)
				{
					File file = files[i];
					String fileName = file.getAbsolutePath();
					if (!fileName.endsWith(".xml") || file.isHidden())
						continue;
					EditAction action = CommandoCommand.create(fileName);
					commands.addAction(action);
				}
			}
		}
	}

	/**
		A fix for keyboard bindings that are dynamically generated.
	*/
	static private void redoKeyboardBindings(ActionSet actionSet)
	/* Code duplication from jEdit.initKeyBindings() is bad, but
	   otherwise invoking 'rescan commando directory' will leave
	   old actions in the input handler
	*/
	{
		EditAction[] ea = actionSet.getActions();
		for (int i = 0; i < ea.length; ++i)
		{
			String shortcut1 = jEdit.getProperty(ea[i].getName() + ".shortcut");
			if (shortcut1 != null)
				jEdit.getInputHandler().addKeyBinding(shortcut1, ea[i]);

			String shortcut2 = jEdit.getProperty(ea[i].getName() + ".shortcut2");
			if (shortcut2 != null)
				jEdit.getInputHandler().addKeyBinding(shortcut2, ea[i]);
		}
	}

	public static String userCommandDirectory;

	public static void scanCommandoActions() {
		scanDirectory(userCommandDirectory);
		redoKeyboardBindings(commands);
		jEdit.addActionSet(commands);
	}
	
	private BerichtsheftToolBar(View dockable)
	{
		view = dockable;
		setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
		setFloatable(true);
		
//		scanCommandoActions();
		
		updateButtons(false);
	}

	public void addNotify() {
		super.addNotify();
		EditBus.addToBus(this);
	}

	public void removeNotify() {
		super.removeNotify();
		EditBus.removeFromBus(this);
	}

	@EBHandler
	public void handleMessage(DynamicMenuChanged msg)
	{
		if (BerichtsheftPlugin.MENU.equals(msg.getMenuName()))
			updateButtons(false);
	}
	
	private void updateButtons(boolean remove)
	{
		if (remove) {
			for (int i = 0; i < getComponentCount(); i++)
				if (getComponent(i) instanceof ActionPanel) 
					((ActionPanel)getComponent(i)).finish();
			return;
		}
		removeAll();
		if (!remove) {
//			addCommandoButtons();

//			add(BerichtsheftPlugin.makeCustomButton("berichtsheft.export-document", new ActionListener() {
//				public void actionPerformed(ActionEvent evt) {
//					jEdit.openTemporary(view, "", "Notes", true, null);
//					BufferSetManager.createUntitledBuffer();
//					BerichtsheftPlugin.installDoubleBuffer();
//				}
//			}, false));
			
//			NotePicker actionPanel = new NotePicker(BerichtsheftPlugin.getTextEditor());
//			actionPanel.joinContainer(this);
//			add(Box.createGlue());
		}
	}

	public void addCommandoButtons() {
		ActionSet allCommands = ConsolePlugin.getAllCommands();
		Set<String> names = sortedSet(asList(commands.getActionNames()));
		for (String name : names) {
			CommandoCommand command = (CommandoCommand) commands.getAction(name);
			CommandoButton button = new CommandoButton(command);
			button.setActionCommand(command.getName());
			button.addActionListener(actionHandler);
			button.setRequestFocusEnabled(false);
			button.setMargin(new Insets(1, 2, 1, 2));
			add(button);
			allCommands.addAction(command);
		}
	}

	ActionListener actionHandler = new ActionListener()
	{
		public void actionPerformed(ActionEvent evt)
		{
			String cmd = evt.getActionCommand();
			new CommandoDialog(view, cmd);
		}
	};

	private View view;

	/**
	 * For each view, we might add a toolbar.
	 * This map keeps track of what
	 * views had toolbars added to them.
	 */
	static HashMap<View, BerichtsheftToolBar> smToolBarMap = 
			new HashMap<View, BerichtsheftToolBar>();
}

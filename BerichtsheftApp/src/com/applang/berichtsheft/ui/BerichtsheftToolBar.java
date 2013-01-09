package com.applang.berichtsheft.ui;

//{{{ Imports

import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.Iterator;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JToolBar;

import org.gjt.sp.jedit.EditAction;
import org.gjt.sp.jedit.EditBus;
import org.gjt.sp.jedit.EditPane;
import org.gjt.sp.jedit.View;
import org.gjt.sp.jedit.jEdit;
import org.gjt.sp.jedit.EditBus.EBHandler;
import org.gjt.sp.jedit.msg.DynamicMenuChanged;
import org.gjt.sp.jedit.textarea.JEditTextArea;

import com.applang.berichtsheft.ui.components.NotePicker;

//}}}
//{{{ BerichtsheftToolBar class
public class BerichtsheftToolBar extends JToolBar
{

	// {{{ init()

	public static void init() {
		remove();
		View views[]  = jEdit.getViews();

		for (int i=0; i<views.length; ++i) {
			create(views[i]);
		}

	}

	public static BerichtsheftToolBar create(View view)
	{
		if (!jEdit.getBooleanProperty(BerichtsheftPlugin.OPTION_PREFIX + "show-toolbar"))
			return null;
		
		BerichtsheftToolBar tb = new BerichtsheftToolBar(view);
		view.addToolBar(tb);
		smToolBarMap.put(view, tb);
		return tb;
	}

	// }}}

	// {{{ remove()
	/** Remove the instance from the all views */
	public static void remove()
	{
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
	// }}}

	/** Remove the instance from the all views */
	public static void remove(View v)
	{
		BerichtsheftToolBar tb = smToolBarMap.get(v);
		if (tb != null) {
			v.removeToolBar(tb);
			smToolBarMap.remove(v);
		}
		
	}
	// }}}

	
	
	// {{{ BerichtsheftToolBar constructor
	private BerichtsheftToolBar(View dockable)
	{
		view = dockable;
		setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
		setFloatable(true);
		updateButtons();

	}
	// }}}

	// {{{ addNotify() method
	public void addNotify()
	{
		super.addNotify();
		EditBus.addToBus(this);
	}
	// }}}

	// {{{ removeNotify() method
	public void removeNotify()
	{
		super.removeNotify();
		EditBus.removeFromBus(this);
	}
	// }}}

	// {{{ handleMessage() method
	@EBHandler
	public void handleMessage(DynamicMenuChanged msg)
	{
		if (BerichtsheftPlugin.MENU.equals(msg.getMenuName()))
			updateButtons();
	} // }}}

	// {{{ updateButtons() method
	private void updateButtons()
	{
		removeAll();

		add(new NotePicker(new JEditTextComponent(jEdit.getActiveView())));
		add(Box.createGlue());
	}
	// }}}

	// {{{ Data members
	private View view;

	/**
	 * For each view, we might add a toolbar.
	 * This map keeps track of what
	 * views had toolbars added to them.
	 */
	static HashMap<View, BerichtsheftToolBar> smToolBarMap = 
		new HashMap<View, BerichtsheftToolBar>();
	// }}}
} // }}}

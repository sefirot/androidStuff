package android.app;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.event.ContainerAdapter;
import java.awt.event.ContainerEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JComboBox;
import javax.swing.JDialog;

import com.applang.SwingUtil.Behavior;

import static com.applang.Util.*;
import static com.applang.SwingUtil.*;

public abstract class Dialog extends JDialog
{
	protected static final String TAG = Dialog.class.getSimpleName();
	
	public Dialog(Frame owner, int behavior) {
		super(owner, Behavior.hasFlags(behavior, Behavior.MODAL));
		setAlwaysOnTop(Behavior.hasFlags(behavior, Behavior.ALWAYS_ON_TOP));
		mBehavior = behavior;
		_init();
	}

	protected int mBehavior;
	
	public Dialog open(Object...params) {
		Object param0 = param(null, 0, params);
		if (param0 instanceof Dimension)
			setSize((Dimension)param0);
		else if (param0 instanceof Double) {
			scaleSize(this, arraycast(params, new Double[0]));
		}
		if (Behavior.hasFlags(mBehavior, Behavior.TIMEOUT))
			deadline = Deadline.start(this);
		setVisible(true);
		toFront();
		return this;
	}
	
	private Deadline deadline = null;

	public void dismiss() {
		setVisible(false);
	}

	public Object result = null;

	public int getResult() {
		return (Integer)result;
	}

	public abstract void ok();
	public abstract void cancel();

	private void _init()
	{
		((Container)getLayeredPane()).addContainerListener(new ContainerHandler());
		getContentPane().addContainerListener(new ContainerHandler());
		keyHandler = new KeyHandler();
		addKeyListener(keyHandler);
		addWindowListener(new WindowHandler());
		setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
		enterEnabled = true;
	}

	public boolean getEnterEnabled()
	{
		return enterEnabled;
	}

	public void setEnterEnabled(boolean enterEnabled)
	{
		this.enterEnabled = enterEnabled;
	}

	protected boolean enterEnabled;
	protected KeyHandler keyHandler;

	class ContainerHandler extends ContainerAdapter
	{
		public void componentAdded(ContainerEvent evt)
		{
			componentAdded(evt.getChild());
		}

		public void componentRemoved(ContainerEvent evt)
		{
			componentRemoved(evt.getChild());
		}

		private void componentAdded(Component comp)
		{
			comp.addKeyListener(keyHandler);
			if(comp instanceof Container)
			{
				Container cont = (Container)comp;
				cont.addContainerListener(this);
				Component[] comps = cont.getComponents();
				for(int i = 0; i < comps.length; i++)
				{
					componentAdded(comps[i]);
				}
			}
		}

		private void componentRemoved(Component comp)
		{
			comp.removeKeyListener(keyHandler);
			if(comp instanceof Container)
			{
				Container cont = (Container)comp;
				cont.removeContainerListener(this);
				Component[] comps = cont.getComponents();
				for(int i = 0; i < comps.length; i++)
				{
					componentRemoved(comps[i]);
				}
			}
		}
	}

	class KeyHandler extends KeyAdapter
	{
		public void keyPressed(KeyEvent evt)
		{
			if(evt.isConsumed()) return;
			Component comp = getFocusOwner();
			if(evt.getKeyCode() == KeyEvent.VK_ENTER && enterEnabled)
			{
				if (deadline != null) {
					deadline.cancel();
					deadline = null;
				}
				while(comp != null)
				{
					if(comp instanceof JComboBox)
					{
						@SuppressWarnings("rawtypes")
						JComboBox combo = (JComboBox)comp;
						if(combo.isEditable())
						{
							Object selected = combo.getEditor().getItem();
							if(selected != null)
								combo.setSelectedItem(selected);
						}

						if(combo.isPopupVisible())
						{
							evt.consume();
							combo.setPopupVisible(false);
						}
						return;
					}
					// TODO: add other classes that need custom key handling here.
					comp = comp.getParent();
				}
				evt.consume();
				ok();
			}
			else if(evt.getKeyCode() == KeyEvent.VK_ESCAPE)
			{
				evt.consume();
				if(comp instanceof JComboBox)
				{
					@SuppressWarnings("rawtypes")
					JComboBox combo = (JComboBox)comp;
					if (combo.isPopupVisible())
					{
						combo.setPopupVisible(false);
					}
					else cancel();
				}
				else cancel();
			}
		}
	}

	class WindowHandler extends WindowAdapter
	{
		public void windowClosing(WindowEvent evt)
		{
			cancel();
			if (Behavior.hasFlags(mBehavior, Behavior.EXIT_ON_CLOSE))
				System.exit(0);
		}
	}
}

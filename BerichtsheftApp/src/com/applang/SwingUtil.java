package com.applang;

import java.awt.AWTEvent;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Frame;
import java.awt.Insets;
import java.awt.KeyboardFocusManager;
import java.awt.LayoutManager;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.AWTEventListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.awt.event.InputEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.font.FontRenderContext;
import java.awt.geom.AffineTransform;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Writer;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EventListener;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;

import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.DefaultCellEditor;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListModel;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButton;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.ProgressMonitor;
import javax.swing.ProgressMonitorInputStream;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import javax.swing.border.TitledBorder;
import javax.swing.event.AncestorListener;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.text.JTextComponent;

import com.applang.Util.Constraint;
import com.applang.Util.Job;
import com.applang.Util.ValList;

import android.app.Activity;
import android.app.AlertDialog;
import android.util.Log;
import android.widget.Toast;
import static com.applang.Util.*;
import static com.applang.Util2.*;

public class SwingUtil
{
    private static final String TAG = SwingUtil.class.getSimpleName();

	public interface ComponentFunction<T> {
		public T apply(Component comp, Object[] parms);
	}
	
	public static Component[] components(Component...params) {
		return params;
	}
	
	public static <T> Object[] iterateComponents(Container container, ComponentFunction<T> func, Object...params) {
		params = reduceDepth(params);
		if (container != null) {
			Component[] components = params.length > 0 ? 
				container.getComponents() : 
				components(container);
			for (Component comp : components) {
				T t = func.apply(comp, params);
				if (comp instanceof Container)
					iterateComponents((Container)comp, func, t);
			}
		}
		return params;
	}
    
	/**
	 * @param <C> the type of the return value (derived from <code>Component</code>)
	 * @param container	the containing <code>Component</code>
	 * @param regex	pattern for the name of the <code>Component</code> searched for in the container including descendants
	 * @return	the <code>Component</code> found if unequal <code>null</code> otherwise nothing was found
	 */
	public static <C extends Component> C findFirstComponent(Container container, final String regex) {
		return findFirstComponent(container, new Predicate<Component>() {
			public boolean apply(Component c) {
				String name = c.getName();
				return name != null && name.matches(regex);
			}
		});
	}

	public static <C extends Component> C findFirstComponent(Container container, final String part, final Constraint constraint) {
		return findFirstComponent(container, new Predicate<Component>() {
			public boolean apply(Component c) {
				String name = c.getName();
				return name != null && check(name, constraint, part);
			}
		});
	}

	@SuppressWarnings("unchecked")
	public static <C extends Component> C findFirstComponent(Container container, Predicate<Component> predicate) {
		Component[] comps = findComponents(container, predicate);
		if (comps.length > 0) 
			try {
				return (C)comps[0];
			} catch (Exception e) {}
		return null;
	}
    
	public static Component[] findComponents(Container container, final Predicate<Component> predicate) {
		final ArrayList<Component> al = alist();
		iterateComponents(container, new ComponentFunction<Object[]>() {
			public Object[] apply(Component c, Object[] parms) {
				if (predicate.apply(c))
					al.add(c);
				return parms;
			} 
		}, null, null);
		return al.toArray(new Component[al.size()]);
	}
    
	public static Component findAncestor(Component component, Predicate<Component> predicate) {
		do {
			component = component.getParent();
			if(component == null)
				break;
		}
		while(!predicate.apply(component));
		return component;
	}

	public static int getComponentIndex(Component component) {
		Container container = component.getParent();
		if (component != null && container != null) {
			for (int i = 0; i < container.getComponentCount(); i++) {
				if (container.getComponent(i) == component)
					return i;
			}
		}
		return -1;
	}

	public static boolean swapComponents(Component component, Component peer) {
		Container container = component.getParent();
		if (component != null && container != null && peer != null && container.equals(peer.getParent())) {
			printContainer("before swap", container, _null());
			int cIndex = getComponentIndex(component);
			int pIndex = getComponentIndex(peer);
			if (cIndex > pIndex) {
				container.remove(cIndex);
				container.add(component, pIndex);
				container.remove(pIndex + 1);
				container.add(peer, cIndex);
			}
			else if (pIndex > cIndex) {
				container.remove(pIndex);
				container.add(peer, cIndex);
				container.remove(cIndex + 1);
				container.add(component, pIndex);
			}
			printContainer("after swap", container, _null());
			return true;
		}
		return false;
	}
	
	public static void addNamePart(Component component, String part) {
		component.setName(addPart(component.getName(), part));
	}

	public static void removeNamePart(Component component, String part) {
		component.setName(removePart(component.getName(), part));
	}

	public static boolean containsComponent(Container container, final Component component) {
		return findComponents(container, new Predicate<Component>() {
			public boolean apply(Component c) {
				return component.equals(c);
			}
		}).length > 0;
	}

	public static void addCenterComponent(Component target, Container container) {
		if (container.getLayout() instanceof BorderLayout)
			container.add(target, BorderLayout.CENTER);
		else
			container.add(target);
	}
	
	public static boolean replaceCenterComponent(Component target, Container container) {
		if (container.getLayout() instanceof BorderLayout) {
			BorderLayout borderLayout = (BorderLayout) container.getLayout();
			for (Component c : container.getComponents()) 
				if (BorderLayout.CENTER.equals(borderLayout.getConstraints(c))) {
					container.remove(c);
				}
			container.add(target, BorderLayout.CENTER);
			return true;
		}
		return false;
	}
	
	public static Container getRootContainer(Container container) {
		if (container == null)
			return null;
		Container parent = container.getParent();
		while (parent != null) {
			container = parent;
			parent = container.getParent();
		}
		return container;
	}
    
    public static class Timing
    {
    	public Timing(Component comp) {
    		this.comp = comp;
    		if (this.comp != null) {
        		this.curs = this.comp.getCursor();
        		this.comp.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        		this.comp.requestFocus();
    		}
    		this.millis = System.currentTimeMillis();
    	}
    	
		private long millis;
		
		public long current() {
			return System.currentTimeMillis() - millis;
		}
		
    	Component comp;
    	Cursor curs;
    	
    	/* (non-Javadoc)
    	 * @see java.lang.Object#finalize()
    	 */
    	@Override 
    	public void finalize() {
    		try {
        		this.millis -= System.currentTimeMillis();
        		if (this.comp != null) 
        			this.comp.setCursor(curs);
    		}
    		finally {
    			try {
					super.finalize();
				} catch (Throwable e) {}
    		}
    	}
    }
	
	public static long waiting(Component component, 
			ComponentFunction<Void> func, 
			Object... params)
	{
        final Timing timing = new Timing(component);
		try {
			func.apply(component, arrayextend(params, true, timing));
			long current = timing.current();
			return current;
		}
		finally {
			timing.finalize();
		}
	}

	public static class Deadline extends SwingWorker<Void, Void>
	{
		public Deadline(Window wnd, Integer[] keyEvents) {
			mWnd = wnd;
			mKeyEvents = keyEvents;
		}
		
		Window mWnd;
		Integer[] mKeyEvents = null;

		@Override
		protected Void doInBackground(){
			println(String.format("deadline after %d ms", WAIT));

	        waiting(mWnd, new ComponentFunction<Void>() {
				public Void apply(Component comp, Object[] parms) {
					Timing timing = (Timing)parms[0];
					
					while (timing.current() < WAIT && !isCancelled()) 
						Thread.yield();
					
					return null;
				}
			});
				
			return null;
		}
		
		@Override
		protected void done() {
			if (isCancelled())
				return;
			
			if (!nullOrEmpty(mKeyEvents)) 
				doKeyEvents();
			else if (mWnd != null) 
				pullThePlug(mWnd);
			
			timedOut = true;
		}
		
		public void cancel() {
			if (this.cancel(true))
				println("deadline cancelled");
			
			started = false;
		}

		void doKeyEvents() {
			try {
				Robot robot = new Robot();
				for (int i = 0; i < mKeyEvents.length; i++) {
					if (mKeyEvents[i] != null) {
						robot.keyPress(mKeyEvents[i]);
					}
				}
				
				robot.delay(DELAY);
				
				for (int i = 0; i < mKeyEvents.length; i++) {
					if (mKeyEvents[i] != null) {
						robot.keyRelease(mKeyEvents[i]);
					}
				}
				
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		public static int DELAY = 1000;
		public static int WAIT = 1000;
		
		public static Deadline start(int millis, Integer... keyEvents) {
			Deadline.WAIT = millis;
			return start(null, keyEvents);
		}
		
		public static Deadline start(Window wnd, Integer... keyEvents) {
			if (wnd == null && notAvailable(0, keyEvents))
				return null;
			timedOut = false;
			final Deadline deadline = new Deadline(wnd, keyEvents);
			deadline.execute();
			Toolkit.getDefaultToolkit().addAWTEventListener(new AWTEventListener() {
				public void eventDispatched(AWTEvent ev) {
		            deadline.cancel();
				}
			}, AWTEvent.MOUSE_MOTION_EVENT_MASK);
			started = true;
			return deadline;
		}
		
		public static boolean started = false;
		public static boolean timedOut = false;
		
		public static void finish() {
			if (started) {
				try {
					SwingUtilities.invokeAndWait(doEvents);
				} catch (Exception e) {
					e.printStackTrace();
				}
				
				started = false;
				
				println("deadline finished");
			}
		}

		static Runnable doEvents = new Runnable() {
			public void run() {
				Deadline deadline = new Deadline(null, null);
				deadline.execute();
			}
		};
	}
	
    /**
     * closes the <code>Window</code> programmatically
     * @param wnd
     */
    public static void pullThePlug(Window wnd) {
        WindowEvent wev = new WindowEvent(wnd, WindowEvent.WINDOW_CLOSING);
        Toolkit.getDefaultToolkit().getSystemEventQueue().postEvent(wev);
    }
	
	public static class Bounds extends Rectangle
	{
		public Bounds() {}
		
		public Bounds(Window window, Object... params) {
			super(window != null ? 
				window.getBounds() : 
				param(new Rectangle(300,300,400,400), 0, params));
		}
		
		public static Rectangle load(Window window, 
				String category, String title,
				Object...params)
		{
			String key = key(category, title);
			Bounds rect = new Bounds(null, params);
			for (MatchResult mr : findAllIn(getSetting(key, ""), Pattern.compile("(\\w+)=(\\d+)"))) {
				if ("x".equals(mr.group(1)))
					rect.x = toInt(-1, mr.group(2));
				else if ("y".equals(mr.group(1)))
					rect.y = toInt(-1, mr.group(2));
				else if ("width".equals(mr.group(1)))
					rect.width = toInt(-1, mr.group(2));
				else if ("height".equals(mr.group(1)))
					rect.height = toInt(-1, mr.group(2));
			}
			
			Rectangle bounds = new Bounds(window, params);
			
			if (rect.x > -1)
				bounds.x = rect.x;
			if (rect.y > -1)
				bounds.y = rect.y;
			if (rect.width > -1)
				bounds.width = rect.width;
			if (rect.height > -1)
				bounds.height = rect.height;
			
			
			if (window != null)
				window.setBounds(bounds);
			
			return bounds;
		}
		
		static String key(String category, String title) {
			return category + "." + title.replaceAll("\\s+", "") + ".bounds";
		}
		
		public static void save(Window window, 
				String category, String title,
				Object...params)
		{
			String key = key(category, title);
			String value = stringValueOf(new Bounds(window, params));
			int brac = value.indexOf('[');
			putSetting(key, value.substring(brac > -1 ? brac : 0));
		}
	}
	
    public static class Behavior
    {
    	public static final int NONE = 4;
    	public static final int MODAL = 8;
    	public static final int TIMEOUT = 16;
    	public static final int ALWAYS_ON_TOP = 32;
    	public static final int EXIT_ON_CLOSE = 64;
    	public static final int HIDDEN = 128;
        
        public static int getOptionType(int behavior) {
        	return (behavior + 1) % 4 - 1;
        }

        public static int getFlags(int behavior) { 
            return (behavior + 1) / 4 * 4; 
        }

        public static boolean hasFlags(int behavior, int flags) { 
			return (getFlags(behavior) & flags) > 0; 
        }

        public static int setFlags(int behavior, int flags) { 
			return flags < 0 ? 
					behavior & ~Math.abs(flags) : 
					behavior | flags; 
        }

        public static int setTimeout(int behavior, boolean timeout) { 
			return setFlags(behavior, timeout ? TIMEOUT : -TIMEOUT); 
        }

		public static int setModal(int behavior, boolean modal) {
			return setFlags(behavior, modal ? MODAL : -MODAL); 
		}
    }

	public interface UIFunction extends ComponentFunction<Component[]> {
	}

	private static boolean finished = false;
	
	public static JFrame showFrame(Object relative, 
			String title, 
			UIFunction assembleUI,
			UIFunction arrangeUI,
    		final UIFunction completeUI,
    		int behavior, 
    		final Object...params)
	{
		final JFrame frame = new JFrame(title);
		try {
			frame.setName(title);
			
			if (assembleUI != null) {
				Component[] widgets = assembleUI.apply(frame, params);
				if (widgets != null)
					if (widgets.length == 1 && widgets[0] instanceof Container)
						frame.setContentPane((Container)widgets[0]);
					else
						for (Component widget : widgets)
							frame.getContentPane().add(widget);
			}
			
			frame.pack();
			if (relative == null || relative instanceof Component)
				frame.setLocationRelativeTo((Component)relative);
			else if (relative instanceof Point)
				frame.setLocation((Point)relative);
			else if (relative instanceof Rectangle)
				frame.setBounds((Rectangle)relative);
			boolean exitOnClose = Behavior.hasFlags(behavior, Behavior.EXIT_ON_CLOSE);
			frame.setDefaultCloseOperation(exitOnClose ? JFrame.EXIT_ON_CLOSE : JFrame.DISPOSE_ON_CLOSE);
			frame.setVisible(!Behavior.hasFlags(behavior, Behavior.HIDDEN));
			
			if (arrangeUI != null)
				arrangeUI.apply(frame, params);
			
			frame.addWindowListener(new WindowAdapter() {
				public void windowClosing(WindowEvent event) {
					if (completeUI != null)
						completeUI.apply(frame, params);
					finished = true;
				}
			});
			
			boolean deadline = Behavior.hasFlags(behavior, Behavior.TIMEOUT);
			if (deadline) {
				Deadline.start(frame);
				finished = false;
				while (!finished) {
					Thread.yield();
				}
			}
		} catch (Exception e) {
			handleException(e);
		}
		return frame;
	}
	
	public static void startFrame(final Action action, Object...params) {
		showFrame(null, 
				"Start",
				new UIFunction() {
					public Component[] apply(final Component comp, Object[] parms) {
						return components(action != null ? 
								new JButton(action) : 
								new JButton(new AbstractAction("Dispose") {
									@Override
									public void actionPerformed(ActionEvent ev) {
										pullThePlug((JFrame)comp);
									}
								}));
					}
				}, 
				null, null, 
				Behavior.TIMEOUT, 
				params);
	}
	
	public static int dialogResult = JOptionPane.CLOSED_OPTION;
	
    public static JDialog showDialog(Component parent, Component relative, 
    		String title, 
    		UIFunction assembleUI,
    		UIFunction arrangeUI,
    		final UIFunction completeUI,
    		final int behavior, 
    		final Object...params)
    {
    	dialogResult = JOptionPane.CLOSED_OPTION;
    	
    	boolean modal = Behavior.hasFlags(behavior, Behavior.MODAL);
		Frame frame = JOptionPane.getFrameForComponent(parent);
		final JDialog dlg = new JDialog(frame, title, modal);
		boolean alwaysOnTop = Behavior.hasFlags(behavior, Behavior.ALWAYS_ON_TOP);
		dlg.setAlwaysOnTop(alwaysOnTop);
		
		if (assembleUI != null) {
			Component[] widgets = assembleUI.apply(dlg, params);
			if (widgets != null)
				if (widgets.length == 1 && widgets[0] instanceof Container)
					dlg.setContentPane((Container) widgets[0]);
				else
					for (Component widget : widgets)
						dlg.getContentPane().add(widget);
		}
		
		boolean deadline = Behavior.hasFlags(behavior, Behavior.TIMEOUT);
		if (deadline)
			Deadline.start(dlg);
		
		dlg.pack();
		dlg.setLocationRelativeTo(relative);
		dlg.setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);
		
		if (modal && arrangeUI != null)
			arrangeUI.apply(dlg, params);

		if (!deadline)
			Deadline.start(null);
		
		dlg.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent event) {
				if (completeUI != null)
					completeUI.apply(dlg, params);
				if (Behavior.hasFlags(behavior, Behavior.EXIT_ON_CLOSE))
					System.exit(0);
			}
		});
		
		dlg.setVisible(!Behavior.hasFlags(behavior, Behavior.HIDDEN));
		
		if (!modal && arrangeUI != null)
			arrangeUI.apply(dlg, null);
    	
		no_println("dialogResult", dialogResult);
		
		return dlg;
	}
	
    public static int showOptionDialog(Component parent, 
    		final Object message, String title,
    		final int optionType, final int messageType,
            Icon icon,
            final Object[] options, final Object initialOption, 
            Object...params) 
    {
    	if (optionType > JOptionPane.OK_CANCEL_OPTION) {
    		final Function<Boolean> optionHandler = param(null, 0, params);
    		UIFunction assembleUI = new UIFunction() {
    			public Component[] apply(final Component dialog, Object[] parms) {
    				final JOptionPane optionPane = new JOptionPane(message, 
    						messageType,
    						Behavior.getOptionType(optionType),
    						null,
    						options, 
    						initialOption);
    				optionPane.addPropertyChangeListener(new PropertyChangeListener() {
    					public void propertyChange(PropertyChangeEvent ev) {
    						Object source = ev.getSource();
    						String prop = ev.getPropertyName();
    						if (dialog.isVisible() && 
								optionPane.equals(source) && 
								JOptionPane.VALUE_PROPERTY.equals(prop))
    						{
    							Object value = optionPane.getValue();
    							if (value == JOptionPane.UNINITIALIZED_VALUE) {
    								dialogResult = JOptionPane.CLOSED_OPTION;
    								return;
    							}
    							else {
	    							optionPane.setValue(JOptionPane.UNINITIALIZED_VALUE);
	    							if (options != null)
	    								dialogResult = asList(options).indexOf(value);
	    							else 
	    								dialogResult = (Integer) value;
	    							Boolean visibility = false;
	    							if (optionHandler != null)
	    								visibility = optionHandler.apply(ev, options, message);
	    							dialog.setVisible(visibility);
	    							if (!visibility && Behavior.hasFlags(optionType, Behavior.EXIT_ON_CLOSE))
	    								System.exit(0);
    							}
    						};
    					}
    				});
    				return components(optionPane);
    			}
    		};
    		showDialog(JOptionPane.getFrameForComponent(parent), parent, 
					title,
					assembleUI, 
					null, null, 
					optionType,
					params);
    		return dialogResult;
    	}
    	else {
    		Deadline.start(null);
    		parent = JOptionPane.getFrameForComponent(parent);
        	return dialogResult = JOptionPane.showOptionDialog(parent, 
        			message, title, 
        			optionType, messageType, 
        			icon, 
        			options, initialOption);
    	}
	}
    
    public static Icon defaultIcon(int optionType) {
		switch (optionType) {
		case JOptionPane.DEFAULT_OPTION:
		case 3:
			return UIManager.getIcon("OptionPane.informationIcon");
		default:
			return UIManager.getIcon("OptionPane.questionIcon");
		}
	}
    
    public static ValList defaultOptions(int optionType) {
    	Object[] array;
    	ValList list = vlist();
       	Object yesString = UIManager.get("OptionPane.yesButtonText");
       	Object noString = UIManager.get("OptionPane.noButtonText");
       	Object cancelString = UIManager.get("OptionPane.cancelButtonText");
       	Object okString = UIManager.get("OptionPane.okButtonText");
		switch (optionType) {
    	case JOptionPane.YES_NO_OPTION:
    		array = objects( yesString, noString, null );
        	break;
    	case JOptionPane.YES_NO_CANCEL_OPTION:
    		array = objects( yesString, noString, cancelString, cancelString );
        	break;
    	case JOptionPane.OK_CANCEL_OPTION:
    		array = objects( okString, cancelString, cancelString );
    		break;
    	case JOptionPane.DEFAULT_OPTION:
    	case 3:
			array = objects( "Close", null );
    		break;
    	case 5:
    		array = objects( "Yes", "Yes all", "No", "No all", "Cancel", null );
        	break;
    	case 10:
    		array = objects("OK","Add","Remove","Cancel","OK");
        	break;
    	case 11:
    		array = objects("OK","Add","Remove","Edit","Cancel","OK");
        	break;
    	case 12:
    		array = objects("OK","Save","Cancel","Save");
        	break;
    	case 13:
    		array = objects("Accept","Reject","Accept");
        	break;
    	case 14:
    		array = objects("Sync", null);
        	break;
    	default:
    		return list;
       	}
       	list.addAll(asList(array));
    	return list;
    }
	
    public static Object showInputDialog(Component parent, 
    		Object message, String title,
            int optionType, int messageType,
            Icon icon,
            Object[] selections, Object initialValue, 
            Integer... keyEvents) 
    {
    	dialogResult = JOptionPane.CLOSED_OPTION;
    	
        Object[] options = selections;
        Object initialOption = initialValue;
        if (options == null) {
        	ValList list = defaultOptions(optionType);
        	initialOption = list.remove(-1);
        	options = list.toArray();
        }
        
        Object[] widgets = null;
    	JTextComponent textComponent = null;
    	String text = notNullOrEmpty(initialValue) ? initialValue.toString() : "";
    	
		parent = JOptionPane.getFrameForComponent(parent);
		
		Deadline.start(null, keyEvents);
		
    	switch (optionType) {
    	case JOptionPane.DEFAULT_OPTION:
        	return JOptionPane.showInputDialog(parent, 
        			message, title, 
        			messageType, 
        			icon, 
        			selections, initialValue);
        	
    	default:
    		optionType = JOptionPane.DEFAULT_OPTION;
    		textComponent = new JTextArea(text);
    		textComponent.setEditable(false);
			widgets = objects(new JScrollPane(textComponent));
			break;
    	    
    	case JOptionPane.YES_NO_OPTION:
    	case JOptionPane.YES_NO_CANCEL_OPTION:
    	case JOptionPane.OK_CANCEL_OPTION:
			JTextField textField = new JTextField(text);
    		textField.setHorizontalAlignment(JTextField.CENTER);
    		addSelectAllObserver(textField);
    		
	    	JLabel label = new JLabel(message.toString());
    	    label.setDisplayedMnemonic(KeyEvent.VK_I);
		    label.setLabelFor(textField);
    		
    		textComponent = textField;
    		textComponent.requestFocusInWindow();
		    
    		widgets = objects(label, textComponent);
    	    break;
    	}
    	
	    dialogResult = JOptionPane.showOptionDialog(parent, 
				widgets, title, 
				optionType,
				messageType,
				icon,
				options, initialOption);
		
    	return textComponent.getText();
	}

    public static <T> T showResizableDialog(final JComponent component, 
    		AncestorListener ancestorListener, 
    		Function<T> show, Object...params) 
    {
		component.addHierarchyListener(new HierarchyListener() {
			public void hierarchyChanged(HierarchyEvent e) {
				Window window = SwingUtilities.getWindowAncestor(component);
				if (window instanceof Dialog) {
					Dialog dialog = (Dialog) window;
					if (!dialog.isResizable()) 
						dialog.setResizable(true);
				}
			}
		});
		if (ancestorListener != null)
			component.addAncestorListener(ancestorListener);
		return show.apply(arrayappend(objects(component), params));
    }
    
    public static String getWindowTitle(Component component) {
		Window window = SwingUtilities.windowForComponent(component);
		if (window instanceof Frame) 
			return ((Frame)window).getTitle();
		else if (window instanceof Dialog) 
			return ((Dialog)window).getTitle();
		return null;
    }
    
    public static boolean setWindowTitle(Component component, String title) {
    	boolean retval = true;
		Window window = SwingUtilities.windowForComponent(component);
		if (window instanceof Frame) 
			((Frame)window).setTitle(title);
		else if (window instanceof Dialog) 
			((Dialog)window).setTitle(title);
		else
			retval = false;
		return retval;
    }

	@SuppressWarnings("unchecked")
	public static File getFileFromStore(int type, 
			final String title, 
			FileNameExtensionFilter filter, 
			Function<File> chooser, 
			Function<String[]> loader, 
			Job<String[]> saver, 
			Object...params) 
	{
		Function<String[]> lister = new Function<String[]>() {
			public String[] apply(Object... params) {
				List<String> list = alist();
				String element = param(null, 0, params);
				String[] array = param(null, 1, params);
				if (array != null) {
					arrayexclude(arrayindexof(element, array), array, list);
				}
				boolean sort = param_Boolean(false, 2, params);
				if (sort) {
					Set<String> set = sortedSet(list);
					list = alist();
					list.addAll(set);
				}
				boolean first = param_Boolean(true, 3, params);
				if (first)
					list.add(0, element);
				return toStrings(list);
			}
		};
		String fileName = param(null, 0, params);
		File file = notNullOrEmpty(fileName) ? new File(fileName) : null;
		boolean allowEdit = type > 0;
		int option = 1;
		String[] fileNames = null;
		try {
			if (loader != null)
				fileNames = loader.apply(params);
		} catch (Exception e) {
			Log.e(TAG, "getFileFromStore", e);
		}
		boolean storeEmpty = nullOrEmpty(fileNames);
		if (!storeEmpty) {
			if (nullOrEmpty(fileName))
				fileName = fileNames[0];
			fileNames = lister.apply(fileName, fileNames, true);
			@SuppressWarnings({ "rawtypes" })
			JComboBox combo = new JComboBox(fileNames);
			JLabel label = new JLabel(title);
			label.setLabelFor(combo);
			Object message = objects(label, combo);
			ValList list = defaultOptions(allowEdit ? 11 : 10);
			Object initialOption = list.remove(-1);
			Object[] options = list.toArray();
			option = JOptionPane.showOptionDialog(null, message, "Store", 
					JOptionPane.DEFAULT_OPTION, 
					JOptionPane.PLAIN_MESSAGE, 
					null, 
					options, initialOption);
			if (option < 0 || option == options.length - 1)
				return null;
			if (option == 2) {			//	Remove
				if (fileNames != null) {
					fileNames = lister.apply(combo.getSelectedItem(), fileNames, false, false);
					try {
						saver.perform(fileNames, params);
					} catch (Exception e) {
						Log.e(TAG, "getFileFromStore", e);
					}
				}
				return getFileFromStore(type, title, filter, chooser, loader, saver, params);
			}
			if (option > -1)
				file = new File(combo.getSelectedItem().toString());
			if (option == 3) {			//	Edit
				JTextArea textArea = new JTextArea("");
	    		textArea.setEditable(true);
	    		textArea.setText(contentsFromFile(file));
				list = defaultOptions(12);
				initialOption = list.remove(-1);
				options = list.toArray();
				option = showResizableDialog(textArea, null, new Function<Integer>() {
					public Integer apply(Object...parms) {
						JScrollPane scrollPane = new JScrollPane((Component) parms[0]);
						scrollPane.setPreferredSize(new Dimension(500,200));
						return JOptionPane.showOptionDialog(null, scrollPane, title, 
								JOptionPane.DEFAULT_OPTION, 
								JOptionPane.PLAIN_MESSAGE, 
								null, 
								(Object[])parms[1], parms[2]);
					}
				}, options, initialOption);
				if (option < 0 || option == options.length - 1)
					return null;
				if (option == 0) {		//	OK
					file = tempFile("." + filter.getExtensions()[0]);
					contentsToFile(file, textArea.getText());
					return file;
				}
				file = chooser == null ? 
						chooseFile(false, null, title, file, filter) : 
						chooser.apply(false, null, title, file, filter);
				if (file != null)
					contentsToFile(file, textArea.getText());
			}
		}
		if (option == 1) {				//	Add
			file = chooser == null ? 
					chooseFile(true, null, title, file, filter) : 
					chooser.apply(true, null, title, file, filter);
			if (fileNames == null)
				return file;
			if (file == null)
				return getFileFromStore(type, title, filter, chooser, loader, saver, params);
		}
		if (file != null) {
			fileName = file.getPath();
			if (fileName.indexOf(NEWLINE) < 0) {
				fileNames = lister.apply(fileName, fileNames);
				try {
					saver.perform(fileNames, params);
				} catch (Exception e) {
					Log.e(TAG, "getFileFromStore", e);
				}
			}
			if (!storeEmpty && option > 0) {
				if (isAvailable(0, params))
					params[0] = fileName;
				else
					params = objects(fileName);
				return getFileFromStore(type, title, filter, chooser, loader, saver, params);
			}
		}
		return file;
	}

	public static String[] chooseFileNames(boolean toOpen, Container parent, 
			String title, 
			String fileName, 
			FileFilter...fileFilters)
	{
		File f = new File(stringValueOf(fileName));
		f = chooseFile(toOpen, parent, title, f, fileFilters);
		return strings(f == null ? "" : f.getPath());
	}

	public static String[] chooseDirectoryNames(Container parent, 
			String title, 
			String dirName)
	{
		File f = new File(stringValueOf(dirName));
		f = chooseDirectory(parent, title, f);
		return strings(f == null ? "" : f.getPath());
	}
	
	/**
	 * @param parent
	 * @param title
	 * @param file
	 * @param fileFilter
	 * @return	the chosen <code>File</code>
	 */
	public static File chooseFile(boolean toOpen, Container parent, 
			String title, 
			File file, 
			FileFilter...fileFilters)
	{
		File dir = null;
		try {
			dir = file.getParentFile();
		} catch (Exception e) {}
		if (dir == null)
			dir = new File(relativePath());
		
		JFileChooser chooser = new JFileChooser();
		chooser.setCurrentDirectory(dir);
	    chooser.setDialogTitle(title);
	    chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
	    if (fileFilters != null)
	    	for (FileFilter ff : fileFilters)
	    		setFileFilter(chooser, ff);
		
	    if (fileExists(file))
	    	chooser.setSelectedFile(file);
	    
		parent = JOptionPane.getFrameForComponent(parent);
		dialogResult = toOpen ?
				chooser.showOpenDialog(parent) : 
				chooser.showSaveDialog(parent);
	    if (dialogResult == JFileChooser.APPROVE_OPTION)
	    	return chooser.getSelectedFile();
	    else
	    	return null;
	}
	
	static void setFileFilter(JFileChooser chooser, FileFilter fileFilter) {
	    if (fileFilter == null)
	    	chooser.resetChoosableFileFilters();
	    else {
	    	chooser.addChoosableFileFilter(chooser.getAcceptAllFileFilter());
	    	chooser.addChoosableFileFilter(fileFilter);
	    }
	    chooser.setAcceptAllFileFilterUsed(true);
	}

	/**
	 * @param parent
	 * @param title
	 * @param dir
	 * @return	the chosen directory <code>File</code>
	 */
	public static File chooseDirectory(Container parent, 
			String title, 
			File dir)
	{
		JFileChooser chooser = new JFileChooser();
		chooser.setCurrentDirectory(dir);
	    chooser.setDialogTitle(title);
	    chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
	    chooser.setAcceptAllFileFilterUsed(false);
		
		parent = JOptionPane.getFrameForComponent(parent);
		dialogResult = chooser.showOpenDialog(parent);
	    if (dialogResult == JFileChooser.APPROVE_OPTION)
	    	return chooser.getSelectedFile();
	    else
	    	return null;
	}

	public static void tabKeyForwarding(JComponent jc) {
	    jc.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				no_println("pressed", e.getKeyCode(), "ctrl", e.isControlDown());
				if (e.getKeyCode() == KeyEvent.VK_TAB) {
					e.consume();
					if (e.isShiftDown()) 
						KeyboardFocusManager.getCurrentKeyboardFocusManager().focusPreviousComponent();
					else
						KeyboardFocusManager.getCurrentKeyboardFocusManager().focusNextComponent();
				}
			}
			@Override
			public void keyReleased(KeyEvent e) {
				no_println("released", e.getKeyCode(), "ctrl", e.isControlDown());
			}
		});
	}

	@SuppressWarnings("rawtypes")
	public static JTextField comboEdit(JComboBox combo) {
		return (JTextField)combo.getEditor().getEditorComponent();
	}
	
	public static boolean underTest = false;
	
	public static JToolBar northToolBar(Container container, Object...params) {
		JToolBar bar = new JToolBar();
		bar.setFloatable(true);
		container.add(bar, param(BorderLayout.NORTH, 0, params));
		bar.setName(param(BorderLayout.NORTH, 1, params));
		return bar;
	}
	
	public static JToolBar southStatusBar(Container container, Object...params) {
		JToolBar bar = new JToolBar();
		bar.setFloatable(false);
		messageBox(bar);
		container.add(bar, param(BorderLayout.SOUTH, 0, params));
		bar.setName(param(BorderLayout.SOUTH, 1, params));
		return bar;
	}

	public static JLabel messageBox(Container container) {
		JLabel label = new JLabel("");
		label.setName("mess");
		container.add(label);
		SwingUtil.container = container;
		return label;
	}

	private static Container container = null;
	public static Function<String> messRedirection = null;
	
	public static void message(String text) {
		if (underTest)
			println(text);
		else if (messRedirection != null)
			messRedirection.apply(text);
		else {
			Container rootContainer = underTest ? container : getRootContainer(container);
			JLabel mess = findFirstComponent(rootContainer, "mess");
			if (mess != null) 
				mess.setText(text);
			else
				alert(text);
		}
	}
	
	public static void longToast(String text) {
		Toast.makeText(Activity.getInstance(), text, Toast.LENGTH_LONG).show(true);
	}

	public static void alert(Object...params) {
		JOptionPane.showMessageDialog(
				container, 
				param_String("", 0, params), 
				param_String("Alert", 1, params), 
				param_Integer(JOptionPane.PLAIN_MESSAGE, 2, params));
	}

	public static boolean question(Object...params) {
		return JOptionPane.OK_OPTION == JOptionPane.showConfirmDialog(
				container, 
				param_String("Are you sure", 0, params), 
				param_String("Question", 1, params), 
				param_Integer(JOptionPane.OK_CANCEL_OPTION, 2, params));
	}

	public static void handleException(Exception ex) {
		if (ex != null) {
			AlertDialog.alerter(new Activity(), "Exception", ex);
//			String message = ex.getMessage();
//			if (nullOrEmpty(message))
//				message = String.valueOf(ex);
//			message(message);
		}
	}

	public static String resourceFrom(String path, String encoding) {
		if (notNullOrEmpty(path)) {
			InputStream is = null;
			try {
				is = SwingUtil.class.getResourceAsStream(path);
				BufferedReader rd = new BufferedReader(new InputStreamReader(is, Charset.forName(encoding)));
				return readAll(rd);
			} catch (Exception e) {
				if (is != null)
					try {
						is.close();
					} catch (Exception e1) {}
			}
		}
		return null;
	}

	public static ImageIcon iconFrom(String path) {
		if (notNullOrEmpty(path)) {
			URL url = SwingUtil.class.getResource(path);
			return new ImageIcon(url);
		}
		else
			return null;
	}
	
	public interface CustomActionType 
	{
		public int index();
		public String resourceName();
	    public String iconName();
	    public String name(int state);
	    public String description();
	}
	
    public static class CustomAction extends AbstractAction
    {
    	public static boolean actionBlocked = false;
    	
    	public static void blocked(Job<Void> job, Object[] params) throws Exception {
    		actionBlocked = true;
    		job.perform(null, params);
    		actionBlocked = false;
    	}
    	
    	private CustomActionType type = null;
    	
        public CustomActionType getType() {
        	return type;
		}

		public void setType(CustomActionType type) {
			this.type = type;
			putValue(SMALL_ICON, iconFrom(this.type.iconName()));
			putValue(SHORT_DESCRIPTION, this.type.description());
//			putValue(MNEMONIC_KEY, KeyEvent.CHAR_UNDEFINED);
		}

		public CustomAction(CustomActionType type) {
			super(null);
			setType(type);
        }

		public CustomAction(String text) {
			super(text);
        }
        
        @Override
        public void actionPerformed(ActionEvent ae) {
        	if (actionBlocked || (getType() == null && getValue(CustomAction.NAME) == null))
        		return;
        	message("");
        	Log.d(TAG, type == null ? ae.getActionCommand() : type.toString());
        	action_Performed(ae);
        }
        
        protected void action_Performed(ActionEvent ae) {
        	if (actionBlocked)
        		return;
        }
    }
	
	public static void toggle(CustomAction toggleAction, Job<Boolean> job, Object...params) {
		String name1 = toggleAction.getType().name(1);
		String name2 = toggleAction.getType().name(2);
		Object name = toggleAction.getValue(Action.NAME);
		name = name != null && name.equals(name1) ? name2 : name1;
		toggleAction.putValue(Action.NAME, name);
    	try {
    		boolean toggle = name.equals(name2);
			job.perform(toggle, params);
		} 
    	catch (Exception e) {
			Log.e(TAG, "toggle", e);
		}
	}
	
    /**
     * @param <T>	type of container
     * @param container	if T is <code>JPanel</code> then <code>JButton</code> are added else <code>JMenuItem</code>
     * @param params	button specifications
     * 	<table border="1">
     * 		<tr><th>index</th><th>description</th></tr>
     * 		<tr><td>0</td><td>button text</td></tr>
     * 		<tr><td>1</td><td>action (<code>EventListener</code>)</td></tr>
     * 		<tr><td>2</td><td>button name</td></tr>
     * 		<tr><td>3</td><td>button tooltip text</td></tr>
     * 		<tr><td>4</td><td>enabled (<code>Boolean</code>)</td></tr>
     * 		<tr><td>5</td><td>mnemonic (<code>Integer</code>)</td></tr>
     * 		<tr><td>6</td><td>selected (<code>Boolean</code>)</td></tr>
     * 		<tr><td>7</td><td>accelerator (<code>KeyStroke</code>)</td></tr>
     * 	</table>
     * @return
     */
    public static <T extends JComponent> T addButtons(T container, Object... params) {
    	AbstractButton button = null;
		ButtonGroup group = null;
        
        for (int i = 0; i < params.length; i++) {
        	boolean multi = params[i] instanceof Object[];
        	Object[] parms = multi ? (Object[])param(null, i, params) : params;
        	
        	String text = param_String("-", 0, parms);
        	String name = param_String("button" + i, 2, parms);
        	if ("-".equals(text)) {
        		group = (ButtonGroup)param(null, 1, parms);
        		
        		if (param_Boolean(true, 2, parms)) {
            		if (container instanceof JPanel) {
            			LayoutManager lm = container.getLayout();
            			int axis = lm instanceof BoxLayout ? 
            					((BoxLayout)lm).getAxis() : -1;
            			switch (axis) {
            			case BoxLayout.LINE_AXIS:
            			case BoxLayout.X_AXIS:
            				container.add(Box.createHorizontalStrut(param_Integer(0, 3, parms)));
            				break;
            			case BoxLayout.PAGE_AXIS:
            			case BoxLayout.Y_AXIS:
            				container.add(Box.createVerticalStrut(param_Integer(0, 3, parms)));
            				break;
            			default:
            				container.add(new JLabel(param_String(" ", 3, parms)));
            				break;
            			}
            		}
            		else if (container instanceof JPopupMenu)
            			((JPopupMenu)container).addSeparator();
    				else
    					((JMenu)container).addSeparator();
        		}
        	}
        	else if ("+".equals(text) || "+".equals(name)) {
        		JMenu menu = param(null, 1, parms);
        		menu.setName(name);
				container.add(menu);
        	}
        	else if (notNullOrEmpty(text)) {
        		boolean minus = text.startsWith("-");
        		boolean check = text.startsWith("-check ");
        		boolean radio = text.startsWith("-radio ");
        		if (container instanceof JPanel) {
        			if (minus) {
	            		if (check)
		        			button = new JCheckBox(text.substring(7));
		        		else if (radio) {
		        			button = new JRadioButton(text.substring(7));
		        			if (group != null)
		        				group.add(button);
		        		}
		        		else
		        			continue;
	            		
	        		} else 
	        			button = new JButton(text);
        		}
        		else {
        			if (minus) {
	            		if (check)
		        			button = new JCheckBoxMenuItem(text.substring(7));
		        		else if (radio) {
		        			button = new JRadioButtonMenuItem(text.substring(7));
		        			if (group != null)
		        				group.add(button);
		        		}
		        		else
		        			continue;
	        		} else 
	        			button = new JMenuItem(text);
        		}
        		
        		Object o = param(null, 1, parms);
        		EventListener evl = o instanceof EventListener ? (EventListener)o : null;
        		if (evl instanceof ItemListener) 
                	button.addItemListener((ItemListener)evl);
        		if (evl instanceof ActionListener) 
        			button.addActionListener((ActionListener)evl);
        		
        		if (evl instanceof CustomAction) {
        			CustomAction act = (CustomAction)evl;
					button.setAction(act);
					button.setText(act.getValue(CustomAction.SHORT_DESCRIPTION).toString());
	                button.setName(name);
        		}
        		else {
	                button.setName(name);
	                button.setActionCommand(name);
	                button.setToolTipText(param_String("", 3, parms));
	                button.setEnabled(param_Boolean(true, 4, parms));
	            	button.setMnemonic(param_Integer(0, 5, parms));
	                button.setSelected(param_Boolean(false, 6, parms));
	                KeyStroke ks = param(KeyStroke.getKeyStroke(' '), 7, parms);
	                if (isType(JMenuItem.class, button))
	                	((JMenuItem)button).setAccelerator(ks);
        		}
                
                container.add(button);
        	}
        	
        	if (!multi)
        		break;
		}
        
        return container;
    }
	
    public static JMenu newMenu(String name, Object... params) {
		return addButtons(new JMenu(name), params);
	}
	
    public static JPopupMenu newPopupMenu(Object... params) {
    	JPopupMenu popupMenu = addButtons(new JPopupMenu(), params);
        
        for (int i = 0; i < params.length; i++) {
        	boolean multi = params[i] instanceof Object[];
        	Object[] parms = multi ? (Object[])param(null, i, params) : params;
        	
	    	PopupMenuListener popupMenuListener = param(null, 8, parms);
	    	if (popupMenuListener != null)
	    		popupMenu.addPopupMenuListener(popupMenuListener);
        }
    	return popupMenu;
	}
	
    /**
     * @param params
     * @return
     */
    public static MouseListener newPopupAdapter(Object... params) {
    	return new PopupAdapter(newPopupMenu(params));
	}
	
    /**
     * @param params
     * @return
     */
    public static DropdownAdapter newDropdownAdapter(Object... params) {
    	return new DropdownAdapter(newPopupMenu(params));
	}

    /**
    *
    */
    public static class PopupMenuAdapter implements PopupMenuListener
    {
		public void popupMenuCanceled(PopupMenuEvent e) {
		//	println("Canceled");
		}
		
		public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
		//	println("Becoming invisible");
		}
		
		public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
		//	println("Becoming visible");
		}
    }
    
    /**
     *
     */
    public static class DropdownAdapter extends MouseAdapter
    {
    	JPopupMenu popupMenu;
    	
        public DropdownAdapter(JPopupMenu popupMenu) {
        	this.popupMenu = popupMenu;
        }

        /* (non-Javadoc)
         * @see java.awt.event.MouseAdapter#mousePressed(java.awt.event.MouseEvent)
         */
        @Override
        public void mousePressed(MouseEvent e) {
            maybeShowPopup(e);
        }

        /* (non-Javadoc)
         * @see java.awt.event.MouseAdapter#mouseReleased(java.awt.event.MouseEvent)
         */
        @Override
        public void mouseReleased(MouseEvent e) {
            maybeShowPopup(e);
        }

        /**
         * @param e
         */
        protected void maybeShowPopup(MouseEvent e) {
    		Component c = e.getComponent();
            if (c.isEnabled()) {
				popupMenu.show(c, 0, c.getHeight());
			}
        }
	}
    
    public static boolean isCtrlKeyHeld(InputEvent ev) {
    	return (ev.getModifiers() & ActionEvent.CTRL_MASK) == ActionEvent.CTRL_MASK;
    }
    
    public static void mouseEventOutput(String eventDescription, MouseEvent e) {
        println(eventDescription
                + " (" + e.getX() + "," + e.getY() + ")"
                + " with button "
                + e.getButton()
                + " detected on "
                + e.getComponent().getClass().getName());
    }
    
    public static MouseEvent popupEvent = null;
    
    /**
     *
     */
    public static class PopupAdapter extends DropdownAdapter
    {
        public PopupAdapter(JPopupMenu popup) {
        	super(popup);
        }

        @Override
        protected void maybeShowPopup(MouseEvent e) {
        	if (e.isPopupTrigger()) {
        		popupEvent = e;
            	popupMenu.show(e.getComponent(), e.getX(), e.getY());
            }
        }
    }

	public static JComponent attachDropdownMenu(JComponent component, JPopupMenu popupMenu) {
    	component.setComponentPopupMenu(popupMenu);
    	component.addMouseListener(new DropdownAdapter(popupMenu));
    	return component;
	}
	
    public static JComponent addDropdownAdapter(JComponent component, DropdownAdapter dropdownAdapter) {
    	component.setComponentPopupMenu(dropdownAdapter.popupMenu);
    	component.addMouseListener(dropdownAdapter);
    	return component;
	}
	
    public static JComponent removeDropdownAdapter(JComponent component, DropdownAdapter dropdownAdapter) {
    	component.setComponentPopupMenu(null);
    	component.removeMouseListener(dropdownAdapter);
    	return component;
	}
	
	/**
	 * @param <C>
	 * @param container
	 * @param cmd
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static <C extends AbstractButton> C findButtonByActionCommand(Container container, String cmd) {
		for (Component comp : container.getComponents()) 
			if (comp instanceof AbstractButton) {
				try {
					C btn = (C)comp;
					String command = btn.getActionCommand();
					if (command != null && command.equals(cmd)) 
						return btn;
				} catch (Exception e) {
					break;
				}
			}
			else if (comp instanceof Container) {
				C btn = SwingUtil.<C>findButtonByActionCommand((Container)comp, cmd);
				if (btn != null)
					return btn;
			}
		
		return null;
	}

	public static void addSelectAllObserver(JTextComponent jtc) {
	    jtc.addFocusListener(new FocusListener() {
	        public void focusGained(FocusEvent e) {
	            ((JTextComponent)e.getSource()).selectAll();
	        }
	        public void focusLost(FocusEvent arg0) {    
	        }
	    });
	}
	
	public static void setLongText(JTextComponent tc, String text) {
		tc.setText(text);
		int length = tc.getDocument().getLength();
		tc.setCaretPosition(length);
	}
	
	public static String trimPath(String path, int width, Font font, JComponent jc) {
        FontMetrics fm = jc.getFontMetrics(font);
        if (width > 0 && fm != null) {
            int strWidth = fm.stringWidth(path);
            if (strWidth > width) {
                StringBuilder sb = new StringBuilder(path);
                String prefix = "...";
                while (fm.stringWidth(prefix + sb.toString()) > width) {
                    sb.delete(0, 1);
                }
                path = prefix + sb.toString();
            }
        }
        return path;
	}
	
	public static int[] textMeasures(String text, Font font) {
		AffineTransform affinetransform = new AffineTransform();     
		FontRenderContext frc = new FontRenderContext(affinetransform, true, true);     
		int textwidth = (int)(font.getStringBounds(text, frc).getWidth());
		int textheight = (int)(font.getStringBounds(text, frc).getHeight());
		return ints(textwidth, textheight);
	}
	
	public static Font monoSpaced(Object... params) {
		return new Font("Monospaced", param(0, 0, params), param(12, 1, params));
	}
	
	public static void adjustButtonSize(AbstractButton btn, int...tlbr) {
		String text = btn.getText();
		if (text.length() > -1) {
			Insets insets = isAvailable(3, tlbr) ?
					new Insets(param(0,0,tlbr), param(0,1,tlbr), param(0,2,tlbr), param(0,3,tlbr)) :
						btn.getInsets();
			int[] meas = textMeasures(text, btn.getFont());
			btn.setPreferredSize(new Dimension(2 + meas[0] + insets.left + insets.right, meas[1] + insets.top + insets.bottom));
			btn.setMargin(insets);
		}
	}
	
	public static void printContainer(String message, Container container, Object...params) {
		Boolean diag = param_Boolean(DIAG_OFF, 0, params);
		if (diag == null)
			return;
		StringBuilder sb = new StringBuilder();
		sb.append(message + " :" 
				+ TAB + stringValueOf(container.getName()) 
				+ TAB + stringValueOf(container.getClass())
				+ TAB + stringValueOf(container.getLayout())
				+ NEWLINE);
		iterateContainer(container, sb, 1);
		diag_print(diag, sb.toString());
	}

	public static void iterateContainer(Container container, final StringBuilder sb, int indent) {
		iterateComponents(container, new ComponentFunction<Integer>() {
			public Integer apply(Component comp, Object[] parms) {
				Integer indent = param_Integer(0, 0, parms);
				String indentString = "";
				for (int i = 0; i < indent; i++) 
					indentString += "    ";
				String size = comp.getSize().toString();
				sb.append(String.format("%s%s%s\tsize%s\n", 
						indentString, 
						comp.getName() == null ? "" : comp.getName() + " : ", 
						comp.getClass(),
						size.substring(size.indexOf("["))));
				return ++indent;
			}
		}, indent);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static class MapEditorComponent extends JTable
	{
		private DefaultTableModel model;
		
		public MapEditorComponent(final Map map, Object...options) {
			final Object[] keys = sortedSet(map.keySet()).toArray();
			model = new DefaultTableModel(keys.length, 2) {
				@Override
				public Object getValueAt(int row, int col) {
					if (col == 0)
						return keys[row];
					else
						return map.get(keys[row]);
				}
				@Override
				public void setValueAt(Object value, int row, int col) {
					if (col == 1)
						map.put(keys[row], value);
				}
				@Override
		        public Class<?> getColumnClass(int col) {
					if (col == 1) {
						return String.class;
					}
		            return super.getColumnClass(col);
		        }
				@Override
				public boolean isCellEditable(int row, int col) {
					if (col == 0)
						return false;
					else
						return true;
				}
			};
			setModel(model);
			setName("table");
			column0background = getTableHeader().getBackground();
			setTableHeader(null);
			setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY, 1));
			setRowSelectionAllowed(false);
			setColumnSelectionAllowed(false);
			getColumnModel().getColumn(0).setCellRenderer(new CustomRenderer());
			editors = new TableCellEditor[keys.length];
			for (int i = 0; i < keys.length; i++) 
				if (isAvailable(i, options)) {
					Object[] items = (Object[]) options[i];
					editors[i] = new DefaultCellEditor(new JComboBox(items));
				}
		}

		TableCellEditor[] editors;
		
		@Override
		public TableCellEditor getCellEditor(int row, int col)
		{
		    TableCellEditor editor = editors[row];
		    if (editor != null)
		        return editor;
		    else
		    	return super.getCellEditor(row,col);
		}
		
		Color column0background;

		class CustomRenderer extends DefaultTableCellRenderer 
		{
		    public Component getTableCellRendererComponent(JTable table, Object value, 
		    		boolean isSelected, boolean hasFocus, int row, int column)
		    {
		        Component cellComponent = super.getTableCellRendererComponent(table, value, 
		        		isSelected, hasFocus, row, column);
		        cellComponent.setBackground(column0background);
		        return cellComponent;
		    }
		}
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static DefaultListModel defaultListModel(final Collection<?> collection) {
		return new DefaultListModel() { 
			{
				for (Object item : collection) 
					addElement(item);
			}
		};
	}
	
	@SuppressWarnings("rawtypes")
	public static class Memory extends DefaultComboBoxModel
	{
		@SuppressWarnings({ "unchecked" })
		public static void update(final JComboBox cb, Object...params)
		{
			Object value = param(null, 0, params);
			Object value2 = param(null, 1, params);
			
			if (value instanceof Boolean) {
				Memory model = new Memory(stringValueOf(value2));
				cb.setModel(model);
				if ((Boolean)value && model.getSize() > 0) {
					Object el = model.getElementAt(0);
					model.setSelectedItem(el);
				}
		        final JTextField tf = comboEdit(cb);
				tf.addMouseListener(newPopupAdapter(
					objects("+", 
		        		newMenu("Memory", 
		        			objects("add", new ActionListener() {
		        	        	public void actionPerformed(ActionEvent ae) {
		        	        		Memory.update(cb, tf.getText());
		        	        	}
		        	        }), 
		        	        objects("remove", new ActionListener() {
		        	        	public void actionPerformed(ActionEvent ae) {
		        	        		Memory.update(cb, null, tf.getText());
		        	        	}
		        	        })
		        		)
			        )
		        ));
				cb.setEditable(true);
			}
			else {
				boolean add = value != null;
				Object removal = add ? value : value2;
				
				Memory memory = (Memory)cb.getModel();
				if (memory.contains(removal)) 
					cb.removeItem(removal);
				
				if (add) {
					cb.addItem(value);
					cb.setSelectedItem(value);
				}
			}
		}
		
		ArrayDeque<Object> deque = null;
	
		int capacity = 10;
		
		public int getCapacity() {
			return capacity;
		}
	
		public void setCapacity(int capacity) {
			this.capacity = capacity;
		}
	
		String name = "";
	
		public String getName() {
			return name;
		}
	
		public void setName(String name) {
			this.name = name;
		}
	
		public Memory() {
		}
		
		public Memory(String name, Object... params) {
			this.name = name;
			
			capacity = getSetting(name + ".memory.capacity", capacity);
			ValList values = (ValList) getListSetting(name + ".memory", vlist());
			deque = new ArrayDeque<Object>(values);
	
			for (Object p : params)
				addElement(p);
		}
		
		public void contentsChanged() {
			Object[] values = deque.toArray();
			putListSetting(name + ".memory", asList(values));
		}
	
	    Object selectedObject = null;
	    
		@Override
	    public void setSelectedItem(Object anItem) {
	    	super.setSelectedItem(anItem);
	    	selectedObject = anItem;
	    }
	    
		public boolean contains(Object o) {
			return deque.contains(o);
		}
		
		@Override
		public int getSize() {
			return deque.size();
		}
	
		@Override
		public Object getElementAt(int index) {
			Object el = null;
			Iterator<Object> it = deque.iterator();
			int i = 0;
			while (i <= index && it.hasNext()) {
				el = it.next();
				if (i >= index)
					break;
				i++;
			}
			return el;
		}
	
		@Override
		public void addElement(Object obj) {
			deque.addFirst(obj.toString());
			while (getSize() > capacity)
				deque.removeLast();
			
			fireIntervalAdded(this, 1, 1);
	        if ( getSize() == 1 && selectedObject == null && obj != null ) {
	            setSelectedItem(obj);
	        }
	        
	        contentsChanged();
	    }
	
		@Override
		public void removeElement(Object obj) {
			Iterator<Object> it = deque.iterator();
			int index = 0;
			while (it.hasNext()) {
				Object el = it.next();
				if (obj.equals(el)) {
					deque.remove(el);
					fireIntervalRemoved(this, index, index);
					break;
				}
				index++;
			}
	        
	        contentsChanged();
		}
	
		@Override
		public void insertElementAt(Object obj, int index) {
			throw new UnsupportedOperationException("Not implemented");
		}
	
		@Override
		public void removeElementAt(int index) {
			throw new UnsupportedOperationException("Not implemented");
		}
	
		@Override
		public void removeAllElements() {
			throw new UnsupportedOperationException("Not implemented");
		}
	
		@Override
		public String toString() {
			Writer writer = write(null, "[");
			writer = write_assoc(writer, "name", this.name);
			writer = write_assoc(writer, "capacity", this.capacity, 1);
			writer = write_assoc(writer, "deque", this.deque, 1);
			return write(writer, "]").toString();
		}
		
	}
	
	public static boolean hasNoSelection(JTable table) {
		ListSelectionModel sm = table.getSelectionModel();
		return sm.isSelectionEmpty();
	}
	
	public static void setColumnWidthsAsPercentages(JTable table, Object...percentages) {
		double factor = 10000;
		TableColumnModel model = table.getColumnModel();
		int columnCount = model.getColumnCount();
		if (columnCount > 0) {
			Double percentage = 1.0 / columnCount;
			for (int i = 0; i < columnCount; i++) {
				TableColumn column = model.getColumn(i);
				double val = param_Double(percentage, i, percentages) * factor;
				column.setPreferredWidth((int) val);
			}
		}
	}
	
    public static JScrollPane scrollableViewport(JTable table, Object...params) {
    	Dimension size = param(null,0,params);
		if (size != null)
			table.setPreferredScrollableViewportSize(size);
		boolean fills = param_Boolean(false, 1, params);
		table.setFillsViewportHeight(fills);
    	return new JScrollPane(table);
	}
	
    public static void selectRowAndScrollToVisible(JTable table, int...rows) {
    	if (!isAvailable(0, rows))
    		return;
    	int row1;
    	if (rows.length < 2)
    		row1 = rows[0];
    	else
    		row1 = rows[1];
    	table.getSelectionModel().setSelectionInterval(rows[0], row1);
    	Rectangle cellRect = table.getCellRect(rows[0], 0, true);
    	table.scrollRectToVisible(cellRect);
    }

	public static void setMaximumDimension(Component component, Integer...fac) {
		Dimension size = component.getPreferredSize();
		int fWidth = param(1, 0, fac);
		int fHeight = param(1, 1, fac);
		component.setMaximumSize(new Dimension(size.width * fWidth, size.height * fHeight));
	}

	public static Dimension scaledDimension(Dimension size, Double...scaleFac) {
		Double fWidth = param(1.0, 0, scaleFac);
		Double fHeight = param(1.0, 1, scaleFac);
		size = new Dimension(
				(int)Math.round(size.width * fWidth), 
				(int)Math.round(size.height * fHeight));
		return size;
	}

	public static void scaleSize(Component component, Double...scaleFac) {
		Dimension size = scaledDimension(component.getSize(), scaleFac);
		if (component instanceof JComponent)
			component.setPreferredSize(size);
		else
			component.setSize(size);
	}

	public static JPanel surroundingBox(JComponent contents, String title, Object...params) {
		JPanel box = new JPanel();
		int titleJustification = param_Integer(TitledBorder.DEFAULT_JUSTIFICATION, 0, params);
		int titlePosition = param_Integer(TitledBorder.DEFAULT_POSITION, 1, params) ;
		box.setBorder(
			BorderFactory.createTitledBorder(
				BorderFactory.createEtchedBorder(),
				title,
				titleJustification,
				titlePosition
			)
		);
		box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));
		box.add( contents );
		box.add( Box.createVerticalStrut(10) );
		return box;
	}
	
	public static Dimension sizeOfScreen() {
		return Toolkit.getDefaultToolkit().getScreenSize();
	}
	
	public static Point centerOfScreen() {
		Dimension screenSize = sizeOfScreen();
		return new Point(screenSize.width / 2, screenSize.height / 2);
	}
	
	public static JSplitPane splitPane(int orientation, Object...params) {
		JSplitPane splitPane = new JSplitPane(orientation);
		splitPane.setResizeWeight(0.5);
		splitPane.setOneTouchExpandable(true);
		splitPane.addPropertyChangeListener("dividerLocation", 
				param((PropertyChangeListener)null,0,params));
		return splitPane;
	}
	
	public static String readFromUrlWithProgress(String url, String encoding) throws IOException {
		InputStream is = null;
		try {
			is = new URL(url).openStream();
			ProgressMonitorInputStream pmis = new ProgressMonitorInputStream(null, url, is);
			BufferedReader rd = new BufferedReader(new InputStreamReader(pmis, Charset.forName(encoding)));
			return readAll(rd);
		} finally {
			if (is != null)
				is.close();
		}
	}
	
	public static int urlContentLength(String fileURL) {
		try {
			URL url = new URL(fileURL);
			URLConnection urlConnection = url.openConnection();
			if (urlConnection != null) {
				return urlConnection.getContentLength();
			}
		} catch (Exception e) {
			Log.e(TAG, "urlContentLength", e);
		}
		return -1;
	}
	
	public static class ScanTask extends Task<Void>
	{
		public ScanTask(Component parent, String title, InputStream is, 
				Job<Void> goAlong, 
				Job<Void> followUp, 
				Object...params) {
			super(null, followUp, params);
			this.goAlong = goAlong;
			if (followUp != null)
				sb = new StringBuilder();
			pmis = new ProgressMonitorInputStream(parent, title, is);
			progressMonitor = pmis.getProgressMonitor();
			progressMonitor.setMillisToDecideToPopup(0);
			progressMonitor.setMillisToPopup(0);
			progressMonitor.setProgress(0);
			in = new Scanner(pmis);
			execute();
		}
		
		ProgressMonitorInputStream pmis;
		ProgressMonitor progressMonitor;
		
		@Override
		protected Void doInBackground() throws Exception {
			while (in.hasNextLine()) {
				String line = in.nextLine();
				if (goAlong != null)
					goAlong.perform(null, objects(line, params));
				if (sb != null) {
					sb.append(line);
					sb.append("\n");
				}
			}
			in.close();
			return null;
		}

		Scanner in;
		Job<Void> goAlong;
		StringBuilder sb = null;
		 
		@Override
		protected void done() {
			if (followUp != null) 
				try {
					followUp.perform(null, objects(sb.toString(), params));
					cancel(true);
				} catch (Exception e) {
					Log.e(TAG, "follow-up", e);
				}
		}
	}
}

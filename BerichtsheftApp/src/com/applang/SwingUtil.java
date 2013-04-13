package com.applang;

import java.awt.AWTEvent;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Frame;
import java.awt.KeyboardFocusManager;
import java.awt.LayoutManager;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.AWTEventListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.ItemListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EventListener;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;

import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
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
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.WindowConstants;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.text.JTextComponent;

public class SwingUtil
{
	public interface ComponentFunction<T> {
		public T apply(Component comp, Object[] parms);
	}
	
	public static <T> Object[] iterateComponents(Container container, ComponentFunction<T> func, Object... params) {
		params = Util.reduceDepth(params);
		
		if (container != null) {
			Component[] components = params.length > 0 ? 
				container.getComponents() : 
				new Component[] {container};
				
			for (Component comp : components)
			{
				T t = func.apply(comp, params);
				
				if (comp instanceof Container)
					iterateComponents((Container)comp, func, t);
			}
		}
		
		return params;
	}
    
	/**
	 * @param container
	 * @param pattern
	 * @return	an array of those child <code>Component</code> objects of container which names match the given pattern
	 */
	public static Component[] findComponents(Container container, final String pattern) {
		final ArrayList<Component> al = new ArrayList<Component>();
		
		iterateComponents(container, new ComponentFunction<Object[]>() {
			public Object[] apply(Component comp, Object[] parms) {
				String name = comp.getName();
				if (name != null && name.matches(pattern))
					al.add(comp);
				
				return parms;
			}
		}, null, null);
		
		return al.toArray(new Component[al.size()]);
	}
    
	/**
	 * @param <C> the type of the return value (derived from <code>Component</code>)
	 * @param container	the containing <code>Component</code>
	 * @param key	the name of the <code>Component</code> searched for in the container including descending components
	 * @return	the <code>Component</code> found if unequal <code>null</code> otherwise nothing was found
	 */
	@SuppressWarnings("unchecked")
	public static <C extends Component> C findComponent(Container container, String key) {
		Component[] comps = findComponents(container, key);
		if (comps.length > 0) 
			try {
				return (C)comps[0];
			} catch (Exception e) {}
		
		return null;
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
			func.apply(component, Util2.arrayextend(params, true, timing));
		}
		finally {
			timing.finalize();
		}
		
		return timing.millis;
	}

	public static class Deadline extends SwingWorker<Void, Void>
	{
		public Deadline(Window wnd, Integer[] keyEvents) {
			this.wnd = wnd;
			this.keyEvents = keyEvents;
		}
		
		Window wnd;
		Integer[] keyEvents = null;

		@Override
		protected Void doInBackground(){
			Util2.println(String.format("deadline %d ms", value));

	        waiting(wnd, new ComponentFunction<Void>() {
				public Void apply(Component comp, Object[] parms) {
					Timing timing = (Timing)parms[0];
					
					while (timing.current() < value && !isCancelled()) 
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
			
			if (!Util.nullOrEmpty(keyEvents)) 
				doKeyEvents();
			else if (wnd != null) 
				pullThePlug(wnd);
		}
		
		public void cancel() {
			if (this.cancel(true))
				Util2.println("deadline cancelled");
			
			started = false;
		}

		void doKeyEvents() {
			try {
				Robot robot = new Robot();
				for (int i = 0; i < keyEvents.length; i++) {
					if (keyEvents[i] != null) {
						robot.keyPress(keyEvents[i]);
					}
				}
				
				robot.delay(delay);
				
				for (int i = 0; i < keyEvents.length; i++) {
					if (keyEvents[i] != null) {
						robot.keyRelease(keyEvents[i]);
					}
				}
				
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		public static int delay = 1000;
		public static int value = 1000;
		
		public static Deadline start(Window wnd, Integer... keyEvents) {
			if ((wnd == null && keyEvents.length < 1))
				return null;
				
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
		
		public static void finish() {
			if (started) {
				try {
					SwingUtilities.invokeAndWait(doEvents);
				} catch (Exception e) {
					e.printStackTrace();
				}
				
				started = false;
				
				Util2.println("deadline finished");
			}
		}

		static Runnable doEvents = new Runnable() {
			public void run() {
				Deadline deadline = new Deadline(null, null);
				deadline.execute();
			}
		};
	}
	
	public static class Bounds extends Rectangle
	{
		public Bounds() {}
		
		public Bounds(Window window, Object... params) {
			super(window != null ? 
				window.getBounds() : 
				Util.param(new Rectangle(300,300,400,400), 0, params));
		}
		
		public static Rectangle load(Window window, 
				String category, String title,
				Object... params)
		{
			String key = key(category, title);
			Bounds rect = new Bounds(null, params);
			for (MatchResult mr : Util.findAllIn(Util2.getSetting(key, ""), Pattern.compile("(\\w+)=(\\d+)"))) {
				if ("x".equals(mr.group(1)))
					rect.x = Util.toInt(-1, mr.group(2));
				else if ("y".equals(mr.group(1)))
					rect.y = Util.toInt(-1, mr.group(2));
				else if ("width".equals(mr.group(1)))
					rect.width = Util.toInt(-1, mr.group(2));
				else if ("height".equals(mr.group(1)))
					rect.height = Util.toInt(-1, mr.group(2));
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
				Object... params)
		{
			String key = key(category, title);
			Util2.putSetting(key, Util2.toString("", new Bounds(window, params)));
		}
	}

	static boolean finished = false;
	
	public static void showFrame(Component relative, 
			String title, 
    		ComponentFunction<Component[]> assembleUI,
    		ComponentFunction<Component[]> arrangeUI,
    		final ComponentFunction<Component[]> completeUI,
    		boolean deadline, 
    		Integer... keyEvents)
	{
		try {
			final JFrame frame = new JFrame(title);
			frame.setName(title);
			
			if (assembleUI != null) {
				Component[] widgets = assembleUI.apply(frame, null);
				if (widgets != null)
					if (widgets.length == 1 && widgets[0] instanceof Container)
						frame.setContentPane((Container)widgets[0]);
					else
						for (Component widget : widgets)
							frame.getContentPane().add(widget);
			}
			
			frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
			frame.setLocationRelativeTo(relative);
			frame.pack();
			
			frame.setVisible(true);
			
			if (arrangeUI != null)
				arrangeUI.apply(frame, null);
			
			frame.addWindowListener(new WindowAdapter() {
				public void windowClosing(WindowEvent event) {
					if (completeUI != null)
						completeUI.apply(frame, null);
					
					finished = true;
				}
			});
			
			if (deadline || keyEvents.length > 0) {
				Deadline.start(
						deadline ? frame : null, 
						keyEvents);
				
				finished = false;
				
				while (!finished) {
					Thread.yield();
				}
			}
		} catch (Exception e) {
			handleException(e);
		}
	}
	
    /**
     * closes the <code>Window</code> programmatically
     * @param wnd
     */
    public static void pullThePlug(Window wnd) {
        WindowEvent wev = new WindowEvent(wnd, WindowEvent.WINDOW_CLOSING);
        Toolkit.getDefaultToolkit().getSystemEventQueue().postEvent(wev);
    }
	
	public static int dialogResult = -1;
	
    public static int showDialog(Component parent, Component relative, 
    		String title, 
    		ComponentFunction<Component[]> assembleUI,
    		ComponentFunction<Component[]> arrangeUI,
    		final ComponentFunction<Component[]> completeUI,
    		Modality modality, 
    		Integer... keyEvents)
    {
    	dialogResult = -1;
    	
		Frame frame = JOptionPane.getFrameForComponent(parent);
		final JDialog dlg = new JDialog(frame, title, modality.flags(Modality.MODAL));
		
		if (assembleUI != null) {
			Component[] widgets = assembleUI.apply(dlg, null);
			if (widgets != null)
				if (widgets.length == 1 && widgets[0] instanceof Container)
					dlg.setContentPane((Container) widgets[0]);
				else
					for (Component widget : widgets)
						dlg.getContentPane().add(widget);
		}
		
		boolean deadline = modality.flags(Modality.TIMEOUT);
		if (deadline)
			Deadline.start(dlg);
		
		dlg.pack();
		dlg.setLocationRelativeTo(relative);
		dlg.setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);

		if (!deadline)
			Deadline.start(null, keyEvents);
		
		dlg.setVisible(true);
		
		if (arrangeUI != null)
			arrangeUI.apply(dlg, null);
		
		dlg.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent event) {
				if (completeUI != null)
					completeUI.apply(dlg, null);
			}
		});
    	
		Util2.noprintln("dialogResult", dialogResult);
		
		return dialogResult;
	}
	
    public enum Modality {
    	NONE(0), MODAL(4), TIMEOUT(8), MODAL_TIMEOUT(12);

        int index;   

        Modality(int index) {
            this.index = index;
        }

        public boolean flags(Modality flag) { 
            return (index & flag.index) > 0; 
        }

        //
        // Option types
        //
        /*
         * Type meaning Look and Feel should not supply any options -- only
         * use the options from the <code>JOptionPane</code>.
         */
        public static final int         DEFAULT_OPTION = -1;
        /* Type used for <code>showConfirmDialog</code>. */
        public static final int         YES_NO_OPTION = 0;
        /* Type used for <code>showConfirmDialog</code>. */
        public static final int         YES_NO_CANCEL_OPTION = 1;
        /* Type used for <code>showConfirmDialog</code>. */
        public static final int         OK_CANCEL_OPTION = 2;

        //
        // Return values.
        //
        /* Return value from class method if YES is chosen. */
        public static final int         YES_OPTION = 0;
        /* Return value from class method if NO is chosen. */
        public static final int         NO_OPTION = 1;
        /* Return value from class method if CANCEL is chosen. */
        public static final int         CANCEL_OPTION = 2;
        /* Return value form class method if OK is chosen. */
        public static final int         OK_OPTION = 0;
        /* Return value from class method if user closes window without selecting
         * anything, more than likely this should be treated as either a
         * <code>CANCEL_OPTION</code> or <code>NO_OPTION</code>. */
        public static final int         CLOSED_OPTION = -1;
    }
	
    public static int showOptionDialog(Component parent, 
    		final Object message, String title,
            int optionType, final int messageType,
            Icon icon,
            final Object[] options, final Object initialOption, 
            Integer... keyEvents) 
    {
    	int type = Math.abs(optionType);
    	if (type > 3) {
    		Modality modality = Modality.NONE;
    		modality.index = type & ~3;
    		final int option_Type = optionType < 0 ? -1 : type & 3;
    		
    		return showDialog(parent, parent, 
					title,
    				new ComponentFunction<Component[]>() {
						public Component[] apply(final Component dlg, Object[] parms) {
				    		final JOptionPane optionPane = new JOptionPane(message, 
				    				messageType,
				    				option_Type,
				    				null,
				    				options, 
				    				initialOption);
				            optionPane.addPropertyChangeListener(new PropertyChangeListener() {
				                public void propertyChange(PropertyChangeEvent e) {
				                    String prop = e.getPropertyName();

				                    if (dlg.isVisible() && optionPane.equals(e.getSource()) && 
				                    	JOptionPane.VALUE_PROPERTY.equals(prop))
				                    {
				                        Object value = optionPane.getValue();
				                        if (value == JOptionPane.UNINITIALIZED_VALUE) {
				                        	dialogResult = -1;
				                        	return;
				                        }
				                        else
				                        	optionPane.setValue(JOptionPane.UNINITIALIZED_VALUE);
				                        
				                        dialogResult = Arrays.asList(options).indexOf(value);
				                		dlg.setVisible(false);
				                	};
				                }
				            });
							return new Component[] {optionPane};
						}
					},
					null, null, 
					modality,
    				keyEvents);
    	}
    	else {
    		Deadline.start(null, keyEvents);
    		
    		parent = JOptionPane.getFrameForComponent(parent);
        	return JOptionPane.showOptionDialog(parent, 
        			message, title, 
        			optionType, messageType, 
        			icon, 
        			options, initialOption);
    	}
	}
	
    public static Object showInputDialog(Component parent, 
    		Object message, String title,
            int optionType, int messageType,
            Icon icon,
            Object[] selections, Object initialSelection, 
            Integer... keyEvents) 
    {
        Object[] options = selections;
        Object initialOption = initialSelection;
        
        Object[] widgets = null;
    	JTextComponent textComponent = null;
    	
		parent = JOptionPane.getFrameForComponent(parent);
		
    	switch (optionType) {
    	default:
    		Deadline.start(null, keyEvents);
    		
        	return JOptionPane.showInputDialog(parent, 
        			message, title, 
        			messageType, 
        			icon, 
        			selections, initialSelection);
        	
    	case JOptionPane.DEFAULT_OPTION:
    		widgets = new Object[] {
    				new JScrollPane(new JTextArea(message.toString()))};
    	    break;
    	    
    	case JOptionPane.YES_NO_OPTION:
    	case JOptionPane.YES_NO_CANCEL_OPTION:
    	case JOptionPane.OK_CANCEL_OPTION:
    		JTextField textField = new JTextField(initialSelection.toString());
    		textField.setHorizontalAlignment(JTextField.CENTER);
    		addFocusObserver(textField);
    		
	    	JLabel label = new JLabel(message.toString());
    	    label.setDisplayedMnemonic(KeyEvent.VK_I);
		    label.setLabelFor(textField);
    		
    		textComponent = textField;
		    
    	    initialOption = options.length > 0 ? options[0] : null;
    		widgets = new Object[] {label, textComponent};
    	    break;
    	}
	    
    	Object selection = null;
    	
	    int ans;
		switch (ans = showOptionDialog(parent, 
				widgets, title, 
				optionType,
				messageType,
				icon,
				options, initialOption, 
				keyEvents)) {
		case 0:
			if (textComponent != null) {
				selection = textComponent.getText();
				break;
			}

		default:
			if (optionType == JOptionPane.OK_CANCEL_OPTION)
				selection = "";
			else
				selection = ans;
			break;
		}
		
    	return selection;
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
			FileFilter fileFilter)
	{
		File dir = new File(Util.relativePath());
		try {
			dir = file.getParentFile();
		} catch (Exception e) {}
		
		JFileChooser chooser = new JFileChooser();
		chooser.setCurrentDirectory(dir);
	    chooser.setDialogTitle(title);
	    chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
	    if (fileFilter != null)
	    	setFileFilter(chooser, fileFilter);
		
	    if (Util.fileExists(file))
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
	    chooser.setAcceptAllFileFilterUsed(false);
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

	public static void addFocusObserver(JTextComponent jtc) {
	    jtc.addFocusListener(new FocusListener() {
	        public void focusGained(FocusEvent e) {
	            ((JTextComponent)e.getSource()).selectAll();
	        }
	        public void focusLost(FocusEvent arg0) {    
	        }
	    });
	}

	public static void addTabKeyForwarding(JComponent jc) {
	    jc.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_TAB) {
					e.consume();
					KeyboardFocusManager.getCurrentKeyboardFocusManager().focusNextComponent();
				}
				if (e.getKeyCode() == KeyEvent.VK_TAB && e.isShiftDown()) {
					e.consume();
					KeyboardFocusManager.getCurrentKeyboardFocusManager().focusPreviousComponent();
				}
			}
		});
	}

	@SuppressWarnings("rawtypes")
	public static JTextField comboEdit(JComboBox combo) {
		return (JTextField)combo.getEditor().getEditorComponent();
	}
	
	public static Container container = null;
	public static boolean underTest = false;

	public static boolean question(String text) {
		return JOptionPane.YES_OPTION == JOptionPane.showConfirmDialog(
				container, text, 
				"Question", 
				JOptionPane.YES_NO_OPTION);
	}

	public static void message(String text) {
		JLabel mess = findComponent(getRootContainer(container), "mess");
		if (mess != null)
			mess.setText(text);
		else if (underTest)
			Util2.println(text);
		else
			JOptionPane.showMessageDialog(container, text, "Message", JOptionPane.PLAIN_MESSAGE);
	}

	public static void handleException(Exception e) {
		if (e != null) 
        	message(e.getMessage());
	}
	
	public interface IActionType 
	{
		public int index();
	    public String iconName();
	    public String description();
	}

	public static ImageIcon iconFrom(String path) {
		if (Util.notNullOrEmpty(path)) {
			URL url = SwingUtil.class.getResource("/images/" + path);
			return new ImageIcon(url);
		}
		else
			return null;
	}
	
    public static class Action extends AbstractAction
    {
    	public static boolean actionBlocked = false;
    	
    	public static void blocked(Util.Job<Void> job, Object[] params) throws Exception {
    		actionBlocked = true;
    		
    		job.perform(null, params);
    		
    		actionBlocked = false;
    	}
    	
    	private IActionType type = null;
    	
        public IActionType getType() {
        	return type;
		}

		public void setType(IActionType type) {
			this.type = type;
			putValue(SMALL_ICON, iconFrom(this.type.iconName()));
			putValue(SHORT_DESCRIPTION, this.type.description());
//			putValue(MNEMONIC_KEY, KeyEvent.CHAR_UNDEFINED);
		}

		public Action(IActionType type) {
			super(null);
			setType(type);
        }

		public Action(String text) {
			super(text);
        }
        
        @Override
        public void actionPerformed(ActionEvent ae) {
        	if (actionBlocked || getType() == null)
        		return;
        	
        	message("");
        	Util2.println(type == null ? ae.getActionCommand() : type.toString());
        	
        	action_Performed(ae);
        }
        
        protected void action_Performed(ActionEvent ae) {
        	if (actionBlocked)
        		return;
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
        	Object[] parms = multi ? (Object[])Util.param(null, i, params) : params;
        	
        	String text = Util.paramString("-", 0, parms);
        	if ("-".equals(text)) {
        		group = (ButtonGroup)Util.param(null, 1, parms);
        		
        		if (Util.paramBoolean(true, 2, parms)) {
            		if (container instanceof JPanel) {
            			LayoutManager lm = container.getLayout();
            			int axis = lm instanceof BoxLayout ? 
            					((BoxLayout)lm).getAxis() : -1;
            			switch (axis) {
            			case BoxLayout.LINE_AXIS:
            			case BoxLayout.X_AXIS:
            				container.add(Box.createHorizontalStrut(Util.paramInteger(0, 3, parms)));
            				break;
            			case BoxLayout.PAGE_AXIS:
            			case BoxLayout.Y_AXIS:
            				container.add(Box.createVerticalStrut(Util.paramInteger(0, 3, parms)));
            				break;
            			default:
            				container.add(new JLabel(Util.paramString(" ", 3, parms)));
            				break;
            			}
            		}
            		else if (container instanceof JPopupMenu)
            			((JPopupMenu)container).addSeparator();
    				else
    					((JMenu)container).addSeparator();
        		}
        	}
        	else if ("+".equals(text)) {
        		container.add((JMenu)Util.param(null, 1, parms));
        	}
        	else if (Util.notNullOrEmpty(text)) {
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
        		
        		EventListener evl = (EventListener)Util.param(null, 1, parms);
        		if (evl instanceof ItemListener) 
                	button.addItemListener((ItemListener)evl);
        		if (evl instanceof ActionListener) 
        			button.addActionListener((ActionListener)evl);
        		
        		if (evl instanceof Action) {
        			Action act = (Action)evl;
					button.setAction(act);
					button.setText(act.getValue(Action.SHORT_DESCRIPTION).toString());
        		}
        		else {
	            	String name = Util.paramString("button" + i, 2, parms);
	                button.setName(name);
	                button.setActionCommand(name);
	                button.setToolTipText(Util.paramString("", 3, parms));
	                button.setEnabled(Util.paramBoolean(true, 4, parms));
	            	button.setMnemonic(Util.paramInteger(0, 5, parms));
	                button.setSelected(Util.paramBoolean(false, 6, parms));
	                KeyStroke ks = Util.param(KeyStroke.getKeyStroke(' '), 7, parms);
	                if (Util.isType(JMenuItem.class, button))
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
        	Object[] parms = multi ? (Object[])Util.param(null, i, params) : params;
        	
	    	PopupMenuListener popupMenuListener = Util.param(null, 8, parms);
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
    
    public static void mouseEventOutput(String eventDescription, MouseEvent e) {
        Util2.println(eventDescription
                + " (" + e.getX() + "," + e.getY() + ")"
                + " with button "
                + e.getButton()
                + " detected on "
                + e.getComponent().getClass().getName());
    }
    
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
	
}

package com.applang;

import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.KeyboardFocusManager;
import java.awt.LayoutManager;
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
import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.EventListener;

import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButton;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.text.JTextComponent;

import com.applang.Util.Job;
import com.applang.berichtsheft.ui.components.NotePicker;

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
			func.apply(component, Util.arrayextend(params, true, timing));
		}
		finally {
			timing.finalize();
		}
		
		return timing.millis;
	}
	
	public static int dialogResult = -1;
	
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
			System.out.println(text);
		else
			JOptionPane.showMessageDialog(container, text, "Message", JOptionPane.PLAIN_MESSAGE);
	}

	public static void handleException(Exception e) {
		if (e != null) 
        	message(e.getMessage());
	}
	
	public interface ActionType 
	{
		public int index();
	    public String iconName();
	    public String description();
	}

	public static ImageIcon iconFrom(String path) {
		if (Util.notNullOrEmpty(path)) {
			URL url = NotePicker.class.getResource("/images/" + path);
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
    		
    		job.dispatch(null, params);
    		
    		actionBlocked = false;
    	}
    	
    	private ActionType type = null;
    	
        public ActionType getType() {
        	return type;
		}

		public void setType(ActionType type) {
			this.type = type;
			putValue(SMALL_ICON, iconFrom(this.type.iconName()));
			putValue(SHORT_DESCRIPTION, this.type.description());
//			putValue(MNEMONIC_KEY, KeyEvent.CHAR_UNDEFINED);
		}

		public Action(ActionType type) {
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
        	System.out.println(type == null ? ae.getActionCommand() : type.toString());
        	
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
		//	System.out.println("Canceled");
		}
		
		public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
		//	System.out.println("Becoming invisible");
		}
		
		public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
		//	System.out.println("Becoming visible");
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

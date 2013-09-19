package com.applang.berichtsheft.components;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.File;
import java.sql.Connection;
import java.sql.Statement;

import javax.swing.AbstractButton;
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JToolBar;

import org.w3c.dom.Element;

import com.applang.berichtsheft.plugin.BerichtsheftPlugin;

import static com.applang.Util.*;
import static com.applang.Util2.*;
import static com.applang.SwingUtil.*;

public class ActionPanel extends ManagerBase
{
	public static void createAndShowGUI(String title, 
			final Dimension preferred, 
			final ActionPanel actionPanel, 
			final Component target, 
			final Object... params)
	{
		showFrame(null, title, 
				new UIFunction() {
					public Component[] apply(Component comp, Object[] parms) {
						JToolBar top = new JToolBar();
						top.setName("top");
						JToolBar bottom = new JToolBar();
						bottom.setName("bottom");
						bottom.setFloatable(false);
						messageBox(bottom);
						
						JFrame frame = (JFrame) comp;
						Container contentPane = frame.getContentPane();
						if (preferred != null)
							contentPane.setPreferredSize(preferred);
						
						actionPanel.joinContainer(top);
						contentPane.add(top, BorderLayout.PAGE_START);
						contentPane.add(bottom, BorderLayout.PAGE_END);
						
						contentPane.add(target, BorderLayout.CENTER);
						return null;
					}
				}, 
				new UIFunction() {
					public Component[] apply(Component comp, Object[] parms) {
						return null;
					}
				}, 
				new UIFunction() {
					public Component[] apply(Component comp, Object[] parms) {
						actionPanel.finish(params);
						return null;
					}
				}, 
				false);
	}
	
	protected void start(Object... params) {
		Settings.load();
	}
	
	public void finish(Object... params) {
		Settings.save();
	}
	
	public enum ActionType implements CustomActionType
	{
		CALENDAR	(0, "berichtsheft.action-CALENDAR"), 
		PREVIOUS	(1, "berichtsheft.action-PREVIOUS"), 
		NEXT		(2, "berichtsheft.action-NEXT"), 
		DATABASE	(3, "berichtsheft.action-DATABASE"), 
		DOCUMENT	(4, "berichtsheft.action-DOCUMENT"), 
		ADD			(5, "berichtsheft.action-ADD"), 
		UPDATE		(6, "berichtsheft.action-UPDATE"), 
		DELETE		(7, "berichtsheft.action-DELETE"), 
		SPELLCHECK	(8, "berichtsheft.action-SPELLCHECK"), 
		PICK		(9, "berichtsheft.action-PICK"), 
		DATE		(10, "berichtsheft.action-DATE"), 
		CATEGORY	(11, "berichtsheft.action-CATEGORY"), 
		IMPORT		(12, "berichtsheft.action-IMPORT"), 
		TEXT		(13, "berichtsheft.action-TEXT"), 
		ANDROID		(14, "berichtsheft.action-ANDROID"), 
		ACTIONS		(15, "Actions"); 		//	needs to stay last !
		
		private final int index;   
	    private final String resourceName;
	    
	    ActionType(int index, String resourceName) {
	    	this.index = index;
	        this.resourceName = resourceName;
	    }

		@Override
	    public String resourceName()   { return resourceName; }
		@Override
	    public int index() { return index; }
		@Override
		public String iconName() {
			return BerichtsheftPlugin.getProperty(resourceName + ".icon");
		}
		@Override
		public String description() {
			return BerichtsheftPlugin.getProperty(resourceName.concat(".label"));
		}
	}

    protected String dbName;
	protected String caption;

	public ActionPanel(DataComponent dataComponent, Object... params) {
		this.dataComponent = dataComponent;
		if (dataComponent instanceof TextComponent) {
			this.textArea = (TextComponent) dataComponent;
			setupTextArea();
		}
		dbName = com.applang.Util.paramString("", 0, params);
		caption = com.applang.Util.paramString("Database", 1, params);
		
		setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));
		
		start(params);
	}
	
	public void joinContainer(Container container, Object...params) {
		Object param0 = param(null, 0, params);
		if (param0 instanceof String)
			for (int i = 0; i < buttons.length; i++) {
				AbstractButton btn = buttons[i];
				if (btn != null) {
					Object name = btn.getAction().getValue(CustomAction.NAME);
					if (asList(params).indexOf(name) > -1)
						container.add(btn);
				}
			}
		else
			container.add(this, param0);
	}

	protected AbstractButton[] buttons = new AbstractButton[1 + ActionType.ACTIONS.index()];
	
	public void addButton(Container container, int index, CustomAction customAction) {
		if (index > -1 && index < buttons.length)
			container.add(buttons[index] = BerichtsheftPlugin.makeCustomButton(customAction, false));
	}
	
	public CustomAction getAction(int index) {
		if (index > -1 && index < buttons.length)
			return (CustomAction) buttons[index].getAction();
		else
			return null;
	}
	
	public void setAction(int index, CustomAction customAction) {
		if (index > -1 && index < buttons.length)
			buttons[index].setAction(customAction);
	}
	
	public void clickAction(int index) {
		if (index > -1 && index < buttons.length)
			buttons[index].doClick();
	}
	
	public void enableAction(int index, boolean enabled) {
		buttons[index].getAction().setEnabled(enabled);
	}

	protected DataComponent dataComponent = null;
	protected TextComponent textArea = null;

	protected boolean hasTextArea() {
		return this.textArea != null;
	}

	public void setText(String text) {
		if (hasTextArea()) 
			this.textArea.setText(text);
	}

	public String getText() {
		return hasTextArea() ? this.textArea.getText() : null;
	}
    
	boolean dirty = false;
	
	public boolean isDirty() {
		return dirty;
//		return hasTextArea() && textArea.isDirty();
	}

	public void setDirty(boolean dirty) {
		this.dirty = dirty;
//		if (hasTextArea()) textArea.setDirty(dirty);
	}
	
	protected void setupTextArea() {
		if (hasTextArea()) {
			this.textArea.addKeyListener(new KeyAdapter() {
				@Override
				public void keyTyped(KeyEvent e) {
					setDirty(true);
				}
			});
		}
	}

	protected String chooseDatabase(String dbName) {
		if (dbName != null) {
			File file = chooseFile(true, this, caption, new File(dbName));
			if (file != null) 
				dbName = file.getPath();
		}
		return dbName;
	}
	
	DataBaseConnect dbConnect = new DataBaseConnect() {
		@Override
		public void preConnect(String path) throws Exception {
			beforeConnecting(path);
		}
		@Override
		public void postConnect() throws Exception {
			afterConnecting();
		}
	};

	public Statement getStmt() {
		return dbConnect.getStmt();
	}

	public String getScheme() {
		return dbConnect.getScheme();
	}

	public Connection getCon() {
		return dbConnect.getCon();
	}

	public boolean openConnection(String dbPath, Object... params) throws Exception {
		return dbConnect.open(dbPath, params);
	}
	
	protected void beforeConnecting(String path) throws Exception {
		if (path.endsWith("*"))
			throw new Exception(String.format("'%s' is not a legal database name", path));
	}
	
	protected void afterConnecting() throws Exception {
	}
	
	protected void updateOnRequest(boolean ask) {
	}

	@Override
	protected Element select(String... params) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected void updateItem(boolean update, Object... params) {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected boolean addItem(boolean refresh, String item) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	protected boolean removeItem(String item) {
		// TODO Auto-generated method stub
		return false;
	}

}

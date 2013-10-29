package com.applang.components;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.File;
import java.sql.Connection;
import java.sql.Statement;

import javax.swing.AbstractButton;
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JToolBar;

import com.applang.berichtsheft.BerichtsheftApp;
import com.applang.berichtsheft.plugin.BerichtsheftPlugin;

import static com.applang.Util.*;
import static com.applang.Util2.*;
import static com.applang.SwingUtil.*;

public class ActionPanel extends ManagerBase<Object>
{
	public static void createAndShowGUI(String title, 
			final Dimension preferred, 
			final ActionPanel actionPanel, 
			final Component target, 
			int modality, 
			final Object... params)
	{
		showFrame(null, title, 
				new UIFunction() {
					public Component[] apply(Component comp, Object[] parms) {
						Container contentPane = new JPanel(new BorderLayout());
						southStatusBar(contentPane);
						actionPanel.joinContainer(northToolBar(contentPane));
						contentPane.add(target, BorderLayout.CENTER);
						Container secondPane = param(null, 0, params);
						if (secondPane != null) {
							JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
							splitPane.setResizeWeight(0.5);
							splitPane.setOneTouchExpandable(true);
							splitPane.setTopComponent(contentPane);
							splitPane.setBottomComponent(secondPane);
							contentPane = splitPane;
						}
						if (preferred != null)
							contentPane.setPreferredSize(preferred);
						return components(contentPane);
					}
				}, 
				new UIFunction() {
					public Component[] apply(Component comp, Object[] parms) {
						actionPanel.start();
						return null;
					}
				}, 
				new UIFunction() {
					public Component[] apply(Component comp, Object[] parms) {
						actionPanel.finish();
						return null;
					}
				}, 
				modality);
	}
	
	protected void start(Object... params) {
		BerichtsheftApp.loadSettings();
	}
	
	public void finish(Object... params) {
		Settings.save();
	}
	
	public enum ActionType implements CustomActionType
	{
		CALENDAR	(0, "manager.action-CALENDAR"), 
		PREVIOUS	(1, "manager.action-PREVIOUS"), 
		NEXT		(2, "manager.action-NEXT"), 
		DATABASE	(3, "manager.action-DATABASE"), 
		INSERT		(4, "manager.action-INSERT"), 
		FIRST		(5, "manager.action-FIRST"), 
		UPDATE		(6, "manager.action-UPDATE"), 
		LAST		(7, "manager.action-LAST"), 
		DELETE		(8, "manager.action-DELETE"), 
		PICK		(9, "manager.action-PICK"), 
		DATE		(10, "manager.action-DATE"), 
		CATEGORY	(11, "manager.action-CATEGORY"), 
		IMPORT		(12, "manager.action-IMPORT"), 
		TEXT		(13, "manager.action-TEXT"), 
		ANDROID		(14, "manager.action-ANDROID"), 
		ACTIONS		(15, "Actions"); 		//	needs to stay last !
		
		private final int index;   
	    private final String resourceName;
	    
	    ActionType(int index, String resourceName) {
	    	this.index = index;
	        this.resourceName = resourceName;
	    }

	    @Override
	    public int index() { return index; }
		@Override
	    public String resourceName()   { return resourceName; }
		@Override
		public String iconName() {
			return BerichtsheftPlugin.getProperty(resourceName + ".icon");
		}
		@Override
		public String description() {
			return BerichtsheftPlugin.getProperty(resourceName.concat(".label"));
		}
	}

	private Container view;
	
	public Container getView() {
		return view;
	}
	
    protected String dbName;
	protected String caption;

	public ActionPanel(DataComponent dataComponent, Object... params) {
		this.dataComponent = dataComponent;
		if (dataComponent instanceof TextComponent) {
			this.textArea = (TextComponent) dataComponent;
			setupTextArea();
		}
		this.view = param(null, 0, params);
		this.caption = com.applang.Util.paramString("Database", 1, params);
		setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));
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
	
	public boolean isActionEnabled(int index) {
		return buttons[index].getAction().isEnabled();
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
		if (memoryDb)
			handleMemoryDb(false);
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
		if (memoryDb && fileExists(memoryDbName))
			getStmt().executeUpdate("restore from " + memoryDbName);
	}
	
	protected boolean memoryDb = false;
	private String memoryDbName = "";

	private void handleMemoryDb(boolean restore) {
		try {
			if (restore) {
				memoryDbName = tempPath(BerichtsheftPlugin.NAME, "memory.db");
				openConnection(memoryDbName);
			}
			else if ("sqlite".equals(getScheme()) && memoryDbName.length() > 0) {
				getStmt().executeUpdate("backup to " + memoryDbName);
				memoryDbName = "";
			}
		} catch (Exception e) {
			handleException(e);
		}
		
	}
	
	protected void updateOnRequest() {
	}

	@Override
	protected Object select(Object... args) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected void updateItem(boolean update, Object... args) {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected boolean addItem(boolean refresh, Object item) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	protected boolean removeItem(Object item) {
		// TODO Auto-generated method stub
		return false;
	}

}

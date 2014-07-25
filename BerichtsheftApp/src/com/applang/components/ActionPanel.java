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
import javax.swing.JPanel;
import javax.swing.JToggleButton;

import com.applang.PluginUtils;
import com.applang.berichtsheft.BerichtsheftApp;
import com.applang.berichtsheft.plugin.BerichtsheftPlugin;

import static com.applang.Util.*;
import static com.applang.Util2.*;
import static com.applang.SwingUtil.*;
import static com.applang.PluginUtils.*;

public class ActionPanel extends ManagerBase<Object>
{
    protected static final String TAG = ActionPanel.class.getSimpleName();
    
	public static void createAndShowGUI(String title, 
			final Dimension preferred, 
			int modality, 
			final ActionPanel actionPanel, 
			final Function<Component> centerComponent, 
			final Object... params)
	{
		showFrame(null, title, 
				new UIFunction() {
					public Component[] apply(Component comp, Object[] parms) {
						Container contentPane = new JPanel(new BorderLayout());
						actionPanel.memberOf(northToolBar(contentPane));
						contentPane.add(centerComponent.apply(params), BorderLayout.CENTER);
						southStatusBar(contentPane);
						if (preferred != null)
							contentPane.setPreferredSize(preferred);
						return components(contentPane);
					}
				}, 
				new UIFunction() {
					public Component[] apply(Component comp, Object[] parms) {
						printContainer("createAndShowGUI", (Container)comp, true);
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
		TITLE		(11, "manager.action-TITLE"), 
		IMPORT		(12, "manager.action-IMPORT"), 
		TEXT		(13, "manager.action-TEXT"), 
		TOGGLE1		(14, "manager.action-TOGGLE1"), 
		TOGGLE2		(15, "manager.action-TOGGLE2"), 
		STRUCT		(16, "manager.action-STRUCT"), 
		ANDROID		(17, "manager.action-ANDROID"), 
		ACTIONS		(18, "Actions"); 		//	needs to stay last !
		
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
			return getProperty(resourceName + ".icon");
		}
		@Override
		public String description() {
			return getProperty(resourceName.concat(".label"));
		}
		@Override
		public String name(int state) {
			return getProperty(resourceName.concat(".label") + "." + state);
		}
	}

	private Container view;
	
	public Container getView() {
		return view;
	}
	
    protected String dbFilePath;
	protected String caption;

	public ActionPanel(IComponent iComponent, Object... params) {
		this.iComponent = iComponent;
		if (iComponent instanceof ITextComponent) {
			setupTextArea((ITextComponent) iComponent);
		}
		view = param(null, 0, params);
		caption = param_String("Database", 1, params);
		setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));
		bars = param_Integer(1, 2, params);
		buttons = new AbstractButton[bars * (1 + ActionType.ACTIONS.index())];
	}
	
	public void memberOf(Container container, Object...params) {
		Object param0 = param(null, 0, params);
		if (param0 instanceof String)
			for (int i = 0; i < buttons.length; i++) {
				AbstractButton btn = buttons[i];
				if (btn != null) {
					Object actionName = btn.getAction().getValue(CustomAction.NAME);
					if (asList(params).indexOf(actionName) > -1)
						container.add(btn);
				}
			}
		else
			container.add(this, param0);
	}

	protected AbstractButton[] buttons;
	private int bars;
	
	private int index(int i, int...bar) {
		return i += param(0, 0, bar) * buttons.length / bars;
	}
	
	private void _add(Container container, int index, AbstractButton btn, int...bar) {
		index = index(index, bar);
		if (index > -1 && index < buttons.length) {
			int pos = param(container.getComponentCount(), 1, bar);
			container.add(buttons[index] = btn, pos);
		}
	}
	
	public void addButton(Container container, int index, CustomAction customAction, int...bar) {
		_add(container, index, makeCustomButton(customAction, false), bar);
	}
	
	public void addToggle(Container container, int index, CustomAction customAction, int...bar) {
		_add(container, index, new JToggleButton(customAction), bar);
	}
	
	public void removeButton(Container container, int index, int...bar) {
		index = index(index, bar);
		if (index > -1 && index < buttons.length)
			if (buttons[index] != null) {
				container.remove(buttons[index]);
				buttons[index] = null;
			}
	}
	
	public CustomAction getAction(int index, int...bar) {
		index = index(index, bar);
		if (index > -1 && index < buttons.length)
			return (CustomAction) buttons[index].getAction();
		else
			return null;
	}
	
	public void setAction(int index, CustomAction customAction, int...bar) {
		index = index(index, bar);
		if (index > -1 && index < buttons.length)
			buttons[index].setAction(customAction);
	}
	
	public void clickAction(int index, int...bar) {
		index = index(index, bar);
		if (index > -1 && index < buttons.length)
			buttons[index].doClick();
	}
	
	public void enableAction(int index, boolean enabled, int...bar) {
		if (buttons[index] != null)
			buttons[index].getAction().setEnabled(enabled);
	}
	
	public boolean isActionEnabled(int index, int...bar) {
		return buttons[index].getAction().isEnabled();
	}

	protected IComponent iComponent = null;

	protected boolean hasTextComponent() {
		return iComponent instanceof ITextComponent;
	}
	
	public TextToggle getTextToggle() {
		return hasTextComponent() ? (TextToggle) iComponent : null;
	}
	
	protected void setupTextArea(ITextComponent iTextComponent) {
		if (hasTextComponent()) {
			getTextToggle().addKeyListener(new KeyAdapter() {
				@Override
				public void keyTyped(KeyEvent e) {
					setDirty(true);
				}
			});
		}
	}

	public void setText(String text) {
		if (hasTextComponent()) 
			getTextToggle().setText(text);
	}

	public String getText() {
		return hasTextComponent() ? 
				getTextToggle().getText() : 
				null;
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
	
	public void closeConnection() {
		dbConnect.close();
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
}

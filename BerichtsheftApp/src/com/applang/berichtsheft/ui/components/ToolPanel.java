package com.applang.berichtsheft.ui.components;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JToolBar;

import com.applang.SwingUtil;
import com.applang.Util;

public class ToolPanel extends JPanel
{
	public static void createAndShowGUI(String title, Dimension preferred, 
			final ToolPanel toolPanel, Component target, final Object... params)
	{
		try {
			JToolBar top = new JToolBar();
			top.setName("top");
			JToolBar bottom = new JToolBar();
			bottom.setName("bottom");
			bottom.setFloatable(false);
			JLabel label = new JLabel("");
			label.setName("mess");
			bottom.add(label);
			
			JFrame frame = new JFrame(title) {
				protected void processWindowEvent(WindowEvent we) {
					if (we.getID() == WindowEvent.WINDOW_CLOSING)
						toolPanel.finish(params);
					
					super.processWindowEvent(we);
				}
			};
			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			
			Container contentPane = frame.getContentPane();
			contentPane.setPreferredSize(preferred);
			
			toolPanel.addToContainer(top, BorderLayout.PAGE_START);
			contentPane.add(top, BorderLayout.PAGE_START);
			contentPane.add(bottom, BorderLayout.PAGE_END);

			JScrollPane scroll = new JScrollPane(target);
			contentPane.add(scroll, BorderLayout.CENTER);
			
			frame.pack();
			frame.setLocationRelativeTo(null);
			frame.setVisible(true);
			
			toolPanel.start(params);
		} catch (Exception e) {
			SwingUtil.handleException(e);
		}
	}
	
	protected void start(Object... params) {
		Util.Settings.load();
	}
	
	protected void finish(Object... params) {
		Util.Settings.save();
	}
	
	public enum ToolType implements SwingUtil.ActionType
	{
		CALENDAR	(0, "calendar_16x16.png", "pick date from calendar"), 
		PREVIOUS	(1, "Arrow Left_16x16.png", "previous note(s)"), 
		NEXT		(2, "Arrow Right_16x16.png", "next note(s)"), 
		DATABASE	(3, "book_open_16x16.png", "choose database"), 
		DOCUMENT	(4, "export_16x16.png", "export document"), 
		ADD			(5, "plus_16x16.png", "enter 'update' mode"), 
		UPDATE		(6, "update_16x16.png", "update note(s)"), 
		DELETE		(7, "minus_16x16.png", "delete note(s)"), 
		SPELLCHECK	(8, "abc_16x16.png", "spell check"), 
		PICK		(9, "pick_16x16.png", "pick note(s)"), 
		DATE		(10, "", "date or week of year"), 
		CATEGORY	(11, "", "category"), 
		IMPORT		(12, "import_16x16.png", "import data"), 
		TEXT		(13, "", "export text"), 
		ACTIONS		(14, "", "Actions"); 		//	needs to stay last !
		
	    private final String iconName;
	    private final String toolTip;
	    private final int index;   
	    
	    ToolType(int index, String iconName, String toolTip) {
	    	this.index = index;
	        this.iconName = iconName;
	        this.toolTip = toolTip;
	    }

	    public int index() { return index; }
	    public String iconName()   { return iconName; }
	    public String description() { return toolTip; }
	}

    protected String dbName;
	protected String caption;

	public ToolPanel(TextComponent textArea, Object... params) {
		this.textArea = textArea;
		setupTextArea();
		
		dbName = Util.paramString("databases/*", 0, params);
		caption = Util.paramString("Database", 1, params);
		
		setLayout(new FlowLayout(FlowLayout.LEADING));
	}
	
	public void addToContainer(Container container, Object constraints) {
		container.add(this, constraints);
		SwingUtil.container = container;
	}

	protected JButton[] buttons = new JButton[1 + ToolType.ACTIONS.index()];
	
	public void addButton(int index, SwingUtil.Action action) {
		if (index > -1 && index < buttons.length)
			add(buttons[index] = new JButton(action));
	}
	
	public SwingUtil.Action getAction(int index) {
		if (index > -1 && index < buttons.length)
			return (SwingUtil.Action) buttons[index].getAction();
		else
			return null;
	}
	
	public void setAction(int index, SwingUtil.Action action) {
		if (index > -1 && index < buttons.length)
			buttons[index].setAction(action);
	}
	
	public void doAction(int index) {
		if (index > -1 && index < buttons.length)
			buttons[index].doClick();
	}
	
	public void enableAction(int index, boolean enabled) {
		buttons[index].getAction().setEnabled(enabled);
	}

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
	
	public boolean isDirty() {
		return hasTextArea() && textArea.isDirty();
	}

	public void setDirty(boolean dirty) {
		if (hasTextArea()) 
			textArea.setDirty(dirty);
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
			File file = SwingUtil.chooseFile(true, this, caption, new File(dbName), null);
			if (file != null) 
				dbName = file.getPath();
		}

		return dbName;
	}

	protected Connection con = null;
	protected Statement stmt = null;
	protected String scheme = null;

	public Connection getCon() {
		return con;
	}

	public boolean openConnection(String dbPath, Object... params) {
		boolean retval = false;
		ResultSet rs = null;
		try {
			if (con != null && !con.isClosed())
				con.close();
			
			String driver = Util.paramString("org.sqlite.JDBC", 2, params);
			Class.forName(driver);
			
			scheme = Util.paramString("sqlite", 1, params);
			boolean memoryDb = "sqlite".equals(scheme) && dbPath == null;
			
			String url = "jdbc:" + scheme + ":" + (memoryDb ? "" : dbPath);
			con = DriverManager.getConnection(url);
			stmt = con.createStatement();
			
			afterConnecting();
			
			String database = Util.paramString("sqlite_master", 3, params);
			if ("sqlite".equals(scheme))
				rs = stmt.executeQuery("select name from " + database + " where type = 'table'");
			else if ("mysql".equals(scheme)) {
				rs = stmt.executeQuery("show databases;");
				boolean exists = false;
			    while (rs.next()) 
			        if (rs.getString(1).equals(database)) {
			        	exists = true;
			        	break;
			        }
	        	rs.close();
	        	if (!exists)
	        		throw new Exception(String.format("database '%s' not found", database));
	        	else
	        		stmt.execute(String.format("use %s;", database));
	        	
				rs = stmt.executeQuery("show tables in " + database + ";");
			}
			
			String tableName = Util.paramString(null, 0, params);
			if (tableName == null)
				return true;
			
		    while (rs.next()) 
		        if (rs.getString(1).equals(tableName)) 
		        	return true;
		    
		    return false;
		} catch (Exception e) {
			SwingUtil.handleException(e);
			return retval;
		} 
		finally {
			if (rs != null) {
				try {
					rs.close();
				} catch (SQLException e) {
					SwingUtil.handleException(e);
					retval = false;
				}
			}
		}
	}
	
	protected void afterConnecting() throws Exception {
		
	}
	
	protected void updateOnRequest(boolean ask) {
		
	}

}

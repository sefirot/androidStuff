package com.applang.berichtsheft.ui.components;

import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.File;
import java.net.URL;
import java.sql.Connection;
import java.sql.Statement;

import javax.swing.AbstractAction;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import com.applang.Util;

public class ToolPanel extends JPanel
{
	public enum ActionType
	{
		CALENDAR	("calendar_16x16.png", "pick date from calendar"), 
		PREVIOUS	("Arrow Left_16x16.png", "previous note(s)"), 
		NEXT		("Arrow Right_16x16.png", "next note(s)"), 
		DATABASE	("book_open_16x16.png", "choose database"), 
		DOCUMENT	("export_16x16.png", "export document"), 
		ADD			("plus_16x16.png", "enter 'update' mode"), 
		UPDATE		("update_16x16.png", "update note(s)"), 
		DELETE		("minus_16x16.png", "delete note(s)"), 
		SPELLCHECK	("abc_16x16.png", "spell check"), 
		PICK		("pick_16x16.png", "pick note(s)"), 
		DATE		("", "date or week of year"), 
		CATEGORY	("", "category"), 
		IMPORT		("import_16x16.png", "import data"); 
		
	    private final String iconName;
	    private final String toolTip;
	    
	    ActionType(String iconName, String toolTip) {
	        this.iconName = iconName;
	        this.toolTip = toolTip;
	    }
	    
	    public String iconName()   { return iconName; }
	    public String description() { return toolTip; }
	}

	protected boolean actionBlocked = false;
    
    public class ToolAction extends AbstractAction
    {
    	ActionType type;
    	
        public ActionType getType() {
			return type;
		}

		public void setType(ActionType type) {
			this.type = type;
			putValue(SMALL_ICON, iconFrom(this.type.iconName()));
			putValue(SHORT_DESCRIPTION, this.type.description());
//			putValue(MNEMONIC_KEY, KeyEvent.CHAR_UNDEFINED);
		}

		public ToolAction(ActionType type) {
			super(null);
			setType(type);
        }
        
        @Override
        public void actionPerformed(ActionEvent ae) {
        	if (actionBlocked)
        		return;
        	
        	message("");
        	System.out.println(type.toString());
        	
        	action_Performed(ae);
        }
        
        protected void action_Performed(ActionEvent ae) {
        	if (actionBlocked)
        		return;
        }
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

	protected JButton[] buttons = new JButton[9];
	
	public void addButton(int index, ToolAction action) {
		if (index > -1 && index < buttons.length)
			add(buttons[index] = new JButton(action));
	}
	
	public ToolAction getAction(int index) {
		if (index > -1 && index < buttons.length)
			return (ToolAction) buttons[index].getAction();
		else
			return null;
	}
	
	public void setAction(int index, ToolAction action) {
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

	public ImageIcon iconFrom(String path) {
		if (Util.notNullOrEmpty(path)) {
			URL url = NotePicker.class.getResource("/images/" + path);
			return new ImageIcon(url);
		}
		else
			return null;
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
	
	protected void message(String text) {
		JLabel mess = Util.findComponent(this.getParent(), "mess");
		if (mess != null)
			mess.setText(text);
		else
			System.out.println(text);
	}

	protected void handleException(Exception e) {
		if (e != null) 
        	message(e.getMessage());
	}

	protected Connection con = null;
	protected Statement stmt = null;
	protected String scheme = null;

	public Connection getCon() {
		return con;
	}

	protected String chooseDatabase(String dbName) {
		if (dbName != null) {
			File file = Util.chooseFile(true, this, caption, new File(dbName), null);
			if (file != null) 
				dbName = file.getPath();
		}

		return dbName;
	}
	
	protected void updateOnRequest(boolean ask) {
		
	}

}

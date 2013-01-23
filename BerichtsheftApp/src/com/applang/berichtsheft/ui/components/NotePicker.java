package com.applang.berichtsheft.ui.components;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import java.io.File;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;

import javax.swing.AbstractAction;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JToolBar;

import com.applang.Util;
import com.applang.berichtsheft.BerichtsheftApp;
import com.applang.berichtsheft.ui.BerichtsheftTextArea;

public class NotePicker extends JPanel
{
	public static void main(String[] args) {
		BerichtsheftTextArea textArea = new BerichtsheftTextArea();
        JToolBar bar = new JToolBar();
        bar.setFloatable(false);
        JLabel label = new JLabel("");
        label.setName("mess");
        bar.add(label);
		
		final NotePicker notePicker = new NotePicker(textArea, 
				null,
				"Berichtsheft database", label);
		notePicker.handleMemoryDb(true);
		
		String title = NotePicker.class.getSimpleName();
        JFrame frame = new JFrame(title) {
			protected void processWindowEvent(WindowEvent we) {
				if (we.getID() == WindowEvent.WINDOW_CLOSING) 
					try {
						notePicker.updateOnRequest(true);
						notePicker.handleMemoryDb(false);
						notePicker.getCon().close();
					} catch (SQLException e) {
						notePicker.handleException(e);
					}
				
				super.processWindowEvent(we);
			}
        };
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		Container contentPane = frame.getContentPane();
		contentPane.setPreferredSize(new Dimension(1000, 200));

		JScrollPane scroll = new JScrollPane(textArea.textArea);
		contentPane.add(scroll, BorderLayout.CENTER);

		contentPane.add(bar, BorderLayout.PAGE_END);
		contentPane.add(notePicker, BorderLayout.PAGE_START);
		
		frame.pack();
		frame.setLocationRelativeTo(null);
		frame.setVisible(true);
	}
	
	private JButton[] buttons = new JButton[9];
	private JTextField date = new JTextField(20);
	@SuppressWarnings("rawtypes")
	private JComboBox category = new JComboBox();
	private JTextField categoryEdit = Util.comboEdit(category);

	public static ImageIcon iconFrom(String path) {
		if (Util.notNullOrEmpty(path)) {
			URL url = NotePicker.class.getResource("/images/" + path);
			return new ImageIcon(url);
		}
		else
			return null;
	}
    
	boolean dirty = false;
	
	public boolean isDirty() {
		return dirty;
	}

	public void setDirty(boolean dirty) {
		this.dirty = dirty;
//		changeButtonFace(5, this.dirty ? -1 : 5);
	}

	TextComponent textArea = null;

	private boolean hasTextArea() {
		return this.textArea != null;
	}

	public void setText(String text) {
		if (hasTextArea()) 
			this.textArea.setText(text);
	}

	public String getText() {
		return hasTextArea() ? this.textArea.getText() : null;
	}
	
	public void setupTextArea() {
		if (hasTextArea()) {
			this.textArea.addKeyListener(new KeyAdapter() {
				@Override
				public void keyTyped(KeyEvent e) {
					setDirty(true);
				}
			});
		}
	}
	
	void message(String text) {
		JLabel mess = Util.findComponent(this.getParent(), "mess");
		if (mess != null)
			mess.setText(text);
	}
	
    String dbName;
	String caption;

	public NotePicker(TextComponent textArea, Object... params) {
		this.textArea = textArea;
		setupTextArea();
		
		dbName = Util.paramString("databases/*", 0, params);
		caption = Util.paramString("Notes database", 1, params);
		
		setLayout(new FlowLayout(FlowLayout.LEADING));
		
		add(buttons[3] = new JButton(new NoteAction(ActionType.DATABASE)));
		
		date.setHorizontalAlignment(JTextField.CENTER);
		Util.addFocusObserver(date);
		add(date);
		date.setAction(new NoteAction(ActionType.DATE));
		date.addKeyListener(new KeyAdapter() {
			@Override
			public void keyTyped(KeyEvent e) {
				checkDocumentPossible();
			}
		});
		
		add(buttons[0] = new JButton(new NoteAction(ActionType.CALENDAR)));
		
		category.setEditable(true);
		Util.addFocusObserver(categoryEdit);
		add(category);
		categoryEdit.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_ENTER)
					buttons[8].doClick();
			}
		});
		
		add(buttons[8] = new JButton(new NoteAction(ActionType.PICK)));
		
		add(buttons[1] = new JButton(new NoteAction(ActionType.PREVIOUS)));
		add(buttons[2] = new JButton(new NoteAction(ActionType.NEXT)));
		add(buttons[7] = new JButton(new NoteAction(ActionType.SPELLCHECK)));
		add(buttons[5] = new JButton(new NoteAction(ActionType.ADD)));
		add(buttons[6] = new JButton(new NoteAction(ActionType.DELETE)));
		add(buttons[4] = new JButton(new NoteAction(ActionType.DOCUMENT)));
		
		clear();
	}

	enum ActionType
	{
		CALENDAR	("calendar_16x16.png", "pick date from calendar"), 
		PREVIOUS	("Arrow Left_16x16.png", "previous note(s)"), 
		NEXT		("Arrow Right_16x16.png", "next note(s)"), 
		DATABASE	("book_open_16x16.png", "choose database"), 
		DOCUMENT	("merge_16x16.png", "fill document"), 
		ADD			("plus_16x16.png", "enter 'update' mode"), 
		UPDATE		("update_16x16.png", "update note(s)"), 
		DELETE		("minus_16x16.png", "delete note(s)"), 
		SPELLCHECK	("abc_16x16.png", "spell check"), 
		PICK		("pick_16x16.png", "pick note(s)"), 
		DATE		("", "date or week of year"), 
		CATEGORY	("", "category"); 
		
	    private final String iconName;
	    private final String toolTip;
	    
	    ActionType(String iconName, String toolTip) {
	        this.iconName = iconName;
	        this.toolTip = toolTip;
	    }
	    
	    public String iconName()   { return iconName; }
	    public String description() { return toolTip; }
	}
	
    boolean actionBlocked = false;
    
    public class NoteAction extends AbstractAction
    {
    	ActionType type;
    	
        public ActionType getType() {
			return type;
		}

		public void setType(ActionType type) {
			this.type = type;
		}

		public NoteAction(ActionType type) {
			super(null);
			this.type = type;
			putValue(SMALL_ICON, iconFrom(this.type.iconName()));
			putValue(SHORT_DESCRIPTION, this.type.description());
//			putValue(MNEMONIC_KEY, KeyEvent.CHAR_UNDEFINED);
        }
        
        @Override
        public void actionPerformed(ActionEvent ae) {
        	if (actionBlocked)
        		return;
        	
        	message("");
        	System.out.println(type.toString());
        	
        	String dateString = getDate();
        	String pattern = getPattern();
        	
        	switch (type) {
			case DATABASE:
				updateOnRequest(true);
				handleMemoryDb(false);
				dbName = chooseDatabase(dbName);
				break;
			case CALENDAR:
				dateString = pickDate(dateString);
        		pickNote(dateString, pattern);
				date.requestFocusInWindow();
				break;
			case PREVIOUS:
				updateOnRequest(true);
				setText(move(false));
				break;
			case NEXT:
				updateOnRequest(true);
				setText(move(true));
				break;
			case DOCUMENT:
				updateOnRequest(true);
				fillDocument(dateString);
				break;
			case ADD:
				buttons[5].setAction(new NoteAction(ActionType.UPDATE));
				break;
			case UPDATE:
				updateOnRequest(false);
				buttons[5].setAction(new NoteAction(ActionType.ADD));
				pickNote(dateString, pattern);
				break;
			case DELETE:
				deleteOnRequest(dateString, pattern);
				break;
			case SPELLCHECK:
				setDirty(NotePicker.this.textArea.spellcheck() || isDirty());
				break;
			case DATE:
				pickNote(dateString, pattern);
				date.requestFocusInWindow();
				break;
			case PICK:
				searchPattern = getPattern();
				keyLine(searchPattern);
				pickNote(dateString, searchPattern);
				category.requestFocusInWindow();
				break;
			default:
				return;
			}
        }
    }

	private void handleException(Exception e) {
		if (e != null) 
        	message(e.getMessage());
	}
	
	private void enableAction(JButton btn, boolean enabled) {
		btn.getAction().setEnabled(enabled);
	}
	
	private void clear() {
		refreshWith(null);
		enableAction(buttons[1], false);
		enableAction(buttons[2], false);
		enableAction(buttons[5], hasTextArea());
		enableAction(buttons[6], false);
		enableAction(buttons[4], false);
		enableAction(buttons[7], hasTextArea());
	}

	private String chooseDatabase(String dbName) {
		if (dbName != null) {
			File file = Util.chooseFile(true, this, caption, new File(dbName), null);
			if (file != null) 
				dbName = file.getPath();
		}
		if (openConnection(dbName)) {
			retrieveCategories();
			searchPattern = allCategories;
			keyLine(searchPattern);
			setTime(keys.length > 0 ? timeFromKey(keys[0]) : Util.now());
			pickNote(getDate(), searchPattern);
		}
		return dbName;
	}
	
	private String memoryDbName = "";

	private void handleMemoryDb(boolean restore) {
		try {
			if (restore) {
				memoryDbName = "/tmp/memory.db";
				chooseDatabase(null);
			}
			else if ("sqlite".equals(scheme) && memoryDbName.length() > 0) {
				stmt.executeUpdate("backup to " + memoryDbName);
				memoryDbName = "";
			}
		} catch (Exception e) {
			handleException(e);
		}
		
	}

	private String pickDate(String dateString) {
		dateString = new DatePicker(NotePicker.this, dateString, timeLine()).getDateString();
		NotePicker.this.requestFocus();
		setDate(dateString);
		return dateString;
	}

	private boolean updateMode() {
		NoteAction act = (NoteAction) buttons[5].getAction();
		return act.getType() == ActionType.UPDATE;
	}

	private void updateOnRequest(boolean ask) {
		String dateString = getDate();
		if (!ask || isDirty() && JOptionPane.YES_OPTION == JOptionPane.showConfirmDialog(
				NotePicker.this, String.format("update '%s' on '%s'", getCategory(), dateString), 
				caption, 
				JOptionPane.YES_NO_OPTION))
		{
			updateOrInsert(getPattern(), dateString, !ask);
			retrieveCategories();
		}
	}

	private void deleteOnRequest(String dateString, String pattern) {
		if (JOptionPane.YES_OPTION == JOptionPane.showConfirmDialog(
				NotePicker.this, String.format("delete '%s' on '%s'", pattern, dateString), 
				caption, 
				JOptionPane.YES_NO_OPTION))
		{
			delete(pattern, dateString, true);
			retrieveCategories();
		}
	}

	private Connection con = null;
	private Statement stmt = null;
	private String scheme = null;

	public Connection getCon() {
		return con;
	}

	public boolean openConnection(String db, Object... params) {
		ResultSet rs = null;
		try {
			if (con != null && !con.isClosed())
				con.close();
			
			String driver = Util.paramString("org.sqlite.JDBC", 1, params);
			Class.forName(driver);
			
			scheme = Util.paramString("sqlite", 0, params);
			boolean memoryDb = "sqlite".equals(scheme) && db == null;
			
			String url = "jdbc:" + scheme + ":" + (memoryDb ? "" : db);
			con = DriverManager.getConnection(url);
			stmt = con.createStatement();
			
			if (memoryDb && Util.fileExists(new File(memoryDbName)))
				stmt.executeUpdate("restore from " + memoryDbName);
			
			String database = Util.paramString("sqlite_master", 2, params);
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
			
		    while (rs.next()) 
		        if (rs.getString(1).equals("notes")) 
		        	return true;
		    
			if ("sqlite".equals(scheme))
			    stmt.execute("CREATE TABLE notes (" +
			    		"_id INTEGER PRIMARY KEY," +
			    		"title TEXT," +
			    		"note TEXT," +
			    		"created INTEGER," +
			    		"modified INTEGER, UNIQUE (created, title))");
			else if ("mysql".equals(scheme)) 
			    stmt.execute("CREATE TABLE notes (" +
			    		"_id BIGINT PRIMARY KEY," +
			    		"title VARCHAR(40)," +
			    		"note TEXT," +
			    		"created BIGINT," +
			    		"modified BIGINT, UNIQUE (created, title))");
		    
		    return true;
		} catch (Exception e) {
			handleException(e);
			con = null;
			return false;
		}
		finally {
			if (rs != null) {
				try {
					rs.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	private void setTime(Long time) {
		if (this.time.length == 2) 
			setDate(formatWeek(time));
		else
			setDate(formatDate(time));
	}

	private int kindOfDate(String dateString) {
		if (DatePicker.isWeekDate(dateString))
			return 2;
		else if (DatePicker.isCalendarDate(dateString))
			return 1;
		else
			return 0;
	}
	
	private long[] getTime(String dateString) {
		if (kindOfDate(dateString) == 2)
			return DatePicker.weekInterval(dateString, 1);
		else {
			Date date = parseDate(dateString);
			return new long[] {date == null ? Util.now() : date.getTime()};
		}
	}

	private void checkDocumentPossible() {
		enableAction(buttons[4], kindOfDate(getDate()) == 2);
	}
	
	public String getDate() {
		return date.getText();
	}
	
	public void setDate(String text) {
		actionBlocked = true;
		
		date.setText(text);
		
		actionBlocked = false;
	}
	
	@SuppressWarnings("unchecked")
	private void retrieveCategories() {
		String categ = getCategory();
		category.removeAllItems();
		try {
			PreparedStatement ps = con.prepareStatement("select distinct title from notes order by title");
			ResultSet rs = ps.executeQuery();
			while (rs.next())
				category.addItem(rs.getString(1));
		} catch (SQLException e) {
			handleException(e);
		}
		category.addItem(itemAll);
		setCategory(categ);
	}
	
	public void setCategory(String t) {
		actionBlocked = true;
		
		categoryEdit.setText(t);
		
		actionBlocked = false;
	}

	public String getCategory() {
		return categoryEdit.getText();
	}
	
	public static final String allDates = "";
	public static final String allCategories = ".*";
	String itemAll = "-- all --";

	public void setPattern(String p) {
		pattern = p;
		setCategory(p.equals(allCategories) ? itemAll : p);
	}

	public String getPattern() {
		String categ = getCategory();
		return categ.equals(itemAll) ? allCategories : categ;
	}
	
	private Long[] ids = null;
	private int index = -1;
	public Object[][] records = null;
	
	public int registerNotes(ResultSet rs) throws SQLException {
		ArrayList<Long> idlist = new ArrayList<Long>();
		ArrayList<Object[]> reclist = new ArrayList<Object[]>();
		
		ResultSetMetaData rsmd = rs.getMetaData();
		int cols = rsmd.getColumnCount();
		
		while (rs.next()) {
			if (cols > 1) {
				ArrayList<Object> list = new ArrayList<Object>();
				for (int i = 1; i <= cols; i++)
					list.add(rs.getObject(i));
				reclist.add(list.toArray());
			}
			else
				idlist.add(rs.getLong(1));
		}
		rs.close();
		
		records = reclist.toArray(new Object[0][]);
		
		index = ids != null && idlist.size() == 1 ? 
				Arrays.asList(ids).indexOf(idlist.get(0)) : -1;
		if (index < 0)
			ids = idlist.toArray(new Long[0]);
		else
			index--;
		
		return cols > 1 ? records.length : ids.length;
	}
	
	public Long[] timeLine(Object... params) {
		ArrayList<Long> timelist = new ArrayList<Long>();
		try {
			String pattern = Util.param(allCategories, 0, params);
			PreparedStatement ps = con.prepareStatement("select distinct created FROM notes where title regexp ? order by created");
			ps.setString(1, pattern);
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				timelist.add(rs.getLong(1));
			}
			rs.close();
		} catch (Exception e) {}
		return timelist.toArray(new Long[0]);
	}
	
	String[] keys = null;
	long[] time = new long[0];
	String searchPattern = allCategories;
	String pattern = "";
	
	public String[] keyLine(Object... params) {
		ArrayList<String> keylist = new ArrayList<String>();
		try {
			String pattern = Util.param(searchPattern, 0, params);
			PreparedStatement ps = con.prepareStatement("select created,title FROM notes where title regexp ? order by created,title");
			ps.setString(1, pattern);
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				keylist.add(keyValue(rs.getLong(1), rs.getString(2)));
			}
			rs.close();
		} catch (Exception e) {}
		return this.keys = keylist.toArray(new String[0]);
	}
	
	public String keyValue(long time, String category) {
		return time + "_" + category;
	}
	
	public long timeFromKey(String key) {
		return Long.parseLong(key.substring(0, key.indexOf('_')));
	}
	
	private String categoryFromKey(String key) {
		return key.substring(key.indexOf('_') + 1);
	}
	
	private class KeyComparator implements Comparator<String>
	{
		public KeyComparator(boolean partial) {
			this.partial = partial;
		}
		private boolean partial;
		@Override
		public int compare(String o1, String o2) {
			long t1 = timeFromKey(o1);
			long t2 = timeFromKey(o2);
			if (t1 < t2)
				return -1;
			else if (t1 > t2)
				return 1;
			else if (partial)
				return 0;
			
			String c1 = categoryFromKey(o1);
			String c2 = categoryFromKey(o2);
			return c1.compareTo(c2);
		}
	};
	
	private KeyComparator comparator = new KeyComparator(false);
	private KeyComparator partialComparator = new KeyComparator(true);
	
	private int criterion(long time, String category) {
		if (allCategories.equals(category)) {
			String value = keyValue(time, "");
			for (int i = 0; i < this.keys.length; i++) {
				String key = this.keys[i];
				if (partialComparator.compare(key, value) == 0)
					return i;
			}
			return -1;
		}
		
		return Arrays.binarySearch(this.keys, keyValue(time, category), comparator);
	}
	
	private int insert_point(long time, String category) {
		int crit = criterion(time, category);
		if (crit < 0)
			crit = -crit - 1;
		return crit;
	}
	
	public boolean noteAvailable(long... time) {
		int crit = criterion(time[0], pattern);
		if (time.length > 1) {
			int crit2 = criterion(time[1] - 1, pattern);
			return crit > -1 || crit != crit2;
		}
		else
			return crit > -1;
	}
	
	public boolean nextNoteAvailable(long... time) {
		if (time.length > 1) 
			return insert_point(time[1], pattern) < keys.length;
		else 
			return insert_point(time[0], pattern) < keys.length - 1;
	}
	
	public boolean previousNoteAvailable(long... time) {
		return insert_point(time[0], pattern) > 0;
	}

	public String find(boolean next, long... time) {
		boolean timeAvailable = time != null && time.length > 0;
		boolean weekMode = timeAvailable && time.length > 1;
		
		int crit;
		if (next && weekMode) 
			crit = insert_point(time[1], pattern);
		else {
			crit = insert_point(time[0], pattern);
			crit += next ? 1 : -1;
		}
		if (crit < 0 || crit >= keys.length)
			return null;
		
		String key = keys[crit];
		
		long t = timeFromKey(key);
		if (weekMode)
			this.time = DatePicker.weekInterval(formatWeek(t), 1);
		else
			this.time = new long[] {t};
		this.pattern = categoryFromKey(key);
		
		return key;
	}

	private boolean timeNotAvailable() {
		return time == null || time.length < 1;
	}
	
	public void pickNote(String dateString, String pattern) {
		if (updateMode())
			return;
			
		clear();
		
		try {
			checkDocumentPossible();
			
			this.pattern = pattern;
			time = getTime(dateString);
			if (timeNotAvailable()) {
				message(String.format("value for '%s' doesn't make sense", ActionType.DATE.description()));
				date.requestFocus();
				return;
			}
			
			ResultSet rs = null;
			
			if (noteAvailable(time)) 
				rs = query(time);
			
			enableAction(buttons[1], previousNoteAvailable(time));
			enableAction(buttons[2], nextNoteAvailable(time));
			
			setText(refreshWith(rs));			
		} catch (Exception e) {
			handleException(e);
		}
	}
	
	private static final String orderClause = " order by created, title";
	
	public PreparedStatement preparePicking(boolean full, String pattern, long... time) throws Exception {
		if (con == null || time.length < 1)
			return null;
		
		String sql = "select " +
				(full ? 
						"created,title,note" : 
						"_id") +
				" from notes";
		
		PreparedStatement ps;
		if (time.length < 2) {
			ps = con.prepareStatement(sql + " where title regexp ? and created = ?");
			ps.setString(1, pattern);
			ps.setLong(2, time[0]);
		}
		else {
			ps = con.prepareStatement(sql + " where created between ? and ? and title regexp ?" + orderClause);
			ps.setLong(1, time[0]);
			ps.setLong(2, time[1] - 1);
			ps.setString(3, pattern);
		}
		return ps;
	}

	public int update(long id, String note) throws Exception {
		PreparedStatement ps = con.prepareStatement("UPDATE notes SET note = ?, modified = ? where _id = ?");
		ps.setString(1, note);
		ps.setLong(2, Util.now());
		ps.setLong(3, id);
		return ps.executeUpdate();
	}

	public int insert(long id, String note, String category, long time) throws Exception {
		PreparedStatement ps = con.prepareStatement("INSERT INTO notes (_id,title,note,created,modified) VALUES (?,?,?,?,?)");
		ps.setLong(1, id);
		ps.setString(2, category);
		ps.setString(3, note);
		ps.setLong(4, time);
		ps.setLong(5, Util.now());
		return ps.executeUpdate();
	}
	
	public void delete(String pattern, String dateString) throws Exception {
		Date date = parseDate(dateString);
		PreparedStatement ps;
		if (date != null) {
			Long time = date.getTime();
			ps = preparePicking(true, pattern, time);
			ResultSet rs = ps.executeQuery();
			if (rs.next()) {
				ps = con.prepareStatement("DELETE FROM notes where title regexp ? and created = ?");
				ps.setString(1, pattern);
				ps.setLong(2, time);
				ps.executeUpdate();
			}
			rs.close();
		}
		else {
			ps = con.prepareStatement("DELETE FROM notes where title regexp ?");
			ps.setString(1, pattern);
			ps.executeUpdate();
		}
	}

	private void delete(String pattern, String dateString, boolean refresh) {
		try {
			long[] time = getTime(dateString);
			PreparedStatement ps = preparePicking(false, pattern, time);
			if (registerNotes(ps.executeQuery()) > 0) {
				for (int i = 0; i < ids.length; i++) {
					ps = con.prepareStatement("DELETE FROM notes where _id = ?");
					ps.setLong(1, ids[i]);
					ps.executeUpdate();
				}
				
				keyLine(searchPattern);
			
				if (refresh) 
					pickNote(dateString, pattern);
			}
		} catch (Exception e) {
			handleException(e);
		}
	}

	public long newId() throws Exception {
		ResultSet rs = con.createStatement().executeQuery("SELECT max(_id) FROM notes");
		long id = rs.next() ? rs.getLong(1) : -1;
		rs.close();
		return 1 + id;
	}

	public long updateOrInsert(String pattern, String dateString, String note) {
		try {
			long time = parseDate(dateString).getTime();
			PreparedStatement ps = preparePicking(true, pattern, time);
			ResultSet rs = ps.executeQuery();
			
			long id;
			if (rs.next()) {
				id = rs.getLong(1);
				update(id, note);
			}
			else {
				id = newId();
				insert(id, note, getCategory(), time);
			}
			
			rs.close();
			return id;
		} catch (Exception e) {
			handleException(e);
			return -1;
		}
	}

	private void updateOrInsert(String pattern, String dateString, boolean refresh) {
		if (time.length == 2) {
			for (String[] record : getRecords(getText())) 
				updateOrInsert(record[1], record[0], record[2]);
		}
		else {
			long id = updateOrInsert(pattern, dateString, getText());
			if (ids != null)
				index = Arrays.asList(ids).indexOf(id) - 1;
		}
		
		keyLine(searchPattern);
		
		if (refresh) 
			pickNote(dateString, pattern);
	}
	
	private ResultSet query(long... time) {
		ResultSet rs = null;
		try {
			PreparedStatement ps = preparePicking(true, this.pattern, time);
			rs = ps.executeQuery();
		} catch (Exception e) {
			handleException(e);
		}
		return rs;
	}
	
	public String move(boolean next) {
		ResultSet rs = null;
		
		if (find(next, time) != null) 
			rs = query(time);
		
		enableAction(buttons[1], previousNoteAvailable(time));
		enableAction(buttons[2], nextNoteAvailable(time));
		
		return refreshWith(rs);
	}

	private String refreshWith(ResultSet rs) {
		String note = null;
		
		try {
			if (rs == null) {
				setDate("");
				return null;
			}
			
			if (time.length == 2) {
				if (registerNotes(rs) > 0) {
					setDate(formatWeek(time[0]));
					setPattern(this.pattern);
					note = all();
				}
			}
			else {
				if (rs.next()) {
					setDate(formatDate(rs.getLong(1)));
					setCategory(rs.getString(2));
					note = rs.getString(3);
				}
			
				rs.close();
			}
			
			return note;
		} catch (Exception e) {
			handleException(e);
			return refreshWith(null);
		}
		finally {
			setDirty(false);
			enableAction(buttons[6], hasTextArea() && note != null);
		}
	}

	public String all() throws Exception {
		String all = String.format(wrapFormat, ":folding=explicit:");
		for (int i = 0; i < records.length; i++) {
			if (all.length() > 0)
				all += "\n";
			all += wrapNote(records[i]);
		}
		return all;
	}

	public String formatDate(long time) {
		return Util.formatDate(time, DatePicker.dateFormat);
	}

	public String formatWeek(long time) {
		Date date = new Date();
		date.setTime(time);
		return DatePicker.weekDate(Util.weekInterval(date, 1));
	}

	public Date parseDate(String dateString) {
		try {
			return Util.parseDate(dateString, DatePicker.dateFormat);
		} catch (Exception e) {
			return null;
		}
	}

	public String wrapNote(Object... params) throws Exception {
		if (Util.isAvailable(0, params) && params[0] instanceof Long) {
			String dateString = formatDate((Long)params[0]);
			return String.format(noteFormat2, dateString, 
					Util.param("", 1, params), 
					Util.param("", 2, params));
		}
		else
			return String.format(noteFormat1, 
					Util.param("", 0, params), 
					Util.param("", 1, params));
	}
	
	public static String wrapFormat = "{{{ %s }}}";
	public static Pattern wrapPattern = Pattern.compile("(?s)\\{\\{\\{ (.*?) \\}\\}\\}");
	
	public static String noteFormat1 = "{{{ %s\n%s\n}}}";
	public static String noteFormat2 = "{{{ %s '%s'\n%s\n}}}";
	public static Pattern notePattern1 = 
			Pattern.compile("(?s)\\{\\{\\{ ([^\\}]*?)\\n([^\\}]*?)\\n\\}\\}\\}");
	public static Pattern notePattern2 = 
			Pattern.compile("(?s)\\{\\{\\{ ([^\\}]*?) '([^\\}]*?)'\\n(.*?)\\n\\}\\}\\}");
	
	public boolean isWrapped(String text) {
		return text.startsWith("{{{") && text.endsWith("}}}");
	}

	public String[][] getRecords(String text) {
		ArrayList<String[]> list = new ArrayList<String[]>();
		
		for (MatchResult m : Util.findAllIn(text, notePattern2)) 
			list.add(new String[] {m.group(1), m.group(2), m.group(3)});
		
		return list.toArray(new String[0][]);
	}

	public void fillDocument(String dateString) {
		int[] weekDate = DatePicker.parseWeekDate(dateString);
		String docName = "Tagesberichte_" + String.format("%d_%d", weekDate[1], weekDate[0]) + ".odt";
		if (BerichtsheftApp.merge(
			"Vorlagen/Tagesberichte.odt", 
			"Dokumente/" + docName, 
			dbName, 
			weekDate[1], weekDate[0]))
		{
			JOptionPane.showMessageDialog(
					NotePicker.this, String.format("document '%s' created", docName), 
					caption, 
					JOptionPane.INFORMATION_MESSAGE);
		}
	}

}

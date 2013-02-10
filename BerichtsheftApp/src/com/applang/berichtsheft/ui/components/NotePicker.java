package com.applang.berichtsheft.ui.components;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import java.io.File;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;

import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JToolBar;

import com.applang.Util;
import com.applang.berichtsheft.BerichtsheftApp;
import com.applang.berichtsheft.ui.BerichtsheftTextArea;

public class NotePicker extends ToolPanel
{
	public static void main(String[] args) {
		BerichtsheftTextArea textArea = new BerichtsheftTextArea();
        JToolBar bar = new JToolBar();
        bar.setFloatable(false);
        JLabel label = new JLabel("");
        label.setName("mess");
        bar.add(label);
		
        String title = "Berichtsheft database";
		final NotePicker notePicker = new NotePicker(textArea, 
				null,
				title, label);
		notePicker.handleMemoryDb(true);
		
        JFrame frame = new JFrame(title) {
			protected void processWindowEvent(WindowEvent we) {
				if (we.getID() == WindowEvent.WINDOW_CLOSING) 
					try {
						notePicker.updateOnRequest(true);
						notePicker.handleMemoryDb(false);
						notePicker.getCon().close();
					} catch (Exception e) {
						Util.handleException(e);
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
		notePicker.addToContainer(contentPane, BorderLayout.PAGE_START);
		
		frame.pack();
		frame.setLocationRelativeTo(null);
		frame.setVisible(true);
	}
	
	private JTextField date = new JTextField(20);
	@SuppressWarnings("rawtypes")
	private JComboBox category = new JComboBox();
	private JTextField categoryEdit = Util.comboEdit(category);
	
	public NotePicker(TextComponent textArea, Object... params) {
		super(textArea, params);
		
		addButton(3, new NoteAction(ActionType.DATABASE));
		
		date.setHorizontalAlignment(JTextField.CENTER);
		Util.addFocusObserver(date);
		add(date);
		date.addKeyListener(new KeyAdapter() {
			@Override
			public void keyTyped(KeyEvent e) {
				checkDocumentPossible();
			}
			@Override
			public void keyPressed(KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_ENTER)
					doAction(8);
			}
		});
		
		addButton(0, new NoteAction(ActionType.CALENDAR));
		
		category.setEditable(true);
		Util.addFocusObserver(categoryEdit);
		add(category);
		categoryEdit.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_ENTER)
					doAction(8);
			}
		});
		
		addButton(8, new NoteAction(ActionType.PICK));
		
		addButton(1, new NoteAction(ActionType.PREVIOUS));
		addButton(2, new NoteAction(ActionType.NEXT));
		addButton(7, new NoteAction(ActionType.SPELLCHECK));
		addButton(5, new NoteAction(ActionType.ADD));
		addButton(6, new NoteAction(ActionType.DELETE));
		addButton(4, new NoteAction(ActionType.DOCUMENT));
		
		clear();
	}
    
    public class NoteAction extends ToolAction
    {
		public NoteAction(ActionType type) {
			super(type);
        }
        
        @Override
        protected void action_Performed(ActionEvent ae) {
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
				date.requestFocusInWindow();
				break;
			case PREVIOUS:
				updateOnRequest(true);
				setText(move(Direction.PREV));
				break;
			case NEXT:
				updateOnRequest(true);
				setText(move(Direction.NEXT));
				break;
			case DOCUMENT:
				updateOnRequest(true);
				fillDocument(dateString);
				break;
			case ADD:
				setAction(5, new NoteAction(ActionType.UPDATE));
				break;
			case UPDATE:
				updateOnRequest(false);
				setAction(5, new NoteAction(ActionType.ADD));
				break;
			case DELETE:
				deleteOnRequest(dateString, pattern);
				break;
			case SPELLCHECK:
				NotePicker.this.textArea.spellcheck();
				break;
			case DATE:
				date.requestFocusInWindow();
				break;
			case CATEGORY:
				category.requestFocusInWindow();
				break;
			case PICK:
				searchPattern = getPattern();
				keyLine(searchPattern);
				pickNote(dateString, searchPattern);
				break;
			default:
				return;
			}
        }
    }
	
	private void clear() {
		refreshWith(null);
		enableAction(1, false);
		enableAction(2, false);
		enableAction(5, hasTextArea());
		enableAction(6, false);
		enableAction(4, false);
		enableAction(7, hasTextArea());
	}

	@Override
	protected String chooseDatabase(String dbName) {
		dbName = super.chooseDatabase(dbName);
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
			Util.handleException(e);
		}
		
	}

	private String pickDate(String dateString) {
		Long[] times = timeLine();
		if (times.length > 0)
			times = Util.arrayextend(times, true, times[0] - Util.getMillis(1) + 1);
		dateString = new DatePicker(NotePicker.this, dateString, times).getDateString();
		NotePicker.this.requestFocus();
		setDate(dateString);
		return dateString;
	}

	private boolean updateMode() {
		return getAction(5).getType() == ActionType.UPDATE;
	}

	@Override
	protected void updateOnRequest(boolean ask) {
		String dateString = getDate();
		if (!ask || isDirty() && JOptionPane.YES_OPTION == JOptionPane.showConfirmDialog(
				NotePicker.this, String.format("update '%s' on '%s'", getCategory(), dateString), 
				caption, 
				JOptionPane.YES_NO_OPTION))
		{
			updateOrInsert(getPattern(), dateString);
			
			keyLine(searchPattern);
			pickNote(dateString, pattern);
			retrieveCategories();
		}
	}

	private void deleteOnRequest(String dateString, String pattern) {
		if (delete(true, pattern, dateString)) {
			keyLine(searchPattern);
			pickNote(dateString, pattern);
			retrieveCategories();
		}
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
			Util.handleException(e);
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
		setDate(formatDate(this.time.length == 2, time));
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
		enableAction(4, kindOfDate(getDate()) == 2);
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
			Util.handleException(e);
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
	
	public Long[] ids = null;
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
		ids = idlist.toArray(new Long[0]);
		
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

	private String category(String pattern) {
		return allCategories.equals(pattern) ? "" : pattern;
	}
	
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
	
	public String categoryFromKey(String key) {
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
		String value = keyValue(time, category(category));
		if (allCategories.equals(category)) {
			for (int i = 0; i < this.keys.length; i++) {
				String key = this.keys[i];
				if (partialComparator.compare(key, value) == 0)
					return i;
			}
			return -1;
		}
		else
			return Arrays.binarySearch(this.keys, value, comparator);
	}
	
	private int pointer(int crit) {
		if (crit < 0)
			crit = -crit - 1;
		return crit;
	}
	
	private int pointer(long time, String category) {
		return pointer(criterion(time, category));
	}
	
	private int[] index = new int[2];
	
	public boolean bunchAvailable(long... time) {
		index[0] = criterion(time[0], pattern);
		if (time.length == 2) {
			index[1] = criterion(time[1] - 1, pattern);
			return index[0] > -1 || index[0] != index[1];
		}
		else
			return index[0] > -1;
	}
	
	public boolean nextBunchAvailable(long... time) {
		if (time.length == 2) 
			return pointer(time[1], pattern) < keys.length;
		else 
			return pointer(time[0], pattern) < keys.length - 1;
	}
	
	public boolean previousBunchAvailable(long... time) {
		return pointer(time[0], pattern) > 0;
	}

	public String find(Direction direct, long... time) {
		boolean weekMode = time.length == 2;
		boolean available = bunchAvailable(time);
		
		int index = pointer(this.index[0]);
		if (direct == Direction.HERE) 
			if (!available) {
				Util.message(String.format("No '%s' on %s !", category(pattern), formatDate(weekMode, time[0])));
				if (index >= keys.length) {
					direct = Direction.PREV;
					available = true;
				}
				else
					direct = Direction.NEXT;
			}
		
		boolean next = direct == Direction.NEXT;
		if (next && weekMode) 
			index = pointer(this.index[1]);
		else {
			if (available && direct != Direction.HERE)
				index += next ? 1 : -1;
		}
		if (index < 0 || index >= keys.length)
			return null;
		
		String key = keys[index];
		
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
				Util.message(String.format("value for '%s' doesn't make sense", ActionType.DATE.description()));
				date.requestFocus();
				return;
			}
			
			setText(move(Direction.HERE));			
		} catch (Exception e) {
			Util.handleException(e);
			setText("");
		}
	}
	
	private static final String orderClause = " order by created, title";
	
	public PreparedStatement preparePicking(boolean record, String pattern, long... time) throws Exception {
		String sql = "select " +
				(record ? 
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
	
	public int count(String pattern, String dateString, boolean delete) throws Exception {
		PreparedStatement ps;
		Date date = parseDate(dateString);
		if (date == null) {
			if (delete)
				ps = con.prepareStatement("delete from notes where title regexp ?");
			else
				ps = con.prepareStatement("select count(*) from notes where title regexp ?");
			ps.setString(1, pattern);
		}
		else {
			if (delete)
				ps = con.prepareStatement("delete from notes where title regexp ? and created = ?");
			else
				ps = con.prepareStatement("select count(*) from notes where title regexp ? and created = ?");
			ps.setString(1, pattern);
			ps.setLong(2, date.getTime());
		}
		if (!delete) {
			ResultSet rs = ps.executeQuery();
			rs.next();
			return rs.getInt(1);
		}
		else
			return ps.executeUpdate();
	}

	public boolean delete(boolean ask, String pattern, String dateString) {
		boolean retval = false;
		try {
			long[] interval = getTime(dateString);
			if (interval.length == 1)
				interval = Util.dayInterval(interval[0], 1);
			PreparedStatement ps = preparePicking(false, pattern, interval);
			if (registerNotes(ps.executeQuery()) > 0) {
				for (int i = 0; i < ids.length; i++) 
					if (!ask || confirmDelete(ids[i])) {
						ps = con.prepareStatement("DELETE FROM notes where _id = ?");
						ps.setLong(1, ids[i]);
						ps.executeUpdate();
						retval = true;
					}
			}
		} catch (Exception e) {
			Util.handleException(e);
		}
		return retval;
	}

	private boolean confirmDelete(long id) throws SQLException {
		PreparedStatement ps = con.prepareStatement("select title,created,note from notes where _id = ?");
		ps.setLong(1, id);
		ResultSet rs = ps.executeQuery();
		boolean retval = false;
		if (rs.next())
			retval = JOptionPane.YES_OPTION == JOptionPane.showConfirmDialog(
				NotePicker.this, String.format("delete '%s' on '%s' : %s ?", rs.getString(1), rs.getLong(2), rs.getString(3)), 
				caption, 
				JOptionPane.YES_NO_OPTION);
		rs.close();
		return retval;
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
			PreparedStatement ps = preparePicking(false, pattern, Util.dayInterval(time, 1));
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
			Util.handleException(e);
			return -1;
		}
	}

	private void updateOrInsert(String pattern, String dateString) {
		if (time.length == 2) {
			for (String[] record : getRecords(getText())) 
				updateOrInsert(record[1], record[0], record[2]);
		}
		else 
			updateOrInsert(pattern, dateString, getText());
	}
	
	private ResultSet query(long... time) {
		ResultSet rs = null;
		try {
			String pattern = time.length == 2 ? searchPattern : this.pattern;
			PreparedStatement ps = preparePicking(true, pattern, time);
			rs = ps.executeQuery();
		} catch (Exception e) {
			Util.handleException(e);
		}
		return rs;
	}
	
	public enum Direction { PREV, HERE, NEXT }
	
	public String move(Direction direct) {
		ResultSet rs = null;
		
		if (find(direct, time) != null) 
			rs = query(time);
		
		enableAction(1, previousBunchAvailable(time));
		enableAction(2, nextBunchAvailable(time));
		
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
					setDate(formatDate(true, time[0]));
					setPattern(searchPattern);
					note = all();
				}
			}
			else {
				if (rs.next()) {
					setDate(formatDate(false, rs.getLong(1)));
					setCategory(rs.getString(2));
					note = rs.getString(3);
				}
			
				rs.close();
			}
			
			return note;
		} catch (Exception e) {
			Util.handleException(e);
			return refreshWith(null);
		}
		finally {
			setDirty(false);
			enableAction(6, hasTextArea() && note != null);
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

	public String formatDate(boolean week, long time) {
		if (week)
			return formatWeek(time);
		else
			return Util.formatDate(time, DatePicker.dateFormat);
	}

	private String formatWeek(long time) {
		return DatePicker.weekDate(Util.weekInterval(new Date(time), 1));
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
			String dateString = formatDate(false, (Long)params[0]);
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
		if (BerichtsheftApp.export(
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

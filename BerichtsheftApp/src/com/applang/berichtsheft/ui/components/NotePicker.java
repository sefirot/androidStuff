package com.applang.berichtsheft.ui.components;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;

import javax.swing.JComboBox;
import javax.swing.JOptionPane;
import javax.swing.JTextField;

import static com.applang.SwingUtil.*;
import static com.applang.Util.*;
import static com.applang.Util2.*;
import com.applang.berichtsheft.BerichtsheftApp;

public class NotePicker extends ActionPanel
{
	public static void main(String[] args) {
		TextArea textArea = new TextArea();
		
        String title = "Berichtsheft database";
		final NotePicker notePicker = new NotePicker(textArea, 
				null,
				title);
		
		ActionPanel.createAndShowGUI(title, new Dimension(1000, 200), notePicker, textArea.textArea);
	}
	
	static boolean memoryDb = false;
	
	@Override
	protected void start(Object... params) {
		super.start(params);
		if (memoryDb)
			handleMemoryDb(true);
		else {
			dbName = getSetting("database", "databases/*");
			if (fileExists(new File(dbName)))
				initialize(dbName);
		}
	}
	
	@Override
	protected void finish(Object... params) {
		try {
			if (getCon() != null)
				getCon().close();
		} catch (SQLException e) {
			handleException(e);
		}
		putSetting("database", dbName);
		super.finish(params);
	}
	
	private JTextField date = new JTextField(20);
	@SuppressWarnings("rawtypes")
	private JComboBox category = new JComboBox();
	private JTextField categoryEdit = comboEdit(category);
	
	public NotePicker(TextComponent textArea, Object... params) {
		super(textArea, params);
		
		addButton(ActionType.DATABASE.index(), new NoteAction(ActionType.DATABASE));
		
		date.setHorizontalAlignment(JTextField.CENTER);
		addFocusObserver(date);
		add(date);
		date.addKeyListener(new KeyAdapter() {
			@Override
			public void keyTyped(KeyEvent e) {
				super.keyTyped(e);
				checkDocumentPossible();
			}
			@Override
			public void keyPressed(KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_ENTER)
					doAction(8);
				else
					super.keyPressed(e);
			}
		});
		
		addButton(ActionType.CALENDAR.index(), new NoteAction(ActionType.CALENDAR));
		
		category.setEditable(true);
		addFocusObserver(categoryEdit);
		add(category);
		categoryEdit.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_ENTER)
					doAction(8);
			}
		});
		
		addButton(ActionType.PICK.index(), new NoteAction(ActionType.PICK));
		
		addButton(ActionType.PREVIOUS.index(), new NoteAction(ActionType.PREVIOUS));
		addButton(ActionType.NEXT.index(), new NoteAction(ActionType.NEXT));
		addButton(ActionType.SPELLCHECK.index(), new NoteAction(ActionType.SPELLCHECK));
		addButton(ActionType.ADD.index(), new NoteAction(ActionType.ADD));
		addButton(ActionType.DELETE.index(), new NoteAction(ActionType.DELETE));
		addButton(ActionType.ACTIONS.index(), new NoteAction(ActionType.ACTIONS.description()));
		
		attachDropdownMenu(buttons[ActionType.ACTIONS.index()], newPopupMenu(
		    	new Object[] {ActionType.DOCUMENT.description(), documentActions[0]}, 
		    	new Object[] {ActionType.DOCUMENT.description(), documentActions[1]} 
	    ));
		
		clear();
	}
	
	class NoteAction extends Action
    {
		public NoteAction(ActionType type) {
			super(type);
        }
        
        public NoteAction(String text) {
			super(text);
		}

		@Override
        protected void action_Performed(ActionEvent ae) {
        	String dateString = getDate();
        	String pattern = getPattern();
        	
        	switch ((ActionType)getType()) {
			case DATABASE:
				updateOnRequest(true);
				dbName = chooseDatabase(dbName);
				break;
			case CALENDAR:
				dateString = pickDate(dateString);
				checkDocumentPossible();
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
				finder.keyLine(searchPattern);
				pickNote(dateString, searchPattern);
				break;
			default:
				return;
			}
        }
	}
	
	private NoteAction[] documentActions = new NoteAction[] {
			new NoteAction(ActionType.DOCUMENT), 
			new NoteAction(ActionType.TEXT),
	};

	private void checkDocumentPossible() {
		int kind = DatePicker.kindOfDate(getDate());
		documentActions[0].setEnabled(kind > 1 && getCategory().length() > 0);
	}
	
	private void clear() {
		refreshWith(null);
		enableAction(ActionType.PREVIOUS.index(), false);
		enableAction(ActionType.NEXT.index(), false);
		enableAction(ActionType.ADD.index(), hasTextArea());
		enableAction(ActionType.DELETE.index(), false);
		enableAction(ActionType.ACTIONS.index(), true);
		enableAction(ActionType.SPELLCHECK.index(), hasTextArea());
	}

	private void initialize(String dbName) {
		if (openConnection(dbName)) {
			retrieveCategories();
			searchPattern = allCategories;
			finder.keyLine(searchPattern);
			setDate(
					formatDate(0, 
							finder.keys.length > 0 ? 
							finder.epochFromKey(finder.keys[0]) : 
							now()));
			pickNote(getDate(), searchPattern);
		}
	}

	@Override
	protected String chooseDatabase(String dbName) {
		if (memoryDb)
			handleMemoryDb(false);
		
		dbName = super.chooseDatabase(dbName);
		initialize(dbName);
		
		return dbName;
	}
	
	private String memoryDbName = "";

	private void handleMemoryDb(boolean restore) {
		try {
			if (restore) {
				memoryDbName = "/tmp/memory.db";
				initialize(memoryDbName);
			}
			else if ("sqlite".equals(getScheme()) && memoryDbName.length() > 0) {
				getStmt().executeUpdate("backup to " + memoryDbName);
				memoryDbName = "";
			}
		} catch (Exception e) {
			handleException(e);
		}
		
	}

	private String pickDate(String dateString) {
		Long[] times = timeLine();
		if (times.length > 0)
			times = arrayextend(times, true, times[0] - getMillis(1) + 1);
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
			
			finder.keyLine(searchPattern);
			pickNote(dateString, finder.pattern);
			retrieveCategories();
		}
	}

	private void deleteOnRequest(String dateString, String pattern) {
		if (remove(true, pattern, dateString)) {
			finder.keyLine(searchPattern);
			pickNote(dateString, pattern);
			retrieveCategories();
		}
	}

	@Override
	public boolean openConnection(String dbPath, Object... params) {
		try {
			if (super.openConnection(dbPath, arrayextend(params, true, "notes")))
				return true;
		    
			if ("sqlite".equals(getScheme()))
				getStmt().execute("CREATE TABLE notes (" +
			    		"_id INTEGER PRIMARY KEY," +
			    		"title TEXT," +
			    		"note TEXT," +
			    		"created INTEGER," +
			    		"modified INTEGER, " +
			    		"UNIQUE (created, title))");
			else if ("mysql".equals(getScheme())) 
				getStmt().execute("CREATE TABLE notes (" +
			    		"_id BIGINT PRIMARY KEY," +
			    		"title VARCHAR(40)," +
			    		"note TEXT," +
			    		"created BIGINT," +
			    		"modified BIGINT, " +
			    		"UNIQUE (created, title))");
		    
		    return true;
		} catch (Exception e) {
			handleException(e);
			return false;
		}
	}
	
	@Override
	protected void afterConnecting() throws Exception {
		if (memoryDb && fileExists(new File(memoryDbName)))
			getStmt().executeUpdate("restore from " + memoryDbName);
	}

	private long[] getTime(String dateString) {
		int kind = DatePicker.kindOfDate(dateString);
		switch (kind) {
		case 3:
			return DatePicker.monthInterval(dateString, 1);
		case 2:
			return DatePicker.weekInterval(dateString, 1);
		default:
			Date date = parseDate(dateString);
			return new long[] {date == null ? now() : date.getTime()};
		}
	}
	
	public String getDate() {
		String text = date.getText();
		return text;
	}
	
	public void setDate(final String text) {
		try {
			Action.blocked(new Job<Void>() {
				public void dispatch(Void v, Object[] params) throws Exception {
					date.setText(text);
				}
			}, null);
		} catch (Exception e) {}
	}
	
	@SuppressWarnings("unchecked")
	private void retrieveCategories() {
		try {
			String categ = getCategory();
			category.removeAllItems();
			
			PreparedStatement ps = getCon().prepareStatement("select distinct title from notes order by title");
			ResultSet rs = ps.executeQuery();
			while (rs.next())
				category.addItem(rs.getString(1));
			
			category.addItem(itemAll);
			category.addItem(itemBAndB);
			setCategory(categ);
		} catch (Exception e) {
			handleException(e);
		}
	}
	
	public void setCategory(final String t) {
		try {
			Action.blocked(new Job<Void>() {
				public void dispatch(Void v, Object[] params) throws Exception {
					categoryEdit.setText(t);
				}
			}, null);
		} catch (Exception e) {}
	}

	public String getCategory() {
		return categoryEdit.getText();
	}
	
	public static final String allDates = "";
	public static final String allCategories = ".*";
	public static final String itemAll = "-- all --";
	public static final String bAndB = "(?i)(bemerk\\w*|bericht\\w*)";
	public static final String itemBAndB = "Berichte & Bemerkungen";

	public void setPattern(String p) {
		finder.pattern = p;
		
		for (Object value : finder.specialPatterns.getValues())
			if (p.equals(value)) 
				p = finder.specialPatterns.getKey(value).toString();
		
		setCategory(p);
	}

	public String getPattern() {
		String c = getCategory();
		
		for (Object key : finder.specialPatterns.getKeys())
			if (c.equals(key)) 
				c = finder.specialPatterns.getValue(key).toString();
		
		return c;
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
				ValList list = new ValList();
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
			String pattern = param(allCategories, 0, params);
			PreparedStatement ps = getCon().prepareStatement("select distinct created FROM notes where title regexp ? order by created");
			ps.setString(1, pattern);
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				timelist.add(rs.getLong(1));
			}
			rs.close();
		} catch (Exception e) {}
		return timelist.toArray(new Long[0]);
	}
	
	String searchPattern = allCategories;
	
	public class NoteFinder
    {
		public BidiMap specialPatterns = new BidiMap();
		
		public NoteFinder() {
			specialPatterns.put(itemAll, allCategories);
			specialPatterns.put(itemBAndB, bAndB);
		}
		
		String pattern = "";
		String[] keys = null;
		long[] epoch = new long[0];
		
		public String[] keyLine(String pattern) {
			ArrayList<String> keylist = new ArrayList<String>();
			try {
				PreparedStatement ps = getCon().prepareStatement("select created,title FROM notes where title regexp ? order by created,title");
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
		
		public long epochFromKey(String key) {
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
				long t1 = epochFromKey(o1);
				long t2 = epochFromKey(o2);
				
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
		
		private int criterion(long epoch, String pattern) {
			String value = keyValue(epoch, pattern);
			if (Arrays.asList(specialPatterns.getValues()).contains(pattern)) {
				ArrayList<Integer> a = new ArrayList<Integer>();
				ArrayList<Integer> b = new ArrayList<Integer>();
				ArrayList<Integer> c = new ArrayList<Integer>();
				int comp = 0;
				for (int i = 0; i < this.keys.length; i++) {
					String key = this.keys[i];
					if ((comp = partialComparator.compare(key, value)) == 0 && categoryFromKey(key).matches(pattern))
						a.add(i);
					else if (comp < 0)
						b.add(i);
					else
						c.add(i);
				}
				if (a.size() > 0) 
					return Collections.min(a);
				else if (c.size() > 0)
					return -Collections.min(c) - 1;
				else if (b.size() > 0)
					return -Collections.max(b) - 2;
				else
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
		
		public boolean bunchAvailable(long... epoch) {
			index[0] = criterion(epoch[0], pattern);
			if (epoch.length > 1) {
				index[1] = criterion(epoch[1] - 1, pattern);
				return index[0] > -1 || index[0] != index[1];
			}
			else
				return index[0] > -1;
		}
		
		public boolean nextBunchAvailable(long... epoch) {
			if (epoch.length > 1) {
				int pointer = pointer(epoch[1], pattern);
				return pointer > 0 && pointer < keys.length;
			} else 
				return pointer(epoch[0], pattern) < keys.length - 1;
		}
		
		public boolean previousBunchAvailable(long... epoch) {
			int pointer = pointer(epoch[0], pattern);
			return pointer > 0;
		}
	
		public String find(Direction direct, long... epoch) {
			boolean available = bunchAvailable(epoch);
			
			int index = pointer(this.index[0]);
			if (direct == Direction.HERE) 
				if (!available) {
					message(String.format("'%s' on %s not available", pattern, formatDate(epoch.length, epoch[0])));
					if (index >= keys.length) {
						direct = Direction.PREV;
						available = true;
					}
					else
						direct = Direction.NEXT;
				}
			
			boolean next = direct == Direction.NEXT;
			if (next && epoch.length > 1) 
				index = pointer(this.index[1]);
			else {
				if (available && direct != Direction.HERE)
					index += next ? 1 : -1;
			}
			if (index < 0 || index >= keys.length)
				return null;
			
			String key = keys[index];
			
			long t = epochFromKey(key);
			if (epoch.length > 1) {
				if (epoch.length == 2)
					this.epoch = DatePicker.weekInterval(formatWeek(t), 1);
				else
					this.epoch = DatePicker.monthInterval(formatMonth(t), 1);
			}
			else
				this.epoch = new long[] {t};
			this.pattern = categoryFromKey(key);
			
			return key;
		}
    }
	
	public NoteFinder finder = new NoteFinder();
	
	public void pickNote(String dateString, String pattern) {
		if (updateMode())
			return;
			
		clear();
		setDate(dateString);
		
		try {
			checkDocumentPossible();
			
			finder.pattern = pattern;
			finder.epoch = getTime(dateString);
			if (nullOrEmpty(finder.epoch)) {
				message(String.format("value for '%s' doesn't make sense", ActionType.DATE.description()));
				date.requestFocus();
				return;
			}
			
			setText(move(Direction.HERE));			
		} catch (Exception e) {
			handleException(e);
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
			ps = getCon().prepareStatement(sql + " where title regexp ? and created = ?");
			ps.setString(1, pattern);
			ps.setLong(2, time[0]);
		}
		else {
			ps = getCon().prepareStatement(sql + " where created between ? and ? and title regexp ?" + orderClause);
			ps.setLong(1, time[0]);
			ps.setLong(2, time[1] - 1);
			ps.setString(3, pattern);
		}
		return ps;
	}

	public int update(long id, String note) throws Exception {
		PreparedStatement ps = getCon().prepareStatement("UPDATE notes SET note = ?, modified = ? where _id = ?");
		ps.setString(1, note);
		ps.setLong(2, now());
		ps.setLong(3, id);
		return ps.executeUpdate();
	}

	public int insert(long id, String note, String category, long time) throws Exception {
		PreparedStatement ps = getCon().prepareStatement("INSERT INTO notes (_id,title,note,created,modified) VALUES (?,?,?,?,?)");
		ps.setLong(1, id);
		ps.setString(2, category);
		ps.setString(3, note);
		ps.setLong(4, time);
		ps.setLong(5, now());
		return ps.executeUpdate();
	}
	
	public int delete(String pattern, String dateString, boolean delete) throws Exception {
		Connection con = getCon();
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

	public boolean remove(boolean ask, String pattern, String dateString) {
		boolean retval = false;
		try {
			long[] interval = getTime(dateString);
			if (interval.length == 1)
				interval = dayInterval(interval[0], 1);
			PreparedStatement ps = preparePicking(false, pattern, interval);
			if (registerNotes(ps.executeQuery()) > 0) {
				for (int i = 0; i < ids.length; i++) 
					if (!ask || confirmDelete(ids[i])) {
						ps = getCon().prepareStatement("DELETE FROM notes where _id = ?");
						ps.setLong(1, ids[i]);
						ps.executeUpdate();
						retval = true;
					}
			}
		} catch (Exception e) {
			handleException(e);
		}
		return retval;
	}

	private boolean confirmDelete(long id) throws SQLException {
		PreparedStatement ps = getCon().prepareStatement("select title,created,note from notes where _id = ?");
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
		ResultSet rs = getStmt().executeQuery("SELECT max(_id) FROM notes");
		long id = rs.next() ? rs.getLong(1) : -1;
		rs.close();
		return 1 + id;
	}

	public long updateOrInsert(String pattern, String dateString, String note) {
		try {
			long time = parseDate(dateString).getTime();
			PreparedStatement ps = preparePicking(false, pattern, dayInterval(time, 1));
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

	private void updateOrInsert(String pattern, String dateString) {
		if (finder.epoch.length == 2) {
			for (String[] record : getRecords(getText())) 
				updateOrInsert(record[1], record[0], record[2]);
		}
		else 
			updateOrInsert(pattern, dateString, getText());
	}
	
	private ResultSet query(long... time) {
		ResultSet rs = null;
		try {
			String pattern = time.length > 1 ? searchPattern : finder.pattern;
			PreparedStatement ps = preparePicking(true, pattern, time);
			rs = ps.executeQuery();
		} catch (Exception e) {
			handleException(e);
		}
		return rs;
	}
	
	public enum Direction { PREV, HERE, NEXT }
	
	public String move(Direction direct) {
		ResultSet rs = null;
		
		if (finder.find(direct, finder.epoch) != null) 
			rs = query(finder.epoch);
		
		enableAction(ActionType.PREVIOUS.index(), finder.previousBunchAvailable(finder.epoch));
		enableAction(ActionType.NEXT.index(), finder.nextBunchAvailable(finder.epoch));
		
		return refreshWith(rs);
	}

	private String refreshWith(ResultSet rs) {
		String note = null;
		
		try {
			if (rs == null) {
				setDate("");
				return null;
			}
			
			int kind = finder.epoch.length;
			if (kind > 1) {
				if (registerNotes(rs) > 0) {
					setDate(formatDate(kind, finder.epoch[0]));
					setPattern(searchPattern);
					note = all();
				}
			}
			else {
				if (rs.next()) {
					setDate(formatDate(kind, rs.getLong(1)));
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
			enableAction(ActionType.DELETE.index(), hasTextArea() && note != null);
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

	public String formatDate(int kind, long time) {
		switch (kind) {
		case 2:
			return formatWeek(time);
		case 3:
			return formatMonth(time);
		default:
			return com.applang.Util.formatDate(time, DatePicker.calendarFormat);
		}
	}

	private String formatWeek(long time) {
		return DatePicker.weekDate(weekInterval(new Date(time), 1));
	}

	private String formatMonth(long time) {
		return DatePicker.monthDate(monthInterval(new Date(time), 1));
	}

	public Date parseDate(String dateString) {
		try {
			return toDate(dateString, DatePicker.calendarFormat);
		} catch (Exception e) {
			return null;
		}
	}

	public String wrapNote(Object... params) throws Exception {
		if (isAvailable(0, params) && params[0] instanceof Long) {
			String dateString = formatDate(1, (Long)params[0]);
			return String.format(noteFormat2, dateString, 
					param("", 1, params), 
					param("", 2, params));
		}
		else
			return String.format(noteFormat1, 
					param("", 0, params), 
					param("", 1, params));
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
		
		for (MatchResult m : findAllIn(text, notePattern2)) 
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

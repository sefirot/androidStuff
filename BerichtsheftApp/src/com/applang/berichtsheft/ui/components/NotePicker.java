package com.applang.berichtsheft.ui.components;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
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
import java.util.Date;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;

import com.applang.Util;
import com.applang.berichtsheft.BerichtsheftApp;
import com.applang.berichtsheft.ui.BerichtsheftTextArea;

@SuppressWarnings("rawtypes") 
public class NotePicker extends JPanel implements ActionListener
{
	public static void main(String[] args) {
		JFrame f = new JFrame(NotePicker.class.getSimpleName());
		f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		BerichtsheftTextArea textArea = new BerichtsheftTextArea();
		
		Container contentPane = f.getContentPane();
		contentPane.setPreferredSize(new Dimension(1000, 200));
		
		NotePicker notePicker = new NotePicker(textArea, 
				null,
				"Berichtsheft database");
		contentPane.add(notePicker, BorderLayout.NORTH);

		JScrollPane scroll = new JScrollPane(textArea.textArea);
		contentPane.add(scroll, BorderLayout.CENTER);
		
		f.pack();
		f.setLocationRelativeTo(null);
		f.setVisible(true);
	}
	
	private JTextField date = new JTextField(20);
	private JComboBox titleCombo = new JComboBox();
	
    String dbName;
	String[] titles = new String[] {"Bericht", "Bemerkung"};
	String[] patterns = new String[] {"(?i).*bericht.*", "(?i).*bemerk.*"};
	
	public static ImageIcon iconFrom(String path) {
		URL url = NotePicker.class.getResource("/images/" + path);
		return new ImageIcon(url);
	}
	
	public static String[] resources = new String[] {
		"category", "calendar date / week date", 
		"update_16x16.png", "update note", 
		"calendar_16x16.png", "pick date from calendar", 
		"Arrow Left_16x16.png", "previous note", 
		"Arrow Right_16x16.png", "next note", 
		"book_open_16x16.png", "choose database", 
		"merge_16x16.png", "fill document", 
		"plus_16x16.png", "add note", 
		"minus_16x16.png", "delete note", 
		"abc_16x16.png", "spell check", 
		"refresh_16x16.png", "refresh view", 
	};
	
	private JButton[] buttons = new JButton[9];
	
	JButton button(int n) {
		int m = n + 2;
		buttons[n] = new JButton("", iconFrom(resources[2*m]));
		buttons[n].setToolTipText(resources[2*m+1]);
		return buttons[n];
	}
	
	void changeButtonFace(int n, int f) {
		int m = f + 2;
		buttons[n].setIcon(iconFrom(resources[2*m]));
		buttons[n].setToolTipText(resources[2*m+1]);
	}
    
	boolean dirty = false;
	
	public boolean isDirty() {
		return dirty;
	}

	public void setDirty(boolean dirty) {
		this.dirty = dirty;
		changeButtonFace(5, this.dirty ? -1 : 5);
	}

	TextComponent textArea = null;

	private boolean hasTextArea() {
		return this.textArea != null;
	}

	private void setText(String text) {
		if (hasTextArea()) 
			this.textArea.setText(text);
	}

	private String getText() {
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

	@SuppressWarnings("unchecked")
	public NotePicker(TextComponent textArea, Object... params) {
		this.textArea = textArea;
		setupTextArea();
		
		this.dbName = Util.paramString("databases/*", 0, params);
		final String caption = Util.paramString("Notes database", 1, params);
		
		this.setLayout(new FlowLayout(FlowLayout.LEADING));
		
		JButton open = button(3);
		add(open);
		open.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				updateOnRequest(caption);
				chooseDatabase(caption);
			}
		});
		
		date.setHorizontalAlignment(JTextField.CENTER);
		date.setToolTipText(resources[1]);
		date.addKeyListener(new KeyAdapter() {
			@Override
			public void keyTyped(KeyEvent e) {
				checkMergePossible();
			}
		});
		Util.addFocusObserver(date);
		add(date);
		date.addActionListener(this);
		
		final JButton pick = button(0);
		pick.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				String dateString = new DatePicker(pick, getDate()).getDateString();
				NotePicker.this.requestFocus();
				setDate(dateString);
				kindOfDate = kindOfDate(dateString);
        		checkMergePossible();
        		pick();
				date.requestFocusInWindow();
			}
		});
		add(pick);
		
		titleCombo.setEditable(true);
		titleCombo.setToolTipText(resources[0]);
		for (String t : titles)
			titleCombo.addItem(t);
		titleCombo.setSelectedIndex(-1);
		Util.addFocusObserver(Util.comboEdit(titleCombo));
		add(titleCombo);
		titleCombo.addActionListener(this);
		
		JButton refresh = button(8);
		add(refresh);
		refresh.addActionListener(this);
		
		JButton prev = button(1);
		add(prev);
		prev.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				updateOnRequest(caption);
				setText(previous());
			}
		});
		JButton next = button(2);
		add(next);
		next.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				updateOnRequest(caption);
				setText(next());
			}
		});
		
		JButton spell = button(7);
		add(spell);
		spell.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (hasTextArea()) {
					setDirty(NotePicker.this.textArea.spellcheck() || isDirty());
				}
			}
		});
		
		JButton insert = button(5);
		add(insert);
		insert.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				if (hasTextArea()) {
					updateOrInsert(getPattern(), getDate(), true);
				}
			}
		});
		JButton delete = button(6);
		add(delete);
		delete.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				if (hasTextArea() && kindOfDate == 1) {
					String dateString = getDate();
					if (JOptionPane.YES_OPTION == JOptionPane.showConfirmDialog(
							NotePicker.this, String.format("delete '%s' on '%s'", getTitle(), dateString), 
							caption, 
							JOptionPane.YES_NO_OPTION))
					{
						try {
							delete(getPattern(), dateString);
							display();
						} catch (Exception e) {
							handle(e);
						}
					}
				}
			}
		});
		JButton merge = button(4);
		add(merge);
		merge.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String dateString = getDate();
				if (kindOfDate == 2) {
					updateOnRequest(caption);
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
		});
		
		clear();
	}
	
	private int kindOfDate = 0;

	private int kindOfDate(String dateString) {
		if (DatePicker.isWeekDate(dateString))
			return 2;
		else if (DatePicker.isCalendarDate(dateString))
			return 1;
		else
			return 0;
	}

	private void checkMergePossible() {
		buttons[4].setEnabled(kindOfDate == 2);
	}

	private void updateOnRequest(String caption) {
		String dateString = getDate();
		if (isDirty() && JOptionPane.YES_OPTION == JOptionPane.showConfirmDialog(
				NotePicker.this, String.format("update '%s' on '%s'", getTitle(), dateString), 
				caption, 
				JOptionPane.YES_NO_OPTION))
		{
			updateOrInsert(getPattern(), dateString, false);
		}
	}

	private void chooseDatabase(String caption) {
		File file = Util.chooseFile(true, NotePicker.this, caption, new File(dbName), null);
		if (file != null) {
			dbName = file.getPath();
			if (openConnection(dbName)) 
				display();
			
			titleCombo.requestFocusInWindow();
		}
	}

	private Connection con = null;

	public Connection getCon() {
		return con;
	}

	public boolean openConnection(String db, Object... params) {
		ResultSet rs = null;
		try {
			if (con != null && !con.isClosed())
				con.close();
			
			clear();
			
			String driver = Util.paramString("org.sqlite.JDBC", 1, params);
			Class.forName(driver);
			
			String scheme = Util.paramString("sqlite", 0, params);
			con = DriverManager.getConnection("jdbc:" + scheme + ":" + db);
			Statement st = con.createStatement();
			
			String database = Util.paramString("sqlite_master", 2, params);
			if ("sqlite".equals(scheme))
				rs = st.executeQuery("select name from " + database + " where type = 'table'");
			else if ("mysql".equals(scheme)) {
				rs = st.executeQuery("show databases;");
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
	        		st.execute(String.format("use %s;", database));
	        	
				rs = st.executeQuery("show tables in " + database + ";");
			}
			
		    while (rs.next()) 
		        if (rs.getString(1).equals("notes")) 
		        	return true;
		    
			if ("sqlite".equals(scheme))
			    st.execute("CREATE TABLE notes (" +
			    		"_id INTEGER PRIMARY KEY," +
			    		"title TEXT," +
			    		"note TEXT," +
			    		"created INTEGER," +
			    		"modified INTEGER)");
			else if ("mysql".equals(scheme)) 
			    st.execute("CREATE TABLE notes (" +
			    		"_id BIGINT PRIMARY KEY," +
			    		"title TEXT," +
			    		"note TEXT," +
			    		"created BIGINT," +
			    		"modified BIGINT)");
		    
		    return true;
		} catch (Exception e) {
			handle(e);
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
	
	private Long[] ids = null;
	private int index = -1;
	public Object[][] records = null;
	
	public int registerNotes(ResultSet rs) throws SQLException {
		ArrayList<Long> idlist = new ArrayList<Long>();
		ArrayList<Object[]> reclist = new ArrayList<Object[]>();
		
		ResultSetMetaData rsmd = rs.getMetaData();
		int cols = rsmd.getColumnCount();
		boolean c = cols > 1;
		
		while (rs.next()) {
			idlist.add(rs.getLong(1));
			if (c) {
				ArrayList<Object> list = new ArrayList<Object>();
				for (int i = 2; i <= cols; i++)
					list.add(rs.getObject(i));
				reclist.add(list.toArray());
			}
		}
		rs.close();
		
		records = reclist.toArray(new Object[0][]);
		index = ids != null && idlist.size() == 1 ? 
				Arrays.asList(ids).indexOf(idlist.get(0)) : -1;
		if (index < 0)
			ids = idlist.toArray(new Long[0]);
		else
			index--;
		
		boolean b = ids.length > 0;
		buttons[1].setEnabled(b || c);
		buttons[2].setEnabled(b || c);
		buttons[5].setEnabled(hasTextArea());
		buttons[6].setEnabled(b);
		return ids.length;
	}
	
	private boolean retrieveNotes() throws SQLException {
		PreparedStatement ps = con.prepareStatement("select _id from notes where title regexp ? order by created, title");
		ps.setString(1, getPattern());
		int numberOfNotes = registerNotes(ps.executeQuery());
		return numberOfNotes > 0;
	}
	
	public void display(Object... params) {
		setDate("");
		try {
			if (retrieveNotes()) {
				this.index = Util.paramInteger(this.index, 0, params);
				setText(next());
			}
		} catch (Exception e) {
			handle(e);
		}
	}
	
	public PreparedStatement preparePicking(String pattern, long... time) throws Exception {
		if (con == null || time.length < 1)
			return null;
		
		PreparedStatement ps;
		if (time.length < 2) {
			ps = con.prepareStatement("SELECT _id FROM notes where title regexp ? and created = ?");
			ps.setString(1, pattern);
			ps.setLong(2, time[0]);
		}
		else {
			ps = con.prepareStatement("SELECT _id,created,title,note FROM notes where created between ? and ? and title regexp ?");
			ps.setLong(1, time[0]);
			ps.setLong(2, time[1] - 1);
			ps.setString(3, pattern);
		}
		return ps;
	}

	private int update(Long id, String note) throws Exception {
		PreparedStatement ps = con.prepareStatement("UPDATE notes SET note = ?, modified = ? where _id = ?");
		ps.setString(1, note);
		ps.setLong(2, new Date().getTime());
		ps.setLong(3, id);
		return ps.executeUpdate();
	}

	private int insert(Long id, String title, String note, long time) throws Exception {
		PreparedStatement ps = con.prepareStatement("INSERT INTO notes (_id,title,note,created,modified) VALUES (?,?,?,?,?)");
		ps.setLong(1, id);
		ps.setString(2, title);
		ps.setString(3, note);
		ps.setLong(4, time);
		ps.setLong(5, time);
		return ps.executeUpdate();
	}
	
	private void delete(String pattern, String dateString) throws Exception {
		long time = parseDate(dateString).getTime();
		PreparedStatement ps = preparePicking(pattern, time);
		ResultSet rs = ps.executeQuery();
		if (rs.next()) {
			ps = con.prepareStatement("DELETE FROM notes where title regexp ? and created = ?");
			ps.setString(1, pattern);
			ps.setLong(2, time);
			ps.executeUpdate();
		}
		rs.close();
	}

	public long updateOrInsert(String pattern, String dateString, String note) {
		try {
			long time = parseDate(dateString).getTime();
			PreparedStatement ps = preparePicking(pattern, time);
			ResultSet rs = ps.executeQuery();
			
			long id;
			if (rs.next()) {
				id = rs.getLong(1);
				update(id, note);
			}
			else {
				rs.close();
				rs = con.createStatement().executeQuery("SELECT max(_id) FROM notes");
				id = rs.next() ? rs.getLong(1) : -1;
				insert(++id, getTitle(), note, time);
			}
			
			rs.close();
			return id;
		} catch (Exception e) {
			handle(e);
			return -1;
		}
	}

	private void updateOrInsert(String pattern, String dateString, boolean retrieve) {
		int index = -1;
		
		if (kindOfDate == 2) {
			for (String[] record : getRecords(getText())) 
				updateOrInsert(record[1], record[0], record[2]);
		}
		else {
			long id = updateOrInsert(pattern, dateString, getText());
			index = Arrays.asList(ids).indexOf(id) - 1;
		}
		
		if (retrieve) 
			display(index);
	}
	
	private ResultSet query(long id) {
		try {
			PreparedStatement ps = con.prepareStatement("SELECT note,title,created FROM notes where _id=?");
			ps.setLong(1, id);
			return ps.executeQuery();
		} catch (Exception e) {
			handle(e);
			return null;
		}
	}
	
	long[] week = null;
	
	private ResultSet queryWeek(long time) {
		ResultSet rs = null;
		try {
			week = DatePicker.weekInterval(Util.formatDate(time, DatePicker.weekFormat), 1);
			PreparedStatement ps = preparePicking(getPattern(), week);
			rs = ps.executeQuery();
			setDate(DatePicker.weekDate(week));
		} catch (Exception e) {
			handle(e);
		}
		return rs;
	}

	private Long findTime(boolean next) {
		Long retval = null;
		try {
			String sql;
			if (week == null)
				sql = "SELECT " + (next ? "min" : "max") + "(created) FROM notes";
			else 
				sql = "SELECT created FROM notes where created " +
					(next ? ">= ?" : "<= ?");
			PreparedStatement ps = con.prepareStatement(sql);
			if (week != null)
				ps.setLong(1, next ? week[1] : week[0]);
			ResultSet rs = ps.executeQuery();
			if (rs.next())
				retval = rs.getLong(1);
			rs.close();
		} catch (Exception e) {
			handle(e);
		}
		return retval;
	}
	
	public String next() {
		ResultSet rs = null;
		
		if (kindOfDate == 2) {
			Long time = findTime(true);
			if (time != null) 
				rs = queryWeek(time);
			buttons[2].setEnabled(time != null);
		} else {
			index++;
			if (index < ids.length)
				rs = query(ids[index]);
			else 
				buttons[2].setEnabled(false);
		}
		buttons[1].setEnabled(ids.length > 0);
		
		return refreshWith(rs);
	}

	public String previous() {
		ResultSet rs = null;
		
		if (kindOfDate == 2) {
			Long time = findTime(false);
			if (time != null) 
				rs = queryWeek(time);
			buttons[1].setEnabled(time != null);
		} else {
			index--;
			if (index > -1)
				rs = query(ids[index]);
			else 
				buttons[1].setEnabled(false);
		}
		buttons[2].setEnabled(ids.length > 0);
		
		return refreshWith(rs);
	}

	private String refreshWith(ResultSet rs) {
		try {
			actionBlocked = true;
			
			if (rs != null && kindOfDate == 2) 
				if (registerNotes(rs) > 0) 
					return all();
			
			if (rs == null) {
//				titleCombo.setSelectedIndex(-1);
//				setDate("");
				setFontColor(Color.GRAY);
				week = null;
				return "";
			}
			
			setTitle(rs.getString(2));
			setDate(formatDate(rs.getLong(3)));
			
			String note = rs.getString(1);
			rs.close();
			return note;
		} catch (Exception e) {
			handle(e);
			return refreshWith(null);
		}
		finally {
			actionBlocked = false;
			setDirty(false);
			buttons[6].setEnabled(rs != null);
		}
	}

	private void pick() {
		String pattern = getPattern();
		String dateString = getDate();
		try {
			PreparedStatement ps;
			
			switch (kindOfDate) {
			case 2:
				week = DatePicker.weekInterval(dateString, 1);
				ps = preparePicking(pattern, week);
				break;

			case 1:
				long time = parseDate(dateString).getTime();
				ps = preparePicking(pattern, time);
				break;

			default:
				setDate("");
				
				ps = con.prepareStatement("SELECT _id FROM notes where title regexp ?");
				ps.setString(1, pattern);
				break;
			}
			
			registerNotes(ps.executeQuery());
			
			setText(kindOfDate == 2 ? 
					all() : 
					next());
		} catch (Exception e) {
			handle(e);
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

	public String formatDate(long time) throws Exception {
		return Util.formatDate(time, DatePicker.dateFormat);
	}

	public Date parseDate(String dateString) throws Exception {
		return Util.parseDate(dateString, DatePicker.dateFormat);
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
	
	private void clear() {
		refreshWith(null);
		buttons[1].setEnabled(false);
		buttons[2].setEnabled(false);
		buttons[5].setEnabled(false);
		buttons[6].setEnabled(false);
		buttons[4].setEnabled(false);
	}

	private void handle(Exception e) {
		if (e != null) 
			e.printStackTrace();
	}

	private boolean isFontColor(Color clr) {
		return date.getForeground() == clr;
	}

	private void setFontColor(Color clr) {
		date.setForeground(clr);
		Util.comboEdit(titleCombo).setForeground(clr);
	}
	
	public String getDate() {
		return date.getText();
	}
	
	public void setDate(String text) {
		date.setText(text);
		setFontColor(Color.BLACK);
	}
	
	public void setTitle(String title) {
		JTextField comboEdit = Util.comboEdit(titleCombo);
		comboEdit.setText(title);
		setFontColor(Color.BLACK);
		for (int i = 0; i < patterns.length; i++) 
			if (title.matches(patterns[i]))
				titleCombo.setSelectedIndex(i);
	}

	public String getTitle() {
		return Util.comboEdit(titleCombo).getText();
	}
	
	public String getPattern() {
		String title = getTitle();
		for (int i = 0; i < patterns.length; i++) 
			if (title.matches(patterns[i]))
				return patterns[i];
		return title.length() > 0 ? title : ".*";
	}
	
	private boolean actionBlocked = false;

	@Override
	public void actionPerformed(ActionEvent ae) {
		if (con != null && !actionBlocked) {
			pick();
		}
	}

}

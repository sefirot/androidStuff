package com.applang.berichtsheft.ui.components;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.KeyboardFocusManager;
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
import java.sql.SQLException;
import java.sql.Statement;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.text.JTextComponent;

import com.applang.Util;
import com.applang.berichtsheft.Main;
import com.applang.berichtsheft.ui.BerichtsheftTextArea;

@SuppressWarnings("rawtypes") 
public class NotePicker extends JPanel implements ActionListener
{
	public static void main(String[] args) {
		JFrame f = new JFrame(NotePicker.class.getSimpleName());
		f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		JTextArea textArea = new BerichtsheftTextArea();
		
		Container contentPane = f.getContentPane();
		NotePicker notePicker = new NotePicker(textArea, 
				null,
				"Berichtsheft database");
		contentPane.add(notePicker, BorderLayout.NORTH);
		contentPane.add(new JScrollPane(textArea), BorderLayout.CENTER);
		
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
		URL url = NotePicker.class.getResource("images/" + path);
		return new ImageIcon(url);
	}
	
	public static String[] resources = new String[] {
		"category of the note", "date / press 'ENTER' to refresh the view", 
		"update_16x16.png", "update note", 
		"calendar_16x16.png", "pick date from calendar", 
		"Arrow Left_16x16.png", "browse previous note", 
		"Arrow Right_16x16.png", "browse next note", 
		"book_open_16x16.png", "choose database", 
		"merge_16x16.png", "fill document", 
		"plus_16x16.png", "add note", 
		"minus_16x16.png", "delete note", 
		"abc_16x16.png", "spell check", 
	};
	
	public JButton[] buttons = new JButton[8];
	
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

	JTextComponent textArea = null;

	private boolean hasTextArea() {
		return this.textArea != null;
	}
	
	private void setupTextArea(JTextComponent textArea) {
		this.textArea = textArea;
		if (hasTextArea()) {
			this.textArea.addKeyListener(new KeyAdapter() {
				@Override
				public void keyTyped(KeyEvent e) {
					setDirty(true);
				}
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
	}

	@SuppressWarnings("unchecked")
	public NotePicker(JTextComponent textArea, Object... params) {
		setupTextArea(textArea);
		
		this.dbName = Util.paramString("databases/*", 0, params);
		final String caption = Util.paramString("Notes database", 1, params);
		
		this.setLayout(new FlowLayout());
		
		JButton open = button(3);
		add(open);
		open.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				updateOnRequest(caption);
				chooseDatabase(caption);
			}
		});
		
		titleCombo.setEditable(true);
		titleCombo.setToolTipText(resources[0]);
		for (String t : titles)
			titleCombo.addItem(t);
		titleCombo.setSelectedIndex(-1);
		Util.addFocusObserver(Util.comboEdit(titleCombo));
		add(titleCombo);
		
		date.setHorizontalAlignment(JTextField.CENTER);
		date.setToolTipText(resources[1]);
		date.addKeyListener(new KeyAdapter() {
			@Override
			public void keyTyped(KeyEvent e) {
				checkMergePossible(date.getText());
			}
		});
		date.addActionListener(this);
		Util.addFocusObserver(date);
		add(date);
		
		final JButton pick = button(0);
		pick.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				String dateString = new DatePicker(pick, date.getText()).getDateString();
				NotePicker.this.requestFocus();
				date.setText(dateString);
        		checkMergePossible(dateString);
				date.requestFocusInWindow();
			}
		});
		add(pick);
		
		JButton prev = button(1);
		add(prev);
		prev.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				updateOnRequest(caption);
				setNote(previous());
			}
		});
		JButton next = button(2);
		add(next);
		next.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				updateOnRequest(caption);
				setNote(next());
			}
		});
		
		JButton spell = button(7);
		add(spell);
		spell.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (hasTextArea()) {
					setDirty(Main.spell(NotePicker.this.textArea) || isDirty());
				}
			}
		});
		
		JButton insert = button(5);
		add(insert);
		insert.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				String dateString = date.getText();
				if (hasTextArea() && DatePicker.isCalendarDate(dateString)) {
					updateOrInsert(getPattern(), dateString, true);
				}
			}
		});
		JButton delete = button(6);
		add(delete);
		delete.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				String dateString = date.getText();
				if (hasTextArea() && DatePicker.isCalendarDate(dateString)) 
					if (JOptionPane.YES_OPTION == JOptionPane.showConfirmDialog(
							NotePicker.this, String.format("delete '%s' on '%s'", getTitle(), dateString), 
							caption, 
							JOptionPane.YES_NO_OPTION))
					{
						try {
							delete(getPattern(), dateString);
							if (retrieveNotes())
								setNote(next());
						} catch (Exception e) {
							handle(e);
						}
					}
			}
		});
		JButton merge = button(4);
		add(merge);
		merge.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String dateString = date.getText();
				if (DatePicker.isWeekDate(dateString)) {
					updateOnRequest(caption);
					int[] weekDate = DatePicker.parseWeekDate(dateString);
					String docName = "Tagesberichte_" + String.format("%d_%d", weekDate[1], weekDate[0]) + ".odt";
					if (Main.merge(
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

	private void checkMergePossible(String dateString) {
		buttons[4].setEnabled(DatePicker.isWeekDate(dateString));
	}

	private void updateOnRequest(String caption) {
		String dateString = date.getText();
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
				setNote(next());
			
			titleCombo.requestFocusInWindow();
		}
	}

	private Connection con = null;

	public Connection getConnection() {
		return con;
	}

	public boolean openConnection(String dbName) {
		ResultSet rs = null;
		try {
			if (con != null && !con.isClosed())
				con.close();
			
			clear();
			
			Class.forName("org.sqlite.JDBC");
			con = DriverManager.getConnection("jdbc:sqlite:" + dbName);
			Statement st = con.createStatement();
			
			rs = st.executeQuery("select name from sqlite_master where type = 'table'");
		    while (rs.next()) {
		        if	(rs.getString(1).equals("notes")) {
		        	return retrieveNotes();
		        }
		    }
		    
		    st.execute("CREATE TABLE notes (" +
		    		"_id INTEGER PRIMARY KEY," +
		    		"title TEXT," +
		    		"note TEXT," +
		    		"created INTEGER," +
		    		"modified INTEGER)");
		    
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

	public int registerNotes(PreparedStatement ps) throws SQLException {
		ArrayList<Long> list = new ArrayList<Long>();
		
		ResultSet rs = ps.executeQuery();
		while (rs.next())
			list.add(rs.getLong(1));
		rs.close();
		
		ids = Util.arraycast(list.toArray(), new Long[0]);
		index = -1;
		
		boolean b = ids.length > 0;
		buttons[1].setEnabled(b);
		buttons[2].setEnabled(b);
		buttons[5].setEnabled(hasTextArea());
		buttons[6].setEnabled(b);
		return ids.length;
	}
	
	private boolean retrieveNotes() throws SQLException {
		date.setText("");
		PreparedStatement ps = con.prepareStatement("select _id from notes where title regexp ? order by created, title");
		ps.setString(1, getPattern());
		int numberOfNotes = registerNotes(ps);
		return numberOfNotes > 0;
	}
	
	PreparedStatement preparePicking(String pattern, long time)	throws Exception {
		PreparedStatement ps = con.prepareStatement("SELECT _id FROM notes where title regexp ? and created = ?");
		ps.setString(1, pattern);
		ps.setLong(2, time);
		return ps;
	}

	int update(Long id, String note) throws Exception {
		PreparedStatement ps = con.prepareStatement("UPDATE notes SET note = ?, modified = ? where _id = ?");
		ps.setString(1, note);
		ps.setLong(2, new Date().getTime());
		ps.setLong(3, id);
		return ps.executeUpdate();
	}

	int insert(Long id, String title, String note, long time) throws Exception {
		PreparedStatement ps = con.prepareStatement("INSERT INTO notes (_id,title,note,created,modified) VALUES (?,?,?,?,?)");
		ps.setLong(1, id);
		ps.setString(2, title);
		ps.setString(3, note);
		ps.setLong(4, time);
		ps.setLong(5, time);
		return ps.executeUpdate();
	}

	private void updateOrInsert(String pattern, String dateString, boolean retrieve) {
		try {
			long time = Util.parseDate(dateString, DatePicker.dateFormat).getTime();
			PreparedStatement ps = preparePicking(pattern, time);
			ResultSet rs = ps.executeQuery();
			long id;
			if (rs.next()) {
				id = rs.getLong(1);
				update(id, getNote());
			}
			else {
				rs.close();
				rs = con.createStatement().executeQuery("SELECT max(_id) FROM notes");
				id = rs.next() ? rs.getLong(1) : -1;
				insert(++id, getTitle(), getNote(), time);
			}
			rs.close();
			
			if (retrieve && retrieveNotes()) {
				index = Arrays.asList(ids).indexOf(id) - 1;
				setNote(next());
			}
		} catch (Exception e) {
			handle(e);
		}
	}

	private void delete(String pattern, String dateString) throws Exception {
		long time = Util.parseDate(dateString, DatePicker.dateFormat).getTime();
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
	
	ResultSet query(Long id) {
		try {
			PreparedStatement ps = con.prepareStatement("SELECT note,title,created FROM notes where _id=?");
			ps.setLong(1, id);
			return ps.executeQuery();
		} catch (Exception e) {
			handle(e);
			return null;
		}
	}

	public String next() {
		ResultSet rs = null;
		
		index++;
		if (index < ids.length)
			rs = query(ids[index]);
		else 
			buttons[2].setEnabled(false);
		buttons[1].setEnabled(ids.length > 0);
		
		return updateComponent(rs);
	}

	public String previous() {
		ResultSet rs = null;
		
		index--;
		if (index > -1)
			rs = query(ids[index]);
		else 
			buttons[1].setEnabled(false);
		buttons[2].setEnabled(ids.length > 0);
		
		return updateComponent(rs);
	}

	private String updateComponent(ResultSet rs) {
		try {
			actionBlocked = true;
			
			titleCombo.setSelectedIndex(-1);
			date.setText("");
			if (rs == null)
				return "";
			
			setTitle(rs.getString(2));
			date.setText(Util.formatDate(rs.getLong(3), DatePicker.dateFormat));
			
			String note = rs.getString(1);
			rs.close();
			return note;
		} catch (Exception e) {
			handle(e);
			return updateComponent(null);
		}
		finally {
			actionBlocked = false;
			setDirty(false);
			buttons[6].setEnabled(rs != null);
		}
	}

	private void clear() {
		updateComponent(null);
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
	
	public void setTitle(String title) {
		Util.comboEdit(titleCombo).setText(title);
		for (int i = 0; i < patterns.length; i++) 
			if (title.matches(patterns[i]))
				titleCombo.setSelectedIndex(i);
	}

	private String getTitle() {
		return Util.comboEdit(titleCombo).getText();
	}
	
	public String getPattern() {
		String title = getTitle();
		for (int i = 0; i < patterns.length; i++) 
			if (title.matches(patterns[i]))
				return patterns[i];
		return title.length() > 0 ? title : ".*";
	}

	private void setNote(String text) {
		if (hasTextArea()) 
			this.textArea.setText(text);
	}

	private String getNote() {
		return this.textArea.getText();
	}
	
	private boolean actionBlocked = false;

	@Override
	public void actionPerformed(ActionEvent ae) {
		if (con != null && !actionBlocked) {
			String pattern = getPattern();
			String dateString = date.getText();
			try {
				PreparedStatement ps;
				
				if (DatePicker.isCalendarDate(dateString )) {
					long time = Util.parseDate(dateString, DatePicker.dateFormat).getTime();
					ps = preparePicking(pattern, time);
				}
				else if (DatePicker.isWeekDate(dateString)) {
	        		Date date = Util.parseDate(dateString, DatePicker.weekFormat);
	        		long[] interval = Util.weekInterval(date);
	        		
	    			ps = con.prepareStatement("SELECT _id FROM notes where created between ? and ? and title regexp ?");
	    			ps.setLong(1, interval[0]);
	    			ps.setLong(2, interval[1]);
	    			ps.setString(3, pattern);
				}
				else {
					date.setText("");
	        		
	    			ps = con.prepareStatement("SELECT _id FROM notes where title regexp ?");
	    			ps.setString(1, pattern);
				}
				
				registerNotes(ps);
				setNote(next());
			} catch (Exception e) {
				handle(e);
			}
		}
	}

}

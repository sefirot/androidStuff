package com.applang.components;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
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

import javax.swing.Box;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.gjt.sp.jedit.View;

import android.content.ContentUris;
import android.content.ContentValues;
import android.net.Uri;

import com.applang.Util.Job;
import com.applang.berichtsheft.BerichtsheftApp;
import com.applang.berichtsheft.plugin.BerichtsheftPlugin;
import com.applang.components.DataView.DataModel;
import com.applang.provider.NotePad;
import com.applang.provider.NotePadProvider;
import com.applang.provider.NotePad.NoteColumns;

import static com.applang.SwingUtil.*;
import static com.applang.Util.*;
import static com.applang.Util1.*;
import static com.applang.Util2.*;
import static com.applang.PluginUtils.*;

public class NotePicker extends ActionPanel
{
	public static void main(String[] args) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				BerichtsheftApp.loadSettings();
				setupSpellChecker(BerichtsheftApp.applicationDataPath());
				final DataView dataView = new DataView();
				final TextToggle textToggle = new TextToggle()
						.createBufferedTextArea("velocity", "/modes/velocity_pure.xml");
				textToggle.getTextEdit().installSpellChecker();
				String title = "Berichtsheft database";
				NotePicker notePicker = new NotePicker(dataView, textToggle, 
						null,
						title, 1);
				createAndShowGUI(title, 
						new Dimension(800, 400), 
						Behavior.EXIT_ON_CLOSE,
						notePicker, 
						new Function<Component>() {
							public Component apply(Object...params) {
								JSplitPane splitPane = splitPane(JSplitPane.VERTICAL_SPLIT,
									new PropertyChangeListener() {
										public void propertyChange(PropertyChangeEvent evt) {
										}
									});
								splitPane.setResizeWeight(0.5);
								splitPane.setOneTouchExpandable(true);
								Component target = textToggle.getUIComponent();
								splitPane.setTopComponent(target);
								Component c = findFirstComponent(dataView, "south");
								if (c != null)
									dataView.remove(c);
								splitPane.setBottomComponent(dataView);
								return splitPane;
							}
						});
			}
		});
	}
	
	@Override
	public void start(Object... params) {
		if (!usingJdbc())
			initialize(dataView.getUriString());
		else {
			dbFilePath = getSetting("database", "");
			if (fileExists(new File(dbFilePath)))
				initialize(dbFilePath);
		}
	}
	
	@Override
	public void finish(Object... params) {
		if (!usingJdbc())
			dataView.nosync();
		getTextToggle().getTextEdit().uninstallSpellChecker();
		try {
			if (getCon() != null)
				getCon().close();
		} catch (SQLException e) {
			handleException(e);
		}
		if (usingJdbc())
			putSetting("database", dbFilePath);
		super.finish(params);
	}
	
	private DataView dataView;

	public void setDataView(DataView dataView) {
		this.dataView = dataView;
		initialize(dataView.getUriString());
	}

	public boolean usingJdbc() {
		return dataView == null;
	}
	
	private JTextField date = null;
	
	public NotePicker(DataView dataView, TextToggle textArea, Object... params) {
		super(textArea, params);
		textArea.setOnTextChanged(new Job<JComponent>() {
			public void perform(JComponent t, Object[] params) throws Exception {
				setDirty(true);
			}
		});
		if (!usingJdbc() && dataView.getUri() == null)
			dataView.resetDataConfiguration();
		this.dataView = dataView;
	}

	public void installNotePicking(Container container) {
		container.removeAll();
		addButton(container, ActionType.DATABASE.index(), new NoteAction(ActionType.DATABASE));
		date = new JTextField(20);
		date.setHorizontalAlignment(JTextField.CENTER);
		addSelectAllObserver(date);
		container.add(date);
		date.addKeyListener(new KeyAdapter() {
			@Override
			public void keyTyped(KeyEvent e) {
				super.keyTyped(e);
			}
			@Override
			public void keyPressed(KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_ENTER)
					clickAction(8);
				else
					super.keyPressed(e);
			}
		});
		addButton(container, ActionType.CALENDAR.index(), new NoteAction(ActionType.CALENDAR));
		installBrowsing(container);
		installAddRemove(container, "note");
		installUpdate(container);
		clear();
	}

	@SuppressWarnings("rawtypes")
	public void installBrowsing(Container container) {
		comboBoxes = new JComboBox[] {new JComboBox()};
		comboBoxes[0].setEditable(true);
		container.add(comboBoxes[0]);
		JTextField textField = comboEdit(0);
		addSelectAllObserver(textField);
		textField.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_ENTER)
					clickAction(ActionType.PICK.index());
			}
		});
		addButton(container, ActionType.PICK.index(), new NoteAction(ActionType.PICK));
		addButton(container, ActionType.FIRST.index(), new NoteAction(ActionType.FIRST));
		addButton(container, ActionType.PREVIOUS.index(), new NoteAction(ActionType.PREVIOUS));
		addButton(container, ActionType.NEXT.index(), new NoteAction(ActionType.NEXT));
		addButton(container, ActionType.LAST.index(), new NoteAction(ActionType.LAST));
	}

	public void installBausteinEditing(Container container) {
		container.removeAll();
		date = null;
		addButton(container, ActionType.DATABASE.index(), new NoteAction(ActionType.DATABASE));
		addButton(this, ActionType.STRUCT.index(), new NoteAction(ActionType.STRUCT));
		container.add( Box.createHorizontalStrut(3) );
		addToggle(this, ActionType.TOGGLE2.index(), new NoteAction(ActionType.TOGGLE2));
		container.add( Box.createHorizontalStrut(3) );
		installBrowsing(container);
		installAddRemove(container, "baustein");
		installUpdate(container);
		clear();
	}
	
	class NoteAction extends CustomAction
    {
		public NoteAction(ActionType type) {
			super(type);
			if (type.equals(ActionType.TOGGLE2)) {
				putValue(NAME, type.name(1));
			}
        }
        
        public NoteAction(String text) {
			super(text);
		}

		@Override
        protected void action_Performed(ActionEvent ae) {
        	String dateString = getDate();
        	ActionType type = (ActionType)getType();
			switch (type) {
			case DATABASE:
				updateOnRequest();
				dbFilePath = chooseDatabase(dbFilePath);
				break;
			case CALENDAR:
				dateString = pickDate(dateString);
				date.requestFocusInWindow();
				break;
			case FIRST:
			case PREVIOUS:
			case NEXT:
			case LAST:
				browse(type);
				break;
			case PICK:
				if (usingJdbc()) {
					searchPattern = getPattern();
					finder.keyLine(searchPattern);
					pickNote(dateString, searchPattern);
				}
				else if (pkColumn > -1) {
					finder.keyLine();
					Long time = toTime(dateString, DatePicker.calendarFormat);
					if (time != null) {
						pkRow = finder.pointer(time, getTitle());
						browse(type);
					}
				}
				break;
			case DATE:
				date.requestFocusInWindow();
				break;
			case TITLE:
				comboBoxes[0].requestFocusInWindow();
				break;
			case INSERT:
				break;
			case DELETE:
				deleteSelection();
				break;
			case TOGGLE2:
				toggle(this, getTextToggle().getTextEdit().toggler);
				break;
			case STRUCT:
				break;
			default:
				return;
			}
        }
	}
	
	private void clear() {
		if (usingJdbc())
			refreshWith(null);
		else {
			setDate("");
			setTitle("");
			setText("");
		}
		enableBrowseActions();
	}

	public void enableBrowseActions() {
		enableAction(ActionType.FIRST.index(), pkRow > 0);
		enableAction(ActionType.PREVIOUS.index(), pkRow > 0);
		enableAction(ActionType.NEXT.index(), pkRow < lastRow() && pkRow > -1);
		enableAction(ActionType.LAST.index(), pkRow < lastRow() && pkRow > -1);
	}
	
	private DataModel dataModel() {
		if (dataView == null)
			return null;
		return (DataModel) dataView.getTable().getModel();
	}
	
	private int convertRowIndexToModel(int rowIndex) {
		return dataView.getTable().convertRowIndexToModel(rowIndex);
	}

	private DataAdapter dataAdapter = null;
	private String uriString = NoteColumns.CONTENT_URI.toString();
	private Object pk = null;
	public int pkColumn = -1, pkRow = -1;
	
	private boolean createProvider() {
		String dbPath = dataView.getDataConfiguration().getPath();
		dataAdapter = new DataAdapter(NotePad.AUTHORITY, new File(dbPath), uriString);
		ValMap info = dataAdapter.info;
		if (info.size() > 0) {
			pk = info.get("PRIMARY_KEY");
			pkColumn = info.getList("name").indexOf(pk);
			if (pkColumn < 0) {
				BerichtsheftPlugin.consoleMessage("dataview.no-primary-key.message");
			}
		}
		else {
			BerichtsheftPlugin.consoleMessage("dataview.no-info-for-table.message", dataAdapter.getTableName());
			pkColumn = -1;
		}
		return pkColumn > -1;
	}

	public Object pkValue() {
		if (pkRow < 0)
			return null;
		int rowIndex = convertRowIndexToModel(pkRow);
		Object[] row = dataModel().getValues(false, rowIndex);
		return row[pkColumn];
	}

	public int lastRow() {
		DataModel model = dataModel();
		if (model == null)
			return -1;
		else
			return model.getRowCount() - 1;
	}
	
	public void setRow(Object pkValue) {
		DataModel model = dataModel();
		for (int i = 0; i < model.getRowCount(); i++) {
			Object[] row = dataModel().getValues(false, i);
			if (row[pkColumn].equals(pkValue)) {
				pkRow = i;
				break;
			}
		}
	}
	
	private void initialize(String dbString) {
		if (usingJdbc()) {
			if (openConnection(dbString)) {
				retrieveTitles();
				if (finder != null) {
					searchPattern = allTitles;
					finder.keyLine(searchPattern);
					setDate(formatDate(
							0,
							finder.keys.length > 0 ? finder
									.epochFromKey(finder.keys[0]) : now()));
					pickNote(getDate(), searchPattern);
				}
			}
		}
		else {
			String tableName = dbTableName(dbString);
			switch (NotePadProvider.tableIndex(tableName)) {
			case 1:
				installBausteinEditing(this);
				getTextToggle().toggle(false, null);
				break;
			default:
				installNotePicking(this);
				getTextToggle().toggle(true, null);
				break;
			}
			uriString = NotePadProvider.contentUri(tableName).toString();
			refresh();
		}
		JComponent jc = (JComponent) getParent();
		if (jc != null)
			setWindowTitle(this, String.format(
				"Berichtsheft database : %s", 
				trimPath(dbString, jc.getWidth() / 2, jc.getFont(), jc)));
	}

	public void refresh() {
		if (usingJdbc()) {
			finder.keyLine(searchPattern);
			pickNote(getDate(), finder.pattern);
			retrieveTitles();
		}
		else if (!noRefresh) {
			clear();
			if (createProvider()) {
				dataView.populate(dataAdapter, null, null, DEFAULT_SORT_ORDER);
				retrieveTitles();
				browse(ActionType.PICK);
			}
		}
	}

    private static final String DEFAULT_SORT_ORDER = "created,title";
    
    @Override
	protected String chooseDatabase(String dbName) {
		if (usingJdbc()) {
			File dbFile = DataView.chooseDb(null, false, dbName, true);
			if (dbFile != null) {
				dbName = dbFile.getPath();
				initialize(dbName);
			}
		}
		else {
			View view = getView() instanceof View ? (View) getView() : null;
			if (dataView.configureData(view, false)) {
				dbName = dataView.getUriString();
				initialize(dbName);
			}
		}
		return dbName;
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

	@Override
	protected void updateOnRequest() {
		Object item = getItem();
		if (isItemValid(item)) {
			save(item, false);
		}
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
	
	private boolean bausteinEditing() {
		return date == null;
	}
	
	public String getDate() {
		if (bausteinEditing())
			return "";
		String text = date.getText();
		return text;
	}
	
	public void setDate(final String text) {
		try {
			CustomAction.blocked(new Job<Void>() {
				public void perform(Void v, Object[] params) throws Exception {
					if (!bausteinEditing())
						date.setText(text);
				}
			}, null);
		} catch (Exception e) {}
	}
	
	@SuppressWarnings("unchecked")
	private void retrieveTitles() {
		try {
			String title = getTitle();
			comboBoxes[0].removeAllItems();
			if (usingJdbc()) {
				PreparedStatement ps = getCon().prepareStatement(
						"select distinct title from notes order by title");
				ResultSet rs = ps.executeQuery();
				while (rs.next())
					comboBoxes[0].addItem(rs.getString(1));
				comboBoxes[0].addItem(itemAll);
				comboBoxes[0].addItem(itemBAndB);
			}
			else {
				Object[][] res = dataAdapter.query(uriString, 
						(String[])null, 
						String.format("select distinct title from %s order by title", dataAdapter.getTableName()));
				for (Object[] row : res) {
					comboBoxes[0].addItem(row[0]);
				}
			}
			setTitle(title);
		} catch (Exception e) {}
	}
	
	public void setTitle(final String t) {
		try {
			CustomAction.blocked(new Job<Void>() {
				public void perform(Void v, Object[] params) throws Exception {
					comboEdit(0).setText(t);
				}
			}, null);
		} catch (Exception e) {}
	}

	public String getTitle() {
		return comboEdit(0).getText();
	}
	
	public static final String allDates = "";
	public static final String allTitles = SOMETHING_OR_NOTHING_REGEX;
	public static final String itemAll = "-- all --";
	public static final String bAndB = "(?i)(bemerk\\w*|bericht\\w*)";
	public static final String itemBAndB = "Berichte & Bemerkungen";

	public void setPattern(String p) {
		finder.pattern = p;
		
		for (Object value : finder.specialPatterns.getValues())
			if (p.equals(value)) 
				p = stringValueOf(finder.specialPatterns.getKey(value));
		
		setTitle(p);
	}

	public String getPattern() {
		String c = getTitle();
		
		for (Object key : finder.specialPatterns.getKeys())
			if (c.equals(key)) 
				c = stringValueOf(finder.specialPatterns.getValue(key));
		
		return c;
	}
	
	public Long[] timeLine(Object... params) {
		ArrayList<Long> timelist = alist();
		try {
			if (usingJdbc()) {
				String pattern = param(allTitles, 0, params);
				PreparedStatement ps = getCon()
						.prepareStatement(
								"select distinct created FROM notes where title regexp ? order by created");
				ps.setString(1, pattern);
				ResultSet rs = ps.executeQuery();
				while (rs.next()) {
					timelist.add(rs.getLong(1));
				}
				rs.close();
			}
			else {
				Object[][] res = dataAdapter.query(uriString, 
						(String[])null, 
						String.format("select distinct created FROM %s order by created", dataAdapter.getTableName()));
				for (Object[] row : res) {
					timelist.add((Long) row[0]);
				}
			}
		} catch (Exception e) {}
		return timelist.toArray(new Long[0]);
	}
	
	String searchPattern = allTitles;
	
	public class NoteFinder
    {
		public BidiMultiMap specialPatterns = new BidiMultiMap();
		
		public NoteFinder() {
			specialPatterns.add(itemAll, allTitles);
			specialPatterns.add(itemBAndB, bAndB);
		}
		
		String pattern = "";
		long[] epoch = new long[0];
		String[] keys = null;
		
		public String[] keyLine(String...pattern) {
			ValList keylist = vlist();
			try {
				if (usingJdbc()) {
					PreparedStatement ps = getCon()
							.prepareStatement(
									"select created,title FROM notes where title regexp ? order by " + DEFAULT_SORT_ORDER);
					ps.setString(1, pattern[0]);
					ResultSet rs = ps.executeQuery();
					while (rs.next()) {
						keylist.add(keyValue(rs.getLong(1), rs.getString(2)));
					}
					rs.close();
				}
				else if (isAvailable(0, pattern)){
					Object[][] result = dataAdapter.query(uriString, 
							strings("created", "title"), 
							"title like ?", 
							strings(pattern[0]),
							DEFAULT_SORT_ORDER);
					for (Object[] res : result) {
						keylist.add(keyValue((Long)res[0], (String)res[1]));
					}
				}
				else {
					DataModel model = dataModel();
					int[] index = {
							model.columns.indexOf("created"), 
							model.columns.indexOf("title")
					};
					for (int i = 0; i < model.getRowCount(); i++) {
						Object[] values = model.getValues(false, i);
						keylist.add(keyValue((Long)values[index[0]], (String)values[index[1]]));
					}
				}
			} catch (Exception e) {}
			return this.keys = toStrings(keylist);
		}
		
		public String keyValue(long time, String title) {
			return time + "_" + title;
		}
		
		public long epochFromKey(String key) {
			return Long.parseLong(key.substring(0, key.indexOf('_')));
		}
		
		public String titleFromKey(String key) {
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
	
				String c1 = titleFromKey(o1);
				String c2 = titleFromKey(o2);
				
				return c1.compareTo(c2);
			}
		};
		
		private KeyComparator comparator = new KeyComparator(false);
		private KeyComparator partialComparator = new KeyComparator(true);
		
		private int criterion(long epoch, String pattern) {
			String value = keyValue(epoch, pattern);
			if (specialPatterns.getValues().contains(pattern)) {
				ArrayList<Integer> a = alist();
				ArrayList<Integer> b = alist();
				ArrayList<Integer> c = alist();
				int comp = 0;
				for (int i = 0; i < this.keys.length; i++) {
					String key = this.keys[i];
					if ((comp = partialComparator.compare(key, value)) == 0 && titleFromKey(key).matches(pattern))
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
		
		public int pointer(long time, String title) {
			return pointer(criterion(time, title));
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
	
		public String find(ActionType direct, long... epoch) {
			boolean available = bunchAvailable(epoch);
			
			int index = pointer(this.index[0]);
			if (direct == ActionType.PICK) 
				if (!available) {
					message(String.format("'%s' on %s not available", pattern, formatDate(epoch.length, epoch[0])));
					if (index >= keys.length) {
						direct = ActionType.PREVIOUS;
						available = true;
					}
					else
						direct = ActionType.NEXT;
				}
			
			boolean next = direct == ActionType.NEXT;
			if (next && epoch.length > 1) 
				index = pointer(this.index[1]);
			else {
				if (available && direct != ActionType.PICK)
					index += next ? 1 : -1;
			}
			if (index < 0 || index >= keys.length)
				return null;
			
			String key = keys[index];
			
			long t = epochFromKey(key);
			if (epoch.length > 1) {
				if (epoch.length == 2)
					this.epoch = DatePicker.weekInterval(formatDate(2, t), 1);
				else
					this.epoch = DatePicker.monthInterval(formatDate(3, t), 1);
			}
			else
				this.epoch = new long[] {t};
			this.pattern = titleFromKey(key);
			
			return key;
		}
    }
	
	public NoteFinder finder = new NoteFinder();
	
	public void pickNote(String dateString, String pattern) {
		clear();
		setDate(dateString);
		try {
			finder.pattern = pattern;
			finder.epoch = getTime(dateString);
			if (nullOrEmpty(finder.epoch)) {
				message(String.format("value for '%s' doesn't make sense", ActionType.DATE.description()));
				date.requestFocus();
			}
			browse(ActionType.PICK);			
		} catch (Exception e) {
			handleException(e);
			setText("");
		}
	}
	
	private boolean browseFree = true;
	
	public void browse(ActionType direct) {
		if (!browseFree)
			return;
		try {
			browseFree = false;
			updateOnRequest();
			String text = "";
			if (usingJdbc()) {
				ResultSet rs = null;
				if (finder.find(direct, finder.epoch) != null)
					rs = query(finder.epoch);
				enableAction(ActionType.PREVIOUS.index(),
						finder.previousBunchAvailable(finder.epoch));
				enableAction(ActionType.NEXT.index(),
						finder.nextBunchAvailable(finder.epoch));
				text = refreshWith(rs);
			}
			else if (pkColumn > -1) {
				switch (direct) {
				case FIRST:
					pkRow = 0;
					break;
				case PREVIOUS:
					pkRow--;
					break;
				case NEXT:
					pkRow++;
					break;
				case LAST:
					pkRow = lastRow();
					break;
				default:
					pkRow = Math.min(Math.max(0, pkRow), lastRow());
					break;
				}
				enableBrowseActions();
				if (direct != ActionType.DELETE) {
					int[] rows = dataView.getTable().getSelectedRows();
					if (rows.length > 0 && direct == ActionType.PICK)
						rows = ints(rows[0], rows[rows.length - 1]);
					else
						rows = ints(pkRow);
					dataView.synchronizeSelection(rows, tablePopup, tableSelectionListener);
				}
				Object[] rec = (Object[]) select();
				if (isAvailable(2, rec)) {
					setDate(bausteinEditing() ? 
							null : 
							formatDate(1, (Long) rec[0]));
					setTitle(stringValueOf(rec[1]));
					text = stringValueOf(rec[2]);
				}
				else
					clear();
			}
			setText(text);
		} 
		finally {
			browseFree = true;
		}
	}

	private JPopupMenu tablePopup = newPopupMenu(
			objects(ActionType.DELETE.description(), new NoteAction(ActionType.DELETE))
	);
	
	private ListSelectionListener tableSelectionListener = new ListSelectionListener() {
		@Override
		public void valueChanged(ListSelectionEvent e) {
			if (e.getValueIsAdjusting())
				return;
			int sel = dataView.getTable().getSelectedRow();
			if (sel > -1) {
				pkRow = sel;
				browse(ActionType.PICK);
			}
		}
	};

	private void deleteSelection() {
		updateOnRequest();
		int[] rows = dataView.getTable().getSelectedRows();
		if (isAvailable(0, rows)) {
			boolean refresh = false;
			try {
				begin_delete();
				for (int i = rows.length - 1; i > -1; i--) {
					pkRow = rows[i];
					browse(ActionType.DELETE);
					Boolean done = do_delete(getLongItem());
					if (done != null)
						refresh |= done;
				}
			} 
			catch (Throwable e) {} 
			finally {
				end_delete();
			}
			if (refresh)
				refresh();
		}
	}
	
	@Override
	public Object select(Object...args) {
		args = reduceDepth(args);
		boolean pkValue = notAvailable(0, args);
		String dateString = param(null, 0, args);
		String pattern = param(null, 1, args);
		if (pkValue || notNullOrEmpty(dateString) || bausteinEditing())
			try {
				Long time = toTime(dateString, DatePicker.calendarFormat);
				long[] interval = dayInterval(time, 1);
				if (usingJdbc()) {
					PreparedStatement ps = preparePicking(true, pattern, interval);
					if (registerNotes(ps.executeQuery()) > 0) {
						return records[0];
					}
				}
				else {
					Object[][] result = dataAdapter.query(uriString, 
							strings("created", "title", "note", "_id"), 
							whereClause(pkValue), 
							pkValue ? 
									strings("" + pkValue()) : 
									(bausteinEditing() ? 
											strings(pattern) :
											strings("" + interval[0], "" + (interval[1] - 1), pattern)));
					if (isAvailable(0, result)) {
						return result[0];
					}
				}
			} catch (Exception e) {
				handleException(e);
			}
		return null;
	}
	
	private String whereClause(boolean pkValue) {
		if (pkValue)
			return pk + "=?";
		else
			return bausteinEditing() ? 
					"created is null and title = ?" :
					"created between ? and ? and title like ?";
	}
	
	public static String shortFormat = "%s %s";
	
	@Override
	protected String toString(Object item) {
		Object[] rec = (Object[]) item;
		return String.format(shortFormat, rec[1], rec[0]);
	}
	
	@Override
	protected String toLongString(Object item) {
		Object[] rec = (Object[]) item;
		return String.format(noteFormat2, rec[1], rec[0], rec[2]);
	}

	@Override
	public boolean isItemValid(Object item) {
		Object[] rec = (Object[]) item;
		Date date = parseDate(stringValueOf(rec[0]));
		return date != null && notNullOrEmpty(rec[1]);
	}

	@Override
	protected Object getItem() {
		return objects(getDate(), getTitle());
	}

	@Override
	protected Object getLongItem() {
		return objects(getDate(), getTitle(), getText());
	}

	@Override
	protected void updateItem(boolean update, Object...args) {
		Object[] rec = reduceDepth(args);
		String text = getText();
		if (isAvailable(0, rec))
			updateOrAdd(false, text, rec);
		else if (update) 
			updateOrAdd(false, text, (Object[]) getItem());
		else {
			rec = (Object[]) select(getItem());
			if (isAvailable(2, rec))
				setText(rec[2].toString());
		}
	}
	
	private long updateOrAdd(boolean add, String text, Object...rec) {
		if (usingJdbc()) 
			return updateOrInsert(rec[0].toString(), rec[1].toString(), text, add);
		else {
			long time = toTime(rec[0].toString(), DatePicker.calendarFormat);
	       	ValMap info = dataAdapter.info;
	       	BidiMultiMap map = new BidiMultiMap();
	       	map.putValue("note", text);
	       	map.putValue("modified", now());
			ContentValues values = contentValues(info, map.getKeys(), map.getValues().toArray());
			if (add) {
				values.putNull(pk.toString());
				values.put("created", time);
				values.put("title", rec[1].toString());
				Uri uri = dataAdapter.insert(uriString, values);
				return ContentUris.parseId(uri);
			}
			else {
				rec = (Object[]) select(rec);
				return dataAdapter.update(appendId(uriString, (Long) rec[3]), values);
			}
		}
	}

	@Override
	protected boolean addItem(boolean refresh, Object item) {
		Object[] rec = (Object[]) item;
		String text = getText();
		long id = updateOrAdd(true, text, rec);
		if (refresh)
			refresh();
		return id > -1;
	}

	@Override
	protected boolean removeItem(Object item) {
		boolean done = false;
		Object[] rec = (Object[]) select(item);
		if (rec != null) {
			if (usingJdbc())
				done = remove(false, rec[1].toString(), rec[0].toString());
			else 
				done = 0 < dataAdapter.delete(appendId(uriString, (Long) rec[3]));
		}
		if (done) {
			clear();
			refresh();
		}
		return done;
	}
	
	@Override
	public void setText(String text) {
		TextToggle textBums = getTextToggle();
		updateText(textBums, text);
		textBums.getTextEdit().undoManager.discardAllEdits();
	}

	public static String formatDate(int kind, long time) {
		switch (kind) {
		case 2:
			return DatePicker.weekDate(weekInterval(new Date(time), 1));
		case 3:
			return DatePicker.monthDate(monthInterval(new Date(time), 1));
		default:
			return com.applang.Util.formatDate(time, DatePicker.calendarFormat);
		}
	}

	public Date parseDate(String dateString) {
		try {
			return new Date(toTime(dateString, DatePicker.calendarFormat));
		} catch (Exception e) {
			return null;
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

	private static final String orderClause = " order by created, title";
	
	public PreparedStatement preparePicking(boolean record, String pattern, long... time) throws Exception {
		String sql = "select " +
				(record ? 
					"created,title,note,_id" : 
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

	public int insert(long id, String note, String title, long time) throws Exception {
		PreparedStatement ps = getCon().prepareStatement("INSERT INTO notes (_id,title,note,created,modified) VALUES (?,?,?,?,?)");
		ps.setLong(1, id);
		ps.setString(2, title);
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

	public long updateOrInsert(String dateString, String pattern, String note, boolean insert) {
		long id = -1;
		try {
			long time = toTime(dateString, DatePicker.calendarFormat);
			PreparedStatement ps = preparePicking(false, pattern, dayInterval(time, 1));
			ResultSet rs = ps.executeQuery();
			if (rs.next()) {
				id = rs.getLong(1);
				update(id, note);
			} else if (insert) {
				id = newId();
				insert(id, note, getTitle(), time);
			}
			rs.close();
		} catch (Exception e) {
			handleException(e);
			return -1;
		}
		return id;
	}

	@SuppressWarnings("unused")
	private void updateOrInsert(String pattern, String dateString) {
		if (finder.epoch.length == 2) {
			for (String[] record : listRecords(getText())) 
				updateOrInsert(record[0], record[1], record[2], true);
		}
		else 
			updateOrInsert(dateString, pattern, getText(), true);
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
	
	public Long[] ids = null;
	public Object[][] records = null;
	
	public int registerNotes(ResultSet rs) throws SQLException {
		ArrayList<Long> idlist = alist();
		ArrayList<Object[]> reclist = alist();
		
		ResultSetMetaData rsmd = rs.getMetaData();
		int cols = rsmd.getColumnCount();
		
		while (rs.next()) {
			if (cols > 1) {
				ValList list = vlist();
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
					setTitle(rs.getString(2));
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
//			enableAction(ActionType.DELETE.index(), hasTextArea() && note != null);
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
	
	public static String wrapFormat = enclose(FOLD_MARKER[0], " %s ", FOLD_MARKER[1]);
	public static Pattern wrapPattern = 
			Pattern.compile("(?s)" + enclose(FOLD_MARKER_REGEX[0], " (.*?) \\}\\}\\}", FOLD_MARKER_REGEX[1]));
	
	public static String noteFormat1 = enclose(FOLD_MARKER[0], " %s\n%s\n", FOLD_MARKER[1]);
	public static String noteFormat2 = enclose(FOLD_MARKER[0], " %s '%s'\n%s\n", FOLD_MARKER[1]);
	public static Pattern notePattern1 = 
			Pattern.compile("(?s)" + enclose(FOLD_MARKER_REGEX[0], " ([^\\}]*?)\\n([^\\}]*?)\\n", FOLD_MARKER_REGEX[1]));
	public static Pattern notePattern2 = 
			Pattern.compile("(?s)" + enclose(FOLD_MARKER_REGEX[0], " ([^\\}]*?) '([^\\}]*?)'\\n(.*?)\\n", FOLD_MARKER_REGEX[1]));
	
	public boolean isWrapped(String text) {
		return text.startsWith(FOLD_MARKER[0]) && text.endsWith(FOLD_MARKER[1]);
	}

	public String[][] listRecords(String text) {
		ArrayList<String[]> list = alist();
		
		for (MatchResult m : findAllIn(text, notePattern2)) 
			list.add(strings(m.group(1), m.group(2), m.group(3)));
		
		return list.toArray(new String[0][]);
	}

}

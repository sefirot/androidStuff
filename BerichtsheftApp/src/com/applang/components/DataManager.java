package com.applang.components;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.FileWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Date;
import java.util.Properties;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTable;
import javax.swing.JToolBar;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.gjt.sp.jedit.View;

import com.applang.SwingUtil.Behavior;
import com.applang.SwingUtil.UIFunction;
import com.applang.Util.BidiMultiMap;
import com.applang.Util.Job;
import com.applang.Util.ValMap;
import com.applang.berichtsheft.BerichtsheftApp;
import com.applang.berichtsheft.plugin.BerichtsheftPlugin;
import com.applang.components.DataView.DataModel;
import com.applang.provider.NotePad;
import com.applang.provider.NotePadProvider;
import com.applang.provider.NotePad.NoteColumns;

import android.content.ContentUris;
import android.content.ContentValues;
import android.net.Uri;
import android.util.Log;

import static com.applang.Util.*;
import static com.applang.Util1.*;
import static com.applang.Util2.*;
import static com.applang.SwingUtil.*;

public class DataManager extends ActionPanel
{
	public static final int TABLE = 1;
	public static final int FORM = 2;
	public static final int TEXT = 1;
	public static final int SCRIPT = 2;
	
	public static String propsToFile(Properties props, String name) {
		String path = "/tmp/" + name;
		try {
			Writer writer = write(new StringWriter(), ":");
			writer = write_assoc(writer, BerichtsheftPlugin.MAGIC, "data-manage");
			writer = write(writer, ":");
			writer = write_assoc(writer, "wrap", "none");
			writer = write(writer, ":");
			FileWriter fileWriter = new FileWriter(path);
			props.store(fileWriter, writer.toString());
			fileWriter.close();
		}
		catch (Exception e) {
			Log.e(TAG, "installDataManager", e);
		}
		return path;
	}
	
	private Properties props;
	private String name;
	
	public String getName() {
		return name;
	}

	public DataManager(View view, Properties props, String name) {
		super(createTextComponent(), view);
		this.props = props;
		this.name = name;
		container = new JPanel(new BorderLayout());
		JToolBar bar = northToolBar(container, BorderLayout.SOUTH, this.name);
		bar.setName(DoubleFeature.FOCUS);
		joinContainer(bar);
		try {
			layoutToggler.perform(_layout() == TABLE, objects());
		} 
		catch(Exception e) {
			Log.e(TAG, "DataManager", e);
		};
		dataView.setUriString(this.props.getProperty("uri", ""));
		initialize(dataView.getUriString());
	}
	
	private DataView dataView = new DataView() {
		{
			Component component = findComponent(this, BorderLayout.SOUTH);
			if (component != null)
				remove(component);
			getTable().setName(DoubleFeature.FOCUS);
		}
	};
	
	private static ITextComponent createTextComponent() {
		TextEditor2 textEditor2 = new TextEditor2().createBufferedTextArea(null, null);
		textEditor2.getTextEditor().installSpellChecker();
		return textEditor2;
	}
	
	private void reinstallTools(Container bar) {
		bar.removeAll();
		addButton(bar, ActionType.DATABASE.index(), new ManagerAction(ActionType.DATABASE));
		addButton(bar, ActionType.TOGGLE1.index(), new ManagerAction(ActionType.TOGGLE1));
	}
	
	private void installForm(Container bar) {
		reinstallTools(bar);
		addButton(bar, ActionType.FIRST.index(), new ManagerAction(ActionType.FIRST));
		addButton(bar, ActionType.PREVIOUS.index(), new ManagerAction(ActionType.PREVIOUS));
		addButton(bar, ActionType.NEXT.index(), new ManagerAction(ActionType.NEXT));
		addButton(bar, ActionType.LAST.index(), new ManagerAction(ActionType.LAST));
		addButton(bar, ActionType.TOGGLE2.index(), new ManagerAction(ActionType.TOGGLE2));
	}
	
	private Container container = null;
	
	@Override
	public Component getUIComponent() {
		return container;
	}
	
	private int _layout() {
		String layout = props.getProperty("layout");
		if (nullOrEmpty(layout))
			return TABLE;
		else
			return FORM;
	}

	private Job<Boolean> layoutToggler = new Job<Boolean>() {
		public void perform(Boolean tableLayout, Object[] parms) throws Exception {
			props.setProperty("layout", tableLayout ? "" : "form");
			Component center;
			if (tableLayout) {
				reinstallTools(DataManager.this);
				center = dataView;
			}
			else {
				installForm(DataManager.this);
				center = iComponent.getUIComponent();
			}
			replaceCenterComponent(center, container);
			if (!tableLayout)
				try {
					textToggler.perform(_text() == TEXT, objects());
				} 
				catch(Exception e) {
					Log.e(TAG, "layoutToggler", e);
				};
			propsToFile(props, name);
		}
	};
	
	private int _text() {
		String text = props.getProperty("text");
		if ("script".equals(text))
			return SCRIPT;
		else
			return TEXT;
	}
	
	protected Job<Boolean> textToggler = new Job<Boolean>() {
		public void perform(Boolean textView, Object[] parms) throws Exception {
			props.setProperty("text", textView ? "document" : "script");
	    	String text = getText();
			getTextComponent().toggle(textView, null);
			setText(text);
			propsToFile(props, name);
		}
	};

	class ManagerAction extends CustomAction
    {
		public ManagerAction(ActionType type) {
			super(type);
			if (type.equals(ActionType.TOGGLE1)) {
				putValue(NAME, type.name(_layout() == TABLE ? 2 : 1));
			}
			else if (type.equals(ActionType.TOGGLE2)) {
				putValue(NAME, type.name(_text() == TEXT ? 2 : 1));
			}
        }
        
        public ManagerAction(String text) {
			super(text);
		}

		@Override
        protected void action_Performed(ActionEvent ae) {
//        	String dateString = getDate();
        	ActionType type = (ActionType)getType();
			switch (type) {
			case DATABASE:
				updateOnRequest();
				dbFilePath = chooseDatabase(dbFilePath);
				break;
			case CALENDAR:
//				dateString = pickDate(dateString);
//				date.requestFocusInWindow();
				break;
			case FIRST:
			case PREVIOUS:
			case NEXT:
			case LAST:
				browse(type);
				break;
			case PICK:
//				finder.keyLine();
//				Long time = toTime(dateString, DatePicker.calendarFormat);
//				if (time != null) {
//					pkRow = finder.pointer(time, getTitle());
//					browse(type);
//				}
				break;
			case DATE:
//				date.requestFocusInWindow();
				break;
			case TITLE:
//				comboBoxes[0].requestFocusInWindow();
				break;
			case INSERT:
				break;
			case DELETE:
//				deleteSelection();
				break;
			case STRUCT:
				break;
			case TOGGLE1:
				toggle(this, layoutToggler);
				break;
			case TOGGLE2:
				toggle(this, textToggler);
				break;
			default:
				return;
			}
        }
	}
    
	@Override
	protected String chooseDatabase(String dbString) {
		Container container = getView();
		View view = container instanceof View ? (View) container : null;
		if (dataView.configureData(view, false)) {
			dbString = dataView.getUriString();
			props.setProperty("uri", dbString);
			propsToFile(props, name);
			initialize(dbString);
		}
		return dbString;
	}
	
	private void initialize(String dbString) {
		String tableName = dbTableName(dbString);
		uriString = NotePadProvider.contentUri(tableName).toString();
		refresh();
		JComponent jc = (JComponent) getParent();
		if (jc != null)
			setWindowTitle(this, String.format(
				"Database : %s", 
				trimPath(dbString, jc.getWidth() / 2, jc.getFont(), jc)));
	}
	
	private void clear() {
		setText("");
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
		else
			return (DataModel) dataView.getTable().getModel();
	}
	
	private int convertRowIndexToModel(int rowIndex) {
		return dataView.getTable().convertRowIndexToModel(rowIndex);
	}

	private Provider provider = null;
	private String uriString = NoteColumns.CONTENT_URI.toString();
	private Object pk = null;
	public int pkColumn = -1, pkRow = -1;
	
	private boolean createProvider() {
		String dbPath = dataView.getDataConfiguration().getPath();
		provider = new Provider(NotePad.AUTHORITY, new File(dbPath), uriString);
		ValMap info = provider.info;
		if (info.size() > 0) {
			pk = info.get("PRIMARY_KEY");
			pkColumn = info.getList("name").indexOf(pk);
			if (pkColumn < 0) {
				BerichtsheftPlugin.consoleMessage("dataview.no-primary-key.message");
			}
		}
		else {
			BerichtsheftPlugin.consoleMessage("dataview.no-info-for-table.message", provider.getTableName());
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

    private static final String DEFAULT_SORT_ORDER = "created,title";

	public void refresh() {
		if (!noRefresh) {
			clear();
			if (createProvider()) {
				dataView.populate(provider, null, null, DEFAULT_SORT_ORDER);
				browse(ActionType.PICK);
			}
		}
	}

	@Override
	protected void updateOnRequest() {
		Object item = getItem();
		if (isItemValid(item)) {
			save(item, false);
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
			if (pkColumn > -1) {
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
/*					setDate(bausteinEditing() ? 
							null : 
							formatDate(1, (Long) rec[0]));
					setTitle(stringValueOf(rec[1]));
*/					text = stringValueOf(rec[2]);
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
			objects(ActionType.DELETE.description(), new ManagerAction(ActionType.DELETE))
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
		boolean pkValue = !isAvailable(0, args);
		String dateString = param(null, 0, args);
		String pattern = param(null, 1, args);
		if (pkValue || notNullOrEmpty(dateString) || bausteinEditing())
			try {
				Long time = toTime(dateString, DatePicker.calendarFormat);
				long[] interval = dayInterval(time, 1);
				Object[][] result = provider.query(uriString, 
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
			} catch (Exception e) {
				handleException(e);
			}
		return null;
	}
	
	private boolean bausteinEditing() {
		// TODO Auto-generated method stub
		return false;
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

	private Object getTitle() {
		// TODO Auto-generated method stub
		return null;
	}

	private Object getDate() {
		// TODO Auto-generated method stub
		return null;
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
		long time = toTime(rec[0].toString(), DatePicker.calendarFormat);
		ValMap info = provider.info;
		BidiMultiMap map = new BidiMultiMap();
		map.putValue("note", text);
		map.putValue("modified", now());
		ContentValues values = contentValues(info, map.getKeys(), map.getValues().toArray());
		if (add) {
			values.putNull(pk.toString());
			values.put("created", time);
			values.put("title", rec[1].toString());
			Uri uri = provider.insert(uriString, values);
			return ContentUris.parseId(uri);
		}
		else {
			rec = (Object[]) select(rec);
			return provider.update(appendId(uriString, (Long) rec[3]), values);
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
			done = 0 < provider.delete(appendId(uriString, (Long) rec[3]));
		}
		if (done) {
			clear();
			refresh();
		}
		return done;
	}
	
	@Override
	public void setText(String text) {
		TextEditor2 textBums = getTextComponent();
		updateText(textBums, text);
		textBums.getTextEditor().undo.discardAllEdits();
	}

	public static String noteFormat1 = enclose(FOLD_MARKER[0], " %s\n%s\n", FOLD_MARKER[1]);
	public static String noteFormat2 = enclose(FOLD_MARKER[0], " %s '%s'\n%s\n", FOLD_MARKER[1]);
	
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

	public static void main(String...args) {
		BerichtsheftApp.loadSettings();
		Properties props = new Properties();
    	final DataManager dm = new DataManager(null, props, "DataManager");
		showFrame(null, dm.getName(), 
			new UIFunction() {
				public Component[] apply(final Component comp, Object[] parms) {
					JTable table = dm.dataView.getTable();
					table.setPreferredScrollableViewportSize(new Dimension(600,300));
					return components(dm.getUIComponent());
				}
			}, 
			new UIFunction() {
				public Component[] apply(Component comp, Object[] parms) {
					JFrame frame = (JFrame) comp;
					final Component focus = findComponent(frame, DoubleFeature.FOCUS);
					focus.addMouseListener(new MouseAdapter() {
						@Override
						public void mouseClicked(MouseEvent e) {
							println("mouse clicked on", focus);
						}
					});
					return null;
				}
			}, 
			null, 
			Behavior.EXIT_ON_CLOSE);
	}
	
}

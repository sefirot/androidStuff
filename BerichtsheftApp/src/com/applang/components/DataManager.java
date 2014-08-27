package com.applang.components;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Date;
import java.util.Properties;

import javax.swing.AbstractButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;

import org.gjt.sp.jedit.View;
import org.gjt.sp.jedit.textarea.TextArea;

import com.applang.Dialogs;
import com.applang.PromptDirective;
import com.applang.berichtsheft.BerichtsheftActivity;
import com.applang.berichtsheft.BerichtsheftApp;
import com.applang.berichtsheft.plugin.BerichtsheftPlugin;
import com.applang.components.DataView.DataModel;

import android.app.AlertDialog;
import android.content.ContentUris;
import android.content.ContentValues;
import android.net.Uri;
import android.util.Log;
import static com.applang.Util.*;
import static com.applang.Util1.*;
import static com.applang.Util2.*;
import static com.applang.SwingUtil.*;
import static com.applang.PluginUtils.*;

public class DataManager extends ActionPanel
{
    private static final String TAG = DataManager.class.getSimpleName();
    
	public static final int TABLE = 0;
	public static final int FORM = 1;
	public static final int TEXT = 2;
	public static final int SCRIPT = 3;
	
	private Properties props;
	private String name;
	
	public String getName() {
		return name;
	}

	public DataManager(View view, Properties props, String name, MouseListener clickListener) {
		super(createTextComponent(), view, name, 2);
		this.props = props;
		this.name = name;
		if (clickListener != null)
			this.clickListener = clickListener;
		uriString = this.props.getProperty("uri", "");
		setDataView();
		dataView.setUriString(uriString);
		initialize(dataView.getUriString());
	}
	
	public static class DataPanel extends JPanel
	{
		@Override
		public String toString() {
			Writer writer = write(new StringWriter(), identity(this));
			return writer.toString();
		}

		private DataManager dataManager;
		
		public DataManager getDataManager() {
			return dataManager;
		}

		public DataPanel(DataManager dataManager) {
			super(new BorderLayout());
			this.dataManager = dataManager;
			JPanel gutter = new JPanel();
			gutter.setName("gutter");
			add(gutter, BorderLayout.WEST);
		}
		
		public JComponent getGutter() {
			return findFirstComponent(this, "gutter");
		}
	}

	public void installConstellation(String config) {
		index = -1;
		panes = new DataPanel[0];
		String[] constellation = config.split(",");
		for (String s : constellation) {
			int star = toInt(-1, s);
			if (star > -1) 
				createAnotherPane(++index, star == TABLE);
		}
		props.getProperty("constellation", config);
		featureFile(name, "data-manage", props);
	}
	
	public void modifyConstellation(final Component component) {
		JSplitPane splitPane = (JSplitPane)SwingUtilities.getAncestorOfClass(JSplitPane.class, component);
		switch (panes.length) {
		case 1:
			JComponent pane = getPane(1);
			if (splitPane.getTopComponent() == null)
				splitPane.setTopComponent(pane);
			else
				splitPane.setBottomComponent(pane);
			break;
		case 2:
			Component c = null;
			if (containsComponent((Container)splitPane.getTopComponent(), component)) {
				c = splitPane.getTopComponent();
				splitPane.setTopComponent(null);
			}
			else if (containsComponent((Container)splitPane.getBottomComponent(), component)) {
				c = splitPane.getBottomComponent();
				splitPane.setBottomComponent(null);
			}
			if (c != null) {
				onMouseClickIn((Container) c, clickListener, false);
				panes = arrayslice(panes, panes[0].equals(c) ? 1 : 0, 1);
				index = 0;
				JToolBar bar = findFirstComponent(panes[index], getName());
				for (int i = 0; i < bar.getComponentCount(); i++) {
					AbstractButton btn = (AbstractButton) bar.getComponent(i);
					CustomAction act = (CustomAction) btn.getAction();
					ActionType type = (ActionType)act.getType();
					if (ActionType.DATABASE.equals(type)) {
						addButton(bar, 
								ActionType.TOGGLE1.index(), 
								new ManagerAction(ActionType.TOGGLE1, isTableView(index)), 
								0, 
								i + 1);
						break;
					}
				}
			}
			break;
		}
		splitPane.validate();
		splitPane.repaint();
	}

	private MouseListener clickListener = new MouseAdapter() {
		@Override
		public void mouseClicked(MouseEvent ev) {
			final Component component = (Component) ev.getSource();
			boolean controlKeyPressed = isCtrlKeyHeld(ev);
			println((controlKeyPressed ? "C + " : "") + "mouseClick", identity(component));
			if (controlKeyPressed) {
				modifyConstellation(component);
			}
		}
	};
	
	private DataPanel[] panes = new DataPanel[0];
	
	public int getPanesCount() {
		return panes.length;
	}
	
	public JComponent getPane(int index) {
		while (panes.length <= index) {
			boolean tableView = index > 0 ? isTableView(index - 1) : true;
			createAnotherPane(panes.length, tableView);
		}
		return panes[index];
	}

	private void createAnotherPane(int index, boolean tableView) {
		panes = arrayextend(panes, false, new DataPanel(this));
		JToolBar bar = northToolBar(panes[index], BorderLayout.SOUTH);
		bar.setName(getName());
		this.index = index;
		toggleView(tableView);
		if (panes.length > 1) {
			bar = findFirstComponent(panes[0], getName());
			removeButton(bar, ActionType.TOGGLE1.index());
		}
	}
	
	private int index = -1;
	
	public int getIndex() {
		return index;
	}

	public void setIndex(int index) {
		this.index = index;
	}

	public void setIndexBy(Component component) {
		component = findAncestor(component, new Predicate<Component>() {
			public boolean apply(Component c) {
				if (c instanceof DataPanel) {
					for (index = 0; index < panes.length; index++)
						if (panes[index].equals(c))
							return true;
				}
				return false;
			}
		});
		if (component == null)
			throw new RuntimeException(TAG + " is out of order");
	}

	public boolean isTableView(int index) {
		return containsComponent(panes[index], getTableComponent());
	}
	
	private void onMouseClickIn(Container container, MouseListener mouseListener, boolean addRemove) {
		for (final Component c : DoubleFeature.focusRequestComponents(container)) {
			JComponent jc = (JComponent) (c instanceof TextArea ? ((TextArea) c).getPainter() : c);
			if (addRemove)
				jc.addMouseListener(mouseListener);
			else 
				jc.removeMouseListener(mouseListener);
		}
	}

	private void viewSwitch(Function<Void> func, Object...params) {
		int index = this.index;
		try {
			onMouseClickIn(panes[index], clickListener, false);
			func.apply(params);
		}
		finally {
			onMouseClickIn(panes[index], clickListener, true);
		}
	}
	
	private void setDataView() {
		dataView = new DataView();
		Component component = findFirstComponent(dataView, BorderLayout.SOUTH);
		if (component != null)
			remove(component);
		JTable table = dataView.getTable();
		table.getTableHeader().setName(DoubleFeature.REQUEST);
		table.setFillsViewportHeight(true);
		addNamePart(table, DoubleFeature.FOCUS);
		dataView.showSqlBox(false);
	}
	
	private DataView dataView = null;
	
	public JComponent getTableComponent() {
		return (JComponent) dataView;
	}
	
	private static ITextComponent createTextComponent() {
		TextToggle textToggle = new TextToggle().createBufferedTextArea(null, null);
		textToggle.getTextEdit().installSpellChecker();
		return textToggle;
	}
	
	private DataForm dataForm = null;
	
	private void setDataForm() {
		final BerichtsheftActivity context = BerichtsheftActivity.getInstance((JFrame) getView());
		iComponent = new IComponent() {
			public Component getUIComponent() {
				if (container == null) {	//	lazy initialization
					dataForm = new DataForm(context, 
							DataManager.this, 
							dataView.getDataConfiguration().getProjectionModel(), 
							props.getProperty("layout"));
					container = new JScrollPane(dataForm.getContainer());
					printContainer("panel", container, false);
				}
				return container;
			}
			private Container container = null;
		};
	}
	
	public Container getFormComponent() {
		return (Container) iComponent.getUIComponent();
	}
	
	private Container installTools(boolean tableView) {
		JToolBar bar = findFirstComponent(panes[index], getName());
		bar.removeAll();
		addButton(bar, ActionType.DATABASE.index(), new ManagerAction(ActionType.DATABASE), index);
		if (panes.length == 1)
			addButton(bar, ActionType.TOGGLE1.index(), new ManagerAction(ActionType.TOGGLE1, tableView), index);
		bar.revalidate();
		return bar;
	}
	
	private Container installMoreTools() {
		Container bar = installTools(false);
		addButton(bar, ActionType.FIRST.index(), new ManagerAction(ActionType.FIRST), index);
		addButton(bar, ActionType.PREVIOUS.index(), new ManagerAction(ActionType.PREVIOUS), index);
		addButton(bar, ActionType.NEXT.index(), new ManagerAction(ActionType.NEXT), index);
		addButton(bar, ActionType.LAST.index(), new ManagerAction(ActionType.LAST), index);
		if (getTextToggle() != null)
			addToggle(bar, ActionType.TOGGLE2.index(), new ManagerAction(ActionType.TOGGLE2), index);
		return bar;
	}
	
	private void toggleView(boolean tableView) {
		try {
			viewToggler.perform(tableView, objects());
		} catch (Exception e) {
			Log.e(TAG, "toggleView", e);
		}
	}

	private Job<Boolean> viewToggler = new Job<Boolean>() {
		public void perform(Boolean tableView, Object[] parms) throws Exception {
			Object[] params = objects(_null());
			if (tableView) {
				installTools(true);
				params[0] = getTableComponent();
			}
			else {
				installMoreTools();
				params[0] = getFormComponent();
			}
			viewSwitch(
				new Function<Void>() {
					public Void apply(Object... params) {
						Component center = (Component) params[0];
						replaceCenterComponent(center, panes[index]);
						return null;
					}
				}, 
				params);
			printContainer(String.format("panes[%d]", index), panes[index], DIAG_OFF);
			if (!tableView && getTextToggle() != null)
				try {
					scriptToggler.perform(_script() == TEXT, objects());
				} 
				catch(Exception e) {
					Log.e(TAG, "layoutToggler", e);
				};
			featureFile(name, "data-manage", props);
		}
	};
	
	private int _script() {
		String text = props.getProperty("text");
		if ("script".equals(text))
			return SCRIPT;
		else
			return TEXT;
	}
	
	protected Job<Boolean> scriptToggler = new Job<Boolean>() {
		public void perform(Boolean isText, Object[] parms) throws Exception {
			props.setProperty("text", isText ? "document" : "script");
	    	String text = getText();
			viewSwitch(
				new Function<Void>() {
					public Void apply(Object... params) {
						Boolean textView = (Boolean) params[0];
						getTextToggle().toggle(textView, null);
						return null;
					}
				}, 
				isText);
			setText(text);
			featureFile(name, "data-manage", props);
		}
	};

	class ManagerAction extends CustomAction
    {
		public ManagerAction(ActionType type, Object...params) {
			super(type);
			if (type.equals(ActionType.TOGGLE1)) {
				boolean tableView = param_Boolean(true, 0, params);
				putValue(NAME, type.name(tableView ? 2 : 1));
			}
			else if (type.equals(ActionType.TOGGLE2)) {
				putValue(NAME, type.name(_script() == TEXT ? 2 : 1));
			}
        }
        
        public ManagerAction(String text) {
			super(text);
		}

		@Override
        protected void action_Performed(ActionEvent ae) {
			setIndexBy((Component) ae.getSource());
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
				toggle(this, viewToggler);
				break;
			case TOGGLE2:
				toggle(this, scriptToggler);
				break;
			default:
				return;
			}
			listenToClick(ae);
        }

		public void listenToClick(ActionEvent ae) {
			MouseEvent ev = new MouseEvent((Component) ae.getSource(), 
					ae.getID(), ae.getWhen(), ae.getModifiers(), 0, 0, 1, false);
			clickListener.mouseClicked(ev);
		}
	}
    
	@Override
	protected String chooseDatabase(String dbString) {
		Container container = getView();
		final View view = container instanceof View ? (View) container : null;
		viewSwitch(
			new Function<Void>() {
				public Void apply(Object... params) {
					if (dataView.configureData(view, false)) {
						String dbString = dataView.getUriString();
						props.setProperty("uri", dbString);
						featureFile(name, "data-manage", props);
						initialize(dbString);
					}
					return null;
				}
			});
		return dataView.getUriString();
	}

	private void initialize(String dbString) {
		refresh();
		JComponent parent = (JComponent) getParent();
		if (parent != null)
			setWindowTitle(this, String.format(
				"Database : %s", 
				trimPath(dbString, parent.getWidth() / 2, parent.getFont(), parent)));
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

	private DataAdapter dataAdapter = null;
	private String uriString = null;
	private BidiMultiMap projection = null;
	private String sortOrder = null;
	private Object pk = null;
	public int pkColumn = -1, pkRow = -1;
	
	private boolean createAdapter() {
		DataConfiguration dataConfig = dataView.getDataConfiguration();
		String dbPath = dataConfig.getPath();
		String flavor = dataConfig.getFlavor();
		projection = dataConfig.getProjection();
		sortOrder = dataConfig.getSortOrder();
		uriString = stringValueOf(dataConfig.getUri());
		dataAdapter = new DataAdapter(flavor, new File(dbPath), uriString);
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

	public void refresh() {
		if (!noRefresh) {
			clear();
			if (createAdapter()) {
				dataView.populate(dataAdapter, null, null, sortOrder);
				setDataForm();
				for (index = 0; index < panes.length; index++)
					toggleView(isTableView(index));
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
					selectRowAndScrollToVisible(dataView.getTable(), rows);
				}
				Object[] rec = (Object[]) select();
				if (rec != null && dataForm != null) {
					dataForm.setContent(rec);
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
/*
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
*/	
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
				Object[][] result = dataAdapter.query(uriString, 
						toStrings(projection.getKeys()), 
						whereClause(pkValue), 
						pkValue ? 
								strings("" + pkValue()) : 
								(bausteinEditing() ? 
										strings(pattern) :
										strings("" + interval[0], "" + (interval[1] - 1), pattern)), 
						sortOrder);
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
		if (textBums != null) {
			updateText(textBums, text);
			textBums.getTextEdit().undoManager.discardAllEdits();
		}
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

	public static void askConstellation(Job<String> job, Object...params) {
		AlertDialog.behavior = Behavior.setTimeout(AlertDialog.behavior, true);
		String[] values = strings("1,0");
		PromptDirective.prompt(
			new BerichtsheftActivity(job, arraycast(values, objects())), 
			Dialogs.DIALOG_TEXT_ENTRY, 
			"DataManager", 
			"constellation", 
			values);
	}

	public static void main(String...args) {
    	underTest = param("true", 0, args).equals("true");
		BerichtsheftApp.loadSettings();
		final Properties props = new Properties();
		askConstellation(
			new Job<String>() {
				public void perform(String result, Object[] parms) throws Exception {
					AlertDialog.behavior = Behavior.setTimeout(AlertDialog.behavior, false);
					if (result != null) {
						props.setProperty("constellation", result);
						manage(Behavior.EXIT_ON_CLOSE, props);
					}
					else
						System.exit(0);
				}
			});
	}

	public static Dimension viewportSize = new Dimension(600,200);
	
	public static void manage(int behavior, Properties props) {
    	final DataManager dm = new DataManager(null, props, "Data manager", null);
		dm.installConstellation(props.getProperty("constellation"));
		showFrame(null, dm.getName(), 
			new UIFunction() {
				public Component[] apply(final Component comp, Object[] parms) {
					dm.getTableComponent().setPreferredSize(viewportSize);
					dm.getFormComponent().setPreferredSize(viewportSize);
					JSplitPane splitPane = splitPane(JSplitPane.VERTICAL_SPLIT);
					splitPane.setTopComponent(dm.getPane(0));
					if (dm.getPanesCount() > 1) {
						splitPane.setBottomComponent(dm.getPane(1));
					}
					return components(splitPane);
				}
			}, 
			new UIFunction() {
				public Component[] apply(Component comp, Object[] parms) {
//					Container frame = (JFrame)comp;
//					printContainer("frame", frame, true);
					return null;
				}
			}, 
			null, 
			behavior);
	}
	
}

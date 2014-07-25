package com.applang.components;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import javax.swing.AbstractCellEditor;
import javax.swing.BoxLayout;
import javax.swing.DefaultCellEditor;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListModel;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.border.MatteBorder;
import javax.swing.event.CellEditorListener;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ListDataListener;
import javax.swing.event.ListSelectionListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;

import org.gjt.sp.jedit.View;

import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;

import com.applang.berichtsheft.BerichtsheftActivity;
import com.applang.berichtsheft.BerichtsheftApp;
import com.applang.berichtsheft.plugin.BerichtsheftPlugin;

import static com.applang.Util.*;
import static com.applang.Util1.*;
import static com.applang.Util2.*;
import static com.applang.SwingUtil.*;

@SuppressWarnings("rawtypes")
public class DataView extends JPanel implements IComponent
{
	public static final String TAG = DataView.class.getSimpleName();
	
	private Context context = BerichtsheftActivity.getInstance();
	public Context getContext() {
		return context;
	}

	ContentResolver contentResolver = context.getContentResolver();
	
	public DataView() {
		resetDataConfiguration();
		createUI();
	}
	
	private JComboBox sqlBox;
	private DataTable table;
	
	public JTable getTable() {
		return table;
	}

	public void createUI() {
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		sqlBox = new JComboBox();
		Memory.update(sqlBox, true, "DataView");
        final JTextField textField = comboEdit(sqlBox);
		sqlBox.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				dc.setProjectionModel(null);
				reload(textField.getText());
			}
		});
		textField.addKeyListener(new KeyAdapter() {
			@Override
			public void keyTyped(KeyEvent e) {
				super.keyTyped(e);
				dc.setProjectionModel(null);
			}
		});
		textField.setText("");
		table = new DataTable(new DataModel());
		switchSelectionMode(false);
		add(sqlBox, BorderLayout.NORTH);
		setMaximumDimension(sqlBox, 100);
		add(scrollableViewport(table), BorderLayout.CENTER);
		southStatusBar(this);
	}

	@SuppressWarnings("unused")
	private void toastCellContentsOnDoubleClick() {
		table.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent ev) {
				if (ev.getClickCount() == 2) {
					JTable table = (JTable) ev.getSource();
					int row = table.getSelectedRow();
					int col = table.getSelectedColumn();
					try {
						Object value = table.getModel().getValueAt(row, col);
						Point pt = new Point(ev.getX(), ev.getY());
						SwingUtilities.convertPointToScreen(pt, table);
						Toast.makeText(
								BerichtsheftActivity.getInstance().setLocation(pt), 
								String.valueOf(value), 
								Toast.LENGTH_LONG).show();
					} catch (Exception e) {
						Log.e(TAG, "table cell", e);
					}
				}
			}
		});
	}

	@SuppressWarnings("unused")
	private void selectRowOnRightMouseButtonClick() {
		table.addMouseListener(new MouseAdapter() {
			public void mousePressed(MouseEvent ev) {
				if (SwingUtilities.isRightMouseButton(ev)) {
					int row = table.rowAtPoint(ev.getPoint());
					table.getSelectionModel().setSelectionInterval(row, row);
				}
			}
		});
	}

	private void switchSelectionMode(boolean managed) {
		table.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
		if (managed) {
			table.setCellSelectionEnabled(false);
			table.setRowSelectionAllowed(true);
		} else {
			table.setCellSelectionEnabled(true);
		}
	}
	
	@Override
	public Component getUIComponent() {
		return this;
	}
	
	DataConfiguration dc;
	
	public DataConfiguration getDataConfiguration() {
		return dc;
	}
	
	public void resetDataConfiguration() {
		dc = new DataConfiguration(context, null, null);
	}
	
	public boolean configureData(View view, boolean full) {
		boolean retval = dc.display(view, full);
		if (retval)
			dc.save();
		return retval;
	}

	public void setUri(Uri uri) {
		dc.setUri(uri);
	}
	
	public Uri getUri() {
		return dc.getUri();
	}
	
	public void setUriString(String uriString) {
		setUri(notNullOrEmpty(uriString) ? Uri.parse(uriString) : null);
	}
	
	public String getUriString() {
		return stringValueOf(getUri());
	}

	public void setFlavor(String flavor) {
		dc.getProjectionModel().setFlavor(flavor);
	}

	public String getFlavor() {
		return dc.getProjectionModel().getFlavor();
	}

	public void synchronizeSelection(int[] rows, Object...params) {
		if (sqlBox.isVisible()) {
			showSqlBox(false);
			switchSelectionMode(true);
			JPopupMenu popupMenu = param(null, 0, params);
			popupAdapter = new PopupAdapter(popupMenu);
			table.addMouseListener(popupAdapter);
			tableSelectionListener = param(null, 1, params); 
			table.getSelectionModel().addListSelectionListener(tableSelectionListener);
		}
		selectRowAndScrollToVisible(table, rows);
	}

	public void showSqlBox(boolean show) {
		sqlBox.setVisible(show);
	}
	
	private PopupAdapter popupAdapter = null;
	private ListSelectionListener tableSelectionListener = null;

	public void nosync() {
		if (!sqlBox.isVisible()) {
			showSqlBox(true);
			switchSelectionMode(false);
			if (popupAdapter != null) {
				table.removeMouseListener(popupAdapter);
				popupAdapter = null;
			}
			if (tableSelectionListener != null) {
				table.getSelectionModel().removeListSelectionListener(tableSelectionListener);
				tableSelectionListener = null;
			}
		}
		load();
	}

	public void load() {
		clear();
		reload();
	}

	public boolean load(Uri uri) {
		setUri(uri);
		return reload();
	}
	
	public boolean reload(Object...params) {
		dataAdapter = null;
		final String sql = param(null, 0, params);
		return populate(new Job<Void>() {
			public void perform(Void t, Object[] params) throws Exception {
				wireObserver(contentResolver, true);
				Uri uri = getUri();
				DataModel model = new DataModel();
				String s = sql;
				ProjectionModel projectionModel = dc.getProjectionModel();
				if (projectionModel != null) {
					BidiMultiMap projection = projectionModel.getProjection();
					model.setProjection(projection);
					String tableName = dbTableName(uri);
					s = "select " + join(",", projection.getKeys().toArray()) + " from " + tableName;
				}
				JTextField textField = comboEdit(sqlBox);
				if (nullOrEmpty(s) && sqlBox.isEnabled())
					s = textField.getText();
				Cursor cursor = contentResolver.rawQuery(uri, s);
				if (cursor != null) {
					model.traverse(cursor);
				}
				table.setModel(model);
				if (sqlBox.isVisible())
					textField.setText(getSql());
				else
					textField.setText("");
				wireObserver(contentResolver, false);
			}
		});
    }
	
	public String getSql() {
		return contentResolver.contentProvider.sql;
	}
	
	private ContentObserver contentObserver = new ContentObserver(null) {
		public void onChange(Object arg) {
			table.borderedRowid = null;
			if (sqlBox.isVisible())
				reload();
			else {
				Uri uri = (Uri) arg;
				Long id = parseId(null, uri);
				if (id != null) {
					DataModel model = (DataModel) table.getModel();
					Object pk = dataAdapter.info.get("PRIMARY_KEY");
					table.rowidColumn = model.columns.indexOf(pk);
					int row = model.findRowAt(table.rowidColumn, id);
					ProjectionModel projectionModel = dc.getProjectionModel();
					Object[] columns = projectionModel.getExpandedProjection().getKeys().toArray();
					Object[][] result = dataAdapter.query(getUriString(), toStrings(columns), pk + "=?", strings("" + id));
					if (isAvailable(0, result)) {
						if (row > -1)
							model.setValues(false, row, result[0]);
						else
							model.addValues(false, result[0]);
					}
					else if (row > -1) {
						model.setValues(false, row, null);
					}
					table.borderedRowid = id;
					model.fireTableDataChanged();
				}
			}
		}
	};

	public void wireObserver(ContentResolver contentResolver, boolean unwire) {
		table.setAutoCreateRowSorter(!unwire);
		if (unwire) 
			contentResolver.unregisterContentObserver(contentObserver);
		else 
			contentResolver.registerContentObserver(getUri(), false, contentObserver);
	}
	
	public void clear() {
		resetDataConfiguration();
		table.setModel(new DataModel());
	}
	
	private DataAdapter dataAdapter = null;
	
	private boolean populate(Job<Void> populate) {
		boolean retval = true;
		String msg = "";
		try {
			populate.perform(null, null);
		} catch (Exception e) {
			msg = e.getMessage();
			retval = false;
		}
		if (notNullOrEmpty(dc.getTableName()))
			msg = String.format("%s record(s)", table.getModel().getRowCount());
		else
			setColumnWidthsAsPercentages(table, 
					new double[]{0.10, 0.20, 0.15, 0.05, 0.50});
		message(msg);
		return retval;
    }
	
	public boolean populate(DataAdapter dataAdapter, final Object...params) {
		this.dataAdapter = dataAdapter;
		wireObserver(contentResolver, true);
		return populate(new Job<Void>() {
			public void perform(Void t, Object[] parms) throws Exception {
				DataAdapter dataAdapter = DataView.this.dataAdapter;
				wireObserver(dataAdapter.getContext().getContentResolver(), true);
				ProjectionModel projectionModel = dc.getProjectionModel();
				BidiMultiMap projection = projectionModel.getExpandedProjection();
				DataModel model = dataAdapter.query(getUriString(), 
						projection, 
						params);
				table.setModel(valueOrElse(new DataModel(), model));
				wireObserver(dataAdapter.getContext().getContentResolver(), false);
			}
		});
	}

	public Object[] getSelectedItems() {
		ListSelectionModel sm = table.getSelectionModel();
		if (sm.isSelectionEmpty())
			return null; 
		ValList list = vlist();
		int[] rows = table.getSelectedRows();
		int[] cols = table.getSelectedColumns();
		for (int i = 0; i < rows.length; i++) {
			ValList items = vlist();
			for (int j = 0; j < cols.length; j++) {
				items.add(table.getModel().getValueAt(rows[i], cols[j]));
			}
			list.add(items);
		}
		return list.toArray();
	}
	
	public static ValList getSelectedColumnNames(JTable table) {
		ValList columns = vlist();
		int[] cols = table.getSelectedColumns();
		for (int i = 0; i < cols.length; i++) {
			TableColumn col = table.getTableHeader().getColumnModel().getColumn(cols[i]);
			columns.add(col.getHeaderValue());
		}
		return columns;
	}
	
	public static DefaultTableModel dbTablesModel(Context context, Uri uri) {
		ValList rows = vlist();
		ValList tables = tables(context, uri);
		for (Object table : tables) {
			rows.add(objects(
				table,
				recordCount(context, dbTable(uri, table.toString()))
			));
		}
		return new DefaultTableModel(
			rows.toArray(new Object[0][]), 
			objects("table", "records")) {
				@Override
				public boolean isCellEditable(int row, int column) {
					return false;
				}
		};
	}
	
	public static JComponent dbTablesComponent(Context context, Uri uri, 
			final Job<JTable> onDoubleClick, 
			final Object...params) {
		DefaultTableModel model = dbTablesModel(context, uri);
		JTable table = new JTable(model);
		table.setName("table");
		table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		if (onDoubleClick != null) {
			table.addMouseListener(new MouseAdapter() {
				public void mouseClicked(MouseEvent ev) {
					if (ev.getClickCount() == 2) {
						JTable table = (JTable) ev.getSource();
						try {
							onDoubleClick.perform(table, params);
						} catch (Exception e) {
							Log.e(TAG, "dbTablesComponent", e);
						}
					}
				}
			});
		}
		return scrollableViewport(table);
	}
	
	public static File chooseDb(Function<File> chooser, final boolean providerIncluded, Object...params) {
		final String key = "Database.memory";
		File file = getFileFromStore(0, 
			"SQLite database", 
			new FileNameExtensionFilter("db files", "db"),
			chooser, 
			new Function<String[]>() {
				public String[] apply(Object... params) {
					if (param_Boolean(false, 1, params))
						return null;
					ValList list = (ValList) getListSetting(key, vlist());
					if (providerIncluded) {
						for (Object pkg : providerPackages) {
							list.add(String.valueOf(pkg));
						}
					}
					return list.toArray(strings());
				}
			}, 
			new Job<String[]>() {
				public void perform(String[] fileNames, Object[] params) throws Exception {
					List<String> list = asList(fileNames);
					putListSetting(key, filter(list, true, isProvider));
				}
			}, 
			params);
		if (file != null) {
			String path = file.getPath();
			if (isSQLite(file) || isProvider.apply(path) || !fileExists(file))
				return file;
		}
		return null;
	}
	
	@SuppressWarnings("unchecked")
	public static void fillFlavorCombo(JComboBox comboBox) {
		DefaultComboBoxModel model = (DefaultComboBoxModel) comboBox.getModel();
		model.removeAllElements();
		model.addElement(null);
		ValList flavors = contentAuthorities(providerPackages);
		for (Object flavor : sortedSet(flavors)) {
			model.addElement(flavor);
		}
		comboBox.setModel(model);
	}
	
	public static JComponent dbFlavorsComponent(String packageName, 
			final Job<JTable> onDoubleClick, 
			final Object...params) {
		ValList rows = vlist();
		if (notNullOrEmpty(packageName)) {
			ValList flavors = contentAuthorities(strings(packageName));
			for (Object flavor : flavors) {
				rows.add(objects(split(flavor.toString(), DOT_REGEX).get(-1)));
			}
		}
		else  {
			ValList flavors = contentAuthorities(providerPackages);
			for (Object flavor : flavors) {
				rows.add(objects(flavor));
			}
		}
		if (rows.size() < 1)
			return null;
		JTable table = new JTable(
			new DefaultTableModel(
				rows.toArray(new Object[0][]), 
				objects("authority")) {
					@Override
					public boolean isCellEditable(int row, int column) {
						return false;
					}
			});
		table.setName("table");
		table.setTableHeader(null);
		table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		if (onDoubleClick != null) {
			table.addMouseListener(new MouseAdapter() {
				public void mouseClicked(MouseEvent ev) {
					if (ev.getClickCount() == 2) {
						JTable table = (JTable) ev.getSource();
						try {
							onDoubleClick.perform(table, params);
						} catch (Exception e) {
							Log.e(TAG, "dbFlavorsComponent", e);
						}
					}
				}
			});
		}
		return scrollableViewport(table);
	}

	private AlertDialog dialog = null;
	
	public String askFlavor(String packageName) {
		final Object[] params = objects("");
		final Job<JTable> onDoubleClick = new Job<JTable>() {
			public void perform(JTable table, Object[] params) throws Exception {
				int sel = table.getSelectedRow();
				if (sel > -1) {
					params[0] = table.getModel().getValueAt(sel, 0).toString();
				}
				dialog.dismiss();
			}
		};
		JComponent flavorsComponent = dbFlavorsComponent(packageName, onDoubleClick, params);
		if (flavorsComponent == null)
			return packageName;
		dialog = new AlertDialog.Builder(context)
				.setTitle("Database flavor")
				.setView(flavorsComponent)
				.setNeutralButton(android.R.string.ok, new OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						AlertDialog dlg = (AlertDialog) dialog;
						JTable table = dlg.findComponentById(1, "table");
						try {
							onDoubleClick.perform(table, params);
						} catch (Exception e) {
							Log.e(TAG, "askFlavor", e);
						}
					}
				})
				.create();
		dialog.open(0.5, 0.5);
		return params[0].toString();
	}
	
	public Uri askUri(View view, String path) {
		Uri uri = null;
		File dbFile = chooseDb(BerichtsheftPlugin.fileChooser(view), true, path, true);
    	if (dbFile != null) {
    		String flavor = null;
    		String dbPath = dbFile.getPath();
			if (isProvider.apply(dbPath)) {
				String f = askFlavor(dbPath);
				if (nullOrEmpty(f))
					return null;
				if (f.equals(dbPath))
					flavor = f;
				else
					flavor = join(".", dbPath, f);
				if (!makeSureExists(flavor, null))
					return null;
				uri = contentUri(flavor, null);
			}
			else {
				uri = Uri.fromFile(dbFile);
				if (fileExists(dbFile))
					flavor = null;
				else {
					flavor = askFlavor(null);
					if (nullOrEmpty(flavor))
						return null;
					if (!makeSureExists(flavor, dbFile))
						return null;
				}
			}
    		try {
				final Job<JTable> onDoubleClick = new Job<JTable>() {
					public Uri uri = null;
					public void perform(JTable table, Object[] params) throws Exception {
						int sel = table.getSelectedRow();
						if (sel > -1) {
							String tableName = table.getModel().getValueAt(sel, 0).toString();
							uri = dbTable(uri, tableName);
						}
						dialog.dismiss();
					}
				};
				onDoubleClick.getClass().getField("uri").set(onDoubleClick, uri);
				dialog = new AlertDialog.Builder(context)
						.setTitle(dbPath)
						.setView(dbTablesComponent(context, uri, onDoubleClick))
						.setNeutralButton(android.R.string.ok, new OnClickListener() {
							public void onClick(DialogInterface dialog, int which) {
								AlertDialog dlg = (AlertDialog) dialog;
								JTable table = dlg.findComponentById(1, "table");
								try {
									onDoubleClick.perform(table, null);
								} catch (Exception e) {
									Log.e(TAG, "askUri", e);
								}
							}
						})
						.create();
				dialog.open(0.5, 0.5);
				uri = (Uri) onDoubleClick.getClass().getField("uri").get(onDoubleClick);
			} catch (Exception e) {
				Log.e(TAG, "askUri", e);
			}
		}
    	return uri;
	}

	public static class ProjectionModel extends AbstractTableModel implements ListModel
	{
		public ProjectionModel(Context context, Uri uri, String flavor, Object...params) {
			setFlavor(flavor);
			tableName = dbTableName(uri);
			initialize(table_info2(context, uri, tableName, flavor), params);
		}

		private void initialize(ValMap info, Object...params) {
			this.info = info;
			names = info.getList("name").toArray();
			types = info.getList("type").toArray();
			checks = vlist();
			conversions = vlist();
			styles = vlist();
			sortExpressions = vlist();
			int length = names.length;
			for (int i = 0; i < length; i++) {
				checks.add(true);
				conversions.add("");
				styles.add("");
				sortExpressions.add("");
			}
			BidiMultiMap projection = param(null, 0, params);
			if (projection != null && isAvailable(0, projection.getKeys())) {
				for (int i = 0; i < length; i++) {
					Object name = names[i];
					checks.set(i, projection.getValue(name, 3));
					conversions.set(i, stringValueOf(projection.getValue(name)));
					styles.set(i, projection.getValue(name, 4));
					sortExpressions.set(i, projection.getValue(name, 5));
				}
			}
		}

		public ProjectionModel(DataAdapter dataAdapter) {
			setFlavor(dataAdapter.getFlavor());
			tableName = dataAdapter.getTableName();
			initialize(dataAdapter.info);
			int pkColumn = info.getList("name").indexOf(info.get("PRIMARY_KEY"));
			if (pkColumn > -1)
				checks.set(pkColumn, false);
		}

		private String tableName;
		private ValMap info;
		private Object[] names, types;
		private ValList checks, conversions, styles, sortExpressions;
		
		@Override
		public int getRowCount() {
			return names.length;
		}
		@Override
		public int getColumnCount() {
			return 5;
		}
		@Override
		public String getColumnName(int column) {
			switch (column) {
			case 1:
				return "Column";
			case 2:
				return "Type";
			case 3:
				return "Conversion";
			case 4:
				return "Style";
			case 5:
				return "Sort Order";
			default:
				return "";
			}
		}
		@Override
		public Object getValueAt(int row, int col) {
			switch (col) {
			case 1:
				return names[row];
			case 2:
				return types[row];
			case 3:
				return conversions.get(row);
			case 4:
				Object style = styles.get(row);
				return stringValueOf(style);
			case 5:
				return sortExpressions.get(row);
			default:
				return checks.get(row);
			}
		}
		@Override
		public void setValueAt(Object value, int row, int col) {
			switch (col) {
			case 1:
				names[row] = value;
				break;
			case 2:
				types[row] = value;
				break;
			case 3:
				conversions.set(row, value);
				break;
			case 4:
				styles.set(row, value);
				break;
			case 5:
				sortExpressions.set(row, value);
				break;
			default:
				checks.set(row, value);
			}
			changed = true;
			memorizeFlavor();
		}
		@Override
		public Class<?> getColumnClass(int col) {
			if (col == 0)
				return Boolean.class;
			else
				return String.class;
		}
		@Override
		public boolean isCellEditable(int row, int col) {
			if (col == 1 || col == 2)
				return false;
			else
				return true;
		}
		
		private String flavor;
		public void setFlavor(String flavor) {
			this.flavor = flavor;
		}

		public String getFlavor() {
			return flavor;
		}
		
		public boolean hasFlavor() {
			return notNullOrEmpty(flavor);
		}

		public void injectFlavor() {
			if (hasFlavor()) {
				ValMap map = ScriptManager.getProjectionDefault(flavor, tableName);
				if (map.get("version") == info.get("VERSION")) {
					BidiMultiMap projection = (BidiMultiMap) map.get("projection");
					if (projection != null) {
						for (int i = 0; i < names.length; i++) {
							Integer[] index = projection.get(names[i]);
							if (index.length > 0) {
								conversions.set(i, projection.getValue(names[index[0]], 1));
								checks.set(i, projection.getValue(names[index[0]], 3));
								styles.set(i, projection.getValue(names[index[0]], 4));
								sortExpressions.set(i, projection.getValue(names[index[0]], 5));
							}
						}
						fireTableDataChanged();
					}
				}
			}
		}

		private void memorizeFlavor() {
			if (hasFlavor()) {
				ValMap map = vmap();
				map.put("version", info.get("VERSION"));
				map.put("projection", getExpandedProjection());
				ScriptManager.setProjectionDefault(flavor, tableName, map);
			}
		}
		
		public BidiMultiMap getExpandedProjection() {
			return new BidiMultiMap(vlist(names), 
				new ValList(conversions), 
				vlist(types), 
				new ValList(checks), 
				new ValList(styles), 
				new ValList(sortExpressions));
		}

		public BidiMultiMap getProjection() {
			BidiMultiMap projection = getExpandedProjection();
			ValList lists = vlist(projection.getLists());
			lists.remove(5);
			projection.setLists(lists.toArray(new ValList[0]));
			for (int i = names.length - 1; i >= 0; i--) 
				if (!(Boolean) checks.get(i)) {
					projection.remove(i);
				}
			return projection;
		}
		
		@Override
		public String toString() {
			return write_assoc(null, "flavor", getFlavor(), -1) + 
					getExpandedProjection().toString();
		}

		public boolean changed = false;

		@Override
		public int getSize() {
			return getRowCount();
		}

		@Override
		public Object getElementAt(int index) {
			return getValueAt(index, 1);
		}

		@Override
		public void addListDataListener(ListDataListener l) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void removeListDataListener(ListDataListener l) {
			// TODO Auto-generated method stub
			
		}

		public Object[] getStyles(Context context) {
			return context.getResources().getXMLResourceItem("@style/*");
		}

		public Object[] getAllSortExpressions() {
			ValList expressions = vlist(_null());
			for (Object name : names) {
				expressions.add(name + " ASC");
				expressions.add(name + " DESC");
			}
			return expressions.toArray();
		}
		
		public String getSortOrder() {
			ArrayList<String> sortOrder = alist();
			for (Object expr : sortExpressions)
				if (notNullOrEmpty(expr) && sortOrder.indexOf(expr) < 0)
					sortOrder.add(expr.toString());
			return join(",", sortOrder.toArray());
		}
	}
	
	public static class ConversionCellEditor extends AbstractCellEditor implements TableCellEditor, ActionListener
	{
		JTextField textField = new JTextField();
		JPanel panel = null;
		Component relative;
		
		public ConversionCellEditor(Component relative) {
			this.relative = relative;
		}
		
		@Override
		public Object getCellEditorValue() {
			return textField.getText();
		}
		
		@Override
		public void actionPerformed(ActionEvent e) {
			String func = new ScriptManager(
					BerichtsheftApp.getJEditView(), 
					relative, 
					null, null, null, textField.getText())
				.getFunction();
			if (notNullOrEmpty(func)) {
				textField.setText(func);
			}
			stopCellEditing();
		}
		
		@Override
		public Component getTableCellEditorComponent(JTable table,
				Object value, boolean isSelected, int row, int column)
		{
			textField.setText(stringValueOf(value));
			if (panel == null) {
				panel = new JPanel();
				panel.setLayout(new BoxLayout(panel, BoxLayout.LINE_AXIS));
				panel.add(textField);
				JButton btn = new JButton(iconFrom("/images/ellipsis_16x16.png"));
				btn.addActionListener(this);
				panel.add(btn);
				panel.doLayout();
				scaleSize(btn, 0.333);
			}
			textField.requestFocus();
			return panel;
		}
	}
	
	public static JComponent projectionComponent(final Context context, 
			final Component relative, 
			final ProjectionModel model)
	{
		JTable table = new JTable() {
			@SuppressWarnings("unchecked")
			@Override
			public TableCellEditor getCellEditor(final int row, final int column) {
				switch (column) {
				case 3:
					final ConversionCellEditor cellEditor = new ConversionCellEditor(relative);
					cellEditor.addCellEditorListener(new CellEditorListener() {
						public void editingStopped(ChangeEvent e) {
							Object value = cellEditor.getCellEditorValue();
							getModel().setValueAt(value, row, column);
						}
						public void editingCanceled(ChangeEvent e) {
						}
					});
					return cellEditor;
				case 4:
					return new DefaultCellEditor(new JComboBox(model.getStyles(context)));
				case 5:
					return new DefaultCellEditor(new JComboBox(model.getAllSortExpressions()));
				default:
					return super.getCellEditor(row, column);
				}
			}
		};
		table.setModel(model == null ? new DefaultTableModel() : model);
		table.setName("table");
		table.setRowSelectionAllowed(false);
		table.setColumnSelectionAllowed(false);
		setColumnWidthsAsPercentages(table, 0.10);
		return scrollableViewport(table);
	}
	
	public static ProjectionModel askProjection(Context context, String title, ProjectionModel model, Object...params) { 
		JComponent projectionComponent = projectionComponent(context, null, model);
		AlertDialog dialog = new AlertDialog.Builder(context, 
				param_Integer(AlertDialog.behavior, 0, params), 
				param_Integer(3, 1, params))
				.setTitle(valueOrElse("Projection", title))
				.setView(projectionComponent)
				.create();
		dialog.open(1.0, 0.5);
		JTable table = findFirstComponent(projectionComponent, "table");
		if (table != null) {
			AbstractCellEditor ce = (AbstractCellEditor) table.getCellEditor();
			if (ce != null)
				ce.stopCellEditing();
		}
		int result = (Integer) dialog.result;
		if (result > 0)
			return null;
		else
			return model;
	}
	
	public static class DataModel extends AbstractTableModel
	{
		@Override
		public String toString() {
			return String.valueOf(columns) + "\n\n" + 
				String.valueOf(data).replaceAll("(\\],) (\\[)", "$1\n\n$2");
		}
	
		public Vector<Vector<Object>> data = new Vector<Vector<Object>>();
	
		public DataModel traverse(Cursor cursor) {
			if (cursor != null) {
				if (columns.size() < 1) {
					for (int i = 0; i < cursor.getColumnCount(); i++)
						columns.add(cursor.getColumnName(i));
				}
				data.clear();
				com.applang.Util1.traverse(cursor, new Job<Cursor>() {
					public void perform(Cursor cursor, Object[] params)
							throws Exception {
						for (int j = 0; j < cursor.getCount(); j++) {
							Vector<Object> rec = new Vector<Object>();
							for (int i = 0; i < cursor.getColumnCount(); i++) {
								Object value = getCellValue(cursor, j, i);
								rec.add(value);
							}
							data.add(rec);
						}
					}
				});
			}
			return this;
		}
		
		public ValList columns = vlist();
		public String[] conversions = strings();
		
		private boolean hasConversion(int col) {
			return isAvailable(col, conversions) && conversions[col].length() > 0;
		}
		
		private int[] index = null;
		private int index(int index) {
			return this.index == null || index >= this.index.length ? index : this.index[index];
		}
		
		public BidiMultiMap getProjection() {
			ValList keys = vlist();
			ValList values = vlist();
			for (int i = 0; i < getColumnCount(); i++) {
				keys.add(columns.get(index(i)));
				values.add(conversions[index(i)]);
			}
			return new BidiMultiMap(keys, values);
		}
		
		public DataModel setProjection(BidiMultiMap projection) {
			if (projection != null) {
				columns = projection.getKeys();
				conversions = projection.getValues().toArray(strings());
				List<Integer> list = alist();
				Object[] checks = valueOrElse(vlist(), projection.getValues(3)).toArray();
				for (int i = 0; i < columns.size(); i++) 
					if (param_Boolean(true, i, checks)) 
						list.add(i);
				index = toIntArray(list);
			}
			return this;
		}
	    
		public void addValues(boolean convert, Object[] values) {
			Vector<Object> rec = new Vector<Object>();
			for (int i = 0; i < values.length; i++) {
				Object value = values[i];
				if (convert && hasConversion(i)) 
					value = ScriptManager.doConversion(value, conversions[i], "pull");
				rec.add(value);
			}
			data.add(rec);
		}
		
		public Object[] getValues(boolean convert, int rowIndex) {
			Object[] values = data.get(rowIndex).toArray();
			for (int i = 0; i < values.length; i++) {
				if (convert && hasConversion(i)) 
					values[i] = ScriptManager.doConversion(values[i], conversions[i], "push");
			}
			return values;
		}
		
		public int findRowAt(int columnIndex, Object value) {
			if (columnIndex > -1)
				for (int i = 0; i < getRowCount(); i++) 
					if (data.get(i).get(columnIndex).equals(value))
						return i;
			return -1;
		}
	    
		public void setValues(boolean convert, int rowIndex, Object[] values) {
			if (values != null) {
				Vector<Object> rec = data.get(rowIndex);
				for (int i = 0; i < values.length; i++) {
					Object value = values[i];
					if (convert && hasConversion(i)) 
						value = ScriptManager.doConversion(value, conversions[i], "pull");
					rec.set(i, value);
				}
			}
			else
				data.remove(rowIndex);
		}
		
		@Override
		public Object getValueAt(int rowIndex, int columnIndex) {
			return data.get(rowIndex).get(index(columnIndex));
		}
		@Override
		public int getRowCount() {
			return data.size();
		}
		@Override
		public int getColumnCount() {
			return index != null ? index.length : columns.size();
		}
	    @Override
	    public String getColumnName(int index) {
	        return String.valueOf(columns.get(index(index)));
	    }
	    @Override
	    public Class<?> getColumnClass(int index) {
	        Object value = getRowCount() > 0 ? getValueAt(0, index) : null;
			if (value != null)
				return value.getClass();
			else
				return super.getColumnClass(index(index));
	    }
		
	    public String renderCellValue(Object value, int modelColumn) {
	    	int col = index(modelColumn);
	    	if (hasConversion(col))
	    		value = ScriptManager.doConversion(value, conversions[col], "push");
	    	return stringValueOf(value);
	    }
	    
	    public JTable makeTable() {
	    	JTable table = new DataTable(this);
			table.setAutoCreateRowSorter(true);
			return table;
	    }
	}
	
	public static class DataTable extends JTable
	{
		public DataTable(DataModel cm) {
			super(cm);
		}
		
		public int rowidColumn = -1;
		public Long borderedRowid = null;
		
		@Override
	    public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
	        Component c;
			try {
				c = super.prepareRenderer(renderer, row, column);
			} catch (Exception e) {
				c = new DefaultTableCellRenderer();
			}
			DataModel model = (DataModel) getModel();
			if (c instanceof JLabel) {
				String string = model.renderCellValue(
						getValueAt(row, column), 
						convertColumnIndexToModel(column));
				JLabel lbl = (JLabel) c;
				lbl.setText(string);
				lbl.setToolTipText(string);
	        }
			if (borderedRowid != null) {
				Object value = model.getValues(false, row)[rowidColumn];
				if (borderedRowid.equals(value)) {
					JComponent jc = (JComponent)c;
					jc.setBorder(new MatteBorder(1, 
							column < 1 ? 1 : 0, 
							1, 
							column > model.getColumnCount() - 2 ? 1 : 0, 
							Color.RED));
				}
			}
	        return c;
	    }
	}

	public static void main(String...args) {
		BerichtsheftApp.loadSettings();
    	final DataView dv = new DataView();
		dv.load();
		if (dv.configureData(null, true)) {
			dv.reload();
		}
		showFrame(null, dv.getUriString(), 
			new UIFunction() {
				public Component[] apply(final Component comp, Object[] parms) {
					dv.getTable().setPreferredScrollableViewportSize(new Dimension(800,400));
					return components(dv);
				}
			}, 
			null, null, 
			Behavior.EXIT_ON_CLOSE);
	}
}

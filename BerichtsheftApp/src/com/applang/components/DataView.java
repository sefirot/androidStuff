package com.applang.components;

import java.awt.BorderLayout;
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
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.CellEditorListener;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;

import org.gjt.sp.jedit.BeanShell;
import org.gjt.sp.jedit.View;
import org.gjt.sp.jedit.bsh.NameSpace;
import org.gjt.sp.jedit.bsh.UtilEvalError;
import org.w3c.dom.Element;

import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;

import com.applang.Util.BidiMultiMap;
import com.applang.Util.ValMap;
import com.applang.berichtsheft.BerichtsheftApp;
import com.applang.berichtsheft.R;
import com.applang.berichtsheft.plugin.BerichtsheftPlugin;
import com.applang.berichtsheft.plugin.JEditOptionDialog;

import static com.applang.Util.*;
import static com.applang.Util1.*;
import static com.applang.Util2.*;
import static com.applang.SwingUtil.*;

@SuppressWarnings("rawtypes")
public class DataView extends JPanel implements IComponent
{
	public static final String TAG = DataView.class.getSimpleName();
	
	Context context = BerichtsheftApp.getActivity();
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
        final JTextField tf = comboEdit(sqlBox);
		sqlBox.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				dc.setProjectionModel(null);
				reload(tf.getText());
			}
		});
		tf.addKeyListener(new KeyAdapter() {
			@Override
			public void keyTyped(KeyEvent e) {
				super.keyTyped(e);
				dc.setProjectionModel(null);
			}
		});
		tf.setText("");
		table = new DataTable(new DataModel());
		switchSelectionMode(false);
		add(sqlBox, BorderLayout.NORTH);
		setMaximumDimension(sqlBox, 100);
		add(new JScrollPane(table), BorderLayout.CENTER);
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
								BerichtsheftApp.getActivity().setLocation(pt), 
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
	
	public boolean configureData(View view) {
		boolean retval = dc.display(true, view);
		if (retval && sqlBox.isVisible())
			dc.save();
		return retval;
	}

	public void setUri(Uri uri) {
		dc.setUri(uri);
	}
	
	public Uri getUri() {
		return dc.getUri();
	}
	
	public String getUriString() {
		return stringValueOf(getUri());
	}

	public void setFlavor(String flavor) {
		dc.getProjectionModel().flavor = flavor;
	}

	public String getFlavor() {
		return dc.getProjectionModel().flavor;
	}

	public void synchronizeSelection(int[] rows, Object...params) {
		if (sqlBox.isVisible()) {
			sqlBox.setVisible(false);
			switchSelectionMode(true);
			JPopupMenu popupMenu = param(null, 0, params);
			popupAdapter = new PopupAdapter(popupMenu);
			table.addMouseListener(popupAdapter);
			tableSelectionListener = param(null, 1, params); 
			table.getSelectionModel().addListSelectionListener(tableSelectionListener);
		}
		selectRowAndScrollToVisible(table, rows);
	}
	
	private PopupAdapter popupAdapter = null;
	private ListSelectionListener tableSelectionListener = null;

	public void nosync() {
		if (!sqlBox.isVisible()) {
			sqlBox.setVisible(true);
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
		resetDataConfiguration();
		reload();
	}

	public boolean load(Uri uri) {
		setUri(uri);
		return reload();
	}
	
	public boolean reload(Object...params) {
		final String sql = param(null, 0, params);
		return populate(new Job<Void>() {
			public void perform(Void t, Object[] params) throws Exception {
				wireObserver(true);
				Uri uri = getUri();
				String tableName = dbTableName(uri);
				DataModel model = new DataModel();
				String s = sql;
				ProjectionModel projectionModel = dc.getProjectionModel();
				if (projectionModel != null) {
					BidiMultiMap projection = projectionModel.getProjection();
					model.setProjection(projection);
					s = "select " + join(",", projection.getKeys().toArray()) + " from " + tableName;
				}
				if (nullOrEmpty(s) && sqlBox.isEnabled())
					s = comboEdit(sqlBox).getText();
				Cursor cursor = contentResolver.rawQuery(uri, s);
				if (cursor != null) {
					model.traverse(cursor);
				}
				table.setModel(model);
				if (sqlBox.isVisible())
					comboEdit(sqlBox).setText(getSql());
				else
					comboEdit(sqlBox).setText("");
				wireObserver(false);
			}
		});
    }
	
	public String getSql() {
		return contentResolver.contentProvider.sql;
	}
	
	private ContentObserver contentObserver = new ContentObserver() {
		public void onChange(Object arg) {
			DataConfiguration dc = getDataConfiguration();
			String path = dc.getContext().getDatabasePath((Uri)arg);
			if (path.startsWith(dc.getPath()))
				reload();
		}
	};

	public void wireObserver(boolean unwire) {
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
	
	public boolean populate(final Provider provider, final Object...params) {
		dc = new DataConfiguration(provider);
		return populate(new Job<Void>() {
			public void perform(Void t, Object[] parms) throws Exception {
				table.setAutoCreateRowSorter(false);
				DataModel model = provider.query(getUriString(), 
						dc.getProjectionModel().getExpandedProjection(), 
						params);
				table.setModel(model == null ? new DataModel() : model);
				table.setAutoCreateRowSorter(true);
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
		return new JScrollPane(table);
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
		for (Object flavor : flavors) {
			model.addElement(flavor);
		}
		comboBox.setModel(model);
	}
	
	public static JComponent dbFlavorsComponent(String packageName, 
			final Job<JTable> onDoubleClick, 
			final Object...params) {
		ValList rows = vlist();
		if (notNullOrEmpty(packageName)) {
			ValList flavors = contentAuthorities(packageName);
			for (Object flavor : flavors) {
				rows.add(objects(split(flavor.toString(), "\\.").get(-1)));
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
		return new JScrollPane(table);
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
				.setNeutralButton(R.string.button_ok, new OnClickListener() {
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
				if (!makeSureExists(context, flavor, null))
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
					if (!makeSureExists(context, flavor, dbFile))
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
						.setNeutralButton(R.string.button_ok, new OnClickListener() {
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

	public static class ProjectionModel extends AbstractTableModel
	{
		public ProjectionModel(Context context, Uri uri, String flavor, Object...params) {
			this.flavor = flavor;
			tableName = dbTableName(uri);
			info = table_info(context, uri, tableName);
			initialize(tableName, info);
		}

		private void initialize(String tableName, ValMap info, Object...params) {
			this.tableName = tableName;
			this.info = info;
			names = info.getList("name").toArray();
			types = info.getList("type").toArray();
			checks = vlist();
			conversions = vlist();
			int length = names.length;
			for (int i = 0; i < length; i++) {
				checks.add(true);
				conversions.add("");
			}
			BidiMultiMap projection = param(null, 0, params);
			if (projection != null && isAvailable(0, projection.getKeys())) {
				for (int i = 0; i < length; i++) {
					Object name = names[i];
					checks.set(i, projection.getKeys().contains(name));
					conversions.set(i, stringValueOf(projection.getValue(name)));
				}
			}
		}

		public ProjectionModel(Provider provider) {
			this.flavor = provider.flavor;
			initialize(provider.tableName, provider.info);
			int pkColumn = info.getList("name").indexOf(info.get("PRIMARY_KEY"));
			if (pkColumn > -1)
				checks.set(pkColumn, false);
		}

		private String tableName;
		private ValMap info;
		private Object[] names, types;
		private ValList checks, conversions;
		
		@Override
		public int getRowCount() {
			return names.length;
		}
		@Override
		public int getColumnCount() {
			return 4;
		}
		@Override
		public String getColumnName(int column) {
			if (column == 0)
				return "";
			else if (column == 1)
				return "Column";
			else if (column == 2)
				return "Type";
			else
				return "Conversion";
		}
		@Override
		public Object getValueAt(int row, int col) {
			if (col == 0)
				return checks.get(row);
			else if (col == 1)
				return names[row];
			else if (col == 2)
				return types[row];
			else
				return conversions.get(row);
		}
		@Override
		public void setValueAt(Object value, int row, int col) {
			if (col == 0)
				checks.set(row, value);
			else if (col == 1)
				names[row] = value;
			else if (col == 2)
				types[row] = value;
			else
				conversions.set(row, value);
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

		public void injectFlavor() {
			if (notNullOrEmpty(flavor)) {
				ValMap map = ScriptManager.getDefaultConversions(flavor, tableName);
				for (int i = 0; i < names.length; i++) {
					Object conv = map.get(names[i]);
					conversions.set(i, stringValueOf(conv));
				}
				fireTableDataChanged();
			}
		}

		private void memorizeFlavor() {
			if (notNullOrEmpty(flavor)) {
				ValMap map = vmap();
				for (int i = 0; i < names.length; i++) 
					if ((Boolean) checks.get(i)) {
						String key = names[i].toString();
						Object value = conversions.get(i);
						if (notNullOrEmpty(value))
							map.put(key, value);
					}
				ScriptManager.setDefaultConversions(flavor, tableName, map);
			}
		}
		
		public BidiMultiMap getExpandedProjection() {
			return new BidiMultiMap(new ValList(asList(names)), 
				new ValList(conversions), 
				new ValList(asList(types)), 
				new ValList(checks));
		}

		public BidiMultiMap getProjection() {
			BidiMultiMap projection = getExpandedProjection();
			for (int i = names.length - 1; i >= 0; i--) 
				if (!(Boolean) checks.get(i)) {
					projection.remove(i);
				}
			return projection;
		}
		
		@Override
		public String toString() {
			return getExpandedProjection().toString();
		}

		public boolean changed = false;
	}
	
	public static class ConversionCellEditor extends AbstractCellEditor implements TableCellEditor, ActionListener
	{
		JTextField txt = new JTextField();
		JPanel panel = null;
		Component relative;
		
		public ConversionCellEditor(Component relative) {
			this.relative = relative;
		}
		
		@Override
		public Object getCellEditorValue() {
			return txt.getText();
		}
		
		@Override
		public void actionPerformed(ActionEvent e) {
			String func = new ScriptManager(
					BerichtsheftApp.getJEditView(), 
					relative, 
					null, null, null, txt.getText())
				.getFunction();
			if (notNullOrEmpty(func)) {
				txt.setText(func);
			}
			stopCellEditing();
		}
		
		@Override
		public Component getTableCellEditorComponent(JTable table,
				Object value, boolean isSelected, int row, int column)
		{
			txt.setText(stringValueOf(value));
			if (panel == null) {
				panel = new JPanel();
				panel.setLayout(new BoxLayout(panel, BoxLayout.LINE_AXIS));
				panel.add(txt);
				JButton btn = new JButton(iconFrom("/images/ellipsis_16x16.png"));
				btn.addActionListener(this);
				panel.add(btn);
				panel.doLayout();
				scaleSize(btn, 0.333);
			}
			txt.requestFocus();
			return panel;
		}
	}
	
	public static JComponent projectionComponent(final Component relative, TableModel model) {
		JTable table = new JTable() {
			@Override
			public TableCellEditor getCellEditor(final int row, final int column) {
				if (column == 3) {
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
				}
				return super.getCellEditor(row, column);
			}
		};
		table.setModel(model);
		table.setName("table");
		table.setRowSelectionAllowed(false);
		table.setColumnSelectionAllowed(false);
		setColumnWidthsAsPercentages(table, 0.10);
		return new JScrollPane(table);
	}
	
	public ProjectionModel askProjection(ProjectionModel model) { 
		JComponent projectionComponent = projectionComponent(dialog, model);
		dialog = new AlertDialog.Builder(context)
				.setTitle("Projection")
				.setView(projectionComponent)
				.setNeutralButton(R.string.button_close, new OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
					}
				})
				.create();
		dialog.open(0.5, 0.5);
		JTable table = findComponent(projectionComponent, "table");
		if (table != null) {
			AbstractCellEditor ce = (AbstractCellEditor) table.getCellEditor();
			if (ce != null)
				ce.stopCellEditing();
		}
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
		
		boolean hasConversion(int col) {
			return isAvailable(col, conversions) && conversions[col].length() > 0;
		}
		
		private int[] index = null;
		private int index(int index) {
			return this.index == null ? index : this.index[index];
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
				List<Integer> list = new ArrayList<Integer>();
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
		
		@Override
	    public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
	        Component c;
			try {
				c = super.prepareRenderer(renderer, row, column);
			} catch (Exception e) {
				c = new DefaultTableCellRenderer();
			}
			if (c instanceof JLabel) {
				DataModel model = (DataModel) getModel();
				String string = model.renderCellValue(
						getValueAt(row, column), 
						convertColumnIndexToModel(column));
				JLabel lbl = (JLabel) c;
				lbl.setText(string);
				lbl.setToolTipText(string);
	        }
	        return c;
	    }
	}

	public static class Provider
	{
		private Context context;
		public Context getContext() {
			return context;
		}

		public ContentResolver contentResolver;
		
		public Provider(DataView dv) {
			this(dv.getUriString());
		}
		
		public Provider(String uriString) {
			this(BerichtsheftApp.getActivity(), uriString);
		}
		
		public Provider(final String flavor, final File dbFile, String uriString) {
			this(
				new Context() {
	    			{
	    				mPackageInfo = new PackageInfo("", dbFile.getParent());
	    				registerFlavor(flavor, dbFile.getPath());
	    			}
	    		}, 
	    		uriString);
			this.flavor = flavor;
		}
		
		public Provider(Context context, String uriString) {
			this.context = context;
			contentResolver = context.getContentResolver();
			tableName = dbTableName(uriString);
			info = table_info(
					context, 
					uriString, 
					tableName);
		}
		
		private String tableName;
		public String getTableName() {
			return tableName;
		}

		public ValMap info;
		public String flavor = null;

		public Uri makeUri(String uriString) {
			uriString = valueOrElse("", uriString);
			if (flavor == null)
				return Uri.parse(uriString);
			if (uriString.startsWith(ContentResolver.CURSOR_DIR_BASE_TYPE))
				return contentUri(flavor, tableName);
			if (uriString.startsWith(ContentResolver.CURSOR_ITEM_BASE_TYPE))
				return contentUri(flavor, tableName);
			return contentUri(flavor, null);
		}
		
		public Object[][] query(String uriString, String[] columns, Object...params) {
			Uri uri = makeUri(uriString);
			String selection = param(null, 0, params);
			String[] selectionArgs = param(null, 1, params);
			String sortOrder = param(null, 2, params);
			try {
				final ArrayList<Object[]> rows = new ArrayList<Object[]>();
				Cursor cursor = contentResolver.query(uri, 
						columns, 
						selection, selectionArgs, 
						sortOrder);
				traverse(cursor, new Job<Cursor>() {
					public void perform(Cursor c, Object[] params) throws Exception {
						rows.add(getRow(c).toArray());
					}
				});
				return rows.toArray(new Object[0][]);
			} catch (Exception e) {
				BerichtsheftPlugin.consoleMessage("dataview.query.message", uri.toString(), columns);
				return null;
			}
		}
		
		public DataModel query(String uriString, BidiMultiMap projection, Object...params) {
			String selection = param(null, 0, params);
			String[] selectionArgs = param(null, 1, params);
			String sortOrder = param(null, 2, params);
			Uri uri = makeUri(uriString);
			try {
				Cursor cursor = contentResolver.query(uri, 
						projection.getKeys().toArray(strings()), 
						selection, selectionArgs, 
						sortOrder);
				return new DataModel()
						.setProjection(projection)
						.traverse(cursor);
			} catch (Exception e) {
				BerichtsheftPlugin.consoleMessage("dataview.query.message", uri.toString(), projection.getKeys());
				return null;
			}
		}
		
		private Object updateOrInsert_query(Uri uri, 
				ValMap profile, 
				BidiMultiMap projection, 
				String pk, 
				ContentValues values,
				Function<Integer> skipThis) throws UtilEvalError
		{
			boolean found = false;
			if (ProfileManager.transportsLoaded() && profile != null) {
				Object name = profile.get("name");
				Object flavor = profile.get("flavor");
				String xpath = "//FLAVOR[@name='" + flavor + "']";
				xpath += "/FUNCTION[@name='updateOrInsert-clause' and @profile='" + name + "']";
				Element el = selectElement(ProfileManager.transports, xpath);
				if (el != null) {
					String script = el.getTextContent();
					NameSpace tmp = new NameSpace(BeanShell.getNameSpace(), "transport");
					tmp.setVariable("values", values);
					String clause = (String) BeanShell.eval(null, tmp, script);
					Cursor cursor = null;
					try {
						projection.insert(0, pk, null);
						cursor = contentResolver.query(uri, 
								projection.getKeys().toArray(strings()), 
								clause, 
								null, null);
						if (cursor.moveToFirst()) {
							if (skipThis != null) {
								ValList rec = getRow(cursor);
								switch (skipThis.apply(projection, rec.toArray())) {
								case 2:		//	no
								case 3:		//	no all
									return 0;
								case 4:		//	cancel
									return null;
								}
							}
							else
								decision = -1;
							values.put(pk, cursor.getLong(0));
							found = true;
						}
					} 
					finally {
						projection.remove(0);
						if (cursor != null)
							cursor.close();
					}
				}
			}
			return found;
		}
		
		private int decision = -1;
		
		public Function<Integer> skipThis(final View view, final Object[] items) {
			return new Function<Integer>() {
				public Integer apply(Object... params) {
					switch (decision) {
					case 1:
						return 0;
					case 3:
						return 2;
					default:
						BidiMultiMap projection = param(null, 0, params);
						DataModel model = 
								new DataModel().setProjection(projection);
						model.addValues(false, (Object[])param(null, 1, params));
						ValList row = vlist();
						row.add(null);
						row.addAll(asList(items));
						model.addValues(false, row.toArray());
						JTable table = model.makeTable();
						table.setPreferredScrollableViewportSize(new Dimension(800,100));
						table.getSelectionModel().setSelectionInterval(1, 1);
						return decision = new JEditOptionDialog(view, 
								BerichtsheftPlugin.getProperty("dataview.prompt-update.message"), 
								"", 
								new JScrollPane(table), 
								5, 
								Behavior.MODAL, 
								null, null).getResult();
					}
				}
			};
		}
		
		public Object updateOrInsert(String uriString, 
				ValMap profile, 
				BidiMultiMap projection, 
				Object primaryKeyColumnName, 
				ContentValues values,
				Object...params) 
		{
			Uri uri = makeUri(uriString);
			try {
				boolean primaryKey = notNullOrEmpty(primaryKeyColumnName);
				boolean primaryKeyExtraColumn = primaryKey && 
						!projection.getKeys().contains(primaryKeyColumnName.toString());
				if (primaryKeyExtraColumn) {
					boolean found;
					Object retval = updateOrInsert_query(uri, 
							profile, 
							projection, 
							primaryKeyColumnName.toString(), 
							values,
							param((Function<Integer>)null, 0, params));
					if (retval instanceof Boolean)
						found = (boolean) retval;
					else if (retval instanceof Integer)
						return retval;
					else 
						return null;
					primaryKeyExtraColumn = !found;
				}
				Object retval = null;
				if (primaryKeyExtraColumn) {
					values.putNull(primaryKeyColumnName.toString());
					retval = contentResolver.insert(uri, values);
				}
				else if (primaryKey) {
					String pk = primaryKeyColumnName.toString();
					Object pkval = values.get(pk);
					values.remove(pk);
					retval = contentResolver.update(uri, values, pk + "=?", strings(pkval.toString()));
				} else 
					retval = contentResolver.insert(uri, values);
				return retval;
			} catch (Exception e) {
				BerichtsheftPlugin.consoleMessage("dataview.updateOrInsert.message.1", uri.toString(), values);
				return null;
			}
		}
		
		public int[] pickRecords(View view, JTable table, String uriString, ValMap profile) {
			DataModel model = (DataModel) table.getModel();
			BidiMultiMap projection = model.getProjection();
			Object pk = info.get("PRIMARY_KEY");
			int[] results = ints(0,0,0);
			for (int i = 0; i < model.getRowCount(); i++) {
				int row = table.convertRowIndexToView(i);
				if (table.isRowSelected(row)) {
					Object[] items = model.getValues(false, i);
					ContentValues values = contentValues(info, projection.getKeys(), items);
					Object result = updateOrInsert(uriString, profile,
							projection, pk, values, 
							skipThis(view, items));
					if (!checkResult(result, ++results[2])) {
						results = null;
						break;
					} 
					else if (result instanceof Uri)
						results[0]++;
					else
						results[1] += (int) result;
				}
			}
			return results;
		}
		
		public boolean checkResult(Object result, int recno) {
			if (result != null) {
				if (result instanceof Uri) {
					long id = ContentUris.parseId((Uri) result);
					if (id < 0)
						BerichtsheftPlugin.consoleMessage("dataview.updateOrInsert.message.3", recno);
				}
				else if (result instanceof Integer) {
					if ((int)result < 1)
						BerichtsheftPlugin.consoleMessage("dataview.updateOrInsert.message.4", recno);
				}
			}
			else if (decision == 4) {
				BerichtsheftPlugin.consoleMessage("dataview.updateOrInsert.message.2", recno);
				return false;
			}
			return true;
		}

		public Uri insert(String uriString, ContentValues values) {
			Uri uri = makeUri(uriString);
			return contentResolver.insert(uri, values);
		}

		public int update(String uriString, ContentValues values, Object...params) {
			Uri uri = makeUri(uriString);
			String where = param(null, 0, params);
			String[] whereArgs = param(null, 1, params);
			return contentResolver.update(uri, values, where, whereArgs);
		}

		public int delete(String uriString, Object...params) {
			Uri uri = makeUri(uriString);
			String where = param(null, 0, params);
			String[] whereArgs = param(null, 1, params);
			return contentResolver.delete(uri, where, whereArgs);
		}
	}
	
	public static void main(String...args) {
		BerichtsheftApp.loadSettings();
    	underTest = param("true", 0, args).equals("true");
    	final DataView dv = new DataView();
		dv.load();
		if (dv.configureData(null)) {
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

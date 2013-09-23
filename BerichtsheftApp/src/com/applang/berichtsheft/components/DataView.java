package com.applang.berichtsheft.components;

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
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import javax.swing.AbstractCellEditor;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.CellEditorListener;
import javax.swing.event.ChangeEvent;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;

import org.gjt.sp.jedit.BeanShell;
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
import android.view.View;
import android.widget.Toast;

import com.applang.SwingUtil.Behavior;
import com.applang.SwingUtil.UIFunction;
import com.applang.Util.BidiMultiMap;
import com.applang.Util.ValList;
import com.applang.berichtsheft.BerichtsheftApp;
import com.applang.berichtsheft.R;
import com.applang.berichtsheft.plugin.BerichtsheftPlugin;
import com.applang.berichtsheft.plugin.JEditOptionDialog;

import static com.applang.Util.*;
import static com.applang.Util1.*;
import static com.applang.Util2.*;
import static com.applang.SwingUtil.*;

@SuppressWarnings("rawtypes")
public class DataView extends JPanel implements DataComponent
{
	private static final String TAG = DataView.class.getSimpleName();
	
	private Context context = BerichtsheftApp.getActivity();
	
	public DataView() {
		createUI();
	}
	
	private class DataTable extends JTable
	{
		public String renderCellValue(int row, int column) {
			Object value = getValueAt(row, column);
			int col = convertColumnIndexToModel(column);
			if (projection != null) {
				ValList conversions = projection.getValues();
				if (isAvailable(col, conversions))
					value = ScriptManager.doConversion(value, stringValueOf(conversions.get(col)), "push");
			}
			return stringValueOf(value);
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
				String string = renderCellValue(row, column);
				JLabel lbl = (JLabel) c;
				lbl.setText(string);
				lbl.setToolTipText(string);
			}
	        return c;
	    }
	}
	
	private DataTable table;
	private JComboBox sqlBox;
	private JLabel mess;

	public void createUI() {
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		sqlBox = new JComboBox();
		Memory.update(sqlBox, true, "DataView");
        final JTextField tf = comboEdit(sqlBox);
		sqlBox.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				projection = null;
				String sql = tf.getText();
				reload(sql);
			}
		});
		tf.addKeyListener(new KeyAdapter() {
			@Override
			public void keyTyped(KeyEvent e) {
				super.keyTyped(e);
				projection = null;
			}
		});
		table = new DataTable();
		table.addMouseListener(new PopupAdapter(newPopupMenu(
				objects()
		)));
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
		table.setCellSelectionEnabled(true);
		table.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
		add(sqlBox, BorderLayout.NORTH);
		setMaximumDimension(sqlBox, 100);
		add(new JScrollPane(table), BorderLayout.CENTER);
		mess = new JLabel();
		mess.setHorizontalAlignment(JTextField.CENTER);
		add(mess, BorderLayout.SOUTH);
	}
	
	public JTable getTable() {
		return table;
	}

	@Override
	public Component getUIComponent() {
		return this;
	}

	private String schema = null;
	private BidiMultiMap projection = null;
	private Uri uri = null;

	public void setUri(Uri uri) {
		this.uri = uri;
	}
	
	public Uri getUri() {
		return uri;
	}
	
	public String getUriString() {
		return stringValueOf(uri);
	}

	public String getInfo() {
		return dbInfo(this.uri);
	}
	
	public String getDatabasePath() {
		return com.applang.Util1.getDatabaseFile(context, uri).getPath();
	}

	public String uriWithQuery(Uri uri, String tableName, String schema, BidiMultiMap projection) {
		Uri.Builder builder = dbTable(uri, tableName).buildUpon().query("");
		builder.appendQueryParameter("_", stringValueOf(schema));
		ValList names = projection.getKeys();
		ValList convs = projection.getValues(1);
		ValList types = projection.getValues(2);
		for (int i = 0; i < names.size(); i++) {
			String value = stringValueOf(types.get(i)) + "|" + stringValueOf(convs.get(i));
			builder.appendQueryParameter(stringValueOf(names.get(i)), value);
		}
		return builder.toString();
	}

	public void updateUri(String uriString) {
		this.uri = Uri.parse(valueOrElse("", uriString));
		boolean refresh = projection == null;
		if (askProjection() || refresh) {
			uriString = uriWithQuery(uri, dbTableName(uri), schema, projection);
			putSetting("uri", uriString);
			Settings.save();
		}
		load(uri);
	}

	public DataView load() {
		String uriString = getSetting("uri", "");
		if (notNullOrEmpty(uriString)) {
			uri = Uri.parse(uriString);
			String query = uri.getQuery();
			if (query != null) {
				ValList names = vlist();
				ValList convs = vlist();
				ValList types = vlist();
				ValList list = split(query, "&");
				for (int i = 0; i < list.size(); i++) {
					String[] parts = list.get(i).toString().split("=", 2);
					if ("_".equals(parts[0]))
						this.schema = parts[1];
					else {
						names.add(parts[0]);
						parts = parts[1].split("\\|", 2);
						types.add(parts[0]);
						convs.add(parts[1]);
					}
				}
				this.projection = new BidiMultiMap(names, convs, types);
				uri = uri.buildUpon().query(null).build();
			}
			load(uri);
		}
		return this;
	}

	public boolean load(Uri uri) {
		this.uri = uri;
		if (nullOrEmpty(projection))
			return reload();
		String tableName = dbTableName(uri);
		String sql = "select " + join(",", projection.getKeys().toArray()) + " from " + tableName;
		return reload(sql);
	}
	
	public boolean reload(String...sql) {
		boolean retval = true;
		wire(true);
//		sqlBox.setEnabled(!hasAuthority(uri));
		ContentResolver contentResolver = context.getContentResolver();
		String msg = "";
		try {
			if (!isAvailable(0, sql) && sqlBox.isEnabled())
				sql = strings(comboEdit(sqlBox).getText());
			Cursor cursor = contentResolver.rawQuery(uri, sql);
			if (cursor != null) {
				ConsumerModel model = new ConsumerModel().traverse(cursor);
				table.setModel(model);
				if (notNullOrEmpty(dbTableName(uri)))
					msg = String.format("%s record(s)", model.getRowCount());
				else
					setColumnWidthsAsPercentages(table, 
						new double[]{0.10, 0.20, 0.15, 0.05, 0.50});
				wire(false);
			}
		} catch (Exception e) {
			msg = e.getMessage();
			retval = false;
		}
		mess.setText(msg);
		if (sqlBox.isEnabled())
			comboEdit(sqlBox).setText(contentResolver.contentProvider.sql);
		else
			comboEdit(sqlBox).setText("");
		
		return retval;
    }
	
	private ContentObserver contentObserver = new ContentObserver(
		new Job<Uri>() {
			public void perform(Uri u, Object[] params) throws Exception {
				String info = dbInfo(u);
				if (info.startsWith(getInfo()))
					reload();
			}
		});

	public void wire(boolean unwire) {
		if (unwire) {
			context.getContentResolver().unregisterContentObserver(contentObserver);
		}
		else {
			context.getContentResolver().registerContentObserver(getUri(), false, contentObserver);
		}
		table.setAutoCreateRowSorter(!unwire);
	}
	
	public ValMap tableInfo(String...name) {
		String tableName = param(null, 0, name);
		if (tableName != null) 
			return table_info(context, uri, tableName);
		else
			return com.applang.Util1.schema(context, uri);
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
	
	public static JComponent dbTablesComponent(Context context, Uri uri, 
			final Job<JTable> onDoubleClick, 
			final Object...params) {
		ValList rows = vlist();
		ValList tables = tables(context, uri);
		for (Object table : tables) {
			rows.add(objects(
				table,
				recordCount(context, dbTable(uri, table.toString()))
			));
		}
		JTable table = new JTable(
			new DefaultTableModel(
				rows.toArray(new Object[0][]), 
				objects("table", "records")) {
					@Override
					public boolean isCellEditable(int row, int column) {
						return false;
					}
			});
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
	
	public static File chooseDb(Function<File> chooser, final boolean urisIncluded, Object...params) {
		final String key = "Database.memory";
		File file = getFileFromStore(0, 
			"SQLite database", 
			new FileNameExtensionFilter("db files", "db"),
			chooser, 
			new Function<String[]>() {
				public String[] apply(Object... params) {
					ValList list = (ValList) getListSetting(key, vlist());
					if (urisIncluded) {
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
	
	public static JComponent dbSchemasComponent(String packageName, 
			final Job<JTable> onDoubleClick, 
			final Object...params) {
		ValList rows = vlist();
		if (notNullOrEmpty(packageName)) {
			ValList schemas = contentAuthorities(packageName);
			for (Object schema : schemas) {
				rows.add(objects(split(schema.toString(), "\\.").get(-1)));
			}
		}
		else  {
			ValList schemas = contentAuthorities(providerPackages);
			for (Object schema : schemas) {
				rows.add(objects(schema));
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
							Log.e(TAG, "dbSchemasComponent", e);
						}
					}
				}
			});
		}
		return new JScrollPane(table);
	}

	public static boolean openOrCreateDb(Context context, String authority, File target) {
		try {
			Class<?> c = Class.forName(authority + "Provider");
			for (Class<?> cl : c.getDeclaredClasses()) {
				String dbName = c.getDeclaredField("DATABASE_NAME").get(null).toString();
				if ("DatabaseHelper".equals(cl.getSimpleName())) {
					Object inst = cl.getConstructor(Context.class, String.class)
							.newInstance(context, target == null ? dbName : "temp.db");
					Method method = cl.getMethod("getWritableDatabase");
					method.invoke(inst);
					cl.getMethod("close").invoke(inst);
					if (target != null) {
						File tempDb = context.getDatabasePath("temp.db");
						copyFile(tempDb, target);
						tempDb.delete();
					}
				}
			}
			return true;
		} catch (Exception e) {
			Log.e(TAG, "createDb", e);
			return false;
		}
	}
	
	private AlertDialog dialog = null;
	
	public String askSchema(String packageName) {
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
		JComponent schemasComponent = dbSchemasComponent(packageName, onDoubleClick, params);
		if (schemasComponent == null)
			return packageName;
		dialog = new AlertDialog.Builder(context)
				.setTitle("Database schema")
				.setView(new View(schemasComponent).setId(1))
				.setNeutralButton(R.string.button_ok, new OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						AlertDialog dlg = (AlertDialog) dialog;
						View vw = dlg.findViewById(1);
						JTable table = findComponent(vw.getComponent(), "table");
						try {
							onDoubleClick.perform(table, params);
						} catch (Exception e) {
							Log.e(TAG, "askDbSchema", e);
						}
					}
				})
				.create();
		dialog.open(0.5, 0.5);
		return params[0].toString();
	}
	
	public boolean askUri(Function<File> chooser, String info) {
		File dbFile = chooseDb(chooser, true, info);
    	if (dbFile != null) {
    		Context context = BerichtsheftApp.getActivity();
    		String dbPath = dbFile.getPath();
			if (isProvider.apply(dbPath)) {
				String schemaName = askSchema(dbPath);
				if (nullOrEmpty(schemaName))
					return false;
				if (schemaName.equals(dbPath))
					schema = schemaName;
				else
					schema = join(".", dbPath, schemaName);
				if (!openOrCreateDb(context, schema, null))
					return false;
				uri = contentUri(schema, null);
			}
			else {
				uri = Uri.fromFile(dbFile);
				if (fileExists(dbFile))
					schema = null;
				else {
					schema = askSchema(null);
					if (nullOrEmpty(schema))
						return false;
					if (!openOrCreateDb(context, schema, dbFile))
						return false;
				}
			}
			projection = null;
    		final Job<JTable> onDoubleClick = new Job<JTable>() {
				public void perform(JTable table, Object[] params) throws Exception {
					int sel = table.getSelectedRow();
					if (sel > -1) {
						String t = table.getModel().getValueAt(sel, 0).toString();
						uri = dbTable(uri, t);
					}
					dialog.dismiss();
				}
			};
			dialog = new AlertDialog.Builder(context)
					.setTitle(dbPath)
					.setView(new View(dbTablesComponent(context, uri, onDoubleClick)).setId(1))
					.setNeutralButton(R.string.button_ok, new OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							AlertDialog dlg = (AlertDialog) dialog;
							View vw = dlg.findViewById(1);
							JTable table = findComponent(vw.getComponent(), "table");
							try {
								onDoubleClick.perform(table, null);
							} catch (Exception e) {
								Log.e(TAG, "askUri", e);
							}
						}
					})
					.create();
			dialog.open(0.5, 0.5);
			return uri != null;
		}
    	else
    		return false;
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
				scaleDimension(btn, 0.333);
			}
			txt.requestFocus();
			return panel;
		}
	}

	public static JComponent projectionComponent(final Component relative, final BidiMultiMap projection) 
	{
		DefaultTableModel model = new DefaultTableModel(projection.getKeys().size(), 4) {
			ValList checks = projection.getValues(3);
			ValList names = projection.getKeys();
			ValList types = projection.getValues(2);
			ValList convs = projection.getValues();
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
					return names.get(row);
				else if (col == 2)
					return types.get(row);
				else
					return convs.get(row);
			}
			@Override
			public void setValueAt(Object value, int row, int col) {
				if (col == 0)
					checks.set(row, value);
				else if (col == 1)
					names.set(row, value);
				else if (col == 2)
					types.set(row, value);
				else
					convs.set(row, value);
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
		};
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
	
	public boolean askProjection() { 
		String tableName = dbTableName(uri);
		ValMap info = tableInfo(tableName);
		Object[] names = info.getList("name").toArray();
		Object[] types = info.getList("type").toArray();
		ValList checks = vlist();
		ValList conversions = vlist();
		int length = names.length;
		for (int i = 0; i < length; i++) 
			checks.add(true);
		if (projection != null) {
			for (int i = 0; i < length; i++) {
				checks.set(i, projection.getKeys().contains(names[i]));
				conversions.add(projection.getValue(names[i]));
			}
		}
		else if (notNullOrEmpty(schema)) {
			ValMap map = ScriptManager.getDefaultConversions(schema, tableName);
			for (int i = 0; i < length; i++) {
				Object conv = map.get(names[i]);
				conversions.add(conv);
			}
		}
		projection = new BidiMultiMap(new ValList(asList(names)), 
				new ValList(conversions), 
				new ValList(asList(types)), 
				checks);
		JComponent projectionComponent = projectionComponent(dialog, projection);
		dialog = new AlertDialog.Builder(context)
				.setTitle("Projection")
				.setView(new View(projectionComponent).setId(1))
				.setNeutralButton(R.string.button_ok, new OnClickListener() {
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
		boolean convUnchanged = conversions.equals(projection.getValues());
		ValMap map = vmap();
		conversions = projection.getValues();
		checks = projection.getValues(3);
		for (int i = length - 1; i >= 0; i--) 
			if ((boolean) checks.get(i)) {
				String key = names[i].toString();
				Object value = conversions.get(i);
				if (notNullOrEmpty(value))
					map.put(key, value);
			}
			else
				projection.remove(i);
		if (notNullOrEmpty(schema) && !convUnchanged) {
			ScriptManager.setDefaultConversions(schema, tableName, map);
		}
		return !convUnchanged;
	}
	
	public static class ConsumerModel extends AbstractTableModel
	{
		@Override
		public String toString() {
			return String.valueOf(data).replaceAll("(\\],) (\\[)", "$1\n\n$2");
		}
	
		public Vector<Vector<Object>> data = new Vector<Vector<Object>>();
	
		public ConsumerModel traverse(Cursor cursor) {
			if (columns.size() < 1) {
				for (int i = 0; i < cursor.getColumnCount(); i++)
					columns.add(cursor.getColumnName(i));
			}
			data.clear();
			com.applang.Util1.traverse(cursor, new Job<Cursor>() {
				public void perform(Cursor cursor, Object[] params) throws Exception {
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
			return this;
		}
		
		private ValList columns = vlist();
	    private String[] functions = strings();
		
		public BidiMultiMap getProjection() {
			return new BidiMultiMap(columns, new ValList(asList(functions)));
		}
		
		public ConsumerModel setProjection(BidiMultiMap projection) {
			columns = projection.getKeys();
			functions = projection.getValues().toArray(strings());
			return this;
		}
	    
		public void addValues(boolean convert, Object[] values) {
			Vector<Object> rec = new Vector<Object>();
			for (int i = 0; i < values.length; i++) {
				Object value = values[i];
				if (convert && isAvailable(i, functions)) 
					value = ScriptManager.doConversion(value, functions[i], "pull");
				rec.add(value);
			}
			data.add(rec);
		}
		
		public Object[] getValues(boolean convert, int rowIndex) {
			Object[] values = data.get(rowIndex).toArray();
			for (int i = 0; i < values.length; i++) {
				if (convert && isAvailable(i, functions)) 
					values[i] = ScriptManager.doConversion(values[i], functions[i], "push");
			}
			return values;
		}
		
		@Override
		public Object getValueAt(int rowIndex, int columnIndex) {
			return data.get(rowIndex).get(columnIndex);
		}
		@Override
		public int getRowCount() {
			return data.size();
		}
		@Override
		public int getColumnCount() {
			return columns.size();
		}
	    @Override
	    public String getColumnName(int index) {
	        return String.valueOf(columns.get(index));
	    }
	    @Override
	    public Class<?> getColumnClass(int c) {
	        Object value = getRowCount() > 0 ? getValueAt(0, c) : null;
			if (value != null)
				return value.getClass();
			else
				return super.getColumnClass(c);
	    }
		
	    public JTable makeTable() {
	    	JTable table = new JTable(this) {
	    		public String renderCellValue(int row, int column) {
	    			Object value = getValueAt(row, column);
	    			int col = convertColumnIndexToModel(column);
	    			if (isAvailable(col, functions))
	    				value = ScriptManager.doConversion(value, functions[col], "push");
	    			return stringValueOf(value);
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
	    	        	JLabel lbl = (JLabel) c;
	    				String string = renderCellValue(row, column);
	    				lbl.setText(string);
	    				lbl.setToolTipText(string);
	    	        }
	    	        return c;
	    	    }
	    	};
			table.setAutoCreateRowSorter(true);
			return table;
	    }
	}

	public static class ProviderModel
	{
		private Context context = BerichtsheftApp.getActivity();
		
		public ProviderModel(String uriString) {
			tableName = dbTableName(uriString);
			info = table_info(
					context, 
					uriString, 
					tableName);
		}
		
		public String tableName;
		public ValMap info;
		
		ContentResolver contentResolver = context.getContentResolver();
		
		public Object[][] query(String uriString, String[] columns, Object...params) {
			String selection = param(null, 0, params);
			String[] selectionArgs = param(null, 1, params);
			String sortOrder = param(null, 2, params);
			try {
				final ArrayList<Object[]> rows = new ArrayList<Object[]>();
				Cursor cursor = contentResolver.query(Uri.parse(uriString), 
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
				BerichtsheftPlugin.consoleMessage("berichtsheft.query.message", uriString, columns);
				return null;
			}
		}
		
		public ConsumerModel query(String uriString, BidiMultiMap projection, Object...params) {
			String selection = param(null, 0, params);
			String[] selectionArgs = param(null, 1, params);
			String sortOrder = param(null, 2, params);
			try {
				Cursor cursor = contentResolver.query(Uri.parse(uriString), 
						projection.getKeys().toArray(strings()), 
						selection, selectionArgs, 
						sortOrder);
				return new ConsumerModel().setProjection(projection)
						.traverse(cursor);
			} catch (Exception e) {
				BerichtsheftPlugin.consoleMessage("berichtsheft.query.message", uriString, projection.getKeys());
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
				Object schema = profile.get("schema");
				String xpath = "//SCHEMA[@name='" + schema + "']";
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
		
		public Function<Integer> skipThis(final org.gjt.sp.jedit.View view, final Object[] items) {
			return new Function<Integer>() {
				public Integer apply(Object... params) {
					switch (decision) {
					case 1:
						return 0;
					case 3:
						return 2;
					default:
						BidiMultiMap projection = param(null, 0, params);
						ConsumerModel model = 
								new ConsumerModel().setProjection(projection);
						model.addValues(false, (Object[])param(null, 1, params));
						ValList row = vlist();
						row.add(null);
						row.addAll(asList(items));
						model.addValues(false, row.toArray());
						JTable table = model.makeTable();
						table.setPreferredScrollableViewportSize(new Dimension(800,200));
						table.getSelectionModel().setSelectionInterval(1, 1);
						return decision = new JEditOptionDialog(view, 
								BerichtsheftPlugin.getProperty("berichtsheft.transport-from-buffer.label"), 
								BerichtsheftPlugin.getProperty("berichtsheft.prompt-update.message"), 
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
			try {
				Uri uri = Uri.parse(uriString);
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
				BerichtsheftPlugin.consoleMessage("dataview.updateOrInsert.message.1", uriString, values);
				return null;
			}
		}
		
		public int[] pickRecords(org.gjt.sp.jedit.View view, JTable table, String uriString, ValMap profile) {
			ConsumerModel model = (ConsumerModel) table.getModel();
			BidiMultiMap projection = model.getProjection();
			Object pk = info.get("PRIMARY_KEY");
			int[] results = new int[]{0,0,0};
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
	}
	
	public static void main(String...args) {
		BerichtsheftApp.loadSettings();
    	underTest = param("true", 0, args).equals("true");
		int modality = Behavior.NONE;
		if (underTest)
			modality |= Behavior.EXIT_ON_CLOSE;
    	final DataView dv = new DataView();
		if (!dv.askUri(null, dv.getInfo())) {
			dv.load();
		}
		showFrame(null, dv.getUriString(), 
			new UIFunction() {
				public Component[] apply(final Component comp, Object[] parms) {
					dv.updateUri(dv.getUriString());
					dv.getTable().setPreferredScrollableViewportSize(new Dimension(800,400));
					return components(dv);
				}
			}, 
			null, null, 
			modality);
	}
}

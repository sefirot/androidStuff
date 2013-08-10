package com.applang.berichtsheft.components;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.lang.reflect.Method;
import java.util.List;

import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;

import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.applang.Util.ValList;
import com.applang.Util.ValMap;
import com.applang.berichtsheft.BerichtsheftApp;
import com.applang.berichtsheft.R;

import static com.applang.Util.*;
import static com.applang.Util1.*;
import static com.applang.Util2.*;
import static com.applang.SwingUtil.*;

public class DataView extends JPanel
{
	private static final String TAG = DataView.class.getSimpleName();
	
	private Context context;
	
	public DataView(Context context) {
		this.context = context;
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        final JTextField tf = comboEdit(cb);
		cb.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				reload(tf.getText());
			}
		});
		cb.setEditable(true);
		cb.setName("DataView");
		Memory.update(cb, false);
		comboEdit(cb).addMouseListener(newPopupAdapter(
			objects("+", 
        		newMenu("Memory", 
        			objects("add", new ActionListener() {
        	        	public void actionPerformed(ActionEvent ae) {
        	        		Memory.update(cb, tf.getText());
        	        	}
        	        }), 
        	        objects("remove", new ActionListener() {
        	        	public void actionPerformed(ActionEvent ae) {
        	        		Memory.update(cb, null, tf.getText());
        	        	}
        	        })
        		)
	        )
        ));
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
		add(cb);
		add(new JScrollPane(table));
		add(lbl);
	}
	
	@SuppressWarnings("rawtypes")
	private JComboBox cb = new JComboBox();
	private JTable table = new JTable() {
	    public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
	        Component c = super.prepareRenderer(renderer, row, column);
	        if (c instanceof JComponent) {
	            JComponent jc = (JComponent) c;
	            jc.setToolTipText(String.valueOf(getValueAt(row, column)));
	        }
	        return c;
	    }
	};
	public JTable getTable() {
		return table;
	}

	private JLabel lbl = new JLabel();
	
	private Uri uri = null;
	
	public Uri getUri() {
		return uri;
	}

	public void setUri(Uri uri) {
		this.uri = uri;
	}

	public void setUri(String uriString) {
		this.uri = Uri.parse(valueOrElse("", uriString));;
	}

	public String getInfo() {
		return dbInfo(this.uri);
	}
	
	public String getDatabasePath() {
		if (hasAuthority(uri)) {
			String name = databaseName(uri.getAuthority());
			return context.getDatabasePath(name).getPath();
		}
		else
			return uri.getPath();
	}

	public boolean load(String dbPath, String dbTable) {
		return load(fileUri(dbPath, dbTable));
	}
	
	public boolean load(Uri uri) {
		this.uri = uri;
		return reload();
	}
	
	public boolean reload(String...sql) {
		boolean retval = true;
		if (table.getModel() instanceof ProviderModel)
			((ProviderModel)table.getModel()).cursor.close();
		
		ContentResolver contentResolver = context.getContentResolver();
		String msg = "";
		try {
			Cursor cursor = contentResolver.rawQuery(uri, sql);
			if (cursor != null) {
				table.setModel(new ProviderModel(cursor));
				if (notNullOrEmpty(dbTableName(uri)))
					msg = String.format("%s record(s)", cursor.getCount());
				else
					setWidthAsPercentages(table, 
						new double[]{0.10, 0.20, 0.15, 0.05, 0.50});
			}
		} catch (Exception e) {
			msg = e.getMessage();
			retval = false;
		}
		lbl.setText(msg);
		comboEdit(cb).setText(contentResolver.contentProvider.sql);
		
		return retval;
    }
	
	public ValMap schema(String...name) {
		String tableName = param(null, 0, name);
		if (tableName != null) 
			return table_info(context, uri, tableName);
		else
			return com.applang.Util1.schema(context, uri);
	}
   
	class ProviderModel extends AbstractTableModel
	{
		private Cursor cursor;

		public ProviderModel(Cursor cursor) {
			super();
			this.cursor = cursor;
		}
		
		@Override
		protected void finalize() throws Throwable {
			cursor.close();
			super.finalize();
		}

		@Override
		public int getRowCount() {
			return cursor.getCount();
		}

		@Override
		public int getColumnCount() {
			return cursor.getColumnCount();
		}

        public String getColumnName(int columnIndex) {
            return cursor.getColumnName(columnIndex);
        }

		@Override
		public Object getValueAt(int rowIndex, int columnIndex) {
			if (cursor.moveToPosition(rowIndex))
				switch (cursor.getType(columnIndex)) {
				case Cursor.FIELD_TYPE_FLOAT:
					return cursor.getDouble(columnIndex);
				case Cursor.FIELD_TYPE_INTEGER:
					return cursor.getLong(columnIndex);
				case Cursor.FIELD_TYPE_STRING:
					return cursor.getString(columnIndex);
				case Cursor.FIELD_TYPE_BLOB:
					return cursor.getBlob(columnIndex);
				case Cursor.FIELD_TYPE_NULL:
					break;
				}
			return null;
		}
	}
	
	public static ValList getProjection(JTable table) {
		ValList projection = new ValList();
		int[] cols = table.getSelectedColumns();
		for (int i = 0; i < cols.length; i++) {
			TableColumn col = table.getTableHeader().getColumnModel().getColumn(cols[i]);
			projection.add(col.getHeaderValue());
		}
		return projection;
	}
	
	public static JComponent dbTablesComponent(Context context, Uri uri, 
			final Job<JTable> onDoubleClick, 
			final Object...params) {
		ValList rows = new ValList();
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
	
	public static File askDb(Function<File> chooser, Object...params) {
		final String key = "Database.memory";
		File file = getFileFromStore(0, 
			"SQLite database", 
			new FileNameExtensionFilter("db files", "db"),
			chooser, 
			new Function<String[]>() {
				public String[] apply(Object... params) {
					ValList list = (ValList) getListSetting(key, new ValList());
					for (Object authority : 
						contentAuthorities(BerichtsheftApp.getActivity(), providerPackage)) {
						list.add(String.valueOf(authority));
					}
					return list.toArray(new String[0]);
				}
			}, 
			new Job<String[]>() {
				public void perform(String[] fileNames, Object[] params) throws Exception {
					List<String> list = com.applang.Util.list(fileNames);
					putListSetting(key, filter(list, true, isProvider));
				}
			}, params);
		if (file != null) {
			String path = file.getPath();
			if (isSQLite(file) || isProvider.apply(path) || !fileExists(file))
				return file;
		}
		return null;
	}
	
	public static JComponent dbSchemasComponent(
			final Job<JTable> onDoubleClick, 
			final Object...params) {
		ValList rows = new ValList();
		ValList schemas = contentAuthorities(BerichtsheftApp.getActivity(), providerPackage);
		for (Object schema : schemas) {
			rows.add(objects(split(schema.toString(), "\\.").get(-1)));
		}
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
	
	public String askDbSchema() {
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
		dialog = new AlertDialog.Builder(context)
				.setTitle("Database schema")
				.setView(new View(dbSchemasComponent(onDoubleClick, params)).setId(1))
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
		Dimension size = dialog.getSize();
		dialog.setSize(new Dimension(size.width / 2, size.height / 2));
		dialog.open();
		return params[0].toString();
	}

	public static boolean createDb(Context context, String authority, File dbFile) {
		try {
			Class<?> c = Class.forName(authority + "Provider");
			for (Class<?> cl : c.getDeclaredClasses()) {
				if ("DatabaseHelper".equals(cl.getSimpleName())) {
					Object inst = cl.getConstructor(Context.class, String.class)
							.newInstance(context, "temp.db");
					Method method = cl.getMethod("getWritableDatabase");
					method.invoke(inst);
					cl.getMethod("close").invoke(inst);
					copyFile(context.getDatabasePath("temp.db"), dbFile);
				}
			}
			return true;
		} catch (Exception e) {
			Log.e(TAG, "createDb", e);
			return false;
		}
	}
	
	private AlertDialog dialog = null;
	
	public boolean askUri(Function<File> chooser, String info) {
		File dbFile = askDb(chooser, info);
    	if (dbFile != null) {
    		Context context = BerichtsheftApp.getActivity();
			if (!fileExists(dbFile)) {
				String schema = askDbSchema();
				if (!notNullOrEmpty(schema))
					return false;
				if (!createDb(context, join(".", objects(providerPackage, schema)), dbFile))
					return false;
			}
    		if (isSQLite(dbFile))
    			uri = Uri.fromFile(dbFile);
    		else {
        		String authority = dbFile.getPath();
        		uri = contentUri(authority, null);
    		}
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
					.setTitle(dbFile.getPath())
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
			Dimension size = dialog.getSize();
			dialog.setSize(new Dimension(size.width / 2, size.height / 2));
			dialog.open();
			return uri != null;
		}
    	else
    		return false;
	}

	public Object[] getSelectedItems() {
		ListSelectionModel sm = table.getSelectionModel();
		if (sm.isSelectionEmpty())
			return null; 
		ValList list = new ValList();
		int[] rows = table.getSelectedRows();
		int[] cols = table.getSelectedColumns();
		for (int i = 0; i < rows.length; i++) {
			for (int j = 0; j < cols.length; j++) {
				list.add(table.getModel().getValueAt(rows[i], cols[j]));
			}
		}
		return list.toArray();
	}

	public String addQueryStringToUri(ValList projection) {
		String tableName = dbTableName(getUri());
		ValMap schema = schema(tableName);
		ValList fields = schema.getList("name");
		Uri.Builder builder = getUri().buildUpon();
		for (Object field : projection) {
			int index = fields.indexOf(field);
			Object type = schema.getListValue("type", index);
			builder.appendQueryParameter(field.toString(), type.toString());
		}
		return builder.toString();
	}
}

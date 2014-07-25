package com.applang.components;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;
import java.util.Properties;

import javax.swing.AbstractCellEditor;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;

import org.gjt.sp.jedit.View;

import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.SharedPreferences;
import android.net.Uri;

import com.applang.PluginUtils;
import com.applang.SwingUtil.MapEditorComponent;
import com.applang.berichtsheft.BerichtsheftActivity;
import com.applang.berichtsheft.BerichtsheftApp;
import com.applang.berichtsheft.R;
import com.applang.berichtsheft.plugin.BerichtsheftPlugin;
import com.applang.components.DataView.ProjectionModel;

import static com.applang.SwingUtil.*;
import static com.applang.Util.*;
import static com.applang.Util1.*;
import static com.applang.Util2.*;
import static com.applang.PluginUtils.*;

@SuppressWarnings("rawtypes")
public class DataConfiguration
{
	public static final String TAG = DataConfiguration.class.getSimpleName();
	
	private Context mContext;
	
	public void setContext(Context context) {
		mContext = context;
		prefs = mContext.getSharedPreferences();
	}

	public Context getContext() {
		return mContext;
	}

	private Uri uri = null;
	public Uri getUri() {
		return uri;
	}

	public void setUri(Uri uri) {
		this.uri = uri;
		tableName = dbTableName(uri);
		path = mContext.getDatabasePath(uri);
		if (nullOrEmpty(uri))
			setProjectionModel(null);
		if (mProjectionModel != null && mProjectionModel.hasFlavor()) {
			mContext.registerFlavor(mProjectionModel.getFlavor(), path);
		}
	}

	private String path, tableName;
	
	public String getPath() {
		return dbPath(path);
	}

	public String getTableName() {
		return tableName;
	}

	private PropertyChangeSupport mPcs = new PropertyChangeSupport(this);

	public void
    addPropertyChangeListener(PropertyChangeListener listener) {
        mPcs.addPropertyChangeListener(listener);
    }
    
    public void
    removePropertyChangeListener(PropertyChangeListener listener) {
        mPcs.removePropertyChangeListener(listener);
    }
	
	private ProjectionModel mProjectionModel = null;
	
	public void setProjectionModel(ProjectionModel projectionModel) {
		if (mProjectionModel != null && mProjectionModel.hasFlavor()) {
			mContext.unregisterFlavor(mProjectionModel.getFlavor());
		}
		ProjectionModel oldProjectionModel = mProjectionModel;
		mProjectionModel = projectionModel;
		mPcs.firePropertyChange("projectionModel", oldProjectionModel, mProjectionModel);
	}

	public ProjectionModel getProjectionModel() {
		return mProjectionModel;
	}

	public String getFlavor() {
		return mProjectionModel != null ? mProjectionModel.getFlavor() : null;
	}

	public BidiMultiMap getProjection() {
		return mProjectionModel != null ? mProjectionModel.getProjection() : null;
	}

	public String getSortOrder() {
		return mProjectionModel != null ? mProjectionModel.getSortOrder() : null;
	}
	
	private ValMap options = vmap();

	public DataConfiguration(Context context, Uri uri, ProjectionModel model) {
		setContext(context);
		if (nullOrEmpty(uri))
			load();
		else
			setUri(uri);
		if (model != null)
			setProjectionModel(model);
		options.put("Layout", "standard");
	}
	
	public DataConfiguration(DataAdapter dataAdapter) {
		setContext(dataAdapter.getContext());
		String uriString = stringValueOf(uri);
		setUri(dataAdapter.makeUri(uriString));
		setProjectionModel(new ProjectionModel(dataAdapter));
	}

	private AlertDialog dialog;
	private JTextField entry;
	private JComboBox comboBox;
	private JTable[] tables = new JTable[2];
	
	public Boolean display(final View view, Object...params) {
		final boolean more = param_Boolean(prefs.getBoolean("dataConfig_more", Boolean.TRUE), 0, params);
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));
		panel.add(display_Flavor());
		panel.add(Box.createVerticalStrut(10));
		panel.add(display_DbFile(view));
		panel.add( Box.createVerticalStrut(10) );
		Dimension size = new Dimension(400, 110);
		JComponent component = DataView.dbTablesComponent(mContext, uri, onDoubleClick);
		tables[0] = findFirstComponent(component, "table");
		tables[0].setPreferredScrollableViewportSize(scaledDimension(size, 1.0, 0.8));
		panel.add(component);
		if (more) {
			component = DataView.projectionComponent(getContext(), dialog, mProjectionModel);
			tables[1] = findFirstComponent(component, "table");
			tables[1].setPreferredScrollableViewportSize(scaledDimension(size, 1.0, 1.2));
			panel.add(component);
			component = new MapEditorComponent(options, _null());
			panel.add(component);
		}
		tables[0].getSelectionModel().addListSelectionListener(
			new ListSelectionListener() {
				public void valueChanged(ListSelectionEvent e) {
					if (e.getValueIsAdjusting()) return;
					stopCellEditing();
					int sel = tables[0].getSelectedRow();
					if (sel > -1) {
						tableName = stringValueOf(tables[0].getModel().getValueAt(sel, 0));
						uri = dbTable(uri, tableName);
						setProjectionModel(new ProjectionModel(mContext, uri, mProjectionModel.getFlavor()));
						mProjectionModel.injectFlavor();
						if (more) {
							tables[1].setModel(mProjectionModel);
						}
					} else {
						tableName = null;
						if (tables[1] != null)
							tables[1].setModel(new DefaultTableModel());
					}
				}
			}
		);
		result = null;
		dialog = new AlertDialog.Builder(mContext)
				.setTitle("Data configuration")
				.setView(panel)
				.setPositiveButton(android.R.string.ok, new OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						finish();
						result = true;
					}
				})
				.setNeutralButton(more ? R.string.button_less : R.string.button_more, new OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						prefs.edit().putBoolean("dataConfig_more", !more);
						finish();
					}
				})
				.setNegativeButton(android.R.string.cancel, new OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						finish();
						result = false;
					}
				})
                .setOnCancelListener(new DialogInterface.OnCancelListener() {
					public void onCancel(DialogInterface dialog) {
						finish();
						result = false;
					}
                })
				.create();
		setLongText(entry, path);
		if (mProjectionModel == null)
			setProjectionModel(new ProjectionModel(mContext, getUri(), mContext.getFlavor()));
		comboBox.getModel().setSelectedItem(mProjectionModel.getFlavor());
		dialog.open();
		if (result == null)
			return display(view, !more);
		else
			return result;
	}

	private Box display_Flavor() {
		Box box = new Box(BoxLayout.X_AXIS);
		box.add(new JLabel("Flavor"));
		box.add(Box.createHorizontalStrut(10));
		comboBox = new JComboBox();
		DataView.fillFlavorCombo(comboBox);
		comboBox.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (mProjectionModel.hasFlavor())
					getContext().unregisterFlavor(mProjectionModel.getFlavor());
				String flavor = stringValueOf(comboBox.getSelectedItem());
				mProjectionModel.setFlavor(flavor);
				mProjectionModel.injectFlavor();
				path = entry.getText();
				if (mProjectionModel.hasFlavor()) {
					if (nullOrEmpty(path))
						path = mContext.getDatabasePath(databaseName(flavor)).getPath();
					getContext().registerFlavor(flavor, path);
				}
				setDbFile(new File(path), tableName);
			}
		});
		box.add(comboBox);
		setMaximumDimension(box, 100);
		return box;
	}

	private Box display_DbFile(final View view) {
		Box box = new Box(BoxLayout.X_AXIS);
		box.add(new JLabel("Database"));
		box.add(Box.createHorizontalStrut(10));
		entry = new JTextField(40);
		entry.addKeyListener(new KeyAdapter() {
			@Override
			public void keyReleased(KeyEvent e) {
				setDbFile(new File(entry.getText()), tableName);
			}
		});
		entry.addComponentListener(new ComponentAdapter() {
			@Override
			public void componentResized(ComponentEvent e) {
				setLongText(entry, entry.getText());
			}
		});
		box.add(entry);
		box.add(makeCustomButton("datadock.choose-db", new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				File dbFile = DataView.chooseDb(BerichtsheftPlugin.fileChooser(view), true, path, true);
				if (dbFile != null) {
					setLongText(entry, dbFile.getPath());
					String flavor = stringValueOf(comboBox.getSelectedItem());
					if (notNullOrEmpty(flavor))
						makeSureExists(flavor, dbFile);
					setDbFile(dbFile, null);
				}
				else
					alert(getProperty("dataview.sqlite-required.message"));
			}
		}, false));
		setMaximumDimension(box, 100);
		return box;
	}

	private void setDbFile(File dbFile, String tableName) {
		if (fileExists(dbFile) && isSQLite(dbFile)) {
			setUri(fileUri(dbFile.getPath(), tableName));
			setLongText(entry, path);
			tables[0].setModel(DataView.dbTablesModel(mContext, uri));
		}
		else if (flavoredUri()) {
			String flavor = mProjectionModel.getFlavor();
			setUri(contentUri(flavor, tableName));
			entry.setText("");
			tables[0].setModel(DataView.dbTablesModel(mContext, uri));
		}
		else {
			setUri(null);
			tables[0].setModel(new DefaultTableModel());
		}
		TableModel model = tables[0].getModel();
		for (int i = 0; i < model.getRowCount(); i++)
			if (model.getValueAt(i, 0).equals(tableName)) {
				tables[0].getSelectionModel().setSelectionInterval(i, i);
				return;
			}
		tables[0].getSelectionModel().setSelectionInterval(-1, -1);
	}

	private boolean flavoredUri() {
		return mProjectionModel.hasFlavor() && nullOrEmpty(entry.getText());
	}
	
	private Job<JTable> onDoubleClick = new Job<JTable>() {
		public void perform(JTable table, Object[] params) throws Exception {
			int sel = table.getSelectedRow();
			if (sel > -1) {
				tableName = stringValueOf(tables[0].getModel().getValueAt(sel, 0));
			}
			result = true;
			finish();
		}
	};
	
	private void finish() {
		stopCellEditing();
		dialog.dismiss();
		if (flavoredUri())
			uri = contentUri(mProjectionModel.getFlavor(), tableName);
		else
			uri = fileUri(path, tableName);
	}

	private void stopCellEditing() {
		if (tables[1] != null) {
			AbstractCellEditor ce = (AbstractCellEditor) tables[1].getCellEditor();
			if (ce != null)
				ce.stopCellEditing();
		}
	}
	
	private Boolean result = null;
	private SharedPreferences prefs;

	public void load() {
		String uriString = stringValueOf(prefs.getString("uri", ""));
		String database = stringValueOf(prefs.getString("database", ""));
		diag_println(DIAG_OFF, "loaded uri, database", uriString, database);
		if (isFileUri(uriString))
			database = Uri.parse(uriString).getPath();
		if (!fileExists(database)) {
			database = null;
		}
		projectionFromUri(Uri.parse(uriString), database);
	}

	public void projectionFromUri(Uri uri, String database) {
		String flavor = null;
		if (ContentResolver.SCHEME_CONTENT.equals(uri.getScheme())) {
			flavor = uri.getAuthority();
		}
		String query = uri.getQuery();
		if (query != null) {
			ValList names = vlist();
			ValList types = vlist();
			ValList conversions = vlist();
			ValList checks = vlist();
			ValList list = split(query, "&");
			for (int i = 0; i < list.size(); i++) {
				String[] parts = list.get(i).toString().split("=", 2);
				names.add(parts[0]);
				parts = parts[1].split("\\|", 3);
				types.add(parts[0]);
				conversions.add(parts[1]);
				checks.add(parts.length > 2 ? Boolean.parseBoolean(parts[2]) : true);
			}
			BidiMultiMap projection = new BidiMultiMap(names, conversions, types, checks);
			setProjectionModel(new ProjectionModel(mContext, uri, flavor, projection));
		}
		String tableName = dbTableName(uri);
		uri = uri.buildUpon().query(null).fragment(null).build();
		dbTable(uri, tableName);
		getContext().registerFlavor(flavor, database);
		setUri(uri);
	}

	public void save() {
		Uri uri = getUri();
		String database = mContext.getDatabasePath(uri);
		if (notNullOrEmpty(tableName)) {
			uri = dbTable(uri, tableName);
			if (mProjectionModel != null) 
				uri = projectionToUri(uri);
		}
		String uriString = uri != null ? Uri.decode(uri.toString()) : null;
		prefs.edit().putString("uri", uriString).putString("database", database).commit();
		diag_println(DIAG_OFF, "saved uri, database", uriString, database);
	}

	public Uri projectionToUri(Uri uri) {
		Uri.Builder builder = uri.buildUpon().query("");
		if (mProjectionModel.hasFlavor()) {
			builder
				.scheme(ContentResolver.SCHEME_CONTENT)
    			.authority(mProjectionModel.getFlavor())
    			.path(dbTableName(uri));
		}
		BidiMultiMap projection = mProjectionModel.getExpandedProjection();
		ValList names = projection.getKeys();
		ValList conversions = projection.getValues(1);
		ValList types = projection.getValues(2);
		ValList checks = projection.getValues(3);
		for (int i = 0; i < names.size(); i++) {
			builder.appendQueryParameter(
					stringValueOf(names.get(i)),
					join("|", objects(
							types.get(i), 
							stringValueOf(conversions.get(i)), 
							stringValueOf(checks.get(i))
							)));
		}
		uri = builder.build();
		return uri;
	}
	
	//	NOTE	used in scripts
	public static String inquireUri(String uriString, boolean full) {
		DataConfiguration dc = 
			new DataConfiguration(BerichtsheftActivity.getInstance(), 
					Uri.parse(stringValueOf(uriString)), 
					null);
		if (dc.display(null, full)) 
			return stringValueOf(dc.getUri());
		else
			return null;
	}
	
	public static boolean dataProperties(View view, Properties props) {
		Uri uri = Uri.parse(props.getProperty("uri", ""));
		DataConfiguration dc = new DataConfiguration(BerichtsheftActivity.getInstance(view), uri, null);
		if (dc.display(view, true)) {
			props.setProperty("uri", stringValueOf(dc.getUri()));
			return true;
		}
		return false;
	}
	/**
	 * @param args
	 */
	public static void main(String...args) {
		BerichtsheftApp.loadSettings();
		DataConfiguration dc = new DataConfiguration(BerichtsheftActivity.getInstance(), null, null);
		do {
			if (dc.display(null)) 
				dc.save();
			else
				break;
			ProjectionModel model = dc.getProjectionModel();
			println("%s\n%s\n%s\n%s\n%s\n", dc.getUri(), dc.getPath(), dc.getTableName(), model, model.getFlavor());
		} 
		while (true);
		System.exit(0);
	}

}

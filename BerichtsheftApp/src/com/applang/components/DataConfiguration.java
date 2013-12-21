package com.applang.components;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.File;

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
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.SharedPreferences;
import android.net.Uri;

import com.applang.berichtsheft.BerichtsheftActivity;
import com.applang.berichtsheft.BerichtsheftApp;
import com.applang.berichtsheft.R;
import com.applang.berichtsheft.plugin.BerichtsheftPlugin;
import com.applang.components.DataView.ProjectionModel;

import static com.applang.SwingUtil.*;
import static com.applang.Util.*;
import static com.applang.Util1.*;
import static com.applang.Util2.*;

@SuppressWarnings("rawtypes")
public class DataConfiguration
{
	private static final String FLAVOR_PARAMETER_NAME = "[]";

	public static final String TAG = DataConfiguration.class.getSimpleName();
	
	private Context context;
	public void setContext(Context context) {
		this.context = context;
		prefs = this.context.getSharedPreferences(null, Context.MODE_PRIVATE);
	}

	public Context getContext() {
		return context;
	}

	private Uri uri = null;
	public Uri getUri() {
		return uri;
	}

	public void setUri(Uri uri) {
		this.uri = uri;
		tableName = dbTableName(uri);
		path = context.getDatabasePath(uri);
	}

	private String path, tableName;
	
	public String getPath() {
		return path;
	}

	public String getTableName() {
		return tableName;
	}

	private ProjectionModel projectionModel = null;
	public void setProjectionModel(ProjectionModel projectionModel) {
		this.projectionModel = projectionModel;
	}

	public ProjectionModel getProjectionModel() {
		return projectionModel;
	}

	public DataConfiguration(Context context, Uri uri, ProjectionModel model) {
		setContext(context);
		if (nullOrEmpty(uri))
			load();
		else
			setUri(uri);
		if (model != null)
			setProjectionModel(model);
		else if (projectionModel == null)
			setProjectionModel(new ProjectionModel(context, getUri(), ""));
	}
	
	public DataConfiguration(Provider provider) {
		setContext(provider.getContext());
		String uriString = stringValueOf(uri);
		setUri(provider.makeUri(uriString));
		setProjectionModel(new ProjectionModel(provider));
	}

	private AlertDialog dialog;
	private JTextField entry;
	private JComboBox comboBox;
	private JTable[] tables = new JTable[2];
	
	public Boolean display(final View view, Object...params) {
		final boolean more = param_Boolean(prefs.getBoolean("dataConfig_more", Boolean.TRUE), 0, params);
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));
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
		box.add(BerichtsheftPlugin.makeCustomButton("datadock.choose-db", new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				File dbFile = DataView.chooseDb(BerichtsheftPlugin.fileChooser(view), true, path, true);
				if (dbFile != null) {
					setLongText(entry, dbFile.getPath());
					setDbFile(dbFile, null);
				}
				else
					alert(BerichtsheftPlugin.getProperty("dataview.sqlite-required.message"));
			}
		}, false));
		setMaximumDimension(box, 100);
		panel.add(box);
		panel.add( Box.createVerticalStrut(10) );
		if (more || !more) {
			box = new Box(BoxLayout.X_AXIS);
			box.add(new JLabel("Flavor"));
			box.add(Box.createHorizontalStrut(10));
			comboBox = new JComboBox();
			DataView.fillFlavorCombo(comboBox);
			comboBox.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					String flavor = stringValueOf(comboBox.getSelectedItem());
					projectionModel.setFlavor(flavor);
					projectionModel.injectFlavor();
					setDbFile(new File(entry.getText()), tableName);
				}
			});
			box.add(comboBox);
			setMaximumDimension(box, 100);
			panel.add(box);
			panel.add(Box.createVerticalStrut(10));
		}
		Dimension size = new Dimension(400, 110);
		JComponent component = DataView.dbTablesComponent(context, uri, onDoubleClick);
		tables[0] = findComponent(component, "table");
		tables[0].setPreferredScrollableViewportSize(scaledDimension(size, 1.0, 0.8));
		panel.add(component);
		if (more) {
			component = DataView.projectionComponent(dialog, projectionModel);
			tables[1] = findComponent(component, "table");
			tables[1].setPreferredScrollableViewportSize(scaledDimension(size, 1.0, 1.2));
			panel.add(component);
		}
		setDbFile(null, tableName);
		tables[0].getSelectionModel().addListSelectionListener(
			new ListSelectionListener() {
				public void valueChanged(ListSelectionEvent e) {
					if (e.getValueIsAdjusting()) return;
					stopCellEditing();
					int sel = tables[0].getSelectedRow();
					if (sel > -1) {
						tableName = stringValueOf(tables[0].getModel().getValueAt(sel, 0));
						uri = dbTable(uri, tableName);
						setProjectionModel(new ProjectionModel(context, uri, projectionModel.getFlavor()));
						projectionModel.injectFlavor();
						if (more) {
							tables[1].setModel(projectionModel);
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
		dialog = new AlertDialog.Builder(context)
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
		comboBox.getModel().setSelectedItem(projectionModel.getFlavor());
		dialog.open();
		if (result == null)
			return display(view, !more);
		else
			return result;
	}

	private void setDbFile(File dbFile, String tableName) {
		if (fileExists(dbFile) && isSQLite(dbFile)) {
			setUri(fileUri(dbFile.getPath(), tableName));
			setLongText(entry, path);
			tables[0].setModel(DataView.dbTablesModel(context, uri));
		}
		else if (flavoredUri()) {
			String flavor = projectionModel.getFlavor();
			setUri(contentUri(flavor, tableName));
			entry.setText("");
			tables[0].setModel(DataView.dbTablesModel(context, uri));
		}
		else if (dbFile != null) {
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
		return projectionModel.hasFlavor() && nullOrEmpty(entry.getText());
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
			uri = contentUri(projectionModel.getFlavor(), tableName);
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
		projectionFromUri(Uri.parse(uriString));
	}

	private void projectionFromUri(Uri uri) {
		String query = uri.getQuery();
		if (query != null) {
			String flavor = null;
			ValList names = vlist();
			ValList convs = vlist();
			ValList types = vlist();
			ValList list = split(query, "&");
			for (int i = 0; i < list.size(); i++) {
				String[] parts = list.get(i).toString().split("=", 2);
				if (FLAVOR_PARAMETER_NAME.equals(parts[0]))
					flavor = parts[1];
				else {
					names.add(parts[0]);
					parts = parts[1].split("\\|", 2);
					types.add(parts[0]);
					convs.add(parts[1]);
				}
			}
			setUri(uri.buildUpon().query(null).build());
			BidiMultiMap projection = new BidiMultiMap(names, convs, types);
			setProjectionModel(new ProjectionModel(context, uri, flavor, projection));
		}
		else
			setUri(uri);
	}

	public void save() {
		Uri uri = getUri();
		if (notNullOrEmpty(tableName)) {
			uri = dbTable(uri, tableName);
			if (projectionModel != null) 
				uri = projectionToUri(uri);
		}
		String uriString = uri != null ? Uri.decode(uri.toString()) : null;
		prefs.edit().putString("uri", uriString).commit();
	}

	private Uri projectionToUri(Uri uri) {
		Uri.Builder builder = uri.buildUpon().query("");
		if (projectionModel.hasFlavor())
			builder.appendQueryParameter(
					FLAVOR_PARAMETER_NAME,
					stringValueOf(projectionModel.getFlavor()));
		BidiMultiMap projection = projectionModel.getProjection();
		ValList names = projection.getKeys();
		ValList convs = projection.getValues(1);
		ValList types = projection.getValues(2);
		for (int i = 0; i < names.size(); i++) {
			builder.appendQueryParameter(
					stringValueOf(names.get(i)),
					join("|", objects(types.get(i), stringValueOf(convs.get(i)))));
		}
		uri = builder.build();
		return uri;
	}
	
	//	NOTE	used in scripts
	public static String inquireUri(String uriString, boolean full) {
		DataConfiguration dc = 
			new DataConfiguration(new BerichtsheftActivity(), 
					Uri.parse(stringValueOf(uriString)), 
					null);
		if (dc.display(null, full)) 
			return stringValueOf(dc.getUri());
		else
			return null;
	}
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		BerichtsheftApp.loadSettings();
		DataConfiguration dc = new DataConfiguration(new BerichtsheftActivity(), null, null);
		do {
			if (dc.display(null)) 
				dc.save();
			else
				break;
			ProjectionModel model = dc.getProjectionModel();
			println("%s\n%s\n%s\n%s\n%s\n", dc.getUri(), dc.getPath(), dc.getTableName(), model, model.getFlavor());
		} while (true);
		System.exit(0);
	}

}

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
import android.net.Uri;
import android.util.Log;

import com.applang.Util.BidiMultiMap;
import com.applang.Util.ValList;
import com.applang.Util2.Settings;
import com.applang.berichtsheft.BerichtsheftApp;
import com.applang.berichtsheft.R;
import com.applang.berichtsheft.plugin.BerichtsheftPlugin;
import com.applang.components.DataView.ProjectionModel;
import com.applang.components.DataView.Provider;

import static com.applang.SwingUtil.*;
import static com.applang.Util.*;
import static com.applang.Util1.*;
import static com.applang.Util2.getSetting;
import static com.applang.Util2.putSetting;

@SuppressWarnings("rawtypes")
public class DataConfiguration
{
	public static final String TAG = DataConfiguration.class.getSimpleName();
	
	private Context context;
	public Context getContext() {
		return context;
	}

	private Uri uri;
	public void setUri(Uri uri) {
		this.uri = uri;
		tableName = dbTableName(uri);
		path = context.getDatabasePath(uri);
		setProjectionModel(null);
	}

	public Uri getUri() {
		return uri;
	}

	private ProjectionModel projectionModel = null;
	public void setProjectionModel(ProjectionModel projectionModel) {
		this.projectionModel = projectionModel;
	}

	public ProjectionModel getProjectionModel() {
		return projectionModel;
	}

	private String path, tableName;
	
	public String getPath() {
		return path;
	}

	public String getTableName() {
		return tableName;
	}

	public DataConfiguration(Context context, Uri uri, ProjectionModel model) {
		this.context = context;
		setUri(uri);
		if (nullOrEmpty(uri))
			load();
		if (model != null)
			projectionModel = model;
		else if (projectionModel == null)
			projectionModel = new ProjectionModel(context, getUri(), "");
	}
	
	public DataConfiguration(Provider provider) {
		if (notNullOrEmpty(uri))
			save();
		context = provider.getContext();
		uri = provider.makeUri(uri.toString());
		tableName = provider.getTableName();
		path = context.getDatabasePath(uri);
		projectionModel = new ProjectionModel(provider);
	}

	private AlertDialog dialog;
	private JTextField entry;
	private JComboBox comboBox;
	private JTable[] tables = new JTable[2];
	
	public boolean display(final boolean full, final View view) {
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
		if (full) {
			box = new Box(BoxLayout.X_AXIS);
			box.add(new JLabel("Flavor"));
			box.add(Box.createHorizontalStrut(10));
			comboBox = new JComboBox();
			DataView.fillFlavorCombo(comboBox);
			comboBox.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					projectionModel.setFlavor(stringValueOf(comboBox.getSelectedItem()));
					projectionModel.injectFlavor();
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
		setDbFile(null, tableName);
		panel.add(component);
		if (full) {
			component = DataView.projectionComponent(dialog, projectionModel);
			tables[1] = findComponent(component, "table");
			tables[1].setPreferredScrollableViewportSize(scaledDimension(size, 1.0, 1.2));
			panel.add(component);
		}
		tables[0].getSelectionModel().addListSelectionListener(
			new ListSelectionListener() {
				public void valueChanged(ListSelectionEvent e) {
					if (e.getValueIsAdjusting()) return;
					int sel = tables[0].getSelectedRow();
					if (sel > -1) {
						tableName = stringValueOf(tables[0].getModel().getValueAt(sel, 0));
						uri = fileUri(path, tableName);
						if (full) {
							projectionModel = new ProjectionModel(context, uri, projectionModel.getFlavor());
							projectionModel.injectFlavor();
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
		dialog = new AlertDialog.Builder(context)
				.setTitle("Data configuration")
				.setView(panel)
				.setPositiveButton(R.string.button_ok, new OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						dismiss();
						result = true;
					}
				})
				.setNegativeButton(R.string.button_cancel, new OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						dismiss();
						result = false;
					}
				})
				.create();
		setLongText(entry, path);
		if (projectionModel != null) 
			comboBox.getModel().setSelectedItem(projectionModel.getFlavor());
		dialog.open();
		return result;
	}

	private void setDbFile(File dbFile, String tableName) {
		if (fileExists(dbFile) && isSQLite(dbFile)) {
			path = dbFile.getPath();
			setLongText(entry, path);
			uri = fileUri(path, null);
			tables[0].setModel(DataView.dbTablesModel(context, uri));
		}
		else if (dbFile != null) {
			tables[0].setModel(new DefaultTableModel());
			uri = null;
		}
		TableModel model = tables[0].getModel();
		for (int i = 0; i < model.getRowCount(); i++)
			if (model.getValueAt(i, 0).equals(tableName)) {
				tables[0].getSelectionModel().setSelectionInterval(i, i);
				return;
			}
		tables[0].getSelectionModel().setSelectionInterval(-1, -1);
	}
	
	private Job<JTable> onDoubleClick = new Job<JTable>() {
		public void perform(JTable table, Object[] params) throws Exception {
			int sel = table.getSelectedRow();
			if (sel > -1) {
				tableName = stringValueOf(tables[0].getModel().getValueAt(sel, 0));
			}
			result = true;
			dismiss();
		}
	};
	
	private void dismiss() {
		dialog.dismiss();
		if (tables[1] != null) {
			AbstractCellEditor ce = (AbstractCellEditor) tables[1].getCellEditor();
			if (ce != null)
				ce.stopCellEditing();
		}
		uri = fileUri(path, tableName);
	}
	
	private boolean result = false;

	public void load() {
		String uriString = getSetting("uri", "");
		if (notNullOrEmpty(uriString)) {
			setUri(Uri.parse(uriString));
			projectionFromUri();
		}
	}

	private void projectionFromUri() {
		String query = uri.getQuery();
		if (query != null) {
			String flavor = null;
			ValList names = vlist();
			ValList convs = vlist();
			ValList types = vlist();
			ValList list = split(query, "&");
			for (int i = 0; i < list.size(); i++) {
				String[] parts = list.get(i).toString().split("=", 2);
				if ("_".equals(parts[0]))
					flavor = parts[1];
				else {
					names.add(parts[0]);
					parts = parts[1].split("\\|", 2);
					types.add(parts[0]);
					convs.add(parts[1]);
				}
			}
			uri = uri.buildUpon().query(null).build();
			BidiMultiMap projection = new BidiMultiMap(names, convs, types);
			projectionModel = new ProjectionModel(context, uri, flavor, projection);
		}
	}

	public void save() {
		Uri uri = getUri();
		if (notNullOrEmpty(uri)) {
			uri = dbTable(uri, tableName);
			if (notNullOrEmpty(tableName) && projectionModel != null) {
				uri = projectionToUri(uri);
			}
		}
		String uriString = stringValueOf(uri);
		putSetting("uri", uriString);
		Settings.save();
	}

	private Uri projectionToUri(Uri uri) {
		Uri.Builder builder = uri.buildUpon().query("");
		builder.appendQueryParameter("_",
				stringValueOf(projectionModel.getFlavor()));
		BidiMultiMap projection = projectionModel.getProjection();
		ValList names = projection.getKeys();
		ValList convs = projection.getValues(1);
		ValList types = projection.getValues(2);
		for (int i = 0; i < names.size(); i++) {
			String value = stringValueOf(types.get(i)) + "|"
					+ stringValueOf(convs.get(i));
			builder.appendQueryParameter(stringValueOf(names.get(i)),
					value);
		}
		uri = builder.build();
		return uri;
	}
	
	public static String inquireUri(String uriString) {
		DataConfiguration dc = 
			new DataConfiguration(BerichtsheftApp.getActivity(), 
					uriString == null ? null : Uri.parse(uriString), 
					null);
		if (dc.display(false, null)) 
			return stringValueOf(dc.getUri());
		else
			return null;
	}
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Log.logConsoleHandling(Log.DEBUG);
		BerichtsheftApp.loadSettings();
		DataConfiguration dc = new DataConfiguration(BerichtsheftApp.getActivity(), null, null);
		do {
			if (dc.display(true, null)) 
				dc.save();
			else
				break;
		} while (true);
		System.exit(0);
	}

}

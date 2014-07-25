package com.applang.components;

import java.awt.Dimension;
import java.io.File;
import java.util.ArrayList;

import javax.swing.JScrollPane;
import javax.swing.JTable;

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
import android.database.Cursor;
import android.net.Uri;

import com.applang.berichtsheft.BerichtsheftActivity;
import com.applang.berichtsheft.plugin.BerichtsheftPlugin;
import com.applang.components.DataView.DataModel;

import static com.applang.Util.*;
import static com.applang.Util1.*;
import static com.applang.Util2.*;
import static com.applang.SwingUtil.*;
import static com.applang.PluginUtils.*;

public class DataAdapter
{
	private Context context;
	public Context getContext() {
		return context;
	}

	private ContentResolver contentResolver;
	
	public DataAdapter(DataView dv) {
		this(dv.getUriString());
	}
	
	public DataAdapter(String uriString) {
		this(BerichtsheftActivity.getInstance(), uriString);
	}
	
	public DataAdapter(String flavor, final File dbFile, String uriString) {
		this(Context.contextForFlavor(BerichtsheftActivity.packageName, flavor, dbFile), uriString);
	}
	
	private DataAdapter(Context context, String uriString) {
		this.context = context;
		contentResolver = context.getContentResolver();
		tableName = dbTableName(uriString);
		info = table_info2(
				context, 
				Uri.parse(uriString), 
				tableName, 
				context.getFlavor());
	}
	
	private String tableName;
	public String getTableName() {
		return tableName;
	}

	public ValMap info;
	
	public String getFlavor() {
		return context.getFlavor();
	}

	public Uri makeUri(String uriString) {
		uriString = stringValueOf(uriString);
		String flavor = getFlavor();
		Uri uri = Uri.parse(uriString);
		if (flavor == null)
			return uri;
		else {
			Uri curi = contentUri(flavor, tableName);
			Long id = parseId(null, uri);
			if (id != null)
				return ContentUris.appendId(curi.buildUpon(), id).build();
			else
				return curi;
		}
	}
	
	public Object[][] query(String uriString, String[] columns, Object...params) {
		Uri uri = makeUri(uriString);
		String selection = param(null, 0, params);
		String[] selectionArgs = param(null, 1, params);
		String sortOrder = param(null, 2, params);
		try {
			final ArrayList<Object[]> rows = alist();
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
			BerichtsheftPlugin.consoleMessage("dataview.query.message", stringValueOf(uri), e.getMessage());
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
					toStrings(projection.getKeys()), 
					selection, selectionArgs, 
					sortOrder);
			return new DataModel()
					.setProjection(projection)
					.traverse(cursor);
		} catch (Exception e) {
			BerichtsheftPlugin.consoleMessage("dataview.query.message", stringValueOf(uri), e.getMessage());
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
					table.getSelectionModel().setSelectionInterval(1, 1);
					return decision = new AlertDialog(view, 
							getProperty("dataview.prompt-update.message"), 
							"", 
							scrollableViewport(table, new Dimension(800,100)), 
							5, 
							AlertDialog.behavior, 
							null, null).open().getResult();
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
						stringValueOf(primaryKeyColumnName), 
						values,
						param((Function<Integer>)null, 0, params));
				if (retval instanceof Boolean)
					found = (Boolean) retval;
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
			BerichtsheftPlugin.consoleMessage("dataview.updateOrInsert.message.1", stringValueOf(uri), e.getMessage());
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
					results[1] += (Integer) result;
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
				if ((Integer)result < 1)
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
package com.applang;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONStringer;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Bitmap.CompressFormat;
import android.net.Uri;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import static com.applang.Util.*;
import static com.applang.Util2.*;

public class Util1
{
	public static boolean traverse(Cursor cursor, Job<Cursor> job, Object...params) {
		if (cursor == null)
			return false;
		try {
			boolean retval = false;
	    	if (cursor.moveToFirst()) 
	    		do {
	    			if (job != null)
	    				job.perform(cursor, params);
					retval = true;
	    		} while (cursor.moveToNext());
	    	return retval;
		} catch (Exception e) {
            Log.e(TAG, "traversing cursor", e);
            return false;
		}
		finally {
			cursor.close();
		}
    }
	
	public static ValMap getResults(Cursor cursor, final Function<String> key, final Function<Object> value) {
		final ValMap map = vmap();
		traverse(cursor, new Job<Cursor>() {
			public void perform(Cursor c, Object[] params) throws Exception {
				String k = key.apply(c);
				Object v = value.apply(c);
				map.put(k, v);
			}
		});
		return map;
	}

	public static ValList getStrings(Cursor cursor) {
		ValList list = vlist();
		for (int i = 0; i < cursor.getColumnCount(); i++) {
			list.add(cursor.getString(i));
		}
		return list;
	}

	public static LinearLayout linearLayout(Context context, int orientation, int width, int height) {
	    LinearLayout linear = new LinearLayout(context);
	    linear.setOrientation(orientation);
		linear.setLayoutParams(new LinearLayout.LayoutParams(width, height));
		return linear;
	}

	public static RelativeLayout relativeLayout(Context context, int width, int height) {
		RelativeLayout relative = new RelativeLayout(context);
	    relative.setLayoutParams(new RelativeLayout.LayoutParams(width, height));
		return relative;
	}

	public static ViewGroup.MarginLayoutParams marginLayoutParams(int width, int height, Integer... ltrb) {
		ViewGroup.MarginLayoutParams layoutParams = new ViewGroup.MarginLayoutParams(width, height);
		layoutParams.setMargins(param(0, 0, ltrb), param(0, 1, ltrb), param(0, 2, ltrb), param(0, 3, ltrb));
		return layoutParams;
	}

	public static Object[] iterateViews(ViewGroup viewGroup, int indent, Function<Object[]> func, Object... params) {
		if (viewGroup != null) {
			for (int i = 0; i < viewGroup.getChildCount(); i++) {
				View view = viewGroup.getChildAt(i);
				params = func.apply(view, indent, params);
				if (view instanceof ViewGroup) {
					iterateViews((ViewGroup) view, indent + 1, func, params);
				}
			}
		}
		return params;
	}

	public static String viewLine(View v, int indent) {
		String string = v.getTag() + " : " + v.getClass().getSimpleName();
		if (v instanceof LinearLayout) {
			LinearLayout l = (LinearLayout) v;
			string += TAB + 
				(l.getOrientation() == LinearLayout.HORIZONTAL ? 
					"horizontal" : "vertical");
		}
		string += TAB + v.getLayoutParams();
		return indentedLine(string, TAB, indent);
	}

	public static String viewHierarchy(ViewGroup container) {
		String s = viewLine(container, 0);
		Object[] params = iterateViews(container, 
			1, 
			new Function<Object[]>() {
				public Object[] apply(Object... params) {
					View v = param(null, 0, params);
					int indent = param_Integer(null, 1, params);
					Object[] parms = param(null, 2, params);
					String s = (String) parms[0];
					s += viewLine(v, indent);
					parms[0] = s;
					return parms;
				}
			}, 
			objects(s)
		);
		return param_String("", 0, params);
	}

	public static View getContentView(Activity activity) {
		View rootView = activity.findViewById(android.R.id.content);
		ViewGroup viewGroup = (ViewGroup)rootView;
		return viewGroup.getChildAt(0);
	}

	public static String[] providerPackages = strings("com.applang.provider");
	
	public static Predicate<String> isProvider = new Predicate<String>() {
		public boolean apply(String t) {
			for (int i = 0; i < providerPackages.length; i++) {
				if (t.startsWith(providerPackages[i]))
					return true;
			}
			return false;
		}
	};

	@SuppressWarnings("rawtypes")
	public static ValList contentAuthorities(String[] packageNames, Object...params) {
		ValList list = vlist();
		try {
			for (String packageName : packageNames) {
				Class[] cls = getLocalClasses(packageName, params);
				for (Class c : filter(asList(cls), false, new Predicate<Class>() {
					public boolean apply(Class c) {
						String name = c.getName();
						return !name.contains("$")
								&& !name.endsWith("Provider");
					}
				})) {
					Object name = c.getDeclaredField("AUTHORITY").get(null);
					if (name != null)
						list.add(name.toString());
				}
			}
		} catch (Exception e) {
			Log.e(TAG, "contentAuthorities", e);
			list.addAll(asList(strings(
					"com.applang.provider.NotePad", 
					"com.applang.provider.WeatherInfo", 
					"com.applang.provider.PlantInfo")));
		}
	    return list;
	}

	public static Object[] fullProjection(Object flavor) {
		try {
			Class<?> cls = Class.forName(flavor + "Provider");
			Field field = cls.getDeclaredField("FULL_PROJECTION");
			if (field == null)
				return null;
			else
				return (Object[]) field.get(null);
		} catch (Exception e) {
			Log.e(TAG, "fullProjection", e);
			return null;
		}
	}

	public static Uri contentUri(String flavor, String path) {
    	Uri uri = new Uri.Builder()
    		.scheme(ContentResolver.SCHEME_CONTENT)
    		.authority(flavor)
    		.path(path)
    		.build();
    	return toStringUri(uri);
    }
	
	public static boolean isContentUri(String uriString) {
		return uriString.toLowerCase().startsWith(ContentResolver.SCHEME_CONTENT);
	}

	public static Uri fileUri(String path, String fragment) {
    	return Uri.parse(path).buildUpon()
    		.scheme(ContentResolver.SCHEME_FILE)
    		.fragment(fragment)
    		.build();
    }
	
	public static boolean isFileUri(String uriString) {
		return uriString.toLowerCase().startsWith(ContentResolver.SCHEME_FILE);
	}
	
	public static boolean isJarUri(String uriString) {
		return uriString.toLowerCase().startsWith("jar:");
	}
	
	public static Uri toStringUri(Uri uri) {
		return Uri.parse(uri.toString());
	}
	
	public static String appendId(String uriString, long id) {
		return ContentUris.appendId(Uri.parse(uriString).buildUpon(), id).build().toString();
	}

	public static Long parseId(Long defaultValue, Uri uri) {
		try {
			return ContentUris.parseId(uri);
		} catch (Exception e) {
			return defaultValue;
		}
	}

	public static String codeUri(String uriString, boolean decode) {
		return decode ? Uri.decode(uriString) : Uri.encode(uriString);
	}

	public static boolean hasAuthority(Uri uri) {
		return uri != null && notNullOrEmpty(uri.getAuthority());
	}

	public static Uri dbTable(Uri uri, String tableName) {
		if (uri == null) return null;
		Uri.Builder builder = uri.buildUpon();
		if (hasAuthority(uri))
			builder = builder.path(tableName);
		else
			builder = builder.fragment(tableName);
		return builder.build();
	}
	
	public static String dbLegalize(Object name) {
		if (notNullOrEmpty(name))
			return stringValueOf(name).replaceAll("[ \\?\\*]", "_");
		else
			throw new RuntimeException("null or empty name not possible");
	}
	
	public static Uri dbTable(String uriString, String tableName) {
		return dbTable(Uri.parse(uriString), tableName);
	}

	public static String dbTableName(Uri uri) {
		if (uri == null) 
			return null;
		else if (hasAuthority(uri)) {
	        boolean hasPath = notNullOrEmpty(uri.getPath());
	        return hasPath ? uri.getPathSegments().get(0) : "";
		}
		else
			return uri.getFragment();
	}
	
	public static String dbTableName(String uriString) {
		return dbTableName(Uri.parse(uriString));
	}
	
	public static String dbPath(String uriString) {
		return Uri.parse(stringValueOf(uriString)).getPath();
	}
	
	public static ValMap schema(Context context, Uri uri) {
    	uri = dbTable(uri, null);
		Cursor cursor = context.getContentResolver().query(
				uri, 
				null, 
				"select name,sql from sqlite_master where type = 'table'", 
				null, 
				null);
		final ValMap schema = vmap();
		traverse(cursor, new Job<Cursor>() {
			public void perform(Cursor c, Object[] params) throws Exception {
				schema.put(c.getString(0), c.getString(1));
			}
		});
		return schema;
	}
	
	public static ValMap table_info(Context context, Uri uri, String tableName) {
		final ValMap info = vmap();
		if (uri == null || nullOrEmpty(tableName))
			return info;
    	uri = dbTable(uri, null);
		ContentResolver contentResolver = context.getContentResolver();
		Cursor cursor = contentResolver.query(
				uri, 
				null, 
				String.format("pragma table_info(%s)", tableName),  
				null, 
				null);
		info.getList("cid");
		info.getList("name");
		info.getList("type");
		info.getList("notnull");
		info.getList("dflt_value");
		info.getList("pk");
		traverse(cursor, new Job<Cursor>() {
			public void perform(Cursor c, Object[] params) throws Exception {
				for (int i = 0; i < c.getColumnCount(); i++) {
					String columnName = c.getColumnName(i);
					ValList column = (ValList) info.get(columnName);
					if ("cid".equals(columnName) || "notnull".equals(columnName) || "pk".equals(columnName))
						column.add(c.getInt(i));
					else
						column.add(c.getString(i));
				}
			}
		});
		ValList pk = info.getList("pk");
		List<Integer> indices = filterIndex(pk, false, new Predicate<Object>() {
			public boolean apply(Object t) {
				return Integer.valueOf(t.toString()) > 0;
			}
		});
		if (indices.size() == 1) {
			pk.set(indices.get(0), -Integer.valueOf(pk.get(indices.get(0)).toString()));
		}
		ValList fieldNames = info.getList("name");
		indices = filterIndex(pk, false, new Predicate<Object>() {
			public boolean apply(Object t) {
				return Integer.valueOf(t.toString()) < 0;
			}
		});
		if (indices.size() == 1) {
			info.put("PRIMARY_KEY", fieldNames.get(indices.get(0)));
		}
		return info;
	}

    public static int getFlavorVersion(String flavor, SQLiteDatabase db) {
    	Object[] params = {00};
    	traverse(getMetadata(db, flavor), new Job<Cursor>() {
			public void perform(Cursor c, Object[] parms) throws Exception {
				int index = c.getColumnIndex("version");
				parms[0] = c.getInt(index);
			}
    	}, params);
    	Integer version = (Integer) params[0];
		return version;
    }
    
    private static Cursor getMetadata(SQLiteDatabase db, String flavor) {
    	db.execSQL("create table if not exists metadata (flavor text, version integer);");
    	return db.rawQuery("select * from metadata where flavor=?;", strings(flavor));
    }

    public static void setFlavorVersion(String flavor, SQLiteDatabase db, int version) {
    	if (flavor == null)
    		db.setVersion(version);
    	ContentValues values = new ContentValues();
    	values.put("version", version);
    	if (getFlavorVersion(flavor, db) > 0) 
    		db.update("metadata", values, "flavor=?", strings(flavor));
    	else {
    		values.put("flavor", flavor);
    		db.insert("metadata", null, values);
    	}
	}
	
	public static int version(Context context, Uri uri) {
    	uri = dbTable(uri, null);
		Cursor cursor = context.getContentResolver().query(
				uri, 
				null, 
				"pragma user_version", 
				null, 
				null);
        if (cursor != null && cursor.moveToFirst())
        	return cursor.getInt(0);
        else
        	return 0;
	}
    
    public static ValList tables(Context context, Uri uri) {
    	final ValList tables = vlist();
    	if (uri != null && (!isFileUri(uri.toString()) || notNullOrEmpty(uri.getPath()))) {
			uri = dbTable(uri, null);
			Cursor cursor = context.getContentResolver().query(uri, null,
					"select name from sqlite_master where type = 'table'",
					null, null);
			traverse(cursor, new Job<Cursor>() {
				public void perform(Cursor c, Object[] params) throws Exception {
					tables.add(c.getString(0));
				}
			});
		}
		return tables;
	}
    
    public static boolean table_exists(SQLiteDatabase db, String tableName) {
    	Object[] params = objects(false, tableName);
    	traverse(db.rawQuery("select name from sqlite_master where type = 'table'", null), 
    		new Job<Cursor>() {
				public void perform(Cursor c, Object[] parms) throws Exception {
					String name = param_String("", 1, parms);
	        		if (name.compareToIgnoreCase(c.getString(0)) == 0) {
	        			parms[0] = true;
	        		}
				}
			}, params);
	    return param_Boolean(null, 0, params);
    }
    
    public static boolean table_upgrade(final SQLiteDatabase db, final String tableName, 
    		final Job<Void> creation, final Object...params)
    {
    	boolean retval = false;
        try {
        	if (table_exists(db, tableName)) {
        		db.execSQL("ALTER TABLE " + tableName + " RENAME TO temp_" + tableName);
        		Cursor c = db.rawQuery("select * from temp_" + tableName, null);
        		Object[] parms = {retval};
            	traverse(c, new Job<Cursor>() {
        			public void perform(Cursor c, Object[] parms) throws Exception {
        				ValList list = vlist();
        				for (int i = 0; i < c.getColumnCount(); i++) {
        					list.add(c.getColumnName(i));
        				}
        				creation.perform(null, params);
        				String cols = join(",", list.toArray());
        				db.execSQL(String.format( 
        						"INSERT INTO %s (%s) SELECT %s from temp_%s", 
        						tableName, cols, cols, tableName));
        				parms[0] = true;
        			}
            	}, parms);
            	retval = (Boolean) parms[0];
        		db.execSQL("DROP TABLE temp_" + tableName);
        	}
        	if (!retval) {
        		creation.perform(null, params);
    			retval = true;
        	}
	    }
	    catch (Exception e) {
	    	Log.e(TAG, "upgrade failed", e);
	    	retval = false;
	    }
        return retval;
    }

    public static int recordCount(Context context, Uri uri) {
		String tableName = dbTableName(uri);
    	uri = dbTable(uri, null);
		Cursor cursor = context.getContentResolver().query(
				uri, 
				null, 
				"select count(*) from " + tableName, 
				null, 
				null);
		Object[] params = objects(0);
		traverse(cursor, new Job<Cursor>() {
			public void perform(Cursor c, Object[] params) throws Exception {
				params[0] = c.getInt(0);
			}
		}, params);
		return (Integer)params[0];
    }

    public static void turnForeignKeys(SQLiteDatabase db, boolean on) {
    	try {
			String sql = String.format("pragma foreign_keys = %s", on ? "ON" : "OFF");
			db.rawQuery(sql, null);
		} 
    	catch (SQLiteException e) {
    		Log.e(TAG, "foreign_keys", e);
    	}
	}

	public static String databaseName(Object flavor) {
		try {
			Class<?> c = Class.forName(flavor + "Provider");
			return c.getDeclaredField("DATABASE_NAME").get(null).toString();
		} catch (Exception e) {
			return null;
		}
    }

	public static String[] databases(Activity activity) {
		ArrayList<String> list = alist();
		ValList authorities = contentAuthorities(providerPackages, activity);
		for (Object authority : authorities) {
			String name = databaseName(authority);
			if (notNullOrEmpty(name))
				list.add(name);
		}
		return toStrings(list);
	}
	
	public static File getDatabaseFile(Context context, Uri uri) {
		if (hasAuthority(uri)) {
			try {
				String name = databaseName(uri.getAuthority());
				File path = context.getDatabasePath(name);
				return path.getCanonicalFile();
			} catch (IOException e) {
				Log.e(TAG, "getDatabaseFile", e);
				return null;
			}
		}
		else if (uri != null)
			return new File(uri.getPath());
		else
			return null;
	}

	public static ContentValues contentValues(ValMap info, List<Object> projection, Object...items) {
		ContentValues values = new ContentValues();
		ValList names = info.getList("name");
		for (int i = 0; i < projection.size(); i++) {
			String name = stringValueOf(projection.get(i));
			int index = names.indexOf(name);
			Object type = info.getListValue("type", index);
			Object item = param(null, i, items);
			if ("TEXT".equals(type))
				values.put(name, (String) item);
			else if ("INTEGER".equals(type))
				values.put(name, toLong(null, stringValueOf(item)));
			else if ("REAL".equals(type) || "FLOAT".equals(type) || "DOUBLE".equals(type))
				values.put(name, toDouble(Double.NaN, stringValueOf(item)));
			else if ("BLOB".equals(type))
				values.put(name, (byte[]) item);
			else
				values.putNull(name);
		}
		return values;
	}

	public static ContentValues contentValuesFromQuery(String uriString, Object...items) {
		ContentValues values = new ContentValues();
		Uri uri = Uri.parse(uriString);
		if (uri != null) {
			String query = uri.getQuery();
			if (query != null) {
				ValList list = split(query, "&");
				for (int i = 0; i < list.size(); i++) {
					String[] parts = list.get(i).toString().split("=");
					Object item = param(null, i, items);
					if ("TEXT".equals(parts[1]))
						values.put(parts[0], (String) item);
					else if ("INTEGER".equals(parts[1]))
						values.put(parts[0], toLong(null, stringValueOf(item)));
					else if ("REAL".equals(parts[1]))
						values.put(parts[0],
								toDouble(Double.NaN, stringValueOf(item)));
					else if ("BLOB".equals(parts[1]))
						values.put(parts[0], (byte[]) item);
					else
						values.putNull(parts[0]);
				}
			}
		}
		return values;
	}

	public static final String[] PARENS = {"(", ")"};
	public static final String[] BRACKETS = {"[", "]"};
	public static final String[] BRACES = {"{", "}"};
	
	public static boolean isNULL(Object value) {
		return JSONObject.NULL.equals(value);
	}

	@SuppressWarnings("unchecked")
	public static Object walkJSON(Object[] path, Object json, Function<Object> filter, Object...params) {
		Object object = json;
		if (path == null)
			path = objects();
		if (json instanceof JSONObject) {
			JSONObject jo = (JSONObject) json;
			ValMap map = vmap();
			Iterator<String> it = jo.keys();
			while (it.hasNext()) {
				String key = it.next();
				Object value = null;
				try {
					value = jo.get(key);
				} catch (JSONException e) {}
				Object[] path2 = arrayappend(path, key);
				String string = String.valueOf(value);
				if (string.startsWith(BRACKETS[0])) {
					try {
						JSONArray jsonArray = jo.getJSONArray(key);
						value = walkJSON(path2, jsonArray, filter, params);
					} catch (JSONException e) {
						value = walkJSON(path2, value, filter, params);
					}
				}
				else if (string.startsWith(BRACES[0])) {
					try {
						JSONObject jsonObject = jo.getJSONObject(key);
						value = walkJSON(path2, jsonObject, filter, params);
					} catch (JSONException e) {
						value = walkJSON(path2, value, filter, params);
					}
				}
				else
					value = walkJSON(path2, value, filter, params);
				map.put(key, value);
			}
			object = map;
		}
		else if (json instanceof JSONArray) {
			JSONArray ja = (JSONArray) json;
			ValList list = vlist();
			for (int i = 0; i < ja.length(); i++) {
				try {
					Object value = ja.get(i);
					list.add(walkJSON(arrayappend(path, i), value, filter, params));
				} catch (JSONException e) {}
			}
			object = list;
		}
		else if (filter != null)
			object = filter.apply(path, json, params);
		return isNULL(object) ? null : object;
	}

	@SuppressWarnings("rawtypes")
	public static void toJSON(Object[] path, JSONStringer stringer, String string, Object object, Function<Object> filter, Object...params) throws Exception {
		if (notNullOrEmpty(string))
			stringer.key(string);
		if (path == null)
			path = objects();
		if (object instanceof Map) {
			stringer.object();
			Map map = (Map) object;
			for (Object key : map.keySet()) {
				toJSON(arrayappend(path, key), stringer, key.toString(), map.get(key), filter, params);
			}
			stringer.endObject();
		}
		else if (object instanceof Collection) {
			stringer.array();
			int i = 0;
			Iterator it = ((Collection) object).iterator();
			while (it.hasNext()) {
				toJSON(arrayappend(path, i), stringer, "", it.next(), filter, params);
				i++;
			}
			stringer.endArray();
		}
		else {
			if (filter != null)
				object = filter.apply(path, object, params);
			stringer.value(object);
		}
	}

	public static String readAsset(Context context, String fileName) {
	    StringBuffer sb = new StringBuffer();
	    try {
	    	AssetManager am = context.getResources().getAssets();
			InputStream is = am.open( fileName );
			while( true ) {
			    int c = is.read();
	            if( c < 0 )
	                break;
			    sb.append( (char)c );
			}
		} catch (Exception e) {
			Log.e(TAG, "readAsset", e);
		}
	    return sb.toString();
	}
	
	public static byte[] getBytes(Bitmap bitmap) {
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		bitmap.compress(CompressFormat.JPEG, 0, stream);
		return stream.toByteArray();
	}

	public static Bitmap getBitmap(byte[] bytes) {
		return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
	}

}

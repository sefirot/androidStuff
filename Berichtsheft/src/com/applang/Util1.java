package com.applang;

import static com.applang.Util.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONStringer;

import com.applang.Util.Function;
import com.applang.Util.Job;
import com.applang.Util.Predicate;
import com.applang.Util.ValList;
import com.applang.Util.ValMap;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

public class Util1
{
	public static final String[] BRACKETS = {"[", "]"};
	public static final String[] PARENS = {"(", ")"};
	public static final String[] BRACES = {"{", "}"};

	public static boolean traverse(Cursor cursor, Job<Cursor> job, Object...params) {
		if (cursor == null)
			return false;
		try {
	    	if (cursor.moveToFirst()) 
	    		do {
					job.perform(cursor, params);
	    		} while (cursor.moveToNext());
	    	return true;
		} catch (Exception e) {
            Log.e(TAG, "traversing cursor", e);
            return false;
		}
		finally {
			cursor.close();
		}
    }
	
	public static ValMap getResults(Cursor cursor, final Function<String> key, final Function<Object> value) {
		final ValMap map = new ValMap();
		traverse(cursor, new Job<Cursor>() {
			public void perform(Cursor c, Object[] params) throws Exception {
				String k = key.apply(c);
				Object v = value.apply(c);
				map.put(k, v);
			}
		});
		return map;
	}

	public static ValList getRecord(Cursor cursor) {
		ValList list = new ValList();
		for (int i = 0; i < cursor.getColumnCount(); i++)
			list.add(cursor.getString(i));
		return list;
	}

	public static LinearLayout linearLayout(Context context, int orientation, int width, int height) {
	    LinearLayout linear = new LinearLayout(context);
	    linear.setOrientation(orientation);
		linear.setLayoutParams(new LayoutParams(width, height));
		return linear;
	}

	public static LinearLayout.LayoutParams linearLayoutParams(int width, int height, Integer... ltrb) {
		LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(width, height);
		layoutParams.setMargins(param(0, 0, ltrb), param(0, 1, ltrb), param(0, 2, ltrb), param(0, 3, ltrb));
		return layoutParams;
	}

	public static RelativeLayout.LayoutParams relativeLayoutParams(int width, int height, Integer... ltrb) {
		RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(width, height);
		layoutParams.setMargins(param(0, 0, ltrb), param(0, 1, ltrb), param(0, 2, ltrb), param(0, 3, ltrb));
		return layoutParams;
	}

	public static RelativeLayout relativeLayout(Context context, int width, int height) {
		RelativeLayout relative = new RelativeLayout(context);
	    relative.setLayoutParams(new LayoutParams(width, height));
		return relative;
	}

	public static Object[] iterateViews(ViewGroup container, Function<Object[]> func, int indent, Object... params) {
		if (container != null) {
			for (int i = 0; i < container.getChildCount(); i++) {
				View v = container.getChildAt(i);
				params = func.apply(v, indent, params);
				if (v instanceof ViewGroup) {
					iterateViews((ViewGroup) v, func, indent + 1, params);
				}
			}
		}
		return params;
	}

	public static String viewHierarchy(ViewGroup container) {
		String s = indentedLine(container.getClass().getSimpleName(), TAB, 0);
		Object[] params = iterateViews(container, 
			new Function<Object[]>() {
				public Object[] apply(Object... params) {
					View v = param(null, 0, params);
					int indent = paramInteger(null, 1, params);
					Object[] parms = param(null, 2, params);
					String s = (String) parms[0];
					String line = /*v.getId() + " : " + */v.getClass().getSimpleName();
					s += indentedLine(line, TAB, indent);
					parms[0] = s;
					return parms;
				}
			}, 
			1, 
			objects(s)
		);
		return paramString("", 0, params);
	}

	public static View getContentView(Activity activity) {
		View rootView = activity.findViewById(android.R.id.content);
		ViewGroup viewGroup = (ViewGroup)rootView;
		return viewGroup.getChildAt(0);
	}

	public static String providerPackage = "com.applang.provider";
	
	public static Predicate<String> isProvider = new Predicate<String>() {
		public boolean apply(String t) {
			return t.startsWith(providerPackage);
		}
	};

	@SuppressWarnings("rawtypes")
	public static ValList contentAuthorities(Context context, String packageName) {
		ValList list = new ValList();
		try {
			Class[] cls = getLocalClasses(packageName);
			for (Class c : filter(list(cls), false, new Predicate<Class>() {
				public boolean apply(Class c) {
					String name = c.getName();
					return !name.contains("$") && !name.endsWith("Provider");
				}
			})) {
				Object name = c.getDeclaredField("AUTHORITY").get(null);
				if (name != null)
					list.add(name.toString());
			}
		} catch (Exception e) {
			Log.e(TAG, "contentAuthorities", e);
		}
	    return list;
	}

	public static Uri contentUri(String authority, String path) {
    	return new Uri.Builder()
    		.scheme(ContentResolver.SCHEME_CONTENT)
    		.authority(authority)
    		.path(path)
    		.build();
    }

	public static Uri fileUri(String path, String table) {
    	return Uri.parse(path).buildUpon()
    		.scheme(ContentResolver.SCHEME_FILE)
    		.fragment(table)
    		.build();
    }

	public static boolean hasAuthority(Uri uri) {
		return notNullOrEmpty(uri.getAuthority());
	}

	public static String dbInfo(Uri uri) {
		if (uri == null)
			return "";
		else if (hasAuthority(uri))
			return uri.getAuthority();
		else
			return uri.getPath();
	}

	public static Uri dbTable(Uri uri, String table) {
		Uri.Builder builder = uri.buildUpon();
		if (hasAuthority(uri))
			builder = builder.path(table);
		else
			builder = builder.fragment(table);
		return builder.build();
	}

	public static String dbTableName(Uri uri) {
		if (hasAuthority(uri)) {
	        boolean hasPath = notNullOrEmpty(uri.getPath());
	        return hasPath ? uri.getPathSegments().get(0) : "";
		}
		else
			return uri.getFragment();
	}
	
	public static ValMap schema(Context context, Uri uri) {
    	uri = dbTable(uri, null);
		Cursor cursor = context.getContentResolver().query(
				uri, 
				null, 
				"select name,sql from sqlite_master where type = 'table'", 
				null, 
				null);
		final ValMap schema = new ValMap();
		traverse(cursor, new Job<Cursor>() {
			public void perform(Cursor c, Object[] params) throws Exception {
				schema.put(c.getString(0), c.getString(1));
			}
		});
		return schema;
	}
	
	public static ValMap table_info(Context context, Uri uri, String table) {
    	uri = dbTable(uri, null);
		Cursor cursor = context.getContentResolver().query(
				uri, 
				null, 
				String.format("pragma table_info(%s)", table),  
				null, 
				null);
		final ValMap info = new ValMap();
		info.put("cid", new ValList());
		info.put("name", new ValList());
		info.put("type", new ValList());
		info.put("notnull", new ValList());
		info.put("dflt_value", new ValList());
		info.put("pk", new ValList());
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
		return info;
	}
    
    public static ValList tables(Context context, Uri uri) {
    	uri = dbTable(uri, null);
		Cursor cursor = context.getContentResolver().query(
				uri, 
				null, 
				"select name from sqlite_master where type = 'table'", 
				null, 
				null);
		final ValList tables = new ValList();
		traverse(cursor, new Job<Cursor>() {
			public void perform(Cursor c, Object[] params) throws Exception {
				tables.add(c.getString(0));
			}
		});
		return tables;
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

	public static String databaseName(Object authority) {
		try {
			Class<?> c = Class.forName(authority + "Provider");
			return c.getDeclaredField("DATABASE_NAME").get(null).toString();
		} catch (Exception e) {
			return null;
		}
    }

	public static String[] databases(Context context, String packageName) {
		ArrayList<String> list = new ArrayList<String>();
		for (Object authority : contentAuthorities(context, packageName)) {
			String name = databaseName(authority);
			if (notNullOrEmpty(name))
				list.add(name);
		}
		return list.toArray(new String[0]);
	}

	@SuppressWarnings("unchecked")
	public static Object walkJSON(Object[] path, Object json, Function<Object> filter, Object...params) throws Exception {
		Object object = json;
		
		if (path == null)
			path = new Object[0];
		
		if (json instanceof JSONObject) {
			JSONObject jo = (JSONObject) json;
			ValMap map = new ValMap();
			Iterator<String> it = jo.keys();
			while (it.hasNext()) {
				String key = it.next();
				Object value = jo.get(key);
				Object[] path2 = arrayappend(path, key);
				String string = value.toString();
				if (string.startsWith(BRACKETS[0])) 
					value = walkJSON(path2, jo.getJSONArray(key), filter, params);
				else if (string.startsWith(BRACES[0]))
					value = walkJSON(path2, jo.getJSONObject(key), filter, params);
				else
					value = walkJSON(path2, value, filter, params);
				map.put(key, value);
			}
			object = map;
		}
		else if (json instanceof JSONArray) {
			JSONArray ja = (JSONArray) json;
			ValList list = new ValList();
			for (int i = 0; i < ja.length(); i++) 
				list.add(walkJSON(arrayappend(path, i), ja.get(i), filter, params));
			object = list;
		}
		else if (filter != null)
			object = filter.apply(path, json, params);
		
		return object;
	}

	@SuppressWarnings("rawtypes")
	public static void toJSON(JSONStringer stringer, String string, Object object, Function<Object> filter, Object...params) throws Exception {
		if (notNullOrEmpty(string))
			stringer.key(string);
		
		if (object instanceof Map) {
			stringer.object();
			Map map = (Map) object;
			for (Object key : map.keySet()) 
				toJSON(stringer, key.toString(), map.get(key), filter, params);
			stringer.endObject();
		}
		else if (object instanceof Collection) {
			stringer.array();
			Iterator it = ((Collection) object).iterator();
			while (it.hasNext()) 
				toJSON(stringer, "", it.next(), filter, params);
			stringer.endArray();
		}
		else {
			if (filter != null)
				object = filter.apply(object, params);
			
			if (object != null) 
				stringer.value(object);
		}
	}

}

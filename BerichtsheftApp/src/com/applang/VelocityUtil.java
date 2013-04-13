package com.applang;

import java.io.StringWriter;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import org.apache.velocity.app.Velocity;
import org.apache.velocity.context.AbstractContext;
import org.apache.velocity.context.Context;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONStringer;

import static com.applang.Util.*;
 
public class VelocityUtil
{
    public static void setupVelocity4Android(String packageName, Object resources) {
  		Velocity.setProperty(Velocity.RUNTIME_LOG_LOGSYSTEM_CLASS, "com.applang.VelocityLogger");
  		Velocity.setProperty(Velocity.RESOURCE_LOADER, "android");
  		Velocity.setProperty("android.resource.loader.class", "com.applang.VelocityResourceLoader");
  		Velocity.setProperty("android.content.res.Resources", resources);
  		Velocity.setProperty("packageName", packageName);
  		Velocity.init();
    }

	public static class MapContext extends AbstractContext
	{
		ValMap map;
		
		public MapContext(ValMap map) {
			this.map = map;
		}
	
		@Override
		public Object internalGet(String key) {
			return map == null ? null : map.get(key);
		}
	
		@Override
		public Object internalPut(String key, Object value) {
			return map == null ? null : map.put(key, value);
		}
	
		@Override
		public boolean internalContainsKey(Object key) {
			return map == null ? false : map.containsKey(key);
		}
	
		@Override
		public Object[] internalGetKeys() {
			return map == null ? new Object[0] : map.keySet().toArray();
		}
	
		@Override
		public Object internalRemove(Object key) {
			return map == null ? null : map.remove(key);
		}
	}
	
	public static String evaluation(Context context, String template, String logTag) {
		StringWriter w = new StringWriter();
		Velocity.evaluate( context, w, logTag, template );
		return w.toString();
	}

	@SuppressWarnings("unchecked")
	public static Object walkJSON(Object[] path, Object json, Function<Object> filter, Object...params) throws Exception {
		Object object = json;
		
		if (path == null)
			path = new Object[0];
		
		if (json instanceof JSONObject) {
			JSONObject jo = (JSONObject) json;
			Util.ValMap map = new Util.ValMap();
			Iterator<String> it = jo.keys();
			while (it.hasNext()) {
				String key = it.next();
				Object value = jo.get(key);
				Object[] path2 = Util.arrayappend(path, key);
				if (value.toString().startsWith("[")) 
					value = walkJSON(path2, jo.getJSONArray(key), filter, params);
				else if (value.toString().startsWith("{"))
					value = walkJSON(path2, jo.getJSONObject(key), filter, params);
				else
					value = walkJSON(path2, value, filter, params);
				map.put(key, value);
			}
			object = map;
		}
		else if (json instanceof JSONArray) {
			JSONArray ja = (JSONArray) json;
			Util.ValList list = new Util.ValList();
			for (int i = 0; i < ja.length(); i++) 
				list.add(walkJSON(Util.arrayappend(path, i), ja.get(i), filter, params));
			object = list;
		}
		else if (filter != null)
			object = filter.apply(path, json, params);
		
		return object;
	}

	public static Object member(Object[] path, Object object) {
		for (int i = 0; i < path.length; i++) {
			if (path[i] instanceof Integer) 
				object = ((Util.ValList)object).get((Integer) path[i]);
			else
				object = ((Util.ValMap)object).get(path[i].toString());
		}
		return object;
	}

	@SuppressWarnings("rawtypes")
	public static void toJSON(JSONStringer stringer, String string, Object object, Function<Object> filter, Object...params) throws Exception {
		if (Util.notNullOrEmpty(string))
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

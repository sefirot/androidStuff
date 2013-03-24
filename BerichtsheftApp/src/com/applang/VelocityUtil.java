package com.applang;

import java.io.StringWriter;

import org.apache.velocity.app.Velocity;
import org.apache.velocity.context.AbstractContext;
import org.apache.velocity.context.Context;

import com.applang.Util.ValMap;
 
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
}

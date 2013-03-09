package com.applang;
 
import org.apache.velocity.runtime.RuntimeServices;
import org.apache.velocity.runtime.log.LogChute;
 
import android.util.Log;
 
public class VelocityLogger implements LogChute {
	private final static String tag = "Velocity";
 
	public void init(RuntimeServices arg0) throws Exception {
	}
 
	public boolean isLevelEnabled(int level) {
		return level > LogChute.DEBUG_ID;
	}
 
	public void log(int level, String msg) {
		log(level, msg, null);
	}
 
	public void log(int level, String msg, Throwable t) {
		switch(level) {
			case LogChute.DEBUG_ID:
				Log.d(tag,msg,t);
				break;
			case LogChute.ERROR_ID:
				Log.e(tag,msg,t);
				break;
			case LogChute.INFO_ID:
				Log.i(tag,msg,t);
				break;
			case LogChute.TRACE_ID:
				Log.d(tag,msg,t);
				break;
			case LogChute.WARN_ID:
				Log.w(tag,msg,t);
		}
	}
}
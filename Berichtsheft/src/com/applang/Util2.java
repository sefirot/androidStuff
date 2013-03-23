package com.applang;

import android.database.Cursor;
import android.util.Log;

import static com.applang.Util.*;

public class Util2
{
	public static void delay(int millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Log.e("ERROR", "Thread was Interrupted");
        }
	}
	
	public static ValMap getResultMap(Cursor cursor, Function<String> key, Function<Object> value) {
		ValMap map = new ValMap();
		try {
	    	if (cursor.moveToFirst()) 
	    		do {
					String k = key.apply(cursor);
					Object v = value.apply(cursor);
					map.put(k, v);
	    		} while (cursor.moveToNext());
		} catch (Exception e) {
			return null;
		}
		finally {
			cursor.close();
		}
		return map;
	}

}

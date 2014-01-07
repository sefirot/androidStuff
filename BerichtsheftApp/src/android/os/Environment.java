package android.os;

import static com.applang.Util.*;

import java.io.File;
import java.io.IOException;

import android.util.Log;

public class Environment {
    private static final String TAG = Environment.class.getSimpleName();

	public static File getDataDirectory() {
        try {
			return new File(getDataDir()).getCanonicalFile();
		} catch (Exception e) {
			Log.e(TAG, "", e);
			return null;
		}
    }

	public static String getDataDir() {
		String dir = System.getProperty("data.dir");
		if (notNullOrEmpty(dir))
			return dir;
		else
			return System.getProperty("user.dir");
	}

	public static void setDataDir(String dir) {
		System.setProperty("data.dir", dir);
	}
}

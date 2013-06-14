package android.os;

import static com.applang.Util.*;

import java.io.File;

public class Environment {
    public static File getDataDirectory() {
        return new File(getDataDir());
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

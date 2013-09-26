package android.content;

import static com.applang.Util.*;
import static com.applang.Util2.*;

import java.awt.Point;
import java.io.File;

import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;

public class Context
{
	private final static String TAG = Context.class.getSimpleName();
    private final static boolean DEBUG = false;
    
    public static final int MODE_PRIVATE = 0x0000;
    public static final int MODE_WORLD_READABLE = 0x0001;
    public static final int MODE_WORLD_WRITEABLE = 0x0002;
    public static final int MODE_APPEND = 0x8000;
    
    private final Object mSync = new Object();
    private File mDatabasesDir;

    private File makeFilename(File base, String name) {
        if (name.indexOf(File.separatorChar) < 0) {
            return new File(base, name);
        }
        throw new IllegalArgumentException(
            "File " + name + " contains a path separator");
    }

    private static void setFilePermissionsFromMode(String name, int mode, int extraPermissions) {
        int perms = S_IRUSR|S_IWUSR
            |S_IRGRP|S_IWGRP
            |extraPermissions;
        if ((mode&MODE_WORLD_READABLE) != 0) {
            perms |= S_IROTH;
        }
        if ((mode&MODE_WORLD_WRITEABLE) != 0) {
            perms |= S_IWOTH;
        }
        if (DEBUG) {
            Log.i(TAG, "File " + name + ": mode=0x" + Integer.toHexString(mode)
                  + ", perms=0x" + Integer.toHexString(perms));
        }
        setPermissions(name, perms);
    }

    public static final int S_IRWXU = 00700;
    public static final int S_IRUSR = 00400;
    public static final int S_IWUSR = 00200;
    public static final int S_IXUSR = 00100;

    public static final int S_IRWXG = 00070;
    public static final int S_IRGRP = 00040;
    public static final int S_IWGRP = 00020;
    public static final int S_IXGRP = 00010;

    public static final int S_IRWXO = 00007;
    public static final int S_IROTH = 00004;
    public static final int S_IWOTH = 00002;
    public static final int S_IXOTH = 00001;
    
    public void setPackageInfo(String packageName, Object...params) {
    	if (packageName == null) 
    		mPackageInfo = param(null, 0, params);
    	else
    		mPackageInfo = new PackageInfo(packageName, params);
	}

    public static class PackageInfo {

        public PackageInfo(String name, Object...params) {
            mPackageName = name;
            mDataDir = paramString("", 0, params);
        }

        private final String mDataDir;
        private final String mPackageName;
        
        public String getPackageName() {
            return mPackageName;
        }

        public File getDataDirFile() {
        	File dataDir = notNullOrEmpty(mDataDir) ? 
        			new File(mDataDir) : 
        			Environment.getDataDirectory();
            return new File(dataDir, "data/" + getPackageName());
        }
    }
    
    protected PackageInfo mPackageInfo;
    
    public String getPackageName() {
    	return mPackageInfo != null ? mPackageInfo.getPackageName() : "";
    }
    
    private File getDataDirFile() {
        if (mPackageInfo != null) {
            return mPackageInfo.getDataDirFile();
        }
        throw new RuntimeException("Not supported in system context");
    }
    
    private File getDatabasesDir() {
        synchronized (mSync) {
            if (mDatabasesDir == null) {
                mDatabasesDir = new File(getDataDirFile(), "databases");
            }
            if (mDatabasesDir.getPath().equals("databases")) {
                mDatabasesDir = new File("/data/system");
            }
            return mDatabasesDir;
        }
    }

    public SQLiteDatabase openOrCreateDatabase(String name, int mode, CursorFactory factory) {
    	File dbFile = getDatabasePath(name);
        SQLiteDatabase db = SQLiteDatabase.openOrCreateDatabase(dbFile, factory);
        setFilePermissionsFromMode(dbFile.getPath(), mode, 0);
        return db;
    }
    
    public boolean deleteDatabase(String name) {
        try {
            File f = makeFilename(getDatabasesDir(), name);
            return f.delete();
        } catch (Exception e) {
        }
        return false;
    }
	
    public File getDatabasePath(String name) {
    	File dir = getDatabasesDir();
    	if (!dir.isDirectory() && dir.mkdir()) {
    		setPermissions(dir.getPath(), S_IRWXU|S_IRWXG|S_IXOTH);
    	}
    	return makeFilename(dir, name);
    }

	public String[] databaseList() {
		File[] files = getDatabasesDir().listFiles();
		String[] names = new String[files.length];
		for (int i = 0; i < names.length; i++) {
			names[i] = files[i].getName();
		}
		return names;
	}
	
	ContentResolver contentResolver = new ContentResolver(this);
	
	public ContentResolver getContentResolver() {
		return contentResolver;
	}

	public Cursor managedQuery(Uri uri, 
			String[] projection, String selection,
			String[] selectionArgs, String sortOrder) {
		return getContentResolver().query(uri, projection, selection, selectionArgs, sortOrder);
	}

	public String[] fileList() {
		// TODO Auto-generated method stub
		return null;
	}

	public File getDir(String name, int mode) {
		// TODO Auto-generated method stub
		return null;
	}

	public Resources getResources() {
		return new Resources(this);
	}
	
	public Point location;
	public Context setLocation(Point location) {
		this.location = location;
		return this;
	}
}

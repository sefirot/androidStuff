package android.content;

import java.awt.Point;
import java.io.File;
import java.util.Map;

import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;

import static com.applang.Util.*;
import static com.applang.Util1.*;
import static com.applang.Util2.*;

public class Context
{
	public static Context contextForFlavor(final String packageName, final String flavor, final File dbFile) {
		return new Context() {
			{
				if (dbFile != null) {
					setPackageInfo(packageName, dbFile.getParent());
					registerFlavor(flavor, dbFile.getPath());
				}
			}};
	}
	
	@Override
	public String toString() {
		return String.format("Context : '%s' %s", getPackageName(), getDataDirFile().getPath());
	}

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

    public static class PackageInfo {

        public PackageInfo(String name, Object...params) {
            mPackageName = name;
            mDataDir = param_String("", 0, params);
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
    
    public void setPackageInfo(String packageName, Object...params) {
    	if (packageName == null) 
    		mPackageInfo = param(null, 0, params);
    	else
    		mPackageInfo = new PackageInfo(packageName, params);
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
	
    private String flavor = null;
    
	public String getFlavor() {
		return flavor;
	}

	public void setFlavor(String flavor) {
		this.flavor = flavor;
	}

	private BidiMultiMap databasePaths = bmap(2);
	
	public void registerFlavor(String flavor, String path) {
		String name = database(flavor);
		databasePaths.putValue(name, path);
		setFlavor(flavor);
	}
	
	public boolean unregisterFlavor(String flavor) {
		String name = database(flavor);
		return databasePaths.removeKey(name);
	}

	public String getDatabasePath(Uri uri) {
		if (uri == null)
			return "";
		else if (hasAuthority(uri)) {
			String flavor = uri.getAuthority();
			String dbName = database(flavor);
			File file = getDatabasePath(dbName);
			return file.getPath();
		}
		else
			return uri.getPath();
	}
	
    public File getDatabasePath(String name) {
		if (databasePaths.getKeys().contains(name))
			return new File(databasePaths.getValue(name).toString());
		else {
	    	File dir = getDatabasesDir();
	    	if (!dir.isDirectory() && dir.mkdir()) {
	    		setPermissions(dir.getPath(), S_IRWXU|S_IRWXG|S_IXOTH);
	    	}
	    	return makeFilename(dir, name);
	 	}
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

    /**
     * Retrieve and hold the contents of the preferences file 'name', returning
     * a SharedPreferences through which you can retrieve and modify its
     * values.  Only one instance of the SharedPreferences object is returned
     * to any callers for the same name, meaning they will see each other's
     * edits as soon as they are made.
     *
     * @param path Desired preferences file. If a preferences file by this name
     * does not exist, it will be created when you retrieve an
     * editor (SharedPreferences.edit()) and then commit changes (Editor.commit()).
     * @param mode Operating mode.  Use 0 or {@link #MODE_PRIVATE} for the
     * default operation, {@link #MODE_WORLD_READABLE}
     * and {@link #MODE_WORLD_WRITEABLE} to control permissions.
     *
     * @return Returns the single SharedPreferences instance that can be used
     *         to retrieve and modify the preference values.
     *
     * @see #MODE_PRIVATE
     * @see #MODE_WORLD_READABLE
     * @see #MODE_WORLD_WRITEABLE
     */
    public SharedPreferences getSharedPreferences(final String path, int mode) {
    	Settings.load(path);
    	return new SharedPreferences() {
			public Map<Object, ?> getAll() {
				return Settings.properties;
			}
			public String getString(String key, String defValue) {
				return getSetting(key, defValue);
			}
			public int getInt(String key, int defValue) {
				return getSetting(key, defValue);
			}
			public long getLong(String key, long defValue) {
				return getSetting(key, defValue);
			}
			public float getFloat(String key, float defValue) {
				return getSetting(key, defValue);
			}
			public boolean getBoolean(String key, boolean defValue) {
				return getSetting(key, defValue);
			}
			public boolean contains(String key) {
				return Settings.contains(key);
			}
			public Editor edit() {
				return new Editor() {
					public Editor remove(String key) {
						Settings.remove(key);
						return this;
					}
					public Editor putString(String key, String value) {
						putSetting(key, value);
						return this;
					}
					public Editor putLong(String key, long value) {
						putSetting(key, value);
						return this;
					}
					public Editor putInt(String key, int value) {
						putSetting(key, value);
						return this;
					}
					public Editor putFloat(String key, float value) {
						putSetting(key, value);
						return this;
					}
					public Editor putBoolean(String key, boolean value) {
						putSetting(key, value);
						return this;
					}
					public boolean commit() {
						Settings.save(path);
						return true;
					}
					public Editor clear() {
						Settings.clear();
						return null;
					}
				};
			}
			public void registerOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {
				// TODO Auto-generated method stub
			}
			public void unregisterOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {
				// TODO Auto-generated method stub
			}
    	};
    }
	
	public Point location;
	public Context setLocation(Point location) {
		this.location = location;
		return this;
	}
}

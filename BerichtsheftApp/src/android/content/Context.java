package android.content;

import java.awt.Point;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
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
					setPackageInfo(packageName);
					registerFlavor(flavor, dbFile.getPath());
				}
			}};
	}
	
	@Override
	public String toString() {
		return String.format("Context : '%s' %s", getPackageName(), getDataDirectory().getPath());
	}

	private final static String TAG = Context.class.getSimpleName();
    private final static boolean DEBUG = false;
    
    public static final int MODE_PRIVATE = 0x0000;
    public static final int MODE_WORLD_READABLE = 0x0001;
    public static final int MODE_WORLD_WRITEABLE = 0x0002;
    public static final int MODE_APPEND = 32768;
    
    private File makeFilename(File base, String name) {
        if (name.indexOf(File.separatorChar) < 0) {
            return new File(base, name);
        }
        throw new IllegalArgumentException(
            "File " + name + " contains a path separator");
    }

    @SuppressWarnings("unused")
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
            File dataDir = getDataDirFile();
			if (dataDir.mkdirs())
				no_println("mkdirs", dataDir);
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
            return nullOrEmpty(mPackageName) ?
            		dataDir :
            		new File(dataDir, "data/" + mPackageName);
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
    
    //	NOTE	this does NOT correspond to an Android API
    public File getDataDirectory() {
        if (mPackageInfo != null) {
            return mPackageInfo.getDataDirFile();
        }
        throw new RuntimeException("Not supported in system context");
    }
    
    private final Object mSync = new Object();
    private File mDatabasesDir;

    private File getDatabasesDir() {
        synchronized (mSync) {
            if (mDatabasesDir == null) {
                mDatabasesDir = new File(getDataDirectory(), "databases");
            }
            if (mDatabasesDir.getPath().equals("databases")) {
                mDatabasesDir = new File("/data/system");
            }
	    	if (!mDatabasesDir.isDirectory() && mDatabasesDir.mkdir()) {
	    		setPermissions(mDatabasesDir.getPath(), S_IRWXU|S_IRWXG|S_IXOTH);
	    	}
            return mDatabasesDir;
        }
    }

    public SQLiteDatabase openOrCreateDatabase(String name, int mode, CursorFactory factory) {
    	File dbFile = getDatabasePath(name);
        SQLiteDatabase db = SQLiteDatabase.openOrCreateDatabase(dbFile, factory);
//		setFilePermissionsFromMode(dbFile.getPath(), mode, 0);
        return db;
    }
    
    public boolean deleteDatabase(String name) {
        try {
            File f = makeFilename(getDatabasesDir(), name);
            return f.delete();
        } 
        catch (Exception e) {
        	Log.e(TAG, "deleteDatabase", e);
        }
        return false;
    }
	
    private String flavor = null;
    
    //	NOTE	this does NOT correspond to an Android API
	public String getFlavor() {
		return flavor;
	}

    //	NOTE	this does NOT correspond to an Android API
	public void setFlavor(String flavor) {
		this.flavor = flavor;
	}

	private BidiMultiMap flavorPaths = bmap(2);
	
    //	NOTE	this does NOT correspond to an Android API
	public void registerFlavor(String flavor, String path) {
		String name = databaseName(flavor);
		if (name != null) {
			flavorPaths.putValue(name, dbPath(path));
		}
		setFlavor(flavor);
	}
	
    //	NOTE	this does NOT correspond to an Android API
	public boolean unregisterFlavor(String flavor) {
		String name = databaseName(flavor);
		return name != null ? flavorPaths.removeKey(name) : false;
	}

    //	NOTE	this does NOT correspond to an Android API
	public String getDatabasePath(Uri uri) {
		if (uri == null)
			return "";
		else if (hasAuthority(uri)) {
			String flavor = uri.getAuthority();
			String dbName = databaseName(flavor);
			File dbFile = getDatabasePath(dbName);
			return dbFile.getPath();
		}
		else
			return uri.getPath();
	}
	
    public File getDatabasePath(String name) {
		if (flavorPaths.getKeys().contains(name))
			return new File(stringValueOf(flavorPaths.getValue(name)));
		else {
	    	File dir = getDatabasesDir();
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
		File[] files = getFilesDir().listFiles();
		String[] names = new String[files.length];
		for (int i = 0; i < names.length; i++) {
			names[i] = files[i].getName();
		}
		return names;
	}
	
	public FileOutputStream openFileOutput(String name, int mode) throws FileNotFoundException {
		return new FileOutputStream(new File(getFilesDir(), name), mode == MODE_APPEND);
	}

	public File getFilesDir() {
		File dir = new File(getDataDirectory(), "files");
		if (!fileExists(dir))
			dir.mkdir();
		return dir;
	}

	public File getDir(String name, int mode) {
		File dir = new File(getDataDirectory(), String.format("app_%s", name));
		if (!fileExists(dir))
			dir.mkdir();
		return dir;
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
    	if (Settings.properties == null) {
    		Settings.load(path);
    	}
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

    //	NOTE	this does NOT correspond to an Android API
    public SharedPreferences getSharedPreferences() {
    	return getSharedPreferences(null, Context.MODE_PRIVATE);
    }
    
    //	NOTE	this does NOT correspond to an Android API
	public void message(String key, Object...params) {
		SharedPreferences prefs = getSharedPreferences();
		String format = prefs.getString(key, null);
		String msg = notNullOrEmpty(format) ? 
				String.format(format, params) : 
				String.format("<<< message text missing for key '%s'>>>", key) + com.applang.Util.toString(params);
		com.applang.SwingUtil.message(msg);
	}
	
	private Point location = new Point(0,0);
	
    //	NOTE	this does NOT correspond to an Android API
	public Context setLocation(Point location) {
		this.location = location;
		return this;
	}

	//	NOTE	this does NOT correspond to an Android API
	public Point getLocation() {
		return location;
	}
}

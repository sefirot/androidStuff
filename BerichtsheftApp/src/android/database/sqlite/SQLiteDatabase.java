package android.database.sqlite;

import com.almworks.sqlite4java.SQLite;
import com.almworks.sqlite4java.SQLiteConnection;
import com.almworks.sqlite4java.SQLiteConstants;
import com.almworks.sqlite4java.SQLiteStatement;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

import java.io.File;
import java.sql.SQLException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.applang.Util.*;
import static com.applang.Util2.*;

/**
 * Exposes methods to manage a SQLite database.
 * @hide
 */
public class SQLiteDatabase {
    private static final String TAG = SQLiteDatabase.class.getSimpleName();
    
    public SQLiteDatabase() {
        Logger.getLogger("com.almworks.sqlite4java").setLevel(Level.SEVERE);
    }
    
    public static String getSQLiteVersion() {
		try {
			return SQLite.getSQLiteVersion();
		} catch (Exception e) {
			Log.e(TAG, "native lib loading failed", e);
			return "";
		}
	}

    public static final int CREATE_IF_NECESSARY = 0x10000000;

    SQLiteConnection connection = null;
    /**
     * Used to allow returning sub-classes of {@link Cursor} when calling query.
     */
    public interface CursorFactory {
        /**
         * See
         * {@link SQLiteCursor#SQLiteCursor(SQLiteCursorDriver, String, SQLiteQuery)}.
         */
        public Cursor newCursor(SQLiteConnection con, final SQLiteStatement stm);
    }
    
    private CursorFactory mFactory = new CursorFactory() {
		public Cursor newCursor(SQLiteConnection con, final SQLiteStatement stm) {
			return new Cursor() {
				int cnt = 0, position = -1;
				{
					if (stm != null)
						try {
							if (stm.hasStepped())
								stm.reset(false);
								
							while (stm.step()) {
								cnt++;
							}
							
						} catch (com.almworks.sqlite4java.SQLiteException e) {}
				}
				public int getCount() {
					return cnt;
				}
				public int getPosition() {
					return position;
				}
				public boolean moveToFirst() {
			    	if (stm == null)
			    		return false;
			    	
					try {
						position = -1;
						
						if (stm.hasStepped())
							stm.reset(false);
						
						return moveToNext();
					} catch (com.almworks.sqlite4java.SQLiteException e) {
						return false;
					}
				}
				public boolean moveToNext() {
			    	if (stm == null)
			    		return false;
			    	
					try {
						position++;
						return stm.step();
					} catch (com.almworks.sqlite4java.SQLiteException e) {
						position = -1;
						return false;
					}
				}
				public boolean moveToPosition(int position) {
					boolean retval = stm != null && position > -1 && position < cnt;
					if (position < this.position) {
						this.position = -1;
					}
					while (retval && position > this.position) {
						retval = this.position < 0 ? moveToFirst() : moveToNext();
					}
					return retval;
				}
				public boolean moveToPrevious() {
					return moveToPosition(position - 1);
				}
				public boolean moveToLast() {
					return moveToPosition(cnt - 1);
				}
				public boolean move(int offset) {
					return moveToPosition(position + offset);
				}
				public boolean isClosed() {
			    	if (stm != null)
			    		return stm.isDisposed();
			    	
					return true;
				}
				public void close() {
			    	if (stm != null) {
			    		stm.dispose();
			    	}
				}
				public boolean isNull(int columnIndex) {
					try {
						if (stm != null && stm.hasRow())
							return stm.columnNull(columnIndex);
					} catch (com.almworks.sqlite4java.SQLiteException e) {}
					
					return true;
				}
				public boolean isLast() {
					throw new UnsupportedOperationException("Not implemented");
				}
				public boolean isFirst() {
					throw new UnsupportedOperationException("Not implemented");
				}
				public boolean isBeforeFirst() {
					throw new UnsupportedOperationException("Not implemented");
				}
				public boolean isAfterLast() {
					throw new UnsupportedOperationException("Not implemented");
				}
				public int getType(int columnIndex) {
					try {
						if (stm != null && stm.hasRow())
							switch (stm.columnType(columnIndex)) {
							case SQLiteConstants.SQLITE_FLOAT:
								return FIELD_TYPE_FLOAT;
							case SQLiteConstants.SQLITE_INTEGER:
								return FIELD_TYPE_INTEGER;
							case SQLiteConstants.SQLITE_TEXT:
								return FIELD_TYPE_STRING;
							case SQLiteConstants.SQLITE_BLOB:
								return FIELD_TYPE_BLOB;
							case SQLiteConstants.SQLITE_NULL:
								return FIELD_TYPE_NULL;
							}
					} catch (com.almworks.sqlite4java.SQLiteException e) {}
					
					return -1;
				}
				public String getString(int columnIndex) {
					try {
						if (stm != null && stm.hasRow())
							return stm.columnString(columnIndex);
					} catch (com.almworks.sqlite4java.SQLiteException e) {}
					
					return null;
				}
				public int getInt(int columnIndex) {
					try {
						if (stm != null && stm.hasRow())
							return stm.columnInt(columnIndex);
					} catch (com.almworks.sqlite4java.SQLiteException e) {}
					
					return Integer.MIN_VALUE;
				}
				public short getShort(int columnIndex) {
					try {
						if (stm != null && stm.hasRow())
							return Short.parseShort(stm.columnString(columnIndex));
					} catch (com.almworks.sqlite4java.SQLiteException e) {}
					
					return Short.MIN_VALUE;
				}
				public long getLong(int columnIndex) {
					try {
						if (stm != null && stm.hasRow())
							return stm.columnLong(columnIndex);
					} catch (com.almworks.sqlite4java.SQLiteException e) {}
					
					return Long.MIN_VALUE;
				}
				public float getFloat(int columnIndex) {
					try {
						if (stm != null && stm.hasRow())
							return Float.parseFloat(stm.columnString(columnIndex));
					} catch (com.almworks.sqlite4java.SQLiteException e) {}
					
					return Float.NaN;
				}
				public double getDouble(int columnIndex) {
					try {
						if (stm != null && stm.hasRow())
							return stm.columnDouble(columnIndex);
					} catch (com.almworks.sqlite4java.SQLiteException e) {}
					
					return Double.NaN;
				}
				public byte[] getBlob(int columnIndex) {
					try {
						if (stm != null && stm.hasRow())
							return stm.columnBlob(columnIndex);
					} catch (com.almworks.sqlite4java.SQLiteException e) {}
					
					return null;
				}
				public int getColumnCount() {
					try {
						if (stm != null)
							return stm.columnCount();
					} catch (com.almworks.sqlite4java.SQLiteException e) {}
					
					return 0;
				}
				public String getColumnName(int columnIndex) {
					try {
						if (stm != null)
							return stm.getColumnName(columnIndex);
					} catch (com.almworks.sqlite4java.SQLiteException e) {}
					
					return null;
				}
				public String[] getColumnNames() {
					String[] names = new String[getColumnCount()];
					for (int i = 0; i < names.length; i++)
						names[i] = getColumnName(i);
					return names;
				}
				public int getColumnIndex(String columnName) {
					return asList(getColumnNames()).indexOf(columnName);
				}
				public int getColumnIndexOrThrow(String columnName)	throws IllegalArgumentException {
					throw new UnsupportedOperationException("Not implemented");
				}
				public boolean requery() {
					throw new UnsupportedOperationException("Not implemented");
				}
				public void deactivate() {
					throw new UnsupportedOperationException("Not implemented");
				}
				public void setNotificationUri(ContentResolver cr, Uri uri) {
					// TODO Auto-generated method stub
				}
			};
		}
	};

    public static SQLiteDatabase openDatabase(String path, CursorFactory factory, int flags) {
    	SQLiteDatabase db = new SQLiteDatabase();
        db.mFlags = flags;
		try {
			db.connection = new SQLiteConnection(path == null ? null : new File(path));
			db.connection.open((flags & CREATE_IF_NECESSARY) > 0);
			String libPath = System.getProperty("sqlite4java.library.path");
		    String os = getOs();
		    String arch = getArch(os);
		    String libName = String.format("pcre-%s-%s.so", os, arch);
			File extensionFile = new File(libPath, libName);
			if (fileExists(extensionFile)) {
				db.connection.setExtensionLoadingEnabled(true);
				db.connection.loadExtension(extensionFile, "sqlite3_regexp_init");
			}
		} catch (com.almworks.sqlite4java.SQLiteException e) {
			diag_println(DIAG_OFF, "openDatabase", path);
			Log.e(TAG, "openDatabase", e);
			return null;
		}
		
		if (factory != null)
			db.mFactory = factory;
		
        return db;
    }

    private static String getArch(String os) {
      String arch = System.getProperty("os.arch");
      if (arch == null) {
        Log.w(TAG, "os.arch is null");
        arch = "x86";
      } else {
        arch = arch.toLowerCase(Locale.US);
        if ("win32".equals(os) && "amd64".equals(arch)) {
          arch = "x64";
        }
      }
      Log.d(TAG, "os.arch=" + arch);
      return arch;
    }

    private static String getOs() {
      String osname = System.getProperty("os.name");
      String os;
      if (osname == null) {
        Log.w(TAG, "os.name is null");
        os = "linux";
      } else {
        osname = osname.toLowerCase(Locale.US);
        if (osname.startsWith("mac") || osname.startsWith("darwin") || osname.startsWith("os x")) {
          os = "osx";
        } else if (osname.startsWith("windows")) {
          os = "win32";
        } else {
          String runtimeName = System.getProperty("java.runtime.name");
          if (runtimeName != null && runtimeName.toLowerCase(Locale.US).contains("android")) {
            os = "android";
          } else {
            os = "linux";
          }
        }
      }
      Log.d(TAG, "os.name=" + osname + "; os=" + os);
      return os;
    }

    public static SQLiteDatabase openOrCreateDatabase(File file, CursorFactory factory) {
        return openOrCreateDatabase(file.getPath(), factory);
    }

    public static SQLiteDatabase openOrCreateDatabase(String path, CursorFactory factory) {
        return openDatabase(path, factory, CREATE_IF_NECESSARY);
    }

    public static SQLiteDatabase create(CursorFactory factory) {
        // This is a magic string with special meaning for SQLite.
        return openDatabase(":memory:", factory, CREATE_IF_NECESSARY);
    }

	public boolean isOpen() {
		return connection.isOpen();
	}

	public void close() {
		connection.dispose();		
	}

	public void execSQL(String sql) {
		try {
			connection.exec(sql);
		} catch (com.almworks.sqlite4java.SQLiteException e) {
			Log.e(TAG, "execSQL", e);
		}		
	}
	
	SQLiteStatement doBind(SQLiteStatement stm, Object[] bindArgs) throws com.almworks.sqlite4java.SQLiteException {
        if (bindArgs != null) {
        	clearBindings(stm);
        	
            int size = bindArgs.length;
            for (int i = 0; i < size; i++) {
                this.addToBindArgs(i + 1, bindArgs[i]);
            }
            
            if (size > 0)
            	for (int index : mBindArgs.keySet()) {
	                Object value = mBindArgs.get(index);
	                if (value == null) {
	                	stm.bindNull(index);
	                } else if (value instanceof Double || value instanceof Float) {
	                	stm.bind(index, ((Number) value).doubleValue());
	                } else if (value instanceof Number) {
	                	stm.bind(index, ((Number) value).longValue());
	                } else if (value instanceof Boolean) {
	                    Boolean bool = (Boolean)value;
	                    stm.bind(index, (bool) ? 1 : 0);
	                } else if (value instanceof byte[]){
	                	stm.bind(index, (byte[]) value);
	                } else {
	                	stm.bind(index, value.toString());
	                }
            	}
        }
    	
		return stm;
	}
	
	HashMap<Integer, Object> mBindArgs = null;

    void clearBindings(SQLiteStatement stm) throws com.almworks.sqlite4java.SQLiteException {
    	if (stm != null && mBindArgs != null)
    		stm.clearBindings();
    	
        mBindArgs = null;
    }

    void addToBindArgs(int index, Object value) {
        if (mBindArgs == null) {
            mBindArgs = new HashMap<Integer, Object>();
        }
        
        mBindArgs.put(index, value);
    }

    public boolean execSQL(String sql, Object[] bindArgs) {
		SQLiteStatement stm = null;
    	try {
			stm = doBind(connection.prepare(sql), bindArgs);
			stm.step();
			return true;
		} catch (com.almworks.sqlite4java.SQLiteException e) {
    		Log.d(TAG, String.format("%s : %s", stm, e.getMessage()));
			Log.e(TAG, "execSQL", e);
		}
    	finally {
	    	if (stm != null)
	    		stm.dispose();
    	}
		return false;
	}

	public Cursor rawQuery(String sql, String[] selectionArgs) throws SQLiteException {
        return rawQueryWithFactory(null, sql, selectionArgs, null);
	}

    public Cursor rawQueryWithFactory(
            CursorFactory cursorFactory, String sql, String[] selectionArgs,
            String editTable) throws SQLiteException {
    	SQLiteStatement stm = null;
    	try {
    		stm = doBind(connection.prepare(sql), selectionArgs);
    		return (cursorFactory != null ? cursorFactory : mFactory)
    				.newCursor(connection, stm);
    	} catch (com.almworks.sqlite4java.SQLiteException e) {
    		Log.d(TAG, String.format("%s : %s", stm, e.getMessage()));
    		throw new SQLiteException(2, TAG, e);
    	} 
    }

	public long insert(String table, Object nullColumnHack, ContentValues values) {
        StringBuilder sql = new StringBuilder();
        sql.append("INSERT");
//		sql.append(CONFLICT_VALUES[conflictAlgorithm]);
        sql.append(" INTO ");
        sql.append(table);
        sql.append('(');

        Object[] bindArgs = null;
        int size = (values != null && values.size() > 0) ? values.size() : 0;
        if (size > 0) {
            bindArgs = new Object[size];
            int i = 0;
            for (String colName : values.keySet()) {
                sql.append((i > 0) ? "," : "");
                sql.append(colName);
                bindArgs[i++] = values.get(colName);
            }
            sql.append(')');
            sql.append(" VALUES (");
            for (i = 0; i < size; i++) {
                sql.append((i > 0) ? ",?" : "?");
            }
        } else {
            sql.append(nullColumnHack + ") VALUES (NULL");
        }
        sql.append(')');
        
        if (execSQL(sql.toString(), bindArgs))
			try {
				return connection.getLastInsertId();
			} catch (com.almworks.sqlite4java.SQLiteException e) {}
        
        return -1;
	}
	
    public long insertOrThrow(String table, String nullColumnHack, ContentValues values) throws SQLException {
        return insert(table, nullColumnHack, values);
    }

	public int update(String table, ContentValues values, String whereClause, String[] whereArgs) {
        if (values == null || values.size() == 0) {
            throw new IllegalArgumentException("Empty values");
        }

        StringBuilder sql = new StringBuilder(120);
        sql.append("UPDATE ");
//		sql.append(CONFLICT_VALUES[conflictAlgorithm]);
        sql.append(table);
        sql.append(" SET ");

        // move all bind args to one array
        int setValuesSize = values.size();
        int bindArgsSize = (whereArgs == null) ? setValuesSize : (setValuesSize + whereArgs.length);
        Object[] bindArgs = new Object[bindArgsSize];
        int i = 0;
        for (String colName : values.keySet()) {
            sql.append((i > 0) ? "," : "");
            sql.append(colName);
            bindArgs[i++] = values.get(colName);
            sql.append("=?");
        }
        if (whereArgs != null) {
            for (i = setValuesSize; i < bindArgsSize; i++) {
                bindArgs[i] = whereArgs[i - setValuesSize];
            }
        }
        if (notNullOrEmpty(whereClause)) {
            sql.append(" WHERE ");
            sql.append(whereClause);
        }
        
        if (execSQL(sql.toString(), bindArgs))
			try {
				return connection.getChanges();
			} catch (com.almworks.sqlite4java.SQLiteException e) {}
        
        return -1;
	}

	public int delete(String table, String whereClause, String[] whereArgs) {
    	String sql = "DELETE FROM " + table +
            	(notNullOrEmpty(whereClause) ? " WHERE " + whereClause : "");
    	
        if (execSQL(sql, whereArgs))
			try {
				return connection.getChanges();
			} catch (com.almworks.sqlite4java.SQLiteException e) {}
        
        return -1;
	}

    public Cursor query(String table, String[] columns, String selection,
            String[] selectionArgs, String groupBy, String having,
            String orderBy) {

		return query(false, table, columns, selection, selectionArgs, groupBy,
		        having, orderBy, null /* limit */);
    }

	public Cursor query(boolean distinct, String table, String[] columns,
			String selection, String[] selectionArgs, 
			String groupBy, String having, String orderBy, String limit) throws SQLiteException 
	{
		try {
			String sql = SQLiteQueryBuilder.buildQueryString(
					distinct, table, columns, selection, groupBy, having, orderBy, limit);
			
			return rawQuery(sql, selectionArgs);
		} catch (SQLiteException e) {
			Log.e(TAG, "query", e);
			return null;
		}
	}

	/**
     * Sets the database version.
     *
     * @param version the new database version
     */
    public void setVersion(int version) {
        execSQL("PRAGMA user_version = " + version);
    }
    /**
     * Gets the database version.
     *
     * @return the database version
     */
    public int getVersion() {
        Cursor cursor = null;
        lock();
        try {
        	cursor = rawQuery("PRAGMA user_version;", null);
        	int version = 0;
            if (cursor != null && cursor.moveToFirst()) {
            	version = cursor.getInt(0);
            }
            return version;
        } catch (SQLiteException e) {
			Log.e(TAG, "getVersion", e);
			return 0;
		} finally {
            if (cursor != null) 
            	cursor.close();
            unlock();
        }
    }

    /**
     * Control whether or not the SQLiteDatabase is made thread-safe by using locks
     * around critical sections. This is pretty expensive, so if you know that your
     * DB will only be used by a single thread then you should set this to false.
     * The default is true.
     * @param lockingEnabled set to true to enable locks, false otherwise
     */
    public void setLockingEnabled(boolean lockingEnabled) {
        mLockingEnabled = lockingEnabled;
    }
    /**
     * If set then the SQLiteDatabase is made thread-safe by using locks
     * around critical sections
     */
    private boolean mLockingEnabled = false;
    
    void lock() {
    	if (mLockingEnabled) {
    		
    	}
    }
    
    void unlock() {
    	if (mLockingEnabled) {
    		
    	}
    }

    /**
     * Sets the locale for this database.  Does nothing if this database has
     * the NO_LOCALIZED_COLLATORS flag set or was opened read only.
     * @throws SQLException if the locale could not be set.  The most common reason
     * for this is that there is no collator available for the locale you requested.
     * In this case the database remains unchanged.
     */
    public void setLocale(Locale locale) {
        lock();
        try {
//            native_setLocale(locale.toString(), mFlags);
        } finally {
            unlock();
        }
    }
    
    public void beginTransaction() {
    	mTransactionIsSuccessful = false;
    	execSQL("BEGIN EXCLUSIVE;");
    }
	private boolean mTransactionIsSuccessful = false;
    public void setTransactionSuccessful() {
        mTransactionIsSuccessful = true;
    }
    public void endTransaction() {
        if (mTransactionIsSuccessful) {
            execSQL("COMMIT;");
        } else {
        	execSQL("ROLLBACK;");
        }
    }

    /**
     * Finds the name of the first table, which is editable.
     *
     * @param tables a list of tables
     * @return the first table listed
     */
    public static String findEditTable(String tables) {
        if (notNullOrEmpty(tables)) {
            // find the first word terminated by either a space or a comma
            int spacepos = tables.indexOf(' ');
            int commapos = tables.indexOf(',');

            if (spacepos > 0 && (spacepos < commapos || commapos < 0)) {
                return tables.substring(0, spacepos);
            } else if (commapos > 0 && (commapos < spacepos || spacepos < 0) ) {
                return tables.substring(0, commapos);
            }
            return tables;
        } else {
            throw new IllegalStateException("Invalid tables");
        }
    }
    
    public static final int OPEN_READWRITE = 0x00000000;
    public static final int OPEN_READONLY = 0x00000001;
    private static final int OPEN_READ_MASK = 0x00000001;

    private int mFlags;

	public boolean isReadOnly() {
        return (mFlags & OPEN_READ_MASK) == OPEN_READONLY;
	}
}

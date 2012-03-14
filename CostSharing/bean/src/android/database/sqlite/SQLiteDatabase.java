package android.database.sqlite;

import com.almworks.sqlite4java.*;
import com.applang.Util;

import android.content.ContentValues;
import android.database.Cursor;
import android.util.Log;

import java.io.File;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Exposes methods to manage a SQLite database.
 * @hide
 */
public class SQLiteDatabase {
    private static final String TAG = SQLiteDatabase.class.getSimpleName();

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
    
    CursorFactory cursorFactory = new CursorFactory() {
		
		@Override
		public Cursor newCursor(SQLiteConnection con, final SQLiteStatement stm) {
			return new Cursor()
			{
				int cnt = 0;
				
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
				
				@Override
				public int getCount() {
					return cnt;
				}
						
				@Override
				public boolean moveToFirst() {
			    	if (stm == null)
			    		return false;
			    	
					try {
						if (stm.hasStepped())
							stm.reset(false);
						
						return moveToNext();
					} catch (com.almworks.sqlite4java.SQLiteException e) {
						return false;
					}
				}
				
				@Override
				public boolean moveToNext() {
			    	if (stm == null)
			    		return false;
			    	
					try {
						return stm.step();
					} catch (com.almworks.sqlite4java.SQLiteException e) {
						return false;
					}
				}
				
				@Override
				public boolean isClosed() {
			    	if (stm != null)
			    		return stm.isDisposed();
			    	
					return true;
				}
				
				@Override
				public void close() {
			    	if (stm != null)
			    		stm.dispose();
				}
				
				@Override
				public boolean requery() {
					throw new UnsupportedOperationException("Not implemented");
				}
				
				@Override
				public boolean moveToPrevious() {
					throw new UnsupportedOperationException("Not implemented");
				}
				
				@Override
				public boolean moveToPosition(int position) {
					throw new UnsupportedOperationException("Not implemented");
				}
				
				@Override
				public boolean moveToLast() {
					throw new UnsupportedOperationException("Not implemented");
				}
				
				@Override
				public boolean move(int offset) {
					throw new UnsupportedOperationException("Not implemented");
				}
				
				@Override
				public boolean isNull(int columnIndex) {
					try {
						if (stm != null && stm.hasRow())
							return stm.columnNull(columnIndex);
					} catch (com.almworks.sqlite4java.SQLiteException e) {}
					
					return true;
				}
				
				@Override
				public boolean isLast() {
					throw new UnsupportedOperationException("Not implemented");
				}
				
				@Override
				public boolean isFirst() {
					throw new UnsupportedOperationException("Not implemented");
				}
				
				@Override
				public boolean isBeforeFirst() {
					throw new UnsupportedOperationException("Not implemented");
				}
				
				@Override
				public boolean isAfterLast() {
					throw new UnsupportedOperationException("Not implemented");
				}
				
				@Override
				public int getType(int columnIndex) {
					try {
						if (stm != null && stm.hasRow())
							return stm.columnType(columnIndex);
					} catch (com.almworks.sqlite4java.SQLiteException e) {}
					
					return -1;
				}
				
				@Override
				public String getString(int columnIndex) {
					try {
						if (stm != null && stm.hasRow())
							return stm.columnString(columnIndex);
					} catch (com.almworks.sqlite4java.SQLiteException e) {}
					
					return null;
				}
				
				@Override
				public int getInt(int columnIndex) {
					try {
						if (stm != null && stm.hasRow())
							return stm.columnInt(columnIndex);
					} catch (com.almworks.sqlite4java.SQLiteException e) {}
					
					return Integer.MIN_VALUE;
				}
				
				@Override
				public short getShort(int columnIndex) {
					try {
						if (stm != null && stm.hasRow())
							return Short.parseShort(stm.columnString(columnIndex));
					} catch (com.almworks.sqlite4java.SQLiteException e) {}
					
					return Short.MIN_VALUE;
				}
				
				@Override
				public long getLong(int columnIndex) {
					try {
						if (stm != null && stm.hasRow())
							return stm.columnLong(columnIndex);
					} catch (com.almworks.sqlite4java.SQLiteException e) {}
					
					return Long.MIN_VALUE;
				}
				
				@Override
				public float getFloat(int columnIndex) {
					try {
						if (stm != null && stm.hasRow())
							return Float.parseFloat(stm.columnString(columnIndex));
					} catch (com.almworks.sqlite4java.SQLiteException e) {}
					
					return Float.NaN;
				}
				
				@Override
				public double getDouble(int columnIndex) {
					try {
						if (stm != null && stm.hasRow())
							return stm.columnDouble(columnIndex);
					} catch (com.almworks.sqlite4java.SQLiteException e) {}
					
					return Double.NaN;
				}
				
				@Override
				public int getPosition() {
					throw new UnsupportedOperationException("Not implemented");
				}
				
				@Override
				public String[] getColumnNames() {
					throw new UnsupportedOperationException("Not implemented");
				}
				
				@Override
				public String getColumnName(int columnIndex) {
					try {
						if (stm != null && stm.hasRow())
							return stm.getColumnName(columnIndex);
					} catch (com.almworks.sqlite4java.SQLiteException e) {}
					
					return null;
				}
				
				@Override
				public int getColumnIndex(String columnName) {
					throw new UnsupportedOperationException("Not implemented");
				}
				
				@Override
				public int getColumnIndexOrThrow(String columnName)	throws IllegalArgumentException {
					throw new UnsupportedOperationException("Not implemented");
				}
				
				@Override
				public int getColumnCount() {
					try {
						if (stm != null && stm.hasRow())
							return stm.columnCount();
					} catch (com.almworks.sqlite4java.SQLiteException e) {}
					
					return 0;
				}
				
				@Override
				public byte[] getBlob(int columnIndex) {
					try {
						if (stm != null && stm.hasRow())
							return stm.columnBlob(columnIndex);
					} catch (com.almworks.sqlite4java.SQLiteException e) {}
					
					return null;
				}
				
				@Override
				public void deactivate() {
					throw new UnsupportedOperationException("Not implemented");
				}
			};
		}
	};

    public static SQLiteDatabase openDatabase(String path, CursorFactory factory, int flags) {
    	SQLiteDatabase db = new SQLiteDatabase();
		try {
			db.connection = new SQLiteConnection(new File(path));
			db.connection.open((flags & CREATE_IF_NECESSARY) > 0);
		} catch (com.almworks.sqlite4java.SQLiteException e) {
			Log.e(TAG, "execSQL", e);
		}
		
		if (factory != null)
			db.cursorFactory = factory;
		
        return db;
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

	boolean execSql(String sql, Object[] bindArgs) {
		SQLiteStatement stm = null;
    	try {
			stm = doBind(connection.prepare(sql), bindArgs);
			stm.step();
			return true;
		} catch (com.almworks.sqlite4java.SQLiteException e) {
			Log.e(TAG, "execSQL", e);
		}
    	finally {
	    	if (stm != null)
	    		stm.dispose();
    	}
		return false;
	}

	public Cursor rawQuery(String sql, String[] selectionArgs) throws SQLiteException {
		SQLiteStatement stm = null;
	    try {
	    	stm = doBind(connection.prepare(sql), selectionArgs);
	    	return cursorFactory.newCursor(connection, stm);
		} catch (com.almworks.sqlite4java.SQLiteException e) {
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
        
        if (execSql(sql.toString(), bindArgs))
			try {
				return connection.getLastInsertId();
			} catch (com.almworks.sqlite4java.SQLiteException e) {}
        
        return -1;
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
        if (Util.notNullOrEmpty(whereClause)) {
            sql.append(" WHERE ");
            sql.append(whereClause);
        }
        
        if (execSql(sql.toString(), bindArgs))
			try {
				return connection.getChanges();
			} catch (com.almworks.sqlite4java.SQLiteException e) {}
        
        return -1;
	}

	public int delete(String table, String whereClause, String[] whereArgs) {
    	String sql = "DELETE FROM " + table +
            	(Util.notNullOrEmpty(whereClause) ? " WHERE " + whereClause : "");
    	
        if (execSql(sql, whereArgs))
			try {
				return connection.getChanges();
			} catch (com.almworks.sqlite4java.SQLiteException e) {}
        
        return -1;
	}

	public Cursor query(boolean distinct, String table, String[] columns,
			String selection, String[] selectionArgs, 
			String groupBy, String having, String orderBy, String limit) throws SQLiteException 
	{
        String sql = buildQueryString(
                distinct, table, columns, selection, groupBy, having, orderBy, limit);
        
		return rawQuery(sql, selectionArgs);
	}

    private static final Pattern sLimitPattern =
            Pattern.compile("\\s*\\d+\\s*(,\\s*\\d+\\s*)?");
    
    /**
     * Build an SQL query string from the given clauses.
     *
     * @param distinct true if you want each row to be unique, false otherwise.
     * @param tables The table names to compile the query against.
     * @param columns A list of which columns to return. Passing null will
     *            return all columns, which is discouraged to prevent reading
     *            data from storage that isn't going to be used.
     * @param where A filter declaring which rows to return, formatted as an SQL
     *            WHERE clause (excluding the WHERE itself). Passing null will
     *            return all rows for the given URL.
     * @param groupBy A filter declaring how to group rows, formatted as an SQL
     *            GROUP BY clause (excluding the GROUP BY itself). Passing null
     *            will cause the rows to not be grouped.
     * @param having A filter declare which row groups to include in the cursor,
     *            if row grouping is being used, formatted as an SQL HAVING
     *            clause (excluding the HAVING itself). Passing null will cause
     *            all row groups to be included, and is required when row
     *            grouping is not being used.
     * @param orderBy How to order the rows, formatted as an SQL ORDER BY clause
     *            (excluding the ORDER BY itself). Passing null will use the
     *            default sort order, which may be unordered.
     * @param limit Limits the number of rows returned by the query,
     *            formatted as LIMIT clause. Passing null denotes no LIMIT clause.
     * @return the SQL query string
     */
    public static String buildQueryString(
            boolean distinct, String tables, String[] columns, String where,
            String groupBy, String having, String orderBy, String limit) {
        if (!Util.notNullOrEmpty(groupBy) && Util.notNullOrEmpty(having)) {
            throw new IllegalArgumentException(
                    "HAVING clauses are only permitted when using a groupBy clause");
        }
        if (Util.notNullOrEmpty(limit) && !sLimitPattern.matcher(limit).matches()) {
            throw new IllegalArgumentException("invalid LIMIT clauses:" + limit);
        }

        StringBuilder query = new StringBuilder(120);

        query.append("SELECT ");
        if (distinct) {
            query.append("DISTINCT ");
        }
        if (columns != null && columns.length != 0) {
            appendColumns(query, columns);
        } else {
            query.append("* ");
        }
        query.append("FROM ");
        query.append(tables);
        appendClause(query, " WHERE ", where);
        appendClause(query, " GROUP BY ", groupBy);
        appendClause(query, " HAVING ", having);
        appendClause(query, " ORDER BY ", orderBy);
        appendClause(query, " LIMIT ", limit);

        return query.toString();
    }

    private static void appendClause(StringBuilder s, String name, String clause) {
        if (Util.notNullOrEmpty(clause)) {
            s.append(name);
            s.append(clause);
        }
    }

    /**
     * Add the names that are non-null in columns to s, separating
     * them with commas.
     */
    public static void appendColumns(StringBuilder s, String[] columns) {
        int n = columns.length;

        for (int i = 0; i < n; i++) {
            String column = columns[i];

            if (column != null) {
                if (i > 0) {
                    s.append(", ");
                }
                s.append(column);
            }
        }
        s.append(' ');
    }
}

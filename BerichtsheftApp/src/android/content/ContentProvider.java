package android.content;

import java.io.File;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;

import static com.applang.Util.*;
import static com.applang.Util1.*;
import static com.applang.Util2.*;

public class ContentProvider {
    private Context mContext = null;

    public void setContext(Context context) {
		this.mContext = context;
	}

	public final Context getContext() {
        return mContext;
    }

	public boolean onCreate() {
		return false;
	}

	public String getType(Uri uri) {
		if (notNullOrEmpty(uri.getPath()) && notNullOrEmpty(uri.getFragment())) {
			if (notNullOrEmpty(uri.getQuery()))
				return ContentResolver.CURSOR_ITEM_BASE_TYPE;
			else
				return ContentResolver.CURSOR_DIR_BASE_TYPE;
		}
		return ContentResolver.RAW;
	}
    
    public SQLiteOpenHelper openHelper() {
    	return null;
    }

	protected SQLiteDatabase mDb = null;
	protected String mTable = "";
	
	protected boolean open(Uri uri, int mode) {
		if (mDb != null && mDb.isOpen())
			return true;
		else if (mode < 0)
			return false;
		
		SQLiteOpenHelper oh = openHelper();
		if (oh != null) {
			switch (mode) {
			case SQLiteDatabase.OPEN_READONLY:
				mDb = oh.getReadableDatabase();
				break;
			default:
				mDb = oh.getWritableDatabase();
			}
		}
		else {
			String path = uri.getPath();
			File file = new File(path);
			if (!fileExists(file))
				return false;
			mDb = SQLiteDatabase.openDatabase(
					file.getPath(), 
					null, 
					mode);
		}
		
		mTable = dbTableName(uri);
		if (nullOrEmpty(mTable))
			mTable = "sqlite_master";
		
		return open(uri, -1);
	}
	
	public void close() {
		if (mDb != null) {
			mDb.close();
			mDb = null;
		}
	}

	@Override
	protected void finalize() throws Throwable {
		close();
		super.finalize();
	}
	
	public String sql = null;

	public Cursor rawQuery(Uri uri, String...sql) {
		Cursor cursor = null;
		if (open(uri, SQLiteDatabase.OPEN_READONLY)) {
			String[] args = strings();
			if (isAvailable(0, sql) && sql[0].length() > 0) {
				this.sql = sql[0];
				args = arrayslice(sql, 1, sql.length - 1);
			}
			else
				this.sql = "select * from " + mTable;
			cursor = mDb.rawQuery(this.sql, args);
		}
		return cursor;
	}

	public Cursor query(Uri uri, String[] projection,
			String selection, String[] selectionArgs,
			String sortOrder) {
		Cursor cursor = null;
		if (open(uri, SQLiteDatabase.OPEN_READONLY)) {
			cursor = mDb.query(mTable, 
					projection, 
					selection, selectionArgs, 
					null, null, 
					sortOrder);
			if (cursor != null)
				cursor.setNotificationUri(getContext().getContentResolver(), uri);
		}
		return cursor;
	}

	public Uri insert(Uri uri, ContentValues initialValues) {
		if (open(uri, SQLiteDatabase.OPEN_READWRITE)) {
			long rowId = mDb.insert(mTable, null, initialValues);
	        getContext().getContentResolver().notifyChange(uri, null);
			return ContentUris.withAppendedId(uri, rowId);
		}
		return null;
	}

	public int delete(Uri uri, String where, String[] whereArgs) {
		int retval = 0;
		if (open(uri, SQLiteDatabase.OPEN_READWRITE)) {
			retval = mDb.delete(mTable, where, whereArgs);
	        getContext().getContentResolver().notifyChange(uri, null);
		}
		return retval;
	}

	public int update(Uri uri, ContentValues values, String where, String[] whereArgs) {
		int retval = 0;
		if (open(uri, SQLiteDatabase.OPEN_READWRITE)) {
			retval = mDb.update(mTable, values, where, whereArgs);
	        getContext().getContentResolver().notifyChange(uri, null);
		}
		return retval;
	}
}

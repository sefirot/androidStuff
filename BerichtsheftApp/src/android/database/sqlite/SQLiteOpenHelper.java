package android.database.sqlite;

import android.content.Context;

public class SQLiteOpenHelper {

	public SQLiteOpenHelper(Context context, String databaseName, Object object, int databaseVersion) {
	}

	public void onCreate(SQLiteDatabase db) {
	}

	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
	}

	public SQLiteDatabase getWritableDatabase() {
		return null;
	}

	public SQLiteDatabase getReadableDatabase() {
		return null;
	}

	public void close() {
	}
}

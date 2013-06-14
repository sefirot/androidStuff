package android.content;

import java.io.File;

//import android.database.sqlite.SQLiteDatabase;
//import android.database.sqlite.SQLiteDatabase.CursorFactory;

public abstract class Context {

    public abstract String[] fileList();
    public abstract File getDir(String name, int mode);

//	public abstract SQLiteDatabase openOrCreateDatabase(String name, int mode, CursorFactory factory);

    public abstract File getDatabasePath(String name);
    public abstract String[] databaseList();
    public abstract boolean deleteDatabase(String name);
	public abstract ContentResolver getContentResolver();
}

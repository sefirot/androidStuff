package android.content;

import android.database.Cursor;
import android.net.Uri;

public class ContentResolver
{
    public static final String CURSOR_DIR_BASE_TYPE = "vnd.android.cursor.dir";
    public static final String CURSOR_ITEM_BASE_TYPE = "vnd.android.cursor.item";

	public void notifyChange(Uri uri, Object object) {
		// TODO Auto-generated method stub
		
	}
    
	public void update(Uri uri, ContentValues values, String where, String[] whereArgs) {
		// TODO Auto-generated method stub
		
	}

	public Cursor query(Uri uri, String[] strings, String selection,
			String[] selectionArgs, String defaultSortOrder) {
		// TODO Auto-generated method stub
		return null;
	}

}

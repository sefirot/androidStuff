package android.content;

import android.database.Cursor;
import android.net.Uri;

public class ContentProvider {
    private Context mContext = null;

    public final Context getContext() {
        return mContext;
    }

	public String getType(Uri uri) {
		// TODO Auto-generated method stub
		return null;
	}

	public boolean onCreate() {
		// TODO Auto-generated method stub
		return false;
	}

	public Cursor query(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder) {
		// TODO Auto-generated method stub
		return null;
	}

	public Uri insert(Uri uri, ContentValues initialValues) {
		// TODO Auto-generated method stub
		return null;
	}

	public int delete(Uri uri, String where, String[] whereArgs) {
		// TODO Auto-generated method stub
		return 0;
	}

	public int update(Uri uri, ContentValues values, String where,
			String[] whereArgs) {
		// TODO Auto-generated method stub
		return 0;
	}

}

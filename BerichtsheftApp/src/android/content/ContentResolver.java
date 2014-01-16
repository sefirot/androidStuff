package android.content;

import static com.applang.Util1.*;

import java.io.File;
import java.util.Observable;

import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

public class ContentResolver extends Observable
{
    private static final String TAG = ContentResolver.class.getSimpleName();

	public static final String CURSOR_DIR_BASE_TYPE = "vnd.android.cursor.dir";
    public static final String CURSOR_ITEM_BASE_TYPE = "vnd.android.cursor.item";
    public static final String RAW = "raw";
    public static final String SCHEME_CONTENT = "content";
    public static final String SCHEME_FILE = "file";
    
    public ContentResolver(Context context) {
    	mContext = context;
	}

    Context mContext;

    public ContentProvider acquireProvider(final Uri uri)
    {
    	Context context = mContext;
    	contentProvider = new ContentProvider();
    	String flavor = null;
    	try {
    		if (SCHEME_CONTENT.equals(uri.getScheme())) {
    			flavor = uri.getAuthority();
    			if (flavor != null) {
    				Class<?> c = Class.forName(flavor + "Provider");
    				contentProvider = (ContentProvider) c.newInstance();
    			}
    		} else if (SCHEME_FILE.equals(uri.getScheme())) {
    			final File file = new File(uri.getPath());
    			context = new Context() {
    				{
    					setPackageInfo("", file.getParent());
    				}
    				
    				@Override
    				public ContentResolver getContentResolver() {
    					return ContentResolver.this;
    				}
    			};
    		}
    	} catch (Exception e) {
    		Log.e(TAG, "acquireProvider", e);
    	}
    	context.setFlavor(flavor);
    	contentProvider.setContext(context);
    	contentProvider.onCreate();
		com.applang.Util2.no_println("contentProvider", contentProvider.getClass());
		return contentProvider;
    }

    public ContentProvider contentProvider = null;
    
	public Cursor rawQuery(Uri uri, String...sql) {
		return acquireProvider(uri).rawQuery(uri, sql);
    }

	public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
		if (selection != null && projection == null && selectionArgs == null && sortOrder == null)
			return rawQuery(uri, selection);
		else if (hasAuthority(uri) || projection != null || selectionArgs != null || sortOrder != null)
			return acquireProvider(uri).query(uri, projection, selection, selectionArgs, sortOrder);
		else
			return rawQuery(uri, selection);
	}

	public int delete(Uri uri, String where, String[] whereArgs) {
		return acquireProvider(uri).delete(uri, where, whereArgs);
	}

	public Uri insert(Uri uri, ContentValues initialValues) {
		return acquireProvider(uri).insert(uri, initialValues);
	}
    
	public int update(Uri uri, ContentValues values, String where, String[] whereArgs) {
        return acquireProvider(uri).update(uri, values, where, whereArgs);
	}

    /**
     * Return the MIME type of the given content URL.
     *
     * @param url A Uri identifying content (either a list or specific type),
     * using the content:// scheme.
     * @return A MIME type for the content, or null if the URL is invalid or the type is unknown
     */
	public String getType(Uri uri) {
		return acquireProvider(uri).getType(uri);
	}

	public void notifyChange(Uri uri, ContentObserver observer) {
		setChanged();
		notifyObservers(uri);
	}

    public final void registerContentObserver(Uri uri, boolean notifyForDescendents, ContentObserver observer) {
    	addObserver(observer);
    }
    
	public final void unregisterContentObserver(ContentObserver observer) {
		if (countObservers() > 0)
			deleteObserver(observer);
	}
}

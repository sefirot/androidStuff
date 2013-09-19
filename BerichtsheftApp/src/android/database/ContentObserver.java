package android.database;

import java.util.Observable;
import java.util.Observer;

import static com.applang.Util.*;

import android.net.Uri;
import android.util.Log;

public class ContentObserver implements Observer
{
	private static final String TAG = ContentObserver.class.getSimpleName();

	public ContentObserver(Job<Uri> followUp, Object...params) {
		this.followUp = followUp;
		this.params = params;
	}
	
	Job<Uri> followUp = null;
	Object[] params = null;

	@Override
	public void update(Observable o, Object arg) {
		Uri uri = (Uri) arg;
		if (followUp != null) 
			try {
				followUp.perform(uri, params);
			} catch (Exception e) {
				Log.e(TAG, "update", e);
			}
	}

}

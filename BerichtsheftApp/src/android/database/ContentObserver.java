package android.database;

import java.util.Observable;
import java.util.Observer;

import android.os.Handler;

public class ContentObserver implements Observer
{
	public ContentObserver(Handler notifyHandler) {
		// TODO Auto-generated constructor stub
	}

	@Override
	public void update(Observable o, Object arg) {
        onChange(arg);
        onChange(true);
	}
	
    public void onChange(Object arg) {}

	public void onChange(boolean selfChange) {
	}
}

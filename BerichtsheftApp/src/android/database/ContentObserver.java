package android.database;

import java.util.Observable;
import java.util.Observer;

public class ContentObserver implements Observer
{
	@Override
	public void update(Observable o, Object arg) {
        onChange(arg);
	}
	
    public void onChange(Object arg) {}
}

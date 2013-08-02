package android.widget;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JComponent;
import javax.swing.JTextArea;
import javax.swing.Popup;
import javax.swing.PopupFactory;
import javax.swing.Timer;

import android.app.Activity;
import android.content.Context;
import android.view.View;

public class Toast {

    public static final int LENGTH_ZERO = -1;
    public static final int LENGTH_SHORT = 0;
    public static final int LENGTH_LONG = 1;
    
    final Context mContext;
    JComponent mComponent;
    int mDuration;
    
    public Toast(Context context) {
        mContext = context;
    }
    
    public void setView(View view) {
    	mComponent = view.getComponent();
    }

    public static Toast makeText(Context context, CharSequence text, int duration) {
        Toast toast = new Toast(context);

        toast.mComponent = new JTextArea(text.toString());
        toast.mDuration = duration + 1;

        return toast;
    }
	
    public synchronized void show() {
    	if (mDuration < 1)
    		return;
		PopupFactory factory = PopupFactory.getSharedInstance();
		final Popup popup = factory.getPopup(
				Activity.frame,
				mComponent, 
				mContext.location.x + 10, 
				mContext.location.y + 10);
		popup.show();
		ActionListener hider = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				popup.hide();
			}
		};
		Timer timer = new Timer(mDuration * 1000, hider);
		timer.start();
    }
}

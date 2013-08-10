package android.view;

import static com.applang.Util.*;
import static com.applang.Util1.*;

import java.awt.Container;

import javax.swing.JPanel;

import android.content.Context;

public class ViewGroup extends View {

	public ViewGroup(Context context) {
		super(context);
	}

	public static class LayoutParams {
		public static final int FILL_PARENT = -1;
		public static final int WRAP_CONTENT = -2;
		
        public int width, height;
        
        public LayoutParams(int width, int height) {
            this.width = width;
            this.height = height;
		}
	}
	
	ValList views = new ValList();

    public void addView(View child, LayoutParams params) {
    	views.add(child);
        child.setLayoutParams(params);
	}

    public int getChildCount() {
    	return views.size();
    }
    
    public View getChildAt(int index) {
    	return (View) views.get(index);
    }
    
    public View findViewById(final int id) {
    	Object[] params = iterateViews(this, 
			new Function<Object[]>() {
				public Object[] apply(Object... params) {
					View v = param(null, 0, params);
					Object[] parms = param(null, 2, params);
					if (v.findViewTraversal(id) != null)
						return new Object[] {v};
					else
						return parms;
				}
			}, 0);
    	return param(null, 0, params);
    }
    
    public Container getLayout() {
    	return new JPanel();
    }
}

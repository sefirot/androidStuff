package android.view;

import java.awt.Container;

import javax.swing.JPanel;

import android.content.Context;

import static com.applang.Util.*;
import static com.applang.Util1.*;

public class ViewGroup extends View implements ViewManager
{
	@Override
	public String toString() {
		return viewHierarchy(this);
	}

	public ViewGroup(Context context) {
		super(context);
		setComponent(new JPanel());
	}

	public static class LayoutParams {
		public static final int FILL_PARENT = -1;
		public static final int WRAP_CONTENT = -2;
		
		public static int value(String s) {
			if ("fill_parent".equals(s))
				return FILL_PARENT;
			else if ("wrap_content".equals(s))
				return WRAP_CONTENT;
			else
				return toInt(0, stripUnits(s));
		}
		
        public int width, height;
        
        public LayoutParams(int width, int height) {
            this.width = width;
            this.height = height;
		}
	}

	public static class MarginLayoutParams extends LayoutParams {
        public void setMargins(int left, int top, int right, int bottom) {
            leftMargin = left;
            topMargin = top;
            rightMargin = right;
            bottomMargin = bottom;
        }
		
        public int leftMargin, topMargin, rightMargin, bottomMargin;
        
        public MarginLayoutParams(int width, int height) {
            super(width, height);
		}
    	
    	public static int value(Context context, String s) {
			Object o = context.getResources().getXMLResourceItem(s);
			if (o != null)
				s = stringValueOf(o);
			return toInt(0, stripUnits(s));
    	}
	}
	
	ValList views = vlist();

	@Override
    public void addView(View view, LayoutParams params) {
    	views.add(view);
    	view.setLayoutParams(params);
	}

	@Override
	public void removeView(View view) {
    	views.remove(view);
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

	@Override
    public void updateViewLayout(View view, ViewGroup.LayoutParams params) {
        if (!checkLayoutParams(params)) {
            throw new IllegalArgumentException("Invalid LayoutParams supplied to " + this);
        }
        if (!views.contains(view)) {
            throw new IllegalArgumentException("Given view not a child of " + this);
        }
        view.setLayoutParams(params);
    }

    protected boolean checkLayoutParams(ViewGroup.LayoutParams p) {
        return  p != null;
    }
    
    public Container getContainer() {
    	return getComponent();
    }

}

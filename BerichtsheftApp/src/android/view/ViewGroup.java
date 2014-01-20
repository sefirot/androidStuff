package android.view;

import java.awt.Component;
import java.awt.Container;
import java.io.Writer;

import javax.swing.Box;
import javax.swing.BoxLayout;

import android.content.Context;

import static com.applang.Util.*;
import static com.applang.Util1.*;
import static com.applang.Util2.write;
import static com.applang.Util2.write_assoc;

public class ViewGroup extends View implements ViewManager
{
	@Override
	public String toString() {
		return viewHierarchy(this);
	}

	public ViewGroup(Context context) {
		super(context);
		setComponent(new Container());
	}

	public static class LayoutParams {
		@Override
		public String toString() {
			Writer writer = write(null, identity(this));
			writer = write_assoc(writer, "dimension", objects(width, height), 1);
			return writer.toString();
		}

		public static final int FILL_PARENT = -1;
		public static final int WRAP_CONTENT = -2;
		
		public static int value(Context context, String s) {
			if ("fill_parent".equals(s))
				return FILL_PARENT;
			else if ("wrap_content".equals(s))
				return WRAP_CONTENT;
			else {
				Object o = context.getResources().getXMLResourceItem(s);
				if (o instanceof Integer)
					return (Integer) o;
				else 
					return toInt(0, stripUnits(s));
			}
		}
		
        public int width, height;
        
        public LayoutParams(int width, int height) {
            this.width = width;
            this.height = height;
		}
	}

	public static class MarginLayoutParams extends LayoutParams {
		@Override
		public String toString() {
			Writer writer = write(null, super.toString());
			writer = write_assoc(writer, "margin", objects(leftMargin, topMargin, rightMargin, bottomMargin), 1);
			return writer.toString();
		}

    	public static int value(Context context, String s) {
			Object o = context.getResources().getXMLResourceItem(s);
			if (o instanceof Integer)
				return (Integer) o;
			else 
				return toInt(0, stripUnits(stringValueOf(o)));
    	}
		
        public int leftMargin, topMargin, rightMargin, bottomMargin;
        
        public void setMargins(int left, int top, int right, int bottom) {
            leftMargin = left;
            topMargin = top;
            rightMargin = right;
            bottomMargin = bottom;
        }
    	
        public MarginLayoutParams(int width, int height) {
            super(width, height);
		}
        
        public Component strutsOuterFirst(int orientation) {
        	return orientation == BoxLayout.X_AXIS ? 
        			Box.createHorizontalStrut(leftMargin) :
        			Box.createVerticalStrut(topMargin);
        }
        
        public Component strutsInnerFirst(int orientation) {
        	return orientation == BoxLayout.Y_AXIS ? 
        			Box.createHorizontalStrut(leftMargin) :
        			Box.createVerticalStrut(topMargin);
        }
        
        public Component strutsInnerLast(int orientation) {
        	return orientation == BoxLayout.Y_AXIS ? 
        			Box.createHorizontalStrut(rightMargin) :
        			Box.createVerticalStrut(bottomMargin);
        }
        
        public Component strutsOuterLast(int orientation) {
        	return orientation == BoxLayout.X_AXIS ? 
        			Box.createHorizontalStrut(rightMargin) :
        			Box.createVerticalStrut(bottomMargin);
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
    	return (Container) getComponent();
    }

}

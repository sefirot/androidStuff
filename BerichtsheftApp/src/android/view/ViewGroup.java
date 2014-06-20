package android.view;

import java.awt.Component;
import java.awt.Container;
import java.io.Writer;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.SpringLayout;

import com.applang.Util.Function;

import android.content.Context;
import android.content.res.Resources;
import android.util.AttributeSet;

import static com.applang.Util.*;
import static com.applang.Util1.*;
import static com.applang.Util2.*;
import static com.applang.SwingUtil.*;

public class ViewGroup extends View implements ViewManager
{
	public ViewGroup(Context context) {
		super(context, null);
	}

	@Override
	protected void create() {
		setComponent(new Container());
	}

	@Override
	public String toString() {
		return viewHierarchy(this);
	}

	public static class LayoutParams
	{
		@Override
		public String toString() {
			Writer writer = write(null, identity(this));
			writer = write_assoc(writer, "dimension", objects(width, height), 1);
			return writer.toString();
		}

		public static final int FILL_PARENT = -1;
		public static final int WRAP_CONTENT = -2;
		
		public static int dimensionalValue(Context context, String s) {
			if ("fill_parent".equals(s))
				return FILL_PARENT;
			else if ("wrap_content".equals(s))
				return WRAP_CONTENT;
			else 
				return Resources.dimensionalValue(context, s);
		}
		
        public int width = WRAP_CONTENT, height = WRAP_CONTENT;
        
        public LayoutParams(Context context, AttributeSet attrs) {
			width = dimensionalValue(context, attrs.getAttributeValue("android:layout_width"));
			height = dimensionalValue(context, attrs.getAttributeValue("android:layout_height"));
		}
        
        public LayoutParams(int width, int height) {
            this.width = width;
            this.height = height;
		}
	}

	public static class MarginLayoutParams extends LayoutParams
	{
		@Override
		public String toString() {
			Writer writer = write(null, super.toString());
			writer = write_assoc(writer, "margin", objects(leftMargin, topMargin, rightMargin, bottomMargin), 1);
			return writer.toString();
		}
		
        public int leftMargin, topMargin, rightMargin, bottomMargin;
        
        public void setMargins(int left, int top, int right, int bottom) {
            leftMargin = left;
            topMargin = top;
            rightMargin = right;
            bottomMargin = bottom;
        }
        
        public int getMargin(String edge) {
        	switch (asList(edges()).indexOf(edge)) {
			case 0:
				return leftMargin;
			case 1:
				return topMargin;
			case 2:
				return rightMargin;
			case 3:
				return bottomMargin;
			default:
				throw new RuntimeException(String.format("no such edge : %s", edge));
			}
        }
        
        public void setMargin(String edge, int value) {
        	switch (asList(edges()).indexOf(edge)) {
			case 0:
				leftMargin = value;
				break;
			case 1:
				topMargin = value;
				break;
			case 2:
				rightMargin = value;
				break;
			case 3:
				bottomMargin = value;
				break;
			default:
				throw new RuntimeException(String.format("no such edge : %s", edge));
			}
        }
    	
        public MarginLayoutParams(int width, int height) {
            super(width, height);
		}
 
        public MarginLayoutParams(Context context, AttributeSet attrs) {
        	super(context, attrs);
        	leftMargin = Resources.dimensionalValue(context, attrs.getAttributeValue("android:layout_marginLeft"));
        	topMargin = Resources.dimensionalValue(context, attrs.getAttributeValue("android:layout_marginTop"));
        	rightMargin = Resources.dimensionalValue(context, attrs.getAttributeValue("android:layout_marginRight"));
        	bottomMargin = Resources.dimensionalValue(context, attrs.getAttributeValue("android:layout_marginBottom"));
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
	
	protected ValList views = vlist();

    public int getChildCount() {
    	return views.size();
    }
    
    public View getChildAt(int index) {
    	return (View) views.get(index);
    }

	@Override
    public void addView(View view, LayoutParams params) {
    	views.add(view);
    	view.setParent(this);
    	view.setLayoutParams(params);
	}

	@Override
	public void removeView(View view) {
		view.setParent(null);
    	views.remove(view);
	}
    
    @Override
    public View findViewById(final int id) {
		if (findViewByIdTraversal(id) != null)
			return this;
    	Object[] params = iterateViews(this, 0, 
			new Function<Object[]>() {
				public Object[] apply(Object... params) {
					View v = param(null, 0, params);
					Object[] parms = param(null, 2, params);
					if (v.findViewByIdTraversal(id) != null)
						return objects(v);
					else
						return parms;
				}
			});
    	return param(null, 0, params);
    }

    @Override
    public View findViewWithTag(final Object tag, final Object...params) {
		if (findViewWithTagTraversal(tag, params) != null)
			return this;
    	Object[] findings = iterateViews(this, 0, 
			new Function<Object[]>() {
				public Object[] apply(Object... params) {
					View v = param(null, 0, params);
					Object[] parms = param(null, 2, params);
					if (v.findViewWithTagTraversal(tag, parms) != null)
						return arrayextend(parms, true, v);
					else
						return parms;
				}
			}, params);
    	return param(null, 0, findings);
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

	public void preLayout() {
	}

	public void doLayout(View view) {
	}

	public Container doLayout() {
		return getContainer();
	}

	public static String[] edges() {
		return strings(SpringLayout.WEST, SpringLayout.NORTH, SpringLayout.EAST, SpringLayout.SOUTH);
	}
	
    public static Container build(View view, boolean finalLayout) {
    	Container container = null;
    	if (view instanceof ViewGroup) {
	    	ViewGroup viewGroup = (ViewGroup) view;
	    	viewGroup.preLayout();
	    	container = viewGroup.getContainer();
			for (int i = 0; i < viewGroup.getChildCount(); i++) {
				view = viewGroup.getChildAt(i);
				build(view, false);
				View parent = view.getParent();
				if (parent instanceof ViewGroup)
					((ViewGroup) parent).doLayout(view);
				container.add(view.getComponent());
			}
			if (finalLayout) {
				viewGroup.applyAttributes();
				iterateViews(viewGroup, 0, 
					new Function<Object[]>() {
						public Object[] apply(Object... params) {
							View v = param(null, 0, params);
							if (v != null)
								v.applyAttributes();
							return null;
						}
					});
				container = viewGroup.doLayout();
				printContainer("build", container, DIAG_OFF);
			}
    	}
    	return container;
    }

}

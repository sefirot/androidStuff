package android.view;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;

import javax.swing.JComponent;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;

import android.content.Context;
import android.content.res.Resources;
import android.util.AttributeSet;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewGroup.MarginLayoutParams;

import static com.applang.Util.*;
import static com.applang.Util2.*;
import static com.applang.SwingUtil.*;

public class View
{
	public static int uniqueCounter = 0;
	
	public View setTag(Object tag) {
		if (component != null) {
			String name = stringValueOf(tag) + (++uniqueCounter);
			component.setName(name);
		}
		return this;
	}
	
	@SuppressWarnings("unchecked")
	protected <T extends Component> T taggedComponent() {
		Component component = getComponent();
		if (component == null)
			return null;
		else if ((nullOrEmpty(component.getName())) && component instanceof Container) {
			component = findFirstComponent((Container) component, wildcardRegex("*"));
		}
		return (T) component;
	}
   
	public Object getTag() {
		Component component = taggedComponent();
		if (component == null)
			return null;
		else 
			return component.getName();
	}

    public View findViewWithTag(Object tag) {
        if (tag == null) {
            return null;
        }
        return findViewWithTagTraversal(tag);
    }
    
    protected View findViewWithTagTraversal(Object tag) {
        String regex = wildcardRegex(tag);
		String string = stringValueOf(getTag());
		if (string.matches(regex)) {
            return this;
        }
        return null;
    }
	
	private int mId;
	
	public int getId() {
		return mId;
	}
	
	public View setId(int id) {
		mId = id;
		return this;
	}

    public View findViewById(int id) {
        if (id < 0) {
            return null;
        }
        return findViewTraversal(id);
    }

    protected View findViewTraversal(int id) {
        if (id == mId) {
            return this;
        }
        return null;
    }
	
	public View(Component component) {
		this.component = component;
	}
	
	private Component component = null;

	public Component getComponent() {
		return component;
	}

	public void setComponent(Component component) {
		this.component = component;
	}

	private Context mContext = null;

	public Context getContext() {
		return mContext;
	}

	public View(Context context, AttributeSet attrs) {
		mContext = context;
		attributeSet = attrs;
		setId(0);
		String tag = "view";
		if (attributeSet != null) {
			tag = attributeSet.getIdAttribute();
			inputType = attributeSet.getAttributeValue("android:inputType");
		}
		create();
		setTag(tag);
    }
	
	public AttributeSet attributeSet = null;
	
	public void applyAttributes() {
		if (attributeSet != null) {
			for (int i = 0; i < attributeSet.getAttributeCount(); i++) {
				String name = attributeSet.getAttributeName(i);
				String value = attributeSet.getAttributeValue(i);
				Dimension size = component.getPreferredSize();
				if ("android:width".equals(name)) {
					int width = Resources.dimensionalValue(mContext, value);
					component.setPreferredSize(new Dimension(width, size.height));
				}
				else if ("android:height".equals(name)) {
					int height = Resources.dimensionalValue(mContext, value);
					component.setPreferredSize(new Dimension(size.width, height));
				}
				else if ("android:background".equals(name)) {
					int color= Resources.colorValue(mContext, value);
					setBackgroundColor(color);
				}

			}
			attributeSet = null;
		}
	}
	
	protected String inputType = null;

	protected void create(Object... params) {
	}

	private LayoutParams mLayoutParams;
	
    public LayoutParams getLayoutParams() {
        return mLayoutParams;
    }
    
	public void setLayoutParams(LayoutParams params) {
		mLayoutParams = params;
		if (component != null) {
			Dimension size = component.getPreferredSize();
			if (mLayoutParams instanceof MarginLayoutParams) {
				MarginLayoutParams margs = (MarginLayoutParams) mLayoutParams;
				Dimension dim = new Dimension(params.width, params.height);
				if (dim.width > -1) {
					dim.width -= margs.leftMargin + margs.rightMargin;
				}
				if (dim.height > -1) {
					dim.height -= margs.topMargin + margs.bottomMargin;
				}
				if (dim.width > -1) 
					size.width = dim.width;
				if (dim.height > -1) 
					size.height = dim.height;
			}
			component.setPreferredSize(size);
		}
	}

	public void setPadding(int left, int top, int right, int bottom) {
		if (component != null && component instanceof JComponent) {
		    Border padding = new EmptyBorder(top, left, bottom, right);
		    JComponent jc = (JComponent) component;
		    Border border = jc.getBorder();
		    jc.setBorder(new CompoundBorder(border, padding));
		}
	}
	
	public void setBackgroundColor(int color) {
		Component component = taggedComponent();
		if (component != null)
			component.setBackground(new Color(color));
	}

}

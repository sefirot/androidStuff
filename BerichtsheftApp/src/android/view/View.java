package android.view;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;

import javax.swing.AbstractButton;
import javax.swing.JComponent;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;

import android.content.Context;
import android.content.res.Resources;
import android.util.AttributeSet;
import android.view.ViewGroup.LayoutParams;

import static com.applang.Util.*;
import static com.applang.SwingUtil.*;

public class View
{
    protected static final String TAG = View.class.getSimpleName();

	@Override
	public String toString() {
		return String.format("%s\t%s", identity(this), getTag());
	}

	public static int uniqueCounter = 0;
	
	public View setTag(Object tag) {
		if (mComponent != null) {
			String name = stringValueOf(tag);
			if (name.endsWith("_"))
				name += (++uniqueCounter);
			mComponent.setName(name);
		}
		return this;
	}
	
	@SuppressWarnings("unchecked")
	public <T extends Component> T taggedComponent() {
		Component component = mComponent;
		if (component == null)
			return null;
		else if ((nullOrEmpty(component.getName())) && component instanceof Container) {
			component = findFirstComponent((Container) component, wildcardRegex("*", GLUE_REGEX));
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

    public View findViewWithTag(Object tag, Object...params) {
        if (tag == null) {
            return null;
        }
        return findViewWithTagTraversal(tag, params);
    }
    
    protected View findViewWithTagTraversal(Object tag, Object...params) {
    	Object param0 = param(null, 0, params);
    	if (param0 instanceof View)
    		return null;
    	String string = stringValueOf(getTag());
        String regex = wildcardRegex(tag);
		if (among(string, regex)) 
            return this;
		else
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
        return findViewByIdTraversal(id);
    }

    protected View findViewByIdTraversal(int id) {
        if (id == mId) {
            return this;
        }
        return null;
    }
	
	public View(Component component) {
		mComponent = component;
		setTag("view_");
	}
	
	private Component mComponent = null;

	public void setComponent(Component component) {
		mComponent = component;
	}

	public Component getComponent() {
		return mComponent;
	}

	private Context mContext = null;

	protected void setContext(Context context) {
		mContext = context;
	}

	public Context getContext() {
		return mContext;
	}
	
	protected void message(String key, Object...params) {
		if (mContext != null)
			mContext.message(key, params);
	}
	
	public View(Context context, AttributeSet attrs) {
		mContext = context;
		attributeSet = attrs;
		setId(0);
		String tag = "view_";
		if (attributeSet != null) {
			String idAttr = attributeSet.getIdAttribute();
			if (notNullOrEmpty(idAttr))
				tag = idAttr;
			inputType = attributeSet.getAttributeResourceItem("android:inputType");
			feature = attributeSet.getAttributeResourceItem("feature");
		}
		create();
		setTag(tag);
    }

	protected void create() {
	}
	
	protected String inputType = null;
	protected String feature = null;

	public boolean hasFeature(String value) {
		return value.equals(feature);
	}
	
	public AttributeSet attributeSet = null;
	
	public void applyAttributes() {
		if (attributeSet != null) {
			boolean paddingChanged = false;
			for (int i = 0; i < attributeSet.getAttributeCount(); i++) {
				String name = attributeSet.getAttributeName(i);
				String value = attributeSet.getAttributeValue(i);
				Dimension size = mComponent.getPreferredSize();
				if ("android:width".equals(name)) {
					int width = Resources.dimensionalValue(mContext, value);
					mComponent.setPreferredSize(new Dimension(width, size.height));
				}
				else if ("android:height".equals(name)) {
					int height = Resources.dimensionalValue(mContext, value);
					mComponent.setPreferredSize(new Dimension(size.width, height));
				}
				else if ("android:background".equals(name)) {
					int color= Resources.colorValue(mContext, value);
					setBackgroundColor(color);
				}
				else if (name.startsWith("android:padding")) {
					if (name.endsWith("Left"))
						paddingTLBR[1] = Resources.dimensionalValue(mContext, value);
					else if (name.endsWith("Top"))
						paddingTLBR[0] = Resources.dimensionalValue(mContext, value);
					else if (name.endsWith("Right"))
						paddingTLBR[3] = Resources.dimensionalValue(mContext, value);
					else if (name.endsWith("Bottom"))
						paddingTLBR[2] = Resources.dimensionalValue(mContext, value);
					paddingChanged = true;
				}

			}
			if (paddingChanged)
				setPadding();
		}
	}

	private View mParent = null;

	//	this does NOT correspond to an Android API
	public View getParent() {
		return mParent;
	}

	//	this does NOT correspond to an Android API
	public void setParent(View parent) {
		this.mParent = parent;
	}

	private LayoutParams mLayoutParams;
	
    public LayoutParams getLayoutParams() {
    	return mLayoutParams;
    }
    
	public void setLayoutParams(LayoutParams params) {
		mLayoutParams = params;
/*		if (component != null) {
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
*/	}
	
	private int[] paddingTLBR = new int[4];

	public void setPadding(int left, int top, int right, int bottom) {
		paddingTLBR[1] = left;
		paddingTLBR[0] = top;
		paddingTLBR[3] = right;
		paddingTLBR[2] = bottom;
		setPadding();
	}

	private void setPadding() {
		if (mComponent != null && mComponent instanceof JComponent) {
			if (mComponent instanceof AbstractButton)
				adjustButtonSize((AbstractButton)mComponent, paddingTLBR);
			else {
				Border padding = new EmptyBorder(paddingTLBR[0], paddingTLBR[1], paddingTLBR[2], paddingTLBR[3]);
				JComponent jc = (JComponent) mComponent;
				Border border = jc.getBorder();
				jc.setBorder(new CompoundBorder(border, padding));
			}
		}
	}
	
	public void setBackgroundColor(int color) {
		Component component = taggedComponent();
		if (component != null)
			component.setBackground(new Color(color));
	}

}

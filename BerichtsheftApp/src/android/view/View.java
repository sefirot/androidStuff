package android.view;

import java.awt.Color;

import javax.swing.JComponent;

import android.content.Context;
import android.view.ViewGroup.LayoutParams;

import static com.applang.Util.*;

public class View
{
	public View(JComponent component) {
		this.component = component;
	}
	
	private JComponent component = null;

	public JComponent getComponent() {
		return component;
	}

	public void setComponent(JComponent component) {
		this.component = component;
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

	public View(Context context) {
		mContext = context;
	}

	private Context mContext = null;

	public Context getContext() {
		return mContext;
	}

	private LayoutParams mLayoutParams;
	
    public LayoutParams getLayoutParams() {
        return mLayoutParams;
    }
    
	public void setLayoutParams(LayoutParams params) {
		mLayoutParams = params;
	}

	public void setBackgroundColor(int color) {
		getComponent().setBackground(new Color(color));
	}

}

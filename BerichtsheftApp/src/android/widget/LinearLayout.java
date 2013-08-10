package android.widget;

import java.awt.Container;
import java.awt.LayoutManager;

import javax.swing.BoxLayout;

import android.content.Context;
import android.view.ViewGroup;

public class LinearLayout extends ViewGroup {

    public LinearLayout(Context context) {
		super(context);
	}
    
	public static class LayoutParams extends ViewGroup.LayoutParams {

		public LayoutParams(int width, int height) {
            super(width, height);
		}

		public void setMargins(int left, int top, int right, int bottom) {
			// TODO Auto-generated method stub
		}

	}
	
	public void setLayoutParams(ViewGroup.LayoutParams layoutParams) {
		// TODO Auto-generated method stub
		
	}
	
	public static final int HORIZONTAL = 0;
    public static final int VERTICAL = 1;
    
    int orientation = VERTICAL;
    
	public int getOrientation() {
		return orientation;
	}
	public void setOrientation(int orientation) {
		this.orientation = orientation;
	}
    
	@Override
    public Container getLayout() {
		Container container = super.getLayout();
		LayoutManager mgr = 
				new BoxLayout(container, orientation == HORIZONTAL ? BoxLayout.X_AXIS : BoxLayout.Y_AXIS);
		container.setLayout(mgr);
    	return container;
    }
}

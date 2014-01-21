package android.widget;

import java.awt.Container;

import javax.swing.BoxLayout;

import android.content.Context;
import android.view.ViewGroup;

public class LinearLayout extends ViewGroup {

    public LinearLayout(Context context) {
		super(context);
	}
    
	public static class LayoutParams extends ViewGroup.MarginLayoutParams
	{
		public LayoutParams(int width, int height) {
            super(width, height);
		}

		public int gravity;
		public float weight;
	}
	
	public void setLayoutParams(ViewGroup.LayoutParams layoutParams) {
		super.setLayoutParams(layoutParams);
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
    public Container getContainer() {
		Container container = super.getContainer();
		container.setLayout(
			new BoxLayout(container, orientation == HORIZONTAL ? 
					BoxLayout.X_AXIS : 
					BoxLayout.Y_AXIS));
		return container;
    }
}

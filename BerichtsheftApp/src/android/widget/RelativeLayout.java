package android.widget;

import java.awt.Container;

import android.content.Context;
import android.view.ViewGroup;

public class RelativeLayout extends ViewGroup {

	public RelativeLayout(Context context) {
		super(context);
	}

	public static class LayoutParams extends ViewGroup.MarginLayoutParams {

		public LayoutParams(int width, int height) {
            super(width, height);
		}

		public void setMargins(int left, int top, int right, int bottom) {
			super.setMargins(left, top, right, bottom);
		}

	}
	
	public void setLayoutParams(ViewGroup.LayoutParams layoutParams) {
		// TODO Auto-generated method stub
		
	}
    
	@Override
    public Container getContainer() {
		Container container = super.getContainer();
    	return container;
    }

}

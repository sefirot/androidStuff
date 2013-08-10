package android.widget;

import java.awt.Container;

import android.content.Context;
import android.view.ViewGroup;

public class RelativeLayout extends ViewGroup {

	public RelativeLayout(Context context) {
		super(context);
	}

	public static class LayoutParams {

		public LayoutParams(int width, int height) {
			// TODO Auto-generated constructor stub
		}

		public void setMargins(int left, int top, int right, int bottom) {
			// TODO Auto-generated method stub
			
		}

	}
	
	public void setLayoutParams(ViewGroup.LayoutParams layoutParams) {
		// TODO Auto-generated method stub
		
	}
    
	@Override
    public Container getLayout() {
		Container container = super.getLayout();
    	return container;
    }

}

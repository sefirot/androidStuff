package android.widget;

import java.awt.Container;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.SpringLayout;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
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

		public LayoutParams(Context c, AttributeSet attrs) {
            super(c, attrs);
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
		if (!(container.getLayout() instanceof BoxLayout))
			container.setLayout(
				new BoxLayout(container, orientation == HORIZONTAL ? 
						BoxLayout.X_AXIS : 
						BoxLayout.Y_AXIS));
		return container;
    }
	
	public BoxLayout getLayout() {
		return (BoxLayout) getContainer().getLayout();
    }

	@Override
	public Container doLayout() {
		return getContainer();
    }

	@Override
	public void doLayout(View view) {
		ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
		if (layoutParams instanceof MarginLayoutParams) {
			MarginLayoutParams margs = (MarginLayoutParams) layoutParams;
			BoxLayout boxLayout = getLayout();
			int axis = boxLayout.getAxis();
			Box outerBox = new Box(axis);
			outerBox.add(margs.strutsOuterFirst(axis));
			Box innerBox = axis == BoxLayout.X_AXIS ? 
					Box.createVerticalBox() : 
					Box.createHorizontalBox();
			innerBox.add(margs.strutsInnerFirst(axis));
			innerBox.add(view.getComponent());
			innerBox.add(margs.strutsInnerLast(axis));
			outerBox.add(innerBox);
			outerBox.add(margs.strutsOuterLast(axis));
			view.setComponent(outerBox);
		}
	}
}

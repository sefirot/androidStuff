package android.widget;

import java.awt.Component;
import java.awt.Container;
import java.io.Writer;

import javax.swing.Spring;
import javax.swing.SpringLayout;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.MarginLayoutParams;

import static com.applang.Util.*;
import static com.applang.Util2.*;

public class RelativeLayout extends ViewGroup
{
    /**
     * Rule that aligns a child's right edge with another child's left edge.
     */
    public static final int LEFT_OF                  = 0;
    /**
     * Rule that aligns a child's left edge with another child's right edge.
     */
    public static final int RIGHT_OF                 = 1;
    /**
     * Rule that aligns a child's bottom edge with another child's top edge.
     */
    public static final int ABOVE                    = 2;
    /**
     * Rule that aligns a child's top edge with another child's bottom edge.
     */
    public static final int BELOW                    = 3;

    /**
     * Rule that aligns a child's baseline with another child's baseline.
     */
    public static final int ALIGN_BASELINE           = 4;
    /**
     * Rule that aligns a child's left edge with another child's left edge.
     */
    public static final int ALIGN_LEFT               = 5;
    /**
     * Rule that aligns a child's top edge with another child's top edge.
     */
    public static final int ALIGN_TOP                = 6;
    /**
     * Rule that aligns a child's right edge with another child's right edge.
     */
    public static final int ALIGN_RIGHT              = 7;
    /**
     * Rule that aligns a child's bottom edge with another child's bottom edge.
     */
    public static final int ALIGN_BOTTOM             = 8;

    /**
     * Rule that aligns the child's left edge with its RelativeLayout
     * parent's left edge.
     */
    public static final int ALIGN_PARENT_LEFT        = 9;
    /**
     * Rule that aligns the child's top edge with its RelativeLayout
     * parent's top edge.
     */
    public static final int ALIGN_PARENT_TOP         = 10;
    /**
     * Rule that aligns the child's right edge with its RelativeLayout
     * parent's right edge.
     */
    public static final int ALIGN_PARENT_RIGHT       = 11;
    /**
     * Rule that aligns the child's bottom edge with its RelativeLayout
     * parent's bottom edge.
     */
    public static final int ALIGN_PARENT_BOTTOM      = 12;

    /**
     * Rule that centers the child with respect to the bounds of its
     * RelativeLayout parent.
     */
    public static final int CENTER_IN_PARENT         = 13;
    /**
     * Rule that centers the child horizontally with respect to the
     * bounds of its RelativeLayout parent.
     */
    public static final int CENTER_HORIZONTAL        = 14;
    /**
     * Rule that centers the child vertically with respect to the
     * bounds of its RelativeLayout parent.
     */
    public static final int CENTER_VERTICAL          = 15;

    private static final int VERB_COUNT              = 16;

	public RelativeLayout(Context context) {
		super(context);
	}

	public static class LayoutParams extends ViewGroup.MarginLayoutParams
	{
		@Override
		public String toString() {
			Writer writer = write(null, super.toString());
			for (int i = 0; i < VERB_COUNT; i++) 
            	if (notNullOrEmpty(mRules[i])) {
					String attr = attributeName(i);
					attr = attr.substring(1 + attr.indexOf(':'));
					writer = write_assoc(writer, attr, mRules[i], 1);
				}
			return writer.toString();
		}
		
		public LayoutParams(int width, int height) {
            super(width, height);
		}

		public boolean alignWithParent;
		public String[] mRules = new String[VERB_COUNT];
		
		private String attributeName(int verb) {
			switch (verb) {
            case ALIGN_PARENT_TOP:    return "android:layout_alignParentTop";
            case ALIGN_PARENT_BOTTOM:    return "android:layout_alignParentBottom";
            case ALIGN_PARENT_LEFT:    return "android:layout_alignParentLeft";
            case ALIGN_PARENT_RIGHT:    return "android:layout_alignParentRight";
            case ALIGN_LEFT:    return "android:layout_alignLeft";
            case ALIGN_RIGHT:    return "android:layout_alignRight";
            case ALIGN_BOTTOM:    return "android:layout_alignBottom";
            case ALIGN_TOP:    return "android:layout_alignTop";
            case ABOVE:    return "android:layout_above";
            case BELOW:    return "android:layout_below";
            case LEFT_OF:    return "android:layout_toLeftOf";
            case RIGHT_OF:    return "android:layout_toRightOf";
            case ALIGN_BASELINE:    return "android:layout_alignBaseline";
            case CENTER_IN_PARENT:    return "android:layout_centerInParent";
            case CENTER_HORIZONTAL:    return "android:layout_centerHorizontal";
            case CENTER_VERTICAL:    return "android:layout_centerVertical";
			default:
				return null;
			}
		}
		
		public LayoutParams(Context context, AttributeSet attrs) {
            super(context, attrs);
            for (int i = 0; i < VERB_COUNT; i++) {
            	String anchor = attrs.getAttributeValue(attributeName(i));
            	if (anchor != null) {
            		if (anchor.startsWith("@"))
	            		anchor = context.getResources().getXMLResourceItem(anchor);
            		addRule(i, anchor);
            	}
			}
		}
		
        public void addRule(int verb, String anchor) {
            mRules[verb] = anchor;
        }

        public void addRule(int verb) {
            mRules[verb] = "true";
        }

        public String[] getRules() {
            return mRules;
        }
	}
	
	public void setLayoutParams(ViewGroup.LayoutParams layoutParams) {
		super.setLayoutParams(layoutParams);
	}
    
	@Override
    public Container getContainer() {
		Container container = super.getContainer();
		if (!(container.getLayout() instanceof SpringLayout))
			container.setLayout(new SpringLayout());
    	return container;
    }
	
	private SpringLayout getLayout() {
		return (SpringLayout) getContainer().getLayout();
    }
	
	private Component getAnchor(View parent, String name) {
		View peer = parent.findViewWithTag(name);
		if (peer != null) 
			return peer.taggedComponent();
		else
			return null;
	}
	
	private void alignToPeer(ViewGroup parent, String peer, String edge, Component component) {
		SpringLayout.Constraints constraint = getLayout().getConstraints(component);
		Component anchor = getAnchor(parent, peer);
		if (anchor != null) {
			SpringLayout.Constraints anchorCons = getLayout().getConstraints(anchor);
			constraint.setX(Spring.sum(
					Spring.constant(5),
					anchorCons.getConstraint(edge)));
		}
	}
	
	private void alignToParent(Container parent, String edge, Component component) {
		getLayout()
			.putConstraint(
				edge, component, 
				5, 
				edge, parent);
	}

	@Override
	public void doLayout(View view) {
		ViewGroup parent = (ViewGroup) view.getParent();
		Component component = view.taggedComponent();
		ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
		if (layoutParams instanceof LayoutParams) {
			LayoutParams rParams = (LayoutParams) layoutParams;
			String[] rules = rParams.getRules();
			for (int verb = 0; verb < rules.length; verb++) {
				String rule = rules[verb];
				if (notNullOrEmpty(rule)) {
					SpringLayout.Constraints viewCons = getLayout()
							.getConstraints(component);
					if (component != null)
						switch (verb) {
						case LEFT_OF:
							alignToPeer(parent, rule, 
									SpringLayout.WEST, component);
							break;
						case RIGHT_OF:
							break;
						case ABOVE:
							break;
						case BELOW:
							break;
						case ALIGN_LEFT:
							break;
						case ALIGN_TOP:
							break;
						case ALIGN_RIGHT:
							break;
						case ALIGN_BOTTOM:
							break;
						case ALIGN_PARENT_LEFT:
							alignToParent(parent.getContainer(),
									SpringLayout.WEST, component);
							break;
						case ALIGN_PARENT_TOP:
							alignToParent(parent.getContainer(),
									SpringLayout.NORTH, component);
							break;
						case ALIGN_PARENT_RIGHT:
							alignToParent(parent.getContainer(),
									SpringLayout.EAST, component);
							break;
						case ALIGN_PARENT_BOTTOM:
							alignToParent(parent.getContainer(),
									SpringLayout.SOUTH, component);
							break;
						case ALIGN_BASELINE:
							break;
						case CENTER_HORIZONTAL:
							break;
						case CENTER_VERTICAL:
							break;
						case CENTER_IN_PARENT:
							break;
						}
				}
			}
		}
    }

	@Override
	public ViewGroup completeLayout() {
		setContainerSize(getContainer(), 0);
		return this;
	}

    private void setContainerSize(Container container, int pad) {
        SpringLayout layout = (SpringLayout) container.getLayout();
        Component[] components = container.getComponents();
        Spring maxRightSpring = Spring.constant(0);
        Spring maxHeightSpring = Spring.constant(0);
        SpringLayout.Constraints cCons = layout.getConstraints(container);

        //Set the container's right edge to the right edge
        //of its rightmost component + padding.
        for (int i = 0; i < components.length; i++) {
            SpringLayout.Constraints cons = layout.getConstraints(components[i]);
            maxRightSpring = 
            	Spring.max(maxRightSpring,
            		cons.getConstraint(SpringLayout.EAST));
        }
        cCons.setConstraint(
                SpringLayout.EAST,
                Spring.sum(Spring.constant(pad), maxRightSpring));

        //Set the container's bottom edge to the bottom edge
        //of its tallest component + padding.
        for (int i = 0; i < components.length; i++) {
            SpringLayout.Constraints cons = layout.getConstraints(components[i]);
            maxHeightSpring = 
            	Spring.max(maxHeightSpring,
            		cons.getConstraint(SpringLayout.SOUTH));
        }
        cCons.setConstraint(
                SpringLayout.SOUTH,
                Spring.sum(Spring.constant(pad), maxHeightSpring));
	}

}

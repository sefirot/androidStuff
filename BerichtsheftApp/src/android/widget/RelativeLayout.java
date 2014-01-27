package android.widget;

import java.awt.Component;
import java.awt.Container;
import java.io.Writer;

import javax.swing.Spring;
import javax.swing.SpringLayout;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;

import static com.applang.Util.*;
import static com.applang.Util1.marginLayoutParams;
import static com.applang.Util2.*;
import static com.applang.SwingUtil.*;

public class RelativeLayout extends ViewGroup
{
	private static final String TAG = RelativeLayout.class.getSimpleName();
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
	
	public SpringLayout getLayout() {
		return (SpringLayout) getContainer().getLayout();
    }
	
	private Component getAnchor(View parent, String name) {
		View peer = parent.findViewWithTag(name);
		if (peer != null) 
			return peer.getComponent();
		else
			return null;
	}
	
	private void alignWithAnchor(View parent, String name, int verb, Component component, LayoutParams relParams) {
		Component anchor = getAnchor(parent, name);
		if (anchor != null) {
			SpringLayout layout = getLayout();
			int componentIndex = getComponentIndex(component);
			int anchorIndex = getComponentIndex(anchor);
			switch (verb) {
			case LEFT_OF:
				if (componentIndex > anchorIndex)
					swapComponents(component, anchor);
				layout.getConstraints(anchor).setX(
					Spring.sum(
						Spring.constant(relParams.leftMargin),
						layout.getConstraints(component).getConstraint(SpringLayout.EAST)));
				break;
			case RIGHT_OF:
				if (componentIndex < anchorIndex)
					swapComponents(component, anchor);
				layout.getConstraints(component).setX(
					Spring.sum(
						Spring.constant(relParams.rightMargin),
						layout.getConstraints(anchor).getConstraint(SpringLayout.EAST)));
				break;
			case ABOVE:
				if (componentIndex > anchorIndex)
					swapComponents(component, anchor);
				layout.getConstraints(anchor).setY(
					Spring.sum(
						Spring.constant(relParams.topMargin),
						layout.getConstraints(component).getConstraint(SpringLayout.SOUTH)));
				break;
			case BELOW:
				if (componentIndex < anchorIndex)
					swapComponents(component, anchor);
				layout.getConstraints(component).setY(
					Spring.sum(
						Spring.constant(relParams.bottomMargin),
						layout.getConstraints(anchor).getConstraint(SpringLayout.SOUTH)));
				break;
			}
		}
	}
	
	private void alignEdges(ViewGroup parent, String peer, int verb, Component component) {
		Component anchor = getAnchor(parent, peer);
		if (anchor != null) {
			SpringLayout.Constraints componentCons = getLayout().getConstraints(component);
			SpringLayout.Constraints anchorCons = getLayout().getConstraints(anchor);
			switch (verb) {
			case LEFT_OF:
				componentCons.setX(anchorCons.getConstraint(SpringLayout.WEST));
				break;
			case ABOVE:
				componentCons.setY(anchorCons.getConstraint(SpringLayout.NORTH));
				break;
			case RIGHT_OF:
				componentCons.setX(anchorCons.getConstraint(SpringLayout.EAST));
				break;
			case BELOW:
				componentCons.setY(anchorCons.getConstraint(SpringLayout.SOUTH));
				break;
			}
		}
	}
	
	private void alignToParent(Container parent, int verb, Component component, LayoutParams relParams) {
		String edge = "";
		int pad = 0;
		switch (verb) {
		case ALIGN_PARENT_LEFT:
			edge = SpringLayout.WEST;
			pad = relParams.leftMargin;
			break;
		case ALIGN_PARENT_TOP:
			edge = SpringLayout.NORTH;
			pad = relParams.topMargin;
			break;
		case ALIGN_PARENT_RIGHT:
			edge = SpringLayout.EAST;
			pad = relParams.rightMargin;
			break;
		case ALIGN_PARENT_BOTTOM:
			edge = SpringLayout.SOUTH;
			pad = relParams.bottomMargin;
			break;
		}
		getLayout().putConstraint(edge, component, pad, edge, parent);
	}

	private void do_Layout(View view) {
		ViewGroup parent = (ViewGroup) view.getParent();
		Component component = view.getComponent();
		ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
		if (layoutParams instanceof LayoutParams && component != null) {
			LayoutParams relParams = (LayoutParams) layoutParams;
			String[] rules = relParams.getRules();
			for (int verb = 0; verb < rules.length; verb++) {
				String rule = rules[verb];
				if (notNullOrEmpty(rule)) {
					switch (verb) {
					case LEFT_OF:
					case RIGHT_OF:
					case ABOVE:
					case BELOW:
						printSprings("before", this, view);
						alignWithAnchor(parent, rule, verb, component, relParams);
						printSprings("after", this, view);
						break;
					case ALIGN_LEFT:
					case ALIGN_TOP:
					case ALIGN_RIGHT:
					case ALIGN_BOTTOM:
						alignEdges(parent, rule, verb, component);
						break;
					case ALIGN_PARENT_LEFT:
					case ALIGN_PARENT_TOP:
					case ALIGN_PARENT_RIGHT:
					case ALIGN_PARENT_BOTTOM:
						alignToParent(parent.getContainer(), verb, component, relParams);
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
		MarginLayoutParams margins = marginLayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT);
		int childCount = getChildCount();
		for (int i = 0; i < childCount; i++) {
			View view = getChildAt(i);
			do_Layout(view);
			do_Margins(getContainer(), view.getComponent(), view.getLayoutParams(), margins);
		}
		setContainer(getContainer(), margins);
		return this;
	}

	public void do_Margins(Container container, Component component, 
			ViewGroup.LayoutParams layoutParams, 
			MarginLayoutParams margins)
	{
        SpringLayout layout = (SpringLayout) container.getLayout();
		if (layoutParams instanceof MarginLayoutParams && component != null) {
			MarginLayoutParams margs = (MarginLayoutParams) layoutParams;
			SpringLayout.Constraints cons = layout.getConstraints(component);
			for (String cardinalPoint : cardinalPoints()) {
				int margin = margs.getMargin(cardinalPoint);
				if (margin > 0)
					cons.setConstraint(cardinalPoint,
						Spring.sum(
							Spring.constant(margin),
							cons.getConstraint(cardinalPoint)));
				if (margins != null)
					margins.setMargin(cardinalPoint, 
						Math.max(margin, margins.getMargin(cardinalPoint)));
			}
		}
	}

	public void setContainer(Container container, MarginLayoutParams margins) {
        SpringLayout layout = (SpringLayout) container.getLayout();
        Component[] components = container.getComponents();
        Spring maxRightSpring = Spring.constant(0);
        Spring maxHeightSpring = Spring.constant(0);

        for (int i = 0; i < components.length; i++) {
            SpringLayout.Constraints cons = layout.getConstraints(components[i]);
            maxRightSpring = 
            	Spring.max(maxRightSpring,
            		cons.getConstraint(SpringLayout.EAST));
        }
        layout.getConstraints(container).setConstraint(
                SpringLayout.EAST,
                Spring.sum(Spring.constant(margins.getMargin(SpringLayout.EAST)), maxRightSpring));

        for (int i = 0; i < components.length; i++) {
            SpringLayout.Constraints cons = layout.getConstraints(components[i]);
            maxHeightSpring = 
            	Spring.max(maxHeightSpring,
            		cons.getConstraint(SpringLayout.SOUTH));
        }
        layout.getConstraints(container).setConstraint(
                SpringLayout.SOUTH,
                Spring.sum(Spring.constant(margins.getMargin(SpringLayout.SOUTH)), maxHeightSpring));
	}

	public static void printSprings(RelativeLayout relLayout) {
		for (int i = 0; i < relLayout.getChildCount(); i++) {
			View v = relLayout.getChildAt(i);
			printSprings(stringValueOf(v.getTag()), relLayout, v);
		}
	}

	public static void printSprings(String string, RelativeLayout relLayout, View v) {
		SpringLayout.Constraints cons = 
				relLayout.getLayout().getConstraints(v.getComponent());
		printEdges(string, cons);
	}

	private static void printEdges(String string, SpringLayout.Constraints cons) {
		Writer writer = write(null, string);
		for (String edge : all_edges()) {
			String con = stringValueOf(cons.getConstraint(edge));
			con = con.replaceAll("CompoundSpring", "Compound");
			write_assoc(writer, edge, con, 3);
		}
		println(writer.toString());
	}

	private static String[] all_edges() {
		String[] edges = getPrivateField(SpringLayout.class, null, "ALL_HORIZONTAL");
		edges = arrayappend(edges, 
				(String[]) getPrivateField(SpringLayout.class, null, "ALL_VERTICAL"));
		return edges;
	}
}

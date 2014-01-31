package android.widget;

import java.awt.Component;
import java.awt.Container;
import java.io.Writer;

import javax.swing.Spring;
import javax.swing.SpringLayout;

import android.content.Context;
import android.content.res.Resources;
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

	public RelativeLayout(Context context, Object...params) {
		super(context);
		if (param(null, 0, params) instanceof Container)
			setComponent((Component) params[0]);
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
		
		private int verb(String attributeName) {
			for (int i = 0; i < VERB_COUNT; i++) 
				if (attributeName(i).equals(attributeName))
					return i;
			return -1;
		}
		
		public LayoutParams() {
			super(FILL_PARENT, WRAP_CONTENT);
		}
		
		public LayoutParams(Context context, AttributeSet attrs) {
            super(context, attrs);
            for (int i = 0; i < VERB_COUNT; i++) {
            	String anchor = attrs.getAttributeValue(attributeName(i));
            	if (anchor != null) 
            		addRule(context, i, anchor);
			}
		}
		
        public void addRules(Context context, Object...params) {
        	for (int i = 0; i < params.length; i+=2) {
        		int verb = verb(param_String(null, i, params));
        		if (verb > -1)
        			addRule(context, verb, param_String(null, i + 1, params));
        	}
		}
		
        public void addRule(Context context, int verb, String anchor) {
        	addRule(verb, Resources.textValue(context, anchor));
        }

        public void addRule(int verb, String anchor) {
            mRules[verb] = anchor;
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
	
	private void alignWithAnchor(View parent, String name, int verb, View view, LayoutParams relParams) {
		View anchor = parent.findViewWithTag(name);
		if (anchor != null && anchor.getComponent() != null) {
			SpringLayout layout = getLayout();
			int viewIndex = views.indexOf(view);
			int anchorIndex = views.indexOf(anchor);
			switch (verb) {
			case LEFT_OF:
				if (relParams == null)
					indexMove(viewIndex, anchorIndex);
				else
					layout.getConstraints(anchor.getComponent()).setX(
						Spring.sum(
							Spring.constant(relParams.leftMargin),
							layout.getConstraints(view.getComponent()).getConstraint(SpringLayout.EAST)));
				break;
			case RIGHT_OF:
				if (relParams == null)
					indexMove(viewIndex, anchorIndex + 1);
				else
					layout.getConstraints(view.getComponent()).setX(
						Spring.sum(
							Spring.constant(relParams.rightMargin),
							layout.getConstraints(anchor.getComponent()).getConstraint(SpringLayout.EAST)));
				break;
			case ABOVE:
				if (relParams == null)
					indexMove(viewIndex, anchorIndex);
				else
					layout.getConstraints(anchor.getComponent()).setY(
						Spring.sum(
							Spring.constant(relParams.topMargin),
							layout.getConstraints(view.getComponent()).getConstraint(SpringLayout.SOUTH)));
				break;
			case BELOW:
				if (relParams == null)
					indexMove(viewIndex, anchorIndex + 1);
				else
					layout.getConstraints(view.getComponent()).setY(
						Spring.sum(
							Spring.constant(relParams.bottomMargin),
							layout.getConstraints(anchor.getComponent()).getConstraint(SpringLayout.SOUTH)));
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
			case ALIGN_LEFT:
				componentCons.setX(anchorCons.getConstraint(SpringLayout.WEST));
				break;
			case ALIGN_TOP:
				componentCons.setY(anchorCons.getConstraint(SpringLayout.NORTH));
				break;
			case ALIGN_RIGHT:
				componentCons.setX(anchorCons.getConstraint(SpringLayout.EAST));
				break;
			case ALIGN_BOTTOM:
				componentCons.setY(anchorCons.getConstraint(SpringLayout.SOUTH));
				break;
			}
		}
	}
	
	private void alignWithParent(Container parent, int verb, Component component, LayoutParams relParams) {
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

	public void doRules(boolean init, View view) {
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
						alignWithAnchor(parent, rule, verb, view, init ? null : relParams);
						break;
					case ALIGN_LEFT:
					case ALIGN_TOP:
					case ALIGN_RIGHT:
					case ALIGN_BOTTOM:
						if (!init)
							alignEdges(parent, rule, verb, component);
						break;
					case ALIGN_PARENT_LEFT:
					case ALIGN_PARENT_TOP:
					case ALIGN_PARENT_RIGHT:
					case ALIGN_PARENT_BOTTOM:
						if (!init)
							alignWithParent(parent.getContainer(), verb, component, relParams);
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
	
	private ValList index = null;
	
	private void indexInit() {
		index = vlist();
		for (int i = 0; i < getChildCount(); i++)
			index.add(i);
	}
	
	private int index(int index) {
		return index < 0 || index >= this.index.size() ? 
				-1 : 
				(Integer) this.index.get(index);
	}
	
	private void indexMove(int from, int to) {
		index.add(to, index.remove(from));
	}

	@Override
	public void initLayout() {
		indexInit();
		for (int i = 0; i < getChildCount(); i++)
			doRules(true, getChildAt(i));
	}

	public ViewGroup final_Layout() {
		MarginLayoutParams margins = marginLayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT);
        Spring xSpring = Spring.constant(0);
        Spring ySpring = Spring.constant(0);
		for (int i = 0; i < getChildCount(); i++) {
			View view = getChildAt(index(i));
//			printSprings(view.getTag() + " before", view);
            SpringLayout.Constraints cons = getLayout().getConstraints(view.getComponent());
            cons.setX(Spring.sum(xSpring, getMarginSpring(view, SpringLayout.WEST)));
            cons.setY(Spring.sum(ySpring, getMarginSpring(view, SpringLayout.NORTH)));
            xSpring = Spring.sum(cons.getConstraint(SpringLayout.EAST), getMarginSpring(view, SpringLayout.EAST));
			println(cons.getConstraint(SpringLayout.WEST).getValue(), cons.getConstraint(SpringLayout.EAST).getValue());
            for (String edge : strings(SpringLayout.EAST, SpringLayout.SOUTH)) {
				margins.setMargin(edge, 
					Math.max(
						getMarginSpring(view, edge).getValue(),
						margins.getMargin(edge)));
			}
//			printSprings(view.getTag() + " after", view);
		}
		setOuterMargins(getContainer(), margins);
		return this;
	}

	public MarginLayoutParams do_Margins(Container container, Component component, 
			ViewGroup.LayoutParams layoutParams, 
			MarginLayoutParams outerMargins)
	{
        SpringLayout layout = (SpringLayout) container.getLayout();
		if (layoutParams instanceof MarginLayoutParams && component != null) {
			MarginLayoutParams margs = (MarginLayoutParams) layoutParams;
			SpringLayout.Constraints cons = layout.getConstraints(component);
			for (String edge : edges()) {
				int margin = margs.getMargin(edge);
				if (margin > 0)
					cons.setConstraint(edge,
						Spring.sum(
							Spring.constant(margin),
							cons.getConstraint(edge)));
				if (outerMargins != null)
					outerMargins.setMargin(edge, 
						Math.max(margin, outerMargins.getMargin(edge)));
			}
		}
		return outerMargins;
	}

	public void setOuterMargins(Container container, MarginLayoutParams margins) {
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

	@Override
	public ViewGroup finalLayout() {
		arrange(1, index.size());
//		makeGrid(getContainer(), 2, index.size() / 2, 0,0,0,0);
		return this;
	}
	
	private Spring getMarginSpring(View view, String edge, int...more) {
		int margin = 0;
		if (view != null && view.getLayoutParams() instanceof MarginLayoutParams) {
			MarginLayoutParams margs = (MarginLayoutParams) view.getLayoutParams();
			if ("horz".equals(edge)) {
				margin += margs.getMargin(SpringLayout.WEST);
				margin += margs.getMargin(SpringLayout.EAST);
			}
			else if ("vert".equals(edge)) {
				margin += margs.getMargin(SpringLayout.NORTH);
				margin += margs.getMargin(SpringLayout.SOUTH);
			}
			else
				margin = margs.getMargin(edge);
		}
		return Spring.constant(margin, margin, margin + param(0, 0, more));
	}

    private View getViewAt(int row, int col, int cols) {
		return getChildAt(index(row * cols + col));
    }

	public void arrange(int rows, int cols, int...xyPadxPady) {
        SpringLayout layout = getLayout();
        Spring x = Spring.constant(param(0,0,xyPadxPady)), y = Spring.constant(param(0,1,xyPadxPady)), z;
        boolean oneRow = rows < 2;
        boolean oneCol = cols < 2;
        View view = null;
        for (int c = 0; c < cols; c++) {
            Spring width = Spring.constant(0);
            Spring dx = width;
			for (int r = 0; r < rows; r++) {
            	view = getViewAt(r,c,cols);
            	if (view == null) continue;
                Spring w = getLayout().getConstraints(view.getComponent()).getWidth();
				width = Spring.max(width, w);
                dx = oneRow || oneCol ? 
                		Spring.max(dx, Spring.sum(w, getMarginSpring(view, "horz"))) :
                		width;
            }
            for (int r = 0; r < rows; r++) {
            	z = x;
            	view = getViewAt(r,c,cols);
            	if (view == null) continue;
                SpringLayout.Constraints constraints = getLayout().getConstraints(view.getComponent());
                if (oneRow || oneCol)
                	z = Spring.sum(z, getMarginSpring(view, SpringLayout.WEST));
                constraints.setX(z);
                if (oneRow || oneCol)
                	z = Spring.sum(z, getMarginSpring(view, SpringLayout.EAST, width.getValue()));
				if (!oneCol)
                	constraints.setWidth(width);
                if (oneRow)
                	x = z;
            }
            x = Spring.sum(x, Spring.sum(dx, Spring.constant(param(0,2,xyPadxPady))));
        }
        for (int r = 0; r < rows; r++) {
            Spring height = Spring.constant(0);
            Spring dy = height;
			for (int c = 0; c < cols; c++) {
            	view = getViewAt(r,c,cols);
            	if (view == null) continue;
                Spring h = getLayout().getConstraints(view.getComponent()).getHeight();
				height = Spring.max(height, h);
                dy = oneCol || oneRow ?
                		Spring.max(dy, Spring.sum(h, getMarginSpring(view, "vert"))) :
                		height;
            }
            for (int c = 0; c < cols; c++) {
            	z = y;
            	view = getViewAt(r,c,cols);
            	if (view == null) continue;
                SpringLayout.Constraints constraints = getLayout().getConstraints(view.getComponent());
                if (oneCol || oneRow)
                	z = Spring.sum(z, getMarginSpring(view, SpringLayout.NORTH));
                constraints.setY(z);
                if (oneCol || oneRow)
                	z = Spring.sum(z, getMarginSpring(view, SpringLayout.SOUTH, height.getValue()));
                if (!oneRow)
                	constraints.setHeight(height);
                if (oneCol)
                	y = z;
            }
            y = Spring.sum(y, Spring.sum(dy, Spring.constant(param(0,3,xyPadxPady))));
        }

        SpringLayout.Constraints cons = layout.getConstraints(getContainer());
        cons.setConstraint(SpringLayout.SOUTH, y);
        cons.setConstraint(SpringLayout.EAST, x);
        println("width", cons.getWidth().getValue(), x.getValue());
        println("height", cons.getHeight().getValue(), y.getValue());
    }

	public void printSprings() {
		for (int i = 0; i < getChildCount(); i++) {
			View v = getChildAt(i);
			printSprings(stringValueOf(v.getTag()), v);
		}
	}

	public void printSprings(String string, View v) {
		SpringLayout.Constraints cons = getLayout().getConstraints(v.getComponent());
		printEdges(string, cons);
	}

	private static void printEdges(String string, SpringLayout.Constraints cons) {
		Writer writer = write(null, string);
		for (String name : all_constraints()) {
			Spring constraint = cons.getConstraint(name);
			String s = stringValueOf(constraint);
			s = s.replaceAll("javax.swing.Spring\\$", "");
			write_assoc(writer, name, s, 3);
		}
		println(writer.toString());
	}

	private static String[] all_constraints() {
		String[] edges = getPrivateField(SpringLayout.class, null, "ALL_HORIZONTAL");
		edges = arrayappend(edges, 
				(String[]) getPrivateField(SpringLayout.class, null, "ALL_VERTICAL"));
		return edges;
	}

    public void makeGrid(Container parent, int rows, int cols, int initialX, int initialY, int xPad, int yPad) {
        SpringLayout layout;
        try {
            layout = (SpringLayout)parent.getLayout();
        } catch (ClassCastException exc) {
            System.err.println("The first argument to makeGrid must use SpringLayout.");
            return;
        }

        Spring xPadSpring = Spring.constant(xPad);
        Spring yPadSpring = Spring.constant(yPad);
        Spring initialXSpring = Spring.constant(initialX);
        Spring initialYSpring = Spring.constant(initialY);
        int max = rows * cols;

        //Calculate Springs that are the max of the width/height so that all
        //cells have the same size.
        Spring maxWidthSpring = layout.getConstraints(parent.getComponent(0)).getWidth();
        Spring maxHeightSpring = layout.getConstraints(parent.getComponent(0)).getHeight();
        for (int i = 1; i < max; i++) {
            SpringLayout.Constraints cons = layout.getConstraints(
                                            parent.getComponent(i));

            maxWidthSpring = Spring.max(maxWidthSpring, cons.getWidth());
            maxHeightSpring = Spring.max(maxHeightSpring, cons.getHeight());
        }

        //Apply the new width/height Spring. This forces all the
        //components to have the same size.
        for (int i = 0; i < max; i++) {
            SpringLayout.Constraints cons = layout.getConstraints(
                                            parent.getComponent(i));

            cons.setWidth(maxWidthSpring);
            cons.setHeight(maxHeightSpring);
        }

        //Then adjust the x/y constraints of all the cells so that they
        //are aligned in a grid.
        SpringLayout.Constraints lastCons = null;
        SpringLayout.Constraints lastRowCons = null;
        for (int i = 0; i < max; i++) {
            SpringLayout.Constraints cons = layout.getConstraints(
                                                 parent.getComponent(i));
            if (i % cols == 0) { //start of new row
                lastRowCons = lastCons;
                cons.setX(initialXSpring);
            } else { //x position depends on previous component
                cons.setX(Spring.sum(lastCons.getConstraint(SpringLayout.EAST),
                                     xPadSpring));
            }

            if (i / cols == 0) { //first row
                cons.setY(initialYSpring);
            } else { //y position depends on previous row
                cons.setY(Spring.sum(lastRowCons.getConstraint(SpringLayout.SOUTH),
                                     yPadSpring));
            }
            lastCons = cons;
        }

        //Set the parent's size.
        SpringLayout.Constraints pCons = layout.getConstraints(parent);
        pCons.setConstraint(SpringLayout.SOUTH,
                            Spring.sum(
                                Spring.constant(yPad),
                                lastCons.getConstraint(SpringLayout.SOUTH)));
        pCons.setConstraint(SpringLayout.EAST,
                            Spring.sum(
                                Spring.constant(xPad),
                                lastCons.getConstraint(SpringLayout.EAST)));
    }
}

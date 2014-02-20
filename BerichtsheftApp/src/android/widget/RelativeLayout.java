package android.widget;

import java.awt.Component;
import java.awt.Container;
import java.io.Writer;

import javax.swing.Spring;
import javax.swing.SpringLayout;

import com.applang.Util;
import com.applang.Util.ValMap;

import android.content.Context;
import android.content.res.Resources;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

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
		
        public void addRules(Context context, Object...attrs) {
			ValMap map = Util.namedParams(attrs);
        	for (String key : map.keySet()) {
        		int verb = verb(key);
        		if (verb > -1)
        			addRule(context, verb, stringValueOf(map.get(key)));
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
	
	public void putConstraint(String edge, Component component, int pad, String edge2, Component component2) {
		printEdges(component.getName() + " before", getLayout().getConstraints(component), edge);
		getLayout().putConstraint(edge, component, pad, edge2, component2);
		printEdges(component.getName() + " after", getLayout().getConstraints(component), edge);
	}
	
	private void alignWithAnchor(View parent, String name, int verb, View view, boolean init) {
		View anchor = parent.findViewWithTag(name);
		if (anchor != null && anchor.getComponent() != null) {
			if (init) {
				int viewIndex = views.indexOf(view);
				int anchorIndex = views.indexOf(anchor);
				switch (verb) {
				case LEFT_OF:
					indexMove(viewIndex, anchorIndex);
				case RIGHT_OF:
					indexMove(viewIndex, anchorIndex + 1);
					break;
				case ABOVE:
					indexMove(viewIndex, anchorIndex);
					break;
				case BELOW:
					indexMove(viewIndex, anchorIndex + 1);
					break;
				}
			}
			else {
				SpringLayout.Constraints coCons = getLayout().getConstraints(view.getComponent());
				SpringLayout.Constraints anCons = getLayout().getConstraints(anchor.getComponent());
				String coEdge = "", anEdge = "";
				switch (verb) {
				case LEFT_OF:
					coEdge = SpringLayout.EAST;
					anEdge = SpringLayout.WEST;
					break;
				case RIGHT_OF:
					coEdge = SpringLayout.WEST;
					anEdge = SpringLayout.EAST;
					break;
				case ABOVE:
					coEdge = SpringLayout.SOUTH;
					anEdge = SpringLayout.NORTH;
					break;
				case BELOW:
					coEdge = SpringLayout.NORTH;
					anEdge = SpringLayout.SOUTH;
					break;
				default:
					return;
				}
				int pad = Math.abs(coCons.getConstraint(coEdge).getValue() - anCons.getConstraint(anEdge).getValue());
				putConstraint(coEdge, view.getComponent(), pad, anEdge, anchor.getComponent());
			}
		}
	}

	private void alignEdges(ViewGroup parent, String name, int verb, View view) {
		View anchor = parent.findViewWithTag(name);
		if (anchor != null && anchor.getComponent() != null) {
			SpringLayout.Constraints coCons = getLayout().getConstraints(view.getComponent());
			SpringLayout.Constraints anCons = getLayout().getConstraints(anchor.getComponent());
			switch (verb) {
			case ALIGN_LEFT:
				coCons.setConstraint(SpringLayout.WEST, anCons.getConstraint(SpringLayout.WEST));
				break;
			case ALIGN_TOP:
				coCons.setConstraint(SpringLayout.NORTH, anCons.getConstraint(SpringLayout.NORTH));
				break;
			case ALIGN_RIGHT:
				coCons.setConstraint(SpringLayout.EAST, anCons.getConstraint(SpringLayout.EAST));
				break;
			case ALIGN_BOTTOM:
				coCons.setConstraint(SpringLayout.SOUTH, anCons.getConstraint(SpringLayout.SOUTH));
				break;
			}
		}
	}
	
	private void alignWithParent(ViewGroup parent, int verb, View view) {
		SpringLayout.Constraints parCons = getLayout().getConstraints(parent.getContainer());
		SpringLayout.Constraints coCons = getLayout().getConstraints(view.getComponent());
		String edge = "";
		switch (verb) {
		case ALIGN_PARENT_LEFT:
			edge = SpringLayout.WEST;
			break;
		case ALIGN_PARENT_TOP:
			edge = SpringLayout.NORTH;
			break;
		case ALIGN_PARENT_RIGHT:
			edge = SpringLayout.EAST;
			break;
		case ALIGN_PARENT_BOTTOM:
			edge = SpringLayout.SOUTH;
			break;
		default:
			return;
		}
		int pad = Math.abs(parCons.getConstraint(edge).getValue() - coCons.getConstraint(edge).getValue());
		putConstraint(edge, view.getComponent(), pad, edge, parent.getContainer());
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
						alignWithAnchor(parent, rule, verb, view, init);
						break;
					case ALIGN_LEFT:
					case ALIGN_TOP:
					case ALIGN_RIGHT:
					case ALIGN_BOTTOM:
						if (!init)
							alignEdges(parent, rule, verb, view);
						break;
					case ALIGN_PARENT_LEFT:
					case ALIGN_PARENT_TOP:
					case ALIGN_PARENT_RIGHT:
					case ALIGN_PARENT_BOTTOM:
						if (!init)
							alignWithParent(parent, verb, view);
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
	public void preLayout() {
		indexInit();
		for (int i = 0; i < getChildCount(); i++)
			doRules(true, getChildAt(i));
	}

	@Override
	public Container doLayout() {
		arrange(1, index.size());
//		printSprings();
		for (int i = 0; i < getChildCount(); i++)
			doRules(false, getChildAt(index(i)));
//		printSprings();
		return getContainer();
	}

	public void arrange(int rows, int cols, int...xyPadxPady) {
        SpringLayout layout = getLayout();
        Spring x = Spring.constant(param(0,0,xyPadxPady)), y = Spring.constant(param(0,1,xyPadxPady));
        Spring xPad = Spring.constant(param(0,2,xyPadxPady)), yPad = Spring.constant(param(0,3,xyPadxPady));
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
            	view = getViewAt(r,c,cols);
            	if (view == null) continue;
                SpringLayout.Constraints constraints = getLayout().getConstraints(view.getComponent());
                Spring z = x;
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
            x = Spring.sum(x, Spring.sum(dx, xPad));
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
            	view = getViewAt(r,c,cols);
            	if (view == null) continue;
                SpringLayout.Constraints constraints = getLayout().getConstraints(view.getComponent());
                Spring z = y;
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
            y = Spring.sum(y, Spring.sum(dy, yPad));
        }

        SpringLayout.Constraints cons = layout.getConstraints(getContainer());
        cons.setConstraint(SpringLayout.SOUTH, y);
        cons.setConstraint(SpringLayout.EAST, x);
//		println("width", cons.getWidth().getValue(), x.getValue());
//		println("height", cons.getHeight().getValue(), y.getValue());
    }

    private View getViewAt(int row, int col, int cols) {
		return getChildAt(index(row * cols + col));
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

	public void printSprings() {
		for (int i = 0; i < getChildCount(); i++) {
			View v = getChildAt(index(i));
			printSprings(stringValueOf(v.getTag()), v);
		}
	}

	public void printSprings(String string, View v) {
		SpringLayout.Constraints cons = getLayout().getConstraints(v.getComponent());
		printEdges(string, cons, all_constraints());
	}

	private static void printEdges(String string, SpringLayout.Constraints cons, String...edges) {
		Writer writer = write(null, string);
		for (String edge : edges) {
			writeConstraint(writer, cons, edge);
		}
		println(writer.toString());
	}

	private static Writer writeConstraint(Writer writer, SpringLayout.Constraints cons, String edge) {
		Spring constraint = cons.getConstraint(edge);
		String s = stringValueOf(constraint);
		s = s.replaceAll("javax.swing.Spring\\$", "");
		return write_assoc(writer, edge, s, 3);
	}

	private static String[] all_constraints() {
		String[] edges = getPrivateField(SpringLayout.class, null, "ALL_HORIZONTAL");
		edges = arrayappend(edges, 
				(String[]) getPrivateField(SpringLayout.class, null, "ALL_VERTICAL"));
		return edges;
	}

	public static void setOuterMargins(Container container, MarginLayoutParams margins) {
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

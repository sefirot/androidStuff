package android.view;

import java.io.File;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import android.content.Context;
import android.content.res.Resources;
import android.util.Log;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewGroup.MarginLayoutParams;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import static com.applang.Util.*;
import static com.applang.Util1.*;

public class LayoutInflater
{
	protected static final String TAG = LayoutInflater.class.getSimpleName();
	
	private Context mContext = null;

	public LayoutInflater(Context context) {
		mContext = context;
	}

	public Context getContext() {
		return mContext;
	}

    public static LayoutInflater from(Context context) {
        return new LayoutInflater(context);
    }
	
    /**
     * Inflate a new view hierarchy from the specified xml resource.
     * 
     * @param resourcePath path to resource xml
     * @param root Optional view to be the parent of the generated hierarchy.
     * @return The root View of the inflated hierarchy. If root was supplied,
     *         this is the root View; otherwise it is the root of the inflated
     *         XML file.
     */
    public View inflate(String resourcePath, ViewGroup root) {
    	View vw = null;
    	ViewGroup vg = root == null ? new ViewGroup(mContext) : root;
    	File resourceFile = new File(Resources.getSettingsPath(), resourcePath);
		if (fileExists(resourceFile)) {
			Document document = xmlDocument(resourceFile);
			vw = inflate(vg, document.getDocumentElement());
		}
        return vw;
    }

	public View inflate(ViewGroup vg, Element element) {
    	View vw = null;
		try {
			Class<?> layoutClass = Class.forName("android.widget.".concat(element.getNodeName()));
			int width = LayoutParams.value(element.getAttribute("android:layout_width"));
			int height = LayoutParams.value(element.getAttribute("android:layout_height"));
			if (LinearLayout.class.equals(layoutClass)) {
				vw = linearLayout(mContext, 
						"vertical".equals(element.getAttribute("android:orientation")) ? 
								LinearLayout.VERTICAL : LinearLayout.HORIZONTAL, 
		        		width, height);
			}
			else if (RelativeLayout.class.equals(layoutClass)) {
				vw = relativeLayout(mContext,  width, height);
			}
			else {
				int left = MarginLayoutParams.value(mContext, element.getAttribute("android:layout_marginLeft"));
				int top = MarginLayoutParams.value(mContext, element.getAttribute("android:layout_marginTop"));
				int right = MarginLayoutParams.value(mContext, element.getAttribute("android:layout_marginRight"));
				int bottom = MarginLayoutParams.value(mContext, element.getAttribute("android:layout_marginBottom"));
				if (TextView.class.equals(layoutClass)) {
					vw = new TextView(mContext);
					vw.setLayoutParams(marginLayoutParams(width, height, left, top, right, bottom));
				}
				else if (EditText.class.equals(layoutClass)) {
					vw = new EditText(mContext);
					vw.setLayoutParams(marginLayoutParams(width, height, left, top, right, bottom));
				}
			}
			if (vw != null) {
				if (vw instanceof ViewGroup) {
					NodeList nodes = element.getChildNodes();
					for (int i = 0; i < nodes.getLength(); i++) {
						Node node = nodes.item(i);
						if (node.getNodeType() == Node.ELEMENT_NODE)
							inflate((ViewGroup)vw, (Element)node);
					}
				}
				vg.addView(vw, vw.getLayoutParams());
			}
		} 
		catch (Exception e) {
			Log.e(TAG, "inflate", e);
		}
		return vw;
	}

}

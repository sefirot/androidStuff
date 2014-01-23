package android.view;

import java.io.File;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import android.content.Context;
import android.content.res.Resources;
import android.util.AttributeSet;
import android.util.Log;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewGroup.MarginLayoutParams;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import static com.applang.Util.*;
import static com.applang.Util1.*;
import static com.applang.Util2.*;

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
    public View inflate(String resourcePath, Object...params) {
    	View vw = null;
    	File resourceFile = new File(Resources.getSettingsPath(), resourcePath);
		if (fileExists(resourceFile)) {
			Document document = xmlDocument(resourceFile);
			vw = inflate(document.getDocumentElement(), params);
		}
        return vw;
    }

	public View inflate(final Element element, Object...params) {
    	View vw = null;
		try {
			AttributeSet attributeSet = attributeSet(mContext, element);
			Class<?> layoutClass = Class.forName("android.widget.".concat(element.getNodeName()));
			int width = LayoutParams.dimensionalValue(mContext, element.getAttribute("android:layout_width"));
			int height = LayoutParams.dimensionalValue(mContext, element.getAttribute("android:layout_height"));
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
				int left = Resources.dimensionalValue(mContext, element.getAttribute("android:layout_marginLeft"));
				int top = Resources.dimensionalValue(mContext, element.getAttribute("android:layout_marginTop"));
				int right = Resources.dimensionalValue(mContext, element.getAttribute("android:layout_marginRight"));
				int bottom = Resources.dimensionalValue(mContext, element.getAttribute("android:layout_marginBottom"));
				if (TextView.class.equals(layoutClass)) {
					vw = new TextView(mContext, attributeSet);
					vw.setLayoutParams(marginLayoutParams(width, height, left, top, right, bottom));
				}
				else if (EditText.class.equals(layoutClass)) {
					vw = new EditText(mContext, attributeSet);
					vw.setLayoutParams(marginLayoutParams(width, height, left, top, right, bottom));
				}
				else if (ImageView.class.equals(layoutClass)) {
					vw = new ImageView(mContext, attributeSet);
					vw.setLayoutParams(marginLayoutParams(width, height, left, top, right, bottom));
				}
			}
			if (vw != null) {
				vw.attributeSet = attributeSet;
				if (vw instanceof ViewGroup) {
					ViewGroup vg = (ViewGroup) vw;
					NodeList nodes = element.getChildNodes();
					for (int i = 0; i < nodes.getLength(); i++) {
						Node node = nodes.item(i);
						if (node.getNodeType() == Node.ELEMENT_NODE) {
							View v = inflate((Element)node, params);
							LayoutParams layoutParams = v.getLayoutParams();
							vg.addView(v, layoutParams);
						}
					}
				}
			}
		} 
		catch (Exception e) {
			Log.e(TAG, "inflate", e);
		}
		return vw;
	}

}

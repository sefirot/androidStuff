package android.view;

import java.io.File;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import android.content.Context;
import android.content.res.Resources;
import android.util.AttributeSet;
import android.util.Log;
import android.view.ViewGroup.LayoutParams;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

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
     * @param resourcePath file path to resource xml
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
			String key = param_String(null, 0,  params);
			String style = param_String(null, 1,  params);
			NodeList nodes = document.getElementsByTagName("*");
			for (int i = 0; i < nodes.getLength(); i++) {
				NamedNodeMap attributes = ((Element) nodes.item(i)).getAttributes();
				changeValue(attributes, "android:id", "field_", key);
				changeValue(attributes, "style", "style", style);
			}
			no_println("document", xmlNodeToString(document, true));
			vw = inflate(document.getDocumentElement(), params);
		}
        return vw;
    }

	private void changeValue(NamedNodeMap attributes, String name, String oldValue, String newValue) {
		if (nullOrEmpty(newValue)) 
			return;
		Node node = attributes.getNamedItem(name);
		if (node != null && node.getNodeValue().endsWith(oldValue)) {
			node.setNodeValue(node.getNodeValue().replaceFirst(oldValue + "$", newValue));
			attributes.setNamedItem(node);
		}
	}

	public View inflate(final Element element, Object...params) {
    	View view = null;
		try {
			AttributeSet attributeSet = Resources.attributeSet(mContext, element);
			String className = element.getNodeName();
			if ("view".equals(className)) {
				String attr = attributeSet.getClassAttribute();
				if (notNullOrEmpty(attr))
					className = attr;
				else
					className = "android.view.View";
			}
			else
				className = "android.widget.".concat(className);
			Class<?> layoutClass = Class.forName(className);
			int width = LayoutParams.dimensionalValue(mContext, attributeSet.getAttributeValue("android:layout_width"));
			int height = LayoutParams.dimensionalValue(mContext, attributeSet.getAttributeValue("android:layout_height"));
			if (LinearLayout.class.equals(layoutClass)) {
				view = linearLayout(mContext, 
						"vertical".equals(attributeSet.getAttributeValue("android:orientation")) ? 
								LinearLayout.VERTICAL : LinearLayout.HORIZONTAL, 
		        		width, height);
			}
			else if (RelativeLayout.class.equals(layoutClass)) {
				view = relativeLayout(mContext,  width, height);
			}
			else {
				view = (View) layoutClass
						.getConstructor(Context.class, AttributeSet.class)
						.newInstance(mContext, attributeSet);
				if (view != null) {
					LayoutParams layoutParams;
					View vp = param(null, 0, params);
					if (vp instanceof LinearLayout)
						layoutParams = new LinearLayout.LayoutParams(mContext, attributeSet);
					else if (vp instanceof RelativeLayout)
						layoutParams = new RelativeLayout.LayoutParams(mContext, attributeSet);
					else
						layoutParams = new ViewGroup.MarginLayoutParams(mContext, attributeSet);
					view.setLayoutParams(layoutParams);
				}
			}
			if (view != null) {
				view.attributeSet = attributeSet;
				if (view instanceof ViewGroup) {
					ViewGroup vg = (ViewGroup) view;
					NodeList nodes = element.getChildNodes();
					for (int i = 0; i < nodes.getLength(); i++) {
						Node node = nodes.item(i);
						if (node.getNodeType() == Node.ELEMENT_NODE) {
							View v = inflate((Element)node, vg);
							vg.addView(v, v.getLayoutParams());
						}
					}
				}
			}
		} 
		catch (Exception e) {
			Log.e(TAG, "inflate", e);
		}
		return view;
	}

}

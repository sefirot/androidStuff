package com.applang.components;

import java.awt.Container;
import java.awt.Image;

import org.w3c.dom.Document;

import com.applang.Util.BidiMultiMap;

import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ImageView.Picture;
import android.widget.TextView;

import static com.applang.Util.*;
import static com.applang.Util2.*;

public class Form
{
	private static final String TAG = Form.class.getSimpleName();
	
	public Form(Context context, BidiMultiMap projection, Object...resource) {
		boolean standard = nullOrEmpty(resource);
		if (standard) 
			resource = objects("standard_form.xml");
		Builder builder = new Builder(context, resource[0]);
		if (projection != null) {
			this.projection = new BidiMultiMap(
					projection.getValues(0), 
					projection.getValues(1), 
					projection.getValues(2), 
					vlist());
			for (Object key : this.projection.getKeys()) {
				String name = stringValueOf(key);
				String type = stringValueOf(this.projection.getValue(key, 2));
				View view = standard ? 
						builder.addField(key, name, type) : 
						viewGroup.findViewWithTag(name);
				this.projection.putValue(key, view, 3);
			}
		}
	}

	public BidiMultiMap projection;

	public ViewGroup viewGroup = null;
    
    public Container getContainer() {
    	ViewGroup.build(viewGroup);
    	return viewGroup.getContainer();
    }

	public void setContent(Object key, Object item) {
		Object view = projection.getValue(key, 3);
		if (view != null) {
			String type = stringValueOf(projection.getValue(key, 2));
			switch (fieldTypeAffinity(type)) {
			case Cursor.FIELD_TYPE_BLOB:
				((ImageView)view).setImage((Image) item);
				break;
			default:
				((EditText)view).setText(String.valueOf(item));
			}
		}
	}
	
	public class Builder
	{
		protected LayoutInflater inflater = null;

		public Builder(Context context, Object resource) {
			inflater = LayoutInflater.from(context);
			if (nullOrEmpty(resource)) {
				viewGroup = new ViewGroup(context);
			}
			else if (resource instanceof Integer) {
				Document document = context.getResources().getXml((Integer) resource);
				viewGroup = (ViewGroup) inflater.inflate(document.getDocumentElement());
			}
			else {
				View view = inflater.inflate(templatePath(stringValueOf(resource)));
				if (view instanceof ViewGroup)
					viewGroup = (ViewGroup) view;
				else {
					viewGroup = new ViewGroup(context);
					addView(view, view.getLayoutParams());
				}
			}
			viewGroup.setTag("form");
		}

		protected String templatePath(String name) {
			return Resources.getRelativePath(6, name);
		}

		public void setLabel(Object labelText, ViewGroup vg) {
			TextView textView = (TextView) vg.getChildAt(0);
			textView.setText(stringValueOf(labelText));
		}
		
		public EditText getEdit(ViewGroup vg) {
			return (EditText) vg.getChildAt(1);
		}
		
		public ImageView getImage(ViewGroup vg) {
			return (ImageView) vg.getChildAt(1);
		}
		
	    public void addView(View view, ViewGroup.LayoutParams params) {
	    	viewGroup.addView(view, params);
		}
	    
	    public ViewGroup addTextField(Object description, Object...params) {
	    	ViewGroup vg = (ViewGroup) inflater.inflate(templatePath("field_text.xml"), params);
			addView(vg, vg.getLayoutParams());
			setLabel(description, vg);
			return vg;
		}

	    public ViewGroup addStringField(Object description, Object...params) {
	    	ViewGroup vg = (ViewGroup) inflater.inflate(templatePath("field_string.xml"), params);
			addView(vg, vg.getLayoutParams());
			setLabel(description, vg);
			return vg;
		}

		public ViewGroup addIntegerField(Object description, Object...params) {
			ViewGroup vg = (ViewGroup) inflater.inflate(templatePath("field_integer.xml"), params);
			addView(vg, vg.getLayoutParams());
			setLabel(description, vg);
			return vg;
		}

		public ViewGroup addFloatField(Object description, Object...params) {
			ViewGroup vg = (ViewGroup) inflater.inflate(templatePath("field_float.xml"), params);
			addView(vg, vg.getLayoutParams());
			setLabel(description, vg);
			return vg;
		}

		public ViewGroup addBlobField(Object description, Object...params) {
			ViewGroup vg = (ViewGroup) inflater.inflate(templatePath("field_blob.xml"), params);
			addView(vg, vg.getLayoutParams());
			setLabel(description, vg);
			return vg;
		}

		public View addField(Object description, String key, String type) {
			ViewGroup vg = null;
			switch (fieldTypeAffinity(type)) {
			case Cursor.FIELD_TYPE_STRING:
				vg = addStringField(description, key);
				break;
			case Cursor.FIELD_TYPE_INTEGER:
				vg = addIntegerField(description, key);
				break;
			case Cursor.FIELD_TYPE_FLOAT:
				vg = addFloatField(description, key);
				break;
			case Cursor.FIELD_TYPE_BLOB:
				vg = addBlobField(description, key);
				break;
			default:
				Log.w(TAG, String.format("type of field '%s' not identified : %s", key, type));
				break;
			}
			if (vg != null)
				return vg.findViewWithTag(key);
			else
				return null;
		}

	}
}

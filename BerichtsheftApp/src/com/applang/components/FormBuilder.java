package com.applang.components;

import org.w3c.dom.Document;

import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import static com.applang.Util.*;
import static com.applang.Util2.*;

public class FormBuilder extends Layouter
{
	private static final String TAG = FormBuilder.class.getSimpleName();
	
	protected LayoutInflater inflater = null;

	public FormBuilder(Context context, String resName) {
		super(context);
		inflater = LayoutInflater.from(mContext);
		if (notNullOrEmpty(resName)) {
			View view = inflater.inflate(templatePath(resName));
			if (view instanceof ViewGroup)
				viewGroup = (ViewGroup) view;
			else {
				viewGroup = new ViewGroup(mContext);
				addView(view, view.getLayoutParams());
			}
		}
		else {
			viewGroup = new ViewGroup(mContext);
		}
		viewGroup.setTag("form");
	}

	public FormBuilder(Context context, int resId) {
		this(context, null);
		Document document = mContext.getResources().getXml(resId);
		viewGroup = (ViewGroup) inflater.inflate(document.getDocumentElement());
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
    
    public void addTextField(Object description, Object...params) {
    	ViewGroup vg = (ViewGroup) inflater.inflate(templatePath("field_text.xml"), params);
		addView(vg, vg.getLayoutParams());
		setLabel(description, vg);
	}

    public void addStringField(Object description, Object...params) {
    	ViewGroup vg = (ViewGroup) inflater.inflate(templatePath("field_string.xml"), params);
		addView(vg, vg.getLayoutParams());
		setLabel(description, vg);
	}

	public void addIntegerField(Object description, Object...params) {
		ViewGroup vg = (ViewGroup) inflater.inflate(templatePath("field_integer.xml"), params);
		addView(vg, vg.getLayoutParams());
		setLabel(description, vg);
	}

	public void addFloatField(Object description, Object...params) {
		ViewGroup vg = (ViewGroup) inflater.inflate(templatePath("field_float.xml"), params);
		addView(vg, vg.getLayoutParams());
		setLabel(description, vg);
	}

	public void addBlobField(Object description, Object...params) {
		ViewGroup vg = (ViewGroup) inflater.inflate(templatePath("field_blob.xml"), params);
		addView(vg, vg.getLayoutParams());
		setLabel(description, vg);
	}

	public void addField(Object description, String key, String type) {
		switch (fieldTypeAffinity(type)) {
		case Cursor.FIELD_TYPE_STRING:
			addStringField(description, key);
			break;
		case Cursor.FIELD_TYPE_INTEGER:
			addIntegerField(description, key);
			break;
		case Cursor.FIELD_TYPE_FLOAT:
			addFloatField(description, key);
			break;
		case Cursor.FIELD_TYPE_BLOB:
			addBlobField(description, key);
			break;
		default:
			Log.w(TAG, String.format("type of field '%s' not identified : %s", key, type));
			break;
		}
	}

}

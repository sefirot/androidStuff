package com.applang.components;

import org.w3c.dom.Document;

import android.content.Context;
import android.content.res.Resources;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import static com.applang.Util.*;
import static com.applang.Util2.*;

public class FormBuilder extends LayoutBuilder
{
	public FormBuilder(Context context, ViewGroup viewGroup) {
		super(context, viewGroup);
	}

	public FormBuilder(Context context, String resName) {
		super(context, resName);
		getViewGroup().setTag("form");
	}

	public FormBuilder(Context context, int resId) {
		super(context);
		Document document = mContext.getResources().getXml(resId);
		setViewGroup((ViewGroup) inflater.inflate(document.getDocumentElement()));
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
    
    public void addTextField(Object labelText) {
    	ViewGroup vg = (ViewGroup) inflater.inflate(Resources.getRelativePath(6, "field_text.xml"));
		addView(vg, vg.getLayoutParams());
		setLabel(labelText, vg);
	}

    public void addStringField(Object labelText) {
    	ViewGroup vg = (ViewGroup) inflater.inflate(Resources.getRelativePath(6, "field_string.xml"));
		addView(vg, vg.getLayoutParams());
		setLabel(labelText, vg);
	}

	public void addIntegerField(Object labelText) {
		ViewGroup vg = (ViewGroup) inflater.inflate(Resources.getRelativePath(6, "field_integer.xml"));
		addView(vg, vg.getLayoutParams());
		setLabel(labelText, vg);
	}

	public void addFloatField(Object labelText) {
		ViewGroup vg = (ViewGroup) inflater.inflate(Resources.getRelativePath(6, "field_float.xml"));
		addView(vg, vg.getLayoutParams());
		setLabel(labelText, vg);
	}

	public void addBlobField(Object labelText) {
		ViewGroup vg = (ViewGroup) inflater.inflate(Resources.getRelativePath(6, "field_blob.xml"));
		addView(vg, vg.getLayoutParams());
		setLabel(labelText, vg);
	}

}

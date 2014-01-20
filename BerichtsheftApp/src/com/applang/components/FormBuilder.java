package com.applang.components;

import android.content.Context;
import android.content.res.Resources;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import static com.applang.Util.*;
import static com.applang.Util2.*;

public class FormBuilder extends LayoutBuilder {

	public FormBuilder(Context context, String name) {
		super(context, name);
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
    	ViewGroup vg = (ViewGroup) inflater.inflate(Resources.getRelativePath(6, "field_text.xml"), viewGroup);
		setLabel(labelText, vg);
		EditText editText = getEdit(vg);
		editText.setMovementMethod(new ScrollingMovementMethod());
	}

    public void addStringField(Object labelText) {
    	ViewGroup vg = (ViewGroup) inflater.inflate(Resources.getRelativePath(6, "field_string.xml"), viewGroup);
		setLabel(labelText, vg);
	}

	public void addIntegerField(Object labelText) {
		ViewGroup vg = (ViewGroup) inflater.inflate(Resources.getRelativePath(6, "field_integer.xml"), viewGroup);
		setLabel(labelText, vg);
	}

	public void addFloatField(Object labelText) {
		ViewGroup vg = (ViewGroup) inflater.inflate(Resources.getRelativePath(6, "field_float.xml"), viewGroup);
		setLabel(labelText, vg);
	}

	public void addBlobField(Object labelText) {
		ViewGroup vg = (ViewGroup) inflater.inflate(Resources.getRelativePath(6, "field_blob.xml"), viewGroup);
		setLabel(labelText, vg);
	}

}

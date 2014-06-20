package com.applang.components;

import java.awt.Container;
import java.awt.Image;

import javax.swing.JComponent;
import javax.swing.JLabel;

import org.w3c.dom.Document;

import com.applang.components.DataView.ProjectionModel;

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

public class DataForm
{
	private static final String TAG = DataForm.class.getSimpleName();
	
	public DataForm(Context context, ManagerBase<?> manager, ProjectionModel projectionModel, Object...resource) {
		mManager = manager;
		boolean standard = notAvailable(0, resource);
		if (standard) 
			resource = objects("standard_form.xml");
		builder = new Builder(context, resource[0]);
		mProjectionModel = projectionModel;
		if (mProjectionModel != null) {
			BidiMultiMap projection = mProjectionModel.getProjection();
			projection = new BidiMultiMap(
					projection.getValues(0), 
					projection.getValues(1), 
					projection.getValues(2), 
					vlist(), 
					projection.getValues(4));
			for (Object key : projection.getKeys()) {
				String name = stringValueOf(key);
				String type = stringValueOf(projection.getValue(key, 2));
				String style = stringValueOf(projection.getValue(key, 4));
				if (nullOrEmpty(style))
					style = type.toLowerCase();
				fields.add(standard ? 
						builder.addStandardField(key, name, style) : 
						layout.findViewWithTag(name));
			}
		}
	}

	public Builder builder;
	
	private ValList fields = vlist();
	
	private ViewGroup layout = null;
    
    public Container getContainer() {
    	return ViewGroup.build(layout, true);
    }
    
	public ProjectionModel mProjectionModel;

    private int fieldType(BidiMultiMap projection, Object key) {
    	return fieldTypeAffinity(stringValueOf(projection.getValue(key, 2)));
    }
	
	private Object doConversion(BidiMultiMap projection, Object key, Object value, String oper) {
		Object conversion = projection.getValue(key, 1);
		if (notNullOrEmpty(conversion))
			return ScriptManager.doConversion(value, stringValueOf(conversion), oper);
		else
			return value;
	}

	public Object[] getContent() {
		BidiMultiMap projection = mProjectionModel.getProjection();
		ValList list = vlist();
		ValList keys = projection.getKeys();
		for (int i = 0; i < keys.size(); i++) 
			list.add(getContent(projection, keys.get(i)));
		return list.toArray();
	}

	private Object getContent(BidiMultiMap projection, Object key) {
		Object value = null;
		View view = getFieldView(projection, key);
		if (view != null) {
			switch (fieldType(projection, key)) {
			case Cursor.FIELD_TYPE_BLOB:
				value = ((ImageView)view).getImage();
				break;
			default:
				if (view instanceof TextEdit) {
					TextEdit textEdit = (TextEdit)view;
					if (textEdit.getTextToggle() != null)
						value = textEdit.getScript();
					else
						value = textEdit.getText();
				}
				else
					value = ((EditText)view).getText();
			}
		}
		return doConversion(projection, key, value, "pull");
    }

	private View getFieldView(BidiMultiMap projection, Object key) {
		return (View) fields.get(projection.getKeys().indexOf(key));
	}

	public void setContent(final Object[] values) {
		final BidiMultiMap projection = mProjectionModel.getProjection();
		mManager.blockDirty(new Job<Void>() {
			public void perform(Void t, Object[] parms) throws Exception {
				ValList keys = projection.getKeys();
				for (int i = 0; i < keys.size(); i++) {
					Object key = keys.get(i);
					setContent(projection, key, values[i]);
				}
			}
		});
    }

	private void setContent(BidiMultiMap projection, final Object key, Object value) {
		View view = getFieldView(projection, key);
		if (view != null) {
			final Object o = doConversion(projection, key, value, "push");
			switch (fieldType(projection, key)) {
			case Cursor.FIELD_TYPE_BLOB:
				((ImageView)view).setImage((Image) o);
				break;
			default:
				String text = stringValueOf(o);
				if (view instanceof TextEdit) {
					TextEdit textEdit = (TextEdit)view;
					if (textEdit.getTextToggle() != null) {
						textEdit.setScript(text);
						textEdit.setText(textEdit.getScript());
					}
					else
						textEdit.setText(text);
				}
				else
					((EditText)view).setText(text);
			}
		}
	}
	
	private ManagerBase<?> mManager;
	
	private Job<JComponent> onChanged = new Job<JComponent>() {
		public void perform(JComponent t, Object[] params) throws Exception {
			mManager.setDirty(true);
		}
	};
	
	public class Builder
	{
		protected LayoutInflater inflater = null;

		public Builder(Context context, Object resource) {
			inflater = LayoutInflater.from(context);
			if (nullOrEmpty(resource)) {
				layout = new ViewGroup(context);
			}
			else if (resource instanceof Integer) {
				Document document = context.getResources().getXml((Integer) resource);
				layout = (ViewGroup) inflater.inflate(document.getDocumentElement());
			}
			else {
				View view = inflater.inflate(templatePath(stringValueOf(resource)));
				if (view instanceof ViewGroup)
					layout = (ViewGroup) view;
				else {
					layout = new ViewGroup(context);
					addView(view, view.getLayoutParams());
				}
			}
			layout.setTag("form");
		}

		protected String templatePath(String name) {
			return Resources.getRelativePath(6, name);
		}
		
		public TextView getLabel(ViewGroup vg) {
			return (TextView) vg.getChildAt(0);
		}

		public void setLabelText(Object text, ViewGroup vg) {
			TextView textView = getLabel(vg);
			textView.setText(stringValueOf(text));
		}
		
		public View getEdit(ViewGroup vg) {
			return vg.getChildAt(1);
		}
		
	    public void addView(View view, ViewGroup.LayoutParams params) {
	    	layout.addView(view, params);
		}

		public View addStandardField(Object description, String name, String style) {
			ViewGroup vg = (ViewGroup) inflater.inflate(
					templatePath("standard_field.xml"), 
					name, 
					style);
			addView(vg, vg.getLayoutParams());
			setLabelText(description, vg);
			JLabel label = getLabel(vg).taggedComponent();
			View vw = getEdit(vg);
			if (vw instanceof EditText) {
				((EditText) vw).setOnTextChanged(onChanged);
				if (vw instanceof TextEdit) {
					TextEdit te = (TextEdit) vw;
					if (te.getTextToggle() != null)
						te.getTextToggle().setOnTextChanged(onChanged);
				}
			}
			label.setLabelFor(vw.taggedComponent());
			return vg.findViewWithTag(name);
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
	    
	    public ViewGroup addTextField(Object description, Object...params) {
	    	ViewGroup vg = (ViewGroup) inflater.inflate(templatePath("field_text.xml"), params);
			addView(vg, vg.getLayoutParams());
			setLabelText(description, vg);
			return vg;
		}

	    public ViewGroup addStringField(Object description, Object...params) {
	    	ViewGroup vg = (ViewGroup) inflater.inflate(templatePath("field_string.xml"), params);
			addView(vg, vg.getLayoutParams());
			setLabelText(description, vg);
			return vg;
		}

		public ViewGroup addIntegerField(Object description, Object...params) {
			ViewGroup vg = (ViewGroup) inflater.inflate(templatePath("field_integer.xml"), params);
			addView(vg, vg.getLayoutParams());
			setLabelText(description, vg);
			return vg;
		}

		public ViewGroup addFloatField(Object description, Object...params) {
			ViewGroup vg = (ViewGroup) inflater.inflate(templatePath("field_float.xml"), params);
			addView(vg, vg.getLayoutParams());
			setLabelText(description, vg);
			return vg;
		}

		public ViewGroup addBlobField(Object description, Object...params) {
			ViewGroup vg = (ViewGroup) inflater.inflate(templatePath("field_blob.xml"), params);
			addView(vg, vg.getLayoutParams());
			setLabelText(description, vg);
			return vg;
		}

	}
}

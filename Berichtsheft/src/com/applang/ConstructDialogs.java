package com.applang;

import static com.applang.Util.*;
import static com.applang.Util1.*;
import static com.applang.Util2.*;
import static com.applang.VelocityUtil.*;

import com.applang.berichtsheft.R;

import java.util.Arrays;
import java.util.Collection;
import java.util.Locale;
import java.util.regex.MatchResult;

import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.os.AsyncTask;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class ConstructDialogs extends Dialogs
{
	public static final String CONSTRUCT_ACTION = "com.applang.action.CONSTRUCT";
	public static final int DIALOG_CONSTRUCT = 11;
	
	private static final String LAYOUT_ID = "id";
	private static final String FIELD_ID = "id_2";
	private static final String LABEL_ID = "id_1";
	private static final String TEXT = "text";
	private static final String COMMENT = "comment";
	private static final String OPTIONAL = "optional";
	private static final String ARGUMENT_TYPE = "type";
	private static final String ARGUMENT = "argument";
    private static final String SECOND_FIELD = "second";
	
	private LinearLayout contentView;
	private ScrollView scrollView;
    private String[] defaults;
	
	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case DIALOG_CONSTRUCT:
	    	defaults = defaultValues.toArray(new String[0]);
			mDialog = new Dialog(this) {
				@Override
				public boolean onKeyDown(int keyCode, KeyEvent event) {
				    if ((keyCode == KeyEvent.KEYCODE_BACK)) {
				    	return true;
				    }
					return super.onKeyDown(keyCode, event);
				}
				@Override
				public boolean onKeyUp(int keyCode, KeyEvent event) {
				    if ((keyCode == KeyEvent.KEYCODE_BACK)) {
				    	return true;
				    }
					return super.onKeyUp(keyCode, event);
				}
			};
			mDialog.setTitle(prompt);
			contentView = contentPane();
			contentView.addView(header());
			contentView.addView(scrollView = form());
			mDialog.setContentView(contentView);
			makeSuggestions();
			validate();
			return mDialog;
		}
		return null;
	}

    private View focused = null;
    
    private LinearLayout contentPane(Integer... params) {
    	LinearLayout vertLayout = Util1.linearLayout(this, 
    			LinearLayout.VERTICAL, 
    			param(LayoutParams.FILL_PARENT, 0, params), 
    			param(LayoutParams.FILL_PARENT, 1, params));
    	return vertLayout;
    }

	private boolean isExtensible() {
		return values.length > 0 && UNKNOWN.equals(values[values.length - 1]);
	}

    private RelativeLayout header() {
//		RelativeLayout relLayout = relativeLayout(this, 
//				LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT);
		RelativeLayout relLayout = (RelativeLayout) LayoutInflater.from(this)
				.inflate(isExtensible() ? 
						R.layout.construct_form_header2 : 
						R.layout.construct_form_header, null);
//		ImageButton im = new ImageButton(this);
//		im.setId(R.id.button4);
//		im.setImageResource(R.drawable.dropdown);
//    	layoutParams = 
//    			relativeLayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, 
//    					halfMargin, halfMargin, halfMargin, halfMargin);
//		layoutParams.addRule(RelativeLayout.ALIGN_PARENT_TOP);
//		layoutParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
//		relLayout.addView(im, layoutParams);
		ImageButton im = (ImageButton) relLayout.findViewById(R.id.button1);
		im.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (focused != null) {
					if (focused instanceof AutoCompleteTextView) {
						AutoCompleteTextView actv = (AutoCompleteTextView) focused;
						if (actv.isPopupShowing())
							actv.dismissDropDown();
						else
							actv.showDropDown();
					}
					else
						focused.requestFocus();
				}
			}
		});
//		Button btn = new Button(this);
//		btn.setId(R.id.ok);
//		btn.setText(android.R.string.ok);
//		btn.setPadding(halfpadding, 0, halfpadding, 0);
//    	layoutParams = 
//    			relativeLayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, 
//    					halfMargin, halfMargin, halfMargin, halfMargin);
//		layoutParams.addRule(RelativeLayout.ALIGN_PARENT_TOP);
//		layoutParams.addRule(RelativeLayout.LEFT_OF, R.id.cancel);
//		relLayout.addView(btn, layoutParams);
		Button btn = (Button) relLayout.findViewById(R.id.ok);
		btn.setEnabled(false);
		btn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
            	if (formValidate(true)) {
					Object result = formEvaluate();
					mDialog.dismiss();
					_finish(RESULT_OK, result);
				}
			}
		});
//		btn = new Button(this);
//		btn.setId(R.id.cancel);
//		btn.setText(android.R.string.cancel);
//    	layoutParams = 
//    			relativeLayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, 
//    					halfMargin, halfMargin, halfMargin, halfMargin);
//		layoutParams.addRule(RelativeLayout.ALIGN_PARENT_TOP);
//		layoutParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
//		relLayout.addView(btn, layoutParams);
		btn = (Button) relLayout.findViewById(R.id.cancel);
		btn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				mDialog.cancel();
            	_finish(RESULT_CANCELED, null);
			}
		});
		if (isExtensible()) {
			btn = (Button) relLayout.findViewById(R.id.button2);
			btn.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
		        	ValMap map = (ValMap) fieldMaps.get(-1);
		        	String arg = (String) map.get(ARGUMENT);
		    		MatchResult m = argumentSuffix(arg);
					int index = toInt(1, m.group());
					arg = arg.substring(0, m.start()) + (++index) + arg.substring(m.end());
					if (addField(arg)) {
						index = fieldMaps.size();
						makeSuggestions(index);
						enableMinus();
					}
				}
			});
			btn = (Button) relLayout.findViewById(R.id.button3);
			btn.setEnabled(false);
			btn.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
		        	ValMap map = (ValMap) fieldMaps.get(-1);
					View layout = getLayout(map);
					if (layout != null) {
						((ViewGroup)layout.getParent()).removeView(layout);
						fieldMaps.remove(map);
						enableMinus();
					}
				}
			});
		}
		return relLayout;
    }
    
    private void enableMinus() {
		Button minus = (Button) contentView.findViewById(R.id.button3);
		int size = fieldMaps.size();
		int length = values.length;
		minus.setEnabled(size >= length);
    }
    
    private void enableOK(boolean enabled) {
		Button ok = (Button) contentView.findViewById(R.id.ok);
		ok.setEnabled(enabled);
    }

	private ScrollView form() {
    	scrollView = new ScrollView(this);
    	scrollView.setLayoutParams(
    			new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
        LinearLayout vertLayout = 
        		contentPane(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT);
        vertLayout.setId(android.R.id.content);
        scrollView.addView(vertLayout);
        for (int i = 0; i < values.length; i++) {
        	String argument = values[i];
        	if (!UNKNOWN.equals(argument)) 
        		addField(argument);
        }
		return scrollView;
    }
	
    private ValList fieldMaps = new ValList();

	private boolean addField(String argument) {
		ValMap map = formLine(argument);
		boolean retval = map != null;
		if (retval)
			fieldMaps.add(map);
		return retval;
	}
	
	private ValMap formLine(String argument) {
		int index = fieldMaps.size();
		ValMap map = new ValMap();
		String type = argumentType(argument);
		boolean canDefault = Character.isLowerCase(type.charAt(0));
		LinearLayout linearLayout = canDefault ? 
				lineLayoutWithDefault(argument, index, map) : 
				lineLayout(true, argument, index, map);
		LinearLayout vertLayout = (LinearLayout) scrollView.findViewById(android.R.id.content);
		vertLayout.addView(linearLayout);
		return map;
	}

	private LinearLayout lineLayoutWithDefault(String argument, final int index, ValMap map) {
        LinearLayout vertLayout = 
        		contentPane(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT);
		LinearLayout horzLayout = lineLayout(true, argument, index, map);
		int id = horzLayout.getId();
		horzLayout.setId(View.NO_ID);
		vertLayout.addView(horzLayout);
		vertLayout.setId(id);
		String type = argumentType(argument);
		MatchResult m = argumentSuffix(argument);
		int suffix = toInt(1, m.group());
		argument = "default" + suffix + 
			(isType(0, type) ? 
				ARGUMENT_TYPER + DATA_TYPES[EXPR_TYPE_INDEX] : 
				argument.substring(m.end()));
		ValMap map2 = new ValMap();
		horzLayout = lineLayout(false, optionalize(argument), index, map2);
		map.put(SECOND_FIELD, map2);
		vertLayout.addView(horzLayout);
		return vertLayout;
	}

	private LinearLayout lineLayout(final boolean simple, String argument, final int index, ValMap map) {
		int id = 10 * (1 + index);
		final String type = argumentType(argument);
		boolean optional = isOptionalArgument(argument);
//			LinearLayout horzLayout = linearLayout(this,
//					LinearLayout.HORIZONTAL, 
//					LayoutParams.FILL_PARENT,
//					LayoutParams.WRAP_CONTENT);
//			final TextView label = optional ? 
//					new CheckBox(this) : 
//					new TextView(this);
		LinearLayout horzLayout = (LinearLayout) LayoutInflater.from(this)
				.inflate(optional ? 
						R.layout.construct_form_line1 :
						R.layout.construct_form_line, null);
		if (simple)
			horzLayout.setId(id);
		TextView label = (TextView) horzLayout.findViewById(R.id.textView1);
		String name = argumentName(argument);
		label.setText(name);
		label.setId(id + (simple ? 1 : 3));
		if (optional) {
			CheckBox checkBox = (CheckBox) label;
			checkBox.setOnCheckedChangeListener(new OnCheckedChangeListener() {
				@Override
				public void onCheckedChanged(CompoundButton view, boolean isChecked) {
					View v = scrollView.findViewById(view.getId() + 1);
					if (v != null) {
						v.setEnabled(isChecked);
						if (isChecked) {
							if (simple)
								makeSuggestions(index);
							else
								attachSuggestions(suggestionList(type), v);
						}
						validate();
					}
				}
			});
			checkBox.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					CheckBox cb = (CheckBox) v;
					boolean flag = cb.isChecked();
					int id = cb.getId();
					do {
						id = id - (flag ? 10 : -10);
						v = scrollView.findViewById(id);
						if (v instanceof CheckBox) {
							cb = (CheckBox) v;
							if (flag ^ cb.isChecked())
								cb.setChecked(flag);
						}
					} while (v != null);
				}
			});
		}
//			horzLayout.addView(
//					label,
//					linearLayoutParams(LayoutParams.WRAP_CONTENT,
//							LayoutParams.WRAP_CONTENT, margin, halfMargin,
//							halfMargin, halfMargin));
//			boolean listAlike = false;	//	isTypeLike(56, type);
//			final AutoCompleteTextView field = listAlike ? 
//					new MultiAutoCompleteTextView(this) :
//						new AutoCompleteTextView(this);
//					field.setThreshold(1);
//			if (listAlike)
//				((MultiAutoCompleteTextView)field)
//				.setTokenizer(new MultiAutoCompleteTextView.CommaTokenizer());
		final AutoCompleteTextView field = 
				(AutoCompleteTextView) horzLayout.findViewById(R.id.autoCompleteTextView1);
		field.setText(param("", index, defaults));
		field.setId(id + (simple ? 2 : 4));
		field.setEnabled(!optional);
		field.setOnFocusChangeListener(new OnFocusChangeListener() {
			@Override
			public void onFocusChange(View v, boolean hasFocus) {
				if (v.equals(focused) && !hasFocus)
					focused = null;
				if (v.equals(field) && hasFocus)
					focused = field;
			}
		});
		field.addTextChangedListener(new TextWatcher() {
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				validate();
			}
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {
			}
			@Override
			public void afterTextChanged(Editable s) {
			}
		});
//			horzLayout.addView(
//					field,
//					linearLayoutParams(LayoutParams.FILL_PARENT,
//							LayoutParams.WRAP_CONTENT, halfMargin,
//							halfMargin, margin, halfMargin));
		map.put(ARGUMENT, argument);
		map.put(LAYOUT_ID, horzLayout.getId());
		map.put(LABEL_ID, label.getId());
		map.put(FIELD_ID, field.getId());
		map.put(ARGUMENT_TYPE, type);
		map.put(OPTIONAL, optional);
		return horzLayout;
	}

	private void addComment(ValMap map, String prefix, boolean append, String comment) {
		String old = valueOrElse("", map.get(COMMENT));
		if (append) 
			comment = old + "\n" + comment;
		else
			comment += "\n" + old;
		if (notNullOrEmpty(prefix))
			comment = prefix + " : " + comment;
		map.put(COMMENT, strip(Constraint.START, comment, "\n"));
    }
	
	private boolean isTypeLike(int typeIndex, String type) {
		if (typeIndex == 56)
			return isType(5, type) || isType(6, type);
		else
			return isType(typeIndex, type);
	}

	private boolean anyTypeAllowed(String type) {
		return ANY.equals(type);
	}
	
	private Boolean isOk;
	
	private Boolean isContentValid(final ValMap map, Object... params) {
		final String type = getType(map);
    	String text = getText(map);
    	String prefix = param_String("", 0, params);
    	isOk = param_Boolean(true, 1, params);
    	if (!isOk)
    		return false;
    	String[] messages = getResources().getStringArray(R.array.construct_validation_messages);
    	if (text.length() < 1) {
    		addComment(map, prefix, true, messages[8]);
			return false;
    	}
    	if (isTypeLike(IDENTIFIER_TYPE_INDEX, type)) {
    		isOk = compliesWith(IDENTIFIER_TYPE_INDEX, text);
    		if (!isOk)
    			addComment(map, prefix, true, messages[IDENTIFIER_TYPE_INDEX]);
    		return isOk;
    	}
    	if (compliesWith(0, text))
    		return true;
    	else if (isTypeLike(0, type)) {
			addComment(map, prefix, true, messages[0]);
			return false;
		}
    	boolean allowed = anyTypeAllowed(type);
    	for (int t = 1; t < IDENTIFIER_TYPE_INDEX; t++) {
    		boolean required = isTypeLike(t, type);
			if (required || allowed) {
				if (compliesWith(t, text)) 
					return true;
				else if (required) {
    				addComment(map, prefix, true, messages[t]);
    				return false;
    			}
    		}
		}
		if (isTypeLike(56, type)) {
			try {
				walkJSON(null, new JSONObject(text), new Function<Object>() {
					public Object apply(Object...params) {
						Object[] path = param(null, 0, params);
						Object value = param(null, 1, params);
						String name = Arrays.toString(path);
						map.put(TEXT, value.toString());
						map.put(ARGUMENT_TYPE, ANY);
						isOk &= isContentValid(map, name, isOk);
						map.remove(TEXT);
						map.put(ARGUMENT_TYPE, type);
						return value;
					}
				});
			} 
			catch (Exception e) {
				addComment(map, prefix, true, e.getMessage());
				return false;
			}
	    	if (!isOk)
	    		return false;
		}
		if (allowed) {
    		addComment(map, prefix, true, messages[9]);
			return false;
		}
		return true;
	}
    
	private ValList suggestionList(final String type) {
		ValList list = new ValList();
		if (type.length() < 1) 
			list.add(0, UNKNOWN);
		else {
			Object[] dummies = getDummies(0);
			if (!isTypeLike(IDENTIFIER_TYPE_INDEX, type))
				list.addAll(Arrays.asList(dummies));
	    	for (int t = 1; t < DATA_TYPES.length; t++) {
	    		if (isTypeLike(t, type)) {
					dummies = getDummies(t);
					list.addAll(0, Arrays.asList(dummies));
				}
			}
		}
		Predicate<Object> isExcluded = new Predicate<Object>() {
			@Override
			public boolean apply(Object object) {
				if (!isType(3, type) && MATH_TOOL.equals(object))
					return true;
				if (!isType(URI_TYPE_INDEX, type) && 
						object.toString().toLowerCase(Locale.getDefault()).endsWith("uri"))
					return true;
				return false;
			}
		};
		Collection<Object> suggestions = userContext.suggestions(isExcluded, true);
		list.addAll(0, suggestions);
		return list;
	}
	
	private void attachSuggestions(ValMap map) {
		if (map != null) {
			ValList list = (ValList) map.get("suggest");
			attachSuggestions(list, getField(map));
		}
	}

	private void attachSuggestions(ValList list, View vw) {
		if (list != null) {
			ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
					android.R.layout.simple_dropdown_item_1line,
					list.toArray(new String[0]));
			if (vw instanceof AutoCompleteTextView)
				((AutoCompleteTextView)vw).setAdapter(adapter);
		}
	}
    
    private void makeSuggestions(Integer... indices) {
    	new SuggestionTask().execute(indices);
    }
    
	@SuppressLint("NewApi")
	private class SuggestionTask extends AsyncTask<Integer, ValMap, ValMap>
    {
		@Override
		protected ValMap doInBackground(Integer... index) {
	    	for (int i = param(0, 0, index); i < fieldMaps.size(); i++) {
	        	ValMap fieldMap = (ValMap) fieldMaps.get(i);
				if (isInactive(fieldMap))
					continue;
				String type = getType(fieldMap);
				fieldMap.put("suggest", suggestionList(type));
				if (index.length > 0)
					return fieldMap;
				else
					publishProgress(fieldMap);
			}
	    	return null;
		}
		
		@Override
		protected void onProgressUpdate(ValMap... maps) {
			attachSuggestions(maps[0]);
		}

		@Override
		protected void onPostExecute(ValMap result) {
			super.onPostExecute(result);
			if (result != null) 
				onProgressUpdate(result);
		}
    }

	private void validate() {
		new ValidationTask().execute();
	}
    
	@SuppressLint("NewApi")
	private class ValidationTask extends AsyncTask<Integer, Boolean, Boolean>
    {
		@Override
		protected Boolean doInBackground(Integer... params) {
			return formValidate(false);
		}
		@Override
		protected void onPostExecute(Boolean result) {
			super.onPostExecute(result);
			enableOK(result);
		}
    }

	private View getLayout(ValMap map) {
		Integer id = (Integer) map.get(LAYOUT_ID);
		return contentView.findViewById(id);
	}

	private View getField(ValMap map) {
		Integer id = (Integer) map.get(FIELD_ID);
		return contentView.findViewById(id);
	}
	
	private String getText(ValMap map) {
		if (map.containsKey(TEXT))
			return map.get(TEXT).toString();
		else {
	    	TextView field = (TextView) getField(map);
	    	return field.getText().toString();
		}
	}
	
	private String getType(ValMap map) {
		return map.get(ARGUMENT_TYPE).toString();
	}
	
	private boolean isInactive(ValMap map) {
		Boolean optional = valueOrElse(false, map.get(OPTIONAL));
		if (optional) {
			Integer id = (Integer) map.get(LABEL_ID);
			CheckBox label = (CheckBox) contentView.findViewById(id);
			return label.isChecked() == false;
		}
		return false;
	}

	private boolean fieldValidate(boolean toast, boolean valid, ValMap fieldMap) {
		if (isInactive(fieldMap))
			return valid;
		valid &= isContentValid(fieldMap);
		if (!valid) {
			if (toast) {
				Toast.makeText(ConstructDialogs.this,
						fieldMap.get(COMMENT).toString(),
						Toast.LENGTH_SHORT).show();
				getField(fieldMap).requestFocus();
			}
			fieldMap.remove(COMMENT);
			return false;
		}
		if (fieldMap.containsKey(SECOND_FIELD)) {
			fieldMap = (ValMap) fieldMap.get(SECOND_FIELD);
			return fieldValidate(toast, valid, fieldMap);
		}
		return true;
	}
	
	protected boolean formValidate(boolean toast) {
		boolean valid = true;
		for (int i = 0; i < fieldMaps.size(); i++) {
			ValMap fieldMap = (ValMap) fieldMaps.get(i);
			valid = fieldValidate(toast, valid, fieldMap);
			if (!valid)
				break;
		}
		return valid;
	}
    
    private Object formEvaluate() {
    	ValList list = new ValList();
        for (int i = 0; i < fieldMaps.size(); i++) {
        	ValMap fieldMap = (ValMap) fieldMaps.get(i);
			if (isInactive(fieldMap))
				continue;
			list.add(getText(fieldMap));
		}
    	return list;
	}

}

package com.applang;

import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
//import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.Bundle;
//import android.os.Handler;
//import android.os.Message;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.ViewGroup.LayoutParams;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.database.Cursor;
import android.graphics.Color;

import static com.applang.Util.*;
import static com.applang.Util1.*;
import static com.applang.VelocityUtil.*;

import com.applang.berichtsheft.R;

public class Dialogs extends Activity
{
	protected static final String TAG = Dialogs.class.getSimpleName();
	
	public static final String PROMPT_ACTION = "com.applang.action.PROMPT";
	
	public static final int DIALOG_YES_NO_MESSAGE = 1;
    public static final int DIALOG_YES_NO_LONG_MESSAGE = 2;
    public static final int DIALOG_LIST = 3;
    public static final int DIALOG_PROGRESS = 4;
    public static final int DIALOG_SINGLE_CHOICE = 5;
    public static final int DIALOG_MULTIPLE_CHOICE = 6;
    public static final int DIALOG_SINGLE_CHOICE_CURSOR = 7;
    public static final int DIALOG_MULTIPLE_CHOICE_CURSOR = 8;
    public static final int DIALOG_TEXT_ENTRY = 9;
    public static final int DIALOG_TEXT_INFO = 10;

    protected Dialog mDialog;
/*
    public static final int MAX_PROGRESS = 100;
    public static int sProgress;
    public static ProgressDialog sProgressDialog;
    
    public static Handler sProgressHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg.what < 1) {
                sProgress = 0;
                sProgressDialog.setProgress(0);
            }
            else if (sProgress < MAX_PROGRESS) {
                sProgress = msg.what;
                sProgressDialog.setProgress(sProgress);
            }
            else 
            	sProgressDialog.dismiss();
        }
    };

	public static Runnable sProgressRunnable = null;
*/
	private int checkedItem = -1;
	private List<Boolean> checkedItems = alist();
	CursorHelper cursorHelper = new CursorHelper(this);
	private Cursor cursor;
	
	class CursorHelper
	{
		Activity activity;
		
		public CursorHelper(Activity activity) {
			this.activity = activity;
		}
		
		private Uri uri;
		private String selection;
		private String[] selectionArgs;
		
		public Cursor query(Bundle info) {
			uri = Uri.parse(info.getString("uri"));
	        selection = info.getString("selection");
			selectionArgs = info.getStringArray("selectionArgs");
			String sortOrder = info.getString("sortOrder");
			return activity.managedQuery(uri, 
					values, 
					selection, 
					selectionArgs, 
					sortOrder);
		}
		
		public void update(Bundle info, boolean checked) {
        	ContentValues contentValues = new ContentValues();
        	contentValues.put(values[2], checked);
        	activity.getContentResolver().update(uri, 
        			contentValues, 
        			values[0] + "=? and (" + selection + ")", 
        			arrayappend(strings("" + cursor.getLong(0)), selectionArgs));
		}
	}

    @Override
	protected void onPrepareDialog(int id, Dialog dialog) {
    	super.onPrepareDialog(id, dialog);
		switch (id) {
/*        case DIALOG_PROGRESS:
        	if (sProgressRunnable != null)
        		sProgressRunnable.run();
*/
		default:
		}
	}

	@Override
    protected Dialog onCreateDialog(int id) {
		mDialog = createDialog(id);
		return mDialog;
	}

    protected Dialog createDialog(int id) {
		switch (id) {
/*		case DIALOG_PROGRESS:
        	sProgress = 0;
        	sProgressDialog = new ProgressDialog(Dialogs.this);
        	sProgressDialog.setIcon(R.drawable.ic_launcher);
        	sProgressDialog.setTitle(prompt);
        	sProgressDialog.setProgressStyle(info.getInt(BaseDirective.STYLE, ProgressDialog.STYLE_HORIZONTAL));
        	sProgressDialog.setMax(MAX_PROGRESS);
        	sProgressDialog.setButton(getText(R.string.button_hide), new DialogInterface.OnClickListener() {
        		public void onClick(DialogInterface dialog, int whichButton) {
        			sProgressDialog.hide();
        		}
        	});
        	sProgressDialog.setButton2(getText(android.R.string.cancel), new DialogInterface.OnClickListener() {
        		public void onClick(DialogInterface dialog, int whichButton) {
        			sProgressDialog.cancel();
        		}
        	});
        	return sProgressDialog;
*/
		case DIALOG_YES_NO_MESSAGE:
            return new AlertDialog.Builder(Dialogs.this)
            	.setCancelable(false)
                .setIcon(R.drawable.ic_launcher)
                .setMessage(prompt)
                .setPositiveButton(values[0], new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        _finish(RESULT_OK, true);
                    }
                })
                .setNegativeButton(values[1], new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        _finish(RESULT_OK, false);
                    }
                })
                .create();
        case DIALOG_YES_NO_LONG_MESSAGE:
            return new AlertDialog.Builder(Dialogs.this)
        		.setCancelable(false)
                .setIcon(R.drawable.ic_launcher)
                .setMessage(prompt)
                .setPositiveButton(values[0], new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        _finish(RESULT_OK, values[0]);
                    }
                })
                .setNegativeButton(values[1], new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        _finish(RESULT_OK, values[1]);
                    }
                })
                .setNeutralButton(values[2], new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        _finish(RESULT_CANCELED, param_String(null, 2, arraycast(values, objects())));
                    }
                })
                .create();
        case DIALOG_SINGLE_CHOICE:
        	if (defaultValues != null && defaultValues.size() > 0) {
        		checkedItem = asList(values).indexOf(defaultValues.get(0));
        	}
            return new AlertDialog.Builder(Dialogs.this)
	    		.setCancelable(false)
	            .setIcon(R.drawable.ic_launcher)
                .setTitle(prompt)
                .setSingleChoiceItems(values, checkedItem, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                    	checkedItem = whichButton;
                    }
                })
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        _finish(RESULT_OK, checkedItem < 0 ? null : values[checkedItem]);
                    }
                })
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        _finish(RESULT_CANCELED, null);
                    }
                })
               .create();
        case DIALOG_MULTIPLE_CHOICE:
            for (int i = 0; i < values.length; i++) {
            	checkedItems.add(defaultValues != null ? 
            			defaultValues.contains(values[i]) : 
            			false);
			}
			return new AlertDialog.Builder(Dialogs.this)
    			.setCancelable(false)
                .setIcon(R.drawable.ic_launcher)
                .setTitle(prompt)
                .setMultiChoiceItems(values,
                		toBooleanArray(checkedItems),
                        new DialogInterface.OnMultiChoiceClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton, boolean isChecked) {
                            	checkedItems.set(whichButton, isChecked);
                            }
                        })
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						ValList list = vlist();
                    	Boolean[] checked = checkedItems.toArray(new Boolean[values.length]);
						for (int i = 0; i < checked.length; i++) {
							if (checked[i])
								list.add(values[i]);
						}
                        _finish(RESULT_OK, list);
                    }
                })
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        _finish(RESULT_CANCELED, null);
                    }
                })
               .create();
        case DIALOG_SINGLE_CHOICE_CURSOR:
        	try {
				cursor = cursorHelper.query(info);
				if (defaultValues.size() > 0) {
					if (cursor.moveToFirst())
						do {
							checkedItem++;
							if (defaultValues.containsAll(getStrings(cursor)))
								break;
						} while (cursor.moveToNext());
				}
				return new AlertDialog.Builder(Dialogs.this)
					.setCancelable(false)
				    .setIcon(R.drawable.ic_launcher)
				    .setTitle(prompt)
				    .setSingleChoiceItems(cursor,
				    		checkedItem,
				    		values[0],
				            new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,	int which) {
									checkedItem = which;
								}
				            })
				    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
				        public void onClick(DialogInterface dialog, int whichButton) {
				        	Object value = null;
				        	if (checkedItem > -1 && cursor.moveToFirst()) {
								for (int i = 1; i < checkedItem; i++)
									cursor.moveToNext();
								if (values.length > 1)
									value = getStrings(cursor);
								else
									value = cursor.getString(0);
							}
							_finish(RESULT_OK, value);
				        }
				    })
				    .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
				        public void onClick(DialogInterface dialog, int whichButton) {
				            _finish(RESULT_CANCELED, null);
				        }
				    })
				   .create();
        	} 
        	catch (Exception e) {
        		Log.e(TAG, "Dialogs", e);
			} 
        case DIALOG_MULTIPLE_CHOICE_CURSOR:
        	try {
				cursor = cursorHelper.query(info);
				if (cursor.moveToFirst())
					do {
						checkedItems.add(cursor.getInt(2) != 0);
					} while (cursor.moveToNext());
				return new AlertDialog.Builder(Dialogs.this)
					.setCancelable(false)
				    .setIcon(R.drawable.ic_launcher)
				    .setTitle(prompt)
				    .setMultiChoiceItems(cursor,
				    		values[2],
				            values[1],
				            new DialogInterface.OnMultiChoiceClickListener() {
				                public void onClick(DialogInterface dialog, int whichButton, boolean isChecked) {
				                	checkedItems.set(whichButton, isChecked);
				                }
				            })
				    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int whichButton) {
							int i;
							for (i = 0, cursor.moveToFirst(); i < checkedItems.size(); i++, cursor.moveToNext())
								cursorHelper.update(info, checkedItems.get(i));
				            _finish(RESULT_OK, null);
				        }
				    })
				    .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
				        public void onClick(DialogInterface dialog, int whichButton) {
				            _finish(RESULT_CANCELED, null);
				        }
				    })
				   .create();
        	} 
        	catch (Exception e) {
        		Log.e(TAG, "Dialogs", e);
			} 
        case DIALOG_LIST:
            return new AlertDialog.Builder(Dialogs.this)
                .setTitle(prompt)
                .setItems(values, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        _finish(RESULT_OK, values[which]);
                    }
                })
                .setOnCancelListener(new DialogInterface.OnCancelListener() {
					public void onCancel(DialogInterface dialog) {
                        _finish(RESULT_CANCELED, null);
					}
                })
                .create();
	    case DIALOG_TEXT_ENTRY:
	    case DIALOG_TEXT_INFO:
            LinearLayout linearLayout = linearLayout(this, 
            		LinearLayout.HORIZONTAL, 
            		LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT);
            TextView textView = id == DIALOG_TEXT_ENTRY ? 
            		new EditText(this) : 
            		new TextView(this);
            textView.setText(values[0]);
            textView.setMovementMethod(new ScrollingMovementMethod());
            linearLayout.addView(textView, marginLayoutParams(
            		LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT, 
            		margin,	halfMargin, margin, halfMargin));
    		switch (id) {
            case DIALOG_TEXT_ENTRY:
            	final EditText editText = (EditText) textView;
            	editText.setGravity(Gravity.CENTER);
            	editText.setSelection(0, editText.getText().length());
	            return new AlertDialog.Builder(this)
	    			.setCancelable(false)
	                .setIcon(R.drawable.ic_launcher)
	                .setTitle(prompt)
	                .setView(linearLayout)
	                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
	                    public void onClick(DialogInterface dialog, int whichButton) {
	                        _finish(RESULT_OK, editText.getText().toString());
	                    }
	                })
	                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
	                    public void onClick(DialogInterface dialog, int whichButton) {
	                    	_finish(RESULT_CANCELED, null);
	                    }
	                })
	                .create();
            case DIALOG_TEXT_INFO:
            	textView.setBackgroundColor(Color.WHITE);
            	textView.setTextColor(Color.BLACK);
	            return new AlertDialog.Builder(this)
	      			.setCancelable(false)
	                .setIcon(R.drawable.ic_launcher)
	                .setTitle(prompt)
	                .setView(linearLayout)
	                .setNeutralButton(R.string.button_close, new DialogInterface.OnClickListener() {
	                    public void onClick(DialogInterface dialog, int whichButton) {
	                    	_finish(RESULT_CANCELED, null);
	                    }
	                })
	                .create();
    		}
        }
        
        return null;
    }
	
    protected Bundle info = null;
    protected String prompt = "";
    protected String[] values = null;
    protected List<String> defaultValues = null;
    protected int margin, halfMargin, padding, halfpadding;

    @Override
	protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
    	margin = getResources().getDimensionPixelOffset(R.dimen.margin);
    	halfMargin = getResources().getDimensionPixelOffset(R.dimen.margin_half);
    	padding = getResources().getDimensionPixelOffset(R.dimen.padding);
    	halfpadding = getResources().getDimensionPixelOffset(R.dimen.padding_half);
    	
        setResult(RESULT_FIRST_USER);
        
        info = getIntent().getExtras();
        if (info != null) {
        	if (info.containsKey(BaseDirective.PROMPT))
        		prompt = info.getString(BaseDirective.PROMPT);
        	
        	if (info.containsKey(BaseDirective.VARIABLE))
        		var = info.getString(BaseDirective.VARIABLE);
        	
        	String[] array;
        	if (userContext != null) 
        		array = arrayOfStrings(userContext.get(var));
        	else
        		array = info.getStringArray(BaseDirective.DEFAULTS);
        	if (array != null)
        		defaultValues = asList(array);
        	
        	if (info.containsKey(BaseDirective.VALUES))
        		values = info.getStringArray(BaseDirective.VALUES);
        	
        	if (info.containsKey(BaseDirective.TYPE))
        		showDialog(info.getInt(BaseDirective.TYPE));
        }
    }
    
    protected String var = null;
    protected UserContext userContext = BaseDirective.userContext;
    
	protected void _finish(int resultCode, Object value) {
        if (userContext != null) {
        	userContext.put(var, value);
        	setResult(resultCode);
        }
        else
        	setResult(resultCode, getIntent().putExtra(BaseDirective.RESULT, String.valueOf(value)));
    	if (mDialog != null)
    		mDialog.dismiss();
    	BaseDirective._notify();
    	finish();
    }

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
	    if ((keyCode == KeyEvent.KEYCODE_BACK)) {
	    	_finish(RESULT_CANCELED, null);
	    }
	    return super.onKeyDown(keyCode, event);
	}
}

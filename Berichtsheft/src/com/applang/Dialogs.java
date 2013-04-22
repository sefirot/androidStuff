package com.applang;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.database.Cursor;

import static com.applang.Util.*;
import static com.applang.VelocityUtil.*;

import com.applang.berichtsheft.R;

public class Dialogs extends Activity
{
	public static final String PROMPT_ACTION = "com.applang.action.PROMPT";
	
	public static final int DIALOG_YES_NO_MESSAGE = 1;
    public static final int DIALOG_YES_NO_LONG_MESSAGE = 2;
    public static final int DIALOG_LIST = 3;
    public static final int DIALOG_PROGRESS = 4;
    public static final int DIALOG_SINGLE_CHOICE = 5;
    public static final int DIALOG_MULTIPLE_CHOICE = 6;
    public static final int DIALOG_TEXT_ENTRY = 7;
    public static final int DIALOG_SINGLE_CHOICE_CURSOR = 8;
    public static final int DIALOG_MULTIPLE_CHOICE_CURSOR = 9;

    public static final int MAX_PROGRESS = 100;
    public static ProgressDialog sProgressDialog;
    public static int sProgress;
    public static Handler sProgressHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (sProgress >= MAX_PROGRESS) {
                sProgressDialog.dismiss();
            } else {
                sProgress++;
                sProgressDialog.incrementProgressBy(1);
                sProgressHandler.sendEmptyMessageDelayed(0, 100);
            }
        }
    };

	private int checkedItem = -1;
	private List<Boolean> checkedItems = new ArrayList<Boolean>();
	private Uri uri;
	private Cursor cursor;
	private String selection, sortOrder;
	private String[] selectionArgs;

	private void query() {
		uri = Uri.parse(info.getString("uri"));
        sortOrder = info.getString("sortOrder");
        selection = info.getString("selection");
		selectionArgs = info.getStringArray("selectionArgs");
		cursor = managedQuery(uri, values, 
				selection, 
				selectionArgs, 
				sortOrder);
	}

    @Override
    protected Dialog onCreateDialog(int id) {
		switch (id) {
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
                .setNeutralButton(R.string.button_cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        _finish(RESULT_CANCELED, null);
                    }
                })
                .create();
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
        case DIALOG_PROGRESS:
            sProgressDialog = new ProgressDialog(Dialogs.this);
            sProgressDialog.setIcon(R.drawable.ic_launcher);
            sProgressDialog.setTitle(prompt);
            sProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            sProgressDialog.setMax(MAX_PROGRESS);
            sProgressDialog.setButton(getText(R.string.button_hide), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                	sProgressDialog.hide();
                }
            });
            sProgressDialog.setButton2(getText(R.string.button_cancel), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                	sProgressDialog.cancel();
                }
            });
            return sProgressDialog;
        case DIALOG_SINGLE_CHOICE:
        	if (defaultValues.size() > 0) {
        		checkedItem = Arrays.asList(values).indexOf(defaultValues.get(0));
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
                .setPositiveButton(R.string.button_ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        _finish(RESULT_OK, values[checkedItem]);
                    }
                })
                .setNegativeButton(R.string.button_cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        _finish(RESULT_CANCELED, null);
                    }
                })
               .create();
        case DIALOG_MULTIPLE_CHOICE:
            for (int i = 0; i < values.length; i++) {
            	checkedItems.add(defaultValues.contains(values[i]));
			}
			return new AlertDialog.Builder(Dialogs.this)
    			.setCancelable(false)
                .setIcon(R.drawable.ic_launcher)
                .setTitle(prompt)
                .setMultiChoiceItems(values,
                		Util.toPrimitiveArray(checkedItems),
                        new DialogInterface.OnMultiChoiceClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton, boolean isChecked) {
                            	checkedItems.set(whichButton, isChecked);
                            }
                        })
                .setPositiveButton(R.string.button_ok, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						ValList list = new ValList();
                    	Boolean[] checked = checkedItems.toArray(new Boolean[values.length]);
						for (int i = 0; i < checked.length; i++) {
							if (checked[i])
								list.add(values[i]);
						}
                        _finish(RESULT_OK, list);
                    }
                })
                .setNegativeButton(R.string.button_cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        _finish(RESULT_CANCELED, null);
                    }
                })
               .create();
        case DIALOG_SINGLE_CHOICE_CURSOR:
        	query();
        	if (defaultValues.size() > 0) {
        		if (cursor.moveToFirst())
        			do {
        				checkedItem++;
        				if (defaultValues.containsAll(Util2.listOfStrings(cursor)))
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
                .setPositiveButton(R.string.button_ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                    	int i;
		        		for (i = 0, cursor.moveToFirst(); i < checkedItem; i++, cursor.moveToNext())
		        			;
                        Object value;
                        if (values.length > 1) 
    						value = Util2.listOfStrings(cursor);
                        else
                        	value = cursor.getString(0);
						_finish(RESULT_OK, value);
                    }
                })
                .setNegativeButton(R.string.button_cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        _finish(RESULT_CANCELED, null);
                    }
                })
               .create();
        case DIALOG_MULTIPLE_CHOICE_CURSOR:
        	query();
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
                .setPositiveButton(R.string.button_ok, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						int i;
		        		for (i = 0, cursor.moveToFirst(); i < checkedItems.size(); i++, cursor.moveToNext())
                    	{
                        	ContentValues contentValues = new ContentValues();
                        	contentValues.put(values[2], checkedItems.get(i));
                        	getContentResolver().update(uri, contentValues, 
                        			values[0] + "=? and (" + selection + ")", 
                        			arrayappend(new String[]{"" + cursor.getLong(0)}, selectionArgs));
						}
                        _finish(RESULT_OK, null);
                    }
                })
                .setNegativeButton(R.string.button_cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        _finish(RESULT_CANCELED, null);
                    }
                })
               .create();
        case DIALOG_TEXT_ENTRY:
            LayoutInflater factory = LayoutInflater.from(this);
            View textEntryView = factory.inflate(R.layout.dialog_text_entry, null);
            final EditText et = (EditText) textEntryView.findViewById(R.id.text_value);
            et.setText(values[0]);
            et.setSelection(0, et.getText().length());
            et.setGravity(Gravity.CENTER);
            return new AlertDialog.Builder(Dialogs.this)
    			.setCancelable(false)
                .setIcon(R.drawable.ic_launcher)
                .setTitle(prompt)
                .setView(textEntryView)
                .setPositiveButton(R.string.button_ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        _finish(RESULT_OK, et.getText().toString());
                    }
                })
                .setNegativeButton(R.string.button_cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                    	_finish(RESULT_CANCELED, null);
                    }
                })
                .create();
        }
        
        return null;
    }
    
    Bundle info = null;
    String prompt = "";
    String[] values = null;
    List<String> defaultValues = null;

    @Override
	protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setResult(RESULT_FIRST_USER);
        
        info = getIntent().getExtras();
        if (info != null) {
        	if (info.containsKey("prompt"))
        		prompt = info.getString("prompt");
        	
        	if (info.containsKey("var"))
        		var = info.getString("var");
        	
        	if (userContext != null) 
        		defaultValues = Arrays.asList(arrayOfStrings(userContext.get(var)));
        	
        	if (info.containsKey("values"))
        		values = info.getStringArray("values");
        	
        	if (info.containsKey("type"))
        		showDialog(info.getInt("type"));
        }
    }
    
    String var = null;
    VelocityContext userContext = PromptDirective.userContext;
    
	void _finish(int resultCode, Object value) {
        if (userContext != null) {
        	userContext.put(var, value);
        }
    	setResult(resultCode);
    	PromptDirective._notify();
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

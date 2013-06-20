package com.applang;

import static com.applang.Util.*;
import static com.applang.Util2.*;
import static com.applang.SwingUtil.*;
import static com.applang.VelocityUtil.*;

import java.awt.Component;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.DefaultListModel;
import javax.swing.JDialog;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import android.app.Activity;
import android.app.Dialog;
import android.os.Bundle;
import android.util.Log;
import android.database.Cursor;
import android.net.Uri;
import android.content.ContentValues;
import android.view.KeyEvent;

public class _Dialogs extends Activity
{
	protected static final String TAG = _Dialogs.class.getSimpleName();
	
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

	private int checkedItem = -1;
	private List<Boolean> checkedItems = new ArrayList<Boolean>();
	CursorProvider provider = new CursorProvider(this);
	private Cursor cursor;
	
	class CursorProvider
	{
		Activity activity;
		
		public CursorProvider(Activity activity) {
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
        			arrayappend(new String[]{"" + cursor.getLong(0)}, selectionArgs));
		}
	}

	@Override
    protected Dialog onCreateDialog(int id) {
		switch (id) {
	    case DIALOG_TEXT_ENTRY:
			result = showInputDialog(null, prompt, title, 
					JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE,
		            null,
		            null, values[0]);
			_finish(result != null ? RESULT_OK : RESULT_CANCELED, result);
			break;
	    case DIALOG_TEXT_INFO:
			result = showInputDialog(null, prompt, prompt, 
	    			3, JOptionPane.PLAIN_MESSAGE,
	                null,
	                null, values[0]);
	    	_finish(RESULT_OK, result);
	    	break;
        case DIALOG_LIST:
        	SwingUtil.showDialog(null, null, prompt, 
            		new ComponentFunction<Component[]>() {
            			@SuppressWarnings({ "rawtypes", "unchecked" })
						public Component[] apply(Component comp, Object[] parms) {
							final JDialog dlg = (JDialog)comp;
							final JList list = new JList(new DefaultListModel() { 
								{
									for (Object value : values) 
										addElement(value);
								}
							});
							list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
							list.addListSelectionListener(new ListSelectionListener() {
								public void valueChanged(ListSelectionEvent e) {
									result = list.getSelectedValue();
									list.setSelectedIndex(list.getSelectedIndex());
									new Task<Void>(null, new Job<Void>() {
										public void perform(Void t,	Object[] params) throws Exception {
											dlg.setVisible(false);
										}
									}, 500).execute();
								}
							});
							dlg.getContentPane().add(new JScrollPane(list));
							return null;
						}
            		},
            		null,
            		null,
            		Modality.MODAL);
			_finish(result != null ? RESULT_OK : RESULT_CANCELED, result);
	    	break;
        case DIALOG_YES_NO_MESSAGE:
			result = showOptionDialog(null, prompt, title, 
					JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE,
	                null,
	                null, null);
	    	_finish(RESULT_OK, result);
	    	break;
        case DIALOG_YES_NO_LONG_MESSAGE:
			result = showOptionDialog(null, prompt, title, 
					JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE,
		            null,
		            values, values[0]);
			_finish(result != null ? RESULT_OK : RESULT_CANCELED, result);
	    	break;
	    case DIALOG_SINGLE_CHOICE:
			result = showInputDialog(null, prompt, title, 
					JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE,
		            null,
		            values, isAvailable(0, defaultValues) ? defaultValues.get(0) : null);
			_finish(result != null ? RESULT_OK : RESULT_CANCELED, result);
	    	break;
	    case DIALOG_MULTIPLE_CHOICE:
			@SuppressWarnings({ "rawtypes", "unchecked" })
	    	JList list = new JList(values);
			JOptionPane.showMessageDialog(null, list, prompt, 
					JOptionPane.PLAIN_MESSAGE);
			result = Arrays.toString(list.getSelectedIndices());
			_finish(result != null ? RESULT_OK : RESULT_CANCELED, result);
	    	break;
	    case DIALOG_SINGLE_CHOICE_CURSOR:
        	try {
				cursor = provider.query(info);
				if (defaultValues.size() > 0) {
					if (cursor.moveToFirst())
						do {
							checkedItem++;
							if (defaultValues.containsAll(getRecord(cursor)))
								break;
						} while (cursor.moveToNext());
				}
        	} 
        	catch (Exception e) {
        		Log.e(TAG, "Dialogs", e);
			} 
	    	break;
	    case DIALOG_MULTIPLE_CHOICE_CURSOR:
        	try {
				cursor = provider.query(info);
				if (cursor.moveToFirst())
					do {
						checkedItems.add(cursor.getInt(2) != 0);
					} while (cursor.moveToNext());
        	} 
        	catch (Exception e) {
        		Log.e(TAG, "Dialogs", e);
			} 
        	break;
		}
        
        return null;
    }
	
	private Object result = null;

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
	    if ((keyCode == KeyEvent.KEYCODE_BACK)) {
	    	_finish(RESULT_CANCELED, null);
	    	return true;
	    }
	    return super.onKeyDown(keyCode, event);
	}
	
    protected Bundle info = null;
    protected String prompt = "", title = "";
    protected String[] values = null;
    protected List<String> defaultValues = null;

    @Override
	protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    	
        setResult(RESULT_FIRST_USER);
        
        info = getIntent().getExtras();
        if (info != null) {
        	if (info.containsKey(BaseDirective.TITLE))
        		title = info.getString(BaseDirective.TITLE);
        	
        	if (info.containsKey(BaseDirective.PROMPT))
        		prompt = info.getString(BaseDirective.PROMPT);
        	
        	if (info.containsKey(BaseDirective.VARIABLE))
        		var = info.getString(BaseDirective.VARIABLE);
        	
        	if (userContext != null) 
        		defaultValues = Arrays.asList(arrayOfStrings(userContext.get(var)));
        	
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
        }
    	setResult(resultCode, getIntent().putExtra(BaseDirective.RESULT, String.valueOf(value)));
    	BaseDirective._notify();
    	finish();
    }
	
	public void create() {
		onCreate(null);
	}
}

class ConstructDialogs extends _Dialogs
{
	public static final String CONSTRUCT_ACTION = "com.applang.action.CONSTRUCT";
	public static final int DIALOG_CONSTRUCT = 11;
}

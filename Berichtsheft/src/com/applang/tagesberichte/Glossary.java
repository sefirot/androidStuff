package com.applang.tagesberichte;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import android.app.Activity;
import android.app.ExpandableListActivity;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.BaseExpandableListAdapter;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageButton;
import android.widget.ExpandableListView.OnChildClickListener;
import android.widget.ExpandableListView.OnGroupClickListener;
import android.widget.TextView;

import static com.applang.Util.*;
import static com.applang.Util2.*;

import com.applang.Util;
import com.applang.Util.Job;
import com.applang.berichtsheft.R;
import com.applang.provider.NotePadProvider;
import com.applang.provider.NotePad.NoteColumns;

public class Glossary extends ExpandableListActivity
{
	private static final String TAG = Glossary.class.getSimpleName();

	public static void setThreaded(Context context, boolean value) {
        SharedPreferences prefs = context.getSharedPreferences("prefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor prefsEditor = prefs.edit();
        prefsEditor.putBoolean("threaded", value);
        prefsEditor.commit();
	}

	public static boolean getThreaded(Context context, boolean defaultValue) {
        SharedPreferences prefs = context.getSharedPreferences("prefs", Context.MODE_PRIVATE);
        return prefs.getBoolean("threaded", defaultValue);
	}

	private static final int MENU_ITEM_VIEW = Menu.FIRST;
	private static final int MENU_ITEM_REFRESH = Menu.FIRST + 1;
	private static final int MENU_ITEM_SEARCH = Menu.FIRST + 2;

	public ExpandableListView listView;
	public GlossaryListAdapter adapter;
	public ContentResolver contentResolver;

    private static final String[] PROJECTION = new String[] {
    	NoteColumns._ID, // 0
    	NoteColumns.TITLE, // 1
    	NoteColumns.CREATED_DATE, 
    };
	
    private Uri mUri;
	private int tableIndex;
    
	@Override
	public void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);
    	setContentView(R.layout.glossary);
		
        final Intent intent = getIntent();
        mUri = intent.getData();
        tableIndex = NotePadProvider.tableIndex(2, mUri);
        if (mUri == null) 
        	mUri = NotePadProvider.contentUri(tableIndex);

    	adapter = new GlossaryListAdapter(this, new GlossaryTree());

        Helper.listViewSearch(true, this, adapter.getFilter());
       	
        listView = getExpandableListView();
		
		listView.setOnChildClickListener(new OnChildClickListener() {
			@Override
			public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
//				Toast.makeText(getBaseContext(), "Child clicked", Toast.LENGTH_LONG).show();
				return false;
			}
    	});
    
    	listView.setOnGroupClickListener(new OnGroupClickListener() {
			@Override
			public boolean onGroupClick(ExpandableListView parent, View v, int groupPosition, long id) {
//				Toast.makeText(getBaseContext(), "Group clicked", Toast.LENGTH_LONG).show();
				return false;
			}
    	});
    	
    	listView.setOnItemLongClickListener(new OnItemLongClickListener() {
    	    @Override
    	    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
    	        if (ExpandableListView.getPackedPositionType(id) == ExpandableListView.PACKED_POSITION_TYPE_CHILD) {
    	            int groupPosition = ExpandableListView.getPackedPositionGroup(id);
    	            int childPosition = ExpandableListView.getPackedPositionChild(id);
    	            clickInfo = adapter.getChild(groupPosition, childPosition);
    	            openContextMenu(listView);
    	            return true;
    	        }
    	        if (ExpandableListView.getPackedPositionType(id) == ExpandableListView.PACKED_POSITION_TYPE_GROUP) {
    	            int groupPosition = ExpandableListView.getPackedPositionGroup(id);
    	            clickInfo = adapter.getGroup(groupPosition);
    	            openContextMenu(listView);
    	            return true;
    	        }

    	        return false;
    	    }
    	});

    	listView.setAdapter(adapter);
    	registerForContextMenu(listView);
    	
    	contentResolver = getContentResolver();
    	setContentObserver();
    	
    	needsRefresh = true;
	}

	private void setContentObserver() {
		super.onStart();
		ContentObserver contentObserver = new ContentObserver(notifyHandler) {
			public void onChange(boolean selfChange) {
				if (isRunning)
					repopulate();
				else
					needsRefresh = true;
			}
		};
		Uri notificationUri = NotePadProvider.contentUri(NotePadProvider.tableName(tableIndex));
		if (contentObserver != null)
			contentResolver.registerContentObserver(notificationUri, true, contentObserver);
	}
	
	ContentObserver contentObserver = null;
	
	private void resetContentObserver() {
		if (contentObserver != null)
			contentResolver.unregisterContentObserver(contentObserver);
	}

	@Override
	protected void onDestroy() {
		resetContentObserver();
		super.onStop();
	}

	public void populate(boolean clear) {
		if (clear)
			adapter.clear();
		
		Cursor cursor = null;
        try {
			cursor = contentResolver.query(NotePadProvider.contentUri(tableIndex), 
				PROJECTION, 
				"", null,
				NoteColumns.TITLE_SORT_ORDER);

			if (cursor.moveToFirst()) 
				do {
					adapter.addItem(new GlossaryLeaf(Glossary.this, cursor.getString(1), cursor.getLong(2)));
				} while (cursor.moveToNext());
			
			notifyHandler.sendEmptyMessage(1);
			Util.delay(50);
			Log.i(TAG, "Glossary populated");
		} 
        finally {
        	if (cursor != null)
        		cursor.close();
        }
	}

	private void repopulate() {
		boolean threaded = getThreaded(this, true);
		if (threaded) {
			WorkerThread worker = new WorkerThread(this, new Job<Activity>() {
				public void perform(Activity activity, Object[] params)	throws Exception {
					populate(true);
				}
			}, null);
			worker.start();
		}
		else
			populate(true);
	}
	
	private Handler notifyHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case 1:
				adapter.notifyDataSetInvalidated();
				break;

			case 2:
				adapter.notifyDataSetChanged();
				break;
			}
			super.handleMessage(msg);
		}
	};
	
	private Object clickInfo = null;
	private boolean needsRefresh = false;
	private boolean isRunning = false;

    @Override
    protected void onPause() {
        super.onPause();
        isRunning = false;
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        isRunning = true;
        
		if (needsRefresh) {
			repopulate();
        	needsRefresh = false;
		}
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        
        menu.add(0, MENU_ITEM_SEARCH, 0, R.string.menu_search)
		    	.setShortcut('5', 's')
		    	.setIcon(android.R.drawable.ic_menu_search);
        menu.add(0, MENU_ITEM_REFRESH, 0, R.string.menu_refresh)
        		.setShortcut('6', 'r');
        menu.add(0, MENU_ITEM_VIEW, 0, R.string.menu_view)
		    	.setShortcut('7', 'v')
		    	.setIcon(android.R.drawable.ic_menu_view);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case MENU_ITEM_VIEW:
        	startActivityForResult(
        			new Intent(GlossaryView.GLOSSARY_VIEW_ACTION, mUri), 0);
        	return true;
        	
        case MENU_ITEM_REFRESH:
        	repopulate();
        	return true;
        	
        case MENU_ITEM_SEARCH:
        	Helper.listViewSearch(false, this, null);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    @Override
    public void onCreateContextMenu(ContextMenu menu, View view, ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, view, menuInfo);
        if (clickInfo != null) {
        	menu.setHeaderTitle(clickInfo.toString());
			getMenuInflater().inflate(R.menu.contextmenu_glossary, menu);
//			if (clickInfo instanceof String)
//				menu.removeItem(R.id.menu_item_evaluate);
		}
    }
    
    @Override
    public boolean onContextItemSelected(MenuItem item) {
        switch (item.getItemId()) {
/*		case R.id.menu_item_evaluate: 
			if (clickInfo instanceof GlossaryLeaf) {
				GlossaryLeaf glossaryLeaf = (GlossaryLeaf) clickInfo;
				Uri uri = ContentUris.withAppendedId(NotePadProvider.contentUri(0), glossaryLeaf.getRefId());
				startActivity(new Intent()
						.setClass(this, NoteEvaluator.class)
						.setData(uri));
			}
			return true;
*/            
        case R.id.menu_item_edit: 
			if (clickInfo instanceof GlossaryLeaf) {
				GlossaryLeaf glossaryLeaf = (GlossaryLeaf) clickInfo;
				long id = NotePadProvider.getIdOfNote(contentResolver, tableIndex, 
						NoteColumns.TITLE + "=? and " + NoteColumns.CREATED_DATE + "=?", 
						strings(glossaryLeaf.getGroup(), "" + glossaryLeaf.getRefId()));
				Uri uri = ContentUris.withAppendedId(mUri, id);
				startActivity(new Intent()
						.setClass(this, TitleEditor.class)
						.setData(uri)
						.putExtra("state", NoteEditor.STATE_EDIT));
			}
            else if (clickInfo instanceof String) {
				startActivity(new Intent()
						.setClass(this, TitleEditor.class)
						.setData(mUri)
						.putExtra("title", clickInfo.toString())
						.putExtra("state", NoteEditor.STATE_EDIT));
			}
			return true;
			
        case R.id.menu_item_remove: 
        	String message = getResources().getString(R.string.areUsure, clickInfo.toString());
            if (clickInfo instanceof GlossaryLeaf) {
        		areUsure(this, message, new Job<Void>() {
    				@Override
    				public void perform(Void t, Object[] params) throws Exception {
    					GlossaryLeaf glossaryLeaf = (GlossaryLeaf) clickInfo;
    					adapter.removeItem(glossaryLeaf);
    					contentResolver.delete(mUri,
    							NoteColumns.TITLE + "=? and " + NoteColumns.CREATED_DATE + "=?", 
    							new String[]{glossaryLeaf.getGroup(), "" + glossaryLeaf.getRefId()});
    				}
        		});
			}
            else if (clickInfo instanceof String) {
            	areUsure(this, message, new Job<Void>() {
    				@Override
    				public void perform(Void t, Object[] params) throws Exception {
    					String group = clickInfo.toString();
    					adapter.removeItem(new GlossaryLeaf(null, group, -1));
    					contentResolver.delete(mUri,
    							NoteColumns.TITLE + "=?", 
    							new String[]{group});
    				}
        		});
            }
            else
            	return false;
            
			return true;
        }
        
        return false;
	}
    
	public class GlossaryLeaf
	{
	    @Override
		public String toString() {
			return NotesList.description(0, epoch, title);
		}
	
	    public GlossaryLeaf(Activity activity, String group, long refId) {
	        this.refId = refId;
	        if (activity != null) {
				Cursor cursor = null;
				try {
					cursor = contentResolver.query(NotePadProvider.contentUri(0), 
							PROJECTION,
							NoteColumns._ID + "= ?",
							new String[] { "" + refId }, 
							NoteColumns.DEFAULT_SORT_ORDER);
					if (cursor.moveToFirst()) {
						this.title = cursor.getString(1);
						this.epoch = cursor.getLong(2);
					}
				} finally {
					if (cursor != null)
						cursor.close();
				}
			}
			this.group = group;
	    }
	    
	    private String group;
	    public String getGroup() {
	        return group;
	    }
	    
	    private String title = "";
	    public String getTitle() {
	        return title;
	    }
	    
	    private long epoch = 0;
	    public long getEpoch() {
	        return epoch;
	    }
	    
	    private long refId;
	    public long getRefId() {
	        return refId;
	    }
	}
	
	class GlossaryBranch extends ArrayList<GlossaryLeaf>
	{
	}
	
	class GlossaryTree extends LinkedHashMap<String, GlossaryBranch>
	{
	}
	
	public class GlossaryListAdapter extends BaseExpandableListAdapter implements Filterable
	{
		private Activity activity;
		private GlossaryTree tree, origTree = null;
		
		public GlossaryListAdapter(Activity activity, GlossaryTree tree) {
			this.activity = activity;
	    	this.tree = tree;
		}
	
		Filter glossaryFilter = new Filter() {
			@Override
			protected FilterResults performFiltering(CharSequence constraint) {
				if (origTree == null) 
					origTree = tree;
			    if (notNullOrEmpty(constraint)) {
			    	tree = new GlossaryTree();
			    	String crit = constraint.toString().toLowerCase(Locale.getDefault());
			    	for (Map.Entry<String, GlossaryBranch> entry : origTree.entrySet()) {
			    		String s = entry.getKey();
			    		if (s.toLowerCase(Locale.getDefault()).startsWith(crit)) 
			    			tree.put(s, entry.getValue());
			    	}
			    }
			    else if (origTree != null) 
			    	tree = origTree;
			    FilterResults results = new FilterResults();
			    results.values = tree;
			    results.count = tree.size();
			    return results;
			}
			@Override
			protected void publishResults(CharSequence constraint, FilterResults results) {
				if (results.count < 1)
					notifyDataSetInvalidated();
				else 
					notifyDataSetChanged();
			}
		};
		
		@Override
		public Filter getFilter() {
			return glossaryFilter;
		}
		
		@Override
		public boolean areAllItemsEnabled() {
			return true;
		}
	
		public void addItem(GlossaryLeaf glossaryLeaf) {
			String group = glossaryLeaf.getGroup();
			GlossaryBranch branch;
			if (tree.keySet().contains(group))
				branch = tree.get(group);
			else {
				branch = new GlossaryBranch();
				tree.put(group, branch);
			}
			branch.add(glossaryLeaf);
		}
	
		public void removeItem(GlossaryLeaf glossaryLeaf) {
			String group = glossaryLeaf.getGroup();
			if (tree.keySet().contains(group)) {
				GlossaryBranch branch = tree.get(group);
				if (branch.contains(glossaryLeaf))
					branch.remove(glossaryLeaf);
				if (branch.size() < 1)
					tree.remove(group);
			}
		}
	
		public void clear() {
			tree.clear();
		}
		
		GlossaryBranch getBranch(int groupPosition) {
			return tree.get(getGroup(groupPosition));
		}
	
		@Override
		public Object getChild(int groupPosition, int childPosition) {
			GlossaryBranch branch = getBranch(groupPosition);
			return branch == null ? null : branch.get(childPosition);
		}
	
		@Override
		public long getChildId(int groupPosition, int childPosition) {
			return childPosition;
		}
	
		@Override
		public int getChildrenCount(int groupPosition) {
			GlossaryBranch branch = getBranch(groupPosition);
			return branch == null ? 0 : branch.size();
		}
	
		@Override
		public Object getGroup(int groupPosition) {
			String[] groups = tree.keySet().toArray(new String[0]);
			if (groupPosition > -1 && groupPosition < groups.length) 
				return groups[groupPosition];
			else 
				return null;
		}
	
		@Override
		public int getGroupCount() {
			return tree.size();
		}
	
		@Override
		public long getGroupId(int groupPosition) {
			return groupPosition;
		}
	
		@Override
		public boolean hasStableIds() {
	    	return true;
		}
	
		@Override
		public boolean isChildSelectable(int arg0, int arg1) {
	    	return true;
		}
	
		@Override
		public View getGroupView(int groupPosition, boolean isExpanded,	View convertView, ViewGroup parent) {
	    	if (convertView == null) {
	    		LayoutInflater inflater = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	    		convertView = inflater.inflate(R.layout.noteslist_item2, null);
	    	}
	    	
	    	TextView tv = (TextView) convertView.findViewById(R.id.title);
	    	String group = (String) getGroup(groupPosition);
	    	tv.setText(group);
	    	
	    	return convertView;
		}
	
		@Override
		public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
			if (convertView == null) {
				LayoutInflater inflater = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				convertView = inflater.inflate(R.layout.noteslist_item, null);
	    	}
			
			GlossaryLeaf leaf = (GlossaryLeaf) getChild(groupPosition, childPosition);
	    	if (leaf != null) {
				TextView tv = (TextView) convertView.findViewById(R.id.date);
				tv.setText(NotesList.formatDate(leaf.getEpoch()));
				tv = (TextView) convertView.findViewById(R.id.title);
				tv.setText(leaf.getTitle());
			}
			
	    	
	//		android.graphics.drawable.Drawable icon = context.getResources().getDrawable( R.drawable.note );
	//		tv.setCompoundDrawablesWithIntrinsicBounds(icon, null, null, null);
	    	
			return convertView;
		}
	}
}
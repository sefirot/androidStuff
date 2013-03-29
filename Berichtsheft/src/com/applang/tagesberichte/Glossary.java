package com.applang.tagesberichte;

import java.util.ArrayList;

import android.app.Activity;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.OnChildClickListener;
import android.widget.ExpandableListView.OnGroupClickListener;
import android.widget.TextView;

import static com.applang.Util.*;
import static com.applang.Util2.*;

import com.applang.berichtsheft.R;
import com.applang.provider.NotePadProvider;
import com.applang.provider.NotePad.Notes;

public class Glossary extends Activity
{
    private static final String TAG = Glossary.class.getSimpleName();

	private static final int MENU_ITEM_VIEW = Menu.FIRST;

    private ExpandableListView listView;
	private GlossaryListAdapter adapter;

    private Uri mUri;
	private int table = 0;
    
	@Override
	public void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);
		setContentView(R.layout.glossary);
		
        final Intent intent = getIntent();
        mUri = intent.getData();
        Bundle extras = intent.getExtras();
        if (extras != null && extras.containsKey("table")) {
        	table = extras.getInt("table", 0);
        }

		listView = (ExpandableListView) findViewById(R.id.expandableListView);
        
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

    	adapter = new GlossaryListAdapter(this, new ArrayList<String>(), new ArrayList<ArrayList<Vehicle>>());

    	listView.setAdapter(adapter);
    	registerForContextMenu(listView);
    	
    	populate();
	}

	private void populate() {
		WorkerThread worker = new WorkerThread(
    		this,
        	new Job<Activity>() {
				public void dispatch(Activity activity, Object[] params) throws Exception {
					adapter.clear();
					
					Cursor cursor = null;
			        try {
						cursor = managedQuery(Notes.CONTENT_URI, 
							Notes.FULL_PROJECTION, 
							NotePadProvider.selection(table, ""), null,
					        Notes.DEFAULT_SORT_ORDER);

						if (cursor.moveToFirst()) 
							do {
								adapter.addItem(new Vehicle(Glossary.this, cursor.getString(1), cursor.getLong(3)));
							   	handler.sendEmptyMessage(1);
							   	delay(50);
							} while (cursor.moveToNext());
					} 
			        finally {
			        	if (cursor != null)
			        		cursor.close();
			        }
				}
	        },
	        null 
    	);
    	worker.start();
	}
	
	Handler handler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			adapter.notifyDataSetInvalidated();
			super.handleMessage(msg);
		}
	};
	
	private Object clickInfo = null;

    @Override
    protected void onResume() {
        super.onResume();
        if (NotePadProvider.isTableDirty(this, table)) {
        	NotePadProvider.setTableState(this, table, false);
        	populate();
        }
    	NotePadProvider.saveTableIndex(this, table);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        
        menu.add(0, MENU_ITEM_VIEW, 0, R.string.menu_view)
        	.setShortcut('5', 'v')
        	.setIcon(android.R.drawable.ic_menu_view);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case MENU_ITEM_VIEW:
//        	getContentResolver().cancelSync(mUri);
			startActivityForResult(new Intent(GlossaryView.VIEW_GLOSSARY_ACTION, mUri)
					.putExtra("table", table), 0);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    }
    
    @Override
    public void onCreateContextMenu(ContextMenu menu, View view, ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, view, menuInfo);
        if (clickInfo != null) {
        	menu.setHeaderTitle(clickInfo.toString());
			getMenuInflater().inflate(R.menu.contextmenu_glossary, menu);
			if (clickInfo instanceof String)
				menu.removeItem(R.id.menu_item_evaluate);
		}
    }
    
    @Override
    public boolean onContextItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.menu_item_evaluate: 
			if (clickInfo instanceof Vehicle) {
				Vehicle vehicle = (Vehicle) clickInfo;
				Uri uri = ContentUris.withAppendedId(mUri, vehicle.getRefId());
				startActivity(new Intent()
						.setClass(Glossary.this, NoteEvaluator.class)
						.setData(uri)
						.putExtra("table", 0));
			}
			return true;
			
        case R.id.menu_item_remove: 
            if (clickInfo instanceof Vehicle) {
				Vehicle vehicle = (Vehicle) clickInfo;
				adapter.removeItem(vehicle);
				Uri noteUri = ContentUris.withAppendedId(mUri, vehicle.getRefId());
				getContentResolver().delete(noteUri,
						NotePadProvider.selection(table, ""), 
						null);
			}
            else if (clickInfo instanceof String) {
				String group = clickInfo.toString();
				adapter.removeItem(new Vehicle(null, group, -1));
				getContentResolver().delete(mUri,
						NotePadProvider.selection(table, Notes.TITLE + "=?"), 
						new String[]{group});
            }
            else
            	return false;
            
            handler.sendEmptyMessage(1);
			return true;
        }
        
        return false;
	}
}

class Vehicle
{
    @Override
	public String toString() {
		return NotesList.description(0, epoch, title);
	}

	private static final String[] PROJECTION = new String[] {
        Notes._ID, // 0
        Notes.TITLE, // 1
        Notes.CREATED_DATE, 
    };

    public Vehicle(Activity activity, String group, long refId) {
        this.refId = refId;
        if (activity != null) {
			Cursor cursor = null;
			try {
				cursor = activity.managedQuery(Notes.CONTENT_URI, PROJECTION,
						NotePadProvider.selection(0, Notes._ID + "= ?"),
						new String[] { "" + refId }, Notes.DEFAULT_SORT_ORDER);
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

class GlossaryListAdapter extends BaseExpandableListAdapter 
{
	@Override
	public boolean areAllItemsEnabled() {
		return true;
	}

	private Activity activity;
	private ArrayList<String> groups;
	private ArrayList<ArrayList<Vehicle>> childItems;
	
	public GlossaryListAdapter(Activity activity, ArrayList<String> groups, ArrayList<ArrayList<Vehicle>> children) {
		this.activity = activity;
    	this.groups = groups;
    	this.childItems = children;
	}

	public void addItem(Vehicle vehicle) {
		if (!groups.contains(vehicle.getGroup())) {
			groups.add(vehicle.getGroup());
		}
		int index = groups.indexOf(vehicle.getGroup());
		if (childItems.size() < index + 1) {
			childItems.add(new ArrayList<Vehicle>());
		}
		childItems.get(index).add(vehicle);
	}

	public void removeItem(Vehicle vehicle) {
		int index = groups.indexOf(vehicle.getGroup());
		if (index > -1 && index < childItems.size()) {
			ArrayList<Vehicle> list = childItems.get(index);
			boolean childAvailable = list.contains(vehicle);
			if (childAvailable)
				list.remove(vehicle);
			if (!childAvailable || list.size() < 1) {
				childItems.remove(index);
				groups.remove(vehicle.getGroup());
			}
		}
	}

	public void clear() {
		groups.clear();
		childItems.clear();
	}

	@Override
	public Object getChild(int groupPosition, int childPosition) {
		return childItems.get(groupPosition).get(childPosition);
	}

	@Override
	public long getChildId(int groupPosition, int childPosition) {
		return childPosition;
	}

	@Override
	public int getChildrenCount(int groupPosition) {
		return childItems.get(groupPosition).size();
	}

	@Override
	public Object getGroup(int groupPosition) {
		return groups.get(groupPosition);
	}

	@Override
	public int getGroupCount() {
		return groups.size();
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
		
		Vehicle vehicle = (Vehicle) getChild(groupPosition, childPosition);
    	TextView tv = (TextView) convertView.findViewById(R.id.date);
    	tv.setText(NotesList.formatDate(vehicle.getEpoch()));
    	tv = (TextView) convertView.findViewById(R.id.title);
    	tv.setText(vehicle.getTitle());
    	
//		android.graphics.drawable.Drawable icon = context.getResources().getDrawable( R.drawable.note );
//		tv.setCompoundDrawablesWithIntrinsicBounds(icon, null, null, null);
    	
		return convertView;
	}
}
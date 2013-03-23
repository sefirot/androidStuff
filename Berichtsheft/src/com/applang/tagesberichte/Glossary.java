package com.applang.tagesberichte;

import java.util.ArrayList;
import java.util.Locale;

import android.app.Activity;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.OnChildClickListener;
import android.widget.ExpandableListView.OnGroupClickListener;
import android.widget.TextView;
import android.widget.Toast;

import static com.applang.Util.formatDate;
import static com.applang.Util2.*;
import com.applang.berichtsheft.R;
import com.applang.provider.NotePadProvider;
import com.applang.provider.NotePad.Notes;

public class Glossary extends Activity implements Runnable
{
    private static final String TAG = Glossary.class.getSimpleName();

	private GlossaryListAdapter adapter;

	private int table = 0;
    
	@Override
	public void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);
		setContentView(R.layout.glossary);
		
        final Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        if (extras != null && extras.containsKey("table")) {
        	table = extras.getInt("table", 0);
        }

		ExpandableListView listView = (ExpandableListView) findViewById(R.id.expandableListView);
        
		listView.setOnChildClickListener(new OnChildClickListener() {
			@Override
			public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
				Vehicle vehicle = (Vehicle) adapter.getChild(groupPosition, childPosition);
/*        		Toast.makeText(getBaseContext(), 
        				String.format("Group %s RefId %d", vehicle.getGroup(), vehicle.getRefId()), 
        				Toast.LENGTH_LONG).show();
*/
				Uri uri = ContentUris.withAppendedId(intent.getData(), vehicle.getRefId());
                startActivity(new Intent()
                	.setClass(Glossary.this, NoteEvaluator.class)
                	.setData(uri));
                
				return true;
			}
    	});
    
    	listView.setOnGroupClickListener(new OnGroupClickListener() {
			@Override
			public boolean onGroupClick(ExpandableListView parent, View v, int groupPosition, long id) {
//				Toast.makeText(getBaseContext(), "Group clicked", Toast.LENGTH_LONG).show();
				return false;
			}
    	});

    	adapter = new GlossaryListAdapter(this, new ArrayList<String>(), new ArrayList<ArrayList<Vehicle>>());

    	listView.setAdapter(adapter);
    	
    	Thread thread = new Thread(this);
    	thread.start();
	}

    @Override
    protected void onResume() {
        super.onResume();
    	NotePadProvider.saveTableIndex(this, table);
    }

	@Override
	public void run() {
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
        catch (Exception e) {
            Log.e(TAG, e.getMessage());
		}
        finally {
        	if (cursor != null)
        		cursor.close();
        }
	}

	private Handler handler = new Handler() {
		@Override
		public void handleMessage(Message msg)
		{
			adapter.notifyDataSetInvalidated();
			super.handleMessage(msg);
		}
	};
}

class Vehicle
{
    private static final String[] PROJECTION = new String[] {
        Notes._ID, // 0
        Notes.TITLE, // 1
        Notes.CREATED_DATE, 
    };

    public Vehicle(Activity activity, String group, long refId) {
        this.refId = refId;
        Cursor cursor = null;
        try {
        	cursor = activity.managedQuery(Notes.CONTENT_URI, 
        		PROJECTION, 
        		NotePadProvider.selection(0, Notes._ID + "= ?"), new String[] {"" + refId},
                Notes.DEFAULT_SORT_ORDER);
        	if (cursor.moveToFirst()) {
        		this.title = cursor.getString(1);
        		this.epoch = cursor.getLong(2);
        	}
        }
        finally {
        	if (cursor != null)
        		cursor.close();
        }
        this.group = group;
    }
    
    private String group;
    public String getGroup() {
        return group;
    }
    
    private String title;
    public String getTitle() {
        return title;
    }
    
    private long epoch;
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

	private Context context;
	private ArrayList<String> groups;
	private ArrayList<ArrayList<Vehicle>> children;
	
	public GlossaryListAdapter(Context context, ArrayList<String> groups, ArrayList<ArrayList<Vehicle>> children) {
		this.context = context;
    	this.groups = groups;
    	this.children = children;
	}

	public void addItem(Vehicle vehicle) {
		if (!groups.contains(vehicle.getGroup())) {
			groups.add(vehicle.getGroup());
		}
		int index = groups.indexOf(vehicle.getGroup());
		if (children.size() < index + 1) {
			children.add(new ArrayList<Vehicle>());
		}
		children.get(index).add(vehicle);
	}

	@Override
	public Object getChild(int groupPosition, int childPosition) {
		return children.get(groupPosition).get(childPosition);
	}

	@Override
	public long getChildId(int groupPosition, int childPosition) {
		return childPosition;
	}

	@Override
	public int getChildrenCount(int groupPosition) {
		return children.get(groupPosition).size();
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
    		LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
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
			LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
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
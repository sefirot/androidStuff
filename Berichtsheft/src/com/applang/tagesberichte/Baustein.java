package com.applang.tagesberichte;

import java.io.StringReader;
import java.util.Map;

import org.apache.velocity.runtime.parser.Token;
import org.apache.velocity.runtime.parser.node.Node;
import org.apache.velocity.runtime.parser.node.SimpleNode;

import com.applang.BaseDirective;
import com.applang.Dialogs;
import com.applang.UserContext;
import com.applang.Util.Function;
import com.applang.Util.Job;
import com.applang.Util.ValMap;
import com.applang.VelocityUtil.Visitor;
import com.applang.berichtsheft.R;

import static com.applang.Util.*;
import static com.applang.Util2.*;
import static com.applang.VelocityUtil.*;

import android.app.Activity;
import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

public class Baustein extends ListActivity
{
	private static final String TAG = Baustein.class.getSimpleName();

	private static final int MENU_ITEM_SEARCH = Menu.FIRST;

	private static SimpleNode document = null;
	
	public ListView listView;
	private BausteinListAdapter adapter;
	private EditText searchEdit;

	@Override
	public void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);
    	setContentView(R.layout.baustein);

        LayoutInflater inflater = LayoutInflater.from(this);
        View view = inflater.inflate(R.layout.search, null);
        ViewGroup contentView = (ViewGroup)getContentView(this);
		contentView.addView(view);
		
    	searchEdit = (EditText) findViewById(android.R.id.edit);
    	searchEdit.addTextChangedListener(new TextWatcher() {
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
//				Textbaustein.this.adapter.getFilter().filter(s);	
			}
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,	int after) {
			}
			@Override
			public void afterTextChanged(Editable s) {
			}
		});
    	findViewById(R.id.search).setVisibility(View.GONE);
        
		ImageButton btn = (ImageButton) findViewById(R.id.button1);
		if (btn != null)
			btn.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					searchEdit.setText("");
					findViewById(R.id.search).setVisibility(View.GONE);
				}
			});
   	
		listView = getListView();
		listView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
		listView.setOnCreateContextMenuListener(this);

    	adapter = new BausteinListAdapter();

    	listView.setAdapter(adapter);
//    	registerForContextMenu(listView);
	}
    
    @Override
    protected void onResume() {
        super.onResume();
		populate(true);
    }
	
	Job<Object[]> checkout = new Job<Object[]>() {
		public void perform(Object[] objects, Object[] params) throws Exception {
			int indents = (Integer) objects[0];
			Node node = (Node) objects[1];
			Token t = (Token) objects[2];
			adapter.addItem(node, indents, t);
		}
	};
	
	public void populate(boolean clear) {
		if (clear)
			adapter.clear();
		
		if (document != null) {
			Visitor.walk(document, new Function<Object>() {
				public Object apply(Object...params) {
					Node node = param(null, 0, params);
					Visitor.visitLostAndFound(checkout, null, 
							Visitor.beginLC(node));
					if (Visitor.isEssential.apply(node)) {
						int indents = paramInteger(0, 1, params);
						adapter.addItem(node, indents);
					}
					return null;
				}
			});
			Visitor.visitLostAndFound(checkout, null, 
					Integer.MAX_VALUE, 0);
		}
	}
	
	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		ValMap map = (ValMap) l.getItemAtPosition(position);
		Node node = (Node) map.get(NODE);
		Token t = (Token) map.get(TOKEN);
		int[] span = Visitor.span(t != null ? t : node);
		setResult(RESULT_OK, new Intent()
				.putExtra(NoteEditor.SPAN, span));
		finish();
    }

	private void modify(final Node node) {
		new UserContext.EvaluationTask(this, new ValMap(), null, null, new Job<Object>() {
			public void perform(Object node, Object[] params) {
				if (node != null) {
					populate(true);
				}
			}
		}).execute(new Function<Node>() {
			public Node apply(Object... params) {
				UserContext userContext = (UserContext) params[0];
				return userContext.modifyNode(node);
			}
		});
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        AdapterView.AdapterContextMenuInfo info;
        try {
             info = (AdapterView.AdapterContextMenuInfo) menuInfo;
        } catch (ClassCastException e) {
            Log.e(TAG, "bad menuInfo", e);
            return;
        }
        
        mMap = (ValMap) adapter.getItem(info.position);
        
		menu.clear();
		menu.setHeaderTitle(getString(R.string.particle));
		getMenuInflater().inflate(R.menu.contextmenu_baustein, menu);
		if (!isPossible(DELETE, mMap)) 
			menu.removeItem(R.id.menu_item_delete);
		menu.removeItem(R.id.menu_item_insert);
		if (isPossible(INSERT, mMap)) {
			SubMenu submenu = menu.addSubMenu(Menu.NONE, Menu.NONE, Menu.NONE, R.string.menu__insert);
			anweisungen = UserContext.directives(mMap);
			for (String key : anweisungen.keySet()) 
				submenu.add(Menu.NONE, Menu.FIRST, Menu.NONE, key);
		}
		if (!isPossible(MODIFY, mMap)) 
			menu.removeItem(R.id.menu_item_modify);
	}
	
	ValMap mMap = null;
	Map<String,String> anweisungen = null;

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_item_modify:
			Node node = (Node)mMap.get(NODE);
			modify(node);
			break;
		case R.id.menu_item_delete:
			break;
		case R.id.menu_item_insert:
			break;
		}
		return super.onContextItemSelected(item);
	}

	class BausteinListAdapter extends BaseAdapter
	{
		ValList list = new ValList();
	    
		public void clear() {
			list.clear();
		}
		
		public void addItem(Node node, Object...params) {
			ValMap map = nodeMap(node, 
					paramInteger(0, 0, params), 
					param(null, 1, params));
			list.add(map);
		}

		@Override
		public int getCount() {
			return list.size();
		}

		@Override
		public Object getItem(int position) {
			return list.get(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
	        if (convertView == null) {
	            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
	            convertView = inflater.inflate(R.layout.baustein_list_item, parent, false);
	        }
	 
	        TextView textView = (TextView) convertView.findViewById(R.id.textView1);
	        ValMap map = (ValMap) list.get(position);
	        Node node = (Node) map.get(NODE);
			Integer indents = (Integer)map.get(INDENTS);
			Integer blocks = node != null ? Visitor.blockDepth(node) : (Integer)map.get(BLOCKS);
			Token token = (Token) map.get(TOKEN);
			boolean endToken = hasEndToken(map);
			String text = indentedLine(
					endToken ? token.image : Visitor.nodeInfo(node), 
					indentor, 
					indents, 
					blocks);
	        textView.setText(text);
	        
	        textView = (TextView) convertView.findViewById(R.id.textView2);
	        text = Visitor.nodeCategory(endToken ? null : node);
	        textView.setText(text);
	        
			return convertView;
		}
	}

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        
        menu.add(0, MENU_ITEM_SEARCH, 0, R.string.menu_search)
		    	.setShortcut('5', 's')
		    	.setIcon(android.R.drawable.ic_menu_search);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case MENU_ITEM_SEARCH:
        	View view = findViewById(R.id.search);
        	if (view.getVisibility() == View.VISIBLE)
    			view.setVisibility(View.GONE);
        	else {
        		view.setVisibility(View.VISIBLE);
        		findViewById(android.R.id.edit).requestFocus();
        	}
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

	public static class AnalysisTask extends Task<Boolean>
	{
		public AnalysisTask(Activity activity, Job<Boolean> followUp, Object... params) {
			super(activity, followUp, params);
		}
	
		@Override
		protected Boolean doInBackground(Object... params) {
			boolean problem = false;
			String text = paramString(null, 0, params);
			String title = paramString("", 1, params);
			if (notNullOrEmpty(text)) {
				document = parse(new StringReader(text), title);
				problem = problem();
				if (title.length() > 0 && problem)
					publishProgress(
						new Intent(Dialogs.PROMPT_ACTION)
							.putExtra(BaseDirective.PROMPT, title)
							.putExtra(BaseDirective.VALUES, new String[]{getMessage()})
							.putExtra(BaseDirective.TYPE, Dialogs.DIALOG_TEXT_INFO));
			}
			return problem;
		}
	}
}

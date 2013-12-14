package com.applang.berichtsheft;

import static com.applang.Util.*;
import static com.applang.Util1.*;
import static com.applang.Util2.*;
import static com.applang.VelocityUtil.*;

import java.util.Arrays;
import java.util.Map;
import java.util.TreeSet;

import com.applang.BaseDirective;
import com.applang.Dialogs;
import com.applang.UserContext;
import com.applang.Util.Job;
import com.applang.pflanzen.PlantsList;
import com.applang.provider.NotePadProvider;
import com.applang.tagesberichte.Tagesberichte;
import com.applang.wetterberichte.WeatherList;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

public class BerichtsheftActivity extends Activity
{
	private static final String TAG = BerichtsheftActivity.class.getSimpleName();
	
	private TextView mTextView;
    
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        showDialog(0);
        
        System.out.printf("VERSION.SDK : %s\n", android.os.Build.VERSION.SDK);
        System.out.printf("databaseList : %s\n", Arrays.toString(this.databaseList()));
	}

    @Override
    protected Dialog onCreateDialog(int id) {
    	return waitWhileWorking(this, "Initializing ...",
    		new Job<Activity>() {
	    		public void perform(final Activity activity, Object[] params) throws Exception {
	    			com.applang.UserContext.setupVelocity(activity, true);

	    			runOnUiThread(new Runnable() {
	    				public void run() {
		    				mTextView = (TextView) findViewById(R.id.textView1);
		    				
		    				for (final int id : new int[] {R.id.textView1, R.id.button1, R.id.button2, R.id.button3, R.id.button4}) {
		    					View vw = findViewById(id);
		    					vw.setOnClickListener(new View.OnClickListener() {
		    						@Override
		    						public void onClick(View v) {
//	        	    					String memory = id == R.id.textView1 ? "on" : "off";
//										getContentResolver().query(NotePadProvider.contentUri("memory/" + memory), null, null, null, null);
		    							
		    							popupContextMenu(BerichtsheftActivity.this, mTextView);
		    							switch (id) {
		    							case R.id.textView1:
		    								break;
		    							case R.id.button1:
		    								showTagesberichte(v);
		    								break;
		    							case R.id.button2:
		    								showPflanze(v);
		    								break;
		    							case R.id.button3:
		    								showMore(v);
		    								break;
		    							case R.id.button4:
		    								showEvenMore(v);
		    								break;
		    							}
		    						}
		    					});	
		    				}
	    			    }
	    			});
	    		}
	    	} 
	    );
    }

	public void showTagesberichte(View clickedButton) {
		Intent activityIntent =
				new Intent(this, Tagesberichte.class);
		startActivity(activityIntent);
		}

	public void showPflanze(View clickedButton) {
		Intent activityIntent =
				new Intent(this, PlantsList.class);
		startActivity(activityIntent);
		}
	
	public void showMore(View clickedButton) {
		Intent activityIntent =
				new Intent(this, WeatherList.class);
		startActivity(activityIntent);
		}
	
	public void showEvenMore(View clickedButton) {
		impex();
		}
	
	private void impex() {
		final String[] databases = databases(BerichtsheftActivity.this);
		AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
		alertDialogBuilder.setTitle("Application data");
		alertDialogBuilder
				.setMessage("Export copies data to SD card\nImport copies data from SD card")
				.setCancelable(false)
				.setPositiveButton("Export",
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog,	int id) {
								ImpexTask.doExport(BerichtsheftActivity.this, databases, null);
							}
						})
				.setNegativeButton("Import",
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog,	int id) {
								ImpexTask.doImport(BerichtsheftActivity.this, databases, null);
							}
						})
				.setNeutralButton("Cancel", null);
		AlertDialog alertDialog = alertDialogBuilder.create();
		alertDialog.show();
    }
    
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		menu.clear();
		menu.setHeaderTitle("Tests");
		SubMenu submenu = menu.addSubMenu(Menu.NONE, Menu.NONE, Menu.NONE, R.string.menu_anweisung);
		anweisungen = UserContext.directives();
		for (String key : anweisungen.keySet()) 
			submenu.add(Menu.NONE, Menu.FIRST, Menu.NONE, key);
		submenu = menu.addSubMenu(Menu.NONE, Menu.NONE, Menu.NONE, R.string.menu_baustein);
		bausteine = NotePadProvider.bausteinMap(getContentResolver(), "");
		for (String key : new TreeSet<String>(bausteine.keySet())) 
			submenu.add(Menu.NONE, Menu.FIRST + 1, Menu.NONE, key);
		menu.add(Menu.NONE, Menu.FIRST + 2, Menu.NONE, R.string.menu_editor);
	}
	
	ValMap bausteine;
	Map<String,String> anweisungen;

	@Override
	public boolean onContextItemSelected(MenuItem item) {
    	String text = item.getTitle().toString();
    	switch (item.getItemId()) {
		case Menu.FIRST:
			test(text);
			return true;

		case Menu.FIRST + 1:
	        String[] strings = getResources().getStringArray(R.array.title_edit_array);
			startActivity(new Intent(Dialogs.PROMPT_ACTION)
					.putExtra(BaseDirective.PROMPT, strings[1])
					.putExtra(BaseDirective.VALUES, new String[]{bausteine.get(text).toString()})
					.putExtra(BaseDirective.TYPE, Dialogs.DIALOG_TEXT_INFO));
			return true;

		case Menu.FIRST + 2:
			startActivity(new Intent(Intent.ACTION_INSERT, NotePadProvider.contentUri(1)));
			return true;
		}
   		return super.onContextItemSelected(item);
	}

    private void test(final String key) {
    	Map<String, String> signatures = signatures();
		String signature = signatures.containsKey(key) ? signatures.get(key) : key;
		UserContext.buildDirective(signature, BerichtsheftActivity.this, null, new Job<Object>() {
			public void perform(Object text, Object[] params) { 
				if (text != null)
					Toast.makeText(BerichtsheftActivity.this, 
							text.toString(), 
							Toast.LENGTH_LONG).show();
			}
		});
		
//		Uri.Builder builder = NotePadProvider.contentUri(1).buildUpon();
//		builder.appendPath(NoteColumns.TITLE).appendPath("planets");
//	    startActivity(new Intent(NoteEvaluator.EVALUATE_ACTION, builder.build()));
		
//		startActivity(new Intent(Intent.ACTION_INSERT, NoteColumns.CONTENT_URI));
		
//		NotePadProvider.fetchNoteById(7, getContentResolver(), 1, new Job<Cursor>() {
//			public void perform(Cursor c, Object[] params) throws Exception {
//				new VelocityContext.EvaluationTask(BerichtsheftActivity.this, 
//						getString(R.string.title_evaluator), 
//						NotePadProvider.bausteinMap(getContentResolver(), ""), 
//						new Job<String>() {
//							public void perform(String text, Object[] params) {
//								Toast.makeText(BerichtsheftActivity.this, text, Toast.LENGTH_LONG).show();
//							}
//						}
//				).execute(c.getString(2));
//			}
//		});
    }
}

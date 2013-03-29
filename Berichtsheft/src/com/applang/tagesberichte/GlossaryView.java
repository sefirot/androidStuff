package com.applang.tagesberichte;

import static com.applang.Util.*;
import static com.applang.Util2.*;
import static com.applang.VelocityUtil.*;

import com.applang.VelocityUtil.MapContext;
import com.applang.provider.NotePad.Notes;
import com.applang.provider.NotePadProvider;

import java.io.StringWriter;
import java.util.Map;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.context.Context;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.webkit.WebView;

public class GlossaryView extends Activity
{
    private static final String TAG = GlossaryView.class.getSimpleName();
    
    /**
     * This is a special intent action that means "view the glossary".
     */
    public static final String VIEW_GLOSSARY_ACTION = "com.applang.tagesberichte.action.VIEW_GLOSSARY";

    private WebView webView;

    private Uri mUri;

    int table = 0;
    
	@SuppressLint("SetJavaScriptEnabled") 
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		webView = new WebView(this);
		webView.getSettings().setJavaScriptEnabled(true);
		setContentView(webView);
    	
        Intent intent = getIntent();
		mUri = intent.getData();

        Bundle extras = intent.getExtras();
        if (extras != null && extras.containsKey("table"))
        	table = extras.getInt("table", 0);
		
		showDialog(0);
	}
	
	void mergeTemplateAndLoad(Context vc, String templateName) {
		StringWriter sw = new StringWriter();
		Template template = Velocity.getTemplate(templateName);
		template.merge(vc, sw);
		webView.loadData(sw.toString(), "text/html", "UTF-8");
	}
	
	void evaluateTemplateAndLoad(Context vc, String template, String logTag) {
		String s = evaluation( vc, template, logTag );
		webView.loadData(s, "text/html", "UTF-8");
	}
    
    @Override
    protected Dialog onCreateDialog(int id) {
        return waitWhileWorking(this, "Evaluating ...", 
        	new Job<Activity>() {
				public void dispatch(Activity activity, Object[] params) throws Exception {
					setupVelocity4Android(packageName(activity), getResources());
					
	    			MapContext noteContext = new MapContext(NoteEvaluator.bausteinMap(activity, ""));
	    			Map<String, String> pmap = NotePadProvider.projectionMap(NotePadProvider.NOTES_WORDS);
	    			
	    			ValList words = new ValList();
					for (String word : TitleEditor.wordSet(activity, 2, "")) {
						ValMap map = new ValMap();
						ValList list = new ValList();
						
						Uri uri = Uri.withAppendedPath(Notes.CONTENT_URI, "words");
						Cursor cursor = activity.managedQuery(uri, 
								new String[] {Notes.NOTE}, 
								pmap.get(Notes.TITLE) + "=?", 
								new String[]{word}, 
								pmap.get("date"));
						try {
							if (cursor.moveToFirst())
								do {
									String note = cursor.getString(0);
									note = evaluation(noteContext, note, "notes");
									list.add(note);
								} while (cursor.moveToNext());
						} catch (Exception e) {
				            Log.e(TAG, "traversing cursor", e);
						}
						finally {
							cursor.close();
						}
						
						map.put("title", word);
						map.put("notes", list);
						words.add(map);
					}
	    			
	    			final VelocityContext vc = new VelocityContext();
	    			vc.put("words", words);
	    			
	    			runOnUiThread(new Runnable() {
	    			    public void run() {
	    			    	 mergeTemplateAndLoad(vc, "glossary");
	    			    }
	    			});
				}
	        });
    }
}

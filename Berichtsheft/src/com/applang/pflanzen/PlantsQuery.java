package com.applang.pflanzen;


import java.util.Random;

import com.applang.Util;
import com.applang.berichtsheft.R;
import com.applang.berichtsheft.R.layout;
import com.applang.provider.NotePad.NoteColumns;
import com.applang.provider.PlantInfo.Plants;


import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.FrameLayout.LayoutParams;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Random;

public class PlantsQuery extends Activity
{
  private static final int COLUMN_INDEX_BOTNAME = 3;
  private static final int COLUMN_INDEX_FAMILY = 2;
  private static final int COLUMN_INDEX_NOTE = 1;
  private static final String ORIGINAL_CONTENT = "origContent";
  private static final String[] PROJECTION = { "_id", "name", "family", "botname", "botfamily", "crop_group" };
  private static final String[] PROJECTION_ID = { "_id" };
  private static final String TAG = "PlantsQuery";
  private Cursor mCursor;
  private TextView mFirstAnswerText;
  private TextView mFirstGuessText;
  private TextView mLargeText;
  private String mOriginalContent;
  private RelativeLayout mRelativeLayout;
  private TextView mSecondAnswerText;
  private TextView mSecondGuessText;
  private Uri mUri;
  private View mView1;
  private View mView2;
  private View mView3;
  
  
  protected void onCreate(final Bundle savedInstanceState)
  {
    super.onCreate(savedInstanceState);
    mUri = getIntent().getData();
    setContentView(R.layout.plants_query_view);
    mLargeText = ((TextView)findViewById(R.id.largeText));
    mFirstGuessText = ((EditText)findViewById(R.id.query_edit_name));
    mSecondGuessText = ((EditText)findViewById(R.id.query_edit_fam));
    String str = getRandomEntry();
    if (str == "no entry") {
      onCreate(savedInstanceState);
    } else {
      mLargeText.setText(str);
    }  
 
      final Button checkButton = (Button)findViewById(R.id.query_check_button);
      Button showButton = (Button)findViewById(R.id.query_show_button);
      checkButton.setTag(1);
      showButton.setTag(1);
      checkButton.setOnClickListener(new View.OnClickListener()
      {
        public void onClick(View paramAnonymousView)
        {
          int i = ((Integer)paramAnonymousView.getTag()).intValue();
          if (i == 1) {
            assertQueryEqual(paramAnonymousView);  
          }
          if (i == 2) {
            onCreate(savedInstanceState);
          }
        }
      });
      showButton.setOnClickListener(new View.OnClickListener()
      {
        public void onClick(View paramAnonymousView)
        {
          if (((Integer)paramAnonymousView.getTag()).intValue() == 1)
          {
        	paramAnonymousView.setTag(2);
        	checkButton.setTag(2);
        	showRightAnswer(paramAnonymousView);
            endOfStep(paramAnonymousView);
         // return;
          }
          /*finish();
          paramAnonymousView.setTag(1);*/
        }
      });
      if (savedInstanceState != null)
        mOriginalContent = savedInstanceState.getString("origContent");
      //  onCreate(savedInstanceState);
      // return;
      
    }
 // }

  protected String getRandomEntry()
  {
    mCursor = managedQuery(mUri, PROJECTION_ID, null, null, null);
    int i = mCursor.getCount();
    int j = new Random().nextInt(i);
    if (j != 0)
    {
      mCursor = managedQuery(mUri, PROJECTION, null, null, null);
      mCursor.moveToPosition(j);
      String str1 = mCursor.getString(2);
      String str2 = mCursor.getString(3);
      if ((str1 != null) && (str2 != null) && (str1 != "") && (str2 != "")) {
        return mCursor.getString(1);
      }
    }
    return "no entry";
  }
  
  private void assertQueryEqual(View paramView)
  {
    String nameGuess = this.mFirstGuessText.getText().toString();
    String famGuess = this.mSecondGuessText.getText().toString();
    String familyName = this.mCursor.getString(4) + " ";
    String plantName = this.mCursor.getString(3) + " ";
    Button checkButton = (Button)findViewById(R.id.query_check_button);
    int count = 0;    
    
    
    if (nameGuess.equals("") || famGuess.equals("")) {
    	Toast.makeText(this, "Beides angeben!", Toast.LENGTH_LONG).show();
    }
    else {
    	if (nameGuess.compareTo(plantName) == 0 && famGuess.compareTo(familyName) != 0) {
	        count++;
	        showThumbRating(count);
	        checkButton.setText("Nochmal");
	        showRightAnswer(paramView);
    	} else if (nameGuess.compareTo(plantName) != 0 && famGuess.compareTo(familyName) == 0) {
	        count++;
	        showThumbRating(count);
	        checkButton.setText("Nochmal");
	        paramView.setTag(0);
	        showRightAnswer(paramView);
		} else if (nameGuess.compareTo(plantName) == 0 && famGuess.compareTo(familyName) == 0) {
	        count = 2;
	        showThumbRating(count);
	        paramView.setTag(2);
	        showRightAnswer(paramView);
	        endOfStep(paramView);        
		} else if (nameGuess.compareTo(plantName) != 0 && famGuess.compareTo(familyName) != 0) {
	        showThumbRating(count);
	        paramView.setTag(2);
	        showRightAnswer(paramView);
	        endOfStep(paramView);
		} 
    }  
  }

  private void showThumbRating(int count){
	 
	  ImageView thumb = ((ImageView)findViewById(R.id.image_view));
	  
		switch (count) {
	    case 1:
	    	thumb.setImageResource(R.drawable.thumbs_aside_225);
			break;
		case 2:
			thumb.setImageResource(R.drawable.thumbs_up_225);
			break;
		case 0:
			thumb.setImageResource(R.drawable.thumbs_down_225);
			break;
	    }
  }

  

  private void showRightAnswer(View paramView)
  {
    String famName = mCursor.getString(4);
    String plantName = mCursor.getString(3);
    LinearLayout queryLayout = ((LinearLayout)findViewById(R.id.query_layout));
    EditText queryEditName = ((EditText)findViewById(R.id.query_edit_name));
    EditText queryEditFam = ((EditText)findViewById(R.id.query_edit_fam));
    TextView queryTextName = new TextView(this);
    TextView queryTextFam = new TextView(this);
    LayoutParams textViewLayoutParams = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
    queryTextName.setLayoutParams(textViewLayoutParams);
    queryTextName.setTextSize(23);
	queryTextName.setTextColor(this.getResources().getColor(R.color.light_green));
	queryTextName.setGravity(0x11);
    queryTextFam.setLayoutParams(textViewLayoutParams);
    queryTextFam.setTextSize(23);
	queryTextFam.setTextColor(this.getResources().getColor(R.color.close_mangenta));
    int checkNum = ((Integer)paramView.getTag()).intValue();
    
    switch (checkNum) {
    case 0:
    	queryLayout.removeView(queryEditFam);
		queryTextFam.setText(famName);
		queryLayout.addView(queryTextFam,2);
		paramView.setTag(1);
		break;
    case 1:
    	queryLayout.removeView(queryEditName);
		queryTextName.setText(plantName);
		queryLayout.addView(queryTextName,1);
		break;
	case 2:
		queryLayout.removeView(queryEditName);
		queryTextName.setText(plantName);
		queryLayout.addView(queryTextName,1);
		
		queryLayout.removeView(queryEditFam);
		queryTextFam.setText(famName);
		queryLayout.addView(queryTextFam,2);
		break;
    }
  }

  private void endOfStep(View paramView)
  {
	Button checkButton = (Button)findViewById(R.id.query_check_button);
    Button showButton = (Button)findViewById(R.id.query_show_button);
    checkButton.setText("Weiter");
    showButton.setText("Speichern");
  }
   
}


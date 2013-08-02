package com.applang.pflanzen;


import java.util.Random;

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
import android.widget.RelativeLayout;
import android.widget.TextView;
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

  private void assertQueryEqual(View paramView)
  {
    String str1 = this.mFirstGuessText.getText().toString();
    String str2 = this.mSecondGuessText.getText().toString();
    String str3 = this.mCursor.getString(2);
    String str4 = this.mCursor.getString(3);
    int i;
    if (str1 != null)
      if (str1.compareTo(str4) == 0)
      {
        this.mFirstAnswerText.setText("Botanischer Name ist richtig!");
        i = 0 + 1;
        if ((str2 == null) || (str3 == null))
          break label172;
        if (str2.compareTo(str3) != 0)
          break label160;
        this.mSecondAnswerText.setText("Pflanzenfamilie ist richtig!");
        i++;
      }
    Button localButton;
    while (true)
    {
      localButton = (Button)findViewById(2131296297);
      if (i != 2)
        break label184;
      endOfStep(paramView);
      return;
      mFirstAnswerText.setText("Botanischer Name ist falsch!");
      i = 0;
      break;
      this.mFirstAnswerText.setText("Botanischer Name fehlt!");
      i = 0;
      break;
      label160: this.mSecondAnswerText.setText("Pflanzenfamilie ist falsch!");
      continue;
      label172: this.mSecondAnswerText.setText("Familename fehlt!");
    }
    label184: paramView.setTag(Integer.valueOf(2));
    localButton.setText("Nochmal");
  }

  private void endOfStep(View paramView)
  {
    Button localButton1 = (Button)findViewById(2131296297);
    Button localButton2 = (Button)findViewById(2131296298);
    localButton1.setText("Weiter");
    localButton2.setText("Speichern");
    localButton1.setTag(Integer.valueOf(0));
    localButton2.setTag(Integer.valueOf(0));
  }

  private void showRightAnswer(View paramView)
  {
    String str1 = this.mCursor.getString(2);
    String str2 = this.mCursor.getString(3);
    mFirstGuessText.setText("");
    this.mSecondGuessText.setText("");
    this.mFirstGuessText.setHint("Bot. Name: " + str2);
    this.mSecondGuessText.setHint("Familie: " + str1);
  }

  protected String getRandomEntry()
  {
    this.mCursor = managedQuery(this.mUri, PROJECTION_ID, null, null, null);
    int i = this.mCursor.getCount();
    int j = new Random().nextInt(i);
    if (j != 0)
    {
      this.mCursor = managedQuery(this.mUri, PROJECTION, null, null, null);
      this.mCursor.moveToPosition(j);
      String str1 = this.mCursor.getString(2);
      String str2 = this.mCursor.getString(3);
      if ((str1 != null) && (str2 != null) && (str1 != "") && (str2 != ""))
        return this.mCursor.getString(1);
      return "No Entry";
    }
    return "No Entry";
  }

  protected void onCreate(final Bundle paramBundle)
  {
    super.onCreate(paramBundle);
    this.mUri = getIntent().getData();
    setContentView(2130903047);
    this.mLargeText = ((TextView)findViewById(2131296287));
    this.mFirstGuessText = ((EditText)findViewById(2131296293));
    this.mSecondGuessText = ((EditText)findViewById(2131296295));
    this.mFirstAnswerText = ((TextView)findViewById(2131296288));
    this.mSecondAnswerText = ((TextView)findViewById(2131296289));
    String str = getRandomEntry();
    if (str != "No Entry")
      this.mLargeText.setText(str);
    while (true)
    {
      final Button localButton1 = (Button)findViewById(2131296297);
      Button localButton2 = (Button)findViewById(2131296298);
      localButton1.setTag(Integer.valueOf(1));
      localButton2.setTag(Integer.valueOf(1));
      localButton1.setOnClickListener(new View.OnClickListener()
      {
        public void onClick(View paramAnonymousView)
        {
          int i = ((Integer)paramAnonymousView.getTag()).intValue();
          if (i == 1)
          {
            PlantsQuery.this.assertQueryEqual(paramAnonymousView);
            return;
          }
          if (i == 2)
          {
            PlantsQuery.this.assertQueryEqual(paramAnonymousView);
            localButton1.setText("Weiter");
            paramAnonymousView.setTag(Integer.valueOf(0));
            return;
          }
          PlantsQuery.this.onCreate(paramBundle);
          paramAnonymousView.setTag(Integer.valueOf(1));
        }
      });
      localButton2.setOnClickListener(new View.OnClickListener()
      {
        public void onClick(View paramAnonymousView)
        {
          if (((Integer)paramAnonymousView.getTag()).intValue() == 1)
          {
            PlantsQuery.this.showRightAnswer(paramAnonymousView);
            PlantsQuery.this.endOfStep(paramAnonymousView);
            return;
          }
          PlantsQuery.this.finish();
          paramAnonymousView.setTag(Integer.valueOf(1));
        }
      });
      if (paramBundle != null)
        this.mOriginalContent = paramBundle.getString("origContent");
      return;
      onCreate(paramBundle);
    }
  }
}

package com.applang.berichtsheft;

import com.applang.tagesberichte.*;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

public class BerichtsheftActivity extends Activity {
    private String mButtonMessageTemplate;

	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        mButtonMessageTemplate =
        		getString(R.string.button_message_template);
        		
	}
	
	public void showTagesberichte(View clickedButton) {
		Button button = (Button)clickedButton;
		Intent activityIntent =
				new Intent(this, NotesList.class);
		startActivity(activityIntent);
		}

	public void showPflanze(View clickedButton) {
		Button button = (Button)clickedButton;
		CharSequence text = button.getText();
		String message =
				String.format(mButtonMessageTemplate, text);
		showToast(message);
		}
	
	public void showGroupView(View clickedButton) {
		Button button = (Button)clickedButton;
		CharSequence text = button.getText();
		String message =
				String.format(mButtonMessageTemplate, text);
		showToast(message);
		}
	
	public void showCalenderView(View clickedButton) {
		Button button = (Button)clickedButton;
		CharSequence text = button.getText();
		String message =
				String.format(mButtonMessageTemplate, text);
		showToast(message);
		}
	
	
	
	private void showToast(String text) {
		Toast.makeText(this, text, Toast.LENGTH_LONG).show();	 
		}

}

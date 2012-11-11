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
        
        for (final int id : new int[] {R.id.button1, R.id.button2, R.id.button3, R.id.button4}) {
        	Button btn = (Button) findViewById(id);
        	btn.setOnClickListener(new View.OnClickListener() {
        	    @Override
        	    public void onClick(View v) {
        	        switch (id) {
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
        
		showToast(android.os.Build.VERSION.SDK);
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
	
	public void showMore(View clickedButton) {
		Button button = (Button)clickedButton;
		CharSequence text = button.getText();
		String message =
				String.format(mButtonMessageTemplate, text);
		showToast(message);
		}
	
	public void showEvenMore(View clickedButton) {
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

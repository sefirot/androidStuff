/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.applang.pflanzen;



import com.applang.berichtsheft.R;
import com.applang.berichtsheft.R.array;
import com.applang.berichtsheft.R.id;
import com.applang.berichtsheft.R.layout;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;


public class SortBySpinner extends Activity implements OnItemSelectedListener {

    int language, order;
    private Intent sortOrder;
    private Bundle sortBundle;
    private ArrayAdapter<CharSequence> languageAdapter;
    private ArrayAdapter<CharSequence> orderAdapter;
    private Spinner languageSpinner;
	private Spinner orderSpinner;
	private Button btnSubmit;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.sort_by_spinner);
        languageSpinner = (Spinner) findViewById(R.id.language_spinner);
    	orderSpinner = (Spinner) findViewById(R.id.order_spinner);
    	btnSubmit = (Button) findViewById(R.id.btnSubmit);
    	addListenerOnSubmittButton();
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Create an ArrayAdapter using the string array and a default spinner layout
    	languageAdapter = ArrayAdapter.createFromResource(this,
    	        R.array.by_language_array, android.R.layout.simple_spinner_item);
    	orderAdapter = ArrayAdapter.createFromResource(this,
    	        R.array.by_order_array, android.R.layout.simple_spinner_item);
    	
    	// Specify the layout to use when the list of choices appears
    	languageAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    	orderAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    	
    	// unpack sortOrder bundle from intent
    	sortOrder = getIntent();
    	sortBundle = sortOrder.getExtras();
    	language = sortBundle.getInt("language");
    	order = sortBundle.getInt("order");
    	
    	// Apply the adapter to the spinner
    	orderSpinner.setAdapter(orderAdapter);
    	languageSpinner.setAdapter(languageAdapter);
    	
    	// set string array data position to be posted in Spinner 
    	languageSpinner.setSelection(language);
    	orderSpinner.setSelection(order);
    	
    	// run Spinner & SelectedListener
    	languageSpinner.setOnItemSelectedListener(this);
    	orderSpinner.setOnItemSelectedListener(this);
    	
    }

	@Override
	public void onItemSelected(AdapterView<?> parent, View view, int pos,
			long id) {
//		Toast.makeText(parent.getContext(), "OnItemSelectedListener : " + parent.getItemAtPosition(pos).toString() 
//																		+ parent.getLastVisiblePosition()
//																		+ parent.getItemAtPosition(parent.getLastVisiblePosition()).toString(),
//						Toast.LENGTH_SHORT).show(); 
		
		if (parent.getAdapter() == languageAdapter) { 
			language = parent.getLastVisiblePosition();
//			Toast.makeText(parent.getContext(), "Language : " + parent.getItemAtPosition(pos).toString() 
//					+ parent.getLastVisiblePosition()
//					+ language, 
//					Toast.LENGTH_SHORT).show();
		} else if (parent.getAdapter() == orderAdapter) {
			order = parent.getLastVisiblePosition();
//			Toast.makeText(parent.getContext(), "Order : " + parent.getItemAtPosition(pos).toString() 
//					+ parent.getLastVisiblePosition()
//					+ order,
//					Toast.LENGTH_SHORT).show();
		}
		
		
		
	}

	@Override
	public void onNothingSelected(AdapterView<?> arg0) {
		Toast.makeText(this, "Here we are onNothingSelected!!", Toast.LENGTH_LONG).show();
		
	}
	
	public void addListenerOnSubmittButton() {
		 	
	 
		btnSubmit.setOnClickListener(new OnClickListener() {
	 
		  public void onClick(View v) {
			  sortBundle.putInt("language", language);
				sortBundle.putInt("order", order);
				sortOrder.putExtras(sortBundle);
		        setResult(RESULT_OK, sortOrder);
			    finish();
		  }
	 
		});
	  }
	
	
	@Override
	protected void onPause() {
        super.onPause();
        
        // Toast.makeText(this, "Here we are onPause!!", Toast.LENGTH_LONG).show();
    }
}

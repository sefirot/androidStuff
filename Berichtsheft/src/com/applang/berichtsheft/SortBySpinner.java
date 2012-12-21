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

package com.applang.berichtsheft;



import com.applang.berichtsheft.R;

import android.app.Activity;


import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Spinner;
import android.widget.Toast;

public class SortBySpinner extends Activity implements OnItemSelectedListener {

   
	private Spinner languageSpinner;
	private Spinner orderSpinner;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.sort_by_spinner);
        
        
        languageSpinner = (Spinner) findViewById(R.id.language_spinner);
    	orderSpinner = (Spinner) findViewById(R.id.order_spinner);
    }

    @Override
    protected void onResume() {
        super.onResume();

     // Create an ArrayAdapter using the string array and a default spinner layout
    	ArrayAdapter<CharSequence> languageAdapter = ArrayAdapter.createFromResource(this,
    	        R.array.by_language_array, android.R.layout.simple_spinner_item);
    	ArrayAdapter<CharSequence> orderAdapter = ArrayAdapter.createFromResource(this,
    	        R.array.by_order_array, android.R.layout.simple_spinner_item);
    	// Specify the layout to use when the list of choices appears
    	languageAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    	orderAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    	// Apply the adapter to the spinner
    	orderSpinner.setAdapter(orderAdapter);
    	languageSpinner.setAdapter(languageAdapter);
    	languageSpinner.setOnItemSelectedListener(this);
    	orderSpinner.setOnItemSelectedListener(this);
    	
    }

	@Override
	public void onItemSelected(AdapterView<?> parent, View view, int pos,
			long id) {
		Toast.makeText(parent.getContext(), "OnItemSelectedListener : " + parent.getItemAtPosition(pos).toString(),
				Toast.LENGTH_SHORT).show();
		
		
	}

	@Override
	public void onNothingSelected(AdapterView<?> arg0) {
		Toast.makeText(this, "Here we are onNothingSelected!!", Toast.LENGTH_LONG).show();
		
	}
	
	@Override
	protected void onPause() {
        super.onPause();
        
        // Toast.makeText(this, "Here we are onPause!!", Toast.LENGTH_LONG).show();
    }
}

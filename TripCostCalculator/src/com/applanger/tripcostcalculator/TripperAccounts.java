package com.applanger.tripcostcalculator;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;


public class TripperAccounts extends Activity {
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        TextView textview = new TextView(this);
        textview.setText("Baustelle");
        setContentView(textview);
    }
}
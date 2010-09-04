package com.caspergasper.android.goodreads;

import android.app.Activity;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

public class PreferencesActivity extends Activity {

	private GoodReadsApp myApp;
	private Spinner updatesSpinner;
	
	public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.preferences);
        myApp = GoodReadsApp.getInstance();
        updatesSpinner = (Spinner) findViewById(R.id.num_of_updates_spinner);
        populateSpinners();
	}

	private void populateSpinners() {
		ArrayAdapter<CharSequence> fAdapter;
		fAdapter = ArrayAdapter.createFromResource(this, R.array.max_updates, 
				android.R.layout.simple_spinner_item);
		
		int spinner_dd_item = android.R.layout.simple_spinner_dropdown_item;
		fAdapter.setDropDownViewResource(spinner_dd_item);
		updatesSpinner.setAdapter(fAdapter);
		
	}

	@Override
	public void onPause() {
		super.onPause();
		int updatesSpinnerIndex = updatesSpinner.getSelectedItemPosition();
		
		
	}
	
	
}

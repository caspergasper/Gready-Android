package com.caspergasper.android.goodreads;

import java.util.ArrayList;
import java.util.List;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

public class Preferences extends PreferenceActivity implements OnSharedPreferenceChangeListener {

	private GoodReadsApp myApp;
	
	public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.userpreferences);  
        myApp = GoodReadsApp.getInstance();
        
        populateStartupListPreference();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(myApp);
        prefs.registerOnSharedPreferenceChangeListener(this);
	}
	
	@Override 
	protected void onResume() {
		super.onResume();
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
		ListPreference listPref;
		if(key.compareToIgnoreCase(GoodReadsApp.PREF_NUM_OF_UPDATES) == 0) {
			myApp.numberOfUpdates = myApp.global_settings.getString(GoodReadsApp.PREF_NUM_OF_UPDATES, "All");
			listPref = (ListPreference) findPreference(GoodReadsApp.PREF_NUM_OF_UPDATES);
			listPref.setSummary(myApp.numberOfUpdates);
		} else if(key.compareToIgnoreCase(GoodReadsApp.PREF_STARTUP_SHELF) == 0) {
			listPref = (ListPreference) findPreference(GoodReadsApp.PREF_STARTUP_SHELF);
			listPref.setSummary(myApp.global_settings.getString(GoodReadsApp.PREF_STARTUP_SHELF, "Updates"));
		}
	}

	private void populateStartupListPreference() {
		ListPreference listPref = (ListPreference) findPreference(GoodReadsApp.PREF_STARTUP_SHELF);
		List<CharSequence> startupArray = new ArrayList<CharSequence>();
		startupArray.add("Updates");
		for(Shelf shelf : myApp.userData.shelves) {
			startupArray.add(shelf.title);
		}

		CharSequence[] chars = startupArray.toArray(new CharSequence[startupArray.size()]);
		listPref.setEntries(chars);
		listPref.setEntryValues(chars);
		listPref.setSummary(myApp.global_settings.getString(GoodReadsApp.PREF_STARTUP_SHELF, "Updates"));
		listPref = (ListPreference) findPreference(GoodReadsApp.PREF_NUM_OF_UPDATES);
		listPref.setSummary(myApp.numberOfUpdates);
	}	
	
}

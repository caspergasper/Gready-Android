package com.caspergasper.android.goodreads;

import static com.caspergasper.android.goodreads.GoodReadsApp.TAG;

import java.io.InputStream;

import org.apache.http.HttpResponse;

import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import android.os.AsyncTask;
import android.util.Log;

class MyAsyncTask extends AsyncTask<HttpGet, Void, Integer> {
	GoodReadsApp myApp;
	@Override
	protected Integer doInBackground(HttpGet... request) {	
		myApp = GoodReadsApp.getInstance();
		
	    try {
	    Log.d(TAG, "Start downloading.");
	    // send the request
        HttpClient httpClient = new DefaultHttpClient();
        HttpResponse response = httpClient.execute(request[0]);
	    Log.d(TAG, response.getStatusLine().toString());
	    InputStream is = response.getEntity().getContent();
	    
	    Log.d(TAG, "End downloading.");
	    switch(myApp.oauth.goodreads_url) {
	    case OAuth_interface.GET_USER_ID: 
	    	// Get the ID and add to saved prefs.
	    	Log.d(TAG, "Getting userid");
	    	myApp.userData.getSAXUpdates(is);
	    	myApp.addTokenToPrefs(GoodReadsApp.USER_ID, 
	    			myApp.userID);
	    	break;
//	    case OAuth_interface.GET_USER_INFO: 
////	    	myApp.userData.getUpdates(is);
//	    	break;
	    case OAuth_interface.GET_FRIEND_UPDATES:
	    case OAuth_interface.GET_SHELVES:
	    case OAuth_interface.GET_SHELF:
	    	myApp.userData.getSAXUpdates(is);
	    	break;
	    } 
	    } catch(Exception e) { 
	    	Log.d(TAG, "ERROR! " + e.toString());
//	    	throw new RuntimeException(e.toString());
	    	myApp.errMessage = e.toString();
	    	return 1;
	    }
	    return 0;
	}
	
	@Override
	protected void onProgressUpdate(Void... params) {
	        super.onProgressUpdate(params);
	}

	@Override
	protected void onPostExecute(Integer result) {
	    super.onPostExecute(result);     
	    myApp.goodreads_activity.updateMainScreenForUser(result);
	}


}
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
		myApp.threadLock = true;
	    try {
	    Log.d(TAG, request[0].getRequestLine().toString());
	    // send the request
        HttpClient httpClient = new DefaultHttpClient();
        HttpResponse response = httpClient.execute(request[0]);
	    int responseCode = response.getStatusLine().getStatusCode();
	    if(responseCode == 404 && myApp.oauth.goodreads_url == OAuthInterface.GET_BOOKS_BY_ISBN) {
	    	myApp.errMessage = "Sorry, can't find this book on goodreads -- is it a valid book barcode?";
	    	return 1;
	    } else if(responseCode == 401 || responseCode == 403) {
	    	// Authorization failed.
	    	Log.d(TAG, response.getStatusLine().toString());
	    	myApp.errMessage = "Authentication Error!  Cannot download " + response.getStatusLine() + 
	    		" " + response.getStatusLine().getStatusCode();
	    	return 2;
	    } else if(responseCode != 200) {
	    	myApp.errMessage = "I/O ERROR!  Cannot download " + response.getStatusLine() + 
    		" " + response.getStatusLine().getStatusCode();
	    	return 1;
	    }
	    InputStream is = response.getEntity().getContent();
	    Log.d(TAG, "End downloading.");
	    
	    switch(myApp.oauth.goodreads_url) {
	    case OAuthInterface.GET_USER_ID: 
	    	// Get the ID and add to saved prefs.
	    	Log.d(TAG, "Getting userid");
	    	myApp.userData.getSAXUserid(is);
	    	myApp.addTokenToPrefs(GoodReadsApp.USER_ID, 
	    			myApp.userID);
	    	break;
	    case OAuthInterface.GET_FRIEND_UPDATES:
	    	myApp.userData.getSAXUpdates(is);
	    	break;
	    case OAuthInterface.GET_SHELVES:
	    	myApp.userData.getSAXShelves(is);
	    	break;
	    case OAuthInterface.SEARCH_SHELVES:
	    case OAuthInterface.GET_BOOKS_BY_ISBN:
	    case OAuthInterface.GET_SHELF:
	    case OAuthInterface.GET_SHELF_FOR_UPDATE:
	    	myApp.userData.getSAXBooks(is, myApp.oauth.goodreads_url);
	    	break;
	    } 
	    } catch(Exception e) { 
	    	Log.e(TAG, "ERROR! " + e.toString());
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
	    myApp.threadLock = false;
	    if(myApp.goodreads_activity instanceof UpdatesActivity) {
	    	((UpdatesActivity) myApp.goodreads_activity).updateMainScreenForUser(result);
		} else {
		    ((BooksActivity) myApp.goodreads_activity).updateMainScreenForUser(result);
	    }
	}


}
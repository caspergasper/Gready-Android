package com.caspergasper.android.goodreads;

import static com.caspergasper.android.goodreads.GoodReadsApp.TAG;

import oauth.signpost.OAuthConsumer;
import oauth.signpost.OAuthProvider;
import oauth.signpost.commonshttp.CommonsHttpOAuthConsumer;
import oauth.signpost.commonshttp.CommonsHttpOAuthProvider;
import oauth.signpost.exception.OAuthException;

import org.apache.http.client.methods.HttpGet;

import android.util.Log;

public class OAuth_interface {
	
	private static final String CONSUMER_KEY = "UvPjrkah6sJXg88qs75xRA";
	private static final String CONSUMER_SECRET = "4yFWaSYwjGdzoQ3dVTsv9O3CMMEPIYBuDw6v15rRao";
	static final String URL_ADDRESS = "http://www.goodreads.com/";
	private static final String REQUEST_TOKEN_ENDPOINT_URL = "oauth/request_token";
	private static final String ACCESS_TOKEN_ENDPOINT_URL = "oauth/access_token";
	private static final String AUTHORIZE_WEBSITE_URL = "oauth/authorize";
	
	static final String GET_USER_ID_PATH = "api/auth_user";
	static final String USER_INFO_URL_PATH = "user/show/";
	static final String BOOKPAGE_PATH = "m/book"; // mobile site -- main one is "book/show";
	static final String SHELF_URL_PATH = "review/list/";
	static final String SHELVES_URL_PATH ="shelf/list?format=xml&key=" + CONSUMER_KEY;
	static final String UPDATES_URL_PATH = "updates/friends.xml";
	static final String GET_USER_BOOK_PATH = "review/show_by_user_and_book?format=xml&key="; // msr

	static final String CALLBACK_URL = "GoodreadsActivity://token";
	static final String OAUTH_VERIFIER = "oauth_token";
	
	// More efficient to use these than enum
	public static final int GET_USER_ID = 0;
	public static final int GET_USER_INFO = 1;
	public static final int GET_SHELF = 2;
	public static final int GET_FRIEND_UPDATES = 3;
	public static final int GET_SHELVES = 4;
	public static final int GET_USER_BOOK = 5; // msr
	
	int goodreads_url;
	private OAuthConsumer consumer;
	private OAuthProvider provider;
	private GoodReadsApp myApp;
	
	OAuth_interface() {
		// create a consumer object and configure it with the access
        // token and token secret obtained from the service provider
        consumer = new CommonsHttpOAuthConsumer(CONSUMER_KEY,
                                             CONSUMER_SECRET);

        myApp = GoodReadsApp.getInstance();
	}
	
	void updateTokens() {
		if(myApp.accessToken != null && myApp.accessTokenSecret != null) {
        	consumer.setTokenWithSecret(myApp.accessToken, 
        		myApp.accessTokenSecret);
		}
	}
	
	String getRequestToken() {
		try {
		    // create a new service provider object and configure it with
	        // the URLs which provide request tokens, access tokens, and
	        // the URL to which users are sent in order to grant permission
	        // to your application to access protected resources
	        provider = new CommonsHttpOAuthProvider(
	        		URL_ADDRESS + REQUEST_TOKEN_ENDPOINT_URL, URL_ADDRESS + ACCESS_TOKEN_ENDPOINT_URL,
	        		URL_ADDRESS + AUTHORIZE_WEBSITE_URL);
	        
	        // fetches a request token from the service provider and builds
	        // a url based on AUTHORIZE_WEBSITE_URL and CALLBACK_URL to
	        // which your app must now send the user 
	        String url = provider.retrieveRequestToken(consumer, CALLBACK_URL);
	        Log.d(TAG, url);
	        return url;
	        
			} catch(Exception e) {
				Log.e(TAG, "Exception: " + e.toString());
				myApp.errMessage = e.toString();
				return null;
			}
	}
	
	boolean getAccessToken(String verificationCode) {
		try {
			provider.retrieveAccessToken(consumer, verificationCode);
			myApp.accessToken = consumer.getToken();
			myApp.addTokenToPrefs(GoodReadsApp.ACCESS_TOKEN, myApp.accessToken);
			myApp.accessTokenSecret = consumer.getTokenSecret();
			myApp.addTokenToPrefs(GoodReadsApp.ACCESS_TOKEN_SECRET, 
					myApp.accessTokenSecret);
			return true;
		} catch(OAuthException e) {
			Log.e(TAG, e.toString());
			myApp.errMessage = e.toString();
		}
		return false;
	}
	
	
	void getXMLFile() {
		// create an HTTP request to a protected resource
        String url_string;
		
    	switch(goodreads_url) {
    	case GET_USER_INFO: 
    		url_string = URL_ADDRESS + USER_INFO_URL_PATH + myApp.userID + ".xml?key=" + CONSUMER_KEY;
    		break;
    	case GET_SHELF:
    		url_string = URL_ADDRESS + SHELF_URL_PATH + myApp.userID + 
    		".xml?v=2&key=" + CONSUMER_KEY + "&shelf=" + myApp.userData.shelf_to_get +
    		"&page=" + ++myApp.userData.bookPage;
    		break;
    	case GET_SHELVES:
    		url_string = URL_ADDRESS + SHELVES_URL_PATH + "&user_id=" + myApp.userID;
    		break;
    	case GET_USER_ID:
    		url_string = URL_ADDRESS + GET_USER_ID_PATH;
    		break;
    	case GET_FRIEND_UPDATES:
    		url_string = URL_ADDRESS + UPDATES_URL_PATH;
    		break;
    	case GET_USER_BOOK: // msr
    		url_string = URL_ADDRESS + CONSUMER_KEY + "&book_id=" + myApp.userData.book_to_get + "&user_id=" + myApp.userID;
    		break;
    		
    	default: 
    		url_string = "";
    	
    	}
        
        try {
        	HttpGet request = new HttpGet(url_string);
        	
    		// sign the request
    		consumer.sign(request);
        	        	
        	// Create background thread to download and render XML
        	new MyAsyncTask().execute(request);
		    
        } catch(OAuthException e) {
        	Log.e(TAG, e.toString());
        	myApp.errMessage = e.toString();
        	throw new RuntimeException(e.toString());
        } 
	}

}

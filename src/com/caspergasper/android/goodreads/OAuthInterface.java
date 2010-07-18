package com.caspergasper.android.goodreads;

import static com.caspergasper.android.goodreads.GoodReadsApp.TAG;

import java.util.LinkedList;

import oauth.signpost.OAuthConsumer;
import oauth.signpost.OAuthProvider;
import oauth.signpost.commonshttp.CommonsHttpOAuthConsumer;
import oauth.signpost.commonshttp.CommonsHttpOAuthProvider;
import oauth.signpost.exception.OAuthException;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;

import android.util.Log;

public class OAuthInterface {

	static final String URL_ADDRESS = "http://www.goodreads.com/";
	private static final String REQUEST_TOKEN_ENDPOINT_URL = "oauth/request_token";
	private static final String ACCESS_TOKEN_ENDPOINT_URL = "oauth/access_token";
	private static final String AUTHORIZE_WEBSITE_URL = "oauth/authorize?mobile=1";
	
	static final String GET_USER_ID_PATH = "api/auth_user";
	static final String USER_INFO_URL_PATH = "user/show/";
	static final String BOOKPAGE_PATH = "m/book"; // mobile site -- main one is "book/show";
	static final String BOOKPAGE_SEARCH = "m/book?search_type=books&search[query]=";
	static final String SHELF_URL_PATH = "review/list/";
	static final String SHELVES_URL_PATH ="shelf/list?format=xml&key=" + DeveloperKeys.CONSUMER_KEY;
	static final String UPDATES_URL_PATH = "updates/friends.xml";
	static final String	BOOKS_ISBN_PATH = "book/isbn";
	static final String ADD_BOOK_PATH = "shelf/add_to_shelf.xml";
	static final String CALLBACK_URL = "goodreadsactivity://token";
	static final String OAUTH_VERIFIER = "oauth_token";
	
	// Eventually I'd like to do something clever like detect if the user is on wifi
	// or 3G and download more/less items appropriately.
	final static int ITEMS_TO_DOWNLOAD = 20;
	
	// More efficient to use these than enum
	public static final int GET_USER_ID = 0;
	public static final int GET_USER_INFO = 1;
	public static final int GET_SHELF = 2;
	public static final int GET_FRIEND_UPDATES = 3;
	public static final int GET_SHELVES = 4;
	public static final int GET_BOOKS_BY_ISBN = 5;
	public static final int SEARCH_SHELVES = 6;
	
	int goodreads_url;
	private OAuthConsumer consumer;
	private OAuthProvider provider;
	private GoodReadsApp myApp;
	String searchQuery;
	private int bookId;
	private String shelf;
	
	OAuthInterface() {
		// create a consumer object and configure it with the access
        // token and token secret obtained from the service provider
        consumer = new CommonsHttpOAuthConsumer(DeveloperKeys.CONSUMER_KEY,
        		DeveloperKeys.CONSUMER_SECRET);

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
	        myApp.addTokenToPrefs(GoodReadsApp.ACCESS_TOKEN, consumer.getToken());
	        myApp.addTokenToPrefs(GoodReadsApp.ACCESS_TOKEN_SECRET, 
					consumer.getTokenSecret());
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
			if(provider == null) {
				// If app has been removed from memory need to recreate provider
				provider = new CommonsHttpOAuthProvider(
		        	URL_ADDRESS + REQUEST_TOKEN_ENDPOINT_URL, URL_ADDRESS + ACCESS_TOKEN_ENDPOINT_URL,
		        	URL_ADDRESS + AUTHORIZE_WEBSITE_URL);
			}
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
	
	
	void getXMLFile(int xmlPage, int url) {
		// create an HTTP request to a protected resource
        String url_string;
		goodreads_url = url;
    	switch(goodreads_url) {
    	case GET_USER_INFO: 
    		url_string = URL_ADDRESS + USER_INFO_URL_PATH + myApp.userID + ".xml?key=" + DeveloperKeys.CONSUMER_KEY;
    		break;
    	case GET_SHELF:
    		url_string = URL_ADDRESS + SHELF_URL_PATH + myApp.userID + 
    		".xml?v=2&key=" + DeveloperKeys.CONSUMER_KEY +  "&per_page=" + ITEMS_TO_DOWNLOAD + 
    		"&shelf=" + myApp.userData.shelfToGet + "&page=" + xmlPage; 
    		break;
    	case GET_SHELVES:
    		url_string = URL_ADDRESS + SHELVES_URL_PATH + "&user_id=" + myApp.userID + 
    		"&page=" + xmlPage;
    		break;
    	case SEARCH_SHELVES:
    		url_string = URL_ADDRESS + SHELF_URL_PATH + myApp.userID + 
    		".xml?v=2&key=" + DeveloperKeys.CONSUMER_KEY +  "&per_page=" + ITEMS_TO_DOWNLOAD + 
    		"&page=" + xmlPage + "&search[query]=" + searchQuery;
    		break;
    	case GET_USER_ID:
    		url_string = URL_ADDRESS + GET_USER_ID_PATH;
    		break;
    	case GET_FRIEND_UPDATES:
    		url_string = URL_ADDRESS + UPDATES_URL_PATH;
    		break;
    	case GET_BOOKS_BY_ISBN:
    		url_string = URL_ADDRESS + BOOKS_ISBN_PATH + "?isbn=" + myApp.userData.isbnScan + 
    		"&key=" + DeveloperKeys.CONSUMER_KEY + "&format=xml";
    		break;
    	default: 
    		url_string = "";
    	}
        
        try {
        	HttpGet request = new HttpGet(url_string);
        	
    		// sign the request
    		consumer.sign(request);
        	
        	// Create background thread to download and render XML
    		myApp.getImageThreadRunning = false;
        	new MyAsyncTask().execute(request);
		    
        } catch(OAuthException e) {
        	Log.e(TAG, e.toString());
        	myApp.errMessage = e.toString();
        	throw new RuntimeException(e.toString());
        } 
	}
	
	void postBookToShelf(int _bookId, String _shelf) {
		 // Fire off a thread to do some work that we shouldn't do directly in the UI thread
        bookId = _bookId;
        shelf = _shelf;
		// Create dialog box
        Thread t = new Thread(null, doPostBook, "addBook");
        t.start();
	}
	
	 private Runnable doPostBook = new Runnable() {
	    	public void run() {
	    		postBook();
	    	}
	    };
	    
	 // Create runnable for posting
	    private final Runnable doPostBookUpdateGUI = new Runnable() {
	        public void run() {
	            postBookUpdateResults();
	        }
	    };
	    
	    private void postBook() {
	        try {
	        	HttpClient httpClient = new DefaultHttpClient();
	        	HttpPost post = new HttpPost(OAuthInterface.URL_ADDRESS + OAuthInterface.ADD_BOOK_PATH);
	        	Log.d(TAG, "Adding book id " + bookId + " to " + shelf);     	
	        	LinkedList<BasicNameValuePair> out = new LinkedList<BasicNameValuePair>();
	        	out.add(new BasicNameValuePair("book_id", Integer.toString(bookId)));
	        	out.add(new BasicNameValuePair("name", shelf));
	        	post.setEntity(new UrlEncodedFormEntity(out, HTTP.UTF_8));
	    		consumer.sign(post);  		
	    		HttpResponse response = httpClient.execute(post); 
	    		Log.d(TAG, response.getStatusLine().toString());
	    		if(response.getStatusLine().getStatusCode() == 201){
	    			myApp.goodreads_activity.mHandler.post(doPostBookUpdateGUI);
	    		} else {
	    			myApp.errMessage = response.getStatusLine().toString();
	    			throw new RuntimeException(response.getStatusLine().toString());
	    		}
	        } catch(OAuthException e) {
	        	Log.e(TAG, e.toString());
	        	myApp.errMessage = e.toString();
	        	throw new RuntimeException(e.toString());
	        } catch(Exception e) {
	        	Log.e(TAG, e.toString());
	        	myApp.errMessage = e.toString();
	        	throw new RuntimeException(e.toString());
	        }
	    }
	    
	    private void postBookUpdateResults() {
	    	myApp.goodreads_activity.toastMe(R.string.bookAddedToShelf);
	    	myApp.userData.books.get(0).shelves.add("to-read");
	    }
}

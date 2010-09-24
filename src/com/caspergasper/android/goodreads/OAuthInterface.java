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
	static final String BOOKPAGE_SEARCH = "m/book?search_type=books&search%5Bquery%5D=";
	static final String SHELF_URL_PATH = "review/list/";
	static final String SHELVES_URL_PATH ="shelf/list?format=xml";
	static final String UPDATES_URL_PATH = "updates/friends.xml";
	static final String	BOOKS_ISBN_PATH = "book/isbn";
	static final String ADD_BOOK_PATH = "shelf/add_to_shelf.xml";
	static final String ADD_UPDATE_PATH = "user_status.xml";
	static final String CALLBACK_URL = "goodreadsactivity://token";
	static final String OAUTH_VERIFIER = "oauth_token";
	static final String REVIEW_PATH = "review/";
	
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
	public static final int GET_SHELF_FOR_UPDATE = 7;
	
	int goodreads_url;
	private OAuthConsumer consumer;
	private OAuthProvider provider;
	private GoodReadsApp myApp;
	String searchQuery;
	private int bookId;
	private int reviewId;
	private String shelfTitle;
	private int page;
	private String body;
	private int rating = 0;
	
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
		if(myApp.threadLock) {
			return;
		}
		myApp.threadLock = true;
		// create an HTTP request to a protected resource
        String url_string;
		goodreads_url = url;
    	switch(goodreads_url) {
    	case GET_USER_INFO: 
    		url_string = URL_ADDRESS + USER_INFO_URL_PATH + myApp.userID + ".xml";
    		break;
    	case GET_SHELF:
    	case GET_SHELF_FOR_UPDATE:
    		url_string = URL_ADDRESS + SHELF_URL_PATH + myApp.userID + 
    		".xml?v=2&per_page=" + ITEMS_TO_DOWNLOAD + 
    		"&shelf=" + myApp.userData.shelfToGet + "&page=" + xmlPage; 
    		if(myApp.orderShelfBy != 0) {
    			url_string += myApp.orderShelfbyArray[myApp.orderShelfBy];
    		}
    		break;
    	case GET_SHELVES:
    		url_string = URL_ADDRESS + SHELVES_URL_PATH + "&user_id=" + myApp.userID + 
    		"&page=" + xmlPage;
    		break;
    	case SEARCH_SHELVES:
    		url_string = URL_ADDRESS + SHELF_URL_PATH + myApp.userID + 
    		".xml?v=2"  +  "&per_page=" + ITEMS_TO_DOWNLOAD + 
    		"&page=" + xmlPage + "&format=xml" + "&search%5Bquery%5D=" + searchQuery;
    		break;
    	case GET_USER_ID:
    		url_string = URL_ADDRESS + GET_USER_ID_PATH;
    		break;
    	case GET_FRIEND_UPDATES:
    		url_string = URL_ADDRESS + UPDATES_URL_PATH;
    		if(myApp.numberOfUpdates.compareToIgnoreCase("All") != 0) {
    			url_string += "?max_updates=" + myApp.numberOfUpdates;
    		}
    		break;
    	case GET_BOOKS_BY_ISBN:
    		url_string = URL_ADDRESS + BOOKS_ISBN_PATH + "?isbn=" + myApp.userData.isbnScan + 
    		"&format=xml";
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
        	Log.e(TAG, "OAuthInterface.getXMLFile: " + e.toString());
        	myApp.errMessage = e.toString();
        	throw new RuntimeException(e.toString());
        } 
	}
	
	void postBookToShelf(int _bookId, String _shelf) {
		 // Fire off a thread to do some work that we shouldn't do directly in the UI thread
        bookId = _bookId;
        shelfTitle = _shelf;
        Thread t = new Thread(null, doPostBook, "addBook");
        t.start();
	}
	
	void postUpdateToStatus(int _bookId, int _page, String _body) {
		//book_id, page, body
		bookId = _bookId;
		page = _page;
		body = _body;
		Thread t = new Thread(null, doPostUpdate, "addUpdate");
        t.start();
	}
	
	void postReview(int _reviewId, int _rating, String _body) {
		reviewId = _reviewId;
		body = _body;
		rating = _rating;
		Thread t = new Thread(null, doPostReview, "addReview");
        t.start();
	}
	
	private Runnable doPostReview = new Runnable() {
    	public void run() {
    		postReview();
    	}
    };
  
	
	private Runnable doPostBook = new Runnable() {
	    	public void run() {
	    		postBook();
	    	}
	    };
	    
	    
	 private Runnable doPostUpdate = new Runnable() {
	    	public void run() {
	    		postUpdate();
	    	}
	    };
	    
	 // Create runnable for posting
	 private final Runnable doPostBookUpdateGUI = new Runnable() {
	        public void run() {
	            postBookUpdateResults();
	        }
	    };
	  
	 private final Runnable doPostReviewUpdateGUI = new Runnable() {
		        public void run() {
		            postReviewUpdateResults();
		        }
		    };
	    private final Runnable doPostUpdateStatusGUI = new Runnable() {
	        public void run() {
	            postUpdateStatusResults();
	        }
	    };
	    
	    private void postUpdate() {     	
        	LinkedList<BasicNameValuePair> out = new LinkedList<BasicNameValuePair>();
        	if(bookId != 0) {
        		out.add(new BasicNameValuePair("user_status[book_id]", Integer.toString(bookId)));
        		if(page != 0) {
        			out.add(new BasicNameValuePair("user_status[page]", Integer.toString(page)));
        		}
        	}
        	if(body.length() > 0) {
        		out.add(new BasicNameValuePair("user_status[body]", body));
        	}
        	
        	postUpdateOrBook(out, OAuthInterface.ADD_UPDATE_PATH);
	    }
	    
	    
	    private void postReview() {
	    	LinkedList<BasicNameValuePair> out = new LinkedList<BasicNameValuePair>();;
	    	if(body.length() > 0) {
        		out.add(new BasicNameValuePair("review[review]", body));
        	}
	    	if(rating != 0) {
	    		out.add(new BasicNameValuePair("review[rating]", Integer.toString(rating)));
	    	}
	    	postUpdateOrBook(out, OAuthInterface.REVIEW_PATH + reviewId + ".xml");
	    }
	    
	    private void postBook() {
	    	Log.d(TAG, "Adding book id " + bookId + " to " + shelfTitle);     	
        	LinkedList<BasicNameValuePair> out = new LinkedList<BasicNameValuePair>();
        	out.add(new BasicNameValuePair("book_id", Integer.toString(bookId)));
        	out.add(new BasicNameValuePair("name", shelfTitle));
        	postUpdateOrBook(out, OAuthInterface.ADD_BOOK_PATH);
	    }
	    
	    private void postUpdateOrBook(LinkedList<BasicNameValuePair> postData, String URL) {
	        try {
	        	HttpClient httpClient = new DefaultHttpClient();
	        	HttpPost post = new HttpPost(OAuthInterface.URL_ADDRESS + URL);
	        	post.setEntity(new UrlEncodedFormEntity(postData, HTTP.UTF_8));
	    		consumer.sign(post);  		
	    		HttpResponse response = httpClient.execute(post); 
	    		Log.d(TAG, response.getStatusLine().toString());
	    		int responseCode = response.getStatusLine().getStatusCode();
	    		if(responseCode == 201 || responseCode == 200){
	    			if(URL == OAuthInterface.ADD_BOOK_PATH) { 
	    				((BooksActivity) myApp.goodreads_activity).mHandler.post(doPostBookUpdateGUI);
	    			} else if(URL == OAuthInterface.ADD_UPDATE_PATH) { 
	    				((UpdatesActivity) myApp.goodreads_activity).mHandler.post(doPostUpdateStatusGUI);
	    			} else {
	    				((BooksActivity) myApp.goodreads_activity).mHandler.post(doPostReviewUpdateGUI);
	    			}
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
	    	((BooksActivity) myApp.goodreads_activity).toastMe(R.string.bookAddedToShelf);
	    	// if currentShelf and new shelf are exclusive,
	    	// hide -- else add to shelf.
	    	Shelf currentShelf = null;
	    	Shelf newShelf = null;
	    	String shelfToGet;
	    	
	    	//  Always add new shelf to book
	    	BooksActivity.currentBook.shelves.add(shelfTitle);
	    	if(myApp.userData.shelfToGet == null){
	    		shelfToGet = "";
	    	} else {
	    		shelfToGet = myApp.userData.shelfToGet;
	    	}
	    	
	    	// Get the current and new shelf to check if they're exclusive
	    	for(Shelf tempShelf : myApp.userData.shelves) {
    			if(tempShelf.title.compareTo(shelfToGet) == 0) {
    				currentShelf = tempShelf;
    			} else if(tempShelf.title.compareTo(shelfTitle) == 0) {
    				newShelf = tempShelf;
    			}
	    	}
	    	
	    	if(newShelf != null && newShelf.exclusive) {
	    		if(currentShelf != null && currentShelf.exclusive) {
	    			((BooksActivity) myApp.goodreads_activity).removeCurrentBook();
	    			return;
	    		} else {
	    			// remove the old shelf from the list
	    			for(String stringShelf : BooksActivity.currentBook.shelves) {
	    				for(Shelf tempShelf : myApp.userData.shelves) {
	    					if(tempShelf.exclusive && stringShelf.compareTo(tempShelf.title) == 0) {
	    						BooksActivity.currentBook.shelves.remove(stringShelf);
	    						return;
	    					}
	        			}
	    			}	
	    		}
	    	}
	    	
	    }
	    
	    private void postUpdateStatusResults() {
	    	((UpdatesActivity) myApp.goodreads_activity).toastMe(R.string.statusUpdated);
	    }
	    
	    private void postReviewUpdateResults() {
	    	((BooksActivity) myApp.goodreads_activity).toastMe(R.string.review_updated);
	    }
}

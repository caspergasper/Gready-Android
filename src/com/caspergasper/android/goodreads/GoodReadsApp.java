package com.caspergasper.android.goodreads;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Application;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.DialogInterface.OnClickListener;
import android.content.SharedPreferences.Editor;
import android.content.res.Resources;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.view.MenuItem;
import android.view.Window;
import android.view.WindowManager;

public class GoodReadsApp extends Application {
	
	private static GoodReadsApp singleton;
	OAuthInterface oauth;
	String accessToken;
	String accessTokenSecret;
	String errMessage;
	int orderShelfBy = 0;
	String[] orderShelfbyArray;
	int userID;
	String numberOfUpdates;
	SharedPreferences settings;
	SharedPreferences global_settings;
	UserData userData;
	public static final String TAG = "Goodreads";
	public static final String GOODREADS_PREFS = "GoodreadsPrefs";
	static final String ACCESS_TOKEN = "access_token";
	static final String ACCESS_TOKEN_SECRET = "access_token_secret";
	static final String USER_ID = "user_id"; 
	static final String PREF_NUM_OF_UPDATES = "PREF_NUM_OF_UPDATES";
	static final String PREF_STARTUP_SHELF = "PREF_STARTUP_SHELF";
	Activity goodreads_activity;
	volatile boolean threadLock = false;
	volatile boolean getImageThreadRunning;
	static final String GOODREADS_IMG_URL = "http://photo.goodreads.com/";
	static final int GOODREADS_IMG_URL_LENGTH = GOODREADS_IMG_URL.length();
	MenuItem menuItem = null;
	static final String TO_READ = "to-read";
	
	public static GoodReadsApp getInstance() {
		return singleton;
	}
	
	@Override
	public final void onCreate() {
		super.onCreate();
		singleton = this;
		oauth = new OAuthInterface();
		userData = new UserData();
		settings = getSharedPreferences(GOODREADS_PREFS, 0);
		accessToken = settings.getString(ACCESS_TOKEN, null);
		accessTokenSecret = settings.getString(ACCESS_TOKEN_SECRET, null);
		userID = settings.getInt(USER_ID, 0);
		
		global_settings = PreferenceManager.getDefaultSharedPreferences(this);
		numberOfUpdates = global_settings.getString(PREF_NUM_OF_UPDATES, "All");
		userData.shelfToGet = global_settings.getString(PREF_STARTUP_SHELF, "Updates");
		Resources myResources = getResources();
		orderShelfbyArray = myResources.getStringArray(R.array.sort_by_values);
		oauth.updateTokens();
	}
	
	void addTokenToPrefs(String param, String token) {
		Editor e = settings.edit();
		e.putString(param, token);
		e.commit();
	}
	
	void addTokenToPrefs(String param, int token) {
		Editor e = settings.edit();
		e.putInt(param, token);
		e.commit();
	}
	
	void deleteAllPrefs() {
		Editor e = settings.edit();
		e.remove(ACCESS_TOKEN);
		e.remove(ACCESS_TOKEN_SECRET);
		e.remove(USER_ID);
		e.commit();
		accessToken = null;
		accessTokenSecret = null;
		userID = 0;
	}
	
	
	/**
	 * Converts a UPC to ISBN format
	 *
	 * @param isbn the input UPC 
	 * @return ISBN format
	 */
	public static String ConvertUPCtoISBN(String isbn){ 
		if (isbn.length() == 13 && isbn.indexOf("978") == 0)
		{
		  isbn = isbn.substring(3,12);
		  int xsum = 0;
	
		  for (int i = 0; i < 9; i++)
		  {
		      xsum += (10 - i) * Character.getNumericValue(isbn.charAt(i));
		  }
	
		  xsum %= 11;
		  xsum = 11 - xsum;
	
		  String x_val = String.valueOf(xsum);
	
		  switch (xsum)
		  {
		      case 10: x_val = "X"; break;
		      case 11: x_val = "0"; break;
		  }
	
		  isbn += x_val;
		}
		return isbn;
	}

	
	void gotoWebURL(String path, Activity activity) {
		Uri uri = Uri.parse(path);
		Intent intent = new Intent(Intent.ACTION_VIEW, uri);
		activity.startActivity(intent);
	}
	
	void showErrorDialog(Activity activity) {
		AlertDialog.Builder ad = new AlertDialog.Builder(activity);
		ad.setTitle("ERROR!");
		ad.setMessage(errMessage);
		ad.setPositiveButton("OK", new OnClickListener() {
			public void onClick(DialogInterface dialog, int arg1) {
				// do nothing
			}
		});
		ad.show();
	}
	
	void showGetAuthorizationDialog(final Activity activity) {
		AlertDialog.Builder ad = new AlertDialog.Builder(activity);
		ad.setTitle("ERROR!");
		ad.setMessage(R.string.getAuthorization);
		ad.setPositiveButton("OK", new OnClickListener() {
			public void onClick(DialogInterface dialog, int arg1) {
				deleteAllPrefs();
				activity.startActivity(new Intent(activity, OAuthCallbackActivity.class));
			}
		});
		ad.setNegativeButton("No thanks", null);
		ad.show();
	}
	
	Dialog createDialogBox(Activity activity, int layoutId, boolean hideTitle) {
		Dialog d = new Dialog(activity);
		Window window = d.getWindow();
		window.setFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND, 
				WindowManager.LayoutParams.FLAG_BLUR_BEHIND);
		if(hideTitle) {
			d.requestWindowFeature(Window.FEATURE_NO_TITLE);
		}
		d.setContentView(layoutId);
		return d;
	}
}


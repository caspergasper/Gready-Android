package com.caspergasper.android.goodreads;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Application;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.DialogInterface.OnClickListener;
import android.content.SharedPreferences.Editor;
import android.net.Uri;

public class GoodReadsApp extends Application {
	
	private static GoodReadsApp singleton;
	OAuthInterface oauth;
	String accessToken;
	String accessTokenSecret;
	String errMessage;
	int userID;
	SharedPreferences settings;
	UserData userData;
	public static final String TAG = "Goodreads";
	public static final String GOODREADS_PREFS = "GoodreadsPrefs";
	static final String ACCESS_TOKEN = "access_token";
	static final String ACCESS_TOKEN_SECRET = "access_token_secret";
	static final String USER_ID = "user_id"; 
	Activity goodreads_activity;
	volatile boolean threadLock = false;
	volatile boolean getImageThreadRunning;
	
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
				activity.startActivity(new Intent(activity, SettingsActivity.class));
			}
		});
		ad.setNegativeButton("No thanks", null);
		ad.show();
	}
}


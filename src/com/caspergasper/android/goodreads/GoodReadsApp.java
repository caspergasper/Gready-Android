package com.caspergasper.android.goodreads;

import java.util.List;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Application;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.DialogInterface.OnClickListener;
import android.content.SharedPreferences.Editor;
import android.content.res.Resources;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.RadioGroup.OnCheckedChangeListener;

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
	volatile boolean gettingShelves = false;
	static final String GOODREADS_IMG_URL = "http://photo.goodreads.com/";
	static final int GOODREADS_IMG_URL_LENGTH = GOODREADS_IMG_URL.length();
	MenuItem menuItem = null;
	static final String TO_READ = "to-read";
	static final int SUBMENU_GROUPID = 1;
	int xmlPage = 1;
	SubMenu sub;
	ProgressDialog progressDialog;
	private static final int MAX_UPDATE_SIZE = 420;
	Dialog updateDialog = null;
	static final String CURRENTLY_READING = "currently-reading";
	
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
	
	void createShelvesMenu(List<Shelf> tempShelves) {
    	int shelf_length = tempShelves.size();
    	int totalBooks = 0;
    	int currentShelfBooks = 0;
    	int i;
    	sub.removeGroup(SUBMENU_GROUPID);  // Delete all first
    	sub.add(SUBMENU_GROUPID, 0, Menu.NONE,  
       	     R.string.refresh_shelf_list);
    	for(i=0; i<shelf_length && tempShelves.get(i) != null; i++) {
    		currentShelfBooks = tempShelves.get(i).total;
    		sub.add(SUBMENU_GROUPID, i+1, Menu.NONE,  
    	     "(" + currentShelfBooks + ") " + tempShelves.get(i).title);
    		if(tempShelves.get(i).exclusive) {
    			totalBooks += currentShelfBooks;
    		}
    	}
    	sub.add(SUBMENU_GROUPID, i+1, Menu.NONE,  
       	     "(" + totalBooks + ") All books");
    	gettingShelves = false;
    	if(progressDialog != null) {
    		progressDialog.dismiss();
    	}
	}
	
	boolean handleBookshelfSelection(Context context, int itemId) {
		if(itemId == 0) {
			gettingShelves = true;
			progressDialog = ProgressDialog.show(context, "Getting shelves", "Please be patient...");
			// Refresh shelf list
			userData.shelves.clear();
			xmlPage = 1;
			oauth.getXMLFile(xmlPage, OAuthInterface.GET_SHELVES);
			return false;
		} else if(itemId - 1 == userData.shelves.size()) {
			userData.shelfToGet = "all";
		} else {
			userData.shelfToGet = userData.shelves.get(itemId - 1).title;
		}
		return true;
	}
	
	void showUpdateDialog(final Activity activity) {
		final String CHARS_LEFT = " left";
		updateDialog = createDialogBox(activity, R.layout.updatestatus_dialog, true);
		final Button b = (Button) updateDialog.findViewById(R.id.updatebutton);
		final RadioGroup group = (RadioGroup) updateDialog.findViewById(R.id.RadioGroup);
		final EditText et = (EditText) updateDialog.findViewById(R.id.statusbox);
		final TextView charsLeft = (TextView) updateDialog.findViewById(R.id.char_count_label);
		final EditText pageCountEdit = (EditText) updateDialog.findViewById(R.id.pagestatusbox);
		
		// Show characters remaining
		et.addTextChangedListener(new TextWatcher() {
			@Override
			public void afterTextChanged(Editable arg0) {
				charsLeft.setText(Integer.toString(MAX_UPDATE_SIZE - arg0.length()) + CHARS_LEFT);
			}
			@Override
			public void beforeTextChanged(CharSequence arg0, int arg1,
					int arg2, int arg3) {				}
			@Override
			public void onTextChanged(CharSequence arg0, int arg1, int arg2,
					int arg3) {			}
		});
			
		b.setOnClickListener(new View.OnClickListener() {
			public void onClick(View view) {
				int pageNum = 0;
				int bookId = 0;
				
				String text = et.getText().toString().trim(); 
				int textLength = text.length(); 
				if(textLength > MAX_UPDATE_SIZE) {
					if(activity instanceof UpdatesActivity) {
						((UpdatesActivity)activity).toastMe(R.string.update_too_long);
					} else {
						((BooksActivity)activity).toastMe(R.string.update_too_long);
					}
					return;
				}
				
				if(pageCountEdit.getText().toString().length() > 0) {
					pageNum = Integer.parseInt(pageCountEdit.getText().toString());
				}
				
				if(pageNum != 0 || textLength > 0) {
					int checkedId = group.getCheckedRadioButtonId();
					if(checkedId != R.id.RadioButtonGeneralUpdate) {
						bookId = checkedId;
					}
					oauth.postUpdateToStatus(bookId, pageNum, text);
				}
				updateDialog.hide();
			}
		});
		
		
		
		group.setOnCheckedChangeListener(new OnCheckedChangeListener() {	
			@Override
			public void onCheckedChanged(RadioGroup arg0, int arg1) {
				if(arg1 == R.id.RadioButtonGeneralUpdate) {
					updateDialog.findViewById(R.id.page_num_label).setVisibility(View.INVISIBLE);
					updateDialog.findViewById(R.id.pagestatusbox).setVisibility(View.INVISIBLE);
				} else {
					updateDialog.findViewById(R.id.page_num_label).setVisibility(View.VISIBLE);
					updateDialog.findViewById(R.id.pagestatusbox).setVisibility(View.VISIBLE);
				}
			}
		});
		updateDialog.show();
	}
	
}


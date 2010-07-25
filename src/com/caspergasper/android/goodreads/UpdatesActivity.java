package com.caspergasper.android.goodreads;


import static com.caspergasper.android.goodreads.GoodReadsApp.TAG;

import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnClickListener;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemLongClickListener;


public class UpdatesActivity extends Activity implements OnItemLongClickListener {
	
	private GoodReadsApp myApp;
	private static final int SUBMENU_GROUPID = 1;
	
	private int xmlPage = 1;
	private ListView updatesListView;
	private static final String M_USER = "m/user/";
	private static final String REVIEWS = "/reviews";
	
	@Override
	public void onResume() {
		myApp.goodreads_activity = this;
		try {
			super.onResume();
			if (myApp.accessToken == null  || myApp.accessTokenSecret == null) {
	        	Log.d(TAG, "Missing accessTokens, retrieving...");
	        	startActivity(new Intent(UpdatesActivity.this, SettingsActivity.class));
	        	return;
	        } 
			if(myApp.userID == 0  && !myApp.threadLock) {
				myApp.oauth.getXMLFile(xmlPage, OAuthInterface.GET_USER_ID);
				return;     
			} else {
				if(myApp.userData.updates.size() == 0 && !myApp.threadLock) {
					// Got valid tokens and a userid, let's go get some data...
					Log.d(TAG, "Getting updates now...");
					xmlPage = 1;
					myApp.oauth.getXMLFile(xmlPage, OAuthInterface.GET_FRIEND_UPDATES);
				} 
			}
		} catch(Exception e) {
			myApp.errMessage = "GoodreadsActivity onResume " + e.toString();
			myApp.showErrorDialog(this);
		}
	}
	
	@Override
	public void onPause() {
		super.onPause();
	}
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        try{
	    	super.onCreate(savedInstanceState);
	        setContentView(R.layout.updates);
	        
	        myApp = GoodReadsApp.getInstance();
	        myApp.goodreads_activity = this;
	        updatesListView = (ListView) findViewById(R.id.updates_listview);
	        newQuery();  // For when activity has been cleared from memory but app hasn't
    	} catch(Exception e) {
			myApp.errMessage = "UpdatesActivity onCreate " + e.toString();
			myApp.showErrorDialog(this);
		}
    }
    
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
    	super.onConfigurationChanged(newConfig);
    	// I'm supposed to re-initialize views here when screen is rotated, 
    	// but so far I haven't found I need to.
    }
	
	@Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.updates_menu, menu);
        SubMenu sub = menu.addSubMenu(0, 0, Menu.NONE, R.string.bookshelves_label);
    	sub.setHeaderIcon(android.R.drawable.ic_menu_view);
    	sub.setIcon(android.R.drawable.ic_menu_view);
    	List<Shelf> tempShelves = myApp.userData.shelves;
        if(tempShelves.size() == 0 || myApp.userData.endShelf < myApp.userData.totalShelves) {
        	toastMe(R.string.build_menu);
			return false;
        } else {
        	int shelf_length = tempShelves.size();
        	int totalBooks = 0;
        	int currentShelfBooks = 0;
        	int i;
        	for(i=0; i<shelf_length && tempShelves.get(i) != null; i++) {
        		currentShelfBooks = tempShelves.get(i).total;
        		sub.add(SUBMENU_GROUPID, i, Menu.NONE,  
        	     "(" + currentShelfBooks + ") " + tempShelves.get(i).title);
        		totalBooks += currentShelfBooks;
        	}
        	sub.add(SUBMENU_GROUPID, i, Menu.NONE,  
           	     "(" + totalBooks + ") All books");
        }
        return true;
    }
		
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		super.onOptionsItemSelected(item);
		if(myApp.threadLock) {
			return false;
		}
		if(item.getGroupId() == SUBMENU_GROUPID) {
			if(item.getItemId() == myApp.userData.shelves.size()) {
				myApp.userData.shelfToGet = "all";
			} else {
				myApp.userData.shelfToGet = myApp.userData.shelves.get(item.getItemId()).title;
			}
			startActivity(new Intent(UpdatesActivity.this, BooksActivity.class));
			return true;
		} else if(item.getItemId() == R.id.updates) {
				showUpdateMessage(R.string.getUpdates);
				myApp.oauth.getXMLFile(xmlPage, OAuthInterface.GET_FRIEND_UPDATES);
				return true;
		}
		else if(item.getItemId() == R.id.scanbook) {
			IntentIntegrator.initiateScan(UpdatesActivity.this,IntentIntegrator.DEFAULT_TITLE,IntentIntegrator.DEFAULT_MESSAGE,IntentIntegrator.DEFAULT_YES,IntentIntegrator.DEFAULT_NO,IntentIntegrator.PRODUCT_CODE_TYPES);
			return true;
		}
 		else if(item.getItemId() == R.id.search) {
			showSearchDialog();
			return true;
		}
			return false;	
	}
	
	@Override
	public boolean onItemLongClick(AdapterView<?> _av, View _v, int _index, long arg3) {
		final Update u = myApp.userData.updates.get(_index);
		if(u.updateLink != null) {
			String path = OAuthInterface.URL_ADDRESS +
				M_USER + u.id + REVIEWS + u.updateLink;
			Log.d(TAG, path);
			myApp.gotoWebURL(path, myApp.goodreads_activity);
		} else {
			toastMe(R.string.no_update_page);
		}
		return true;
	}
	
	
	void showUpdateMessage(int resource) {
		TextView textView = (TextView) findViewById(R.id.status_label);
		textView.setText(resource);
		textView.setVisibility(View.VISIBLE);
	}
	
	void showSearchDialog() {
		final Dialog d = new Dialog(UpdatesActivity.this);
		Window window = d.getWindow();
		window.setFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND, 
				WindowManager.LayoutParams.FLAG_BLUR_BEHIND);
		
		d.setContentView(R.layout.booksearch_dialog);
		d.setTitle(R.string.searchTitle);
		final Button b = (Button) d.findViewById(R.id.searchbutton);
		b.setOnClickListener(new View.OnClickListener() {
			public void onClick(View view) {
				// do nothing
				final EditText et = (EditText) d.findViewById(R.id.searchbox);
				String text = et.getText().toString().trim(); 
				if(text == null || text.length() < 1) {
					return;
				}
				RadioGroup radioGroup = (RadioGroup) d.findViewById(R.id.RadioGroup);
				if(radioGroup.getCheckedRadioButtonId() == R.id.RadioButtonSearchSite) {
				myApp.gotoWebURL(OAuthInterface.URL_ADDRESS +
						OAuthInterface.BOOKPAGE_SEARCH + Uri.encode(text), myApp.goodreads_activity);
				} else if(radioGroup.getCheckedRadioButtonId() == R.id.RadioButtonSearchShelves) {
					myApp.oauth.searchQuery = Uri.encode(text);
					myApp.userData.shelfToGet = d.getContext().getString(R.string.searchResults); 
					newQuery();
					showUpdateMessage(R.string.getSearch);
					myApp.oauth.getXMLFile(xmlPage, OAuthInterface.SEARCH_SHELVES);
				}
				d.hide();
			}
		});
		d.show();
	
	}
	
	private void newQuery() {
		xmlPage = 1;
	}
	
    void updateMainScreenForUser(int result) {
    	Log.d(TAG, "updatesMainScreenForUser");
    	TextView tv;
    	UserData ud = myApp.userData;
    	
    	if(result == 1) {
			myApp.showErrorDialog(this);
			return;
		} else if(result == 2) {
			myApp.showGetAuthorizationDialog(this);
			return;
		}
    	switch (myApp.oauth.goodreads_url) {
		case OAuthInterface.GET_FRIEND_UPDATES:
			UpdateAdapter updateAdapter;
			if(ud.updates.size() == 0) {
				updatesListView.setOnItemClickListener(null);
				updatesListView.setOnItemLongClickListener(this);
				updatesListView.setOnScrollListener(null);
			    updateAdapter = new UpdateAdapter(this, R.layout.updateitem,
						ud.updates);
			    updatesListView.setAdapter(updateAdapter);
			} else {
				updateAdapter = (UpdateAdapter) updatesListView.getAdapter();
				updateAdapter.clear();
			}
			addUpdatesToListView(updateAdapter);
			updatesListView.setVisibility(View.VISIBLE);
			findViewById(R.id.status_label).setVisibility(View.INVISIBLE);
			tv = (TextView) findViewById(R.id.updates_label);
			tv.setText(R.string.updates_label);
			if(myApp.userData.shelves.size() == 0) {
				xmlPage = 1;
				myApp.oauth.getXMLFile(xmlPage, OAuthInterface.GET_SHELVES);
			}
			ud.books.clear();
		break;
		case OAuthInterface.GET_USER_ID:
			Log.d(TAG, "Getting friend updates for first time");
			ud.updates.clear();
			myApp.oauth.getXMLFile(xmlPage, OAuthInterface.GET_FRIEND_UPDATES);
		break;
    	case OAuthInterface.GET_SHELVES:
    		// We need to cater for users with > 100 bookshelves like Cait :-)
    		if(ud.endShelf < ud.totalShelves) {
    			Log.d(TAG, "Getting extra shelf for some user that reads too much...");
    			myApp.oauth.getXMLFile(++xmlPage, OAuthInterface.GET_SHELVES);
    		}
    	break;
    	}
    }

	void toastMe(int msgid) {
		Toast toast = Toast.makeText(getApplicationContext(), msgid, Toast.LENGTH_SHORT);
		toast.show();
	}
	
	private void addUpdatesToListView(UpdateAdapter updateAdapter) {
		for(Update u : myApp.userData.tempUpdates) {
			updateAdapter.add(u);
		}
		myApp.userData.tempUpdates.clear();	
	}
	
}
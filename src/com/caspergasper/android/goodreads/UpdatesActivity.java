package com.caspergasper.android.goodreads;


import static com.caspergasper.android.goodreads.GoodReadsApp.TAG;

import java.net.URL;
import java.util.List;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
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
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.RadioGroup.OnCheckedChangeListener;


public class UpdatesActivity extends Activity implements OnItemClickListener, OnItemLongClickListener {
	
	private GoodReadsApp myApp;
	private static final int SUBMENU_GROUPID = 1;
	
	private int xmlPage = 1;
	private ListView updatesListView;
	private static final String M_USER = "m/user/";
	private static final String REVIEWS = "/reviews";
	private Dialog updateDialog = null;
	private static final String CURRENTLY_READING = "currently-reading";
	// Need handler for callbacks to the UI thread
    final Handler mHandler = new Handler();
    UpdateAdapter updateAdapter;
	
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
		else if(item.getItemId() == R.id.update_status) {
			showUpdateDialog();
			myApp.userData.shelfToGet = CURRENTLY_READING;
			xmlPage = 1;
			myApp.oauth.getXMLFile(xmlPage, OAuthInterface.GET_SHELF_FOR_UPDATE);
			return true;
		}
 		
			return false;	
	}
	
	
	@Override
	public void onItemClick(AdapterView<?> _av, View _v, int _index, long arg3) {
		final Update u = myApp.userData.updates.get(_index);
		showUpdateDetail(u);
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
	
	
	private final void showUpdateDetail(Update update) {
		TextView textview;
		Dialog d = new Dialog(UpdatesActivity.this);
		Window window = d.getWindow();
		window.setFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND, 
				WindowManager.LayoutParams.FLAG_BLUR_BEHIND);
		d.requestWindowFeature(Window.FEATURE_NO_TITLE);
		d.setContentView(R.layout.update_dialog);
		
		if(update.bitmap != null) {
			ImageView imgView = (ImageView) d.findViewById(R.id.updateDialogImage);
			imgView.setImageBitmap(update.bitmap);
		}
		textview = (TextView) d.findViewById(R.id.title);
		textview.setText(update.getUpdateText());
		textview = (TextView) d.findViewById(R.id.update);
		textview.setText(update.getBody());
		d.show();	
	}
	
	void showUpdateDialog() {
		updateDialog = new Dialog(UpdatesActivity.this);
		Window window = updateDialog.getWindow();
		window.setFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND, 
				WindowManager.LayoutParams.FLAG_BLUR_BEHIND);
		updateDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
		updateDialog.setContentView(R.layout.updatestatus_dialog);
		updateDialog.setTitle(R.string.update_status);
		final Button b = (Button) updateDialog.findViewById(R.id.updatebutton);
		final RadioGroup group = (RadioGroup) updateDialog.findViewById(R.id.RadioGroup);
		
		b.setOnClickListener(new View.OnClickListener() {
			public void onClick(View view) {
				int pageNum = 0;
				int bookId = 0;
				EditText et = (EditText) updateDialog.findViewById(R.id.statusbox);
				String text = et.getText().toString().trim(); 
				et = (EditText) updateDialog.findViewById(R.id.pagestatusbox);
				if(et.getText().toString().length() > 0) {
					pageNum = Integer.parseInt(et.getText().toString());
				}
				
				if(pageNum != 0 || text.length() > 0) {
					int checkedId = group.getCheckedRadioButtonId();
					if(checkedId != R.id.RadioButtonGeneralUpdate) {
						bookId = checkedId;
					}
					myApp.oauth.postUpdateToStatus(bookId, pageNum, text);
				}
				updateDialog.hide();
			}
		});
		
		group.setOnCheckedChangeListener(new OnCheckedChangeListener() {	
			@Override
			public void onCheckedChanged(RadioGroup arg0, int arg1) {
				Log.d(TAG, Integer.toString(arg1));
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
	
	private void updateDialogRadioGroup() {
		// Callback from getting the currently-reading shelf.
		// book ids are stored as the radiobutton ids.
		RadioGroup group = (RadioGroup) updateDialog.findViewById(R.id.RadioGroup);
		
		for(Book b : myApp.userData.tempBooks){
			RadioButton button = new RadioButton(this);
			button.setId(b.id);
			button.setText(b.title);
			group.addView(button);	
		}
		myApp.userData.tempBooks.clear();
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
			if(ud.updates.size() == 0) {
				updatesListView.setOnItemClickListener(this);
				updatesListView.setOnItemLongClickListener(this);
			    updateAdapter = new UpdateAdapter(this, R.layout.updateitem,
						ud.updates);
			    updatesListView.setAdapter(updateAdapter);
			} else {
				updateAdapter = (UpdateAdapter) updatesListView.getAdapter();
				updateAdapter.clear();
			}
			addUpdatesToListView();
			updatesListView.setVisibility(View.VISIBLE);
			findViewById(R.id.status_label).setVisibility(View.INVISIBLE);
			tv = (TextView) findViewById(R.id.updates_label);
			tv.setText(R.string.updates_label);
			if(myApp.userData.shelves.size() == 0) {
				xmlPage = 1;
				myApp.oauth.getXMLFile(xmlPage, OAuthInterface.GET_SHELVES);
			} else {
				getImages();
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
    		} else {
    			getImages();
    		}
    	break;
    	case OAuthInterface.GET_SHELF_FOR_UPDATE:
    		updateDialogRadioGroup();
    	break;
    	}
    }

	void toastMe(int msgid) {
		Toast toast = Toast.makeText(getApplicationContext(), msgid, Toast.LENGTH_SHORT);
		toast.show();
	}
	
	private void addUpdatesToListView() {
		for(Update u : myApp.userData.tempUpdates) {
			updateAdapter.add(u);
		}
		myApp.userData.tempUpdates.clear();	
	}
	
	
    private void getImages() {
        // Fire off a thread to do some work that we shouldn't do directly in the UI thread
        Thread t = new Thread(null, doBackgroundThreadProcessing, "Background");
        myApp.getImageThreadRunning = true;
        t.start();
    }
    
	// Create runnable for posting
    private final Runnable doUpdateGUI = new Runnable() {
        public void run() {
            updateResultsInUi();
        }
    };

    
    private Runnable doBackgroundThreadProcessing = new Runnable() {
    	public void run() {
    		backgroundThreadProcessing();
    	}
    };
    
    private void backgroundThreadProcessing() {
    	Update u;
    	int size = myApp.userData.updates.size();
    	for(int i = 0; i < size; i++) {
    		if(!myApp.getImageThreadRunning) {
    			Log.d(TAG, "stopping getImage thread.");
    			break;
    		}
			u = updateAdapter.getItem(i);
			try {
				if(u.imgUrl == null) {
					continue;
				}
				if(u.bitmap == null) {
					URL newurl = new URL(BooksActivity.GOODREADS_IMG_URL + u.imgUrl); 
					Log.d(TAG, "Getting " + BooksActivity.GOODREADS_IMG_URL + u.imgUrl);
					u.bitmap = BitmapFactory.decodeStream(newurl.openConnection().getInputStream());
					// Add this image to any further updates by same user
					Update temp;
					for(int j = i+1; j < size; j++) {
						temp = updateAdapter.getItem(j);
						if(temp.id == u.id) {
							temp.bitmap = u.bitmap;
						}
					}
					
				}
				u.imgUrl = null;
				mHandler.post(doUpdateGUI);
			} catch (Exception e) {
				myApp.errMessage = e.toString() + " " + e.getStackTrace().toString();
				myApp.showErrorDialog(this);
			}
		} // for
    		
    }
    
    private void updateResultsInUi() {
        // Back in the UI thread -- update UI elements
    	updateAdapter.notifyDataSetChanged();
    }

	
}
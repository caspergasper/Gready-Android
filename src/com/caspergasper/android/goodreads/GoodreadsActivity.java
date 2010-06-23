package com.caspergasper.android.goodreads;


import static com.caspergasper.android.goodreads.GoodReadsApp.TAG;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnClickListener;
import android.net.Uri;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;


public class GoodreadsActivity extends Activity {
	
	private GoodReadsApp myApp;
	static final int SUBMENU_GROUPID = 1;
	private static final int TOTAL_BOOKS_TO_DOWNLOAD = 100;  // arbitrary limit, need to fix properly
	
	public void onResume() {
//		Log.d(TAG, "Entering onResume() in GoodreadsActivity");
		super.onResume();
		if (myApp.accessToken == null  || myApp.accessTokenSecret == null) {
        	Log.d(TAG, "Missing accessTokens, retrieving...");
        	startActivity(new Intent(GoodreadsActivity.this, SettingsActivity.class));
        	return;
        } 
		if(myApp.userID == 0) {
			myApp.oauth.goodreads_url = OAuth_interface.GET_USER_ID; 
			myApp.oauth.getXMLFile();
			return;     
		} else {
			if(myApp.userData.updates.size() == 0 && myApp.userData.books.size() == 0) {
				// Got valid tokens and a userid, let's go get some data...
				Log.d(TAG, "Getting updates now...");
				myApp.oauth.goodreads_url = OAuth_interface.GET_FRIEND_UPDATES;
				myApp.oauth.getXMLFile();
			} else {
				updateMainScreenForUser(0);
			}
		}

	}
	
	public void onPause() {
		super.onPause();
	}
	

	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        myApp = GoodReadsApp.getInstance();
        myApp.goodreads_activity = this;

    }    
	
	@Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.startmenu, menu);
//        MenuItem item = menu.findItem(R.id.identifyuser);
//        item.setIntent(new Intent(GoodreadsActivity.this, SettingsActivity.class));
//        MenuItem item = menu.findItem(R.id.updates);
//        item.setIntent(new Intent(GoodreadsActivity.this, About.class));
        
        SubMenu sub = menu.addSubMenu(0, 0, Menu.NONE, R.string.bookshelves_label);
        sub.setHeaderIcon(android.R.drawable.ic_menu_view);
        sub.setIcon(android.R.drawable.ic_menu_view);
        
        if(myApp.userData.shelves.size() == 0) {
        	toastMe(R.string.build_menu);
//			myApp.oauth.goodreads_url = OAuth_interface.GET_SHELVES;
//			myApp.oauth.getXMLFile();	
			return false;
        } else {
        	Log.d(TAG, "Populating shelves");
        	int shelf_length = myApp.userData.shelves.size();
        	for(int i=0; i<shelf_length && myApp.userData.shelves.get(i) != null; i++) {
        		sub.add(SUBMENU_GROUPID, i, Menu.NONE,  
        	    " (" + myApp.userData.shelves.get(i).total + ") " + myApp.userData.shelves.get(i).title);
        	}
        }
        return true;
    }
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		super.onOptionsItemSelected(item);
		
		if(item.getGroupId() == SUBMENU_GROUPID) {
			myApp.userData.books.clear();
			myApp.userData.shelf_to_get = myApp.userData.shelves.get(item.getItemId()).title;
			findViewById(R.id.status_label).setVisibility(View.VISIBLE);
			myApp.oauth.goodreads_url = OAuth_interface.GET_SHELF; 
			myApp.userData.bookPage = 0;
			myApp.oauth.getXMLFile();
			return true;
		} else {
			Log.d(TAG, "menuitem:" + item.getItemId());
			if(item.getItemId() == R.id.updates) {
				findViewById(R.id.status_label).setVisibility(View.VISIBLE);
				myApp.userData.updates.clear();
				myApp.oauth.goodreads_url = OAuth_interface.GET_FRIEND_UPDATES;
				myApp.oauth.getXMLFile();
				return true;
			}
		}
	
			return false;	
	}
	
    void updateMainScreenForUser(int result) {
    	Log.d(TAG, "updateMainScreenForUser");
    	TextView tv;
    	ListView lv;
    	
    	if(result != 0) {
			showErrorDialog();
			return;
		}
    	switch (myApp.oauth.goodreads_url) {
		case OAuth_interface.GET_FRIEND_UPDATES:
			if(myApp.userData.updates.size() > 0) {
			    lv = (ListView) findViewById(R.id.updates_listview);
				lv.setOnItemClickListener(null);
				lv.setOnItemLongClickListener(null);
			    ArrayAdapter<Update> adapter = new 
				ArrayAdapter<Update> (this, R.layout.updateitem,
						myApp.userData.updates);
				lv.setAdapter(adapter);
				lv.setVisibility(View.VISIBLE);
				findViewById(R.id.status_label).setVisibility(View.INVISIBLE);
				tv = (TextView) findViewById(R.id.updates_label);
				tv.setText(R.string.updates_label);
				if(myApp.userData.books.size() == 0) {
					myApp.oauth.goodreads_url = OAuth_interface.GET_SHELVES;
					myApp.oauth.getXMLFile();
				}
			}
		break;
		case OAuth_interface.GET_SHELF:
			if(myApp.userData.books.size() > 0) {
				lv = (ListView) findViewById(R.id.updates_listview);
				ShelfAdapter adapter = new 
				ShelfAdapter(this, R.layout.updateitem, myApp.userData.books);
				lv.setAdapter(adapter);	
				tv = (TextView) findViewById(R.id.updates_label);
				tv.setText(myApp.userData.shelf_to_get);
				lv.setVisibility(View.VISIBLE);
				findViewById(R.id.status_label).setVisibility(View.INVISIBLE);
				lv.setOnItemClickListener(new OnItemClickListener() {
					//@Override   // msr comment
					public void onItemClick(AdapterView<?> _av, View _v, int _index, long arg3) {	
						setContentView(R.layout.book_view);
						TextView Title = (TextView) findViewById(R.id.Title);
						Title.setText(myApp.userData.books.get(_index).title);
						TextView Author = (TextView) findViewById(R.id.Author);
						Author.setText(myApp.userData.books.get(_index).author);
						TextView avgrating = (TextView) findViewById(R.id.avgrating);
						avgrating.setText("Average rating: " + myApp.userData.books.get(_index).average_rating);
						TextView Description = (TextView) findViewById(R.id.Description);
						Description.setText(Html.fromHtml(myApp.userData.books.get(_index).description));
						RatingBar UserRating = (RatingBar) findViewById(R.id.UserRating);
						// will need to get actual user ratings and fill stars 
						UserRating.setRating(0);
						
//						RadioButton celsiusButton = (RadioButton) findViewById(R.id.RadioButton01);
						
						
						/* Dialog d = new Dialog(GoodreadsActivity.this);
						Window window = d.getWindow();
						window.setFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND, 
								WindowManager.LayoutParams.FLAG_BLUR_BEHIND);
						
						d.setContentView(R.layout.book_dialog);
						d.setTitle(myApp.userData.books.get(_index).title);
						TextView textview = (TextView) d.findViewById(R.id.author);
						textview.setText(myApp.userData.books.get(_index).author);
						textview = (TextView) d.findViewById(R.id.avg_rating);
						textview.setText("Average rating: " + 
								myApp.userData.books.get(_index).average_rating);
						textview = (TextView) d.findViewById(R.id.description);
						textview.setText(
								Html.fromHtml(myApp.userData.books.get(_index).description));
						d.show();
						*/
					}
				});
				
				lv.setOnItemLongClickListener(new OnItemLongClickListener() {
					//@Override   // msr comment
					public boolean onItemLongClick(AdapterView<?> _av, View _v, int _index, long arg3) {
						if(myApp.userData.books.get(_index).bookLink != null) {
							Uri uri = Uri.parse(OAuth_interface.URL_ADDRESS +
									OAuth_interface.BOOKPAGE_PATH + myApp.userData.books.get(_index).bookLink);
				    		Intent intent = new Intent(Intent.ACTION_VIEW, uri);
				    		startActivity(intent);
						} else {
							toastMe(R.string.no_book_page);
						}
						return true;
					}
				});
				
				if(myApp.userData.endBook < myApp.userData.totalBooks &&
						myApp.userData.endBook < TOTAL_BOOKS_TO_DOWNLOAD) {
	        		// Need to query extra pages in the background
					Log.d(TAG, "Getting extra page of books...");
					myApp.oauth.getXMLFile();
//					lv.setVisibility(View.GONE);  // Fix for updating user interface in background thread.
	        	}
			}
		break;
		case OAuth_interface.GET_USER_ID:
			// Let's get the updates
			Log.d(TAG, "Getting friend updates for first time");
			myApp.oauth.goodreads_url = OAuth_interface.GET_FRIEND_UPDATES;
			myApp.oauth.getXMLFile();
		break;
		}
    	
    }
    
	private void showErrorDialog() {
		AlertDialog.Builder ad = new AlertDialog.Builder(GoodreadsActivity.this);
		ad.setTitle("ERROR!");
		ad.setMessage(myApp.errMessage);
		ad.setPositiveButton("OK", new OnClickListener() {
			public void onClick(DialogInterface dialog, int arg1) {
				// do nothing
			}
		});
		ad.show();
	}

	private void toastMe(int msgid) {
		Toast toast = Toast.makeText(getApplicationContext(), msgid, Toast.LENGTH_SHORT);
		toast.show();
	}
}



	
	

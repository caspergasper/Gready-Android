package com.caspergasper.android.goodreads;


import static com.caspergasper.android.goodreads.GoodReadsApp.TAG;

import java.util.List;

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
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;


public class GoodreadsActivity extends Activity implements OnItemLongClickListener, OnItemClickListener,
OnScrollListener {
	
	private GoodReadsApp myApp;
	private static final int SUBMENU_GROUPID = 1;
	
	// arbitrary limit, may need tweaking but should be 
	// a multiple of OAuthInterface.ITEMS_TO_DOWNLOAD
	private static final int TOTAL_BOOKS_TO_KEEP_IN_MEMORY = 60;  
	private static final int PAGES_TO_MOVE_BY = TOTAL_BOOKS_TO_KEEP_IN_MEMORY / OAuthInterface.ITEMS_TO_DOWNLOAD;
	private boolean gettingScrollData = false;
	private boolean goingForward = true;
	private boolean deleteAllBooks = false;
	private int booksToDelete;
	private int xmlPage = 1;
	
	public void onResume() {
		try {
			super.onResume();
			if (myApp.accessToken == null  || myApp.accessTokenSecret == null) {
	        	Log.d(TAG, "Missing accessTokens, retrieving...");
	        	startActivity(new Intent(GoodreadsActivity.this, SettingsActivity.class));
	        	return;
	        } 
			if(myApp.userID == 0) {
				myApp.oauth.goodreads_url = OAuthInterface.GET_USER_ID; 
				myApp.oauth.getXMLFile(xmlPage);
				return;     
			} else {
				if(myApp.userData.updates.size() == 0 && myApp.userData.books.size() == 0) {
					// Got valid tokens and a userid, let's go get some data...
					Log.d(TAG, "Getting updates now...");
					myApp.oauth.goodreads_url = OAuthInterface.GET_FRIEND_UPDATES;
					xmlPage = 1;
					myApp.oauth.getXMLFile(xmlPage);
				} 
			}
		} catch(Exception e) {
			myApp.errMessage = "GoodreadsActivity onResume " + e.toString();
			showErrorDialog();
		}
	}
	
	public void onPause() {
		super.onPause();
	}
	

	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        try{
	    	super.onCreate(savedInstanceState);
	        setContentView(R.layout.main);
	        
	        myApp = GoodReadsApp.getInstance();
	        myApp.goodreads_activity = this;
    	} catch(Exception e) {
			myApp.errMessage = "GoodreadsActivity onCreate " + e.toString();
			showErrorDialog();
		}
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
		
		if(item.getGroupId() == SUBMENU_GROUPID) {
			if(item.getItemId() == myApp.userData.shelves.size()) {
				myApp.userData.shelfToGet = "all";
			} else {
				myApp.userData.shelfToGet = myApp.userData.shelves.get(item.getItemId()).title;
			}
			findViewById(R.id.status_label).setVisibility(View.VISIBLE);
			myApp.oauth.goodreads_url = OAuthInterface.GET_SHELF; 
			deleteAllBooks = true;
			xmlPage = 1;
			myApp.oauth.getXMLFile(xmlPage);
			return true;
		} else {
			if(item.getItemId() == R.id.updates) {
				findViewById(R.id.status_label).setVisibility(View.VISIBLE);
				myApp.oauth.goodreads_url = OAuthInterface.GET_FRIEND_UPDATES;
				myApp.oauth.getXMLFile(xmlPage);
				return true;
			}
			else if(item.getItemId() == R.id.scanbook) {
				IntentIntegrator.initiateScan(GoodreadsActivity.this,IntentIntegrator.DEFAULT_TITLE,IntentIntegrator.DEFAULT_MESSAGE,IntentIntegrator.DEFAULT_YES,IntentIntegrator.DEFAULT_NO,IntentIntegrator.PRODUCT_CODE_TYPES);
				return true;
			}
		}
	
			return false;	
	}
	
    void updateMainScreenForUser(int result) {
    	Log.d(TAG, "updateMainScreenForUser");
    	TextView tv;
    	ListView lv;
    	UserData ud = myApp.userData;
    	
    	if(result != 0) {
			showErrorDialog();
			return;
		}
    	switch (myApp.oauth.goodreads_url) {
		case OAuthInterface.GET_FRIEND_UPDATES:
			lv = (ListView) findViewById(R.id.updates_listview);
			UpdateAdapter updateAdapter;
			if(ud.updates.size() == 0) {
				lv.setOnItemClickListener(null);
				lv.setOnItemLongClickListener(null);
				lv.setOnScrollListener(null);
			    updateAdapter = new UpdateAdapter(this, R.layout.updateitem,
						ud.updates);
				lv.setAdapter(updateAdapter);
			} else {
				updateAdapter = (UpdateAdapter) lv.getAdapter();
				updateAdapter.clear();
			}
			addUpdatesToListView(updateAdapter);
			lv.setVisibility(View.VISIBLE);
			findViewById(R.id.status_label).setVisibility(View.INVISIBLE);
			tv = (TextView) findViewById(R.id.updates_label);
			tv.setText(R.string.updates_label);
			if(myApp.userData.shelves.size() == 0) {
				myApp.oauth.goodreads_url = OAuthInterface.GET_SHELVES;
				xmlPage = 1;
				myApp.oauth.getXMLFile(xmlPage);
			}
			ud.books.clear();
		break;
		case OAuthInterface.GET_SHELF:
			ShelfAdapter shelfAdapter;
			lv = (ListView) findViewById(R.id.updates_listview);
			if(ud.books.size() == 0) {					
				shelfAdapter = new 
				ShelfAdapter(this, R.layout.updateitem, ud.books);
				lv.setAdapter(shelfAdapter);
				lv.setOnItemClickListener(this);
				lv.setOnItemLongClickListener(this);
				lv.setOnScrollListener(this);
			} else {
				shelfAdapter = (ShelfAdapter) lv.getAdapter();
			}
			tv = (TextView) findViewById(R.id.updates_label);
			tv.setText(ud.shelfToGet);
			lv.setVisibility(View.VISIBLE);
			addBooksToListView(shelfAdapter, lv);
			findViewById(R.id.status_label).setVisibility(View.INVISIBLE);
			ud.updates.clear();
		break;
		case OAuthInterface.GET_USER_ID:
			// Let's get the updates
			Log.d(TAG, "Getting friend updates for first time");
			myApp.oauth.goodreads_url = OAuthInterface.GET_FRIEND_UPDATES;
			myApp.oauth.getXMLFile(xmlPage);
		break;
    	case OAuthInterface.GET_SHELVES:
    		// We need to cater for users with > 100 bookshelves like Cait :-)
    		if(ud.endShelf < ud.totalShelves) {
    			Log.d(TAG, "Getting extra shelf for some user that reads too much...");
    			myApp.oauth.getXMLFile(++xmlPage);
    		}
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
	
	private void addUpdatesToListView(UpdateAdapter updateAdapter) {
		for(Update u : myApp.userData.tempUpdates) {
			updateAdapter.add(u);
		}
		myApp.userData.tempUpdates.clear();	
	}
	
	private void addBooksToListView(ShelfAdapter shelf, ListView lv) {
		// Updating the ListView array directly triggers an exception,
		// hence the need for this.
		Book book;
		UserData ud = myApp.userData;
		int i;
		if(deleteAllBooks) {
			shelf.clear();
		}
		
		if(goingForward) {
			for(Book b : ud.tempBooks) {
				shelf.add(b);
			} 
		} else {
			// If we didn't load all the  books from the last XML file
			// just delete the difference and reload them.
			for(i = booksToDelete - 1; i >= 0; i--) {
				book = ud.books.get(0);
//				Log.d(TAG, "Deleting book "  + book.title + " from start.");
				shelf.remove(book);		
			}
			for(i = OAuthInterface.ITEMS_TO_DOWNLOAD - 1; i >= 0; i--) {
				shelf.insert(ud.tempBooks.get(i), 0);
			}
			if(xmlPage == 1) {
				ud.booksRemovedFromFront = false;
			}
		}
		
		// Free up some memory.
		int extraBooks = shelf.getCount() - TOTAL_BOOKS_TO_KEEP_IN_MEMORY; 
		if(extraBooks > 0) {
			Log.d(TAG, "Freeing " + extraBooks + " books from memory.");
			if(goingForward) {
				for(i=0; i < extraBooks; i++) {
//					Log.d(TAG, "Removing " + myApp.userData.books.get(0).title);
					shelf.remove(ud.books.get(0));
				}
				ud.booksRemovedFromFront = true;
				// Scroll back a bit so the screen doesn't jump
				// Can't get it to align precisely as the items are different size
				lv.setSelection(lv.getCount() - extraBooks - 7);
			} else {
				for(i=0; i < extraBooks; i++) {
					book = ud.books.get(ud.books.size() -1);
//					Log.d(TAG, "Removing book " + book.title);
					shelf.remove(book);
				}
				lv.setSelection(OAuthInterface.ITEMS_TO_DOWNLOAD - booksToDelete + 1);
			}
			
		}
		ud.tempBooks.clear();
		if(deleteAllBooks) {
			lv.setSelection(0);
			deleteAllBooks = false;
		}
		gettingScrollData = false;
	}
	
	@Override
	public boolean onItemLongClick(AdapterView<?> _av, View _v, int _index, long arg3) {
		if(myApp.userData.books.get(_index).bookLink != null) {
			Uri uri = Uri.parse(OAuthInterface.URL_ADDRESS +
					OAuthInterface.BOOKPAGE_PATH + myApp.userData.books.get(_index).bookLink);
    		Intent intent = new Intent(Intent.ACTION_VIEW, uri);
    		startActivity(intent);
		} else {
			toastMe(R.string.no_book_page);
		}
		return true;
	}
	
	@Override
	public void onItemClick(AdapterView<?> _av, View _v, int _index, long arg3) {
		List <Book> books = myApp.userData.books;
		Dialog d = new Dialog(GoodreadsActivity.this);
		Window window = d.getWindow();
		window.setFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND, 
				WindowManager.LayoutParams.FLAG_BLUR_BEHIND);
		
		d.setContentView(R.layout.book_dialog);
		d.setTitle(books.get(_index).title);
		TextView textview = (TextView) d.findViewById(R.id.author);
		textview.setText(books.get(_index).author);
		textview = (TextView) d.findViewById(R.id.avg_rating);
		textview.setText("Average rating: " + 
				books.get(_index).average_rating);
		textview = (TextView) d.findViewById(R.id.description);
		textview.setText(
				Html.fromHtml(books.get(_index).description));
		d.show();
		
	}
	
	@Override
	public void onScroll(final AbsListView view, final int first, final int visible, final int total) {
		// detect if last item is visible
		if (visible < total && (first + visible == total)) {
			// see if we have more results
			if (!gettingScrollData && myApp.userData.endBook < myApp.userData.totalBooks 
					&& !myApp.threadLock) {
				gettingScrollData = true;
				Log.d(TAG, "We need to load some more..");
				onLastListItemDisplayed();
			}
		} else if(first == 0  && !gettingScrollData && !myApp.threadLock && myApp.userData.booksRemovedFromFront) {
			gettingScrollData = true;
			Log.d(TAG, "We need to load some earlier entries..");
			onFirstListItemDisplayed();
		}
//		Log.d(TAG, "first:" + first + " Visible:" + visible + " total:" + total);
	}

	private void onLastListItemDisplayed() {
		findViewById(R.id.status_label).setVisibility(View.VISIBLE);
		if(goingForward) {
			xmlPage++;
		} else {
			xmlPage += PAGES_TO_MOVE_BY;
		}
		goingForward = true;
		myApp.oauth.getXMLFile(xmlPage);
	}

	private void onFirstListItemDisplayed() {
		// Work out which page to get.
		// Change direction? 
		if(goingForward) {
			// how many books did you get going forward last time?
			booksToDelete = OAuthInterface.ITEMS_TO_DOWNLOAD - 
			(myApp.userData.endBook - myApp.userData.startBook + 1);
			xmlPage -= PAGES_TO_MOVE_BY;
		} else {
			// Went backward last time
			booksToDelete = 0;
			xmlPage--;
		}
		
		// This shouldn't be needed.
//		if(myApp.userData.xmlPage < 1) {
//			myApp.userData.xmlPage = 1;
//			return;
//		}
		goingForward = false;
		Log.d(TAG, "Getting page " + xmlPage + " booksToDelete " + booksToDelete);
		findViewById(R.id.status_label).setVisibility(View.VISIBLE);
		myApp.oauth.getXMLFile(xmlPage);
	}
	
	@Override
	public void onScrollStateChanged(AbsListView arg0, int arg1) {
		// TODO Auto-generated method stub
		
	}
	//activity result for the barcode scanner intent.
	public void onActivityResult(int requestCode, int resultCode, Intent intent) {
		IntentResult scanResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, intent);
		if (scanResult != null) {
		// handle scan result
			String barcode = scanResult.getContents();
			Log.d(TAG, "scanned:" + barcode);
		}

		Log.d(TAG, "scanner done");
		// else continue with any other code you need in the method
				    
	}
}



	
	

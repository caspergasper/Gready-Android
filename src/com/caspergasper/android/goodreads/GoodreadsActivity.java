package com.caspergasper.android.goodreads;


import static com.caspergasper.android.goodreads.GoodReadsApp.TAG;

import java.net.URL;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnClickListener;
import android.content.res.Configuration;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
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
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RadioGroup;
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
	private ListView updatesListView;
	private ShelfAdapter shelfAdapter;
	static final String GOODREADS_IMG_URL = "http://photo.goodreads.com/";
	private boolean booksRemovedFromFront = false;
	
	// Need handler for callbacks to the UI thread
    final Handler mHandler = new Handler();

	public void onResume() {
		try {
			super.onResume();
			if (myApp.accessToken == null  || myApp.accessTokenSecret == null) {
	        	Log.d(TAG, "Missing accessTokens, retrieving...");
	        	startActivity(new Intent(GoodreadsActivity.this, SettingsActivity.class));
	        	return;
	        } 
			if(myApp.userID == 0  && !myApp.threadLock) {
				myApp.oauth.getXMLFile(xmlPage, OAuthInterface.GET_USER_ID);
				return;     
			} else {
				if(myApp.userData.updates.size() == 0 && myApp.userData.books.size() == 0 
						&& !myApp.threadLock) {
					// Got valid tokens and a userid, let's go get some data...
					Log.d(TAG, "Getting updates now...");
					xmlPage = 1;
					myApp.oauth.getXMLFile(xmlPage, OAuthInterface.GET_FRIEND_UPDATES);
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
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        try{
	    	super.onCreate(savedInstanceState);
	        setContentView(R.layout.main);
	        
	        myApp = GoodReadsApp.getInstance();
	        myApp.goodreads_activity = this;
	        updatesListView = (ListView) findViewById(R.id.updates_listview);
	        newQuery();  // For when activity has been cleared from memory but app hasn't
    	} catch(Exception e) {
			myApp.errMessage = "GoodreadsActivity onCreate " + e.toString();
			showErrorDialog();
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
		if(myApp.threadLock) {
			return false;
		}
		if(item.getGroupId() == SUBMENU_GROUPID) {
			if(item.getItemId() == myApp.userData.shelves.size()) {
				myApp.userData.shelfToGet = "all";
			} else {
				myApp.userData.shelfToGet = myApp.userData.shelves.get(item.getItemId()).title;
			}
			showUpdateMessage(R.string.getBooks);
			newQuery();
			myApp.oauth.getXMLFile(xmlPage, OAuthInterface.GET_SHELF);
			return true;
		} else if(item.getItemId() == R.id.updates) {
				showUpdateMessage(R.string.getUpdates);
				myApp.oauth.getXMLFile(xmlPage, OAuthInterface.GET_FRIEND_UPDATES);
				return true;
		}
		else if(item.getItemId() == R.id.scanbook) {
			IntentIntegrator.initiateScan(GoodreadsActivity.this,IntentIntegrator.DEFAULT_TITLE,IntentIntegrator.DEFAULT_MESSAGE,IntentIntegrator.DEFAULT_YES,IntentIntegrator.DEFAULT_NO,IntentIntegrator.PRODUCT_CODE_TYPES);
			return true;
		}
 		else if(item.getItemId() == R.id.search) {
			showSearchDialog();
			return true;
		}
			return false;	
	}
	
	void showUpdateMessage(int resource) {
		TextView textView = (TextView) findViewById(R.id.status_label);
		textView.setText(resource);
		textView.setVisibility(View.VISIBLE);
	}
	
	void showSearchDialog() {
		final Dialog d = new Dialog(GoodreadsActivity.this);
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
				gotoWebURL(OAuthInterface.URL_ADDRESS +
						OAuthInterface.BOOKPAGE_SEARCH + Uri.encode(text));
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
		deleteAllBooks = true;
		booksRemovedFromFront = false;
		xmlPage = 1;
	}
	
    void updateMainScreenForUser(int result) {
    	Log.d(TAG, "updateMainScreenForUser");
    	TextView tv;
    	UserData ud = myApp.userData;
    	
    	if(result != 0) {
			showErrorDialog();
			return;
		}
    	switch (myApp.oauth.goodreads_url) {
		case OAuthInterface.GET_FRIEND_UPDATES:
			UpdateAdapter updateAdapter;
			if(ud.updates.size() == 0) {
				updatesListView.setOnItemClickListener(null);
				updatesListView.setOnItemLongClickListener(null);
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
		case OAuthInterface.SEARCH_SHELVES:
		case OAuthInterface.GET_BOOKS_BY_ISBN:
		case OAuthInterface.GET_SHELF:
			if(ud.books.size() == 0) {					
				shelfAdapter = new 
				ShelfAdapter(this, R.layout.booklistitem, ud.books);
				updatesListView.setAdapter(shelfAdapter);
				updatesListView.setOnItemClickListener(this);
				updatesListView.setOnItemLongClickListener(this);
				updatesListView.setOnScrollListener(this);
			} else {
				shelfAdapter = (ShelfAdapter) updatesListView.getAdapter();
			}
			tv = (TextView) findViewById(R.id.updates_label);
			if(myApp.oauth.goodreads_url == OAuthInterface.GET_BOOKS_BY_ISBN) {
				tv.setText("");
			} else {
				tv.setText("(" + ud.totalBooks + ") " + ud.shelfToGet);
			}
			updatesListView.setVisibility(View.VISIBLE);
			addBooksToListView();
			findViewById(R.id.status_label).setVisibility(View.INVISIBLE);
			ud.updates.clear();
			// Load images in background
			getImages();
		break;
		case OAuthInterface.GET_USER_ID:
			Log.d(TAG, "Getting friend updates for first time");
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
	
	private void addBooksToListView() {
		// Updating the ListView array directly triggers an exception,
		// hence the need for this.
		Book book;
		UserData ud = myApp.userData;
		int i;
		if(deleteAllBooks) {
			shelfAdapter.clear();
		}
		
		if(goingForward) {
			for(Book b : ud.tempBooks) {
				shelfAdapter.add(b);
			} 
		} else {
			// If we didn't load all the  books from the last XML file
			// just delete the difference and reload them.
			for(i = booksToDelete - 1; i >= 0; i--) {
				book = ud.books.get(0);
//				Log.d(TAG, "Deleting book "  + book.title + " from start.");
				shelfAdapter.remove(book);		
			}
			for(i = OAuthInterface.ITEMS_TO_DOWNLOAD - 1; i >= 0; i--) {
				shelfAdapter.insert(ud.tempBooks.get(i), 0);
			}
			if(xmlPage == 1) {
				booksRemovedFromFront = false;
			}
		}
		
		// Free up some memory.
		int extraBooks = shelfAdapter.getCount() - TOTAL_BOOKS_TO_KEEP_IN_MEMORY; 
		if(extraBooks > 0) {
			Log.d(TAG, "Freeing " + extraBooks + " books from memory.");
			if(goingForward) {
				for(i=0; i < extraBooks; i++) {
//					Log.d(TAG, "Removing " + myApp.userData.books.get(0).title);
					shelfAdapter.remove(ud.books.get(0));
				}
				booksRemovedFromFront = true;
				// Scroll back a bit so the screen doesn't jump
				// Can't get it to align precisely as the items are different size
				updatesListView.setSelection(updatesListView.getCount() - extraBooks - 7);
			} else {
				for(i=0; i < extraBooks; i++) {
					book = ud.books.get(ud.books.size() -1);
//					Log.d(TAG, "Removing book " + book.title);
					shelfAdapter.remove(book);
				}
				updatesListView.setSelection(OAuthInterface.ITEMS_TO_DOWNLOAD - booksToDelete + 1);
			}
			
		}
		ud.tempBooks.clear();
		if(deleteAllBooks) {
			updatesListView.setSelection(0);
			deleteAllBooks = false;
		}
		gettingScrollData = false;
	}
	
	void gotoWebURL(String path) {
		Uri uri = Uri.parse(path);
		Intent intent = new Intent(Intent.ACTION_VIEW, uri);
		startActivity(intent);
	}
	
	@Override
	public boolean onItemLongClick(AdapterView<?> _av, View _v, int _index, long arg3) {
		final Book b =  myApp.userData.books.get(_index);
		if(b.bookLink != null) {
			gotoWebURL(OAuthInterface.URL_ADDRESS +
				OAuthInterface.BOOKPAGE_PATH + b.bookLink);
		} else {
			toastMe(R.string.no_book_page);
		}
		return true;
	}
	
	private void addBookToShelf(final Book b) {
		// Ask user if they want to add this to shelf
		AlertDialog.Builder ad = new AlertDialog.Builder(GoodreadsActivity.this);
		ad.setTitle(R.string.addToShelf);
		ad.setMessage(R.string.addToShelfQ);
		ad.setPositiveButton("Yes", new OnClickListener() {
			public void onClick(DialogInterface dialog, int arg1) {
				myApp.oauth.postBookToShelf(b.id, "to-read");
			}
		});
		ad.setNegativeButton("No", new OnClickListener() {
			public void onClick(DialogInterface dialog, int arg1) {
				showBookDetail(b);
			}
		});
		ad.show();
	}
	
	@Override
	public void onItemClick(AdapterView<?> _av, View _v, int _index, long arg3) {
		Book b = myApp.userData.books.get(_index);
		if(myApp.oauth.goodreads_url == OAuthInterface.GET_BOOKS_BY_ISBN && 
				b.shelves.size() == 0){
			addBookToShelf(b);
		} else {
			showBookDetail(b);
		}
	}
	
	private final void showBookDetail(Book b) {
		TextView textview;
		Dialog d = new Dialog(GoodreadsActivity.this);
		Window window = d.getWindow();
		window.setFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND, 
				WindowManager.LayoutParams.FLAG_BLUR_BEHIND);
		d.requestWindowFeature(Window.FEATURE_NO_TITLE);
		d.setContentView(R.layout.book_dialog);
		
		if(b.bitmap != null) {
			ImageView imgView = (ImageView) d.findViewById(R.id.bookDialogImage);
			imgView.setImageBitmap(b.bitmap);
		}
		
		textview = (TextView) d.findViewById(R.id.title);
		textview.setText(b.title);
		textview = (TextView) d.findViewById(R.id.author);
		textview.setText(b.author);
		textview = (TextView) d.findViewById(R.id.avg_rating);
		textview.setText("Average rating: " + b.average_rating);
		textview = (TextView) d.findViewById(R.id.shelves);
		if(b.shelves.size() == 0) {
			textview.setText("Shelves: not shelved");
		} else {
			textview.setText("Shelves: " + b.getShelves());
		}
		textview = (TextView) d.findViewById(R.id.description);
		textview.setText(Html.fromHtml(b.description));
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
		} else if(first == 0  && !gettingScrollData && !myApp.threadLock && booksRemovedFromFront) {
			gettingScrollData = true;
			Log.d(TAG, "We need to load some earlier entries..");
			onFirstListItemDisplayed();
		}
//		Log.d(TAG, "first:" + first + " Visible:" + visible + " total:" + total);
	}

	private void onLastListItemDisplayed() {
		showUpdateMessage(R.string.getBooks);
		if(goingForward) {
			xmlPage++;
		} else {
			xmlPage += PAGES_TO_MOVE_BY;
		}
		goingForward = true;
		myApp.oauth.getXMLFile(xmlPage, OAuthInterface.GET_SHELF);
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
		
		goingForward = false;
		Log.d(TAG, "Getting page " + xmlPage + " booksToDelete " + booksToDelete);
		showUpdateMessage(R.string.getBooks);
		myApp.oauth.getXMLFile(xmlPage, OAuthInterface.GET_SHELF);
	}
	
	@Override
	public void onScrollStateChanged(AbsListView arg0, int arg1) {
		// TODO Auto-generated method stub
		
	}
	
	//do something when book scanner finishes.
	public void onActivityResult(int requestCode, int resultCode, Intent intent) {
		IntentResult scanResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, intent);
		if (scanResult != null) {
			// handle scan result
			String barcode = scanResult.getContents();
			if(barcode == null){
				return;
			}
			Log.d(TAG, "scanned UPC:" + barcode);
			String isbn = GoodReadsApp.ConvertUPCtoISBN(barcode);
			Log.d(TAG, "converted ISBN:" + isbn);
			// TODO show book details 
			myApp.userData.isbnScan = isbn;
			showUpdateMessage(R.string.getBookByISBN);
			newQuery();
			myApp.oauth.getXMLFile(xmlPage, OAuthInterface.GET_BOOKS_BY_ISBN);
		}				    
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
    	ShelfAdapter adapter = (ShelfAdapter) updatesListView.getAdapter();
    	Book b;
    	int size = myApp.userData.books.size();
    	for(int i = 0; i < size; i++) {
    		if(!myApp.getImageThreadRunning) {
    			Log.d(TAG, "stopping getImage thread.");
    			break;
    		}
			b = adapter.getItem(i);
			try {
				if(b.small_image_url == null) {
					continue;
				}
				if(b.bitmap == null) {
					URL newurl = new URL(GOODREADS_IMG_URL + b.small_image_url); 
					Log.d(TAG, "Getting " + GOODREADS_IMG_URL + b.small_image_url);
					b.bitmap = BitmapFactory.decodeStream(newurl.openConnection().getInputStream());
					b.small_image_url = null;
				}
				mHandler.post(doUpdateGUI);
			} catch (Exception e) {
				myApp.errMessage = e.toString() + " " + e.getStackTrace().toString();
				showErrorDialog();
			}
		} // for
    	
    	
    }
    
    private void updateResultsInUi() {
        // Back in the UI thread -- update UI elements
    	shelfAdapter.notifyDataSetChanged();
    }

}



	
	

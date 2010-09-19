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
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RadioGroup;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;


public class BooksActivity extends Activity implements OnItemClickListener, OnItemLongClickListener, 
OnScrollListener {
	
	private GoodReadsApp myApp;
	private static final int SUBMENU_GROUPID = 1;
	private static final int SUBMENU_GROUPID_RADIO = 2;
	
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
	private boolean booksRemovedFromFront = false;
	static Book currentBook;
	// Need handler for callbacks to the UI thread
    final Handler mHandler = new Handler();
	

    void removeCurrentBook() {
    	shelfAdapter.remove(currentBook);
    }
    
	public void onResume() {
		myApp.goodreads_activity = this;
		try {
			super.onResume();
			if (myApp.accessToken == null  || myApp.accessTokenSecret == null) {
	        	Log.d(TAG, "Missing accessTokens, retrieving...");
	        	startActivity(new Intent(BooksActivity.this, OAuthCallbackActivity.class));
	        	return;
	        } 
			if(myApp.userID == 0  && !myApp.threadLock) {
				myApp.oauth.getXMLFile(xmlPage, OAuthInterface.GET_USER_ID);
				return;     
			} else {
				if(myApp.menuItem != null) {
					onOptionsItemSelected(myApp.menuItem);
					myApp.menuItem = null;
					return;
				}
				if(myApp.userData.books.size() == 0 && !myApp.threadLock && 
						myApp.userData.shelfToGet != null) {
					// Got valid tokens and a userid, let's go get some data...
					Log.d(TAG, "Getting books now...");
					xmlPage = 1;
					myApp.oauth.getXMLFile(xmlPage, OAuthInterface.GET_SHELF);
				} 
			}
		} catch(Exception e) {
			myApp.errMessage = "BooksActivity onResume " + e.toString();
			myApp.showErrorDialog(this);
		}
	}
	
	public void onPause() {
		super.onPause();
	}
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        try{
	    	super.onCreate(savedInstanceState);
	        setContentView(R.layout.bookslist);
	        myApp = GoodReadsApp.getInstance();
	        updatesListView = (ListView) findViewById(R.id.updates_listview);
	        newQuery();  // For when activity has been cleared from memory but app hasn't
	        
    	} catch(Exception e) {
			myApp.errMessage = "BooksActivity onCreate " + e.toString();
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
		super.onCreateOptionsMenu(menu);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.startmenu, menu);
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
			myApp.userData.shelfToGet = "Updates";
			startActivity(new Intent(BooksActivity.this, UpdatesActivity.class));	
			finish();
			return true;
		} else if(item.getItemId() == R.id.scanbook) {
			IntentIntegrator.initiateScan(BooksActivity.this,IntentIntegrator.DEFAULT_TITLE,IntentIntegrator.DEFAULT_MESSAGE,IntentIntegrator.DEFAULT_YES,IntentIntegrator.DEFAULT_NO,IntentIntegrator.PRODUCT_CODE_TYPES);
			return true;
		} else if(item.getItemId() == R.id.search) {
			showSearchDialog();
			return true;
		} else if(item.getItemId() == R.id.preferences) {
			startActivity(new Intent(BooksActivity.this, Preferences.class));
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
		final Dialog d = myApp.createDialogBox(BooksActivity.this, R.layout.booksearch_dialog, false);
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
		deleteAllBooks = true;
		booksRemovedFromFront = false;
		xmlPage = 1;
	}
	
    void updateMainScreenForUser(int result) {
    	Log.d(TAG, "booksMainScreenForUser");
    	UserData ud = myApp.userData;
    	
    	if(result == 1) {
			myApp.showErrorDialog(this);
			return;
		} else if(result == 2) {
			myApp.showGetAuthorizationDialog(this);
			return;
		}
    	try {
    	switch (myApp.oauth.goodreads_url) {
		case OAuthInterface.SEARCH_SHELVES:
		case OAuthInterface.GET_BOOKS_BY_ISBN:
		case OAuthInterface.GET_SHELF:
			if(ud.books.size() == 0) {					
				shelfAdapter = new 
				ShelfAdapter(this, R.layout.booklistitem, ud.books);
				updatesListView.setAdapter(shelfAdapter);
				updatesListView.setOnItemClickListener(this);
				updatesListView.setOnItemLongClickListener(this);
				registerForContextMenu(updatesListView);
				updatesListView.setOnScrollListener(this);
			} else {
				shelfAdapter = (ShelfAdapter) updatesListView.getAdapter();
			}
			if(myApp.oauth.goodreads_url == OAuthInterface.GET_BOOKS_BY_ISBN) {
				((TextView) findViewById(R.id.updates_label)).setText("");
			} else {
				((TextView) findViewById(R.id.updates_label)).setText("(" + ud.totalBooks + ") " 
						+ ud.shelfToGet);
			}
			updatesListView.setVisibility(View.VISIBLE);
			addBooksToListView();
			findViewById(R.id.status_label).setVisibility(View.INVISIBLE);
			ud.updates.clear();
			if(myApp.userData.shelves.size() == 0) {
				xmlPage = 1;
				myApp.oauth.getXMLFile(xmlPage, OAuthInterface.GET_SHELVES);
			} else {
				// Load images in background
				getImages();
			}
		break;
		case OAuthInterface.GET_SHELVES:
    		// We need to cater for users with > 100 bookshelves like Cait :-)
    		if(ud.endShelf < ud.totalShelves) {
    			Log.d(TAG, "Getting extra shelf -- total shelves: " + ud.totalShelves);
    			myApp.oauth.getXMLFile(++xmlPage, OAuthInterface.GET_SHELVES);
    		} else {
    			myApp.oauth.goodreads_url = OAuthInterface.GET_SHELF;
    			getImages();
    		}
    	break;
    	}
    	} catch(Exception e) {
			myApp.errMessage = "BooksActivity updateMainScreenForUser " + e.toString();
			myApp.showErrorDialog(this);
		}
    }
    
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
    		ContextMenu.ContextMenuInfo menuInfo) {
    	super.onCreateContextMenu(menu, v, menuInfo);
    	
    	menu.setHeaderTitle(R.string.what_to_do);
    	menu.add(0, Menu.FIRST, Menu.NONE, R.string.goto_mobilesite);
    	menu.add(0, 2, Menu.NONE, R.string.rate_review);
    	menu.add(0, 3, Menu.NONE, R.string.see_book_details);
    	SubMenu sub = menu.addSubMenu(R.string.change_shelves);
    	List<Shelf> tempShelves = myApp.userData.shelves;
    	// Copied text
    	int shelf_length = tempShelves.size();
       	Shelf shelf;
    	for(int i=0; i<shelf_length; i++) {
    		shelf = tempShelves.get(i);
    
    		if(shelf.exclusive) {
    			sub.add(SUBMENU_GROUPID_RADIO, i, Menu.NONE,
        				shelf.title);
    		} else {
    			sub.add(SUBMENU_GROUPID, i, Menu.NONE,
    				shelf.title).setCheckable(true);
    		}
    		for(String shelfTitle : currentBook.shelves) {
    			if(shelfTitle.compareTo(shelf.title) == 0) {
    				sub.getItem(i).setChecked(true).setEnabled(false);
    				break;
    			}
    		}
    	}
    	sub.setGroupCheckable(SUBMENU_GROUPID_RADIO, true, true);
    }
    
    @Override
    public boolean onContextItemSelected(MenuItem item) {
    	super.onContextItemSelected(item);
    	int groupId = item.getGroupId(); 
    	int itemId = item.getItemId();
    	if(groupId == 0 && itemId == Menu.FIRST) {
    		if(currentBook.bookLink != null) {
    			String path = OAuthInterface.URL_ADDRESS +
    			OAuthInterface.BOOKPAGE_PATH + currentBook.bookLink;
    			Uri uri = Uri.parse(path);
    			Intent intent = new Intent(Intent.ACTION_VIEW, uri);
    			startActivity(intent);
    		} else {
    			toastMe(R.string.no_book_page);
    		}
    	} else if(groupId == 0 && itemId == 2) {
    		showReviewDialog();	
    	} else if(groupId == 0 && itemId == 3) {
    		showBookDetail(currentBook);
    	} else if(groupId == SUBMENU_GROUPID || groupId == SUBMENU_GROUPID_RADIO) {
    		Log.d(TAG, "add book to shelf " + item.getTitle().toString());
    		myApp.oauth.postBookToShelf(currentBook.id, item.getTitle().toString());
    	}
    	return true;
    }
    
	void toastMe(int msgid) {
		Toast toast = Toast.makeText(getApplicationContext(), msgid, Toast.LENGTH_SHORT);
		toast.show();
	}
	
	private void showReviewDialog() {
		final Dialog d = myApp.createDialogBox(BooksActivity.this, R.layout.review_dialog, false);
		d.setTitle(R.string.edit_review);
		((EditText)d.findViewById(R.id.statusbox)).setText(BooksActivity.currentBook.review);
		((RatingBar)d.findViewById(R.id.set_rating)).setRating(BooksActivity.currentBook.myRating);
		((Button)d.findViewById(R.id.updatebutton)).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View arg0) {
				int rating = (int) ((RatingBar)d.findViewById(R.id.set_rating)).getRating();
				String body = ((EditText)d.findViewById(R.id.statusbox)).getText().toString().trim();
				if(rating != 0 || body.length() > 0) {
					myApp.oauth.postReview(BooksActivity.currentBook.reviewId, rating, body);
					BooksActivity.currentBook.myRating = rating;
					BooksActivity.currentBook.review = body;
				}
				d.hide();
			}
		});
		d.show();
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
	
	@Override
	public boolean onItemLongClick(AdapterView<?> _av, View _v, int _index, long arg3) {
		currentBook = myApp.userData.books.get(_index);
		return false;
	}
		
	private void addBookToShelf(final Book b) {
		// Ask user if they want to add this to shelf
		AlertDialog.Builder ad = new AlertDialog.Builder(BooksActivity.this);
		ad.setTitle(R.string.addToShelf);
		ad.setMessage(R.string.addToShelfQ);
		ad.setPositiveButton("Yes", new OnClickListener() {
			public void onClick(DialogInterface dialog, int arg1) {
				myApp.oauth.postBookToShelf(b.id, GoodReadsApp.TO_READ);
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
		currentBook = myApp.userData.books.get(_index);
		if(myApp.oauth.goodreads_url == OAuthInterface.GET_BOOKS_BY_ISBN && 
				currentBook.shelves.size() == 0){
			addBookToShelf(currentBook);
		} else {
			showBookDetail(currentBook);
		}
	}
	
	private final void showBookDetail(Book b) {
		Dialog d = myApp.createDialogBox(BooksActivity.this, R.layout.book_dialog, true);
		if(b.bitmap != null) {
			((ImageView) d.findViewById(R.id.bookDialogImage)).setImageBitmap(b.bitmap);
		}	
		((RatingBar) d.findViewById(R.id.rating)).setRating(b.myRating);
		((TextView) d.findViewById(R.id.title)).setText(b.title);
		((TextView) d.findViewById(R.id.author)).setText(b.author);
		((TextView) d.findViewById(R.id.avg_rating)).setText("Avg rating: " + b.average_rating);
		if(b.shelves.size() == 0) {
			((TextView) d.findViewById(R.id.shelves)).setText("Shelves: not shelved");
		} else {
			((TextView) d.findViewById(R.id.shelves)).setText("Shelves: " + b.getShelves());
		}
		((TextView) d.findViewById(R.id.description)).setText(Html.fromHtml(b.description));
		if(b.review.length() > 0) {
			d.findViewById(R.id.my_review_label).setVisibility(View.VISIBLE);
			((TextView) d.findViewById(R.id.my_review)).setText(Html.fromHtml(b.review));
		} else {
			d.findViewById(R.id.my_review_label).setVisibility(View.INVISIBLE);
		}
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
		myApp.oauth.getXMLFile(xmlPage, myApp.oauth.goodreads_url);
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
		myApp.oauth.getXMLFile(xmlPage, myApp.oauth.goodreads_url);
	}
	
	@Override
	public void onScrollStateChanged(AbsListView arg0, int arg1) {	}
	
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
			// show book details 
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
				if(b.imgUrl == null) {
					continue;
				}
				if(b.bitmap == null) {
					URL newurl = new URL(GoodReadsApp.GOODREADS_IMG_URL + b.imgUrl); 
					Log.d(TAG, "Getting " + GoodReadsApp.GOODREADS_IMG_URL + b.imgUrl);
					b.bitmap = BitmapFactory.decodeStream(newurl.openConnection().getInputStream());
					b.imgUrl = null;
				}
				mHandler.post(doUpdateGUI);
			} catch (Exception e) {
				myApp.errMessage = e.toString() + " " + e.getStackTrace().toString();
				myApp.showErrorDialog(this);
			}
		} // for
    		
    }
    
    private void updateResultsInUi() {
        // Back in the UI thread -- update UI elements
    	shelfAdapter.notifyDataSetChanged();
    }

}



	
	

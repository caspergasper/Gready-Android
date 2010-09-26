package com.caspergasper.android.goodreads;

import static com.caspergasper.android.goodreads.GoodReadsApp.TAG;

import java.util.List;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;

public class ReviewsActivity extends Activity {

	private GoodReadsApp myApp;
	private ListView reviewsListView;
	private ReviewAdapter reviewAdapter;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        try{
	    	super.onCreate(savedInstanceState);
	        setContentView(R.layout.reviews);
	        myApp = GoodReadsApp.getInstance();
	        reviewsListView = (ListView) findViewById(R.id.reviews_listview);
	 
        	} catch(Exception e) {
			myApp.errMessage = "ReviewsActivity onCreate " + e.toString();
			myApp.showErrorDialog(this);
		}
    }
    
    @Override
    public void onResume() {
    	super.onResume();
    	if(reviewsListView.getAdapter() == null) {
    		reviewAdapter = new 
			ReviewAdapter(this, R.layout.reviewlistitem, myApp.userData.reviews);
			reviewsListView.setAdapter(reviewAdapter);
			reviewsListView.setVisibility(View.VISIBLE);
			((TextView)findViewById(R.id.updates_label)).setText("(" + myApp.userData.reviews.size() +
					") reviews");
    	}
    	
    }
    
	@Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.reviews_menu, menu);
        myApp.sub = menu.addSubMenu(0, 0, Menu.NONE, R.string.bookshelves_label);
        myApp.sub.setHeaderIcon(android.R.drawable.ic_menu_view);
        myApp.sub.setIcon(android.R.drawable.ic_menu_view);
    	List<Shelf> tempShelves = myApp.userData.shelves;
        myApp.createShelvesMenu(tempShelves);
        return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		super.onOptionsItemSelected(item);
		int itemId = item.getItemId();
		if(item.getGroupId() == GoodReadsApp.SUBMENU_GROUPID) {
			if(myApp.handleBookshelfSelection(this, itemId)) {
				startActivity(new Intent(ReviewsActivity.this, BooksActivity.class));
				finish();
			}
			return true;
		} else if(itemId == R.id.updates) {
			myApp.userData.shelfToGet = "Updates";
			startActivity(new Intent(ReviewsActivity.this, UpdatesActivity.class));	
			finish();
			return true;
		}
		return false;
	}
}

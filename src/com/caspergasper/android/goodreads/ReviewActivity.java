package com.caspergasper.android.goodreads;


import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RatingBar;


public class ReviewActivity extends Activity implements OnClickListener {
	
	private GoodReadsApp myApp;
	
	public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.review_dialog);
        myApp = GoodReadsApp.getInstance();
        ((RatingBar)findViewById(R.id.rating)).setRating(BooksActivity.currentBook.myRating);
        ((Button)findViewById(R.id.updatebutton)).setOnClickListener(this);
	}

	@Override
	public void onClick(View arg0) {
		try{
		int rating = (int) ((RatingBar)findViewById(R.id.rating)).getRating();
		String body = ((EditText)findViewById(R.id.statusbox)).getText().toString().trim();
		if(rating != 0 || body.length() > 0) {
			myApp.oauth.postReview(BooksActivity.currentBook.reviewId, rating, body);
		}
		finish();
		} catch(Exception e) {
			myApp.errMessage = "BooksActivity onResume " + e.toString();
			myApp.showErrorDialog(this);
		}
	}

}

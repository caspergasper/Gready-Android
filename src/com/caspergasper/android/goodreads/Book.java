package com.caspergasper.android.goodreads;

import java.util.ArrayList;
import java.util.List;
import android.graphics.Bitmap;


public class Book {

	int id;
	int reviewId;
	Bitmap bitmap;
	String imgUrl;
	String title;
	String bookLink;
	String average_rating;
	int myRating = 0;
	String description = "";
	List <String> shelves = new ArrayList<String>();
	String author = "";

	Book(String str) {
		title = str;
	}
	
	Book(int _id) {
		reviewId = _id;
	}
	
	@Override
	public String toString() {
		return title + "\n" + author;
	}
	
	void setBookLink(String strLink) {
		if(strLink != null) {
			bookLink = strLink.substring(strLink.lastIndexOf('/'));
		}
	}
	
	String getShelves() {
		String retVal = "";
		for(String str : shelves){
			if(retVal == "") {
				retVal = str;
			} else {
				retVal += ", " + str;
			}
		}
		return retVal;
	}
}

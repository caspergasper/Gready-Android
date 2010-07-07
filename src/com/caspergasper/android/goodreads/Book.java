package com.caspergasper.android.goodreads;

import android.graphics.Bitmap;


public class Book {

	String title;
	String bookLink;
	String small_image_url;
	String average_rating;
	String description = "";
	Bitmap bitmap;
//	int id;
	String author = "";

	Book(String str) {
		title = str;
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
}

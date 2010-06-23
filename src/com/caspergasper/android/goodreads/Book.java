package com.caspergasper.android.goodreads;


public class Book {

	String title;
	String bookLink;
//	String small_image_url;
	String average_rating;
	String description = "";
	int id;
	String author = "";
	Float user_rating;

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

package com.caspergasper.android.goodreads;


public class Book {

	String title;
	String link;
//	String small_image_url;
	String average_rating;
	String description;
	int id;
	String author = "";

	Book(String str) {
		title = str;
	}
	
	@Override
	public String toString() {
		return title + "\n" + author;
	}
}

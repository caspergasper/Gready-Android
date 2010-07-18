package com.caspergasper.android.goodreads;

import java.util.ArrayList;
import java.util.List;

import android.graphics.Bitmap;


public class Book {

	String title;
	String bookLink;
	String small_image_url;
	String average_rating;
	String description = "";
	Bitmap bitmap;
	int id;
	List <String> shelves = new ArrayList<String>();
	String author = "";

	Book(String str) {
		title = str;
	}
	
	Book(int _id) {
		id = _id;
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

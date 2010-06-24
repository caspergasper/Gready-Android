package com.caspergasper.android.goodreads;

import static com.caspergasper.android.goodreads.GoodReadsApp.TAG;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.SAXException;

import android.util.Log;

class UserData {

	String username;
	int num_of_friends;
	int endBook;
	int totalBooks;
	int bookPage;
	List <Update> updates;
	List <Shelf> shelves;
	List <Book> books;
	String shelf_to_get;
	int book_to_get; // MSR
	
	private SAXParserFactory factory;
	private SAXParser parser;
	private SaxHandler handler;
	
	UserData() {
		bookPage = 0;
		updates = new ArrayList<Update>();
		shelves = new ArrayList<Shelf>();
		books = new ArrayList<Book>();
		factory = SAXParserFactory.newInstance();
        try {
            parser = factory.newSAXParser();
            handler = new SaxHandler(this);
        } catch (Exception e) {
            throw new RuntimeException(e);
        } 

	}
	
	void getSAXUpdates(InputStream is) {
		try {
			Log.d(TAG, "Start parsing.");
			parser.parse(is, handler);
			Log.d(TAG, "End parsing.");
		} catch (SAXException e) {
			// XML not well-formed -- ignore.
			Log.e(TAG, "SAXException: " + e.toString());	
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
            return;
	}
		
	void getSAXUserid(InputStream is) {
		try {
			UserSAXHandler saxHandler = new UserSAXHandler();
			parser.parse(is, saxHandler);
		} catch (SAXException e) {
			// XML not well-formed -- ignore.
			Log.e(TAG, "SAXException: " + e.toString());	
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
            return;
	}

	


	
}

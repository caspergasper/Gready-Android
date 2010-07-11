package com.caspergasper.android.goodreads;

import static com.caspergasper.android.goodreads.GoodReadsApp.TAG;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import android.util.Log;

class UserData {

	String username;
	int startBook;
	int endBook;
	int totalBooks;
	int endShelf;
	int totalShelves;
	List <Update> updates;
	List <Update> tempUpdates;
	List <Shelf> shelves;
	List <Book> books;
	List <Book> tempBooks;
	String shelfToGet;
	private SAXParserFactory factory;
	private SAXParser parser;
	
	UserData() {
		updates = new ArrayList<Update>();
		tempUpdates = new ArrayList<Update>();
		shelves = new ArrayList<Shelf>();
		books = new ArrayList<Book>();
		tempBooks = new ArrayList<Book>();
		factory = SAXParserFactory.newInstance();
        try {
            parser = factory.newSAXParser();
        } catch (Exception e) {
            throw new RuntimeException(e);
        } 

	}
	
	void getSAXBooks(InputStream is) {
		Reader reader;
		InputSource source;
		BooksSaxHandler handler;
		try {
			Log.d(TAG, "Start parsing books.");
			// Have to strip off non-UTF-8 characters  -
			// sadly there's a few of those in the XML :-( 
			reader = new InputStreamReader(is, "UTF-8");
			source = new InputSource(reader);
			handler = new BooksSaxHandler(this);
			parser.parse(source, handler);				
			Log.d(TAG, "End parsing books.");
			
		} catch (SAXException e) {
			// XML not well-formed -- abort but carry on reading next XML file.
			Log.e(TAG, "SAXException in getSAXBooks: " + e.toString());
			if(books.size() != 0) {
				Log.e(TAG, books.get(books.size() - 1).title);
			}
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
	
	void getSAXUpdates(InputStream is) {
		UpdatesSaxHandler handler;
		try {
			Log.d(TAG, "Start parsing updates.");
			handler = new UpdatesSaxHandler(this);
			parser.parse(is, handler);				
			Log.d(TAG, "End parsing updates.");
			
		} catch (SAXException e) {
			// XML not well-formed -- abort but carry on reading next XML file.
			Log.e(TAG, "SAXException in getSAXUpdates: " + e.toString());
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
            return;
	}
	
	void getSAXShelves(InputStream is) {
		ShelvesSaxHandler handler;
		try {
			Log.d(TAG, "Start parsing shelves.");
			handler = new ShelvesSaxHandler(this);
			parser.parse(is, handler);				
			Log.d(TAG, "End parsing shelves.");
		} catch (SAXException e) {
			// XML not well-formed -- abort but carry on reading next XML file.
			Log.e(TAG, "SAXException in getSAXShelves: " + e.toString());
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
            return;
	}
	
	
}

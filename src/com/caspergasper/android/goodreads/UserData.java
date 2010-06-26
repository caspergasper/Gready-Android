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
	int num_of_friends;
	int endBook;
	int totalBooks;
	int endShelf;
	int totalShelves;
	int bookPage;
	int shelfPage;
	List <Update> updates;
	List <Shelf> shelves;
	List <Book> books;
	List <Book> temp_books;
	String shelf_to_get;
	
	private SAXParserFactory factory;
	private SAXParser parser;
	private SaxHandler handler;
	
	UserData() {
		bookPage = 0;
		updates = new ArrayList<Update>();
		shelves = new ArrayList<Shelf>();
		books = new ArrayList<Book>();
		temp_books = new ArrayList<Book>();
		factory = SAXParserFactory.newInstance();
        try {
            parser = factory.newSAXParser();
            handler = new SaxHandler(this);
        } catch (Exception e) {
            throw new RuntimeException(e);
        } 

	}
	
	void getSAXUpdates(InputStream is) {
		Reader reader;
		InputSource source;
		try {
			Log.d(TAG, "Start parsing.");
			// Have to strip off non-UTF-8 characters  -
			// sadly there's a few of those in the XML :-( 
			reader = new InputStreamReader(is, "UTF-8");
			source = new InputSource(reader);
			parser.parse(source, handler);				
			Log.d(TAG, "End parsing.");
			
		} catch (SAXException e) {
			// XML not well-formed -- abort but carry on reading next XML file.
			Log.e(TAG, "SAXException in getSAXUpdates: " + e.toString());
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
	
}

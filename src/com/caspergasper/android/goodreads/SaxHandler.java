package com.caspergasper.android.goodreads;

import static com.caspergasper.android.goodreads.GoodReadsApp.TAG;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import android.util.Log;


class SaxHandler extends DefaultHandler {
    private StringBuilder builder;
//    private final static String FRIENDS_COUNT = "friends_count";
    private final static String NAME = "name";
    private final static String ACTION_TEXT = "action_text";
    private final static String SHELVES = "shelves";
    private final static String BOOK_COUNT = "book_count";
    private final static String TITLE = "title";
    private final static String DESCRIPTION = "description";
    private final static String AUTHOR = "author";
    private final static String USER_SHELF = "user_shelf";
    private final static String UPDATES = "updates";
    private final static String REVIEWS = "reviews";
//    private final static String REVIEW = "review";
//    private final static String START = "start";
    private final static String END = "end";
    private final static String TOTAL = "total";
    private final static String AVERAGE_RATING = "average_rating";
//    private final static String SMALL_IMG_URL = "small_image_url";
    private final static String LINK = "link";
    private final static String BODY = "body";
    
    private UserData userdata;
    
    private boolean inShelves = false;
    private boolean inAuthor = false;
    private boolean inUpdates = false;
     
    SaxHandler(UserData ud) {
    	userdata = ud;
    	builder = new StringBuilder();
    }
    
    @Override
    public void characters(char[] ch, int start, int length)
            throws SAXException {
        super.characters(ch, start, length);
        builder.append(ch, start, length);
    }

    @Override
    public void endElement(String uri, String localName, String name)
            throws SAXException {
    	super.endElement(uri, localName, name);
        if(localName.equalsIgnoreCase(USER_SHELF)) {
        	inShelves = false;
        } else if(localName.equalsIgnoreCase(BOOK_COUNT)) {
        	userdata.shelves.add(new Shelf()); 
        	userdata.shelves.get(userdata.shelves.size() - 1).total = 
        	Integer.parseInt(builder.toString().trim());    	
        } else if(localName.equalsIgnoreCase(TITLE)) {
        	userdata.temp_books.add(new Book(builder.toString().trim()));
        } else if(localName.equalsIgnoreCase(AUTHOR)) {
        	inAuthor = false;
        } else if(localName.equalsIgnoreCase(DESCRIPTION)) {
        	if(inShelves == false) {
        		userdata.temp_books.get(userdata.temp_books.size() - 1).description 
        		= builder.toString().trim();
        	}
        } else if(localName.equalsIgnoreCase(ACTION_TEXT)) {
        	userdata.updates.add(new Update(builder.toString().trim().replaceAll("</?a[^>]+>", "")));
//            	Log.d(TAG, "value I'm printing: " + builder.toString().trim());
        } else if(localName.equalsIgnoreCase(UPDATES)) {
        	inUpdates = false;
        } else if(localName.equalsIgnoreCase(LINK)) {
        	// Eww, this is disgusting!  *MUST* create a different SAX handler for different
        	// XML files.  
        	if(inShelves == false && inAuthor == false && inUpdates == false &&  
        			userdata.temp_books.get(userdata.temp_books.size() - 1).bookLink == null) {
        		userdata.temp_books.get(userdata.temp_books.size() - 1).setBookLink(builder.toString().trim());
        	}
        } else if(localName.equalsIgnoreCase(NAME)) {
        	if(inShelves) {
        		userdata.shelves.get(userdata.shelves.size() - 1).title 
        		= builder.toString().trim();
        	} else if(inAuthor) {
        		userdata.temp_books.get(userdata.temp_books.size() -1).author += 
        			builder.toString().trim() + " ";
        	} else if(inUpdates) {
        		userdata.updates.get(userdata.updates.size() -1).username = 
        			builder.toString().trim();
        	}
        } else if(localName.equalsIgnoreCase(AVERAGE_RATING)) {
        	userdata.temp_books.get(userdata.temp_books.size() - 1).average_rating = 
        		builder.toString().trim();
//        } else if(localName.equalsIgnoreCase(SMALL_IMG_URL)) {
//        	userdata.books.get(userdata.books.size() - 1).small_image_url =
//        		builder.toString().trim();
        }  else if(localName.equalsIgnoreCase(BODY)) {
        	if(inUpdates) {
        		userdata.updates.get(userdata.updates.size() - 1).body = builder.toString().trim();
        	}
        	
        } else {
//            	Log.d(TAG, "tag: " + localName);
//            	Log.d(TAG, "value: " + builder.toString().trim());
        }
        builder.setLength(0);
    }

    @Override
    public void startDocument() throws SAXException {
        super.startDocument();
    }

    @Override
    public void startElement(String uri, String localName, String name,
            Attributes attributes) throws SAXException {
    	super.startElement(uri, localName, name, attributes);
//       Log.d(TAG, "Start tag: " + localName);
       if(localName.equalsIgnoreCase(AUTHOR)){
        	inAuthor = true;
        } else if(localName.equalsIgnoreCase(USER_SHELF)){
        	inShelves = true;
        } else if(localName.equalsIgnoreCase(UPDATES)){
        	inUpdates = true;
        } else if(localName.equalsIgnoreCase(REVIEWS)){
        	userdata.endBook = Integer.parseInt(attributes.getValue(END));
        	userdata.totalBooks = Integer.parseInt(attributes.getValue(TOTAL));
        }  else if(localName.equalsIgnoreCase(SHELVES)){
        	if(inShelves) {
        		userdata.endShelf = Integer.parseInt(attributes.getValue(END));
        		userdata.totalShelves = Integer.parseInt(attributes.getValue(TOTAL));
        	}
        }
//        else if(localName.equalsIgnoreCase(REVIEW)){
//        	userdata.currentBook++;
//        }
   
    }

}

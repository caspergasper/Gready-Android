package com.caspergasper.android.goodreads;

import static com.caspergasper.android.goodreads.GoodReadsApp.TAG;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import android.util.Log;

class BooksSaxHandler extends DefaultHandler {
    private StringBuilder builder;
    private final static String NAME = "name";
    private final static String TITLE = "title";
    private final static String DESCRIPTION = "description";
    private final static String REVIEW = "review";
    private final static String REVIEWS = "reviews";
    private final static String START = "start";
    private final static String END = "end";
    private final static String TOTAL = "total";
    private final static String AVERAGE_RATING = "average_rating";
    private final static String LINK = "link";
    private final static String SMALL_IMAGE_URL = "small_image_url";
    private final static String AUTHORS = "authors";
    private final static String SHELF = "shelf";
    private final static String ID = "id";
    private final static String TYPE = "type";
    private final static String INTEGER = "integer";
    
    private UserData userdata;
    private boolean inAuthors = false;
    private boolean inId = false;
    private boolean inReview = false;
    private int pos;
    
    BooksSaxHandler(UserData ud) {
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
        pos = userdata.tempBooks.size() - 1;
        if(inId && localName.equalsIgnoreCase(ID)) {
        	userdata.tempBooks.add(new Book(Integer.parseInt(builder.toString().trim())));
        	inId = inReview = false;
        } else if(localName.equalsIgnoreCase(TITLE)) {
        	userdata.tempBooks.get(pos).title = builder.toString().trim();
        } else if(localName.equalsIgnoreCase(DESCRIPTION)) {
    		userdata.tempBooks.get(pos).description 
    		= builder.toString().trim().replaceAll("&lt;/?div&gt;", "");
        } else if(localName.equalsIgnoreCase(LINK)) { 
        	if(userdata.tempBooks.get(pos).bookLink == null) {
        		userdata.tempBooks.get(pos).setBookLink(builder.toString().trim());
        	}
        } else if(localName.equalsIgnoreCase(NAME)) {
        	userdata.tempBooks.get(pos).author += builder.toString().trim() + " ";
        } else if(localName.equalsIgnoreCase(AVERAGE_RATING)) {
        	userdata.tempBooks.get(pos).average_rating = builder.toString().trim();
        } else if(localName.equalsIgnoreCase(SMALL_IMAGE_URL)) {
        	if(!inAuthors) {
        		String url = builder.toString().trim();
        		if(url.substring(0, GoodReadsApp.GOODREADS_IMG_URL_LENGTH).compareTo(
        				GoodReadsApp.GOODREADS_IMG_URL) == 0) {
        			userdata.tempBooks.get(pos).imgUrl = url.substring(GoodReadsApp.GOODREADS_IMG_URL_LENGTH);
//        			Log.d(TAG, "small_image_url:" +
//        					userdata.tempBooks.get(userdata.tempBooks.size() - 1).small_image_url);
        		}
        	}
        } else if(localName.equalsIgnoreCase(AUTHORS)) {
        	inAuthors = false;
        } else {
//            	Log.d(TAG, "tag: " + localName);
//            	Log.d(TAG, "value: " + builder.toString().trim());
        }
        builder.setLength(0);
    }


    @Override
    public void startElement(String uri, String localName, String name,
            Attributes attributes) throws SAXException {
    	super.startElement(uri, localName, name, attributes);
//       Log.d(TAG, "Start tag: " + localName);
        if(localName.equalsIgnoreCase(REVIEWS)){
        	userdata.startBook = Integer.parseInt(attributes.getValue(START));
        	userdata.endBook = Integer.parseInt(attributes.getValue(END));
        	userdata.totalBooks = Integer.parseInt(attributes.getValue(TOTAL));
        } else if(localName.equalsIgnoreCase(REVIEW)){
        	inReview = true;
        } else if(localName.equalsIgnoreCase(AUTHORS)){
        	inAuthors = true;
        } else if(localName.equalsIgnoreCase(SHELF)){
        	userdata.tempBooks.get(pos).shelves.add(attributes.getValue(NAME));
        } else if(localName.equalsIgnoreCase(ID)){
        	String attribute = attributes.getValue(TYPE);
        	if(inReview && attribute != null && attribute.equalsIgnoreCase(INTEGER)) {
        		inId = true;
        	}
        }
    }

}

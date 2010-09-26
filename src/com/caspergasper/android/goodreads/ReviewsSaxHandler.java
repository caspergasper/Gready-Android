package com.caspergasper.android.goodreads;

import static com.caspergasper.android.goodreads.GoodReadsApp.TAG;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import android.util.Log;


class ReviewsSaxHandler extends DefaultHandler {
    private StringBuilder builder;
    private final static String BODY = "body";
    private final static String RATING = "rating";
    private final static String REVIEW = "review";
    private final static String NAME = "name";
    
    private UserData userdata;
    private boolean inReview = false;
    
    ReviewsSaxHandler(UserData ud) {
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
    	int position = userdata.reviews.size() - 1;
    	if(localName.equalsIgnoreCase(BODY)) {
    		if(inReview) {
    			if(userdata.reviews.get(position).rating == 0) {
    				// Delete blank reviews
    				userdata.reviews.remove(position);
    			} else {
    				userdata.reviews.get(position).review = builder.toString().trim();
    			}
    		}
    	} else if(localName.equalsIgnoreCase(NAME)) {
    		if(inReview) {
    			userdata.reviews.add(new Review());
    			userdata.reviews.get(++position).username = builder.toString().trim();
    		} 
    	} else if(localName.equalsIgnoreCase(RATING)) {
    		if(inReview) {
    			userdata.reviews.get(position).rating = Integer.parseInt(builder.toString().trim());
    		}
    	} else if(localName.equalsIgnoreCase(REVIEW)) {
    		inReview = false;
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
    	
    	if(localName.equalsIgnoreCase(REVIEW)){
    		inReview = true;
    	}

    }
    
//    @Override
//    public void startDocument() throws SAXException {
//    	super.startDocument();
//    	
//    }

}

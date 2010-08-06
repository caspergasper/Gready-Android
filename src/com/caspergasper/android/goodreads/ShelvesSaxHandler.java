package com.caspergasper.android.goodreads;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

class ShelvesSaxHandler extends DefaultHandler {
    private StringBuilder builder;

    private final static String NAME = "name";
    private final static String SHELVES = "shelves";
    private final static String BOOK_COUNT = "book_count";
    private final static String END = "end";
    private final static String TOTAL = "total";
    private final static String EXCLUSIVE_FLAG = "exclusive_flag";
    
    private UserData userdata;
    
     ShelvesSaxHandler(UserData ud) {
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
        if(localName.equalsIgnoreCase(BOOK_COUNT)) {
        	userdata.shelves.add(new Shelf()); 
        	userdata.shelves.get(userdata.shelves.size() - 1).total = 
        	Integer.parseInt(builder.toString().trim());    	
        } else if(localName.equalsIgnoreCase(NAME)) {
        		userdata.shelves.get(userdata.shelves.size() - 1).title 
        		= builder.toString().trim();
        } else if(localName.equalsIgnoreCase(EXCLUSIVE_FLAG)) {
        	if(builder.toString().trim().compareTo("true") == 0) {
        		userdata.shelves.get(userdata.shelves.size() - 1).exclusive = true;
        	}
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
        if(localName.equalsIgnoreCase(SHELVES)){
    		userdata.endShelf = Integer.parseInt(attributes.getValue(END));
    		userdata.totalShelves = Integer.parseInt(attributes.getValue(TOTAL));
        }
    }

}

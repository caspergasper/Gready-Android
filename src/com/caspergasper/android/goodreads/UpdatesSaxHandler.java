package com.caspergasper.android.goodreads;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

class UpdatesSaxHandler extends DefaultHandler {
    private StringBuilder builder;
//    private final static String FRIENDS_COUNT = "friends_count";
    private final static String NAME = "name";
    private final static String ACTION_TEXT = "action_text";
//    private final static String SMALL_IMG_URL = "small_image_url";
    private final static String BODY = "body";
    
    private UserData userdata;
     
    UpdatesSaxHandler(UserData ud) {
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
        if(localName.equalsIgnoreCase(ACTION_TEXT)) {
        	userdata.tempUpdates.add(new Update(builder.toString().trim().replaceAll("</?a[^>]+>", "")));
        } else if(localName.equalsIgnoreCase(NAME)) {
        		userdata.tempUpdates.get(userdata.tempUpdates.size() -1).username = 
        			builder.toString().trim();
        }  else if(localName.equalsIgnoreCase(BODY)) {
        		userdata.tempUpdates.get(userdata.tempUpdates.size() - 1).body = builder.toString().trim();        	
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
   
    }

}

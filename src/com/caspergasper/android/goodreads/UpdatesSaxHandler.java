package com.caspergasper.android.goodreads;

import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

class UpdatesSaxHandler extends DefaultHandler {
    private StringBuilder builder;
    private final static String NAME = "name";
    private final static String ACTION_TEXT = "action_text";
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
    	int pos = userdata.tempUpdates.size() - 1;
        if(localName.equalsIgnoreCase(ACTION_TEXT)) {
        	userdata.tempUpdates.add(new Update(builder.toString().trim().replaceAll("</?a[^>]+>", "")));
        } else if(localName.equalsIgnoreCase(NAME)) {
        		userdata.tempUpdates.get(pos).username = builder.toString().trim();
        }  else if(localName.equalsIgnoreCase(BODY)) {
        		userdata.tempUpdates.get(pos).body = builder.toString().trim();        	
        } else {
//            	Log.d(TAG, "tag: " + localName);
//            	Log.d(TAG, "value: " + builder.toString().trim());
        }
        builder.setLength(0);
    }

}

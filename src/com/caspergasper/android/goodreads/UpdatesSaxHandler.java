package com.caspergasper.android.goodreads;

import static com.caspergasper.android.goodreads.GoodReadsApp.TAG;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import android.util.Log;

class UpdatesSaxHandler extends DefaultHandler {
    private StringBuilder builder;
    private final static String NAME = "name";
    private final static String ACTION_TEXT = "action_text";
    private final static String BODY = "body";
    private final static String ID = "id";
    private final static String LINK = "link";
    private final static String UPDATE = "update";
    private final static String TYPE = "type";
    private final static String REVIEW = "review";
    private final static String COMMENT = "comment";
    
    private UserData userdata;
    private boolean inReview = false;
     
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
        }  else if(localName.equalsIgnoreCase(ID)) {
        	userdata.tempUpdates.get(pos).id = Integer.parseInt(builder.toString().trim());
        }  else if(localName.equalsIgnoreCase(LINK)) {
        	if(inReview) {
        		String url = builder.toString().trim();
        		userdata.tempUpdates.get(pos).updateLink = url.substring(url.lastIndexOf('/'));
        		inReview = false;
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
    	if(localName.equalsIgnoreCase(UPDATE)) {
    		if(attributes.getValue(TYPE).equalsIgnoreCase(REVIEW) ||
    				attributes.getValue(TYPE).equalsIgnoreCase(COMMENT)) {
    			inReview = true;
    		} 
//    		Log.d(TAG, "attribval:" + attributes.getValue(TYPE));
    	}
//    	for(int i = 0; i<attributes.getLength(); i++) {
//    		Log.d(TAG, "attrib:" + attributes.getLocalName(i));
//    		Log.d(TAG, "attribval:" + attributes.getValue(i));
//    	}
    	
    }

}

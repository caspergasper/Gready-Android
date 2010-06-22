package com.caspergasper.android.goodreads;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class UserSAXHandler extends DefaultHandler {
  private final static String USER = "user";
  private final static String ID = "id";
  
  private GoodReadsApp appRef;
      
  UserSAXHandler() {
  	appRef = GoodReadsApp.getInstance();
  }
  
  @Override
  public void startElement(String uri, String localName, String name,
          Attributes attributes) throws SAXException {
  	super.startElement(uri, localName, name, attributes);
     
      if(localName.equalsIgnoreCase(USER)) {
      	// Get attribute id for user
      	appRef.userID = Integer.parseInt(attributes.getValue(ID));
      } 
  }

}

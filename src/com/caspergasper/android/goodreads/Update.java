package com.caspergasper.android.goodreads;

public class Update {
	String updateText;
	String username;
	
	Update(String text) {
		updateText = text;
	}
	
	public String toString() {
		// Strip off username if it's included in the update.
		// Can username be null?
		if(username.compareToIgnoreCase(updateText.substring(0, username.length())) 
				== 0) {
			return updateText;
		} else {
			return username + " " + updateText;
		}
	}
}

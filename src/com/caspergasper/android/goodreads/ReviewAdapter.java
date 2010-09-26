package com.caspergasper.android.goodreads;


import java.util.List;

import android.content.Context;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.RatingBar;
import android.widget.TextView;

public class ReviewAdapter extends ArrayAdapter<Review> {

	int resource;
	LayoutInflater vi;
	
	class ReviewViewHolder {
		TextView reviewText;
		TextView userName;
		RatingBar ratingBar;
	}
	
	
	public ReviewAdapter(Context context, int textViewResourceId, List<Review> items) {
        super(context, textViewResourceId, items);
        resource = textViewResourceId;
        vi = (LayoutInflater)getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	}

	@Override
    public View getView(int position, View convertView, ViewGroup parent) {
		ReviewViewHolder holder;  
		
		Review item = getItem(position);
		
        if (convertView == null) {
        	convertView = vi.inflate(resource, null);
            holder = new ReviewViewHolder();
            holder.reviewText = (TextView)convertView.findViewById(R.id.reviewlist_textview);
            holder.userName = (TextView)convertView.findViewById(R.id.reviewlist_username);
            holder.ratingBar = (RatingBar)convertView.findViewById(R.id.rating);
            convertView.setTag(holder);
        } else {
        	holder = (ReviewViewHolder) convertView.getTag();
        }
        
        holder.reviewText.setText(Html.fromHtml(item.review + "<br/><b>"));
        holder.ratingBar.setRating(item.rating);
        holder.userName.setText(item.username);
        return convertView;
    }
}

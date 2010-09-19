package com.caspergasper.android.goodreads;


import java.util.List;

import android.content.Context;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;

public class ShelfAdapter extends ArrayAdapter<Book> {

	int resource;
	LayoutInflater vi;
	
	class ShelfViewHolder {
		TextView text;
		ImageView image;
		RatingBar ratingBar;
	}
	
	
	public ShelfAdapter(Context context, int textViewResourceId, List<Book> items) {
        super(context, textViewResourceId, items);
        resource = textViewResourceId;
        vi = (LayoutInflater)getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	}

	@Override
    public View getView(int position, View convertView, ViewGroup parent) {
		ShelfViewHolder holder;  
		
		Book item = getItem(position);
		
        if (convertView == null) {
        	convertView = vi.inflate(resource, null);
            holder = new ShelfViewHolder();
            holder.text = (TextView)convertView.findViewById(R.id.booklist_textview);
            holder.image = (ImageView)convertView.findViewById(R.id.bookListViewImage);
            holder.ratingBar = (RatingBar)convertView.findViewById(R.id.rating);
            convertView.setTag(holder);
        } else {
        	holder = (ShelfViewHolder) convertView.getTag();
        }
        
        holder.text.setText(Html.fromHtml(item.title + "<br/><b>" + item.author + "</b>"));
        holder.ratingBar.setRating(item.myRating);
        if(item.bitmap != null) {
        	holder.image.setImageBitmap(item.bitmap);
        } else {
        	holder.image.setImageResource(R.drawable.icon);
        }
        return convertView;
    }
}

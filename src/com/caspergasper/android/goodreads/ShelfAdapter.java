package com.caspergasper.android.goodreads;


import java.util.List;

import com.caspergasper.android.goodreads.UpdateAdapter.ViewHolder;

import android.content.Context;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class ShelfAdapter extends ArrayAdapter<Book> {

	int resource;
	LayoutInflater vi;
	
	public ShelfAdapter(Context context, int textViewResourceId, List<Book> items) {
        super(context, textViewResourceId, items);
        resource = textViewResourceId;
        vi = (LayoutInflater)getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	}

	@Override
    public View getView(int position, View convertView, ViewGroup parent) {
		ViewHolder holder;  
		
		Book item = getItem(position);
		
        if (convertView == null) {
        	convertView = vi.inflate(resource, null);
            holder = new ViewHolder();
            holder.text = (TextView)convertView.findViewById(R.id.booklist_textview);
            holder.image = (ImageView)convertView.findViewById(R.id.bookListViewImage);
            convertView.setTag(holder);
        } else {
        	holder = (ViewHolder) convertView.getTag();
        }
        
        holder.text.setText(Html.fromHtml(item.title + "<br/><b>" + item.author + "</b>"));
        if(item.bitmap != null) {
        	holder.image.setImageBitmap(item.bitmap);
        } else {
        	holder.image.setImageResource(R.drawable.icon);
        }
        return convertView;
    }
}

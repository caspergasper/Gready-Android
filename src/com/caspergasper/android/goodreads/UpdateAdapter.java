package com.caspergasper.android.goodreads;

import java.util.List;

import android.content.Context;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class UpdateAdapter extends ArrayAdapter<Update> {

	int resource;
	LayoutInflater vi;
	
	static class ViewHolder {
		TextView text;
		ImageView image;
	}
	
	public UpdateAdapter(Context context, int textViewResourceId, List<Update> items) {
        super(context, textViewResourceId, items);
        resource = textViewResourceId;
        vi = (LayoutInflater)getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	}

	@Override
    public View getView(int position, View convertView, ViewGroup parent) {
		ViewHolder holder;
		Update item = getItem(position);
		
        if (convertView == null) {
            convertView = vi.inflate(resource, null);
            holder = new ViewHolder();
            holder.text = (TextView)convertView.findViewById(R.id.updates_textview);
            holder.image = (ImageView)convertView.findViewById(R.id.bookListViewImage);
            convertView.setTag(holder);
        } else {
        	holder = (ViewHolder) convertView.getTag();
        }
        
        holder.text.setText(Html.fromHtml(item.toString()));

        if(item.bitmap != null) {
        	holder.image.setImageBitmap(item.bitmap);
        } else {
        	holder.image.setImageResource(R.drawable.icon);
        }
//        int height = tv.getMeasuredHeight();
//        if(height > 57) {
//        	tv.setText(tv.getText() + " See more...");
//        } 
//        Log.d(TAG, "text = " + item.toString());
//        Log.d(TAG, "getMeasuredHeight = " + height);
        
        return convertView;
    }
}
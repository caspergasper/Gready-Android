package com.caspergasper.android.goodreads;


import java.util.List;
import android.content.Context;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class ShelfAdapter extends ArrayAdapter<Book> {

	int resource;
	
	public ShelfAdapter(Context context, int textViewResourceId, List<Book> items) {
        super(context, textViewResourceId, items);
        resource = textViewResourceId;
	}

	@Override
    public View getView(int position, View convertView, ViewGroup parent) {
            
		LinearLayout todoView;
		
		Book item = getItem(position);
		
        if (convertView == null) {
            todoView = new LinearLayout(getContext());
        	LayoutInflater vi = (LayoutInflater)getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            vi.inflate(resource, todoView, true);
        } else {
        	todoView = (LinearLayout) convertView;
        }
        
        TextView tv = (TextView)todoView.findViewById(R.id.booklist_textview);
        tv.setText(Html.fromHtml(item.title + "<br/><b>" + item.author + "</b>"));
        ImageView iv = (ImageView)todoView.findViewById(R.id.bookListViewImage);
        if(item.bitmap != null) {
        	iv.setImageBitmap(item.bitmap);
//        	Log.d(TAG, "Setting item " + position + " to image.");
        } else {
        	iv.setImageResource(R.drawable.icon);
//        	Log.d(TAG, "NOT Setting item " + position + " to image.");
        }
        return todoView;
    }
}

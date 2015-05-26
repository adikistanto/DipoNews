package com.istandev.diponews;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

/**
 * Created by ADIK on 26/04/2015.
 */
public class NewsAdapter extends CursorAdapter {

    private static final int VIEW_TYPE_TODAY = 0;

    /**
     * Cache of the children views for a news list item.
     */
    public static class ViewHolder {
        public final TextView dateView;
        public final TextView titleView;

        public ViewHolder(View view) {
            dateView = (TextView) view.findViewById(R.id.list_item_date_textview);
            titleView = (TextView) view.findViewById(R.id.list_item_title_textview);
        }
    }

    public NewsAdapter(Context context, Cursor c, int flags){
        super(context, c, flags);
    }
    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        // Choose the layout type
        int viewType = getItemViewType(cursor.getPosition());
        int layoutId = -1;
        switch (viewType) {
            case VIEW_TYPE_TODAY: {
                layoutId = R.layout.list_item_news;
                break;
            }
        }

        View view = LayoutInflater.from(context).inflate(layoutId, parent, false);

        ViewHolder viewHolder = new ViewHolder(view);
        view.setTag(viewHolder);

        return view;
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        ViewHolder viewHolder = (ViewHolder) view.getTag();
        int viewType = getItemViewType(cursor.getPosition());
        // Read date from cursor
        String dateString = cursor.getString(NewsFragment.COL_PUBLISHDATE);
        // Find TextView and set formatted date on it
        //viewHolder.dateView.setText(dateString);
        viewHolder.dateView.setText(Utility.formatDate(dateString));
        // Read news title from cursor
        String title = cursor.getString(NewsFragment.COL_TITLE);
        // Find TextView and set news title on it
        viewHolder.titleView.setText(title);

    }
}

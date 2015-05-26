package com.istandev.diponews;


import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.ShareActionProvider;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.TextView;

import com.istandev.diponews.data.DipoContract.NewsEntry;
/**
 * Created by ADIK on 26/04/2015.
 */

public class DetailFragment extends Fragment implements LoaderCallbacks<Cursor> {

    private static final String LOG_TAG = DetailFragment.class.getSimpleName();

    private static final String BERITA_SHARE_HASHTAG = "#DipoNews \n";

    public static final String DATE_KEY = "publish_date";
    private static final String UNIV_KEY = "univ_type";

    private ShareActionProvider mShareActionProvider;
    private String mNews;
    private String mUniv;
    private String mID;

    private static final int DETAIL_LOADER = 0;

    private static final String[] NEWS_COLUMNS = {
            NewsEntry.TABLE_NAME + "." + NewsEntry._ID,
            NewsEntry.COLUMN_PUBLISHDATE,
            NewsEntry.COLUMN_TITLE,
            NewsEntry.COLUMN_LINK,
            NewsEntry.COLUMN_CONTENT_SNIPPET,
            NewsEntry.COLUMN_CONTENT
    };

    private TextView mDateView;
    private TextView mTitleView;
    private WebView  mContentView;
    private TextView mLinkView;




    public DetailFragment() {
        setHasOptionsMenu(true);
    }

    @Override
    public void onSaveInstanceState(Bundle outState){
        outState.putString(UNIV_KEY, mUniv);
        Log.v(LOG_TAG, "outstate: " + outState);
        super.onSaveInstanceState(outState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        Bundle arguments = getArguments();
        if (arguments != null) {
            mID = arguments.getString(DetailActivity.ID_KEY);
            Log.v(LOG_TAG, "arguments : " + mID);
        }

        if (savedInstanceState != null) {
            mUniv = savedInstanceState.getString(UNIV_KEY);
            Log.v(LOG_TAG, "arguments si: " + mUniv);
        }

        View rootView = inflater.inflate(R.layout.fragment_detail, container, false);
        mDateView = (TextView) rootView.findViewById(R.id.detail_date_textview);
        mTitleView = (TextView) rootView.findViewById(R.id.detail_title_textview);
        mContentView = (WebView) rootView.findViewById(R.id.detail_content_web);
        mLinkView = (TextView) rootView.findViewById(R.id.detail_link_textview);
        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        Bundle arguments = getArguments();
        if (arguments != null && arguments.containsKey(DetailActivity.ID_KEY) &&
                mUniv != null &&
                !mUniv.equals(Utility.getPreferredLocation(getActivity()))){
            getLoaderManager().restartLoader(DETAIL_LOADER, null, this);
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        Log.v(LOG_TAG, "in onCreateOptionMenu");
        inflater.inflate(R.menu.detailfragment, menu);

        // Retrieve the share menu item
        MenuItem menuItem = menu.findItem(R.id.action_share);

        // Get the provider and hold onto it to set/change the share intent.
        mShareActionProvider = (ShareActionProvider) MenuItemCompat.getActionProvider(menuItem);

        // Attach an intent to this ShareActionProvider. You can update this at any time,
        // like when the user selects a new piece of data they might like to share.
        if (mShareActionProvider != null ) {
            // If onLoadFinished happens before this, we can go ahead and set the share intent now.
            mShareActionProvider.setShareIntent(createShareNewsIntent());
       }
    }

    private Intent createShareNewsIntent() {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, BERITA_SHARE_HASHTAG + mNews );
        return shareIntent;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (savedInstanceState != null) {
            mUniv = savedInstanceState.getString(UNIV_KEY);
        }
        Bundle arguments = getArguments();
        if (arguments != null && arguments.containsKey(DetailActivity.ID_KEY)) {
            getLoaderManager().initLoader(DETAIL_LOADER, null, this);
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        Log.v(LOG_TAG, "In onCreateLoader");
        // Sort order:  Ascending, by id.
        String sortOrder = NewsEntry._ID + " ASC";


        String mID = getArguments().getString(DetailActivity.ID_KEY);

        mUniv = Utility.getPreferredLocation(getActivity());

        Uri newsForLocationUri = NewsEntry.buildNewsUnivWithID(mID);

        Log.v(LOG_TAG, "URI: " + newsForLocationUri.toString());

        // Now create and return a CursorLoader that will take care of
        // creating a Cursor for the data being displayed.
        return new CursorLoader(
                getActivity(),
                newsForLocationUri,
                NEWS_COLUMNS,
                null,
                null,
                sortOrder
        );
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        Log.v(LOG_TAG, "In onLoadFinished");

        if (data != null && data.moveToFirst()){

            String dateString = data.getString(data.getColumnIndex(NewsEntry.COLUMN_PUBLISHDATE));
            mDateView.setText(dateString);
            String titleString = data.getString(
                    data.getColumnIndex(NewsEntry.COLUMN_TITLE));
            mTitleView.setText(titleString);
            String contentString = data.getString(
                    data.getColumnIndex(NewsEntry.COLUMN_CONTENT));
            mContentView.loadDataWithBaseURL(null, contentString, "text/html", "UTF-8", null);
            String linkString = data.getString(
                    data.getColumnIndex(NewsEntry.COLUMN_LINK));
            mLinkView.setText(linkString);
            String contentSnippetString = data.getString(
                    data.getColumnIndex(NewsEntry.COLUMN_CONTENT_SNIPPET));


            // We still need this for the share intent
            mNews = String.format("%s \n %s \n\n %s \n\n\n %s", dateString, titleString, contentSnippetString , linkString);

            Log.v(LOG_TAG, "News String: " + mNews);

            // If onCreateOptionsMenu has already happened, we need to update the share intent now.
            if (mShareActionProvider != null) {
                mShareActionProvider.setShareIntent(createShareNewsIntent());
            }
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

    }
}

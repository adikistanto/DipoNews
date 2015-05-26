package com.istandev.diponews;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SimpleCursorAdapter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import com.istandev.diponews.data.DipoContract.NewsEntry;

//import android.widget.SimpleCursorAdapter;

/**
 * Created by ADIK on 24/04/2015.
 */
public class NewsFragment extends Fragment implements LoaderCallbacks<Cursor> {
    private static final String LOG_TAG = NewsFragment.class.getSimpleName();
    private SimpleCursorAdapter mNewsAdapter;

    private static final int NEWS_LOADER = 0;
    private ListView mListView;
    private int mPosition = ListView.INVALID_POSITION;
    private String mUniv;

    private static final String SELECTED_KEY = "selected_position";

    // For the news view we're showing only a small subset of the stored data.
    // Specify the columns we need.

    private static final String[] NEWS_COLUMNS = {

            NewsEntry.TABLE_NAME + "." + NewsEntry._ID,
            NewsEntry.COLUMN_PUBLISHDATE,
            NewsEntry.COLUMN_TITLE,
            NewsEntry.COLUMN_LINK,
            NewsEntry.COLUMN_CONTENT_SNIPPET,
            NewsEntry.COLUMN_CONTENT
    };


    // These indices are tied to NEWS_COLUMNS. If NEWS_COLUMNS changes, these
    // must change.
    public static final int COL_NEWS_ID = 0;
    public static final int COL_PUBLISHDATE = 1;
    public static final int COL_TITLE = 2;
    public static final int COL_LINK = 3;
    public static final int COL_AUTHOR = 4;
    public static final int COL_CONTENT = 5;

    /**
     * A callback interface that all activities containing this fragment must
     * implement. This mechanism allows activities to be notified of item
     * selections.
     */
    public interface Callback {
        /**
         * DetailFragmentCallback for when an item has been selected.
         */
        public void onItemSelected(String id);
    }

    public NewsFragment() {
    }
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        getLoaderManager().initLoader(NEWS_LOADER, null, this);
        super.onActivityCreated(savedInstanceState);
    }
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Add this line in order for this fragment to handle menu events.
        setHasOptionsMenu(true);

    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.newsfragment, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            Intent intent = new Intent(getActivity(),SettingActivity.class);
            startActivity(intent);
            return true;}
        return super.onOptionsItemSelected(item);
    }
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // Create and populate a List of planet names.
        // Create some dummy data for the ListView.  Here's a sample weekly forecast
               // The ArrayAdapter will take data from a source (like our dummy forecast) and
        // use it to populate the ListView it's attached to.

        mNewsAdapter = new SimpleCursorAdapter(
                getActivity(),
                R.layout.list_item_news,
                null,
                // the column names to use to fill the textviews
                new String[]{NewsEntry.COLUMN_PUBLISHDATE,
                        NewsEntry.COLUMN_TITLE
                },
                // the textviews to fill with the data pulled from the columns above
                new int[]{R.id.list_item_date_textview,
                        R.id.list_item_title_textview
                },
                0
        );

        final View rootView = inflater.inflate(R.layout.fragment_main, container, false);

        // Get a reference to the ListView, and attach this adapter to it.
        ListView listView = (ListView) rootView.findViewById(R.id.listView_news);
        listView.setAdapter(mNewsAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                //String news = mNewsAdapter.getItem(position);
                //Toast.makeText(getActivity(), news, Toast.LENGTH_SHORT).show();
                Cursor cursor = mNewsAdapter.getCursor();
                if(cursor != null && cursor.moveToPosition(position)){
                    ((Callback)getActivity())
                            .onItemSelected(cursor.getString(COL_NEWS_ID));
                }
                mPosition = position;
                Log.v(LOG_TAG,"position :" + position);

            }
        });

        // If there's instance state, mine it for useful information.
        // The end-goal here is that the user never knows that turning their device sideways
        // does crazy lifecycle related things.  It should feel like some stuff stretched out,
        // or magically appeared to take advantage of room, but data or place in the app was never
        // actually *lost*.
        if (savedInstanceState != null && savedInstanceState.containsKey(SELECTED_KEY)) {
            // The listview probably hasn't even been populated yet.  Actually perform the
            // swapout in onLoadFinished.
            mPosition = savedInstanceState.getInt(SELECTED_KEY);
        }

        return rootView;
    }
    private void updateNews() {
        String undip = "http://undip.ac.id/index.php?format=feed&type=rss";
        String ui = "http://www.ui.ac.id/feed/";
        String itb = "http://www.itb.ac.id/news/rss";
        String ugm = "http://www.ugm.ac.id/id/news/feed.xml";
        String unibraw = "http://www.ub.ac.id/berita/rss";
        String unpad = "http://www.unpad.ac.id/feed/";
        String ipb = "http://news.ipb.ac.id/news/rss?culture=id";

        String univType = Utility.getPreferredLocation(getActivity());
        new FecthingNews(getActivity()).execute(univType);

        }

    @Override
    public void onStart() {
           super.onStart();
          updateNews();

    }
    @Override
    public void onResume() {
        super.onResume();
        if (mUniv != null && !mUniv.equals(Utility.getPreferredLocation(getActivity()))){
            getLoaderManager().restartLoader(NEWS_LOADER, null, this);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        // When tablets rotate, the currently selected list item needs to be saved.
        // When no item is selected, mPosition will be set to Listview.INVALID_POSITION,
        // so check for that before storing.
        if (mPosition != ListView.INVALID_POSITION) {
            outState.putInt(SELECTED_KEY, mPosition);
        }
        super.onSaveInstanceState(outState);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        // This is called when a new Loader needs to be created.  This
        // fragment only uses one loader, so we don't care about checking the id.

        // Sort order:  Ascending, by date.
        String sortOrder = NewsEntry._ID + " ASC";

        Uri newsForUnivUri = NewsEntry.buildNewsUniv();
        Log.v(LOG_TAG, "URI: " + newsForUnivUri);

        // Now create and return a CursorLoader that will take care of
        // creating a Cursor for the data being displayed.
        return new CursorLoader(
                getActivity(),
                newsForUnivUri,
                NEWS_COLUMNS,
                null,
                null,
                sortOrder
        );
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        mNewsAdapter.swapCursor(data);
        if (mPosition != ListView.INVALID_POSITION) {
            // If we don't need to restart the loader, and there's a desired position to restore
            // to, do so now.
            //mListView.smoothScrollToPosition(mPosition);
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mNewsAdapter.swapCursor(null);
    }


}

package com.istandev.diponews;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.istandev.diponews.sync.NewsSyncAdapter;

/**
 * Created by ADIK on 25/04/2015.
 */
public class MainActivity extends ActionBarActivity implements NewsFragment.Callback {
    private final String LOG_TAG = MainActivity.class.getSimpleName();

    private boolean mTwoPane;
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        Log.v(LOG_TAG, "in onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (findViewById(R.id.news_detail_container) != null) {
            // The detail container view will be present only in the large-screen layouts
            // (res/layout-sw600dp). If this view is present, then the activity should be
            // in two-pane mode.
            mTwoPane = true;

            // In two-pane mode, show the detail view in this activity by
            // adding or replacing the detail fragment using a
            // fragment transaction.
            if (savedInstanceState == null) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.news_detail_container, new DetailFragment())
                        .commit();
            }
        } else {
            mTwoPane = false;
        }

      NewsSyncAdapter.initializeSyncAdapter(this);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setIcon(R.drawable.ic_launcher);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.

        return super.onOptionsItemSelected(item);
    }


    @Override
    public void onItemSelected(String id) {
        if (mTwoPane){
            // In two-pane mode, show the detail view in this activity by
            // adding or replacing the dtail fragment using a
            // fragment transaction
            Bundle args = new Bundle();
            args.putString(DetailActivity.ID_KEY, id);

            DetailFragment fragment = new DetailFragment();
            fragment.setArguments(args);

            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.news_detail_container, fragment)
                    .commit();
        }else {
            Intent intent = new Intent(this, DetailActivity.class)
                    .putExtra(DetailActivity.ID_KEY, id);
            startActivity(intent);
        }
    }
}

package com.istandev.diponews.sync;



import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SyncRequest;
import android.content.SyncResult;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.util.Log;

import com.istandev.diponews.MainActivity;
import com.istandev.diponews.R;
import com.istandev.diponews.Utility;
import com.istandev.diponews.data.DipoContract;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Vector;

/**
 * Created by ADIK on 27/04/2015.
 */
public class NewsSyncAdapter extends AbstractThreadedSyncAdapter {
    public final String LOG_TAG = NewsSyncAdapter.class.getSimpleName();

    // Interval at which to sync with the news, in milliseconds.
    // 60 seconds (1 minute) * 180 = 3 hours
    public static final int SYNC_INTERVAL = 60 * 180;
    public static final int SYNC_FLEXTIME = SYNC_INTERVAL/3;

    private static final long DAY_IN_MILLIS = 1000 * 60 * 60 * 24;
    private static final int NEWS_NOTIFICATION_ID = 3076;

    private static final String[] NOTIFY_NEWS_PROJECTION = new String[] {
            DipoContract.NewsEntry._ID,
            DipoContract.NewsEntry.COLUMN_PUBLISHDATE,
            DipoContract.NewsEntry.COLUMN_TITLE
    };

    // these indices must match the projection
    private static final int INDEX_ID = 0;
    private static final int INDEX_PUBLISH_DATE = 1;
    private static final int INDEX_TITLE = 2;

    public NewsSyncAdapter(Context context, boolean autoInitialize) {
        super(context, autoInitialize);
    }

    // brings our database to an empty state
    public void deleteAllRecords() {
        getContext().getContentResolver().delete(
                DipoContract.NewsEntry.CONTENT_URI,
                null,
                null
        );

        Cursor cursor = getContext().getContentResolver().query(
                DipoContract.NewsEntry.CONTENT_URI,
                null,
                null,
                null,
                null
        );
        cursor.close();
    }

    private void notifyNews() {
        Context context = getContext();
        //checking the last update and notify if it' the first of the day
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String displayNotificationsKey = context.getString(R.string.pref_enable_notifications_key);
        boolean displayNotifications = prefs.getBoolean(displayNotificationsKey,
                Boolean.parseBoolean(context.getString(R.string.pref_enable_notifications_default)));
        if (displayNotifications){

            String lastNotificationKey = context.getString(R.string.pref_last_notification);
            long lastSync = prefs.getLong(lastNotificationKey, 0);

            if (System.currentTimeMillis() - lastSync >= DAY_IN_MILLIS) {
                // Last sync was more than 1 day ago, let's send a notification with the news.
                String locationQuery = Utility.getPreferredLocation(context);

                Uri newsUri = DipoContract.NewsEntry.buildNewsUniv();

                // we'll query our contentProvider, as always
                Cursor cursor = context.getContentResolver().query(newsUri, NOTIFY_NEWS_PROJECTION, null, null, null);

                if (cursor.moveToFirst()) {
                    int newsId = cursor.getInt(INDEX_ID);
                    String univType = Utility.getPreferredLocation(getContext());
                    String date = cursor.getString(INDEX_PUBLISH_DATE);
                    String title_news = cursor.getString(INDEX_TITLE);

                    int iconId = R.drawable.ic_launcher1;;
                    String title = context.getString(R.string.app_name);

                    // Define the text of the news.
                    String contentText = String.format(context.getString(R.string.format_notification),
                            date,
                            title_news);

                    // NotificationCompatBuilder is a very convenient way to build backward-compatible
                    // notifications.  Just throw in some data.
                    NotificationCompat.Builder mBuilder =
                            new NotificationCompat.Builder(getContext())
                                    .setSmallIcon(iconId)
                                    .setContentTitle(title)
                                    .setContentText(contentText);

                    // Make something interesting happen when the user clicks on the notification.
                    // In this case, opening the app is sufficient.
                    Intent resultIntent = new Intent(context, MainActivity.class);

                    // The stack builder object will contain an artificial back stack for the
                    // started Activity.
                    // This ensures that navigating backward from the Activity leads out of
                    // your application to the Home screen.
                    TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
                    stackBuilder.addNextIntent(resultIntent);
                    PendingIntent resultPendingIntent =
                            stackBuilder.getPendingIntent(
                                    0,
                                    PendingIntent.FLAG_UPDATE_CURRENT
                            );
                    mBuilder.setContentIntent(resultPendingIntent);

                    NotificationManager mNotificationManager =
                            (NotificationManager) getContext().getSystemService(Context.NOTIFICATION_SERVICE);
                    // NEWS_NOTIFICATION_ID allows you to update the notification later on.
                    mNotificationManager.notify(NEWS_NOTIFICATION_ID, mBuilder.build());


                    //refreshing last sync
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putLong(lastNotificationKey, System.currentTimeMillis());
                    editor.commit();
                }
            }
        }

    }

    @Override
    public void onPerformSync(Account account, Bundle bundle, String s, ContentProviderClient contentProviderClient, SyncResult syncResult) {
        Log.d(LOG_TAG, "Starting sync");
        String univQuery = Utility.getPreferredLocation(getContext());

        // These two need to be declared outside the try/catch
        // so that they can be closed in the finally block.
        HttpURLConnection urlConnection = null;
        BufferedReader reader = null;

        // Will contain the raw JSON response as a string.
        String feedJsonStr = null;
        int numList = 10;

        try {
            // Construct the URL for the Google Feed Api's  query
            // http://ajax.googleapis.com/ajax/services/feed/load?v=1.0/#newsfeed

            final String NEWS_BASE_URL =
                    "http://ajax.googleapis.com/ajax/services/feed/load?v=1.0";
            String QUERY_PARAM = "q";
            String NUM_PARAM = "num";

            Uri builtUri = Uri.parse(NEWS_BASE_URL).buildUpon()
                    .appendQueryParameter(QUERY_PARAM, univQuery)
                    .appendQueryParameter(NUM_PARAM, Integer.toString(numList))
                    .build();

            URL url = new URL(builtUri.toString());

            Log.v(LOG_TAG, "Built URI News " + url);

            // Create the request to Google Feed Api, and open the connection
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("GET");
            urlConnection.connect();

            // Read the input stream into a String
            InputStream inputStream = urlConnection.getInputStream();
            StringBuffer buffer = new StringBuffer();
            if (inputStream == null) {
                // Nothing to do.
                return;
            }
            reader = new BufferedReader(new InputStreamReader(inputStream));

            String line;
            while ((line = reader.readLine()) != null) {
                // Since it's JSON, adding a newline isn't necessary (it won't affect parsing)
                // But it does make debugging a *lot* easier if you print out the completed
                // buffer for debugging.
                buffer.append(line + "\n");
            }

            if (buffer.length() == 0) {
                // Stream was empty.  No point in parsing.
                return;
            }
            feedJsonStr = buffer.toString();
        } catch (IOException e) {
            Log.e(LOG_TAG, "Error ", e);
            // If the code didn't successfully get the news data, there's no point in attemping
            // to parse it.
            return;
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
            if (reader != null) {
                try {
                    reader.close();
                } catch (final IOException e) {
                    Log.e(LOG_TAG, "Error closing stream", e);
                }
            }
        }


        final String FM_RESPONSEDATA = "responseData";
        final String FM_FEED = "feed";
        final String FM_ENTRIES = "entries";
        final String FM_TITLE = "title";
        final String FM_CONTENTSNIPPET = "contentSnippet";
        final String FM_DATE = "publishedDate";
        final String FM_CONTENT = "content";
        final String FM_LINK = "link";

        try {
            JSONObject jsonObject = new JSONObject(feedJsonStr);

            JSONObject jsonObject_responseData = jsonObject.getJSONObject(FM_RESPONSEDATA);
            JSONObject jsonObject_feed = jsonObject_responseData.getJSONObject(FM_FEED);
            JSONArray jsonArray_result = jsonObject_feed.getJSONArray(FM_ENTRIES);

            Vector<ContentValues> cVVector = new Vector<ContentValues>(jsonArray_result.length());

            //String[] resultStrs = new String[numList];
            for (int i = 0; i < jsonArray_result.length(); i++) {
                // For now, using the format "Day, description, hi/low"
                //String day;
                String title;
                String link;
                String content;
                String published;
                String contentSnippet;

                JSONObject listNews = jsonArray_result.getJSONObject(i);

                published = listNews.getString(FM_DATE);
                title = listNews.getString(FM_TITLE);
                link = listNews.getString(FM_LINK);
                content = listNews.getString(FM_CONTENT);
                contentSnippet = listNews.getString(FM_CONTENTSNIPPET);

                ContentValues newsValues = new ContentValues();

                newsValues.put(DipoContract.NewsEntry.COLUMN_PUBLISHDATE, published);
                newsValues.put(DipoContract.NewsEntry.COLUMN_TITLE, title);
                newsValues.put(DipoContract.NewsEntry.COLUMN_LINK, link);
                newsValues.put(DipoContract.NewsEntry.COLUMN_CONTENT, content);
                newsValues.put(DipoContract.NewsEntry.COLUMN_CONTENT_SNIPPET, contentSnippet);

                cVVector.add(newsValues);

            }

            if (cVVector.size() > 0) {
                ContentValues[] cvArray = new ContentValues[cVVector.size()];
                cVVector.toArray(cvArray);
                getContext().getContentResolver().bulkInsert(DipoContract.NewsEntry.CONTENT_URI, cvArray);
                deleteAllRecords();
                notifyNews();
            }
            Log.d(LOG_TAG, "FetchNewsTask Complete. " + cVVector.size() + " Inserted");

        } catch (JSONException e) {
            Log.e(LOG_TAG, e.getMessage(), e);
            e.printStackTrace();
        }
        // This will only happen if there was an error getting or parsing the feed url.
        return;
    }


    /**
     * Helper method to have the sync adapter sync immediately
     * @param context The context used to access the account service
     */
    public static void syncImmediately(Context context) {
        Bundle bundle = new Bundle();
        bundle.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true);
        bundle.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
        ContentResolver.requestSync(getSyncAccount(context),
                context.getString(R.string.content_authority), bundle);
    }

    /**
     * Helper method to get the fake account to be used with SyncAdapter, or make a new one
     * if the fake account doesn't exist yet.  If we make a new account, we call the
     * onAccountCreated method so we can initialize things.
     *
     * @param context The context used to access the account service
     * @return a fake account.
     */
    public static Account getSyncAccount(Context context) {
        // Get an instance of the Android account manager
        AccountManager accountManager =
                (AccountManager) context.getSystemService(Context.ACCOUNT_SERVICE);

        // Create the account type and default account
        Account newAccount = new Account(
                context.getString(R.string.app_name), context.getString(R.string.sync_account_type));

        // If the password doesn't exist, the account doesn't exist
        if ( null == accountManager.getPassword(newAccount) ) {

        /*
         * Add the account and account type, no password or user data
         * If successful, return the Account object, otherwise report an error.
         */
            if (!accountManager.addAccountExplicitly(newAccount, "", null)) {
                return null;
            }
            /*
             * If you don't set android:syncable="true" in
             * in your <provider> element in the manifest,
             * then call ContentResolver.setIsSyncable(account, AUTHORITY, 1)
             * here.
             */

            onAccountCreated(newAccount, context);

        }
        return newAccount;

    }

    /**
     * Helper method to schedule the sync adapter periodic execution
     */
    public static void configurePeriodicSync(Context context, int syncInterval, int flexTime) {
        Account account = getSyncAccount(context);
        String authority = context.getString(R.string.content_authority);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            // we can enable inexact timers in our periodic sync
            SyncRequest request = new SyncRequest.Builder().
                    syncPeriodic(syncInterval, flexTime).
                    setSyncAdapter(account, authority).build();
            ContentResolver.requestSync(request);
        } else {
            ContentResolver.addPeriodicSync(account,
                    authority, new Bundle(), syncInterval);
        }
    }


    private static void onAccountCreated(Account newAccount, Context context) {
        NewsSyncAdapter.configurePeriodicSync(context, SYNC_INTERVAL, SYNC_FLEXTIME);
        ContentResolver.setSyncAutomatically(newAccount, context.getString(R.string.content_authority), true);
        syncImmediately(context);
    }

    public static void initializeSyncAdapter(Context context) {
        getSyncAccount(context);
    }
}

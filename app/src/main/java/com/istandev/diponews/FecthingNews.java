package com.istandev.diponews;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;

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
 * Created by ADIK on 25/04/2015.
 */
public class FecthingNews extends AsyncTask<String, Void, String[]> {

    private final String LOG_TAG = FecthingNews.class.getSimpleName();

    private final Context mContext;

    public FecthingNews(Context context) {
        mContext = context;
    }

    public void deleteAllRecords() {
        mContext.getContentResolver().delete(
                DipoContract.NewsEntry.CONTENT_URI,
                null,
                null
        );

        Cursor cursor = mContext.getContentResolver().query(
                DipoContract.NewsEntry.CONTENT_URI,
                null,
                null,
                null,
                null
        );
        cursor.close();
    }

    private boolean DEBUG = true;

    private void getNewsDataFromJson(String feedJsonStr, int numNews)
            throws JSONException {

        final String FM_RESPONSEDATA = "responseData";
        final String FM_FEED = "feed";
        final String FM_ENTRIES = "entries";
        final String FM_TITLE = "title";
        final String FM_CONTENTSNIPPET = "contentSnippet";
        final String FM_DATE = "publishedDate";
        final String FM_CONTENT = "content";
        final String FM_LINK = "link";

        JSONObject jsonObject = new JSONObject(feedJsonStr);


        JSONObject jsonObject_responseData = jsonObject.getJSONObject(FM_RESPONSEDATA);
        JSONObject jsonObject_feed = jsonObject_responseData.getJSONObject(FM_FEED);
        JSONArray jsonArray_result = jsonObject_feed.getJSONArray(FM_ENTRIES);

        Vector<ContentValues> cVVector = new Vector<ContentValues>(jsonArray_result.length());

        for (int i = 0; i < jsonArray_result.length(); i++) {
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
            mContext.getContentResolver().bulkInsert(DipoContract.NewsEntry.CONTENT_URI, cvArray);
            deleteAllRecords();
            Log.v(LOG_TAG, "All databases was deleted");
            int rowsInserted = mContext.getContentResolver()
                    .bulkInsert(DipoContract.NewsEntry.CONTENT_URI, cvArray);

            Log.v(LOG_TAG, "inserted new " + rowsInserted + " rows of news data");
        }
    }

    @Override
    protected String[] doInBackground(String... params) {
        if(params.length==0){
            return null;
        }

        HttpURLConnection urlConnection = null;
        BufferedReader reader = null;

        // Will contain the raw JSON response as a string.
        String forecastJsonStr = null;
        int numNews = 10;

        try {
            final String NEWS_BASE_URL =
                    "http://ajax.googleapis.com/ajax/services/feed/load?v=1.0";
            String QUERY_PARAM = "q";
            String NUM_PARAM = "num";

            Uri builtUri = Uri.parse(NEWS_BASE_URL).buildUpon()
                    .appendQueryParameter(QUERY_PARAM, params[0])
                    .appendQueryParameter(NUM_PARAM, Integer.toString(numNews))
                    .build();

            URL url = new URL(builtUri.toString());

            // Create the request to OpenWeatherMap, and open the connection
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("GET");
            urlConnection.connect();

            // Read the input stream into a String
            InputStream inputStream = urlConnection.getInputStream();
            StringBuffer buffer = new StringBuffer();
            if (inputStream == null) {
                return null;
            }
            reader = new BufferedReader(new InputStreamReader(inputStream));
            String line;
            while ((line = reader.readLine()) != null) {
                buffer.append(line + "\n");
            }

            if (buffer.length() == 0) {
                return null;
            }
            forecastJsonStr = buffer.toString();
            Log.v(LOG_TAG, "news jason" + forecastJsonStr);

        } catch (IOException e) {
            Log.e(LOG_TAG, "Error ", e);
            // If the code didn't successfully get the weather data, there's no point in attemping
            // to parse it.
            return null;
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
        try {
            getNewsDataFromJson(forecastJsonStr, numNews);
        }
        catch (JSONException e) {
            Log.e(LOG_TAG, e.getMessage(), e);
            e.printStackTrace();
        }
        return null;
    }
}
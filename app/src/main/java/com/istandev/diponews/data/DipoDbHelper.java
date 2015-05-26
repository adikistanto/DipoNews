package com.istandev.diponews.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.istandev.diponews.data.DipoContract.NewsEntry;

/**
 * Created by ADIK on 26/04/2015.
 */
public class DipoDbHelper extends SQLiteOpenHelper {

    private static final int DATABASE_VERSION = 1;

    public static final String DATABASE_NAME = "news.db";

    public DipoDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        // Create a table to hold locations. A location consists of the string supplied in the
        // location setting, the city name, and the latitude and longitude
        // TBD
        final String SQL_CREATE_NEWS_TABLE = "CREATE TABLE " + NewsEntry.TABLE_NAME + " (" +
                NewsEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                // the ID of the location entry associated with this news data
                NewsEntry.COLUMN_TITLE + " TEXT NOT NULL, " +
                NewsEntry.COLUMN_LINK + " TEXT NOT NULL, " +
                NewsEntry.COLUMN_CONTENT_SNIPPET + " TEXT NOT NULL, " +
                NewsEntry.COLUMN_PUBLISHDATE + " TEXT NOT NULL," +
                NewsEntry.COLUMN_CONTENT + " TEXT NOT NULL);";

        sqLiteDatabase.execSQL(SQL_CREATE_NEWS_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int oldVersion, int newVersion) {
        // This database is only a cache for online data, so its upgrade policy is
        // to simply to discard the data and start over
        // Note that this only fires if you change the version number for your database.
        // It does NOT depend on the version number for your application.
        // If you want to update the schema without wiping data, commenting out the next 2 lines
        // should be your top priority before modifying this method.
        sqLiteDatabase.execSQL(" DROP TABLE IF EXIST " + NewsEntry.TABLE_NAME);
        onCreate(sqLiteDatabase);
    }
}
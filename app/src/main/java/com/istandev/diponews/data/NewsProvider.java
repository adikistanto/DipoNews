package com.istandev.diponews.data;


import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
/**
 * Created by ADIK on 26/04/2015.
 */
public class NewsProvider extends ContentProvider {

    // The URI Matcher used by this content provider.
    private static final UriMatcher sUriMatcher = buildUriMatcher();
    private DipoDbHelper mOpenHelper;

    private static final int NEWS = 100;
    private static final int NEWS_BY_UNIV = 101;
    private static final int NEWS_BY_UNIV_AND_DATE = 102;
    private static final int UNIV = 300;

    private static final SQLiteQueryBuilder sNewsByUnivSettingQueryBuilder;

    static {
        sNewsByUnivSettingQueryBuilder = new SQLiteQueryBuilder();
        sNewsByUnivSettingQueryBuilder.setTables(
                DipoContract.NewsEntry.TABLE_NAME
        );
    }

    private static final String sUnivSettingSelection =
            DipoContract.NewsEntry.TABLE_NAME+
                    "." + DipoContract.NewsEntry._ID + " = ? ";

    private Cursor getNewsByUnivSetting(Uri uri, String[] projection, String sortOrder) {
        String univSetting = DipoContract.NewsEntry.getUnivSettingFromUri(uri);
        //String startDate = DipoContract.NewsEntry.getStartDateFromUri(uri);

        String[] selectionArgs;
        String selection;



            selectionArgs = new String[]{univSetting};
            selection = sUnivSettingSelection;

        return sNewsByUnivSettingQueryBuilder.query(mOpenHelper.getReadableDatabase(),
                projection,
                selection,
                selectionArgs,
                null,
                null,
                sortOrder
        );
    }

    private static UriMatcher buildUriMatcher() {

        final UriMatcher matcher = new UriMatcher(UriMatcher.NO_MATCH);
        final String authority = DipoContract.CONTENT_AUTHORITY;

        // For each type of URI you want to add, create a corresponding code.
        matcher.addURI(authority, DipoContract.PATH_NEWS, NEWS);
        matcher.addURI(authority, DipoContract.PATH_NEWS + "/*", NEWS_BY_UNIV);
        matcher.addURI(authority, DipoContract.PATH_NEWS + "/*/*", NEWS_BY_UNIV_AND_DATE);


        return matcher;
    }

    @Override
    public boolean onCreate() {
        mOpenHelper = new DipoDbHelper(getContext());
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
                        String sortOrder) {

        // Here's the switch statement that, given a URI, will determine what kind of request it is,
        // and query the database accordingly.
        Cursor retCursor;
        switch (sUriMatcher.match(uri)) {
            // "news/*"
            case NEWS_BY_UNIV: {
                retCursor = getNewsByUnivSetting(uri, projection, sortOrder);
                break;
            }
            // "news"
            case NEWS: {
                retCursor = mOpenHelper.getReadableDatabase().query(
                        DipoContract.NewsEntry.TABLE_NAME,
                        projection,
                        selection,
                        selectionArgs,
                        null,
                        null,
                        sortOrder
                );
                break;
            }

            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }

        retCursor.setNotificationUri(getContext().getContentResolver(), uri);
        return retCursor;
    }

    @Override
    public String getType(Uri uri) {

        // Use the Uri Matcher to determine what kind of URI this is.
        final int match = sUriMatcher.match(uri);

        switch (match) {
            // "news"
            case NEWS:
                return DipoContract.NewsEntry.CONTENT_TYPE;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }

    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        final int match = sUriMatcher.match(uri);
        Uri returnUri;
        switch (match) {
            case NEWS: {
                long _id = db.insert(DipoContract.NewsEntry.TABLE_NAME, null, values);
                if ( _id > 0 )
                    returnUri = DipoContract.NewsEntry.buildNewsUri(_id);
                else
                    throw new android.database.SQLException("Failed to insert row into " + uri);
                break;
            }
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return returnUri;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        final int match = sUriMatcher.match(uri);
        int rowsDeleted;
        switch (match) {
            case NEWS:
                rowsDeleted = db.delete(
                        DipoContract.NewsEntry.TABLE_NAME, selection, selectionArgs);
                break;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
        // Because a null deletes all rows
        if (selection == null || rowsDeleted != 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return rowsDeleted;
    }

    @Override
    public int update(
            Uri uri, ContentValues values, String selection, String[] selectionArgs) {

        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        final int match = sUriMatcher.match(uri);
        int rowsUpdated;

        switch (match) {
            case NEWS:
                rowsUpdated = db.update(DipoContract.NewsEntry.TABLE_NAME, values, selection,
                        selectionArgs);
                break;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
        if (rowsUpdated != 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return rowsUpdated;
    }

    @Override
    public int bulkInsert(Uri uri, ContentValues[] values) {
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case NEWS:
                db.beginTransaction();
                int returnCount = 0;
                try {
                    for (ContentValues value : values) {
                        long _id = db.insert(DipoContract.NewsEntry.TABLE_NAME, null, value);
                        if (_id != -1) {
                            returnCount++;
                        }
                    }
                    db.setTransactionSuccessful();
                } finally {
                    db.endTransaction();
                }
                getContext().getContentResolver().notifyChange(uri, null);
                return returnCount;
            default:
                return super.bulkInsert(uri, values);
        }
    }
}

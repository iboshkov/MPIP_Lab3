package tech.boshkov.lab03.Data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class PlacesDbHelper extends SQLiteOpenHelper {

    private static final String TEXT_TYPE = " TEXT";
    private static final String REAL_TYPE = " REAL";
    private static final String COMMA_SEP = ",";
    private static final String SQL_CREATE_ENTRIES =
            "CREATE TABLE " + PlacesEntry.TABLE_NAME + " (" +
                    PlacesEntry._ID + " INTEGER PRIMARY KEY," +
                    PlacesEntry.COLUMN_NAME_GMAPS_ID + TEXT_TYPE + " UNIQUE," +
                    PlacesEntry.COLUMN_NAME_TITLE + TEXT_TYPE + COMMA_SEP +
                    PlacesEntry.COLUMN_NAME_ADDRESS + TEXT_TYPE + COMMA_SEP +
                    PlacesEntry.COLUMN_NAME_LONGITUDE + REAL_TYPE + COMMA_SEP +
                    PlacesEntry.COLUMN_NAME_LATITUDE + REAL_TYPE + " )";

    private static final String SQL_DELETE_ENTRIES =
            "DROP TABLE IF EXISTS " + PlacesEntry.TABLE_NAME;

    // If you change the database schema, you must increment the database version.
    public static final int DATABASE_VERSION = 7;
    public static final String DATABASE_NAME = "Places.db";

    public PlacesDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_ENTRIES);
    }

    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // This database is only a cache for online data, so its upgrade policy is
        // to simply to discard the data and start over
        db.execSQL(SQL_DELETE_ENTRIES);
        onCreate(db);
    }

    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onUpgrade(db, oldVersion, newVersion);
    }
}
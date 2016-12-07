package tech.boshkov.lab03.Data;

import android.database.Cursor;
import android.provider.BaseColumns;

import com.google.android.gms.location.places.Places;
import com.google.android.gms.maps.model.LatLng;

/* Inner class that defines the table contents */
public class PlacesEntry implements BaseColumns {
    public static final String TABLE_NAME = "places";
    public static final String COLUMN_NAME_GMAPS_ID = "gmaps_id";
    public static final String COLUMN_NAME_TITLE = "title";
    public static final String COLUMN_NAME_LONGITUDE = "longitude";
    public static final String COLUMN_NAME_LATITUDE = "latitude";
    public static final String COLUMN_NAME_ADDRESS = "address";

    public PlacesEntry(Cursor c) {
        loadFromCursor(c);
    }

    public void loadFromCursor(Cursor c) {
        title = c.getString(c.getColumnIndex(PlacesEntry.COLUMN_NAME_TITLE));
        address = c.getString(c.getColumnIndex(PlacesEntry.COLUMN_NAME_ADDRESS));
        double lat = c.getDouble(c.getColumnIndex(PlacesEntry.COLUMN_NAME_LATITUDE));
        double lng = c.getDouble(c.getColumnIndex(PlacesEntry.COLUMN_NAME_LONGITUDE));
        position = new LatLng(lat, lng);
    }

    public String gmaps_id;
    public String title;
    public String address;
    public LatLng position;
}
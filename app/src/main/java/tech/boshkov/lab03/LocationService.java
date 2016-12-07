package tech.boshkov.lab03;

import android.app.Service;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.location.Location;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import tech.boshkov.lab03.Data.PlacesDbHelper;
import tech.boshkov.lab03.Data.PlacesEntry;

public class LocationService extends Service implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, LocationListener {
    public int mInterval = 6000;
    int mCallNum = 0;
    private Location mLastLocation;
    private boolean mIsConnected;

    PlacesDbHelper mDbHelper;
    private RequestQueue mRequestQueue;

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        mIsConnected = false;
    }


    public Location getLastLocation() {
        return mLastLocation;
    }

    public PlacesDbHelper getDbHelper() {
        return mDbHelper;
    }

    class LocationBinder extends Binder {
        LocationService getService() {
            return LocationService.this;
        }
    }

    private GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;

    private LocationBinder mBinder;

    public LocationService() {
        Log.i("Service", "Constructor");
    }

    public void update() {
        if (getLastLocation() != null) {
            Intent intent = new Intent();
            intent.putExtra("lng", getLastLocation().getLongitude());
            intent.putExtra("lat", getLastLocation().getLatitude());
            intent.setAction("LocationUpdate");
            sendBroadcast(intent);
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i("Service", "Create");

        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }

        mBinder = new LocationBinder();

        mGoogleApiClient.connect();

        if (mRequestQueue == null) {
            mRequestQueue = Volley.newRequestQueue(this);
            mRequestQueue.start();
        }

        if (mDbHelper == null) {
            mDbHelper = new PlacesDbHelper(this);
            dropAllData();
        }
    }

    protected void dropAllData() {
        SQLiteDatabase db = mDbHelper.getWritableDatabase();
        db.delete(PlacesEntry.TABLE_NAME, null, null);
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(
                mGoogleApiClient);

        mLocationRequest = LocationRequest.create();
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setInterval(mInterval);
        mLocationRequest.setFastestInterval(mInterval);

        LocationServices.FusedLocationApi.requestLocationUpdates(
                mGoogleApiClient, mLocationRequest, this);


        onLocationChanged(mLastLocation);
        mIsConnected = true;
    }

    @Override
    public void onConnectionSuspended(int i) {
    }

    protected static final String GMAPS_URL_TEMPLATE = "https://maps.googleapis.com/maps/api/place" +
            "/nearbysearch/json?location=%f,%f" +
            "&key=AIzaSyD1qNrBFoW5ZZzxDb6i0Mm5sr-cxYynbXI&radius=5000&types=restaurant";

    @Override
    public void onLocationChanged(Location location) {
        mLastLocation = location;
        Toast.makeText(this, "location :"+location.getLatitude()+" , "
                +location.getLongitude(), Toast.LENGTH_SHORT).show();

        Log.i("Service", "Loc changed");

        String url = String.format(GMAPS_URL_TEMPLATE, getLastLocation().getLatitude(),
                getLastLocation().getLongitude());

        JsonObjectRequest jsObjRequest = new JsonObjectRequest
                (Request.Method.GET, url, null, new Response.Listener<JSONObject>() {

                    @Override
                    public void onResponse(JSONObject response) {
                        Log.i("Service", "Got json response");

                        // Gets the data repository in write mode
                        SQLiteDatabase db = mDbHelper.getWritableDatabase();

                        String[] projection = {
                                PlacesEntry._ID,
                                PlacesEntry.COLUMN_NAME_GMAPS_ID,
                        };

                        Cursor c = db.query(
                                PlacesEntry.TABLE_NAME,                   // The table to query
                                projection,                               // The columns to return
                                null,                                // The columns for the WHERE clause
                                null,                                     // The values for the WHERE clause
                                null,                                     // don't group the rows
                                null,                                     // don't filter by row groups
                                "_id ASC"                                     // The sort order
                        );

                        long dbCount = c.getCount();
                        JSONArray results = null;

                        try {
                            results = response.getJSONArray("results");

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                        if (results == null) return;

                        boolean doUpdate = false;
                        if (dbCount != results.length()) {
                            doUpdate = true;
                        } else {
                            for (int i = 0; i < results.length(); i++) {
                                try {
                                    JSONObject obj = results.getJSONObject(i);
                                    // If we got a new record in the GMaps response, the dataset
                                    // needs an update
                                    String gmaps_id = obj.getString("id");
                                    Cursor existanceCheck = db.rawQuery("SELECT * FROM " +
                                            PlacesEntry.TABLE_NAME
                                            + " WHERE " + PlacesEntry.COLUMN_NAME_GMAPS_ID + " = '"
                                            + gmaps_id + "'", null);

                                    if (!c.moveToFirst())
                                    {
                                        // Record doesn't exist. Do update.
                                        doUpdate = true;
                                        // We're dropping all data anyway, might as well stop here.
                                        break;
                                    }

                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }

                            }
                        }

                        if (doUpdate) {
                            dropAllData();

                            for (int i = 0; i < results.length(); i++) {

                                // Create a new map of values, where column names are the keys
                                try {
                                    JSONObject obj = results.getJSONObject(i);
                                    JSONObject location = obj.getJSONObject("geometry").getJSONObject("location");

                                    ContentValues values = new ContentValues();
                                    values.put(PlacesEntry.COLUMN_NAME_TITLE, obj.getString("name"));
                                    values.put(PlacesEntry.COLUMN_NAME_GMAPS_ID, obj.getString("id"));
                                    values.put(PlacesEntry.COLUMN_NAME_LONGITUDE, location.getDouble("lng"));
                                    values.put(PlacesEntry.COLUMN_NAME_LATITUDE, location.getDouble("lat"));
                                    values.put(PlacesEntry.COLUMN_NAME_ADDRESS, obj.getString("vicinity"));

                                    // Insert the new row, returning the primary key value of the new row
                                    long newRowId = db.insert(PlacesEntry.TABLE_NAME, null, values);
                                    Log.i("Service", "Entry " + obj.getString("name") + " / " + newRowId + " added");

                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            }

                            update();
                        }
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        // TODO Auto-generated method stub
                    }
                });
        mRequestQueue.add(jsObjRequest);
    }

    @Override
    public void onDestroy() {
        mGoogleApiClient.disconnect();

        Log.i("Service", "Destroying");
        Toast.makeText(this, "Service Stopped", Toast.LENGTH_SHORT).show();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i("Service", "Received start id " + startId + ": " + intent);
        Toast.makeText(this, "Service started", Toast.LENGTH_SHORT).show();
        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        Log.i("Service", "Bound");
        //throw new UnsupportedOperationException("Not yet implemented");
        return mBinder;
    }
}

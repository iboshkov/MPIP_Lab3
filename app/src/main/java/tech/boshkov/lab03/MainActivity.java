package tech.boshkov.lab03;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.location.Location;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.Date;

import tech.boshkov.lab03.Data.PlacesEntry;
import tech.boshkov.lab03.Fragments.PlacesListFragment;

public class MainActivity extends AppCompatActivity implements
        PlacesListFragment.OnListFragmentInteractionListener, OnMapReadyCallback {
    private LocationService mBoundService;
    LinearLayout mFragmentContainer;

    private boolean mIsBound;
    private GoogleMap mMap;
    private android.support.v4.app.Fragment mCurrentFragment;
    private boolean mShowMap;

    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            Toast.makeText(MainActivity.this, "Service Connected",
                    Toast.LENGTH_SHORT).show();
            mBoundService = ((LocationService.LocationBinder) service).getService();
            reloadMapData();
        }

        public void onServiceDisconnected(ComponentName className) {
            mBoundService = null;
            Toast.makeText(MainActivity.this, "Service Disconnected",
                    Toast.LENGTH_SHORT).show();
        }
    };
    private Date mLastUpdateTime;
    private Location mCurrentLocation;

    void doBindService() {
        boolean bind = getApplicationContext().bindService(new Intent(this,
                LocationService.class), mConnection, Context.BIND_AUTO_CREATE);
        mIsBound = true;
    }

    void doUnbindService() {
        if (mIsBound) {
            // Detach our existing connection.
            mIsBound = false;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        doUnbindService();
    }

    @Override
    protected void onResume() {
        super.onResume();

        mShowMap = PreferenceManager.getDefaultSharedPreferences(this).getBoolean("showMap", false);
        Toast.makeText(this, "Resume " + (mShowMap ? "with map" : "with list"), Toast.LENGTH_SHORT).show();
        if (mShowMap) {
            // Obtain the SupportMapFragment and get notified when the map is ready to be used.
            SupportMapFragment mapFragment = SupportMapFragment.newInstance();
            mapFragment.getMapAsync(this);
            mCurrentFragment = mapFragment;
        } else {
            PlacesListFragment listFragment = PlacesListFragment.newInstance(1);
            mCurrentFragment = listFragment;
            mMap = null;
        }

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, mCurrentFragment, "Current")
                .commit();
        getSupportFragmentManager().executePendingTransactions();

        IntentFilter filter = new IntentFilter();
        filter.addAction("LocationUpdate");
        registerReceiver(mBroadcastReceiver, filter);

        reloadMapData();
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mBroadcastReceiver);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar myToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(myToolbar);

        mFragmentContainer = (LinearLayout) findViewById(R.id.fragmentContainer);

        doBindService();
    }

    @Override
    protected void onStart() {
        super.onStart();
    }


    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.action_settings:
                Intent intent = new Intent(this, SettingsActivity.class);
                intent.putExtra(SettingsActivity.EXTRA_SHOW_FRAGMENT, SettingsActivity.GeneralPreferenceFragment.class.getName());
                intent.putExtra(SettingsActivity.EXTRA_NO_HEADERS, true);
                startActivity(intent);
                return true;

            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);

        }
    }


    @Override
    public void onListFragmentInteraction(PlacesEntry item) {

    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        if (mBoundService != null) {
            reloadMapData();
        }
    }

    public void reloadMapData() {
        Log.i("Main", "Broadcast received");
        if (mShowMap && mMap != null) {
            mMap.clear();
            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                    || ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                mMap.setMyLocationEnabled(true);
            }
            SQLiteDatabase db = mBoundService.getDbHelper().getReadableDatabase();

            String[] projection = {
                    PlacesEntry._ID,
                    PlacesEntry.COLUMN_NAME_GMAPS_ID,
                    PlacesEntry.COLUMN_NAME_TITLE,
                    PlacesEntry.COLUMN_NAME_ADDRESS,
                    PlacesEntry.COLUMN_NAME_LATITUDE,
                    PlacesEntry.COLUMN_NAME_LONGITUDE
            };

            Cursor c = db.query(
                    PlacesEntry.TABLE_NAME,                   // The table to query
                    projection,                               // The columns to return
                    null,                                         // The columns for the WHERE clause
                    null,                                     // The values for the WHERE clause
                    null,                                     // don't group the rows
                    null,                                     // don't filter by row groups
                    "_id ASC"                                 // The sort order
            );

            while (c.moveToNext()) {
                MarkerOptions opts = new MarkerOptions();

                PlacesEntry entry = new PlacesEntry(c);

                opts.title(entry.title);
                opts.position(entry.position);
                mMap.addMarker(opts);
            }

            if (mBoundService != null) {
                Location loc = mBoundService.getLastLocation();
                if (loc != null ){
                    mMap.addMarker(new MarkerOptions().title("You").position(new LatLng(loc.getLatitude(), loc.getLongitude())));
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(loc.getLatitude(), loc.getLongitude()), 15));
                }
            }
        } else {
            if (mCurrentFragment instanceof PlacesListFragment) {
                ((PlacesListFragment)mCurrentFragment).reloadData(mBoundService);
            }
        }
    }

    BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            reloadMapData();
        }
    };


}

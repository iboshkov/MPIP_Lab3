package tech.boshkov.lab03.Fragments;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.android.gms.maps.model.MarkerOptions;

import tech.boshkov.lab03.Data.PlacesEntry;
import tech.boshkov.lab03.Fragments.PlacesListFragment.OnListFragmentInteractionListener;
import tech.boshkov.lab03.LocationService;
import tech.boshkov.lab03.R;

import java.util.ArrayList;
import java.util.List;

public class PlacesListRecyclerViewAdapter extends
        RecyclerView.Adapter<PlacesListRecyclerViewAdapter.ViewHolder> {

    private final List<PlacesEntry> mValues;
    private final OnListFragmentInteractionListener mListener;

    public PlacesListRecyclerViewAdapter(OnListFragmentInteractionListener listener) {
        mValues = new ArrayList<>();
        mListener = listener;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.fragment_placeslist, parent, false);
        return new ViewHolder(view);
    }

    public void reloadData(LocationService svc) {
        SQLiteDatabase db = svc.getDbHelper().getReadableDatabase();

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
            PlacesEntry entry = new PlacesEntry(c);
            mValues.add(entry);
        }
        this.notifyDataSetChanged();
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        holder.mItem = mValues.get(position);
        holder.mTitleView.setText(mValues.get(position).title);
        holder.mAddressView.setText(mValues.get(position).address);

        holder.mView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (null != mListener) {
                    // Notify the active callbacks interface (the activity, if the
                    // fragment is attached to one) that an item has been selected.
                    mListener.onListFragmentInteraction(holder.mItem);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return mValues.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public final View mView;
        public final TextView mTitleView;
        public final TextView mAddressView;
        public PlacesEntry mItem;

        public ViewHolder(View view) {
            super(view);
            mView = view;
            mTitleView = (TextView) view.findViewById(R.id.title);
            mAddressView = (TextView) view.findViewById(R.id.address);
        }

        @Override
        public String toString() {
            return super.toString() + " '" + mAddressView.getText() + "'";
        }
    }
}

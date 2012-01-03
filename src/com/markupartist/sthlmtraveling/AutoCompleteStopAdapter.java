/*
 * Copyright (C) 2009 Johan Nilsson <http://markupartist.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.markupartist.sthlmtraveling;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;
import android.widget.Toast;

import com.markupartist.sthlmtraveling.provider.planner.Planner;
import com.markupartist.sthlmtraveling.provider.planner.Stop;
import com.markupartist.sthlmtraveling.utils.LocationUtils;

public class AutoCompleteStopAdapter extends ArrayAdapter<String> implements Filterable {
    protected static final int WHAT_NOTIFY_PERFORM_FILTERING = 1;
    protected static final int WHAT_NOTIFY_PUBLISH_FILTERING = 2;
    private static String TAG = "AutoCompleteStopAdapter";
    private final Object mLock = new Object();
    private Planner mPlanner;
    private Geocoder mGeocoder;
    private List<Object> mValues;
    private boolean mIncludeAddresses = true;
    private LayoutInflater mInflater;
    private FilterListener mFilterListener;

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case WHAT_NOTIFY_PERFORM_FILTERING:
                if (mFilterListener != null) {
                    mFilterListener.onPerformFiltering();
                }
                break;
            case WHAT_NOTIFY_PUBLISH_FILTERING:
                if (mFilterListener != null) {
                    mFilterListener.onPublishFiltering();
                }
                break;
            }
        }
    };

    public AutoCompleteStopAdapter(Context context, int textViewResourceId,
            Planner planner, boolean includeAddresses) {
        super(context, textViewResourceId);
        mPlanner = planner;
        mIncludeAddresses = includeAddresses;
        mGeocoder = new Geocoder(context, new Locale("sv"));
        mInflater = (LayoutInflater) context.getSystemService(
                Context.LAYOUT_INFLATER_SERVICE);
    }

    public Object getValue(int position) {
        if (mValues != null && mValues.size() > 0) {
            return mValues.get(position);
        }
        
        Log.d(TAG, "value was null");
        return null;
    }

    @Override
    public Filter getFilter() {
        Filter nameFilter = new Filter() {
            private boolean mWasSuccess = true; // We are optimistic ones...

            @Override
            protected FilterResults performFiltering(CharSequence constraint) {

                mHandler.sendEmptyMessage(WHAT_NOTIFY_PERFORM_FILTERING);

                FilterResults filterResults = new FilterResults();

                // TODO: Remove hard coded strings here.
                if (constraint != null
                        && !constraint.equals("My location")
                        && !constraint.equals("Min position")
                        && !constraint.equals("Välj en plats på kartan")
                        && !constraint.equals("Point on map")) {

                    List<Object> values = new ArrayList<Object>();

                    if (mIncludeAddresses) {
                        List<Address> addresses = null;
                        try {
                            double lowerLeftLatitude = 58.9074;
                            double lowerLeftLongitude = 17.1002;
                            double upperRightLatitude = 59.8751;
                            double upperRightLongitude = 19.0722;
                            addresses = mGeocoder.getFromLocationName(constraint.toString(),
                                    5, lowerLeftLatitude, lowerLeftLongitude,
                                    upperRightLatitude, upperRightLongitude);
                        } catch (IOException e) {
                            //mWasSuccess = false;
                        }
                        if (addresses != null) {
                            values.addAll(addresses);
                        }
                    }

                    ArrayList<String> list = null;
                    try {
                        String query = constraint.toString();
                        if (Stop.looksValid(query)) {
                            list = mPlanner.findStop(query);
                        }
                    } catch (IOException e) {
                        mWasSuccess = false;
                    }
                    if (list != null) {
                        values.addAll(list);
                    }

                    filterResults.values = values;
                    filterResults.count = values.size();
                }

                return filterResults;
            }

            @SuppressWarnings("unchecked") // For the list used in the for each statement
            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                mHandler.sendEmptyMessage(WHAT_NOTIFY_PUBLISH_FILTERING);

                if (results != null && results.count > 0) {
                    clear();
                    mValues = (List<Object>)results.values;
                    synchronized (mLock) {
                        for (Object value : mValues) {
                            if (value instanceof String) {
                                add((String) value);
                            } else {
                                Address address = (Address) value;
                                add(LocationUtils.getAddressLine(address));
                            }
                        }
                    }
                    notifyDataSetChanged();
                } else if (!mWasSuccess) {
                    Toast.makeText(getContext(), 
                            getContext().getResources().getText(R.string.network_problem_message), 
                            Toast.LENGTH_SHORT).show();
                }
            }
        };
        
        return nameFilter;
    }

    /* (non-Javadoc)
     * @see android.widget.ArrayAdapter#getView(int, android.view.View, android.view.ViewGroup)
     */
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if(convertView == null) {
            convertView = mInflater.inflate(R.layout.autocomplete_item_2line, null);
            // TODO: use a holder here...
        }

        TextView text1 = (TextView) convertView.findViewById(R.id.text1);
        TextView text2 = (TextView) convertView.findViewById(R.id.text2);

        Object value = getValue(position);
        if (value instanceof String) {
            text1.setText((String) value);
            text2.setText(R.string.stop_label);
        } else if (value instanceof Address) {
            Address address = (Address) value;
            text1.setText(address.getAddressLine(0) != null ? address.getAddressLine(0) : "");
            text2.setText(address.getAddressLine(1) != null ? address.getAddressLine(1) : "");
        }

        return convertView;
    }

    public void setFilterListener(FilterListener listener) {
        mFilterListener = listener;
    }

    public static interface FilterListener {
        public void onPerformFiltering();
        public void onPublishFiltering();
    }
}

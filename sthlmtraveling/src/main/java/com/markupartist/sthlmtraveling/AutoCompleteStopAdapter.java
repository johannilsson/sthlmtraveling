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

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
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
import com.markupartist.sthlmtraveling.provider.site.Site;
import com.markupartist.sthlmtraveling.provider.site.SitesStore;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class AutoCompleteStopAdapter extends ArrayAdapter<String> implements Filterable {
    protected static final int WHAT_NOTIFY_PERFORM_FILTERING = 1;
    protected static final int WHAT_NOTIFY_PUBLISH_FILTERING = 2;
    private static String TAG = "AutoCompleteStopAdapter";
    private final Object mLock = new Object();
    private List<Site> mValues;
    private LayoutInflater mInflater;
    private boolean mOnlyStations;
    private static FilterListener sFilterListener;

    private static Handler sHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case WHAT_NOTIFY_PERFORM_FILTERING:
                if (sFilterListener != null) {
                    sFilterListener.onPerformFiltering();
                }
                break;
            case WHAT_NOTIFY_PUBLISH_FILTERING:
                if (sFilterListener != null) {
                    sFilterListener.onPublishFiltering();
                }
                break;
            }
        }
    };

    public AutoCompleteStopAdapter(Context context, int textViewResourceId,
            Planner planner, boolean onlyStations) {
        super(context, textViewResourceId);
        mInflater = (LayoutInflater) context.getSystemService(
                Context.LAYOUT_INFLATER_SERVICE);
        mOnlyStations = onlyStations;
    }

    public Site getValue(int position) {
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

                sHandler.sendEmptyMessage(WHAT_NOTIFY_PERFORM_FILTERING);

                FilterResults filterResults = new FilterResults();

                // TODO: Remove hard coded strings here.
                if (constraint != null
                        && !constraint.equals("My location")
                        && !constraint.equals("Min position")
                        && !constraint.equals("Välj en plats på kartan")
                        && !constraint.equals("Point on map")) {

                    List<Site> values = new ArrayList<Site>();

                    ArrayList<Site> list = null;
                    try {
                        String query = constraint.toString();
                        if (Site.looksValid(query)) {
                            list = SitesStore.getInstance().getSiteV2(
                               getContext(), query, mOnlyStations
                            );
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
                sHandler.sendEmptyMessage(WHAT_NOTIFY_PUBLISH_FILTERING);

                if (results != null && results.count > 0) {
                    clear();
                    mValues = (List<Site>) results.values;

                    synchronized (mLock) {
                        for (Site value : mValues) {
                            add(value.getName());
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

        Site site = getValue(position);

        if (site.isAddress()) {
            text1.setText(site.getName());
            text2.setText(R.string.address_label);
        } else {
            text1.setText(site.getName());
            text2.setText(R.string.stop_label);
        }

        return convertView;
    }

    public Site findSite(String name) {
        if (TextUtils.isEmpty(name) || mValues == null) {
            return null;
        }
        for (Site s : mValues) {
            if (s.hasName() && s.getName().toLowerCase().startsWith(name.trim().toLowerCase())) {
                return s;
            }
        }
        return null;
    }

    public void setFilterListener(FilterListener listener) {
        sFilterListener = listener;
    }

    public static interface FilterListener {
        public void onPerformFiltering();
        public void onPublishFiltering();
    }
}

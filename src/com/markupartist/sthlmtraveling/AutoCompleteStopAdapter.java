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

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.Filterable;

public class AutoCompleteStopAdapter extends ArrayAdapter<String> implements Filterable {
    private static String TAG = "AutoCompleteStopAdapter";
    private Planner mPlanner;

    public AutoCompleteStopAdapter(Context context, int textViewResourceId, Planner planner) {
        super(context, textViewResourceId);
        this.mPlanner = planner;
    }

    @Override
    public Filter getFilter() {
        Filter nameFilter = new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                if (constraint != null) {
                    //Log.d(TAG, "Searching for " + constraint);
                    ArrayList<String> list = 
                        mPlanner.findStop(constraint.toString());

                    FilterResults filterResults = new FilterResults();
                    filterResults.values = list;
                    filterResults.count = list.size();
                    return filterResults;
                } else {
                    return new FilterResults();
                }
            }

            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                if (results != null && results.count > 0) {
                    clear();
                    for (String value : (List<String>)results.values) {
                        add(value);
                    }
                    notifyDataSetChanged();
                }
            }
        };
        return nameFilter;
    }

}

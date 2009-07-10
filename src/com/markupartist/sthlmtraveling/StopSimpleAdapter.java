package com.markupartist.sthlmtraveling;

import java.util.ArrayList;
import java.util.List;


import android.content.Context;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.Filterable;

public class StopSimpleAdapter extends ArrayAdapter<String> implements Filterable {
    private static String TAG = "StopSimpleAdapter";
    private Planner mPlanner;

    public StopSimpleAdapter(Context context, int textViewResourceId, Planner planner) {
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

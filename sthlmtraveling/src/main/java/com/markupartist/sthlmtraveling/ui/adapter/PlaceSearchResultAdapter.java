/*
 * Copyright (C) 2009-2015 Johan Nilsson <http://markupartist.com>
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

package com.markupartist.sthlmtraveling.ui.adapter;

import android.content.Context;
import android.support.annotation.DrawableRes;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.TextView;

import com.markupartist.sthlmtraveling.R;
import com.markupartist.sthlmtraveling.provider.site.Site;
import com.markupartist.sthlmtraveling.ui.view.HeaderFooterRecyclerViewAdapter;
import com.markupartist.sthlmtraveling.utils.ViewHelper;

import java.util.ArrayList;
import java.util.List;

public class PlaceSearchResultAdapter extends HeaderFooterRecyclerViewAdapter implements Filterable {

    List<PlaceItem> mData = new ArrayList<>();
    List<SearchFooterItem> mFooterData = new ArrayList<>();
    private Context mContext;
    private PlaceFilter mFilter;

    public PlaceSearchResultAdapter(Context context) {
        mContext = context;
    }

    public void replaceAll(List<PlaceItem> all) {
        mData.clear();
        mData.addAll(all);
        notifyDataSetChanged();
    }

    public void setFooterData(SearchFooterItem item) {
        mFooterData.clear();
        mFooterData.add(item);
        notifyDataSetChanged();
    }
//    @Override
//    public SimpleViewHolder onCreateViewHolder(ViewGroup parent, int position) {
//        final View view = LayoutInflater.from(mContext).inflate(R.layout.row_place_search, parent, false);
//        return new SimpleViewHolder(view);
//    }

//    @Override
//    public void onBindViewHolder(SimpleViewHolder holder, int position) {
//        PlaceItem placeResult = mData.get(position);
//        ViewHelper.setText(holder.text1, placeResult.getTitle());
//        if (!TextUtils.isEmpty(placeResult.getSubtitle())) {
//            ViewHelper.setText(holder.text2, placeResult.getSubtitle());
//            holder.text2.setVisibility(View.VISIBLE);
//        } else {
//            holder.text2.setVisibility(View.GONE);
//        }
//    }

//    @Override
//    public int getItemCount() {
//        return mData.size();
//    }

    @Override
    protected int getHeaderItemCount() {
        return 0;
    }

    @Override
    public int getFooterItemCount() {
        return mFooterData.size();
    }

    @Override
    public int getContentItemCount() {
        return mData.size();
    }

    @Override
    protected RecyclerView.ViewHolder onCreateHeaderItemViewHolder(ViewGroup parent, int headerViewType) {
        return null;
    }

    @Override
    protected RecyclerView.ViewHolder onCreateFooterItemViewHolder(ViewGroup parent, int footerViewType) {
        final View view = LayoutInflater.from(mContext).inflate(R.layout.row_place_search_footer, parent, false);
        return new ContentViewHolder(view);
    }

    @Override
    protected RecyclerView.ViewHolder onCreateContentItemViewHolder(ViewGroup parent, int contentViewType) {
        final View view = LayoutInflater.from(mContext).inflate(R.layout.row_place_search, parent, false);
        return new ContentViewHolder(view);
    }

    @Override
    protected void onBindHeaderItemViewHolder(RecyclerView.ViewHolder headerViewHolder, int position) {

    }

    @Override
    protected void onBindFooterItemViewHolder(RecyclerView.ViewHolder footerViewHolder, int position) {
        ContentViewHolder holder = (ContentViewHolder) footerViewHolder;
        SearchFooterItem placeResult = mFooterData.get(position);
        ViewHelper.setText(holder.text1, placeResult.text1);
        if (placeResult.iconResource != -1) {
            holder.icon.setImageResource(placeResult.iconResource);
            holder.icon.setVisibility(View.VISIBLE);
        } else {
            holder.icon.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onBindContentItemViewHolder(RecyclerView.ViewHolder contentViewHolder, int position) {
        ContentViewHolder holder = (ContentViewHolder) contentViewHolder;
        PlaceItem placeResult = mData.get(position);
        ViewHelper.setText(holder.text1, placeResult.getTitle());
        if (!TextUtils.isEmpty(placeResult.getSubtitle())) {
            ViewHelper.setText(holder.text2, placeResult.getSubtitle());
            holder.text2.setVisibility(View.VISIBLE);
        } else {
            holder.text2.setVisibility(View.GONE);
        }
        if (placeResult.isTransitStop()) {
            holder.icon.setImageResource(R.drawable.ic_transport_transit_20dp);
            ViewHelper.tint(holder.icon, ContextCompat.getColor(holder.icon.getContext(), R.color.icon_default));
        } else {
            holder.icon.setImageResource(R.drawable.ic_place_24dp);
        }

    }

    public PlaceItem getItem(int position) {
        return mData.get(position);
    }

    public void clear() {
        mData.clear();
    }

    public void setFilter(PlaceFilter filter) {
        mFilter = filter;
    }

    @Override
    public PlaceFilter getFilter() {
        return mFilter;
    }


    /**
     * Places view holder
     */
    public static class ContentViewHolder extends RecyclerView.ViewHolder {
        TextView text1;
        TextView text2;
        TextView distance;
        ImageView icon;

        public ContentViewHolder(View view) {
            super(view);
            text1 = (TextView) view.findViewById(R.id.text1);
            text2 = (TextView) view.findViewById(R.id.text2);
            distance = (TextView) view.findViewById(R.id.distance);
            icon = (ImageView) view.findViewById(R.id.row_icon);
        }
    }

    public static class SearchFooterItem {
        public String text1;
        @DrawableRes
        public int iconResource = -1;
    }


    /**
     * Base place filter.
     */
    public static abstract class PlaceFilter extends Filter {
        private final PlaceSearchResultAdapter mAdapter;
        private boolean mWasSuccess;
        private FilterResultCallback mFilterResultCallback;

        public PlaceFilter(PlaceSearchResultAdapter adapter) {
            mAdapter = adapter;
        }

        public void setFilterResultCallback(FilterResultCallback filterResultCallback) {
            mFilterResultCallback = filterResultCallback;
        }

        public void setStatus(boolean wasSuccess) {
            mWasSuccess = wasSuccess;
        }

        @SuppressWarnings("unchecked")
        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            if (mFilterResultCallback != null) {
                if (mWasSuccess) {
                    mFilterResultCallback.onSuccess();
                } else {
                    mFilterResultCallback.onError();
                }
            }

            if (results != null && results.count > 0) {
                // The API returned at least one result, update the data.
                mAdapter.replaceAll((List<PlaceItem>) results.values);
                mAdapter.notifyDataSetChanged();
            } else {
                // The API did not return any results, invalidate the data set.
                mAdapter.notifyDataSetChanged();
            }
        }

        public abstract void setResultCallback(PlaceItem item, PlaceItemResultCallback resultCallback);

        public interface FilterResultCallback {
            void onSuccess();
            void onError();
        }

        public interface PlaceItemResultCallback {
            // TODO: Replace with Place.
            void onResult(Site site);

            void onError();
        }
    }
}
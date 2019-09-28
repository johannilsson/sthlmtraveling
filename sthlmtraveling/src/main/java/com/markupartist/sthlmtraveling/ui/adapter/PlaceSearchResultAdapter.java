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
import androidx.annotation.DrawableRes;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.markupartist.sthlmtraveling.R;
import com.markupartist.sthlmtraveling.ui.view.HeaderFooterRecyclerViewAdapter;
import com.markupartist.sthlmtraveling.utils.ViewHelper;

import java.util.ArrayList;
import java.util.List;

public class PlaceSearchResultAdapter extends HeaderFooterRecyclerViewAdapter implements Filterable {

    private final Context mContext;
    private final LayoutInflater mLayoutInflator;
    List<PlaceItem> mData = new ArrayList<>();
    List<SearchFooterItem> mFooterData = new ArrayList<>();
    private PlaceFilter mFilter;
    private OnEditItemClickListener mOnEditItemClickListener;

    public PlaceSearchResultAdapter(Context context) {
        mContext = context;
        mLayoutInflator = LayoutInflater.from(mContext);
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
        return new ContentViewHolder(mLayoutInflator.inflate(
                R.layout.row_place_search_footer, parent, false),
                mOnEditItemClickListener);
    }

    @Override
    protected RecyclerView.ViewHolder onCreateContentItemViewHolder(ViewGroup parent, int contentViewType) {
        return new ContentViewHolder(mLayoutInflator.inflate(
                R.layout.row_place_search_result, parent, false),
                mOnEditItemClickListener);
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


    public void setOnEditItemClickListener(OnEditItemClickListener onEditItemClickListener) {
        mOnEditItemClickListener = onEditItemClickListener;
    }

    public interface OnEditItemClickListener {
        void onEditItemClicked(int position);
    }

    /**
     * Places view holder
     */
    public static class ContentViewHolder extends RecyclerView.ViewHolder {
        TextView text1;
        TextView text2;
        TextView distance;
        ImageView icon;
        ImageButton endIcon;

        public ContentViewHolder(View view, final OnEditItemClickListener onEditItemClickListener) {
            super(view);
            text1 = (TextView) view.findViewById(R.id.text1);
            text2 = (TextView) view.findViewById(R.id.text2);
            distance = (TextView) view.findViewById(R.id.distance);
            icon = (ImageView) view.findViewById(R.id.row_icon);
            endIcon = (ImageButton) view.findViewById(R.id.row_end_icon);

            if (endIcon != null) {
                endIcon.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (onEditItemClickListener != null) {
                            onEditItemClickListener.onEditItemClicked(getAdapterPosition());
                        }
                    }
                });
            }
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
        private PlaceFilterResultCallback mPlaceFilterResultCallback;

        public PlaceFilter(PlaceSearchResultAdapter adapter) {
            mAdapter = adapter;
        }

        public void setFilterResultCallback(PlaceFilterResultCallback placeFilterResultCallback) {
            mPlaceFilterResultCallback = placeFilterResultCallback;
        }

        public void setStatus(boolean wasSuccess) {
            mWasSuccess = wasSuccess;
        }

        @SuppressWarnings("unchecked")
        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            if (mPlaceFilterResultCallback != null) {
                if (mWasSuccess) {
                    mPlaceFilterResultCallback.onSuccess();
                } else {
                    mPlaceFilterResultCallback.onError();
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

    }
}
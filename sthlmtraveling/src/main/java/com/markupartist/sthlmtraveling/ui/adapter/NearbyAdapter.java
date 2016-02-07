/*
 * Copyright (C) 2009-2016 Johan Nilsson <http://markupartist.com>
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

import android.support.annotation.NonNull;
import android.support.v4.util.Pair;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.markupartist.sthlmtraveling.R;
import com.markupartist.sthlmtraveling.data.models.NearbyStop;
import com.markupartist.sthlmtraveling.provider.site.SitesStore;

import java.util.Collections;
import java.util.List;

public class NearbyAdapter extends RecyclerView.Adapter<NearbyAdapter.NearbyStopViewHolder> {

    private List<NearbyStop> mNearbyStops = Collections.emptyList();
    private final NearbyStopClickListener mNearbyStopClickListener;

    public NearbyAdapter(NearbyStopClickListener clickListener) {
        mNearbyStopClickListener = clickListener;
    }

    @Override
    public NearbyStopViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new NearbyStopViewHolder(
                LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.row_icon_two_rows, parent, false));
    }

    @Override
    public void onBindViewHolder(NearbyStopViewHolder holder, int position) {
        holder.bindTo(mNearbyStops.get(position));
    }

    @Override
    public int getItemCount() {
        return mNearbyStops.size();
    }

    public void fill(@NonNull List<NearbyStop> nearbyStops) {
        mNearbyStops = nearbyStops;
        notifyDataSetChanged();
    }

    public List<NearbyStop> getNearbyStops() {
        return mNearbyStops;
    }

    public interface NearbyStopClickListener {
        void onNearbyStopClick(NearbyStop nearbyStop);
    }

    public class NearbyStopViewHolder extends RecyclerView.ViewHolder {
        private final TextView mDistanceView;
        private final TextView mNameView;
        private final TextView mLocalityView;

        public NearbyStopViewHolder(View itemView) {
            super(itemView);

            ImageView iconView = (ImageView) itemView.findViewById(R.id.row_icon);
            iconView.setImageResource(R.drawable.ic_search_24dp);

            mDistanceView = (TextView) itemView.findViewById(R.id.distance);
            mNameView = (TextView) itemView.findViewById(R.id.text1);
            mLocalityView = (TextView) itemView.findViewById(R.id.text2);

            this.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    NearbyStop nearbyStop = mNearbyStops.get(getAdapterPosition());
                    mNearbyStopClickListener.onNearbyStopClick(nearbyStop);
                }
            });
        }

        public void bindTo(NearbyStop nearbyStop) {
            Pair<String, String> nameAndLocality = SitesStore.nameAsNameAndLocality(nearbyStop.getName());
            mNameView.setText(nameAndLocality.first);
            mLocalityView.setText(nameAndLocality.second);
            mLocalityView.setVisibility(View.VISIBLE);
            mDistanceView.setText(mDistanceView.getResources()
                    .getString(R.string.distance_in_meter, String.valueOf(nearbyStop.getDistance())));
            mDistanceView.setVisibility(View.VISIBLE);
        }
    }
}
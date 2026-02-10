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

package com.markupartist.sthlmtraveling.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.markupartist.sthlmtraveling.R
import com.markupartist.sthlmtraveling.data.models.NearbyStop
import com.markupartist.sthlmtraveling.provider.site.SitesStore

class NearbyAdapter(
    private val nearbyStopClickListener: NearbyStopClickListener
) : RecyclerView.Adapter<NearbyAdapter.NearbyStopViewHolder>() {

    private var nearbyStops: List<NearbyStop> = emptyList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NearbyStopViewHolder {
        return NearbyStopViewHolder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.row_icon_two_rows, parent, false)
        )
    }

    override fun onBindViewHolder(holder: NearbyStopViewHolder, position: Int) {
        holder.bindTo(nearbyStops[position])
    }

    override fun getItemCount(): Int {
        return nearbyStops.size
    }

    fun fill(nearbyStops: List<NearbyStop>) {
        this.nearbyStops = nearbyStops
        notifyDataSetChanged()
    }

    fun getNearbyStops(): List<NearbyStop> {
        return nearbyStops
    }

    interface NearbyStopClickListener {
        fun onNearbyStopClick(nearbyStop: NearbyStop)
    }

    inner class NearbyStopViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val distanceView: TextView = itemView.findViewById(R.id.distance)
        private val nameView: TextView = itemView.findViewById(R.id.text1)
        private val localityView: TextView = itemView.findViewById(R.id.text2)

        init {
            val iconView: ImageView = itemView.findViewById(R.id.row_icon)
            iconView.setImageResource(R.drawable.ic_search_24dp)

            itemView.setOnClickListener {
                val nearbyStop = nearbyStops[adapterPosition]
                nearbyStopClickListener.onNearbyStopClick(nearbyStop)
            }
        }

        fun bindTo(nearbyStop: NearbyStop) {
            val nameAndLocality = SitesStore.nameAsNameAndLocality(nearbyStop.name ?: "")
            nameView.text = nameAndLocality.first
            localityView.text = nameAndLocality.second
            localityView.visibility = View.VISIBLE
            distanceView.text = distanceView.resources
                .getString(R.string.distance_in_meter, nearbyStop.distance.toString())
            distanceView.visibility = View.VISIBLE
        }
    }
}

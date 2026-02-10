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

package com.markupartist.sthlmtraveling.ui.adapter

import android.content.Context
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.markupartist.sthlmtraveling.R
import com.markupartist.sthlmtraveling.ui.view.HeaderFooterRecyclerViewAdapter
import com.markupartist.sthlmtraveling.utils.ViewHelper

class PlaceSearchResultAdapter(
    private val context: Context
) : HeaderFooterRecyclerViewAdapter(), Filterable {

    private val layoutInflator: LayoutInflater = LayoutInflater.from(context)
    var data: MutableList<PlaceItem> = ArrayList()
    var footerData: MutableList<SearchFooterItem> = ArrayList()
    private var filter: PlaceFilter? = null
    private var onEditItemClickListener: OnEditItemClickListener? = null

    fun replaceAll(all: List<PlaceItem>) {
        data.clear()
        data.addAll(all)
        notifyDataSetChanged()
    }

    fun setFooterData(item: SearchFooterItem) {
        footerData.clear()
        footerData.add(item)
        notifyDataSetChanged()
    }

    override fun getHeaderItemCount(): Int {
        return 0
    }

    override fun getFooterItemCount(): Int {
        return footerData.size
    }

    public override fun getContentItemCount(): Int {
        return data.size
    }

    override fun onCreateHeaderItemViewHolder(parent: ViewGroup, headerViewType: Int): RecyclerView.ViewHolder? {
        return null
    }

    override fun onCreateFooterItemViewHolder(parent: ViewGroup, footerViewType: Int): RecyclerView.ViewHolder {
        return ContentViewHolder(
            layoutInflator.inflate(R.layout.row_place_search_footer, parent, false),
            onEditItemClickListener
        )
    }

    override fun onCreateContentItemViewHolder(parent: ViewGroup, contentViewType: Int): RecyclerView.ViewHolder {
        return ContentViewHolder(
            layoutInflator.inflate(R.layout.row_place_search_result, parent, false),
            onEditItemClickListener
        )
    }

    override fun onBindHeaderItemViewHolder(headerViewHolder: RecyclerView.ViewHolder, position: Int) {
        // No-op
    }

    override fun onBindFooterItemViewHolder(footerViewHolder: RecyclerView.ViewHolder, position: Int) {
        val holder = footerViewHolder as ContentViewHolder
        val placeResult = footerData[position]
        placeResult.text1?.let { ViewHelper.setText(holder.text1, it) }
        if (placeResult.iconResource != -1) {
            holder.icon.setImageResource(placeResult.iconResource)
            holder.icon.visibility = View.VISIBLE
        } else {
            holder.icon.visibility = View.GONE
        }
    }

    override fun onBindContentItemViewHolder(contentViewHolder: RecyclerView.ViewHolder, position: Int) {
        val holder = contentViewHolder as ContentViewHolder
        val placeResult = data[position]
        ViewHelper.setText(holder.text1, placeResult.title)
        if (!TextUtils.isEmpty(placeResult.subtitle)) {
            ViewHelper.setText(holder.text2, placeResult.subtitle)
            holder.text2.visibility = View.VISIBLE
        } else {
            holder.text2.visibility = View.GONE
        }
        if (placeResult.isTransitStop) {
            holder.icon.setImageResource(R.drawable.ic_transport_transit_20dp)
            ViewHelper.tint(holder.icon, ContextCompat.getColor(holder.icon.context, R.color.icon_default))
        } else {
            holder.icon.setImageResource(R.drawable.ic_place_24dp)
        }
    }

    fun getItem(position: Int): PlaceItem {
        return data[position]
    }

    fun clear() {
        data.clear()
    }

    fun setFilter(filter: PlaceFilter) {
        this.filter = filter
    }

    override fun getFilter(): PlaceFilter? {
        return filter
    }

    fun setOnEditItemClickListener(onEditItemClickListener: OnEditItemClickListener) {
        this.onEditItemClickListener = onEditItemClickListener
    }

    interface OnEditItemClickListener {
        fun onEditItemClicked(position: Int)
    }

    /**
     * Places view holder
     */
    class ContentViewHolder(
        view: View,
        private val onEditItemClickListener: OnEditItemClickListener?
    ) : RecyclerView.ViewHolder(view) {
        val text1: TextView = view.findViewById(R.id.text1)
        val text2: TextView = view.findViewById(R.id.text2)
        val distance: TextView = view.findViewById(R.id.distance)
        val icon: ImageView = view.findViewById(R.id.row_icon)
        private val endIcon: ImageButton? = view.findViewById(R.id.row_end_icon)

        init {
            endIcon?.setOnClickListener {
                onEditItemClickListener?.onEditItemClicked(adapterPosition)
            }
        }
    }

    class SearchFooterItem {
        var text1: String? = null
        @DrawableRes
        var iconResource: Int = -1
    }

    /**
     * Base place filter.
     */
    abstract class PlaceFilter(
        private val adapter: PlaceSearchResultAdapter
    ) : Filter() {
        private var wasSuccess: Boolean = false
        private var placeFilterResultCallback: PlaceFilterResultCallback? = null

        fun setFilterResultCallback(placeFilterResultCallback: PlaceFilterResultCallback) {
            this.placeFilterResultCallback = placeFilterResultCallback
        }

        fun setStatus(wasSuccess: Boolean) {
            this.wasSuccess = wasSuccess
        }

        @Suppress("UNCHECKED_CAST")
        override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
            placeFilterResultCallback?.let {
                if (wasSuccess) {
                    it.onSuccess()
                } else {
                    it.onError()
                }
            }

            if (results != null && results.count > 0) {
                // The API returned at least one result, update the data.
                adapter.replaceAll(results.values as List<PlaceItem>)
                adapter.notifyDataSetChanged()
            } else {
                // The API did not return any results, invalidate the data set.
                adapter.notifyDataSetChanged()
            }
        }

        abstract fun setResultCallback(item: PlaceItem, resultCallback: PlaceItemResultCallback)
    }
}

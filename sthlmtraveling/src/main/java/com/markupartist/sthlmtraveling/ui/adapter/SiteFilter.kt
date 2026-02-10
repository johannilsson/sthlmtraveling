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
import com.markupartist.sthlmtraveling.provider.site.Site
import com.markupartist.sthlmtraveling.provider.site.SitesStore
import java.io.IOException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Provides places through our own backend.
 */
class SiteFilter(
    adapter: PlaceSearchResultAdapter,
    private val context: Context,
    private val searchOnlyStops: Boolean
) : PlaceSearchResultAdapter.PlaceFilter(adapter) {

    override fun performFiltering(constraint: CharSequence?): FilterResults {
        val filterResults = FilterResults()

        if (constraint != null) {
            val results = mutableListOf<SiteResult>()
            val latch = CountDownLatch(1)
            val success = AtomicBoolean(false)

            val query = constraint.toString()
            if (Site.looksValid(query)) {
                SitesStore.getInstance().getSiteV2Async(context, query, searchOnlyStops,
                    object : SitesStore.SiteCallback {
                        override fun onSuccess(sites: ArrayList<Site>) {
                            for (s in sites) {
                                results.add(SiteResult(s))
                            }
                            success.set(true)
                            latch.countDown()
                        }

                        override fun onError(error: IOException) {
                            success.set(false)
                            latch.countDown()
                        }
                    })

                try {
                    // Wait with timeout to prevent indefinite blocking
                    if (!latch.await(20, TimeUnit.SECONDS)) {
                        // Timeout occurred
                        success.set(false)
                    }
                } catch (e: InterruptedException) {
                    success.set(false)
                }
            } else {
                success.set(true) // Empty query is valid
            }

            setStatus(success.get())
            filterResults.values = results
            filterResults.count = results.size
        }

        return filterResults
    }

    override fun setResultCallback(item: PlaceItem, resultCallback: PlaceItemResultCallback) {
        if (item is SiteResult) {
            resultCallback.onResult(item.site)
        } else {
            resultCallback.onError()
        }
    }

    class SiteResult(val site: Site) : PlaceItem {
        override val title: String
            get() = site.name

        override val subtitle: String
            get() = site.locality

        override val isTransitStop: Boolean
            get() = site.isTransitStop
    }
}

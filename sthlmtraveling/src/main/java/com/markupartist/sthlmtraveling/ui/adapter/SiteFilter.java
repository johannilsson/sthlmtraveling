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

import com.markupartist.sthlmtraveling.provider.site.Site;
import com.markupartist.sthlmtraveling.provider.site.SitesStore;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Provides places through our own backend.
 */
public class SiteFilter extends PlaceSearchResultAdapter.PlaceFilter {
    private final Context mContext;
    private final boolean mSearchOnlyStops;

    public SiteFilter(PlaceSearchResultAdapter adapter, Context context, boolean searchOnlyStops) {
        super(adapter);
        mContext = context;
        mSearchOnlyStops = searchOnlyStops;
    }

    @Override
    protected FilterResults performFiltering(CharSequence constraint) {
        FilterResults filterResults = new FilterResults();

        if (constraint != null) {
            final List<SiteResult> results = new ArrayList<>();
            final CountDownLatch latch = new CountDownLatch(1);
            final AtomicBoolean success = new AtomicBoolean(false);

            String query = constraint.toString();
            if (Site.looksValid(query)) {
                SitesStore.getInstance().getSiteV2Async(mContext, query, mSearchOnlyStops,
                    new SitesStore.SiteCallback() {
                        @Override
                        public void onSuccess(ArrayList<Site> sites) {
                            for (Site s : sites) {
                                results.add(new SiteResult(s));
                            }
                            success.set(true);
                            latch.countDown();
                        }

                        @Override
                        public void onError(IOException error) {
                            success.set(false);
                            latch.countDown();
                        }
                    });

                try {
                    // Wait with timeout to prevent indefinite blocking
                    if (!latch.await(20, TimeUnit.SECONDS)) {
                        // Timeout occurred
                        success.set(false);
                    }
                } catch (InterruptedException e) {
                    success.set(false);
                }
            } else {
                success.set(true); // Empty query is valid
            }

            setStatus(success.get());
            filterResults.values = results;
            filterResults.count = results.size();
        }

        return filterResults;
    }

    @Override
    public void setResultCallback(PlaceItem item, PlaceItemResultCallback resultCallback) {
        if (item instanceof SiteResult) {
            resultCallback.onResult(((SiteResult) item).site);
        } else {
            resultCallback.onError();
        }
    }

    public static class SiteResult implements PlaceItem {
        public final Site site;
        private String title;
        private String subtitle;

        public SiteResult(Site site) {
            this.site = site;

        }

        @Override
        public String getTitle() {
            return site.getName();
        }

        @Override
        public String getSubtitle() {
            return site.getLocality();
        }

        public boolean isTransitStop() {
            return site.isTransitStop();
        }
    }
}
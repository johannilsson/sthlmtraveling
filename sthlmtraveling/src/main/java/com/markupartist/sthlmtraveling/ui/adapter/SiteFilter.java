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
import android.support.v4.util.Pair;

import com.markupartist.sthlmtraveling.provider.site.Site;
import com.markupartist.sthlmtraveling.provider.site.SitesStore;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Provides places through our own backend.
 */
public class SiteFilter extends PlaceSearchResultAdapter.PlaceFilter {
    private final Context mContext;

    public SiteFilter(PlaceSearchResultAdapter adapter, Context context) {
        super(adapter);
        mContext = context;
    }

    @Override
    protected FilterResults performFiltering(CharSequence constraint) {
        FilterResults filterResults = new FilterResults();

        if (constraint != null) {
            List<SiteResult> results = new ArrayList<>();
            try {
                String query = constraint.toString();
                if (Site.looksValid(query)) {
                    List<Site> list = SitesStore.getInstance().getSiteV2(mContext, query, false);
                    for (Site s : list) {
                        results.add(new SiteResult(s));
                    }
                }
                setStatus(true);
            } catch (IOException e) {
                setStatus(false);
            }

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

//            makeTitleAndSubtitle(site.getName());
        }

        void makeTitleAndSubtitle(String s) {
            Pair<String, String> titleAndSubtitle = SitesStore.nameAsNameAndLocality(s);
            title = titleAndSubtitle.first;
            subtitle = titleAndSubtitle.second;
        }

        @Override
        public String getTitle() {
//            return title;
            return site.getName();
        }

        @Override
        public String getSubtitle() {
//            return subtitle;
            return site.getLocality();
        }
    }
}
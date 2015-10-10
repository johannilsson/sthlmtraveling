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

import android.text.TextUtils;
import android.util.Log;
import android.widget.Filter;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.places.AutocompleteFilter;
import com.google.android.gms.location.places.AutocompletePrediction;
import com.google.android.gms.location.places.AutocompletePredictionBuffer;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.PlaceBuffer;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.markupartist.sthlmtraveling.provider.site.Site;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;

/**
 * Provides places through google places.
 */
public class GooglePlacesFilter extends PlaceSearchResultAdapter.PlaceFilter {

    // TODO: Hardcoded bounds here...
    private static final LatLngBounds STHLM_BOUNDS = new LatLngBounds(
            new LatLng(59.171780, 17.606715), new LatLng(59.902638, 19.160592));
    private final GoogleApiClient mGoogleApiClient;

    public GooglePlacesFilter(PlaceSearchResultAdapter adapter, GoogleApiClient googleApiClient) {
        super(adapter);
        mGoogleApiClient = googleApiClient;
    }

    @Override
    protected Filter.FilterResults performFiltering(CharSequence constraint) {
        FilterResults results = new FilterResults();
        // Skip the autocomplete query if no constraints are given.
        if (constraint != null) {
            // Query the autocomplete API for the (constraint) search string.
            ArrayList<GooglePlaceResult> mResultList = getAutocomplete(constraint);
            if (mResultList != null) {
                // The API successfully returned results.
                results.values = mResultList;
                results.count = mResultList.size();
            }
        }
        return results;
    }

    private ArrayList<GooglePlaceResult> getAutocomplete(CharSequence constraint) {
        if (mGoogleApiClient.isConnected()) {
            AutocompleteFilter placeFilter = null;
            PendingResult<AutocompletePredictionBuffer> results =
                    Places.GeoDataApi
                            .getAutocompletePredictions(mGoogleApiClient, constraint.toString(),
                                    STHLM_BOUNDS, placeFilter);

            AutocompletePredictionBuffer autocompletePredictions = results.await(60, TimeUnit.SECONDS);

            final Status status = autocompletePredictions.getStatus();
            if (!status.isSuccess()) {
                autocompletePredictions.release();
                setStatus(false);
                return null;
            }

            Iterator<AutocompletePrediction> iterator = autocompletePredictions.iterator();
            ArrayList<GooglePlaceResult> resultList = new ArrayList<>(autocompletePredictions.getCount());
            while (iterator.hasNext()) {
                AutocompletePrediction prediction = iterator.next();
                resultList.add(new GooglePlaceResult(prediction.getPlaceId(),
                        prediction.getDescription()));
            }
            autocompletePredictions.release();
            setStatus(true);

            return resultList;
        }
        setStatus(false);
        return null;
    }

    @Override
    public void setResultCallback(PlaceItem item, final PlaceItemResultCallback resultCallback) {
        if (!(item instanceof GooglePlaceResult)) {
            resultCallback.onError();
            return;
        }

        final GooglePlaceResult googlePlaceResult =
                (GooglePlaceResult) item;
        final String placeId = String.valueOf(googlePlaceResult.placeId);
        PendingResult<PlaceBuffer> placeResult = Places.GeoDataApi
                .getPlaceById(mGoogleApiClient, placeId);
        placeResult.setResultCallback(new ResultCallback<PlaceBuffer>() {
            @Override
            public void onResult(PlaceBuffer places) {
                if (!places.getStatus().isSuccess()) {
                    // Request did not complete successfully
                    places.release();

                    resultCallback.onError();
                    return;
                }
                if (places.getCount() > 0) {
                    final Place place = places.get(0);
                    final CharSequence thirdPartyAttribution = places.getAttributions();
                    if (thirdPartyAttribution != null) {
                        Log.e("GooglePlacesFilter", "Attribution: " + thirdPartyAttribution.toString());
                    }
                    Site convertedPlace = new Site();
                    convertedPlace.setId(place.getId());
                    convertedPlace.setSource(Site.SOURCE_GOOGLE_PLACES);
                    convertedPlace.setLocation(place.getLatLng().latitude, place.getLatLng().longitude);
                    convertedPlace.setName((String) place.getName());
                    convertedPlace.setLocality((String) place.getAddress());

                    places.release();
                    resultCallback.onResult(convertedPlace);
                } else {
                    resultCallback.onError();
                }
            }
        });
    }

    /**
     * Holder for Places Geo Data Autocomplete API results.
     */
    public static class GooglePlaceResult implements PlaceItem {

        public CharSequence placeId;
        public CharSequence description;
        private String title;
        private String subtitle;

        GooglePlaceResult(CharSequence placeId, CharSequence description) {
            this.placeId = placeId;
            this.description = description;
        }

        public String getTitle() {
            makeTitleAndSubtitle((String) description);
            return title;
        }

        public String getSubtitle() {
            makeTitleAndSubtitle((String) description);
            return subtitle;
        }

        void makeTitleAndSubtitle(String s) {
            if (title != null || subtitle != null) {
                return;
            }

            String[] res = TextUtils.split(s, ",");
            if (res.length > 0) {
                title = res[0];
            }
            subtitle = TextUtils.join(",", res);
            subtitle = subtitle.replace(title + ",", "").trim();
        }
    }
}
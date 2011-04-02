/*
 * Copyright (C) 2009 Johan Nilsson <http://markupartist.com>
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

package com.markupartist.sthlmtraveling;

import org.json.JSONException;

import android.app.Activity;
import android.content.ContentValues;
import android.net.Uri;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;

import com.markupartist.sthlmtraveling.provider.FavoritesDbAdapter;
import com.markupartist.sthlmtraveling.provider.StarredJourneysProvider.StarredJourney;
import com.markupartist.sthlmtraveling.provider.StarredJourneysProvider.StarredJourney.StarredJourneyColumns;
import com.markupartist.sthlmtraveling.provider.planner.JourneyQuery;

public class FavoriteButtonHelper implements OnClickListener {
    private ImageButton mFavoriteButton;
    private Activity mContext;
    private JourneyQuery mJourneyQuery;
    private static int STAR_ON_RESOURCE = android.R.drawable.star_big_on;
    private static int STAR_OFF_RESOURCE = android.R.drawable.star_big_off;

    public FavoriteButtonHelper(Activity context, JourneyQuery journeyQuery) {
        mContext = context;
        mJourneyQuery = journeyQuery;

        mFavoriteButton = (ImageButton) mContext.findViewById(R.id.route_favorite);
        mFavoriteButton.setOnClickListener(this);
    }

    public FavoriteButtonHelper setJourneyQuery(JourneyQuery journeyQuery) {
        mJourneyQuery = journeyQuery;
        return this;
    }

    public FavoriteButtonHelper loadImage() {
        mFavoriteButton.setImageResource(getImageResource());
        return this;
    }

    private boolean isFavorite() {
        //mFavoriteCursor = mFavoritesDbAdapter.fetch(mStartPoint, mEndPoint);
        //mActivity.startManagingCursor(mFavoriteCursor);

        //return mFavoriteCursor.getCount() > 0;
        return false;
    }

    private int getImageResource() {
        if (isFavorite()) {
            return STAR_ON_RESOURCE;
        }
        return STAR_OFF_RESOURCE;
    }

    @Override
    public void onClick(View view) {
        if (isFavorite()) {
            //long id = mFavoriteCursor.getLong(FavoritesDbAdapter.INDEX_ROWID);
            //mFavoritesDbAdapter.delete(id);
        } else {
            String json;
            try {
                json = mJourneyQuery.toJson(false).toString();
            } catch (JSONException e) {
                json = "\"\"";
            }
            ContentValues values = new ContentValues();
            values.put(StarredJourneyColumns.JOURNEY_DATA, json);
            Uri uri = mContext.getContentResolver().insert(
                    StarredJourney.CONTENT_URI, values);
        }

        mFavoriteButton.setImageResource(getImageResource());
    }
}

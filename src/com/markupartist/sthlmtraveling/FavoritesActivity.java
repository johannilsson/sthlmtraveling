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

import java.util.ArrayList;

import android.app.ListActivity;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.text.format.Time;
import android.view.View;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;

import com.markupartist.sthlmtraveling.SearchRoutesTask.OnSearchRoutesResultListener;
import com.markupartist.sthlmtraveling.provider.FavoritesDbAdapter;

public class FavoritesActivity extends ListActivity {

    private FavoritesDbAdapter mFavoritesDbAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.favorites_list);

        mFavoritesDbAdapter = new FavoritesDbAdapter(this).open();

        fillData();
    }

    private void fillData() {
        Cursor favoritesCursor = mFavoritesDbAdapter.fetch();

        startManagingCursor(favoritesCursor);

        String[] from = new String[] {
                FavoritesDbAdapter.KEY_START_POINT, 
                FavoritesDbAdapter.KEY_END_POINT
            };

        int[] to = new int[]{R.id.favorite_start_point, R.id.favorite_end_point};

        SimpleCursorAdapter favorites = new SimpleCursorAdapter(
                this, R.layout.favorite_row, favoritesCursor, from, to);

        setListAdapter(favorites);
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);

        Cursor favoritesCursor = ((SimpleCursorAdapter) this.getListAdapter()).getCursor();

        String startPoint = favoritesCursor.getString(
                favoritesCursor.getColumnIndex(FavoritesDbAdapter.KEY_START_POINT));
        String endPoint = favoritesCursor.getString(
                favoritesCursor.getColumnIndex(FavoritesDbAdapter.KEY_END_POINT));

        Time time = new Time();
        time.setToNow();

        //searchRoutes(startPoint, endPoint, time);
        OnSearchResult onSearchResult = new OnSearchResult();
        onSearchResult.startPoint = startPoint;
        onSearchResult.endPoint = endPoint;
        new SearchRoutesTask(this)
            .setOnSearchRoutesResultListener(onSearchResult)
            .execute(startPoint, endPoint, time);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mFavoritesDbAdapter.close();
    }

    private class OnSearchResult implements OnSearchRoutesResultListener {
        String startPoint;
        String endPoint;

        @Override
        public void onSearchRoutesResult(ArrayList<Route> routes) {
            Intent i = new Intent(FavoritesActivity.this, RoutesActivity.class);
            i.putExtra("com.markupartist.sthlmtraveling.startPoint", startPoint);
            i.putExtra("com.markupartist.sthlmtraveling.endPoint", endPoint);
            startActivity(i);
        }
    }
}

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

import android.app.ListActivity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.SimpleCursorAdapter.CursorToStringConverter;
import android.widget.SimpleCursorAdapter.ViewBinder;

import com.markupartist.sthlmtraveling.planner.Stop;
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

        final SimpleCursorAdapter favorites = new SimpleCursorAdapter(
                this, R.layout.favorite_row, favoritesCursor, from, to);
        favorites.setViewBinder(new ViewBinder() {
            @Override
            public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
                String name = cursor.getString(columnIndex);
                if (name.equals(Stop.TYPE_MY_LOCATION)) {
                    /*
                    View layout = getLayoutInflater().inflate(R.layout.favorite_row, null);

                    if (view.getId() == R.id.favorite_start_point) {
                        ImageView startImageView = (ImageView) 
                                layout.findViewById(R.id.favorite_start_point_image);
                        startImageView.setImageResource(R.drawable.ic_current_position);
                    } else {
                        ImageView endImageView = (ImageView) 
                                layout.findViewById(R.id.favorite_end_point_image);
                        endImageView.setImageResource(R.drawable.ic_current_position);
                    }
                    */

                    name = getString(R.string.my_location);
                }

                ((TextView) view).setText(name);
                return true;
            }
        });

        setListAdapter(favorites);
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);

        Cursor favoritesCursor = ((SimpleCursorAdapter) this.getListAdapter()).getCursor();

        // TODO: Cache getColumnIndex for the future.
        String startPoint = favoritesCursor.getString(
                favoritesCursor.getColumnIndex(FavoritesDbAdapter.KEY_START_POINT));
        String endPoint = favoritesCursor.getString(
                favoritesCursor.getColumnIndex(FavoritesDbAdapter.KEY_END_POINT));

        Uri routesUri = RoutesActivity.createRoutesUri(startPoint, endPoint);
        Intent i = new Intent(Intent.ACTION_VIEW, routesUri, this, RoutesActivity.class);
        startActivity(i);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mFavoritesDbAdapter.close();
    }
}

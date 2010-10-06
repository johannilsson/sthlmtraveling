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

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Parcelable;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AutoCompleteTextView;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;

import com.markupartist.sthlmtraveling.provider.HistoryDbAdapter;
import com.markupartist.sthlmtraveling.provider.planner.Planner;
import com.markupartist.sthlmtraveling.provider.planner.Stop;


public class SearchDeparturesActivity extends BaseListActivity {
    static String TAG = "SearchDeparturesActivity";
    private static final int DIALOG_HISTORY = 0;
    private static final int DIALOG_MORE = 1;
    private boolean mCreateShortcut;
    private HistoryDbAdapter mHistoryDbAdapter;
    private AutoCompleteTextView mSiteTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.search_departures);

        // If the activity was started with the "create shortcut" action, we
        // remember this to change the behavior upon a search.
        if (Intent.ACTION_CREATE_SHORTCUT.equals(getIntent().getAction())) {
            mCreateShortcut = true;
            registerEvent("Create departure shortcut");
        } else {
            registerEvent("Search departures");
        }


        mHistoryDbAdapter = new HistoryDbAdapter(this).open();

        mSiteTextView = (AutoCompleteTextView) findViewById(R.id.sites); 
        AutoCompleteStopAdapter stopAdapter = new AutoCompleteStopAdapter(this,
                R.layout.simple_dropdown_item_1line, Planner.getInstance(), false);
        mSiteTextView.setSelectAllOnFocus(true);
        mSiteTextView.setAdapter(stopAdapter);

        ImageButton searchButton = (ImageButton) findViewById(R.id.search_departure);
        if (mCreateShortcut) {
            //searchButton.setText(getText(R.string.create_shortcut_label));
            setTitle(R.string.create_shortcut_for_departure_label);
        }
        searchButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mSiteTextView.getText().length() == 0) {
                    mSiteTextView.setError(getText(R.string.empty_value));
                } else {
                    if (mCreateShortcut) {
                        onCreateShortCut(mSiteTextView.getText().toString());
                    } else {
                        onSearchDepartures(mSiteTextView.getText().toString());
                    }
                }
            }
        });

        fillData();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mHistoryDbAdapter.close();
    }

    private void fillData() {
        final Cursor historyCursor =
            mHistoryDbAdapter.fetchByType(HistoryDbAdapter.TYPE_DEPARTURE_SITE);

        startManagingCursor(historyCursor);

        String[] from = new String[] {
                HistoryDbAdapter.KEY_NAME
            };

        int[] to = new int[]{android.R.id.text1};

        final SimpleCursorAdapter favorites = new SimpleCursorAdapter(
                this, android.R.layout.simple_list_item_1, historyCursor, from, to);

        stopManagingCursor(historyCursor);

        setListAdapter(favorites);
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        Cursor historyCursor = ((SimpleCursorAdapter) this.getListAdapter()).getCursor();
        startManagingCursor(historyCursor);
        int index = historyCursor.getColumnIndex(HistoryDbAdapter.KEY_NAME);
        String siteName = historyCursor.getString(index);
        stopManagingCursor(historyCursor);

        if (mCreateShortcut) {
            onCreateShortCut(siteName);
        } else {
            onSearchDepartures(siteName);
        }
    }

    private void onSearchDepartures(String siteName) {
        Stop stop = new Stop();
        stop.setName(siteName);
        mHistoryDbAdapter.create(HistoryDbAdapter.TYPE_DEPARTURE_SITE, stop);

        Intent i = new Intent(getApplicationContext(), DeparturesActivity.class);
        i.putExtra(DeparturesActivity.EXTRA_SITE_NAME, siteName);
        startActivity(i);        
    }

    private void onCreateShortCut(String siteName) {
        // Setup the intent to be launched
        Intent shortcutIntent =
            new Intent(getApplicationContext(), DeparturesActivity.class);
        shortcutIntent.setAction(Intent.ACTION_VIEW);
        shortcutIntent.putExtra(DeparturesActivity.EXTRA_SITE_NAME, siteName);
        shortcutIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        // Then, set up the container intent (the response to the caller)
        Intent intent = new Intent();
        intent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
        intent.putExtra(Intent.EXTRA_SHORTCUT_NAME, siteName);
        Parcelable iconResource = Intent.ShortcutIconResource.fromContext(
                this, R.drawable.shortcut_departures);
        intent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, iconResource);

        // Now, return the result to the launcher
        setResult(RESULT_OK, intent);
        finish();
    }

    @Override
    public boolean onSearchRequested() {
        Intent i = new Intent(this, StartActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(i);
        return true;
    }
}

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

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ImageButton;

import com.markupartist.sthlmtraveling.provider.HistoryDbAdapter;
import com.markupartist.sthlmtraveling.provider.planner.Planner;
import com.markupartist.sthlmtraveling.provider.planner.Stop;


public class SearchDeparturesActivity extends Activity {
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
        }

        mHistoryDbAdapter = new HistoryDbAdapter(this).open();

        mSiteTextView = (AutoCompleteTextView) findViewById(R.id.sites); 
        AutoCompleteStopAdapter stopAdapter = new AutoCompleteStopAdapter(this,
                android.R.layout.simple_dropdown_item_1line, Planner.getInstance());
        mSiteTextView.setSelectAllOnFocus(true);
        mSiteTextView.setAdapter(stopAdapter);

        Button searchButton = (Button) findViewById(R.id.search_departure);
        if (mCreateShortcut) {
            searchButton.setText(getText(R.string.create_shortcut_label));
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
        
        final ImageButton fromDialog = (ImageButton) findViewById(R.id.site_menu);
        fromDialog.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDialog(DIALOG_HISTORY);
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mHistoryDbAdapter.close();
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        switch(id) {
        /*case DIALOG_MORE:
            return new AlertDialog.Builder(this)
                .setTitle(getText(R.string.choose_start_point_label))
                .setItems(getDialogSelectPointItems(),
                    new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int item) {
                    switch(item) {
                    case 0:
                        showDialog(DIALOG_HISTORY);
                        break;
                    }
                }
            })
            .create();*/
        case DIALOG_HISTORY:
            final Cursor historyCursor =
                mHistoryDbAdapter.fetchByType(HistoryDbAdapter.TYPE_DEPARTURE_SITE);
            startManagingCursor(historyCursor);
            return new AlertDialog.Builder(this)
                .setTitle(getText(R.string.history_label))
                .setCursor(historyCursor, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        int index = historyCursor
                                .getColumnIndex(HistoryDbAdapter.KEY_NAME);
                        mSiteTextView.setText(historyCursor.getString(index));
                    }
                }, HistoryDbAdapter.KEY_NAME)
                .create();
        }
        return null;
    }

    /*private CharSequence[] getDialogSelectPointItems() {
        CharSequence[] items = { 
                getText(R.string.history_label),
            };
        return items;
    }*/

    private void onSearchDepartures(String siteName) {
        mHistoryDbAdapter.create(HistoryDbAdapter.TYPE_DEPARTURE_SITE, siteName);

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

}

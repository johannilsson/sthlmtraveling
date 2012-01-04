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
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Parcelable;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.AutoCompleteTextView;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

import com.markupartist.sthlmtraveling.provider.HistoryDbAdapter;
import com.markupartist.sthlmtraveling.provider.planner.Planner;
import com.markupartist.sthlmtraveling.provider.planner.Stop;

public class SearchDeparturesFragment extends BaseListFragment {
    static String TAG = "SearchDeparturesActivity";
    private static final int DIALOG_HISTORY = 0;
    private static final int DIALOG_MORE = 1;
    private boolean mCreateShortcut;
    private HistoryDbAdapter mHistoryDbAdapter;
    private AutoCompleteTextView mSiteTextView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // If the activity was started with the "create shortcut" action, we
        // remember this to change the behavior upon a search.
        if (Intent.ACTION_CREATE_SHORTCUT.equals(getActivity().getIntent()
                .getAction())) {
            mCreateShortcut = true;
            registerEvent("Create departure shortcut");
        } else {
            registerEvent("Search departures");
        }
        mHistoryDbAdapter = new HistoryDbAdapter(getActivity()).open();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        return inflater.inflate(R.layout.search_departures_fragment, container,
                false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        findViews();
        fillData();
        super.onActivityCreated(savedInstanceState);
    }

    private void findViews() {
        View searchHeader = getActivity().getLayoutInflater().inflate(
                R.layout.search_departures_header, null);
        getListView().addHeaderView(searchHeader, null, false);

        mSiteTextView = (AutoCompleteTextView) searchHeader
                .findViewById(R.id.sites);
        AutoCompleteStopAdapter stopAdapter = new AutoCompleteStopAdapter(
                getActivity(), R.layout.simple_dropdown_item_1line,
                Planner.getInstance(), false);

        mSiteTextView.setSelectAllOnFocus(true);
        mSiteTextView.setAdapter(stopAdapter);

        mSiteTextView.setOnEditorActionListener(new OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId,
                    KeyEvent event) {
                boolean isEnterKey = (null != event && event.getKeyCode() == KeyEvent.KEYCODE_ENTER);
                if (actionId == EditorInfo.IME_ACTION_SEARCH
                        || true == isEnterKey) {
                    dispatchSearch();
                    return true;
                }
                return false;
            }
        });

        ImageButton searchButton = (ImageButton) searchHeader
                .findViewById(R.id.search_departure);
        if (mCreateShortcut) {
            // searchButton.setText(getText(R.string.create_shortcut_label));
            getActivity()
                    .setTitle(R.string.create_shortcut_for_departure_label);
        }
        searchButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                dispatchSearch();
            }
        });

    }

    @Override
    public void onDestroyView() {
        setListAdapter(null);
        super.onDestroyView();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mHistoryDbAdapter.close();
    }

    private void fillData() {
        final Cursor historyCursor = mHistoryDbAdapter
                .fetchByType(HistoryDbAdapter.TYPE_DEPARTURE_SITE);

        getActivity().startManagingCursor(historyCursor);

        String[] from = new String[] { HistoryDbAdapter.KEY_NAME };

        int[] to = new int[] { android.R.id.text1 };

        final SimpleCursorAdapter favorites = new SimpleCursorAdapter(
                getActivity(), android.R.layout.simple_list_item_1,
                historyCursor, from, to);

        getActivity().stopManagingCursor(historyCursor);

        setListAdapter(favorites);
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        Cursor historyCursor = ((SimpleCursorAdapter) this.getListAdapter())
                .getCursor();
        getActivity().startManagingCursor(historyCursor);
        int index = historyCursor.getColumnIndex(HistoryDbAdapter.KEY_NAME);
        String siteName = historyCursor.getString(index);
        getActivity().stopManagingCursor(historyCursor);

        Stop stop = new Stop();
        stop.setName(siteName);
        if (mCreateShortcut) {
            onCreateShortCut(stop);
        } else {
            onSearchDepartures(stop);
        }
    }

    private void dispatchSearch() {
        Stop stop = new Stop();
        stop.setName(mSiteTextView.getText().toString());
        if (!stop.looksValid()) {
            mSiteTextView.setError(getText(R.string.empty_value));
        } else {
            if (mCreateShortcut) {
                onCreateShortCut(stop);
            } else {
                onSearchDepartures(stop);
            }
        }
    }

    private void onSearchDepartures(Stop stop) {
        mHistoryDbAdapter.create(HistoryDbAdapter.TYPE_DEPARTURE_SITE, stop);

        Intent i = new Intent(getActivity().getApplicationContext(),
                DeparturesActivity.class);
        i.putExtra(DeparturesActivity.EXTRA_SITE_NAME, stop.getName());
        startActivity(i);
    }

    private void onCreateShortCut(Stop stop) {
        // Setup the intent to be launched
        Intent shortcutIntent = new Intent(getActivity()
                .getApplicationContext(), DeparturesActivity.class);
        shortcutIntent.setAction(Intent.ACTION_VIEW);
        shortcutIntent.putExtra(DeparturesActivity.EXTRA_SITE_NAME, stop.getName());
        shortcutIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        // Then, set up the container intent (the response to the caller)
        Intent intent = new Intent();
        intent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
        intent.putExtra(Intent.EXTRA_SHORTCUT_NAME, stop.getName());
        Parcelable iconResource = Intent.ShortcutIconResource.fromContext(
                getActivity(), R.drawable.shortcut_departures);
        intent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, iconResource);

        // Now, return the result to the launcher
        getActivity().setResult(Activity.RESULT_OK, intent);
        getActivity().finish();
    }
}

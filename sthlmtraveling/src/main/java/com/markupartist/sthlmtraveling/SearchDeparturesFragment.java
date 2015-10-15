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
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.markupartist.sthlmtraveling.provider.HistoryDbAdapter;
import com.markupartist.sthlmtraveling.provider.site.Site;

public class SearchDeparturesFragment extends BaseListFragment implements LoaderManager.LoaderCallbacks<Cursor> {
    private static final int REQUEST_CODE_PICK_SITE = 1;
    private static final int LOADER_HISTORY = 1;
    static String TAG = "SearchDeparturesActivity";
    private boolean mCreateShortcut;
    private HistoryDbAdapter mHistoryDbAdapter;
    private HistoryAdapter mHistoryAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // If the activity was started with the "create shortcut" action, we
        // remember this to change the behavior upon a search.
        if (Intent.ACTION_CREATE_SHORTCUT.equals(getActivity().getIntent()
                .getAction())) {
            mCreateShortcut = true;
            registerScreen("Create departure shortcut");
        }

        mHistoryDbAdapter = new HistoryDbAdapter(getActivity()).open();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.search_departures_fragment, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mHistoryAdapter = new HistoryAdapter(getActivity());
        initViews();
        setListAdapter(mHistoryAdapter);
    }

    @Override
    public void onResume() {
        super.onResume();

        getLoaderManager().restartLoader(LOADER_HISTORY, null, this);
    }

    private void initViews() {
        View searchHeader = getActivity().getLayoutInflater().inflate(
                R.layout.search_departures_header, null);
        getListView().addHeaderView(searchHeader, null, false);

        searchHeader.findViewById(R.id.sites).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent searchIntent = new Intent(getActivity(), PlaceSearchActivity.class);
                searchIntent.putExtra(PlaceSearchActivity.ARG_ONLY_STOPS, true);
                startActivityForResult(searchIntent, REQUEST_CODE_PICK_SITE);
            }
        });

        searchHeader.findViewById(R.id.btn_nearby_stops).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(getActivity().getApplicationContext(),
                        NearbyActivity.class);
                startActivityWithDefaultTransition(i);
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

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        Cursor historyCursor = ((HistoryAdapter) this.getListAdapter()).getCursor();
        Site site = HistoryDbAdapter.mapToSite(historyCursor);
        if (mCreateShortcut) {
            onCreateShortCut(site);
        } else {
            onSearchDepartures(site);
        }
    }

    private void dispatchSearch(Site stop) {
        if (stop.hasName()) {
            if (mCreateShortcut) {
                onCreateShortCut(stop);
            } else {
                onSearchDepartures(stop);
            }
        }
    }

    private void onSearchDepartures(Site stop) {
        mHistoryDbAdapter.create(HistoryDbAdapter.TYPE_DEPARTURE_SITE, stop);

        Intent i = new Intent(getActivity().getApplicationContext(),
                DeparturesActivity.class);
        i.putExtra(DeparturesActivity.EXTRA_SITE, stop);
        startActivityWithDefaultTransition(i);
    }

    private void onCreateShortCut(Site stop) {
        // Setup the intent to be launched
        Intent shortcutIntent = new Intent(getActivity()
                .getApplicationContext(), DeparturesShortcutProxyActivity.class);
        shortcutIntent.setAction(Intent.ACTION_VIEW);
        shortcutIntent.putExtra(DeparturesActivity.EXTRA_SITE_NAME, stop.getName());
        shortcutIntent.putExtra(DeparturesActivity.EXTRA_SITE_ID, stop.getId());
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

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        getLoaderManager().restartLoader(LOADER_HISTORY, null, this);
        switch (requestCode) {
            case REQUEST_CODE_PICK_SITE:
                if (resultCode == Activity.RESULT_OK) {
                    dispatchSearch((Site) data.getParcelableExtra(PlaceSearchActivity.EXTRA_PLACE));
                }
                break;
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        switch (id) {
            case LOADER_HISTORY:
                return new HistoryLoader(getActivity(), mHistoryDbAdapter);
        }
        return null;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        switch (loader.getId()) {
            case LOADER_HISTORY:
                mHistoryAdapter.swapCursor(data);
                break;
        }
    }


    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        switch (loader.getId()) {
            case LOADER_HISTORY:
                mHistoryAdapter.swapCursor(null);
                break;
        }
    }


    private static class HistoryLoader extends CursorLoader {

        private HistoryDbAdapter mHistoryDbAdapter;

        public HistoryLoader(Context context, HistoryDbAdapter historyDbAdapter) {
            super(context);
            mHistoryDbAdapter = historyDbAdapter;
        }

        @Override
        public Cursor loadInBackground() {
            return mHistoryDbAdapter.fetchByType(HistoryDbAdapter.TYPE_DEPARTURE_SITE);
        }

    }

    private static class HistoryAdapter extends CursorAdapter {

        public HistoryAdapter(Context context) {
            super(context, null, CursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER);
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            return LayoutInflater.from(context).inflate(R.layout.row_place_search, parent, false);
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            Site site = HistoryDbAdapter.mapToSite(cursor);
            ((TextView) view.findViewById(R.id.text1)).setText(site.getName());
            TextView text2 = (TextView) view.findViewById(R.id.text2);
            ImageView icon = (ImageView) view.findViewById(R.id.icon);
            if (site.getLocality() != null) {
                text2.setText(site.getLocality());
                text2.setVisibility(View.VISIBLE);
            } else {
                text2.setVisibility(View.GONE);
            }
            icon.setImageResource(R.drawable.ic_history_24dp);
        }
    }
}

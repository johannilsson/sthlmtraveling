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

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

import com.markupartist.sthlmtraveling.provider.HistoryDbAdapter;
import com.markupartist.sthlmtraveling.provider.planner.Planner;
import com.markupartist.sthlmtraveling.provider.site.Site;
import com.markupartist.sthlmtraveling.ui.view.DelayAutoCompleteTextView;
import com.markupartist.sthlmtraveling.utils.RtlUtils;
import com.markupartist.sthlmtraveling.utils.ViewHelper;

public class SearchDeparturesFragment extends BaseListFragment implements AdapterView.OnItemClickListener {
    static String TAG = "SearchDeparturesActivity";
    private boolean mCreateShortcut;
    private HistoryDbAdapter mHistoryDbAdapter;
    private DelayAutoCompleteTextView mSiteTextView;

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
        return inflater.inflate(R.layout.search_departures_fragment, container,
                false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        initViews();
        fillData();
        super.onActivityCreated(savedInstanceState);
    }

    private void initViews() {
        View searchHeader = getActivity().getLayoutInflater().inflate(
                R.layout.search_departures_header, null);
        getListView().addHeaderView(searchHeader, null, false);

        final ImageButton clearButton = (ImageButton) searchHeader.findViewById(R.id.btn_clear);
        ViewHelper.tintIcon(clearButton.getDrawable(), Color.GRAY);
        clearButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mSiteTextView.setText("");
            }
        });

        mSiteTextView = (DelayAutoCompleteTextView) searchHeader
                .findViewById(R.id.sites);
        AutoCompleteStopAdapter stopAdapter = new AutoCompleteStopAdapter(
                getActivity(), R.layout.simple_dropdown_item_1line,
                Planner.getInstance(), true);

        mSiteTextView.setSelectAllOnFocus(true);
        mSiteTextView.setAdapter(stopAdapter);

        mSiteTextView.setOnEditorActionListener(new OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId,
                                          KeyEvent event) {
                boolean isEnterKey = (null != event && event.getKeyCode() == KeyEvent.KEYCODE_ENTER);
                if (actionId == EditorInfo.IME_ACTION_SEARCH
                        || isEnterKey) {
                    dispatchSearch();
                    return true;
                }
                return false;
            }
        });
        mSiteTextView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @SuppressLint("RtlHardcoded")
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (TextUtils.isEmpty(s)) {
                    clearButton.setVisibility(View.INVISIBLE);
                } else {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                        FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) clearButton.getLayoutParams();
                        if (RtlUtils.isRtl(s)) {
                            layoutParams.gravity = Gravity.LEFT;
                        } else {
                            layoutParams.gravity = Gravity.RIGHT;
                        }
                        clearButton.setLayoutParams(layoutParams);
                    }

                    clearButton.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
        mSiteTextView.setOnItemClickListener(this);

        searchHeader.findViewById(R.id.btn_nearby_stops).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(getActivity().getApplicationContext(),
                        NearbyActivity.class);
                startActivity(i);
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

        String[] from = new String[]{HistoryDbAdapter.KEY_NAME};

        int[] to = new int[]{android.R.id.text1};

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

        Site site = mHistoryDbAdapter.mapToSite(historyCursor);

        getActivity().stopManagingCursor(historyCursor);

        if (mCreateShortcut) {
            onCreateShortCut(site);
        } else {
            onSearchDepartures(site);
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Site site = ((AutoCompleteStopAdapter) mSiteTextView.getAdapter()).getValue(position);
        if (mCreateShortcut) {
            onCreateShortCut(site);
        } else {
            onSearchDepartures(site);
        }
    }

    private void dispatchSearch() {
        Site stop = new Site();
        stop.setName(mSiteTextView.getText().toString());

        AutoCompleteStopAdapter autoCompleteAdapter = (AutoCompleteStopAdapter) mSiteTextView.getAdapter();
        //autoCompleteAdapter.getValue(pos);

        // TODO: Change to use looksValid and make sure site id is passed all the way.
        if (!stop.hasName()) {
            mSiteTextView.setError(getText(R.string.empty_value));
        } else {
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
        startActivity(i);
    }

    private void onCreateShortCut(Site stop) {
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

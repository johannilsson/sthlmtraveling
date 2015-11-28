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

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.view.ViewCompat;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CursorAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.markupartist.sthlmtraveling.provider.HistoryDbAdapter;
import com.markupartist.sthlmtraveling.provider.JourneysProvider.Journey.Journeys;
import com.markupartist.sthlmtraveling.provider.TransportMode;
import com.markupartist.sthlmtraveling.provider.planner.JourneyQuery;
import com.markupartist.sthlmtraveling.provider.site.Site;
import com.markupartist.sthlmtraveling.utils.ViewHelper;

import org.json.JSONException;
import org.json.JSONObject;

public class PlannerFragment extends BaseListFragment implements LoaderManager.LoaderCallbacks<Cursor> {
    private static final String TAG = "PlannerFragment";
    protected static final int REQUEST_CODE_POINT_ON_MAP_START = 0;
    protected static final int REQUEST_CODE_POINT_ON_MAP_END = 1;
    private static final int REQUEST_CODE_ROUTE_OPTIONS = 2;
    private static final int REQUEST_CODE_PICK_START = 3;
    private static final int REQUEST_CODE_PICK_END = 4;

    private static final int LOADER_JOURNEY_HISTORY = 1;

    /**
     * The Journey
     */
    static final String EXTRA_JOURNEY_QUERY = "sthlmtraveling.intent.action.JOURNEY_QUERY";

    private TextView mStartPointAutoComplete;
    private TextView mEndPointAutoComplete;
    private Site mStartPoint = new Site();
    private Site mEndPoint = new Site();
    private HistoryDbAdapter mHistoryDbAdapter;
    private boolean mCreateShortcut;
    private View mSearchView;
    private JourneyAdapter mAdapter;
    private JourneyQuery mJourneyQuery;
    private int mStackLevel;


    /**
     * The columns needed by the cursor adapter
     */
    private static final String[] PROJECTION = new String[] { Journeys._ID, // 0
            Journeys.JOURNEY_DATA, // 1
            Journeys.POSITION, // 2
            Journeys.STARRED, // 3
    };

    /**
     * The index of the id column
     */
    private static final int COLUMN_INDEX_ID = 0;

    /**
     * The index of the journey data column
     */
    private static final int COLUMN_INDEX_JOURNEY_DATA = 1;

    /**
     * The index of the starred column
     */
    private static final int COLUMN_INDEX_STARRED = 3;
    private View mOptionsBarView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // If the activity was started with the "create shortcut" action, we
        // remember this to change the behavior upon a search.
        if (Intent.ACTION_CREATE_SHORTCUT.equals(getActivity().getIntent().getAction())) {
            mCreateShortcut = true;
        }

        if (savedInstanceState != null) {
            mStackLevel = savedInstanceState.getInt("level");
            mJourneyQuery = savedInstanceState.getParcelable(EXTRA_JOURNEY_QUERY);
        }

        getLoaderManager().initLoader(LOADER_JOURNEY_HISTORY, null, this);

        mAdapter = new JourneyAdapter(getActivity(), null);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.planner_list_fragment, container, false);

        ImageButton preferenceButton = (ImageButton) rootView.findViewById(R.id.btn_settings);

        ViewHelper.tintIcon(preferenceButton.getDrawable(), Color.GRAY);

        preferenceButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent settingsIntent = new Intent().setClass(getActivity(), SettingsActivity.class);
                startActivityWithDefaultTransition(settingsIntent);
            }
        });

        return rootView;
    }

    public void initViews() {
        getListView().setVerticalFadingEdgeEnabled(false);
        getListView().setHorizontalFadingEdgeEnabled(false);

        mSearchView = getActivity().getLayoutInflater().inflate(R.layout.search, getListView(), false);
        getListView().addHeaderView(mSearchView, null, false);

        // Hide dividers on the header view.
        getListView().setHeaderDividersEnabled(false);

        if (!mStartPoint.hasName()) {
            mStartPoint.setName(Site.TYPE_MY_LOCATION);
        }
        mStartPointAutoComplete = createTextViewForStartEnd(R.id.from, mStartPoint);
        mStartPointAutoComplete.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivityForResult(new Intent(getActivity(), PlaceSearchActivity.class), REQUEST_CODE_PICK_START);
            }
        });

        mEndPointAutoComplete = createTextViewForStartEnd(R.id.to, mEndPoint);
        mEndPointAutoComplete.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivityForResult(new Intent(getActivity(), PlaceSearchActivity.class), REQUEST_CODE_PICK_END);
            }
        });

        try {
            mHistoryDbAdapter = new HistoryDbAdapter(getActivity()).open();
        } catch (final Exception e) {
            showDialog(createDialogReinstallApp());
            return;
        }

        Button searchButton = (Button) mSearchView.findViewById(R.id.do_search);
        if (mCreateShortcut) {
            searchButton.setText(getText(R.string.create_shortcut_label));
        }
        searchButton.setText(((String)searchButton.getText()).toUpperCase());
        searchButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                handleSearchAction();
            }
        });

        Button optionsButton = (Button) mSearchView.findViewById(R.id.btn_options);
        optionsButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mJourneyQuery == null) {
                    mJourneyQuery = new JourneyQuery.Builder().create();
                }

                Intent i = new Intent(getActivity(), ChangeRouteTimeActivity.class);
                i.putExtra(EXTRA_JOURNEY_QUERY, mJourneyQuery);
                startActivityForResult(i, REQUEST_CODE_ROUTE_OPTIONS);
            }
        });

        mOptionsBarView = mSearchView.findViewById(R.id.options_active_container);

        mSearchView.findViewById(R.id.reverse_start_end).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Site newStart = new Site(mEndPoint);
                Site newEnd = new Site(mStartPoint);

                mStartPoint = newStart;
                mEndPoint = newEnd;

                if (mStartPoint.isMyLocation()) {
                    mStartPointAutoComplete.setText(getText(R.string.my_location));
                } else {
                    mStartPointAutoComplete.setText(mStartPoint.getName());
                }

                if (mEndPoint.isMyLocation()) {
                    mEndPointAutoComplete.setText(getText(R.string.my_location));
                } else {
                    mEndPointAutoComplete.setText(mEndPoint.getName());
                }
            }
        });

        // Handle create shortcut.
        if (mCreateShortcut) {
            registerScreen("Planner create shortcut");
            getActivity().setTitle(R.string.create_shortcut_label);

            // Fake an adapter. This needs to be fixed later on so we can use the history.
            setListAdapter(new ArrayAdapter<String>(getActivity(), R.layout.row_journey));
        } else {
            setListAdapter(mAdapter);
        }

        showOrHideOptionsBar();
    }

    public void showOrHideOptionsBar() {
        if (mJourneyQuery != null) {
            mOptionsBarView.setVisibility(View.VISIBLE);

            ViewCompat.setAlpha(mOptionsBarView, 1);

            TextView optionsTimeView = (TextView) mOptionsBarView.findViewById(R.id.options_summary);

            String timeString = DateFormat.getTimeFormat(getActivity()).format(mJourneyQuery.time);
            String dateString = DateFormat.getMediumDateFormat(getActivity()).format(mJourneyQuery.time);

            if (mJourneyQuery.isTimeDeparture) {
                optionsTimeView.setText(getString(R.string.departing_on, timeString, dateString));
            } else {
                optionsTimeView.setText(getString(R.string.arriving_by, timeString, dateString));
            }

            mOptionsBarView.findViewById(R.id.btn_clear_options).setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    mJourneyQuery = null;
                    if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR1) {
                        mOptionsBarView.animate()
                                .alpha(0)
                                .setDuration(200)
                                .setInterpolator(new DecelerateInterpolator())
                                .setListener(new AnimatorListenerAdapter() {
                                    @Override
                                    public void onAnimationEnd(Animator animation) {
                                        mOptionsBarView.setVisibility(View.GONE);
                                    }
                                });
                    } else {
                        mOptionsBarView.setVisibility(View.GONE);
                    }
                }
            });

            if (mJourneyQuery.hasAdditionalFiltering()) {
                mOptionsBarView.findViewById(R.id.options_summary_with_options).setVisibility(View.VISIBLE);
            } else {
                mOptionsBarView.findViewById(R.id.options_summary_with_options).setVisibility(View.GONE);
            }
        } else {
            mOptionsBarView.setVisibility(View.GONE);
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Log.d(TAG, "PlannerFragment.onActivityCreated()");

        restoreState(savedInstanceState);

        initViews();
    }

    @Override
    public void onDestroyView() {
        Log.d(TAG, "PlannerFragment.onDestroyView()");
        setListAdapter(null);
        mHistoryDbAdapter.close();
        super.onDestroyView();
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        JourneyQuery journeyQuery = getJourneyQuery(mAdapter.getCursor());

        Intent routesIntent = new Intent(getActivity(), RoutesActivity.class);
        routesIntent.putExtra(RoutesActivity.EXTRA_JOURNEY_QUERY, journeyQuery);
        startActivity(routesIntent);
    }

    @Override
    public void onResume() {
        super.onResume();

        if (mStartPoint != null && !mStartPoint.hasName()) {
            mStartPointAutoComplete.setText("");
        }

        if (mEndPoint != null && !mEndPoint.hasName()) {
            mEndPointAutoComplete.setText("");
        }
    }

    private Site buildStop(Site site, TextView textView) {
        if (site.hasName()
                && site.getName().equals(textView.getText().toString())) {
            return site;
        } else if (site.isMyLocation()
                && textView.getText().toString()
                        .equals(getString(R.string.my_location))) {
            // Check for my location.
            return site;
        } else if (textView.getText().toString()
                .equals(getString(R.string.point_on_map))) {
            // Check for point-on-map.
            return site;
        }
        site.fromSite(null);
        return site;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putInt("level", mStackLevel);
        if (mStartPoint != null) outState.putParcelable("startPoint", mStartPoint);
        if (mEndPoint != null) outState.putParcelable("endPoint", mEndPoint);
        if (mJourneyQuery != null) outState.putParcelable(EXTRA_JOURNEY_QUERY, mJourneyQuery);

        super.onSaveInstanceState(outState);
    }

    private void restoreState(Bundle state) {
        mStartPoint = new Site();
        mEndPoint = new Site();
        if (state != null) {
            Site startPoint = state.getParcelable("startPoint");
            Site endPoint = state.getParcelable("endPoint");
            if (startPoint != null) mStartPoint.fromSite(startPoint);
            if (endPoint != null) mEndPoint.fromSite(endPoint);
        }
    }

    /**
     * Creates a new {@link AutoCompleteTextView}.
     * 
     * @param autoCompleteResId
     *            The {@link AutoCompleteTextView} resource id.
     * @param site
     *            The stop.
     *            If addresses should be included.
     * @return A AutoCompleteTextView
     */
    private TextView createTextViewForStartEnd(int autoCompleteResId, final Site site) {
        TextView autoCompleteTextView = (TextView) mSearchView.findViewById(autoCompleteResId);
        String name = site.getName();
        if (site.isMyLocation()) {
            name = getString(R.string.my_location);
        }
        autoCompleteTextView.setText(name);
        return autoCompleteTextView;
    }

    /**
     * Start the search.
     * 
     * @param startPoint
     *            the start point
     * @param endPoint
     *            the end point
     */
    private void onSearchRoutes(Site startPoint, Site endPoint) {
        // TODO: We should not handle point-on-map this way. But for now we just
        // want it to work.
        if (!mStartPointAutoComplete.getText().toString().equals(getString(R.string.point_on_map))) {
            mHistoryDbAdapter.create(HistoryDbAdapter.TYPE_JOURNEY_PLANNER_SITE, startPoint);
        }
        if (!mEndPointAutoComplete.getText().toString().equals(getString(R.string.point_on_map))) {
            mHistoryDbAdapter.create(HistoryDbAdapter.TYPE_JOURNEY_PLANNER_SITE, endPoint);
        }

        Log.i(TAG, "START POINT: " + startPoint.toDump());
        Log.i(TAG, "END POINT: " + endPoint.toDump());

        JourneyQuery journeyQuery = new JourneyQuery.Builder()
                .origin(startPoint)
                .destination(endPoint)
                .create();

        if (mJourneyQuery != null) {
            // todo: replace with a merge method.
            journeyQuery.via = mJourneyQuery.via;
            journeyQuery.alternativeStops = mJourneyQuery.alternativeStops;
            journeyQuery.time = mJourneyQuery.time;
            journeyQuery.isTimeDeparture = mJourneyQuery.isTimeDeparture;
            journeyQuery.transportModes = mJourneyQuery.transportModes;
        }

        Intent routesIntent = new Intent(getActivity(), RoutesActivity.class);
        routesIntent.putExtra(RoutesActivity.EXTRA_JOURNEY_QUERY, journeyQuery);
        startActivity(routesIntent);
    }

    /**
     * Setup a search short cut.
     * 
     * @param startPoint
     *            the start point
     * @param endPoint
     *            the end point
     */
    protected void onCreateShortCut(Site startPoint, Site endPoint, String name) {
        Uri routesUri = RoutesActivity.createRoutesUri(startPoint, endPoint,
                null, true);
        Intent shortcutIntent = new Intent(Intent.ACTION_VIEW, routesUri,
                getActivity(), RoutesActivity.class);
        shortcutIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        // Then, set up the container intent (the response to the caller)
        Intent intent = new Intent();
        intent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
        intent.putExtra(Intent.EXTRA_SHORTCUT_NAME, name);
        Parcelable iconResource = Intent.ShortcutIconResource.fromContext(
                getActivity(), R.drawable.shortcut);
        intent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, iconResource);

        // Now, return the result to the launcher
        getActivity().setResult(Activity.RESULT_OK, intent);
        getActivity().finish();
    }

    private void showDialog(Dialog dialog) {
        mStackLevel++;
        // DialogFragment.show() will take care of adding the fragment
        // in a transaction. We also want to remove any currently showing
        // dialog, so make our own transaction and take care of that here.
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        DialogFragment newFragment = PlannerDialogFragment.newInstance(dialog);
        ft.add(newFragment, null);
        ft.commit();
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        switch (id) {
            case LOADER_JOURNEY_HISTORY:
                return new CursorLoader(
                        getActivity(),
                        Journeys.CONTENT_URI,
                        PROJECTION,
                        null, //selection,
                        null, //selectionArgs,
                        Journeys.HISTORY_SORT_ORDER);
        }
        return null;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        switch (loader.getId()) {
            case LOADER_JOURNEY_HISTORY:
                mAdapter.changeCursor(data);
                break;
        }

    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        switch (loader.getId()) {
            case LOADER_JOURNEY_HISTORY:
                mAdapter.changeCursor(null);
                break;
        }
    }

    public static class PlannerDialogFragment extends DialogFragment {

        private static Dialog mDialog;

        static PlannerDialogFragment newInstance(Dialog dialog) {
            mDialog = dialog;
            return new PlannerDialogFragment();
        }

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            return mDialog;
        }
    }

    private Dialog createDialogReinstallApp() {
        return new AlertDialog.Builder(getActivity())
                .setTitle(getText(R.string.attention_label))
                .setMessage(getText(R.string.reinstall_app_message))
                .setPositiveButton(android.R.string.ok, null)
                .create();
    }

    private Dialog createDialogShortcutName() {
        final View chooseShortcutName = getActivity().getLayoutInflater()
                .inflate(R.layout.create_shortcut_name, null, false);
        final EditText shortCutName = (EditText) chooseShortcutName
                .findViewById(R.id.shortcut_name);
        return new AlertDialog.Builder(getActivity())
                .setTitle(R.string.create_shortcut_label)
                .setView(chooseShortcutName)
                .setPositiveButton(android.R.string.ok,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog,
                                    int which) {
                                onCreateShortCut(mStartPoint, mEndPoint,
                                        shortCutName.getText().toString());
                            }
                        }).create();
    }



    private void handleSearchAction() {
        if (TextUtils.isEmpty(mStartPointAutoComplete.getText())) {
            Log.d(TAG, "Start auto was empty");
        } else if (TextUtils.isEmpty(mEndPointAutoComplete.getText())) {
            Log.d(TAG, "End auto was empty");
        } else {
            mStartPoint = buildStop(mStartPoint, mStartPointAutoComplete);
            mEndPoint = buildStop(mEndPoint, mEndPointAutoComplete);
            if (mCreateShortcut) {
                showDialog(createDialogShortcutName());
                // onCreateShortCut(mStartPoint, mEndPoint);
            } else {
                onSearchRoutes(mStartPoint, mEndPoint);
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CODE_POINT_ON_MAP_START:
                if (resultCode == Activity.RESULT_CANCELED) {
                    Log.d(TAG, "action canceled");
                } else {
                    mStartPoint = data.getParcelableExtra(PointOnMapActivity.EXTRA_STOP);
                    mStartPointAutoComplete.setText(getText(R.string.point_on_map));
                    // mStartPointAutoComplete.setText(mStartPoint.getName());
                    Log.d(TAG, "Got Stop " + mStartPoint);
                }
                break;
            case REQUEST_CODE_POINT_ON_MAP_END:
                if (resultCode == Activity.RESULT_CANCELED) {
                    Log.d(TAG, "action canceled");
                } else {
                    mEndPoint = data.getParcelableExtra(PointOnMapActivity.EXTRA_STOP);
                    mEndPointAutoComplete.setText(getText(R.string.point_on_map));
                    // mEndPointAutoComplete.setText(mEndPoint.getName());
                    Log.d(TAG, "Got Stop " + mEndPoint);
                }
                break;
            case REQUEST_CODE_ROUTE_OPTIONS:
                if (resultCode == Activity.RESULT_CANCELED) {
                    Log.d(TAG, "Route options cancelled.");
                } else {
                    mJourneyQuery = data.getParcelableExtra(EXTRA_JOURNEY_QUERY);
                    showOrHideOptionsBar();
                }
                break;
            case REQUEST_CODE_PICK_START:
                if (resultCode == Activity.RESULT_CANCELED) {
                    Log.d(TAG, "Pick start cancelled.");
                } else {
                    mStartPoint = data.getParcelableExtra(PlaceSearchActivity.EXTRA_PLACE);
                    if (mStartPoint.isMyLocation()) {
                        mStartPointAutoComplete.setText(getText(R.string.my_location));
                    } else {
                        Log.d(TAG, "Got startpoint: " + mStartPoint.toDump());
                        mStartPointAutoComplete.setText(mStartPoint.getName());
                        mEndPointAutoComplete.requestLayout();
                    }
                }
                break;
            case REQUEST_CODE_PICK_END:
                if (resultCode == Activity.RESULT_CANCELED) {
                    Log.d(TAG, "Pick start cancelled.");
                } else {
                    mEndPoint = data.getParcelableExtra(PlaceSearchActivity.EXTRA_PLACE);
                    if (mEndPoint.isMyLocation()) {
                        mEndPointAutoComplete.setText(getText(R.string.my_location));
                    } else {
                        Log.d(TAG, "Got endpoint: " + mEndPoint.toDump());
                        mEndPointAutoComplete.setText(mEndPoint.getName());
                        mEndPointAutoComplete.requestLayout();
                    }
                }
                break;
        }
    }


    private class JourneyAdapter extends CursorAdapter {

        public JourneyAdapter(Context context, Cursor c) {
            super(context, c, true);
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            JourneyQuery journeyQuery = getJourneyQuery(cursor);
            if (journeyQuery != null) {
                inflateView(view, journeyQuery, cursor);
            }
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            final LayoutInflater inflater = LayoutInflater.from(context);

            JourneyQuery journeyQuery = getJourneyQuery(cursor);
            View v;
            if (journeyQuery != null) {
                v = inflater.inflate(R.layout.row_journey, parent,
                        false);
                inflateView(v, journeyQuery, cursor);
            } else {
                v = new View(getActivity());
            }

            return v;
        }

        private View inflateView(View v, JourneyQuery journeyQuery, Cursor c) {
            String originStr;
            String destinationStr;
            if (journeyQuery.origin.isMyLocation()) {
                originStr = getString(R.string.my_location);
            } else {
                originStr = journeyQuery.origin.getName();
            }
            if (journeyQuery.destination.isMyLocation()) {
                destinationStr = getString(R.string.my_location);
            } else {
                destinationStr = journeyQuery.destination.getName();
            }

            TextView journeyDescriptionText = (TextView) v.findViewById(R.id.favorite_journey_title);
            journeyDescriptionText.setText(originStr);

            TextView journeyToText = (TextView) v.findViewById(R.id.favorite_to);
            journeyToText.setText(destinationStr);

            TextView viaText = (TextView) v.findViewById(R.id.favorite_via_point);
            if (journeyQuery.hasVia()) {
                viaText.setText(journeyQuery.via.getName());
                viaText.setVisibility(View.VISIBLE);
            } else {
                viaText.setVisibility(View.GONE);
            }

            addTransportModeViews(journeyQuery, v);

            CheckBox starred = (CheckBox) v
                    .findViewById(R.id.journey_star_check);
            boolean isStarred = c.getInt(COLUMN_INDEX_STARRED) == 1;
            if (isStarred) {
                starred.setChecked(true);
            } else {
                starred.setChecked(false);
            }

            final int id = c.getInt(COLUMN_INDEX_ID);

            // TODO: Refactor so we can re-use the same click listener.
            // We're using a click listener instead of an checked listener to
            // avoid callbacks if the list is modified from code.
            starred.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {
                    boolean isChecked = ((CheckBox) v).isChecked();
                    Uri uri = ContentUris.withAppendedId(Journeys.CONTENT_URI, id);
                    ContentValues values = new ContentValues();
                    if (isChecked) {
                        values.put(Journeys.STARRED, 1);
                    } else {
                        values.put(Journeys.STARRED, 0);
                    }
                    getActivity().getContentResolver().update(uri, values,
                            null, null);

                }
            });

            return v;
        }

        private void addTransportModeViews(JourneyQuery journeyQuery, View v) {
            ImageView metroView = (ImageView) v.findViewById(R.id.favorite_transport_mode_metro);
            ImageView busView = (ImageView) v.findViewById(R.id.favorite_transport_mode_bus);
            ImageView trainView = (ImageView) v.findViewById(R.id.favorite_transport_mode_train);
            ImageView tramView = (ImageView) v.findViewById(R.id.favorite_transport_mode_tram);
            ImageView waxView = (ImageView) v.findViewById(R.id.favorite_transport_mode_wax);

            int inactiveColor = ContextCompat.getColor(getActivity(), R.color.transport_icon_inactive);
            int activeColor = ContextCompat.getColor(getActivity(), R.color.icon_default);

            if (journeyQuery.transportModes == null || journeyQuery.transportModes.isEmpty()) {
                inactiveColor = activeColor;
            }

            ViewHelper.tint(metroView, inactiveColor);
            ViewHelper.tint(busView, inactiveColor);
            ViewHelper.tint(trainView, inactiveColor);
            ViewHelper.tint(tramView, inactiveColor);
            ViewHelper.tint(waxView, inactiveColor);

            if (journeyQuery.transportModes != null) {
                for (String transportMode : journeyQuery.transportModes) {
                    switch (transportMode) {
                        case TransportMode.METRO:
                            ViewHelper.tint(metroView, activeColor);
                            break;
                        case TransportMode.BUS:
                            ViewHelper.tint(busView, activeColor);
                            break;
                        case TransportMode.TRAIN:
                            ViewHelper.tint(trainView, activeColor);
                            break;
                        case TransportMode.TRAM:
                            ViewHelper.tint(tramView, activeColor);
                            break;
                        case TransportMode.WAX:
                            ViewHelper.tint(waxView, activeColor);
                            break;
                    }
                }
            }
        }
    }

    private static JourneyQuery getJourneyQuery(Cursor cursor) {
        String jsonJourneyQuery;
        try {
            // TODO: Investigate if we can add some kind of caching here.
            jsonJourneyQuery = cursor.getString(COLUMN_INDEX_JOURNEY_DATA);
        } catch (Exception e) {
            Log.e(TAG, "Failed to retrieve journey data from the cursor.");
            return null;
        }

        JourneyQuery journeyQuery = null;
        try {
            journeyQuery = JourneyQuery.fromJson(new JSONObject(
                    jsonJourneyQuery));
        } catch (JSONException e) {
            e.printStackTrace();
            Log.e(TAG, "Failed to convert to journey from json.");
        }
        return journeyQuery;
    }

}

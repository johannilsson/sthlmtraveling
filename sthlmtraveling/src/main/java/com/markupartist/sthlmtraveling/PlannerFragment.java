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
import java.util.HashMap;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.CursorLoader;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.format.Time;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.CursorAdapter;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SimpleAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.markupartist.sthlmtraveling.AutoCompleteStopAdapter.FilterListener;
import com.markupartist.sthlmtraveling.provider.HistoryDbAdapter;
import com.markupartist.sthlmtraveling.provider.JourneysProvider.Journey.Journeys;
import com.markupartist.sthlmtraveling.provider.TransportMode;
import com.markupartist.sthlmtraveling.provider.planner.JourneyQuery;
import com.markupartist.sthlmtraveling.provider.planner.Planner;
import com.markupartist.sthlmtraveling.provider.planner.Planner.Location;
import com.markupartist.sthlmtraveling.provider.site.Site;

public class PlannerFragment extends BaseListFragment implements
        OnCheckedChangeListener {
    private static final String TAG = "PlannerFragment";
    protected static final int REQUEST_CODE_POINT_ON_MAP_START = 0;
    protected static final int REQUEST_CODE_POINT_ON_MAP_END = 1;

    private AutoCompleteTextView mStartPointAutoComplete;
    private AutoCompleteTextView mEndPointAutoComplete;
    private AutoCompleteTextView mViaPointAutoComplete;
    private Site mStartPoint = new Site();
    private Site mEndPoint = new Site();
    private Site mViaPoint = new Site();
    private HistoryDbAdapter mHistoryDbAdapter;
    private boolean mCreateShortcut;
    private Time mTime;
    private Button mDateButton;
    private Button mTimeButton;
    private LinearLayout mChangeTimeLayout;
    private Spinner mWhenSpinner;
    private View mSearchView;
    private JourneyAdapter mAdapter;

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

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // If the activity was started with the "create shortcut" action, we
        // remember this to change the behavior upon a search.
        if (Intent.ACTION_CREATE_SHORTCUT.equals(getActivity().getIntent().getAction())) {
            mCreateShortcut = true;
        }

        // Enable options menu
        setHasOptionsMenu(true);

        if (savedInstanceState != null) {
            mStackLevel = savedInstanceState.getInt("level");
        }

        CursorLoader cursorLoader = new CursorLoader(
                getActivity(),
                Journeys.CONTENT_URI,
                PROJECTION,
                null, //selection,
                null, //selectionArgs,
                Journeys.HISTORY_SORT_ORDER);
        Cursor cursor = cursorLoader.loadInBackground();
        mAdapter = new JourneyAdapter(getActivity(), cursor);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        return inflater.inflate(R.layout.planner_list_fragment, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        Log.d(TAG, "PlannerFragment.onActivityCreated()");

        restoreState(savedInstanceState);

        mSearchView = getActivity().getLayoutInflater().inflate(R.layout.search, null);
        getListView().addHeaderView(mSearchView, null, false);

        final TextView historyView = (TextView) getActivity()
                .getLayoutInflater().inflate(R.layout.header, null);
        historyView.setText(R.string.history_label);
        historyView.setOnClickListener(null); // Makes the header un-clickable, hack!
        getListView().addHeaderView(historyView);

        if (!mStartPoint.hasName()) {
            mStartPoint.setName(Site.TYPE_MY_LOCATION);
        }
        mStartPointAutoComplete = createAutoCompleteTextView(R.id.from,
                R.id.from_progress, mStartPoint);
        mEndPointAutoComplete = createAutoCompleteTextView(R.id.to,
                R.id.to_progress, mEndPoint);
        mViaPointAutoComplete = createAutoCompleteTextView(R.id.via,
                R.id.via_progress, mViaPoint, true);

        try {
            mHistoryDbAdapter = new HistoryDbAdapter(getActivity()).open();
        } catch (final Exception e) {
            showDialog(createDialogReinstallApp());
            return;
        }

        // Setup view for choosing other data for start and end point.
        final ImageButton fromDialog = (ImageButton) mSearchView.findViewById(R.id.from_menu);
        fromDialog.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                mStartPointAutoComplete.setError(null);
                showDialog(createDialogStartPoint());
            }
        });
        final ImageButton toDialog = (ImageButton) mSearchView.findViewById(R.id.to_menu);
        toDialog.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                mEndPointAutoComplete.setError(null);
                showDialog(createDialogEndPoint());
            }
        });
        final ImageButton viaDialog = (ImageButton) mSearchView.findViewById(R.id.via_menu);
        viaDialog.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                mViaPointAutoComplete.setError(null);
                showDialog(createDialogViaPoint());
            }
        });
        // Views for date and time
        mChangeTimeLayout = (LinearLayout)
                mSearchView.findViewById(R.id.planner_change_time_layout);

        mDateButton = (Button) mSearchView.findViewById(R.id.planner_route_date);
        mDateButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(final View v) {
                showDialog(createDialogDate());
            }
        });

        mTimeButton = (Button) mSearchView.findViewById(R.id.planner_route_time);
        mTimeButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(final View v) {
                showDialog(createDialogTime());
            }
        });

        // Views for radio buttons
        final RadioButton nowRadioButton = (RadioButton)
                mSearchView.findViewById(R.id.planner_check_now);
        nowRadioButton.setOnCheckedChangeListener(this);
        final RadioButton laterRadioButton = (RadioButton)
                mSearchView.findViewById(R.id.planner_check_more_choices);
        laterRadioButton.setOnCheckedChangeListener(this);

        mWhenSpinner = (Spinner) mSearchView.findViewById(R.id.departure_arrival_choice);
        final ArrayAdapter<CharSequence> whenChoiceAdapter =
                ArrayAdapter.createFromResource(getActivity(),
                        R.array.when_choice, android.R.layout.simple_spinner_item);
        whenChoiceAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mWhenSpinner.setAdapter(whenChoiceAdapter);

        // Handle create shortcut.
        if (mCreateShortcut) {
            registerEvent("Planner create shortcut");
            getActivity().setTitle(R.string.create_shortcut_label);
            RadioGroup chooseTimeGroup = (RadioGroup) mSearchView
                    .findViewById(R.id.planner_choose_time_group);
            chooseTimeGroup.setVisibility(View.GONE);
            historyView.setVisibility(View.GONE);
            // Fake an adapter. This needs to be fixed later on so we can use the history.
            setListAdapter(new ArrayAdapter<String>(getActivity(), R.layout.journey_history_row));
        } else {
            registerEvent("Planner");
            setListAdapter(mAdapter);
        }


        super.onActivityCreated(savedInstanceState);
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

        if (mViaPoint != null && !mViaPoint.hasName()) {
            mViaPointAutoComplete.setText("");
        }
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        if (mCreateShortcut) {
            MenuItem item = menu.findItem(R.id.actionbar_search_route);
            item.setTitle(R.string.create_shortcut_label);
        }
        super.onPrepareOptionsMenu(menu);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
    	inflater.inflate(R.menu.options_menu_search, menu);
    	super.onCreateOptionsMenu(menu, inflater);
    }

    private Site buildStop(Site site, AutoCompleteTextView auTextView) {
        if (site.hasName()
                && site.getName().equals(auTextView.getText().toString())) {
            return site;
        } else if (site.isMyLocation()
                && auTextView.getText().toString()
                        .equals(getString(R.string.my_location))) {
            // Check for my location.
            return site;
        } else if (auTextView.getText().toString()
                .equals(getString(R.string.point_on_map))) {
            // Check for point-on-map.
            return site;
        }
        return new Site();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putInt("level", mStackLevel);
        if (mStartPoint != null) outState.putParcelable("startPoint", mStartPoint);
        if (mEndPoint != null) outState.putParcelable("endPoint", mEndPoint);
        if (mViaPoint != null) outState.putParcelable("viaPoint", mViaPoint);

        super.onSaveInstanceState(outState);
    }

    private void restoreState(Bundle state) {
        mStartPoint = new Site();
        mEndPoint = new Site();
        mViaPoint = new Site();
        if (state != null) {
            Site startPoint = state.getParcelable("startPoint");
            Site endPoint = state.getParcelable("endPoint");
            Site viaPoint = state.getParcelable("viaPoint");
            if (startPoint != null) mStartPoint.fromSite(startPoint);
            if (endPoint != null) mEndPoint.fromSite(endPoint);
            if (viaPoint != null) mViaPoint.fromSite(viaPoint);
        }
    }

    private AutoCompleteTextView createAutoCompleteTextView(
            int autoCompleteResId, int progressResId, final Site site) {
        return createAutoCompleteTextView(autoCompleteResId, progressResId,
                site, false);
    }

    /**
     * Creates a new {@link AutoCompleteTextView}.
     * 
     * @param autoCompleteResId
     *            The {@link AutoCompleteTextView} resource id.
     * @param progressResId
     *            The {@link ProgressBar} resource id.
     * @param site
     *            The stop.
     * @param includeAddresses
     *            If addresses should be included.
     * @return
     */
    private AutoCompleteTextView createAutoCompleteTextView(
            int autoCompleteResId, int progressResId, final Site site,
            boolean includeAddresses) {
        // TODO: Wrap the auto complete view in a custom view...
        final AutoCompleteTextView autoCompleteTextView = (AutoCompleteTextView)
                mSearchView.findViewById(autoCompleteResId);
        final ProgressBar progress = (ProgressBar)
                mSearchView.findViewById(progressResId);
        final AutoCompleteStopAdapter stopAdapter = new AutoCompleteStopAdapter(
                getActivity(), R.layout.autocomplete_item_2line,
                Planner.getInstance(), includeAddresses);

        autoCompleteTextView.addTextChangedListener(new ReservedNameTextWatcher(
                getText(R.string.my_location), autoCompleteTextView));
        autoCompleteTextView.addTextChangedListener(new ReservedNameTextWatcher(
                getText(R.string.point_on_map), autoCompleteTextView));
        autoCompleteTextView.addTextChangedListener(new UpdateStopTextWatcher(site));

        String name = site.getName();
        if (site.isMyLocation()) {
            name = getString(R.string.my_location);
        }
        autoCompleteTextView.setText(name);

        stopAdapter.setFilterListener(new FilterListener() {
            @Override
            public void onPublishFiltering() {
                progress.setVisibility(View.INVISIBLE);
            }

            @Override
            public void onPerformFiltering() {
                progress.setVisibility(View.VISIBLE);
            }
        });

        autoCompleteTextView.setSelectAllOnFocus(true);
        autoCompleteTextView.setAdapter(stopAdapter);

        autoCompleteTextView.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                    int position, long id) {
                Site v = stopAdapter.getValue(position);
                site.fromSite(v);
            }
        });

        /*
         * OLD autoCompleteTextView.setOnTouchListener(new OnTouchListener() {
         * 
         * @Override public boolean onTouch(View v, MotionEvent event) { int
         * stop = autoCompleteTextView.getText().length();
         * autoCompleteTextView.setSelection(0, stop); return false; } });
         */

        return autoCompleteTextView;
    }

    /**
     * On date set listener for the date picker. Sets the new date to the time
     * member and updates views if the date was changed.
     */
    private DatePickerDialog.OnDateSetListener mDateSetListener = new DatePickerDialog.OnDateSetListener() {
        @Override
        public void onDateSet(DatePicker view, int year, int monthOfYear,
                int dayOfMonth) {
            mTime.year = year;
            mTime.month = monthOfYear;
            mTime.monthDay = dayOfMonth;
            onTimeChanged();
        }
    };

    /**
     * On time set listener for the time picker. Sets the new time to the time
     * member and updates views if the time was changed.
     */
    private TimePickerDialog.OnTimeSetListener mTimeSetListener = new TimePickerDialog.OnTimeSetListener() {
        @Override
        public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
            mTime.hour = hourOfDay;
            mTime.minute = minute;
            onTimeChanged();
        }
    };
    private int mStackLevel;

    /**
     * Start the search.
     * 
     * @param startPoint
     *            the start point
     * @param endPoint
     *            the end point
     * @param time
     *            the departure time or null to use current time
     */
    private void onSearchRoutes(Site startPoint, Site endPoint, Site viaPoint,
            Time time) {
        // TODO: We should not handle point-on-map this way. But for now we just
        // want it to work.
        if (!mStartPointAutoComplete.getText().toString().equals(getString(R.string.point_on_map))) {
            mHistoryDbAdapter.create(HistoryDbAdapter.TYPE_JOURNEY_PLANNER_SITE, startPoint);
        }
        if (!mEndPointAutoComplete.getText().toString().equals(getString(R.string.point_on_map))) {
            mHistoryDbAdapter.create(HistoryDbAdapter.TYPE_JOURNEY_PLANNER_SITE, endPoint);
        }
        if (!TextUtils.isEmpty(mViaPointAutoComplete.getText().toString())) {
            mHistoryDbAdapter.create(HistoryDbAdapter.TYPE_JOURNEY_PLANNER_SITE, endPoint);
        }

        boolean alternativeStops = false;
        boolean isTimeDeparture = true;
        RadioGroup chooseTimeGroup = (RadioGroup) mSearchView
                .findViewById(R.id.planner_choose_time_group);
        int checkedId = chooseTimeGroup.getCheckedRadioButtonId();
        if (checkedId == R.id.planner_check_more_choices) {
            isTimeDeparture = mWhenSpinner.getSelectedItemId() == 0 ? true
                    : false;

            CheckBox alternativeCheckBox = (CheckBox) getActivity()
                    .findViewById(R.id.planner_alternative_stops);
            alternativeStops = alternativeCheckBox.isChecked();
        } else {
            // User has checked the "now" checkbox, this forces the time to
            // be set in the RoutesActivity upon search.
            time = null;
        }

        Log.i(TAG, "START POINT: " + startPoint.toString());
        Log.i(TAG, "END POINT: " + endPoint.toString());

        JourneyQuery journeyQuery = new JourneyQuery.Builder()
                .origin(startPoint)
                .destination(endPoint)
                .via(viaPoint)
                .isTimeDeparture(isTimeDeparture)
                .time(time)
                .transportModes(getSelectedTransportModes())
                .alternativeStops(alternativeStops)
                .create();

        Intent routesIntent = new Intent(getActivity(), RoutesActivity.class);
        routesIntent.putExtra(RoutesActivity.EXTRA_JOURNEY_QUERY, journeyQuery);
        startActivity(routesIntent);

        /*
         * Uri routesUri = RoutesActivity.createRoutesUri( startPoint, endPoint,
         * time, isTimeDeparture); Intent i = new Intent(Intent.ACTION_VIEW,
         * routesUri, this, RoutesActivity.class); startActivity(i);
         */
    }

    private ArrayList<String> getSelectedTransportModes() {
        CheckBox transportBus = (CheckBox) mSearchView
                .findViewById(R.id.planner_transport_bus);
        CheckBox transportMetro = (CheckBox) mSearchView
                .findViewById(R.id.planner_transport_metro);
        CheckBox transportNar = (CheckBox) mSearchView
                .findViewById(R.id.planner_transport_nar);
        CheckBox transportTrain = (CheckBox) mSearchView
                .findViewById(R.id.planner_transport_train);
        CheckBox transportTram = (CheckBox) mSearchView
                .findViewById(R.id.planner_transport_tram);
        CheckBox transportWax = (CheckBox) mSearchView
                .findViewById(R.id.planner_transport_wax);

        ArrayList<String> transportModes = new ArrayList<String>();
        if (transportBus.isChecked()) {
            transportModes.add(TransportMode.BUS);
        }
        if (transportMetro.isChecked()) {
            transportModes.add(TransportMode.METRO);
        }
        if (transportNar.isChecked()) {
            transportModes.add(TransportMode.NAR);
        }
        if (transportTrain.isChecked()) {
            transportModes.add(TransportMode.TRAIN);
        }
        if (transportTram.isChecked()) {
            transportModes.add(TransportMode.TRAM);
        }
        if (transportWax.isChecked()) {
            transportModes.add(TransportMode.WAX);
        }

        return transportModes;
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

        /*
         * FragmentTransaction ft = getActivity().getSupportFragmentManager()
         * .beginTransaction(); Fragment prev =
         * getActivity().getSupportFragmentManager()
         * .findFragmentByTag("dialog"); if (prev != null) { ft.remove(prev); }
         * ft.addToBackStack(null);
         * 
         * // Create and show the dialog. DialogFragment newFragment =
         * PlannerDialogFragment.newInstance(dialog); newFragment.show(ft,
         * "dialog");
         */
        // TODO: This resolves an issue that raises a IllegalStateException on
        // ICS, Investigate if the above is really needed.
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        DialogFragment newFragment = PlannerDialogFragment.newInstance(dialog);
        ft.add(newFragment, null);
        ft.commit();
    }

    public static class PlannerDialogFragment extends DialogFragment {

        private static Dialog mDialog;

        static PlannerDialogFragment newInstance(Dialog dialog) {
            mDialog = dialog;
            PlannerDialogFragment f = new PlannerDialogFragment();
            return f;
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            return mDialog;
        }
    }

    private Dialog createDialogReinstallApp() {
        return new AlertDialog.Builder(getActivity())
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle(getText(R.string.attention_label))
                .setMessage(getText(R.string.reinstall_app_message))
                .setPositiveButton(android.R.string.ok, null)
                .create();
    }

    private Dialog createDialogShortcutName() {
        final View chooseShortcutName = getActivity().getLayoutInflater()
                .inflate(R.layout.create_shortcut_name, null);
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

    private Dialog createDialogTime() {
        // TODO: Base 24 hour on locale, same with the format.
        return new TimePickerDialog(getActivity(), mTimeSetListener,
                mTime.hour, mTime.minute, true);
    }

    private Dialog createDialogDate() {
        return new DatePickerDialog(getActivity(), mDateSetListener,
                mTime.year, mTime.month, mTime.monthDay);
    }

    private Dialog createDialogNoLocation() {
        return new AlertDialog.Builder(getActivity())
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle(getText(R.string.no_location_title))
                .setMessage(getText(R.string.no_location_message))
                .setPositiveButton(android.R.string.ok, null).create();
    }

    private Dialog createDialogAbout() {
        String version = MyApplication.APP_VERSION;

        View aboutLayout = getActivity().getLayoutInflater().inflate(
                R.layout.about_dialog,
                (ViewGroup) getActivity().findViewById(
                        R.id.about_dialog_layout_root));

        return new AlertDialog.Builder(getActivity())
                .setTitle(getText(R.string.app_name) + " " + version)
                .setIcon(android.R.drawable.ic_dialog_info)
                .setView(aboutLayout)
                .setCancelable(true)
                .setInverseBackgroundForced(true)
                .setPositiveButton(getText(android.R.string.ok), null)
                .setNeutralButton(getText(R.string.donate),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog,
                                    int which) {
                                Intent browserIntent = new Intent(
                                        Intent.ACTION_VIEW,
                                        Uri.parse(getString(R.string.donate_url)));
                                startActivity(browserIntent);
                            }
                        })
                .setNegativeButton(getText(R.string.feedback),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog,
                                    int which) {
                                final Intent emailIntent = new Intent(
                                    Intent.ACTION_SEND);
                                emailIntent.setType("plain/text");
                                emailIntent.putExtra(
                                    android.content.Intent.EXTRA_EMAIL,
                                    new String[] { getString(R.string.send_feedback_email_emailaddress) });
                                emailIntent.putExtra(
                                    android.content.Intent.EXTRA_SUBJECT,
                                    getText(R.string.send_feedback_email_title));
                                startActivity(Intent.createChooser(emailIntent,
                                        getText(R.string.send_email)));
                            }
                        }).create();
    }

    private Dialog createDialogViaPoint() {
        AlertDialog.Builder viaPointDialogBuilder = new AlertDialog.Builder(
                getActivity());
        viaPointDialogBuilder.setTitle(getText(R.string.via));
        final Cursor historyViaCursor = mHistoryDbAdapter.fetchLatest();
        getActivity().startManagingCursor(historyViaCursor);
        final SelectPointAdapter viaPointAdapter = new SelectPointAdapter(
                getActivity(), historyViaCursor, true, true);
        getActivity().stopManagingCursor(historyViaCursor);
        viaPointDialogBuilder.setAdapter(viaPointAdapter,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                        default:
                            Site viaPoint = (Site) viaPointAdapter.getItem(which);
                            mViaPoint = viaPoint;
                            mViaPointAutoComplete.setText(mViaPoint.getName());
                            mViaPointAutoComplete.clearFocus();
                        }
                    }
                });
        return viaPointDialogBuilder.create();
    }

    private Dialog createDialogEndPoint() {
        AlertDialog.Builder endPointDialogBuilder = new AlertDialog.Builder(
                getActivity());
        endPointDialogBuilder
                .setTitle(getText(R.string.choose_end_point_label));
        final Cursor historyDestinationCursor = mHistoryDbAdapter.fetchLatest();
        getActivity().startManagingCursor(historyDestinationCursor);
        final SelectPointAdapter endPointAdapter = new SelectPointAdapter(
                getActivity(), historyDestinationCursor, false, false);
        getActivity().stopManagingCursor(historyDestinationCursor);
        endPointDialogBuilder.setAdapter(endPointAdapter,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                        case 0:
                            mEndPoint.setName(Site.TYPE_MY_LOCATION);
                            mEndPointAutoComplete.setText(getText(R.string.my_location));
                            mEndPointAutoComplete.clearFocus();
                            break;
                        case 1:
                            Intent i = new Intent(getActivity(), PointOnMapActivity.class);
                            i.putExtra(PointOnMapActivity.EXTRA_STOP, mEndPoint);
                            i.putExtra(PointOnMapActivity.EXTRA_HELP_TEXT,
                                   getString(R.string.tap_your_end_point_on_map));
                            startActivityForResult(i,
                                    REQUEST_CODE_POINT_ON_MAP_END);
                            break;
                        default:
                            Site endPoint = (Site) endPointAdapter.getItem(which);
                            mEndPoint = new Site(endPoint);
                            mEndPointAutoComplete.setText(mEndPoint.getName());
                            mEndPointAutoComplete.clearFocus();
                        }
                    }
                });
        return endPointDialogBuilder.create();
    }

    private Dialog createDialogStartPoint() {
        AlertDialog.Builder startPointDialogBuilder = new AlertDialog.Builder(
                getActivity());
        startPointDialogBuilder
                .setTitle(getText(R.string.choose_start_point_label));

        final Cursor historyOriginCursor = mHistoryDbAdapter.fetchLatest();
        getActivity().startManagingCursor(historyOriginCursor);
        final SelectPointAdapter startPointAdapter = new SelectPointAdapter(
                getActivity(), historyOriginCursor, false, false);
        getActivity().stopManagingCursor(historyOriginCursor);
        startPointDialogBuilder.setAdapter(startPointAdapter,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                        case 0:
                            mStartPoint.setName(Site.TYPE_MY_LOCATION);
                            mStartPointAutoComplete.setText(getText(R.string.my_location));
                            mStartPointAutoComplete.clearFocus();
                            break;
                        case 1:
                            Intent i = new Intent(getActivity(), PointOnMapActivity.class);
                            i.putExtra(PointOnMapActivity.EXTRA_STOP, mStartPoint);
                            i.putExtra(PointOnMapActivity.EXTRA_HELP_TEXT,
                                    getString(R.string.tap_your_start_point_on_map));
                            startActivityForResult(i,
                                    REQUEST_CODE_POINT_ON_MAP_START);
                            break;
                        default:
                            Site startPoint = (Site) startPointAdapter.getItem(which);
                            mStartPoint = new Site(startPoint);
                            mStartPointAutoComplete.setText(mStartPoint.getName());
                            mStartPointAutoComplete.clearFocus();
                        }
                    }
                });
        return startPointDialogBuilder.create();
    }

    /**
     * Update time on the buttons.
     */
    private void onTimeChanged() {
        String formattedDate = mTime.format("%x");
        String formattedTime = mTime.format("%R");
        mDateButton.setText(formattedDate);
        mTimeButton.setText(formattedTime);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.actionbar_search_route:
            if (TextUtils.isEmpty(mStartPointAutoComplete.getText())) {
                Log.d(TAG, "Start auto was empty");
                mStartPointAutoComplete.setError(getText(R.string.empty_value));
            } else if (TextUtils.isEmpty(mEndPointAutoComplete.getText())) {
                Log.d(TAG, "End auto was empty");
                mEndPointAutoComplete.setError(getText(R.string.empty_value));
            } else {
                mStartPoint = buildStop(mStartPoint, mStartPointAutoComplete);
                mEndPoint = buildStop(mEndPoint, mEndPointAutoComplete);

                boolean looksValid = true;
                if (!mStartPoint.looksValid()) {
                    Log.d(TAG, "Start was not valid: " + mStartPoint.toDump());
                    mStartPointAutoComplete.setError(getText(R.string.empty_value));
                    looksValid = false;
                }
                if (!mEndPoint.looksValid()) {
                    Log.d(TAG, "End was not valid: " + mEndPoint.toDump());
                    mEndPointAutoComplete.setError(getText(R.string.empty_value));
                    looksValid = false;
                }
                if (!TextUtils.isEmpty(mViaPointAutoComplete.getText())) {
                    mViaPoint = buildStop(mViaPoint, mViaPointAutoComplete);
                    if (!mViaPoint.looksValid()) {
                        Log.d(TAG, "Via was not valid");
                        mViaPointAutoComplete.setError(getText(R.string.empty_value));
                        looksValid = false;
                    }
                }
                if (looksValid) {
                    if (mCreateShortcut) {
                        showDialog(createDialogShortcutName());
                        // onCreateShortCut(mStartPoint, mEndPoint);
                    } else {
                        onSearchRoutes(mStartPoint, mEndPoint, mViaPoint, mTime);
                    }
                }
            }
            break;
        case R.id.about:
            showDialog(createDialogAbout());
            return true;
        case R.id.actionbar_item_reverse:

            Log.i(TAG, "Before reversed start " + mStartPoint.toDump());
            Log.i(TAG, "Before reversed end " + mEndPoint.toDump());

            Site tmpStartPoint = new Site(mEndPoint);
            Site tmpEndPoint = new Site(mStartPoint);

            mStartPoint = tmpStartPoint;
            mEndPoint = tmpEndPoint;

            Log.i(TAG, "Reversed start " + mStartPoint.toDump());
            Log.i(TAG, "Reversed end " + mEndPoint.toDump());

            // Seems like we loose the reference during reverse.
            // Investigate this further.
            mStartPointAutoComplete = createAutoCompleteTextView(R.id.from,
                    R.id.from_progress, mStartPoint);
            mEndPointAutoComplete = createAutoCompleteTextView(R.id.to,
                    R.id.to_progress, mEndPoint);

            mStartPointAutoComplete.setText(mStartPoint.getName());
            mEndPointAutoComplete.setText(mEndPoint.getName());

            return true;
        case R.id.menu_settings:
            Intent settingsIntent = new Intent().setClass(getActivity(),
                    SettingsActivity.class);
            startActivity(settingsIntent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
        case REQUEST_CODE_POINT_ON_MAP_START:
            if (resultCode == Activity.RESULT_CANCELED) {
                Log.d(TAG, "action canceled");
            } else {
                mStartPoint = data
                        .getParcelableExtra(PointOnMapActivity.EXTRA_STOP);
                mStartPointAutoComplete.setText(getText(R.string.point_on_map));
                // mStartPointAutoComplete.setText(mStartPoint.getName());
                Log.d(TAG, "Got Stop " + mStartPoint);
            }
            break;
        case REQUEST_CODE_POINT_ON_MAP_END:
            if (resultCode == Activity.RESULT_CANCELED) {
                Log.d(TAG, "action canceled");
            } else {
                mEndPoint = data
                        .getParcelableExtra(PointOnMapActivity.EXTRA_STOP);
                mEndPointAutoComplete.setText(getText(R.string.point_on_map));
                // mEndPointAutoComplete.setText(mEndPoint.getName());
                Log.d(TAG, "Got Stop " + mEndPoint);
            }
            break;
        }
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "PlannerFragment.onDestroy()");
        super.onDestroy();
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        // We only check the state for later radio button.
        if (isChecked && buttonView.getId() == R.id.planner_check_more_choices) {
            // Set time to now, and notify buttons about the new time.
            if (mTime == null) {
                mTime = new Time();
                mTime.setToNow();
                onTimeChanged();
            }

            mChangeTimeLayout.setVisibility(View.VISIBLE);
        } else if (!isChecked
                && buttonView.getId() == R.id.planner_check_more_choices) {
            mViaPoint = new Site();
            mViaPointAutoComplete.setText("");
            mChangeTimeLayout.setVisibility(View.GONE);
        }
    }

    private class UpdateStopTextWatcher implements TextWatcher {
        private final Site mSite;

        public UpdateStopTextWatcher(Site site) {
            mSite = site;
        }

        @Override
        public void afterTextChanged(Editable s) {
            if (!getString(R.string.my_location).equals(s.toString())
                    || getString(R.string.point_on_map).equals(s.toString())) {
                if (!s.toString().equals(mSite.getName())) {
                    mSite.setName(s.toString());
                    mSite.setLocation(null);
                }
            }
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count,
                int after) {
            // Needed by interface, but not used.
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before,
                int count) {
            // Needed by interface, but not used.
        }

    }

    private class ReservedNameTextWatcher implements TextWatcher {
        private final CharSequence mReservedName;
        private final AutoCompleteTextView mViewToWatch;
        private boolean mRemoveText = false;
        private String mNewText;

        public ReservedNameTextWatcher(CharSequence reservedName,
                AutoCompleteTextView viewToWatch) {
            mReservedName = reservedName;
            mViewToWatch = viewToWatch;
        }

        @Override
        public void afterTextChanged(Editable s) {
            if (mRemoveText) {
                mRemoveText = false;
                if (!mViewToWatch.getText().toString().equals(mNewText)) {
                    mViewToWatch.setText(mNewText);
                }
            }
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count,
                int after) {
            if (mRemoveText == false
                    && s.toString().equals(mReservedName.toString())) {
                mRemoveText = true;
                mViewToWatch.setTextColor(Color.BLACK);
            }
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before,
                int count) {
            mNewText = s.toString().substring(start, start + count);

            if (s.toString().equals(mReservedName.toString())) {
                mViewToWatch.setTextColor(0xFF4F94CD);
            }
        }
    }

    private class SelectPointAdapter extends MultipleListAdapter {

        private SectionedAdapter mHistoryWrapperAdapter = new SectionedAdapter() {
            @Override
            protected View getHeaderView(Section section, int index,
                    View convertView, ViewGroup parent) {
                TextView result = (TextView) convertView;
                if (convertView == null)
                    result = (TextView) getActivity().getLayoutInflater()
                            .inflate(R.layout.header, null);
                result.setText(section.caption);
                return (result);
            }
        };

        public SelectPointAdapter(Context context, Cursor historyCursor,
                boolean onlyHistory, boolean isVia) {
            if (!onlyHistory) {
                ArrayList<HashMap<String, String>> items = new ArrayList<HashMap<String, String>>();

                HashMap<String, String> myLocationItem = new HashMap<String, String>();
                myLocationItem.put("item", getString(R.string.my_location));
                items.add(myLocationItem);

                HashMap<String, String> pointOnMapItem = new HashMap<String, String>();
                pointOnMapItem.put("item", getString(R.string.point_on_map));
                items.add(pointOnMapItem);

                SimpleAdapter itemsAdapter = new SimpleAdapter(context, items,
                        android.R.layout.simple_list_item_1,
                        new String[] { "item" },
                        new int[] { android.R.id.text1 });

                addAdapter(0, itemsAdapter);
            }

            ArrayList<Site> historyList = new ArrayList<Site>();
            historyCursor.moveToFirst();
            for (int i = 0; i < historyCursor.getCount(); i++) {
                Site site = new Site();

                site.setName(historyCursor.getString(HistoryDbAdapter.INDEX_NAME));
                site.setLocation(
                        historyCursor.getInt(HistoryDbAdapter.INDEX_LATITUDE),
                        historyCursor.getInt(HistoryDbAdapter.INDEX_LONGITUDE));
                site.setId(historyCursor.getInt(HistoryDbAdapter.INDEX_SITE_ID));

                if (isVia) {
                    if (!site.hasLocation()) {
                        historyList.add(site);
                    }
                } else {
                    historyList.add(site);
                }
                historyCursor.moveToNext();
            }
            historyCursor.close();
            ArrayAdapter<Site> historyAdapter =
                    new ArrayAdapter<Site>(context,
                            android.R.layout.simple_list_item_1, historyList);

            mHistoryWrapperAdapter.addSection(0,
                    getString(R.string.history_label), historyAdapter);
            addAdapter(1, mHistoryWrapperAdapter);
        }
    }

    private class JourneyAdapter extends CursorAdapter {

        public JourneyAdapter(Context context, Cursor c) {
            super(context, c);
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
                v = inflater.inflate(R.layout.journey_history_row, parent,
                        false);
                inflateView(v, journeyQuery, cursor);
            } else {
                v = new View(getActivity());
            }

            return v;
        }

        private View inflateView(View v, JourneyQuery journeyQuery, Cursor c) {
            TextView originText = (TextView) v
                    .findViewById(R.id.favorite_start_point);
            if (Location.TYPE_MY_LOCATION.equals(journeyQuery.origin.name)) {
                originText.setText(getString(R.string.my_location));
            } else {
                originText.setText(journeyQuery.origin.name);
            }

            TextView destinationText = (TextView) v
                    .findViewById(R.id.favorite_end_point);
            if (Location.TYPE_MY_LOCATION.equals(journeyQuery.destination.name)) {
                destinationText.setText(getString(R.string.my_location));
            } else {
                destinationText.setText(journeyQuery.destination.name);
            }

            View viaView = v.findViewById(R.id.via_row);
            if (journeyQuery.hasVia()) {
                TextView viaText = (TextView) v
                        .findViewById(R.id.favorite_via_point);
                viaText.setText(journeyQuery.via.name);
                viaView.setVisibility(View.VISIBLE);
            } else {
                viaView.setVisibility(View.GONE);
            }

            addTransportModeViews(journeyQuery, v);

            CheckBox starred = (CheckBox) v
                    .findViewById(R.id.journey_star_check);
            boolean isStarred = c.getInt(COLUMN_INDEX_STARRED) == 1 ? true
                    : false;
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
                    Uri uri = ContentUris.withAppendedId(Journeys.CONTENT_URI,
                            id);
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
            // this looks crazy! But we need to reset all transport views so
            // they don't get recycled and then enable them again if they're
            // selected for the journey.
            ImageView metroView = (ImageView) v
                    .findViewById(R.id.favorite_transport_mode_metro);
            metroView.setVisibility(View.GONE);
            ImageView busView = (ImageView) v
                    .findViewById(R.id.favorite_transport_mode_bus);
            busView.setVisibility(View.GONE);
            ImageView trainView = (ImageView) v
                    .findViewById(R.id.favorite_transport_mode_train);
            trainView.setVisibility(View.GONE);
            ImageView tramView = (ImageView) v
                    .findViewById(R.id.favorite_transport_mode_tram);
            tramView.setVisibility(View.GONE);
            ImageView waxView = (ImageView) v
                    .findViewById(R.id.favorite_transport_mode_wax);
            waxView.setVisibility(View.GONE);
            ImageView narView = (ImageView) v
                    .findViewById(R.id.favorite_transport_mode_nar);
            narView.setVisibility(View.GONE);

            if (journeyQuery.transportModes != null) {
                for (String transportMode : journeyQuery.transportModes) {
                    if (transportMode.equals(TransportMode.METRO)) {
                        metroView.setVisibility(View.VISIBLE);
                    } else if (transportMode.equals(TransportMode.BUS)) {
                        busView.setVisibility(View.VISIBLE);
                    } else if (transportMode.equals(TransportMode.TRAIN)) {
                        trainView.setVisibility(View.VISIBLE);
                    } else if (transportMode.equals(TransportMode.TRAM)) {
                        tramView.setVisibility(View.VISIBLE);
                    } else if (transportMode.equals(TransportMode.WAX)) {
                        waxView.setVisibility(View.VISIBLE);
                    } else if (transportMode.equals(TransportMode.NAR)) {
                        narView.setVisibility(View.VISIBLE);
                    }
                }
            } else {
                metroView.setVisibility(View.VISIBLE);
                busView.setVisibility(View.VISIBLE);
                trainView.setVisibility(View.VISIBLE);
                tramView.setVisibility(View.VISIBLE);
                waxView.setVisibility(View.VISIBLE);
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
            Log.e(TAG, "Failed to covert to journey from json.");
        }
        return journeyQuery;
    }

}

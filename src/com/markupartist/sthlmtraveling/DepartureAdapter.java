package com.markupartist.sthlmtraveling;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import android.content.Context;
import android.text.TextUtils;
import android.text.format.Time;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.SimpleAdapter.ViewBinder;

import com.markupartist.sthlmtraveling.provider.TransportMode;
import com.markupartist.sthlmtraveling.provider.departure.DeparturesStore.BusDeparture;
import com.markupartist.sthlmtraveling.provider.departure.DeparturesStore.Departures;
import com.markupartist.sthlmtraveling.provider.departure.DeparturesStore.DisplayRow;
import com.markupartist.sthlmtraveling.provider.departure.DeparturesStore.GroupOfLine;
import com.markupartist.sthlmtraveling.provider.departure.DeparturesStore.MetroDeparture;
import com.markupartist.sthlmtraveling.provider.departure.DeparturesStore.TrainDeparture;
import com.markupartist.sthlmtraveling.provider.departure.DeparturesStore.TramDeparture;

public class DepartureAdapter extends SectionedAdapter {

    private Context mContext;

    public DepartureAdapter(Context context) {
        mContext = context;
    }

    @Override
    protected View getHeaderView(Section section, int index, View convertView,
            ViewGroup parent) {
        TextView result = (TextView) convertView;

        if (convertView == null) {
            LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            result = (TextView) inflater.inflate(R.layout.header, null);
        }

        result.setText(section.caption);
        return (result);
    }

    public void fillDepartures(Departures departures, int transportType) {
        //TextView emptyResultView = (TextView) findViewById(R.id.departures_empty_result);
        //emptyResultView.setVisibility(View.GONE);

        this.clear();
        
        String platformString = mContext.getString(R.string.platform);

        switch (transportType) {
        case TransportMode.METRO_INDEX:
            if (departures.metros.isEmpty()) {
                //emptyResultView.setVisibility(View.VISIBLE);
            } else {
                for (MetroDeparture metroDeparture: departures.metros) {
                    for (GroupOfLine gol : metroDeparture.groupOfLines) {
                        this.addSection(0, gol.name + ", " + platformString +" 1", createAdapter(gol.direction1));
                        this.addSection(0, gol.name + ", " + platformString +" 2", createAdapter(gol.direction2));
                    }
                }
            }
            break;
        case TransportMode.BUS_INDEX:
            if (departures.buses.isEmpty()) {
                //emptyResultView.setVisibility(View.VISIBLE);
            } else {
                for (BusDeparture busDeparture : departures.buses) {
                    this.addSection(0, busDeparture.stopAreaName, createAdapter(busDeparture.departures));
                }
            }
            break;
        case TransportMode.TRAIN_INDEX:
            if (departures.trains.isEmpty()) {
                //emptyResultView.setVisibility(View.VISIBLE);
            } else {
                for (TrainDeparture trainDeparture : departures.trains) {
                    this.addSection(0, trainDeparture.stopAreaName + ", " + platformString +" 1", createAdapter(trainDeparture.direction1));
                    this.addSection(0, trainDeparture.stopAreaName + ", " + platformString +" 2", createAdapter(trainDeparture.direction2));
                }
            }
            break;
        case TransportMode.LOKALBANA_INDEX:
            if (departures.trams.isEmpty()) {
                //emptyResultView.setVisibility(View.VISIBLE);
            } else {
                for (TramDeparture tramDeparture : departures.trams) {
                    this.addSection(0, tramDeparture.stopAreaName + ", " + platformString +" 1", createAdapter(tramDeparture.direction1));
                    this.addSection(0, tramDeparture.stopAreaName + ", " + platformString +" 2", createAdapter(tramDeparture.direction2));
                }
            }
            break;
        }

        this.notifyDataSetChanged();
    }

    private SimpleAdapter createAdapter(ArrayList<DisplayRow> displayRows) {
        ArrayList<Map<String, String>> list = new ArrayList<Map<String, String>>();

        Time now = new Time();
        now.setToNow();

        if (displayRows.size() == 0) {
            Map<String, String> map = new HashMap<String, String>();
            map.put("destination", mContext.getString(R.string.no_departures));
            list.add(map);
        }

        for (DisplayRow displayRow : displayRows) {
            Map<String, String> map = new HashMap<String, String>();
            map.put("line", displayRow.lineNumber);

            String destination = displayRow.destination;
            if (!TextUtils.isEmpty(displayRow.message) &&
                    TextUtils.isEmpty(destination)) {
                destination = displayRow.message;
            }
            map.put("destination", destination);

            //map.put("timeToDisplay", humanTimeUntil(now, displayRow.getExpectedDateTime()));

            String displayTime = displayRow.displayTime;
            /*
            if (!TextUtils.isEmpty(displayRow.expectedDateTime)) {
                // Naive check to see if it's a time. If it's a time we choose
                // that instead cause that means that the time is not based on
                // real-time data.
                if (!displayTime.contains(":")) {
                    Time expectedDateTime = new Time();
                    expectedDateTime.parse3339(displayRow.expectedDateTime);
                    displayTime = humanTimeUntil(now, expectedDateTime);
                }
            }
            */
            
            if (TextUtils.isEmpty(displayTime)
                    && TextUtils.isEmpty(displayRow.message)
                    || TextUtils.equals(displayTime, "0 min")) {
                displayTime = mContext.getString(R.string.now);
            }
            
            map.put("timeToDisplay", displayTime);

            //map.put("groupOfLine", displayRow.getGroupOfLine());
            list.add(map);
        }

        SimpleAdapter adapter = new SimpleAdapter(mContext, list, 
                R.layout.departures_row,
                new String[] {"line", "destination", "timeToDisplay"},
                new int[] { 
                    R.id.departure_line,
                    R.id.departure_destination,
                    R.id.departure_timeToDisplay,
                }
        );

        adapter.setViewBinder(new ViewBinder() {
            @Override
            public boolean setViewValue(View view, Object data,
                    String textRepresentation) {
                switch (view.getId()) {
                case R.id.departure_line:
                case R.id.departure_destination:
                    ((TextView)view).setText(textRepresentation);
                    return true;
                case R.id.departure_timeToDisplay:
                    TextView textView = (TextView) view;
                    textView.setText(textRepresentation);
                    // TODO: Setting the color like this does not work because
                    // the views get recycled.
                    /*
                    if (textRepresentation.equals(mContext.getString(R.string.now))) {
                        textView.setTextColor(0xFFEE4000);
                    }
                    */
                    return true;
                }
                return false;
            }
        });

        return adapter;
    }

    private String humanTimeUntil(Time start, Time end) {
        long distanceInMillis = Math.round(end.toMillis(true) - start.toMillis(true));
        long distanceInSeconds = Math.round(distanceInMillis / 1000);
        long distanceInMinutes = Math.round(distanceInSeconds / 60);

        if (distanceInMinutes <= 0.0) {
            return mContext.getString(R.string.now);
        }
        return String.format("%s min", distanceInMinutes);
    }
}

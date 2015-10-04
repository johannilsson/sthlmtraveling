package com.markupartist.sthlmtraveling;

import android.content.Context;
import android.text.TextUtils;
import android.text.format.Time;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.markupartist.sthlmtraveling.provider.TransportMode;
import com.markupartist.sthlmtraveling.provider.departure.DeparturesStore.BusDeparture;
import com.markupartist.sthlmtraveling.provider.departure.DeparturesStore.Departures;
import com.markupartist.sthlmtraveling.provider.departure.DeparturesStore.DisplayRow;
import com.markupartist.sthlmtraveling.provider.departure.DeparturesStore.GroupOfLine;
import com.markupartist.sthlmtraveling.provider.departure.DeparturesStore.MetroDeparture;
import com.markupartist.sthlmtraveling.provider.departure.DeparturesStore.TrainDeparture;
import com.markupartist.sthlmtraveling.provider.departure.DeparturesStore.TramDeparture;

import java.util.List;

public class DepartureAdapter extends SectionedAdapter {

    private Context mContext;

    public DepartureAdapter(Context context) {
        mContext = context;
    }

    @Override
    protected View getHeaderView(Section section, int index, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(mContext).inflate(R.layout.row_section, parent, false);
        }
        ((TextView) convertView.findViewById(R.id.text1)).setText(section.caption);
        return convertView;
    }

    public void fillDepartures(Departures departures, int transportType) {
        //TextView emptyResultView = (TextView) findViewById(R.id.departures_empty_result);
        //emptyResultView.setVisibility(View.GONE);

        this.clear();
        
        String directionString = mContext.getString(R.string.direction);

        switch (transportType) {
        case TransportMode.METRO_INDEX:
            if (departures.metros.isEmpty()) {
                //emptyResultView.setVisibility(View.VISIBLE);
            } else {
                for (MetroDeparture metroDeparture: departures.metros) {
                    for (GroupOfLine gol : metroDeparture.groupOfLines) {
                        this.addSection(0, gol.name + ", " + directionString +" 1", createAdapter(gol.direction1));
                        this.addSection(0, gol.name + ", " + directionString +" 2", createAdapter(gol.direction2));
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
                    this.addSection(0, trainDeparture.stopAreaName + ", " + directionString +" 1", createAdapter(trainDeparture.direction1));
                    this.addSection(0, trainDeparture.stopAreaName + ", " + directionString +" 2", createAdapter(trainDeparture.direction2));
                }
            }
            break;
        case TransportMode.LOKALBANA_INDEX:
            if (departures.trams.isEmpty()) {
                //emptyResultView.setVisibility(View.VISIBLE);
            } else {
                for (TramDeparture tramDeparture : departures.trams) {
                    this.addSection(0, tramDeparture.stopAreaName + ", " + directionString +" 1", createAdapter(tramDeparture.direction1));
                    this.addSection(0, tramDeparture.stopAreaName + ", " + directionString +" 2", createAdapter(tramDeparture.direction2));
                }
            }
            break;
        }

        this.notifyDataSetChanged();
    }

    public static class DisplayRowAdapter extends ArrayAdapter<DisplayRow> {

        public DisplayRowAdapter(Context context, List<DisplayRow> displayRows) {
            super(context, R.layout.departures_row, displayRows);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            DisplayRow displayRow = getItem(position);

            convertView = LayoutInflater.from(getContext())
                    .inflate(R.layout.departures_row, parent, false);

            TextView lineView = (TextView) convertView.findViewById(R.id.departure_line);
            TextView destinationView = (TextView) convertView.findViewById(R.id.departure_destination);
            TextView timeToDisplayView = (TextView) convertView.findViewById(R.id.departure_timeToDisplay);

            lineView.setText(displayRow.lineNumber);

            String destination = displayRow.destination;
            if (!TextUtils.isEmpty(displayRow.message) &&
                    TextUtils.isEmpty(destination)) {
                destination = displayRow.message;
            }
            destinationView.setText(destination);

            String displayTime = displayRow.displayTime;
            if (TextUtils.isEmpty(displayTime)
                    && TextUtils.isEmpty(displayRow.message)
                    || TextUtils.equals(displayTime, "0 min")) {
                displayTime = getContext().getString(R.string.now);
            }
            timeToDisplayView.setText(displayTime);

            return convertView;
        }
    }

    private DisplayRowAdapter createAdapter(final List<DisplayRow> displayRows) {
        return new DisplayRowAdapter(mContext, displayRows);
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

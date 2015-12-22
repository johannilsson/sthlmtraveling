package com.markupartist.sthlmtraveling;

import android.content.Context;
import android.graphics.Color;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
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
import com.markupartist.sthlmtraveling.utils.DateTimeUtil;
import com.markupartist.sthlmtraveling.utils.ViewHelper;
import com.markupartist.sthlmtraveling.utils.text.RoundedBackgroundSpan;

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
                        if (gol.direction1.size() > 0) {
                            this.addSection(0, directionString + " 1",
                                    createAdapter(gol.direction1, transportType));
                        }
                        if (gol.direction2.size() > 0) {
                            this.addSection(0, directionString + " 2",
                                    createAdapter(gol.direction2, transportType));
                        }
                    }
                }
            }
            break;
        case TransportMode.BUS_INDEX:
            if (departures.buses.isEmpty()) {
                //emptyResultView.setVisibility(View.VISIBLE);
            } else {
                for (BusDeparture busDeparture : departures.buses) {
                    this.addSection(0, busDeparture.stopAreaName, createAdapter(busDeparture.departures, transportType));
                }
            }
            break;
        case TransportMode.TRAIN_INDEX:
            if (departures.trains.isEmpty()) {
                //emptyResultView.setVisibility(View.VISIBLE);
            } else {
                for (TrainDeparture trainDeparture : departures.trains) {
                    if (trainDeparture.direction1.size() > 0) {
                        this.addSection(0,
                                trainDeparture.stopAreaName + ", " + directionString + " 1",
                                createAdapter(trainDeparture.direction1, transportType));
                    }
                    if (trainDeparture.direction2.size() > 0) {
                        this.addSection(0,
                                trainDeparture.stopAreaName + ", " + directionString + " 2",
                                createAdapter(trainDeparture.direction2, transportType));
                    }
                }
            }
            break;
        case TransportMode.TRAM_INDEX:
            if (departures.trams.isEmpty()) {
                //emptyResultView.setVisibility(View.VISIBLE);
            } else {
                for (TramDeparture tramDeparture : departures.trams) {
                    if (tramDeparture.direction1.size() > 0) {
                        this.addSection(0,
                                tramDeparture.stopAreaName + ", " + directionString + " 1",
                                createAdapter(tramDeparture.direction1, transportType));
                    }
                    if (tramDeparture.direction2.size() > 0) {
                        this.addSection(0, tramDeparture.stopAreaName + ", " + directionString + " 2",
                                createAdapter(tramDeparture.direction2, transportType));
                    }
                }
            }
            break;
        }

        this.notifyDataSetChanged();
    }

    public static class DisplayRowAdapter extends ArrayAdapter<DisplayRow> {

        private final int mTransportType;

        public DisplayRowAdapter(Context context, List<DisplayRow> displayRows, int transportType) {
            super(context, R.layout.departures_row, displayRows);
            mTransportType = transportType;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            DisplayRow displayRow = getItem(position);

            ViewHolder holder;
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext())
                        .inflate(R.layout.departures_row, parent, false);
                holder = new ViewHolder(convertView);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

//            TextDrawable d = TextDrawable.builder(getContext())
//                    .buildRound(ViewHelper.getLineName(mTransportType, displayRow.lineNumber),
//                            ViewHelper.getLineColor(
//                                    getContext(),
//                                    mTransportType,
//                                    displayRow.lineNumber,
//                                    displayRow.lineName));
//            holder.lineView.setImageDrawable(d);

            String lineName = ViewHelper.getLineName(mTransportType, displayRow.lineNumber);
            if (TextUtils.isEmpty(lineName)) {
                lineName = "";
            }
            RoundedBackgroundSpan roundedBackgroundSpan = new RoundedBackgroundSpan(
                    ViewHelper.getLineColor(getContext(), mTransportType, displayRow.lineNumber, displayRow.lineName),
                    Color.WHITE,
                    ViewHelper.dipsToPix(getContext().getResources(), 4));
            SpannableStringBuilder sb = new SpannableStringBuilder();
            sb.append(lineName);
            sb.append(' ');
            sb.setSpan(roundedBackgroundSpan, 0, lineName.length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
            holder.lineView.setText(sb);

            String destination = displayRow.destination;
            if (!TextUtils.isEmpty(displayRow.message) &&
                    TextUtils.isEmpty(destination)) {
                destination = displayRow.message;
            }
            holder.destinationView.setText(destination);

            String displayTime = displayRow.displayTime;
            holder.timeToDisplayView.setText(
                    DateTimeUtil.formatDisplayTime(displayTime, getContext()));

            return convertView;
        }
    }

    private DisplayRowAdapter createAdapter(final List<DisplayRow> displayRows, int transportType) {
        return new DisplayRowAdapter(mContext, displayRows, transportType);
    }

    public static class ViewHolder {
        TextView lineView;
        TextView destinationView;
        TextView timeToDisplayView;

        public ViewHolder(View view) {
            lineView = (TextView) view.findViewById(R.id.departure_line);
            destinationView = (TextView) view.findViewById(R.id.departure_destination);
            timeToDisplayView = (TextView) view.findViewById(R.id.departure_timeToDisplay);
        }
    }
}

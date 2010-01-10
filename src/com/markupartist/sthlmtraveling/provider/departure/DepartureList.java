package com.markupartist.sthlmtraveling.provider.departure;

import java.util.ArrayList;
import java.util.Iterator;

public class DepartureList implements Iterable<Departure> {
    private ArrayList<Departure> mDepartures = new ArrayList<Departure>();
    private ArrayList<Departure> mFiltered;

    public DepartureList add(Departure departure) {
        mDepartures.add(departure);
        return this;
    }

    public int size() {
        return mFiltered != null ?
                mFiltered.size() : mDepartures.size();
    }

    public boolean isEmpty() {
        return size() == 0;
    }

    public ArrayList<Departure> filter(DepartureFilter filter) {
        mFiltered = new ArrayList<Departure>();
        for (Departure departure : mDepartures) {
            if (departure.matches(filter)) {
                mFiltered.add(departure);
            }
        }
        return mFiltered;
    }

    @Override
    public Iterator<Departure> iterator() {
        return mFiltered != null ?
                mFiltered.iterator() : mDepartures.iterator();
    }

}

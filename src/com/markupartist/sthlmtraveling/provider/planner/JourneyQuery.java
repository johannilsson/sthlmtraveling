package com.markupartist.sthlmtraveling.provider.planner;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.format.Time;

import com.markupartist.sthlmtraveling.provider.planner.Planner.Location;

public class JourneyQuery implements Parcelable {
    public Location origin;
    public Location destination;
    public Time time;
    public boolean isTimeDeparture;
    public String ident;
    public String seqnr;

    public JourneyQuery() {
    }

    public JourneyQuery(Parcel parcel) {
        origin = parcel.readParcelable(Location.class.getClassLoader());
        destination = parcel.readParcelable(Location.class.getClassLoader());
        time = new Time();
        time.parse(parcel.readString());
        isTimeDeparture = (parcel.readInt() == 1) ? true : false;
        ident = parcel.readString();
        seqnr = parcel.readString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(origin, 0);
        dest.writeParcelable(destination, 0);
        dest.writeString(time.format2445());
        dest.writeInt(isTimeDeparture ? 1 : 0);
        dest.writeString(ident);
        dest.writeString(seqnr);
    }

    public static final Creator<JourneyQuery> CREATOR = new Creator<JourneyQuery>() {
        public JourneyQuery createFromParcel(Parcel parcel) {
            return new JourneyQuery(parcel);
        }

        public JourneyQuery[] newArray(int size) {
            return new JourneyQuery[size];
        }
    };
}

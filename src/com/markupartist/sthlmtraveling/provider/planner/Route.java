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

package com.markupartist.sthlmtraveling.provider.planner;

import java.util.ArrayList;

import android.os.Parcel;
import android.os.Parcelable;

import com.markupartist.sthlmtraveling.R;

public class Route implements Parcelable {
    public String ident;
    public String routeId;
    public String from;
    public String to;
    public String departure;
    public String arrival;
    public String duration;
    public String changes;
    public ArrayList<Transport> transports;

    public Route() {
        
    }

    public Route(Parcel parcel) {
        ident = parcel.readString();
        routeId = parcel.readString();
        from = parcel.readString();
        to = parcel.readString();
        departure = parcel.readString();
        arrival = parcel.readString();
        duration = parcel.readString();
        changes = parcel.readString();

        transports = new ArrayList<Transport>();
        parcel.readTypedList(transports, Transport.CREATOR);
    }
    
    public static Route createInstance() {
        return new Route();
    }

    public void addTransport(Transport transport) {
        if (transports == null)
            transports = new ArrayList<Transport>();
        transports.add(transport);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(ident);
        dest.writeString(routeId);
        dest.writeString(from);
        dest.writeString(to);
        dest.writeString(departure);
        dest.writeString(arrival);
        dest.writeString(duration);
        dest.writeString(changes);
        dest.writeTypedList(transports);
    }

    public static final Creator<Route> CREATOR = new Creator<Route>() {
        public Route createFromParcel(Parcel parcel) {
            return new Route(parcel);
        }

        public Route[] newArray(int size) {
            return new Route[size];
        }
    };

    @Override
    public String toString() {
        //TODO: Will be split up in different views in the routes_row layout later on.
        return departure + " - " + arrival + " (" + duration + ")";
    }

    @Override
    public boolean equals(Object obj) {
        Route other = (Route) obj;
        if (routeId == null) {
            if (other.routeId != null)
                return false;
        } else if (!routeId.equals(other.routeId))
            return false;
        return true;
    }

    public static class Transport implements Parcelable {
        /*
        public static final Transport BUS = new Transport(R.drawable.transport_bus, "Bus");
        public static final Transport METRO_RED = new Transport(R.drawable.transport_metro_red, "Metro red line");
        public static final Transport METRO_BLUE = new Transport(R.drawable.transport_metro_blue, "Metro blue line");
        public static final Transport METRO_GREEN = new Transport(R.drawable.transport_metro_green, "Metro green line");
        public static final Transport COMMUTER_TRAIN = new Transport(R.drawable.transport_train, "Commuter train");
        public static final Transport TVARBANAN = new Transport(R.drawable.transport_train, "Tvärbanan");
        public static final Transport SALTSJOBANAN = new Transport(R.drawable.transport_train, "Saltsjöbanan");
        public static final Transport TRAIN = new Transport(R.drawable.transport_train, "Train");
        */

        private int mImageResource;
        private String mName;
        private int mLineNumber;

        public Transport(int imageResource, String name, int lineNumber) {
            mImageResource = imageResource;
            mName = name;
            mLineNumber = lineNumber;
        }

        public Transport(Parcel parcel) {
            mImageResource = parcel.readInt();
            mName = parcel.readString();
            mLineNumber = parcel.readInt();
        }

        public int imageResource() {
            return mImageResource;
        }

        public String name() {
            return mName;
        }

        public int lineNumber() {
            return mLineNumber;
        }

        @Override
        public String toString() {
            return mName;
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeInt(mImageResource);
            dest.writeString(mName);
            dest.writeInt(mLineNumber);
        }

        public static final Creator<Transport> CREATOR = new Creator<Transport>() {
            public Transport createFromParcel(Parcel parcel) {
                return new Transport(parcel);
            }

            public Transport[] newArray(int size) {
                return new Transport[size];
            }
        };
    }
}

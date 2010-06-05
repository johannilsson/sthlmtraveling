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
import android.text.TextUtils;

import com.markupartist.sthlmtraveling.R;

@Deprecated
public class Route implements Parcelable {
    public String ident;
    public String routeId;
    public String from;
    public String to;
    public String departure;
    public String arrival;
    public String duration;
    public String changes;
    public ArrayList<Transport> transports = new ArrayList<Transport>();

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

    /**
     * Create a text representation of the {@link Route}.
     * @return the text describing the route.
     */
    public String toTextRepresentation() {
        String shareText = from + " ⇒ " + to + "\n"
            + toString() + "\n";

        int transportCount = transports.size();
        int addedTransports = 0;
        String transportsString = "";
        for (Transport transport : transports) {
            switch (transport.mImageResource) {
            case R.drawable.transport_boat:
                transportsString += "F";
                break;
            case R.drawable.transport_bus:
                transportsString += "B";
                break;
            case R.drawable.transport_metro_blue:
            case R.drawable.transport_metro_green:
            case R.drawable.transport_metro_red:
                transportsString += "M";
                break;
            case R.drawable.transport_train:
                transportsString += "T";
                break;
            default:
                transportsString += "?";
                break;
            }

            if (transport.hasLineNumber())
                transportsString += transport.lineNumber(); 

            addedTransports++;
            if (transportCount > addedTransports)
                transportsString += " ⇾ ";
        }
        shareText += transportsString;
        return shareText;
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

    @Deprecated
    public static class Transport implements Parcelable {
        private int mImageResource;
        private String mName;
        private String mLineNumber;

        public Transport(int imageResource, String name, String lineNumber) {
            mImageResource = imageResource;
            mName = name;
            mLineNumber = lineNumber;
        }

        public Transport(int imageResource, String name) {
            mImageResource = imageResource;
            mName = name;
        }

        public Transport(Parcel parcel) {
            mImageResource = parcel.readInt();
            mName = parcel.readString();
            mLineNumber = parcel.readString();
        }

        /**
         * @return if the line number is greater than zero true otherwise false. 
         */
        public boolean hasLineNumber() {
            return !TextUtils.isEmpty(mLineNumber);
        }

        public int imageResource() {
            return mImageResource;
        }

        public String name() {
            return mName;
        }

        public String lineNumber() {
            return mLineNumber;
        }

        public int getColor() {
            if (mImageResource == R.drawable.transport_metro_blue) {
                return 0xFF25368b;
            } else if (mImageResource == R.drawable.transport_metro_green) {
                return 0xFF6ec72d;
            } else if (mImageResource == R.drawable.transport_metro_red) {
                return 0xFFf1491c;
            } else if (mImageResource == R.drawable.transport_bus) {
                return 0xFF176fa7;
            } else if (mImageResource == R.drawable.transport_train) {
                return 0xFF807e77;
            } else if (mImageResource == R.drawable.transport_boat) {
                return 0xFF176fa7;
            } else {
                return 0xFFf1491c;
            }
        }

        public String getShortName() {
            if (mImageResource == R.drawable.transport_metro_blue ||
                    mImageResource == R.drawable.transport_metro_green ||
                    mImageResource == R.drawable.transport_metro_red) {
                return "T" + mLineNumber;
            } else if (mImageResource == R.drawable.transport_bus) {
                return "B" + mLineNumber;
            } else if (mImageResource == R.drawable.transport_train) {
                return "J" + mLineNumber;
            } else if (mImageResource == R.drawable.transport_boat) {
                return "F" + mLineNumber;
            } else {
                return  mLineNumber;
            }
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
            dest.writeString(mLineNumber);
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

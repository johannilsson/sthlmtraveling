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

public class Route {
    public String ident;
    public String routeId;
    public String from;
    public String to;
    public String departure;
    public String arrival;
    public String duration;
    public String changes;
    public ArrayList<Transport> transports;

    public static Route createInstance() {
        return new Route();
    }

    public void addTransport(Transport transport) {
        if (transports == null)
            transports = new ArrayList<Transport>();
        transports.add(transport);
    }

    @Override
    public String toString() {
        //TODO: Will be split up in different views in the routes_row layout later on.
        return departure + "-" + arrival + " (" + duration + ")";
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

    public enum Transport {
        BUS             ("Bus"),
        METRO_RED       ("Metro red line"),
        METRO_BLUE      ("Metro blue line"),
        METRO_GREEN     ("Metro green line"),
        COMMUTER_TRAIN  ("Commuter train"),
        TVARBANAN       ("Tvärbanan"),
        SALTSJOBANAN    ("Saltsjöbanan"),
        TRAIN           ("Train");

        private final String transport;

        Transport(String name) {
            this.transport = name;
        }

        public int imageResource() {
            switch (this) {
                case BUS:
                    return R.drawable.transport_bus;
                case METRO_RED:
                    return R.drawable.transport_metro_red;
                case METRO_BLUE:
                    return R.drawable.transport_metro_blue;
                case METRO_GREEN:
                    return R.drawable.transport_metro_green;
                case COMMUTER_TRAIN:
                    return R.drawable.transport_train;
                case TVARBANAN:
                    return R.drawable.transport_train;
                case TRAIN:
                    return R.drawable.transport_train;
                case SALTSJOBANAN:
                    return R.drawable.transport_train;
                default:
                    return 0;
            }
        }

        public String transport() {
            return transport;
        }

        @Override
        public String toString() {
            return transport();
        }
    }

}

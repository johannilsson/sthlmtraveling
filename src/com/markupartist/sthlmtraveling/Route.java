package com.markupartist.sthlmtraveling;

import java.util.Collections;
import java.util.List;

public class Route {
    public String ident;
    public String routeId;
    public String from;
    public String to;
    public String departure;
    public String arrival;
    public String duration;
    public String changes;
    public List<String> by = Collections.emptyList();

    public static Route createInstance() {
        return new Route();
    }

    @Override
    public String toString() {
        //TODO: Will be split up in different views in the routes_row layout later on.
        return departure + "-" + arrival + ", " + changes + " changes (" + duration + ")";
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
}

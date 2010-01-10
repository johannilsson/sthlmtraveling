package com.markupartist.sthlmtraveling.provider.departure;

import static com.markupartist.sthlmtraveling.provider.EndpointConf.SL_API_ENDPOINT;
import static com.markupartist.sthlmtraveling.provider.EndpointConf.KEY;
import static com.markupartist.sthlmtraveling.provider.EndpointConf.key;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map.Entry;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import com.markupartist.sthlmtraveling.provider.site.Site;
import com.markupartist.sthlmtraveling.utils.HttpManager;

public class DeparturesStore {

    public DeparturesStore() {
    }

    public HashMap<String, DepartureList> find(Site site,
            DepartureFilter filter) throws IOException {
        final HttpGet get = new HttpGet(SL_API_ENDPOINT
                + "dpsdepartures/" + site.getId()
                + "/?key=" + key(KEY));

        HttpEntity entity = null;
        final HttpResponse response = HttpManager.execute(get);
        entity = response.getEntity();

        HashMap<String, DepartureList> departures =
            new HashMap<String, DepartureList>();
        parseResponse(entity.getContent(), departures);

        if (filter == null) {
            return departures;
        }

        for (Entry<String, DepartureList> entry : departures.entrySet()) {
            entry.getValue().filter(filter);
        }
        return departures;
    }

    private void parseResponse(InputStream in, 
                               HashMap<String, DepartureList> departures)
            throws IOException {
        try {
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            factory.setNamespaceAware(true);
            XmlPullParser parser = factory.newPullParser();
            parser.setInput(new InputStreamReader(in));

            int eventType = parser.getEventType();
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG) {
                    if (parser.getName().equals("Buses")) {
                        DepartureList busList = new DepartureList();
                        parseDepartures(parser, busList);
                        if (!busList.isEmpty())
                            departures.put("BUSES", busList);
                    } else if (parser.getName().equals("Metros")) {
                        DepartureList metroList = new DepartureList();
                        parseDepartures(parser, metroList);
                        if (!metroList.isEmpty())
                            departures.put("METROS", metroList);
                    } else if (parser.getName().equals("Trains")) {
                        DepartureList trainList = new DepartureList();
                        parseDepartures(parser, trainList);
                        if (!trainList.isEmpty())
                            departures.put("TRAINS", trainList);
                    } else if (parser.getName().equals("Trams")) {
                        DepartureList tramList = new DepartureList();
                        parseDepartures(parser, tramList);
                        if (!tramList.isEmpty())
                            departures.put("TRAMS", tramList);
                    }
                } else if (eventType == XmlPullParser.END_TAG) {
                } else if (eventType == XmlPullParser.TEXT) {
                }

                eventType = parser.next();
            }
        } catch (XmlPullParserException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private void parseDepartures(XmlPullParser parser, DepartureList departureList)
            throws XmlPullParserException, IOException {
        int type;
        while ((type = parser.next()) != XmlPullParser.END_TAG &&
                type != XmlPullParser.END_DOCUMENT) {

            if (type != XmlPullParser.START_TAG) {
                continue;
            }

            final Departure departure = new Departure();
            if (parseDeparture(parser, departure)) {
                departureList.add(departure);
            }
        }
    }

    private boolean parseDeparture(XmlPullParser parser, Departure departure)
            throws XmlPullParserException, IOException {
        int type;
        String name;
        boolean isValid = true;
        final int depth = parser.getDepth();

        while (((type = parser.next()) != XmlPullParser.END_TAG ||
                parser.getDepth() > depth) && type != XmlPullParser.END_DOCUMENT) {
            if (type != XmlPullParser.START_TAG) {
                continue;
            }

            name = parser.getName();
            if ("TransportMode".equals(name)) {
                if (parser.next() == XmlPullParser.TEXT) {
                    departure.setTransport(parser.getText());
                }
            } else if ("Destination".equals(name)) {
                if (parser.next() == XmlPullParser.TEXT) {
                    departure.setDestination(parser.getText());
                }
            } else if ("LineNumber".equals(name)) {
                if (parser.next() == XmlPullParser.TEXT) {
                    departure.setLineNumber(parser.getText());
                }
            } else if ("DisplayTime".equals(name)) {
                if (parser.next() == XmlPullParser.TEXT) {
                    departure.setDisplayTime(parser.getText());
                }
            }
        }

        return isValid;
    }
}

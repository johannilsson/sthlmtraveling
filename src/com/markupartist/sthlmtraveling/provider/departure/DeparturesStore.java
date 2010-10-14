package com.markupartist.sthlmtraveling.provider.departure;

import static com.markupartist.sthlmtraveling.provider.ApiConf.KEY;
import static com.markupartist.sthlmtraveling.provider.ApiConf.apiEndpoint;
import static com.markupartist.sthlmtraveling.provider.ApiConf.get;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map.Entry;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import android.text.format.Time;
import android.util.Log;

import com.markupartist.sthlmtraveling.provider.site.Site;
import com.markupartist.sthlmtraveling.utils.HttpManager;

public class DeparturesStore {
    static String TAG = "DeparturesStore";

    public DeparturesStore() {
    }

    public HashMap<String, DepartureList> find(Site site,
            DepartureFilter filter) throws IllegalArgumentException, IOException {
    	if (site == null) {
            Log.w(TAG, "Site is null");
    		throw new IllegalArgumentException(TAG + ", Site is null");
    	}
    	
        Log.d(TAG, "About to get departures for " + site.getName());
        final HttpGet get = new HttpGet(apiEndpoint()
                + "/dpsdepartures/" + site.getId()
                + "/?key=" + get(KEY));

        final HttpResponse response = HttpManager.execute(get);

        if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
            Log.w(TAG, "A remote server error occurred when getting departures, status code: " +
                    response.getStatusLine().getStatusCode());
            throw new IOException("A remote server error occurred when getting departures.");
        }

        HttpEntity entity = response.getEntity();
    	if (entity == null) {
            Log.w(TAG, "HttpEntity is null");
    		throw new IllegalArgumentException(TAG + ", HttpEntity is null"); // TODO IllegalArgumentException???
    	}

        HashMap<String, DepartureList> departures =
            new HashMap<String, DepartureList>();
        parseResponse(entity.getContent(), departures);

        if (filter != null) {
	        for (Entry<String, DepartureList> entry : departures.entrySet()) {
	            entry.getValue().filter(filter);
	        }
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

    /**
    <DpsMetro> 
      <SiteId>xxx</SiteId> 
      <StopAreaNumber>xxx</StopAreaNumber> 
      <TransportMode>METRO</TransportMode> 
      <StopAreaName>T-Centralen</StopAreaName> 
      <LineNumber>18</LineNumber> 
      <Destination>Alvik</Destination> 
      <TimeTabledDateTime>2010-01-11T21:23:00</TimeTabledDateTime> 
      <ExpectedDateTime>2010-01-11T21:23:00</ExpectedDateTime> 
      <DisplayTime>21:23</DisplayTime> 
      <JourneyDirection>1</JourneyDirection> 
      <GroupOfLine>tunnelbanans gröna linje</GroupOfLine> 
    </DpsMetro> 
     * @param parser
     * @param departure
     * @return
     * @throws XmlPullParserException
     * @throws IOException
     */
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
            } else if ("TimeTabledDateTime".equals(name)) {
                if (parser.next() == XmlPullParser.TEXT) {
                    Time timeTabledDateTime = new Time();
                    timeTabledDateTime.parse3339(parser.getText());
                    departure.setTimeTabledDateTime(timeTabledDateTime);
                }
            } else if ("ExpectedDateTime".equals(name)) {
                if (parser.next() == XmlPullParser.TEXT) {
                    Time expectedDateTime = new Time();
                    expectedDateTime.parse3339(parser.getText());
                    departure.setExpectedDateTime(expectedDateTime);
                }
            } else if ("GroupOfLine".equals(name)) {
                if (parser.next() == XmlPullParser.TEXT) {
                    departure.setGroupOfLine(parser.getText());
                }
            }
        }

        return isValid;
    }
}

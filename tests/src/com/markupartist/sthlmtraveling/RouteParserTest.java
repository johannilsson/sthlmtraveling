package com.markupartist.sthlmtraveling;

import android.util.Log;
import com.markupartist.sthlmtraveling.planner.RouteParser;
import com.markupartist.sthlmtraveling.planner.Route;
import junit.framework.TestCase;
import org.xml.sax.InputSource;

import java.io.StringReader;
import java.util.ArrayList;

public class RouteParserTest extends TestCase {

    private RouteParser mRouteParser;
    private static final String mRoutesXml = "<findRoutes generator='zend' version='1.0'><requestCount>1</requestCount><ident>54.010259213.1248185539</ident><routes><key_0><routeId>C0-0</routeId><from>Centralen (Klarabergsviad.)</from><to>Tensta</to><departure>approx. 19:36</departure><arrival>20:03</arrival><duration>0:27</duration><changes>1</changes><by><key_0>Bus -</key_0><key_1>Metro blue line 10</key_1></by></key_0><key_1><routeId>C0-1</routeId><from>Centralen (Klarabergsviad.)</from><to>Tensta</to><departure>approx. 19:46</departure><arrival>20:13</arrival><duration>0:27</duration><changes>1</changes><by><key_0>Bus -</key_0><key_1>Metro blue line 10</key_1></by></key_1><key_2><routeId>C0-2</routeId><from>Centralen (Klarabergsviad.)</from><to>Tensta</to><departure>approx. 19:56</departure><arrival>20:23</arrival><duration>0:27</duration><changes>1</changes><by><key_0>Bus -</key_0><key_1>Metro blue line 10</key_1></by></key_2></routes><status>success</status></findRoutes>";

    @Override
    protected void setUp() throws Exception {
        mRouteParser = new RouteParser();
    }

    @Override
    protected void tearDown() {
        mRouteParser = null;
    }

    public void testParseRoutes() {
        StringReader sr = new StringReader(mRoutesXml);
        ArrayList<Route> routes = mRouteParser.parseRoutes(new InputSource(sr));

        assertTrue(routes.size() == 3);

        Route route = routes.get(0);

        assertEquals("C0-0", route.routeId);
        assertEquals("Centralen (Klarabergsviad.)", route.from);
        assertEquals("Tensta", route.to);
        assertEquals("approx. 19:36", route.departure);
        assertEquals("20:03", route.arrival);
        assertEquals("0:27", route.duration);
        assertEquals("1", route.changes);

        assertTrue(2 == route.transports.size());
    }

    public void testParsingOfTransports() {
        StringReader sr = new StringReader("<findRoutes generator='zend' version='1.0'>" +
        		"<requestCount>1</requestCount>" +
        		"<ident>54.010259213.1248185539</ident>" +
        		"<routes>" +
        		"<key_0>" +
        		"<routeId>C0-0</routeId>" +
        		"<from>Centralen (Klarabergsviad.)</from>" +
        		"<to>Tensta</to>" +
        		"<departure>approx. 19:36</departure>" +
        		"<arrival>20:03</arrival>" +
        		"<duration>0:27</duration>" +
        		"<changes>1</changes>" +
        		"<by>" +
        		"<key_0>Bus -</key_0>" +
        		"<key_1>Metro blue line 10</key_1>" +
        		"<key_2>Metro green line 10</key_2>" +
        		"<key_3>Metro red line 10</key_3>" +
                        "<key_4>train</key_4>" +
        		"<key_5>Commuter Train</key_5>" +
        		"<key_6>tvärbanan</key_6>" +
        		"<key_7>Saltsjöbanan 25</key_7>" +
        		"</by>" +
        		"</key_0>" +
        		"</routes>" +
        		"<status>success</status>" +
        		"</findRoutes>");

        ArrayList<Route> routes = mRouteParser.parseRoutes(new InputSource(sr));
        
        Log.d("Test", "rotes: " + routes);
        
        Route route = routes.get(0);

        assertTrue(8 == route.transports.size());
    }
    
    public void testGetRequestCount() {
        StringReader sr = new StringReader(mRoutesXml);
        mRouteParser.parseRoutes(new InputSource(sr));
        assertEquals("54.010259213.1248185539", mRouteParser.getIdent());
    }

    public void testGetIdent() {
        StringReader sr = new StringReader(mRoutesXml);
        mRouteParser.parseRoutes(new InputSource(sr));
        assertEquals(1, mRouteParser.getRequestCount());
    }

}

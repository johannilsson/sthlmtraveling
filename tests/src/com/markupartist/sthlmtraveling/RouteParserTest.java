package com.markupartist.sthlmtraveling;

import java.io.StringReader;
import java.util.ArrayList;

import junit.framework.TestCase;

import org.xml.sax.InputSource;

import com.markupartist.sthlmtraveling.Route;
import com.markupartist.sthlmtraveling.RouteParser;

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

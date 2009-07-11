package com.markupartist.sthlmtraveling;

import java.io.IOException;
import java.util.ArrayList;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import android.util.Log;

public class RouteFinder extends DefaultHandler {
    //private static final String ENDPOINT = "http://markupartist.com/api/sl/";
    private static final String TAG = "RouteFinder";
    private ArrayList<Route> mRoutes = new ArrayList<Route>();
    private String mIdent;
    private String mCurrentText;
    private boolean mInBy = false;
    private Route mCurrentRoute = null;

    public ArrayList<Route> parseRoutes(InputSource input) {
        mRoutes.clear();
        try {
            SAXParserFactory spf = SAXParserFactory.newInstance();
            SAXParser sp = spf.newSAXParser();
            XMLReader xr = sp.getXMLReader();
            xr.setContentHandler(this);
            input.setEncoding("UTF-8");
            xr.parse(input);
            // TODO: Not ok to kill all exceptions like this!!!
        } catch (IOException e) {
            Log.e(TAG, e.toString());
        } catch (SAXException e) {
            Log.e(TAG, e.toString());
        } catch (ParserConfigurationException e) {
            Log.e(TAG, e.toString());
        }

        return mRoutes;
    }

    public void startElement(String uri, String name, String qName, Attributes atts) {
        if (!mInBy) {
            if (name.trim().startsWith("key_")) {
                mCurrentRoute = Route.createInstance();
            }
        }
        if (name.trim().equals("by")) {
            mInBy = true;
        }
    }

    public void characters(char ch[], int start, int length) {
        mCurrentText = (new String(ch).substring(start, start + length));
    }

    public void endElement(String uri, String name, String qName)
                throws SAXException {
        if (name.trim().equals("ident")) {
            mIdent = mCurrentText;
        }

        if (mCurrentRoute != null) {
            if (name.trim().equals("routeId")) {
                mCurrentRoute.routeId = mCurrentText;
            } else if (name.trim().equals("from")) {
                mCurrentRoute.from = mCurrentText.trim();
            } else if (name.trim().equals("to")) {
                mCurrentRoute.to = mCurrentText.trim();
            } else if (name.trim().equals("departure")) {
                mCurrentRoute.departure = mCurrentText.trim();
            } else if (name.trim().equals("arrival")) {
                mCurrentRoute.arrival = mCurrentText.trim();
            } else if (name.trim().equals("changes")) {
                mCurrentRoute.changes = mCurrentText.trim();
            } else if (name.trim().equals("duration")) {
                mCurrentRoute.duration = mCurrentText.trim();
            }
        }

        if (!mInBy) {
            if (name.trim().startsWith("key_")) {
                mCurrentRoute.ident = mIdent;
                //Log.d(TAG, "Adding route: " + currentRoute);
                mRoutes.add(mCurrentRoute);
            }
        }
        if (name.trim().equals("by")) {
            mInBy = false;
        }
    }

    // Used to test xml parsing...
    static String xmlString = "<findRoutes><ident>ng.015773217.1245358576</ident><routes><key_0><routeId>C0-0</routeId><from>T-Centralen</from><to>Tensta</to><departure>22:58</departure><arrival>23:24</arrival><duration>0:26</duration><changes>1</changes><by><key_0>tunnelbanans gröna linje 19</key_0><key_1>tunnelbanans blå linje 10</key_1></by></key_0><key_1><routeId>C0-1</routeId><from>Stockholms central</from><to>Tensta</to><departure>23:13</departure><arrival>23:39</arrival><duration>0:26</duration><changes>1</changes><by><key_0>pendeltåg 35</key_0><key_1>tunnelbanans blå linje 10</key_1></by></key_1><key_2><routeId>C0-2</routeId><from>T-Centralen</from><to>Tensta</to><departure>23:28</departure><arrival>23:54</arrival><duration>0:26</duration><changes>1</changes><by><key_0>tunnelbanans gröna linje 19</key_0><key_1>tunnelbanans blå linje 10</key_1></by></key_2></routes><status>success</status></findRoutes>";
}

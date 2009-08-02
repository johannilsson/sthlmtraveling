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

public class RouteParser extends DefaultHandler {
    private static final String TAG = "RouteFinder";
    private ArrayList<Route> mRoutes = new ArrayList<Route>();
    private String mIdent;
    private int mRequestCount;
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

    public int getRequestCount() {
        return mRequestCount;
    }

    public String getIdent() {
        return mIdent;
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
        } else if (name.trim().equals("requestCount")) {
            mRequestCount = Integer.parseInt(mCurrentText.trim());
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

        if (name.trim().equals("by")) {
            mInBy = false;
        }

        if (!mInBy) {
            if (name.trim().startsWith("key_")) {
                mCurrentRoute.ident = mIdent;
                //Log.d(TAG, "Adding route: " + mCurrentRoute);
                //Log.d(TAG, "Transports: " + mCurrentRoute.transports.toString());
                mRoutes.add(mCurrentRoute);
            }
        } else {
            if (mCurrentText.toLowerCase().contains("metro")) {
                if (mCurrentText.contains("red")) {
                    mCurrentRoute.addTransport(Route.Transport.METRO_RED);
                } else if (mCurrentText.contains("blue")) {
                    mCurrentRoute.addTransport(Route.Transport.METRO_BLUE);
                } else if (mCurrentText.contains("green")) {
                    mCurrentRoute.addTransport(Route.Transport.METRO_GREEN);
                }
            } else if (mCurrentText.toLowerCase().contains("commuter train")) {
                mCurrentRoute.addTransport(Route.Transport.COMMUTER_TRAIN);
            } else if (mCurrentText.toLowerCase().contains("train")) {
                mCurrentRoute.addTransport(Route.Transport.TRAIN);
            } else if (mCurrentText.toLowerCase().contains("tvärbanan")) {
                mCurrentRoute.addTransport(Route.Transport.TVARBANAN);
            } else if (mCurrentText.toLowerCase().contains("bus")) {
                mCurrentRoute.addTransport(Route.Transport.BUS);
            }
        }
    }
}

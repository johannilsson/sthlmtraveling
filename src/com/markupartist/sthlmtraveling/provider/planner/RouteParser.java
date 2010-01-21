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

import java.io.IOException;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import com.markupartist.sthlmtraveling.R;
import com.markupartist.sthlmtraveling.provider.planner.Route.Transport;

import android.util.Log;

public class RouteParser extends DefaultHandler {
    private static final String TAG = "RouteParser";
    private static String TRANSPORT_PATTERN = "([A-Za-zåäöÅÄÖ ]+) ([\\d]+[ A-Z]?)$";
    private Pattern mTransportPattern = Pattern.compile(TRANSPORT_PATTERN);
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
            Log.e(TAG, "IOException: " + e.toString());
        } catch (SAXException e) {
            Log.e(TAG, "SAXException: " + e.toString());
            e.printStackTrace();
        } catch (ParserConfigurationException e) {
            Log.e(TAG, "ParserConfigurationException: " + e.toString());
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
            Transport transport = createTransport(mCurrentText);
            if (transport != null) {
                mCurrentRoute.addTransport(transport);
            }
        }
    }

    private Transport createTransport(String transportString) {
        Matcher matcher = mTransportPattern.matcher(transportString);
        boolean matchFound = matcher.find(); 

        Transport transport = null;
        if (matchFound) {
            String name = matcher.group(1);
            String lineNumber = matcher.group(2);
            transport = new Transport(
                    getTransportImageResource(name), name, lineNumber);
        } else {
            transport = new Transport(
                    getTransportImageResource(transportString), transportString);
        }
        return transport;
    }

    private int getTransportImageResource(String name) {
        if (name.toLowerCase().contains("metro")) {
            if (name.contains("red")) {
                return R.drawable.transport_metro_red;
            } else if (name.contains("blue")) {
                return R.drawable.transport_metro_blue;
            } else if (name.contains("green")) {
                return R.drawable.transport_metro_green;
            }
        } else if (name.toLowerCase().contains("commuter train")) {
            return R.drawable.transport_train;
        } else if (name.toLowerCase().contains("train")) {
            return R.drawable.transport_train;
        } else if (name.toLowerCase().contains("tvärbanan")) {
            return R.drawable.transport_train;
        } else if (name.toLowerCase().contains("airport") || name.toLowerCase().contains("flygbuss")) {
            return R.drawable.transport_bus;
        } else if (name.toLowerCase().contains("bus")) {
            return R.drawable.transport_bus;
        } else if (name.toLowerCase().contains("saltsjöbanan")) {
            return R.drawable.transport_train;
        } else if (name.toLowerCase().contains("wax")) {
            return R.drawable.transport_boat;
        }
        return R.drawable.transport_unkown;
    }
}

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

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import com.markupartist.sthlmtraveling.provider.site.Site;

import android.location.Location;
import android.util.Log;

public class RouteDetailParser extends DefaultHandler {
    private static final String TAG = "StopFinder";
    private StringBuilder mTextBuffer = null;
    boolean mIsBuffering = false; 
    private int mRequestCount;
    private ArrayList<RouteDetail> mDetails = new ArrayList<RouteDetail>();
    private RouteDetail mCurrentDetail;
    private Location mCurrentLocation;
    private Site mCurrentSite;

    public ArrayList<RouteDetail> parseDetail(InputSource input) {
        mDetails.clear();
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

        return mDetails;
    }

    public int getRequestCount() {
        return mRequestCount;
    }

    public void startElement(String uri, String name, String qName, Attributes atts) {
        if (name.trim().startsWith("key_")) {
            mCurrentDetail = new RouteDetail();
            mCurrentLocation = new Location("sthlmtraveling");
            mCurrentSite = new Site();
        } else if (name.equals("description")) {
            startBuffer();
        } else if (name.equals("requestCount")) {
            startBuffer();
        } else if (name.equals("latitude")) {
            startBuffer();
        } else if (name.equals("longitude")) {
            startBuffer();
        } else if (name.equals("name")) {
            startBuffer();
        }
    }

    public void characters(char ch[], int start, int length) {
        if (mIsBuffering) {
            mTextBuffer.append(ch, start, length);
        }
    }

    public void endElement(String uri, String name, String qName)
                throws SAXException {
        if (name.trim().startsWith("key_")) {
            if (mCurrentLocation.getLongitude() != 0 || mCurrentLocation.getLatitude() != 0) {
                mCurrentSite.setLocation(mCurrentLocation);
            }
            mCurrentDetail.setSite(mCurrentSite);
            mDetails.add(mCurrentDetail);
        }

        if (name.trim().equals("requestCount")) {
            endBuffer();
            mRequestCount = Integer.parseInt(mTextBuffer.toString());
        } else if (name.equals("description")) {
            endBuffer();
            mCurrentDetail.setDescription(mTextBuffer.toString().trim());
        } else if (name.equals("latitude")) {
            endBuffer();
            mCurrentLocation.setLatitude(Double.parseDouble(mTextBuffer.toString()));
        } else if (name.equals("longitude")) {
            endBuffer();
            mCurrentLocation.setLongitude(Double.parseDouble(mTextBuffer.toString()));
        } else if (name.equals("name")) {
            endBuffer();
            mCurrentSite.setName(mTextBuffer.toString().trim());
        }
    }

    private void startBuffer() {
        mTextBuffer = new StringBuilder();
        mIsBuffering = true;
    }

    private void endBuffer() {
        mIsBuffering = false;
    }
}

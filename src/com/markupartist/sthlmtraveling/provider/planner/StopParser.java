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

import android.util.Log;

public class StopParser extends DefaultHandler {
    private static final String TAG = "StopFinder";

    private boolean mInKey = false;
    private ArrayList<String> mStops = new ArrayList<String>();

    public ArrayList<String> parseStops(InputSource input) {
        mStops.clear();
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

        return mStops;
    }

    public void startElement(String uri, String name, String qName, Attributes atts) {
        if (name.trim().startsWith("key_"))
            mInKey = true;
    }
    
    public void characters(char ch[], int start, int length) {
        if (mInKey) {
            String chars = (new String(ch).substring(start, start + length));
            mStops.add(chars.trim());
        }
    }

    public void endElement(String uri, String name, String qName)
                throws SAXException {
        if (name.trim().startsWith("key_"))
            mInKey = false;
    }
}

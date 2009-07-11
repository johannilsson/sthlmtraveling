package com.markupartist.sthlmtraveling;

import java.io.IOException;
import java.net.MalformedURLException;
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

public class RouteDetailParser extends DefaultHandler {
    private static final String TAG = "StopFinder";

    private boolean mInKey = false;
    private ArrayList<String> mDetails = new ArrayList<String>();

    public ArrayList<String> parseDetail(InputSource input) {
        mDetails.clear();
        try {
            SAXParserFactory spf = SAXParserFactory.newInstance();
            SAXParser sp = spf.newSAXParser();
            XMLReader xr = sp.getXMLReader();
            xr.setContentHandler(this);
            input.setEncoding("UTF-8");
            xr.parse(input);
            // TODO: Not ok to kill all exceptions like this!!!
        } catch (MalformedURLException e) {
            Log.e(TAG, e.toString());
        } catch (IOException e) {
            Log.e(TAG, e.toString());
        } catch (SAXException e) {
            Log.e(TAG, e.toString());
        } catch (ParserConfigurationException e) {
            Log.e(TAG, e.toString());
        }

        return mDetails;
    }

    public void startElement(String uri, String name, String qName, Attributes atts) {
        if (name.trim().startsWith("key_"))
            mInKey = true;
    }

    public void characters(char ch[], int start, int length) {
        if (mInKey) {
            String chars = (new String(ch).substring(start, start + length));
            mDetails.add(chars.trim());
        }
    }

    public void endElement(String uri, String name, String qName)
                throws SAXException {
        if (name.trim().startsWith("key_"))
            mInKey = false;
    }
}

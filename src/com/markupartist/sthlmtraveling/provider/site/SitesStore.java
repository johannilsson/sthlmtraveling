package com.markupartist.sthlmtraveling.provider.site;

import static com.markupartist.sthlmtraveling.provider.ApiConf.KEY;
import static com.markupartist.sthlmtraveling.provider.ApiConf.apiEndpoint;
import static com.markupartist.sthlmtraveling.provider.ApiConf.get;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URLEncoder;
import java.util.ArrayList;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import com.markupartist.sthlmtraveling.utils.HttpManager;

public class SitesStore {
    public ArrayList<Site> getSite(String name) throws IOException {
        ArrayList<Site> sites = new ArrayList<Site>();

        final HttpGet get = new HttpGet(apiEndpoint() + "sites/"
                + "?q=" + URLEncoder.encode(name)
                + "&key=" + get(KEY));

        HttpEntity entity = null;
        final HttpResponse response = HttpManager.execute(get);
        entity = response.getEntity();
        SiteParser.parseResponse(entity.getContent(), sites);

        return sites;
    }

    private static class SiteParser {
        public static void parseResponse(InputStream in, ArrayList<Site> sites) throws IOException {
            try {
                XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
                factory.setNamespaceAware(true);
                XmlPullParser parser = factory.newPullParser();
                parser.setInput(new InputStreamReader(in));

                int eventType = parser.getEventType();
                boolean inNumber = false;
                int number = 0;
                String name = "";
                boolean inName = false;
                while (eventType != XmlPullParser.END_DOCUMENT) {
                    if (eventType == XmlPullParser.START_TAG) {
                        if ("Name".equals(parser.getName())) {
                            inName = true;
                        } else if ("Number".equals(parser.getName())) {
                            inNumber = true;
                        }
                    } else if (eventType == XmlPullParser.END_TAG) {
                        if ("Name".equals(parser.getName())) {
                            inName = false;
                        } else if ("Number".equals(parser.getName())) {
                            inNumber = false;
                        } else if ("Site".equals(parser.getName())) {
                            Site site = new Site();
                            site.setId(number);
                            site.setName(name);
                            sites.add(site);
                        }
                    } else if (eventType == XmlPullParser.TEXT) {
                        if (inNumber) {
                            number = Integer.parseInt(parser.getText());
                        } else if (inName) {
                            name = parser.getText();
                        }
                    }
                    eventType = parser.next();
                }
            } catch (XmlPullParserException e) {
                e.printStackTrace();
            }
        }

    }
}

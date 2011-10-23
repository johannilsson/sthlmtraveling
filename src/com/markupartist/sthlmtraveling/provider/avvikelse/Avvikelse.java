package com.markupartist.sthlmtraveling.provider.avvikelse;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;

import android.net.Uri;
import android.net.Uri.Builder;
import android.util.Log;

import com.markupartist.sthlmtraveling.provider.planner.Planner.SubTrip;
import com.markupartist.sthlmtraveling.utils.StreamUtils;

public class Avvikelse {
    private SubTrip subTrip;

    public Avvikelse(SubTrip subTrip) {
        this.subTrip = subTrip;
    }

    public void report(SubTrip subTrip) {
        String url = "http://api.av.vikel.se/v1/deviations/";
        HttpPost post = new HttpPost(url);
       
        List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
        nameValuePairs.add(new BasicNameValuePair("latitude", String.valueOf(subTrip.origin.latitude / 1E6)));
        nameValuePairs.add(new BasicNameValuePair("longitude", String.valueOf(subTrip.origin.longitude / 1E6)));
        nameValuePairs.add(new BasicNameValuePair("stop_point", subTrip.origin.name));
        nameValuePairs.add(new BasicNameValuePair("transport", subTrip.transport.type));

        try {
            post.setEntity(new UrlEncodedFormEntity(nameValuePairs));
            HttpClient client = new DefaultHttpClient();
            client.execute(post);
        } catch (UnsupportedEncodingException e1) {
            e1.printStackTrace();
        } catch (ClientProtocolException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public int status(SubTrip subTrip) {
        String url = "http://api.av.vikel.se/v1/deviations/status/";

        Uri u = Uri.parse(url);
        Builder builder = u.buildUpon();
        builder.appendQueryParameter("latitude", String.valueOf(subTrip.origin.latitude / 1E6));
        builder.appendQueryParameter("longitude", String.valueOf(subTrip.origin.longitude / 1E6));
        builder.appendQueryParameter("distance", String.valueOf(200));
        u = builder.build();

        Log.d("Avvikelse", "status url " + u.toString());

        HttpGet get = new HttpGet(u.toString());
        HttpClient client = new DefaultHttpClient();

        HttpResponse response;
        int affects = 0;
        try {
            response = client.execute(get);
            HttpEntity entity = response.getEntity();
            String rawContent = StreamUtils.toString(entity.getContent());
            JSONObject jsonStatus = new JSONObject(rawContent);
            affects = jsonStatus.getInt("affects");
        } catch (ClientProtocolException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return affects;
    }
}

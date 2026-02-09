package com.markupartist.sthlmtraveling.data.misc;

import android.content.Context;
import android.util.Log;

import com.squareup.okhttp.Interceptor;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.Protocol;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;
import com.squareup.okhttp.ResponseBody;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;

public class MockInterceptor implements Interceptor {
    private static final String TAG = "MockInterceptor";
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    private final Context context;

    public MockInterceptor(Context context) {
        this.context = context.getApplicationContext();
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request request = chain.request();
        URL url = request.url();
        String path = url.getPath();

        String assetPath = mapPathToAsset(path);
        if (assetPath == null) {
            Log.d(TAG, "No mock for path: " + path);
            return chain.proceed(request);
        }

        Log.d(TAG, "Mocking " + path + " → " + assetPath);
        String json = readAsset(assetPath);

        return new Response.Builder()
                .request(request)
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .body(ResponseBody.create(JSON, json))
                .build();
    }

    private String mapPathToAsset(String path) {
        // Order matters: check more specific paths first
        if (path.contains("/v1/planner/intermediate")) {
            return "mock/planner_intermediate.json";
        }
        if (path.contains("/v1/planner")) {
            return "mock/planner.json";
        }
        if (path.matches(".*/v1/departures/\\d+.*")) {
            return "mock/departures.json";
        }
        if (path.contains("/v1/site")) {
            return "mock/sites_search.json";
        }
        if (path.contains("/semistatic/site/near") || path.contains("/v1/semistatic/site/near")) {
            return "mock/sites_nearby.json";
        }
        return null;
    }

    private String readAsset(String assetPath) throws IOException {
        InputStream is = context.getAssets().open(assetPath);
        BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line);
        }
        reader.close();
        return sb.toString();
    }
}

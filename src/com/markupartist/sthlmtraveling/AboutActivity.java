package com.markupartist.sthlmtraveling;

import android.os.Bundle;
import android.webkit.WebView;

public class AboutActivity extends BaseActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.web);
        WebView webView = (WebView) findViewById(R.id.web_view); 
        webView.loadUrl("file:///android_asset/legal.html");
    }
}

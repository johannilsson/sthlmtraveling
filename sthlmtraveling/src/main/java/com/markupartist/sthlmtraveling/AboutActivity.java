package com.markupartist.sthlmtraveling;

import android.os.Bundle;
import android.widget.TextView;

import java.io.IOException;
import java.io.InputStream;

public class AboutActivity extends BaseActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.legal);
        TextView webView = (TextView) findViewById(R.id.legal_notice);

        String about = getString(R.string.about_this_app);

        String legal = "";
        try {
            InputStream is = getAssets().open("legal.html");
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            legal += new String(buffer);
        } catch (IOException e) {
            ;
        }

        //String licenseInfo = GooglePlayServicesUtil.getOpenSourceSoftwareLicenseInfo(this);

        about += legal;

        webView.setText(about);
    }
}

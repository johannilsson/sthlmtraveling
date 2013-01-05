package com.markupartist.sthlmtraveling;

import java.io.IOException;
import java.io.InputStream;

import android.os.Bundle;
import android.widget.TextView;

import com.google.android.gms.common.GooglePlayServicesUtil;

public class AboutActivity extends BaseActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.legal);
        TextView webView = (TextView) findViewById(R.id.legal_notice);

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

        String licenseInfo = GooglePlayServicesUtil.getOpenSourceSoftwareLicenseInfo(this);

        legal += licenseInfo;

        webView.setText(legal);
    }
}

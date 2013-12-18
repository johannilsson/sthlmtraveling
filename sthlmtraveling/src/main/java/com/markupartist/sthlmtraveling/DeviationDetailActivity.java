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

package com.markupartist.sthlmtraveling;

import android.app.NotificationManager;
import android.net.Uri;
import android.os.Bundle;
import android.text.format.Time;
import android.util.Log;
import android.widget.TextView;

import com.markupartist.sthlmtraveling.provider.deviation.Deviation;


public class DeviationDetailActivity extends BaseActivity {
    static String TAG = "DeviationDetailActivity";
    //public static final String EXTRA_DEVIATION_NOTIFICATION_ID = "com.markupartist.sthlmtraveling.deviation.notificationId";
    //public static final String EXTRA_DEVIATION = "com.markupartist.sthlmtraveling.deviation";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.deviations_row);

        Log.d(TAG, "details!?");
        
        /*Bundle extras = getIntent().getExtras();
        Deviation deviation = extras.getParcelable(EXTRA_DEVIATION);
        if (extras.containsKey(EXTRA_DEVIATION_NOTIFICATION_ID)) {
            int notificationId = extras.getInt(EXTRA_DEVIATION_NOTIFICATION_ID);
            NotificationManager mNotificationManager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
            mNotificationManager.cancel(notificationId);
        }*/

        final Uri uri = getIntent().getData();
        String header = uri.getQueryParameter("header");
        String details = uri.getQueryParameter("details");
        String scope = uri.getQueryParameter("scope");
        String reference = uri.getQueryParameter("reference");
        Time created = new Time();
        created.parse3339(uri.getQueryParameter("created"));
        int notificationId = Integer.parseInt(uri.getQueryParameter("notificationId"));

        TextView headerView = (TextView) findViewById(R.id.deviation_header);
        headerView.setText(header);
        TextView detailsView = (TextView) findViewById(R.id.deviation_details);
        detailsView.setText(details);
        TextView createdView = (TextView) findViewById(R.id.deviation_created);
        createdView.setText(created.format("%F %R"));

        NotificationManager mNotificationManager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        mNotificationManager.cancel(notificationId);
    }

    public static Uri getUri(Deviation deviation, int notificationId) {
        return Uri.parse(String.format("journeyplanner://deviations?"
                + "header=%s"
                + "&details=%s"
                + "&created=%s"
                + "&scope=%s"
                + "&reference=%s"
                + "&notificationId=%s",
                deviation.getHeader(),
                deviation.getDetails(),
                deviation.getCreated().format3339(false),
                deviation.getScope(),
                deviation.getReference(),
                notificationId));
    }
}

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

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AutoCompleteTextView;
import android.widget.Button;

import com.markupartist.sthlmtraveling.provider.planner.Planner;


public class SearchDeparturesActivity extends Activity {
    static String TAG = "SearchDeparturesActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.search_departures);

        final AutoCompleteTextView siteTextView = (AutoCompleteTextView) findViewById(R.id.sites); 
        AutoCompleteStopAdapter stopAdapter = new AutoCompleteStopAdapter(this,
                android.R.layout.simple_dropdown_item_1line, Planner.getInstance());
        siteTextView.setSelectAllOnFocus(true);
        siteTextView.setAdapter(stopAdapter);

        Button searchButton = (Button) findViewById(R.id.search_departure);
        searchButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (siteTextView.getText().length() == 0) {
                    siteTextView.setError(getText(R.string.empty_value));
                } else {
                    Intent i = new Intent(getApplicationContext(), DeparturesActivity.class);
                    i.putExtra(DeparturesActivity.EXTRA_SITE_NAME, siteTextView.getText().toString());
                    startActivity(i);
                }
            }
        });
    }
}

package com.markupartist.sthlmtraveling;

import android.app.Application;

import com.markupartist.sthlmtraveling.utils.ErrorReporter;

public class MyApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        final ErrorReporter reporter = ErrorReporter.getInstance();
        reporter.init(getApplicationContext());
    }
}

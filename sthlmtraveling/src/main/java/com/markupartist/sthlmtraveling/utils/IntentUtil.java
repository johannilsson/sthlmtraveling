/*
 * Copyright (C) 2009-2014 Johan Nilsson <http://markupartist.com>
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

package com.markupartist.sthlmtraveling.utils;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.provider.Telephony;

/**
 * Created by johan on 5/4/14.
 */
public class IntentUtil {
    public static void smsIntent(final Context context, final String number, final String textBody) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            String defaultSmsPackageName = Telephony.Sms.getDefaultSmsPackage(context);
            final Intent intent = new Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:" +  Uri.encode(number)));
            intent.putExtra("sms_body", textBody);
            if (defaultSmsPackageName != null) {
                intent.setPackage(defaultSmsPackageName);
            }
            context.startActivity(intent);
        } else {
            final Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setType("vnd.android-dir/mms-sms");
            intent.putExtra("address", number);
            intent.putExtra("sms_body", textBody);
            context.startActivity(intent);
        }
    }

    public static void openSettings(Context context) {
        Intent intent = new Intent();
        intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", context.getPackageName(), null);
        intent.setData(uri);
        context.startActivity(intent);
    }
}

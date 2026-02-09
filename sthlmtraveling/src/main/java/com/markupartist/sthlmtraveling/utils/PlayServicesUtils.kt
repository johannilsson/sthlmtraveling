/*
 * Copyright (C) 2009-2015 Johan Nilsson <http://markupartist.com>
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

package com.markupartist.sthlmtraveling.utils

import android.app.Activity
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability

/**
 * Helper for Google Play services-related operations.
 */
object PlayServicesUtils {
    @JvmStatic
    fun checkGooglePlaySevices(activity: Activity): Boolean {
        val apiAvailability = GoogleApiAvailability.getInstance()
        val googlePlayServicesCheck = apiAvailability.isGooglePlayServicesAvailable(activity)
        when (googlePlayServicesCheck) {
            ConnectionResult.SUCCESS -> return true
            ConnectionResult.SERVICE_DISABLED,
            ConnectionResult.SERVICE_INVALID,
            ConnectionResult.SERVICE_MISSING,
            ConnectionResult.SERVICE_VERSION_UPDATE_REQUIRED -> {
                apiAvailability.showErrorDialogFragment(
                    activity, googlePlayServicesCheck, 0
                ) { activity.finish() }
            }
        }
        return false
    }
}

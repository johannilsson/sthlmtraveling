/*
 * Copyright 2009 ZXing authors
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
import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.annotation.StringRes

/**
 * <p>A utility class which helps ease integration with Barcode Scanner via [Intent]s. This
 * is a simple way to invoke barcode scanning.</p>
 *
 * <h2>Sharing text via barcode</h2>
 *
 * <p>To share text, encoded as a QR Code on-screen, similarly, see [shareText].</p>
 *
 * <p>Some code, particularly download integration, was contributed from the Anobiit application.</p>
 *
 * <h3>Note</h3>
 *
 * <p>This is a simple stripped version of the IntentIntegrator from the
 * ZXing project [http://zxing.googlecode.com].</p>
 *
 * @author Sean Owen
 * @author Fred Lin
 * @author Isaac Potoczny-Jones
 */
object BarcodeScannerIntegrator {

    private fun showDownloadDialog(
        activity: Activity,
        stringTitle: String,
        stringMessage: String,
        stringButtonYes: String,
        stringButtonNo: String
    ) {
        val downloadDialog = AlertDialog.Builder(activity)
        downloadDialog.setTitle(stringTitle)
        downloadDialog.setMessage(stringMessage)
        downloadDialog.setPositiveButton(stringButtonYes) { _, _ ->
            val uri = Uri.parse("market://search?q=pname:com.google.zxing.client.android")
            val intent = Intent(Intent.ACTION_VIEW, uri)
            activity.startActivity(intent)
        }
        downloadDialog.setNegativeButton(stringButtonNo) { _, _ -> }
        downloadDialog.show()
    }

    /**
     * Shares the given text by encoding it as a barcode, such that another user can
     * scan the text off the screen of the device.
     *
     * @param text the text string to encode as a barcode
     * @param stringTitle title of dialog prompting user to download Barcode Scanner
     * @param stringMessage text of dialog prompting user to download Barcode Scanner
     * @param stringButtonYes text of button user clicks when agreeing to download
     *  Barcode Scanner (e.g. "Yes")
     * @param stringButtonNo text of button user clicks when declining to download
     *  Barcode Scanner (e.g. "No")
     */
    @JvmStatic
    fun shareText(
        activity: Activity,
        text: String,
        @StringRes stringTitle: Int,
        @StringRes stringMessage: Int,
        @StringRes stringButtonYes: Int,
        @StringRes stringButtonNo: Int
    ) {
        val intent = Intent()
        intent.action = "com.google.zxing.client.android.ENCODE"
        intent.putExtra("ENCODE_TYPE", "TEXT_TYPE")
        intent.putExtra("ENCODE_DATA", text)
        //intent.putExtra("com.google.zxing.client.android.ENCODE_FORMAT", "QR_CODE")
        try {
            activity.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            showDownloadDialog(
                activity,
                activity.getString(stringTitle),
                activity.getString(stringMessage),
                activity.getString(stringButtonYes),
                activity.getString(stringButtonNo)
            )
        }
    }
}

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

package com.markupartist.sthlmtraveling.ui.view;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.widget.Toast;

import com.markupartist.sthlmtraveling.R;
import com.markupartist.sthlmtraveling.utils.Analytics;
import com.markupartist.sthlmtraveling.utils.IntentUtil;

/**
 * Dialog that shows options when buying a SMS ticket.
 * <p/>
 * To be refactored to a DialogFragment when we've modernized the app.
 */
public class SmsTicketDialog {

    public static Dialog createDialog(final Context context, final String tariffZones) {

        Analytics.getInstance(context).event("Ticket", "Open Dialog");

        CharSequence[] smsOptions = {
                context.getText(R.string.sms_ticket_price_full) + " " + getFullPrice(tariffZones),
                context.getText(R.string.sms_ticket_price_reduced) + " " + getReducedPrice(tariffZones)
        };
        return new AlertDialog.Builder(context)
                .setTitle(String.format("%s (%s)", context.getText(R.string.sms_ticket_label), tariffZones))
                .setItems(smsOptions, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int item) {
                        switch (item) {
                            case 0:
                                sendSms(context, false, tariffZones);
                                break;
                            case 1:
                                sendSms(context, true, tariffZones);
                                break;
                        }
                    }
                }).create();
    }

    private static CharSequence getFullPrice(final String tariffZones) {
        final int[] PRICE = new int[]{ 36, 54, 72 };
        return PRICE[tariffZones.length() - 1] + " kr";
    }

    private static CharSequence getReducedPrice(final String tariffZones) {
        final int[] PRICE = new int[]{ 20, 30, 40 };
        return PRICE[tariffZones.length() - 1] + " kr";
    }

    /**
     * Invokes the Messaging application.
     *
     * @param reducedPrice True if the price is reduced, false otherwise.
     */
    private static void sendSms(final Context context, final boolean reducedPrice, final String tariffZones) {
        Toast.makeText(context, R.string.sms_ticket_notice_message, Toast.LENGTH_LONG).show();
        String price = reducedPrice ? "R" : "H";

        Analytics.getInstance(context).event("Ticket", "Buy SMS Ticket", price);

        String number = "0767201010";
        IntentUtil.smsIntent(context, number, price + tariffZones);
    }
}

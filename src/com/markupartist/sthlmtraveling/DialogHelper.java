package com.markupartist.sthlmtraveling;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface.OnClickListener;

public class DialogHelper {
    /**
     * Creates a dialog to display in case of network problems.
     * @param activity the activity
     * @param onClickListener the on click listener for the retry button  
     * @return a dialog
     */
    public static AlertDialog createNetworkProblemDialog(Activity activity,
            OnClickListener onClickListener) {
        return new AlertDialog.Builder(activity)
            .setIcon(android.R.drawable.ic_dialog_alert)
            .setTitle(activity.getText(R.string.attention_label))
            .setMessage(activity.getText(R.string.network_problem_message))
            .setPositiveButton("Retry", onClickListener)
            .setNegativeButton(activity.getText(android.R.string.cancel), null)
            .create();
    }
}

package com.markupartist.sthlmtraveling.utils;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.SuppressLint;
import android.os.Build;
import android.view.View;

/**
 * Created by johan on 4/6/14.
 */
public class ViewHelper {
    @SuppressLint("NewApi")
    public static void crossfade(final View fromView, final View toView) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB_MR1) {
            return;
        }
        // Set the content view to 0% opacity but visible, so that it is visible
        // (but fully transparent) during the animation.

        toView.setAlpha(0f);
        toView.setVisibility(View.VISIBLE);

        // Animate the content view to 100% opacity, and clear any animation
        // listener set on the view.
        toView.animate()
                .alpha(1f)
                .setDuration(200)
                .setListener(null);

        // Animate the loading view to 0% opacity. After the animation ends,
        // set its visibility to GONE as an optimization step (it won't
        // participate in layout passes, etc.)
        fromView.animate()
                .alpha(0f)
                .setDuration(200)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        fromView.setVisibility(View.GONE);
                    }
                });
    }
}

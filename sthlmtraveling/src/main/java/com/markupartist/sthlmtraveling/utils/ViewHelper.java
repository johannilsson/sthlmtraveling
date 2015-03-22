package com.markupartist.sthlmtraveling.utils;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.SuppressLint;
import android.content.res.Resources;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.view.View;

import com.markupartist.sthlmtraveling.R;

/**
 * Created by johan on 4/6/14.
 */
public class ViewHelper {
    @SuppressLint("NewApi")
    public static void crossfade(final View fromView, final View toView) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB_MR1) {
            return;
        }

        if (true) {
            toView.setVisibility(View.VISIBLE);
            fromView.setVisibility(View.GONE);
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

    public static Drawable tintIcon(Resources res, Drawable d) {
        return tintIcon(d, res.getColor(R.color.icon_default));
    }

    public static Drawable tintIcon(Drawable d, int color) {
        Drawable m = d.mutate();
        m.setColorFilter(color, PorterDuff.Mode.SRC_IN);
        m.setAlpha(255);
        return d;
    }
}

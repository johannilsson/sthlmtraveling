package com.markupartist.sthlmtraveling.utils;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.SuppressLint;
import android.content.res.Resources;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.util.TypedValue;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.markupartist.sthlmtraveling.R;
import com.markupartist.sthlmtraveling.provider.TransportMode;

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

    public static Drawable tintIcon(Drawable d, @ColorInt int color) {
        Drawable m = d.mutate();
        m.setColorFilter(color, PorterDuff.Mode.SRC_IN);
        m.setAlpha(255);
        return d;
    }

    public static void tint(@NonNull ImageView v, @ColorInt int color) {
        v.setImageDrawable(tintIcon(v.getDrawable(), color));
    }

    public static void setText(@NonNull TextView v, @NonNull CharSequence t) {
        if (!t.equals(v.getText())) {
            v.setText(t);
        }
    }

    /**
     * Helper method to convert dips to pixels.
     */
    public static int dipsToPix(final Resources res, final float dps) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dps,
                res.getDisplayMetrics());
    }

    @ColorInt
    public static int getLineColor(Resources res, int transportMode, String lineNumber) {
        switch (transportMode) {
            case TransportMode.LOKALBANA_INDEX:
                if ("22".equals(lineNumber)) {
                    return res.getColor(R.color.traffic_type_l22);
                } else if ("12".equals(lineNumber)) {
                    return res.getColor(R.color.traffic_type_l12);
                } else if ("21".equals(lineNumber)) {
                    return res.getColor(R.color.traffic_type_l21);
                } else if ("25".equals(lineNumber) || "26".equals(lineNumber)) {
                    return res.getColor(R.color.traffic_type_l2526);
                } else if ("27".equals(lineNumber) || "28".equals(lineNumber) || "29".equals(lineNumber)) {
                    return res.getColor(R.color.traffic_type_l272829);
                }
                return res.getColor(R.color.train);
            case TransportMode.METRO_INDEX:
                if ("17".equals(lineNumber) || "18".equals(lineNumber) || "19".equals(lineNumber)) {
                    return res.getColor(R.color.metro_green);
                } else if ("13".contains(lineNumber) || "14".contains(lineNumber)) {
                    return res.getColor(R.color.metro_red);
                } else if ("10".contains(lineNumber) || "11".contains(lineNumber)) {
                    return res.getColor(R.color.metro_blue);
                }
            case TransportMode.TRAIN_INDEX:
                if ("35".equals(lineNumber)) {
                    return res.getColor(R.color.traffic_type_j35);
                } else if ("36".equals(lineNumber) || "37".equals(lineNumber) || "38".equals(lineNumber)) {
                    return res.getColor(R.color.traffic_type_j363738);
                }
                return res.getColor(R.color.train);
            case TransportMode.BUS_INDEX:
                if ("1".equals(lineNumber)) {
                    return res.getColor(R.color.traffic_type_b1);
                } else if ("2".equals(lineNumber)) {
                    return res.getColor(R.color.traffic_type_b2);
                } else if ("3".equals(lineNumber)) {
                    return res.getColor(R.color.traffic_type_b3);
                } else if ("4".equals(lineNumber)) {
                    return res.getColor(R.color.traffic_type_b4);
                }
        }
        return res.getColor(R.color.train);
    }
}

package com.markupartist.sthlmtraveling.utils;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Build;

import androidx.annotation.AttrRes;
import androidx.annotation.ColorInt;
import androidx.annotation.ColorRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.markupartist.sthlmtraveling.R;
import com.markupartist.sthlmtraveling.data.models.Leg;
import com.markupartist.sthlmtraveling.data.models.RealTimeState;
import com.markupartist.sthlmtraveling.provider.TransportMode;

import java.util.Locale;

/**
 * Created by johan on 4/6/14.
 */
public class ViewHelper {
    /**
     * Get the height of the display in pixels
     *
     * @param context The Context
     * @return Height of the display in pixels
     */
    public static int getDisplayHeight(Context context) {
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        return displayMetrics.heightPixels;
    }

    /**
     * Get the width of the display in pixels
     *
     * @param context The Context
     * @return Width of the display in pixels
     */
    public static int getDisplayWidth(Context context) {
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        return displayMetrics.widthPixels;
    }

    @SuppressLint("NewApi")
    public static void crossfade(final View fromView, final View toView) {
        // Set the content view to 0% opacity but visible, so that it is visible
        // (but fully transparent) during the animation.

        toView.setVisibility(View.VISIBLE);
        fromView.setVisibility(View.GONE);
        if (true) {
            return;
        }

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
        return tintIcon(d, res.getColor(R.color.icon_default_inverse));
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

    public static Drawable getDrawableColorInt(Context context, @DrawableRes int drawableRes,
                                               @ColorInt int color) {
        return tintIcon(ContextCompat.getDrawable(context, drawableRes), color);
    }

    public static Drawable getDrawableColorRes(Context context, @DrawableRes int drawableRes,
                                               @ColorRes int color) {
        return tintIcon(
                ContextCompat.getDrawable(context, drawableRes),
                ContextCompat.getColor(context, color));
    }

    public static int getColorFromAttr(Context context, @AttrRes int attrColor) {
        TypedValue typedValue = new TypedValue();
        context.getTheme().resolveAttribute(attrColor, typedValue, true);
        return typedValue.data;
    }

    public static void setText(@NonNull TextView v, @NonNull CharSequence t) {
        if (!t.equals(v.getText())) {
            v.setText(t);
        }
    }

    public static void flipIfRtl(View view) {
        if (RtlUtils.isRtl(Locale.getDefault())) {
            ViewCompat.setScaleX(view, -1f);
        }
    }

    /**
     * Helper method to convert dips to pixels.
     */
    public static int dipsToPix(final Resources res, final float dps) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dps,
                res.getDisplayMetrics());
    }

    /**
     * Uppercase the first letter of the passed str.
     *
     * @param str String to uppercase
     * @return str with the first letter uppercase
     */
    public static String uppercaseFirst(final String str, final Locale locale) {
        return str.substring(0, 1).toUpperCase(locale) + str.substring(1);
    }

    public static String getLineName(int transportMode, String lineNumber) {
        // Will be moved to the new API.
        if (TextUtils.isEmpty(lineNumber)) {
            return null;
        }
        switch (transportMode) {
            case TransportMode.TRAM_INDEX:
                if ("7".equals(lineNumber)
                        || "12".equals(lineNumber)
                        || "22".equals(lineNumber)
                        || "21".equals(lineNumber)) {
                    return "S" + lineNumber;
                }
                return "L" + lineNumber;
            case TransportMode.METRO_INDEX:
                return "T" + lineNumber;
            case TransportMode.TRAIN_INDEX:
                return "J" + lineNumber;
            case TransportMode.BUS_INDEX:
                return "B" + lineNumber;
        }
        return lineNumber;
    }

    @ColorInt
    public static int getLineColor(Context context, int transportMode,
                                   String lineNumber, String lineName) {
        switch (transportMode) {
            case TransportMode.TRAM_INDEX:
                if ("22".equals(lineNumber)) {
                    return ContextCompat.getColor(context, R.color.traffic_type_l22);
                } else if ("12".equals(lineNumber)) {
                    return ContextCompat.getColor(context, R.color.traffic_type_l12);
                } else if ("21".equals(lineNumber)) {
                    return ContextCompat.getColor(context, R.color.traffic_type_l21);
                } else if ("25".equals(lineNumber)
                        || "26".equals(lineNumber)) {
                    return ContextCompat.getColor(context, R.color.traffic_type_l2526);
                } else if ("27".equals(lineNumber) || "27S".equals(lineNumber)
                        || "28".equals(lineNumber) || "28S".equals(lineNumber)
                        || "29".equals(lineNumber) || "29S".equals(lineNumber)) {
                    return ContextCompat.getColor(context, R.color.traffic_type_l272829);
                }
                return ContextCompat.getColor(context, R.color.train);
            case TransportMode.METRO_INDEX:
                if ("17".equals(lineNumber)
                        || "18".equals(lineNumber)
                        || "19".equals(lineNumber)) {
                    return ContextCompat.getColor(context, R.color.metro_green);
                } else if ("13".contains(lineNumber)
                        || "14".contains(lineNumber)) {
                    return ContextCompat.getColor(context, R.color.metro_red);
                } else if ("10".contains(lineNumber)
                        || "11".contains(lineNumber)) {
                    return ContextCompat.getColor(context, R.color.metro_blue);
                }
            case TransportMode.TRAIN_INDEX:
                if ("35".equals(lineNumber)) {
                    return ContextCompat.getColor(context, R.color.traffic_type_j35);
                } else if ("36".equals(lineNumber)
                        || "37".equals(lineNumber)
                        || "38".equals(lineNumber)) {
                    return ContextCompat.getColor(context, R.color.traffic_type_j363738);
                }
                return ContextCompat.getColor(context, R.color.train);
            case TransportMode.BUS_INDEX:
                if ("1".equals(lineNumber)) {
                    return ContextCompat.getColor(context, R.color.traffic_type_b1);
                } else if ("2".equals(lineNumber)) {
                    return ContextCompat.getColor(context, R.color.traffic_type_b2);
                } else if ("3".equals(lineNumber)) {
                    return ContextCompat.getColor(context, R.color.traffic_type_b3);
                } else if ("4".equals(lineNumber)) {
                    return ContextCompat.getColor(context, R.color.traffic_type_b4);
                }
                if (lineName != null && lineName.contains("blå")) {
                    return ContextCompat.getColor(context, R.color.traffic_type_b1);
                }
                return ContextCompat.getColor(context, R.color.bus_red);
            case TransportMode.BOAT_INDEX:
                return ContextCompat.getColor(context, R.color.traffic_type_boat);
        }
        return ContextCompat.getColor(context, R.color.train);
    }

    public static Drawable getColoredDrawableForTransport(Context context, int transportMode,
                                                   String lineName, String lineNumber) {
        int color = ViewHelper.getLineColor(context, transportMode, lineNumber, lineName);
        Drawable drawable;
        switch (transportMode) {
            case TransportMode.BUS_INDEX:
            case TransportMode.NAR_INDEX:
                drawable = ContextCompat.getDrawable(context, R.drawable.ic_transport_bus_20dp);
                if (lineName.contains("blå")) {
                    return ViewHelper.tintIcon(drawable, color);
                }
                return ViewHelper.tintIcon(drawable, ContextCompat.getColor(context, R.color.bus_red));
            case TransportMode.METRO_INDEX:
                drawable = ContextCompat.getDrawable(context, R.drawable.ic_transport_sl_metro);
                return ViewHelper.tintIcon(drawable, color);
            case TransportMode.FOOT_INDEX:
                drawable = ContextCompat.getDrawable(context, R.drawable.ic_transport_walk_20dp);
                return ViewHelper.tintIcon(drawable, ContextCompat.getColor(context, R.color.icon_default));
            case TransportMode.TRAIN_INDEX:
                drawable = ContextCompat.getDrawable(context, R.drawable.ic_transport_train_20dp);
                return ViewHelper.tintIcon(drawable, color);
            case TransportMode.TRAM_INDEX:
                drawable = ContextCompat.getDrawable(context, R.drawable.ic_transport_light_train_20dp);
                return ViewHelper.tintIcon(drawable, color);
            case TransportMode.BOAT_INDEX:
                drawable = ContextCompat.getDrawable(context, R.drawable.ic_transport_boat_20dp);
                return ViewHelper.tintIcon(drawable, ContextCompat.getColor(context, R.color.traffic_type_b4));
            default:
                // What to use when we don't know..
                drawable = ContextCompat.getDrawable(context, R.drawable.ic_transport_train_20dp);
        }
        return ViewHelper.tintIcon(drawable, ContextCompat.getColor(context, R.color.train));
    }

    public static Drawable getDrawableForTransport(Context context, int transportMode,
                                                   String lineName, String lineNumber) {
        int color = ContextCompat.getColor(context, R.color.icon_default);
        Drawable drawable;
        switch (transportMode) {
            case TransportMode.BUS_INDEX:
            case TransportMode.NAR_INDEX:
                drawable = ContextCompat.getDrawable(context, R.drawable.ic_transport_bus_20dp);
                return ViewHelper.tintIcon(drawable, color);
            case TransportMode.METRO_INDEX:
                drawable = ContextCompat.getDrawable(context, R.drawable.ic_transport_sl_metro);
                return ViewHelper.tintIcon(drawable, color);
            case TransportMode.FOOT_INDEX:
                drawable = ContextCompat.getDrawable(context, R.drawable.ic_transport_walk_20dp);
                return ViewHelper.tintIcon(drawable, ContextCompat.getColor(context, R.color.icon_default));
            case TransportMode.TRAIN_INDEX:
                drawable = ContextCompat.getDrawable(context, R.drawable.ic_transport_train_20dp);
                return ViewHelper.tintIcon(drawable, color);
            case TransportMode.TRAM_INDEX:
                drawable = ContextCompat.getDrawable(context, R.drawable.ic_transport_light_train_20dp);
                return ViewHelper.tintIcon(drawable, color);
            case TransportMode.BOAT_INDEX:
                drawable = ContextCompat.getDrawable(context, R.drawable.ic_transport_boat_20dp);
                return ViewHelper.tintIcon(drawable, color);
            default:
                // What to use when we don't know..
                drawable = ContextCompat.getDrawable(context, R.drawable.ic_transport_train_20dp);
        }
        return ViewHelper.tintIcon(drawable, color);
    }

    public static @ColorRes int getTextColorByRealtimeState(RealTimeState realTimeState) {
        switch (realTimeState) {
            case AHEAD_OF_SCHEDULE:
            case ON_TIME:
                return R.color.schedule_ahead;
            case BEHIND_SCHEDULE:
                return R.color.schedule_late;
        }
        return R.color.body_text_1;
    }

    public static void setTextColorForTimeView(TextView textView, Leg leg, boolean isDeparture) {
        textView.setTextColor(ContextCompat.getColor(textView.getContext(),
                getTextColorByRealtimeState(leg.realtimeState(isDeparture))));
    }
}

package com.markupartist.sthlmtraveling.utils

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Resources
import android.graphics.PorterDuff
import android.graphics.drawable.Drawable
import android.text.TextUtils
import android.util.DisplayMetrics
import android.util.TypedValue
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import com.markupartist.sthlmtraveling.R
import com.markupartist.sthlmtraveling.data.models.Leg
import com.markupartist.sthlmtraveling.data.models.RealTimeState
import com.markupartist.sthlmtraveling.provider.TransportMode
import java.util.Locale

/**
 * Created by johan on 4/6/14.
 */
object ViewHelper {
    /**
     * Get the height of the display in pixels
     *
     * @param context The Context
     * @return Height of the display in pixels
     */
    @JvmStatic
    fun getDisplayHeight(context: Context): Int {
        val displayMetrics = context.resources.displayMetrics
        return displayMetrics.heightPixels
    }

    /**
     * Get the width of the display in pixels
     *
     * @param context The Context
     * @return Width of the display in pixels
     */
    @JvmStatic
    fun getDisplayWidth(context: Context): Int {
        val displayMetrics = context.resources.displayMetrics
        return displayMetrics.widthPixels
    }

    @SuppressLint("NewApi")
    @JvmStatic
    fun crossfade(fromView: View, toView: View) {
        // Set the content view to 0% opacity but visible, so that it is visible
        // (but fully transparent) during the animation.

        toView.visibility = View.VISIBLE
        fromView.visibility = View.GONE
        if (true) {
            return
        }

        toView.alpha = 0f
        toView.visibility = View.VISIBLE

        // Animate the content view to 100% opacity, and clear any animation
        // listener set on the view.
        toView.animate()
            .alpha(1f)
            .setDuration(200)
            .setListener(null)

        // Animate the loading view to 0% opacity. After the animation ends,
        // set its visibility to GONE as an optimization step (it won't
        // participate in layout passes, etc.)
        fromView.animate()
            .alpha(0f)
            .setDuration(200)
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    fromView.visibility = View.GONE
                }
            })
    }

    @JvmStatic
    fun tintIcon(res: Resources, d: Drawable?): Drawable? {
        return tintIcon(d, res.getColor(R.color.icon_default_inverse))
    }

    @JvmStatic
    fun tintIcon(d: Drawable?, @ColorInt color: Int): Drawable? {
        val m = d?.mutate()
        m?.setColorFilter(color, PorterDuff.Mode.SRC_IN)
        m?.alpha = 255
        return d
    }

    @JvmStatic
    fun tint(v: ImageView, @ColorInt color: Int) {
        v.setImageDrawable(tintIcon(v.drawable, color))
    }

    @JvmStatic
    fun getDrawableColorInt(
        context: Context, @DrawableRes drawableRes: Int,
        @ColorInt color: Int
    ): Drawable? {
        return tintIcon(ContextCompat.getDrawable(context, drawableRes), color)
    }

    @JvmStatic
    fun getDrawableColorRes(
        context: Context, @DrawableRes drawableRes: Int,
        @ColorRes color: Int
    ): Drawable? {
        return tintIcon(
            ContextCompat.getDrawable(context, drawableRes),
            ContextCompat.getColor(context, color)
        )
    }

    @JvmStatic
    fun getColorFromAttr(context: Context, @AttrRes attrColor: Int): Int {
        val typedValue = TypedValue()
        context.theme.resolveAttribute(attrColor, typedValue, true)
        return typedValue.data
    }

    @JvmStatic
    fun setText(v: TextView, t: CharSequence) {
        if (t != v.text) {
            v.text = t
        }
    }

    @JvmStatic
    fun flipIfRtl(view: View) {
        if (RtlUtils.isRtl(Locale.getDefault())) {
            ViewCompat.setScaleX(view, -1f)
        }
    }

    /**
     * Helper method to convert dips to pixels.
     */
    @JvmStatic
    fun dipsToPix(res: Resources, dps: Float): Int {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dps, res.displayMetrics).toInt()
    }

    /**
     * Uppercase the first letter of the passed str.
     *
     * @param str String to uppercase
     * @return str with the first letter uppercase
     */
    @JvmStatic
    fun uppercaseFirst(str: String?, locale: Locale): String? {
        return if (str != null) str.substring(0, 1).uppercase(locale) + str.substring(1) else null
    }

    @JvmStatic
    fun getLineName(transportMode: Int, lineNumber: String?): String? {
        // Will be moved to the new API.
        if (TextUtils.isEmpty(lineNumber)) {
            return null
        }
        return when (transportMode) {
            TransportMode.TRAM_INDEX -> {
                if ("7" == lineNumber || "12" == lineNumber || "22" == lineNumber || "21" == lineNumber) {
                    "S$lineNumber"
                } else {
                    "L$lineNumber"
                }
            }
            TransportMode.METRO_INDEX -> "T$lineNumber"
            TransportMode.TRAIN_INDEX -> "J$lineNumber"
            TransportMode.BUS_INDEX -> "B$lineNumber"
            else -> lineNumber
        }
    }

    @ColorInt
    @JvmStatic
    fun getLineColor(
        context: Context, transportMode: Int,
        lineNumber: String?, lineName: String?
    ): Int {
        return when (transportMode) {
            TransportMode.TRAM_INDEX -> {
                when (lineNumber) {
                    "22" -> ContextCompat.getColor(context, R.color.traffic_type_l22)
                    "12" -> ContextCompat.getColor(context, R.color.traffic_type_l12)
                    "21" -> ContextCompat.getColor(context, R.color.traffic_type_l21)
                    "25", "26" -> ContextCompat.getColor(context, R.color.traffic_type_l2526)
                    "27", "27S", "28", "28S", "29", "29S" -> ContextCompat.getColor(context, R.color.traffic_type_l272829)
                    else -> ContextCompat.getColor(context, R.color.train)
                }
            }
            TransportMode.METRO_INDEX -> {
                when {
                    "17" == lineNumber || "18" == lineNumber || "19" == lineNumber ->
                        ContextCompat.getColor(context, R.color.metro_green)
                    "13".contains(lineNumber ?: "") || "14".contains(lineNumber ?: "") ->
                        ContextCompat.getColor(context, R.color.metro_red)
                    "10".contains(lineNumber ?: "") || "11".contains(lineNumber ?: "") ->
                        ContextCompat.getColor(context, R.color.metro_blue)
                    else -> ContextCompat.getColor(context, R.color.train)
                }
            }
            TransportMode.TRAIN_INDEX -> {
                when (lineNumber) {
                    "35" -> ContextCompat.getColor(context, R.color.traffic_type_j35)
                    "36", "37", "38" -> ContextCompat.getColor(context, R.color.traffic_type_j363738)
                    else -> ContextCompat.getColor(context, R.color.train)
                }
            }
            TransportMode.BUS_INDEX -> {
                when (lineNumber) {
                    "1" -> ContextCompat.getColor(context, R.color.traffic_type_b1)
                    "2" -> ContextCompat.getColor(context, R.color.traffic_type_b2)
                    "3" -> ContextCompat.getColor(context, R.color.traffic_type_b3)
                    "4" -> ContextCompat.getColor(context, R.color.traffic_type_b4)
                    else -> {
                        if (lineName != null && lineName.contains("blå")) {
                            ContextCompat.getColor(context, R.color.traffic_type_b1)
                        } else {
                            ContextCompat.getColor(context, R.color.bus_red)
                        }
                    }
                }
            }
            TransportMode.BOAT_INDEX -> ContextCompat.getColor(context, R.color.traffic_type_boat)
            else -> ContextCompat.getColor(context, R.color.train)
        }
    }

    @JvmStatic
    fun getColoredDrawableForTransport(
        context: Context, transportMode: Int,
        lineName: String, lineNumber: String
    ): Drawable? {
        val color = getLineColor(context, transportMode, lineNumber, lineName)
        val drawable: Drawable? = when (transportMode) {
            TransportMode.BUS_INDEX, TransportMode.NAR_INDEX -> {
                val d = ContextCompat.getDrawable(context, R.drawable.ic_transport_bus_20dp)
                return if (lineName.contains("blå")) {
                    tintIcon(d, color)
                } else {
                    tintIcon(d, ContextCompat.getColor(context, R.color.bus_red))
                }
            }
            TransportMode.METRO_INDEX -> {
                val d = ContextCompat.getDrawable(context, R.drawable.ic_transport_sl_metro)
                return tintIcon(d, color)
            }
            TransportMode.FOOT_INDEX -> {
                val d = ContextCompat.getDrawable(context, R.drawable.ic_transport_walk_20dp)
                return tintIcon(d, ContextCompat.getColor(context, R.color.icon_default))
            }
            TransportMode.TRAIN_INDEX -> {
                val d = ContextCompat.getDrawable(context, R.drawable.ic_transport_train_20dp)
                return tintIcon(d, color)
            }
            TransportMode.TRAM_INDEX -> {
                val d = ContextCompat.getDrawable(context, R.drawable.ic_transport_light_train_20dp)
                return tintIcon(d, color)
            }
            TransportMode.BOAT_INDEX -> {
                val d = ContextCompat.getDrawable(context, R.drawable.ic_transport_boat_20dp)
                return tintIcon(d, ContextCompat.getColor(context, R.color.traffic_type_b4))
            }
            else -> {
                // What to use when we don't know..
                ContextCompat.getDrawable(context, R.drawable.ic_transport_train_20dp)
            }
        }
        return tintIcon(drawable, ContextCompat.getColor(context, R.color.train))
    }

    @JvmStatic
    fun getDrawableForTransport(
        context: Context, transportMode: Int,
        lineName: String?, lineNumber: String?
    ): Drawable? {
        val color = ContextCompat.getColor(context, R.color.icon_default)
        val drawable: Drawable? = when (transportMode) {
            TransportMode.BUS_INDEX, TransportMode.NAR_INDEX -> {
                val d = ContextCompat.getDrawable(context, R.drawable.ic_transport_bus_20dp)
                return tintIcon(d, color)
            }
            TransportMode.METRO_INDEX -> {
                val d = ContextCompat.getDrawable(context, R.drawable.ic_transport_sl_metro)
                return tintIcon(d, color)
            }
            TransportMode.FOOT_INDEX -> {
                val d = ContextCompat.getDrawable(context, R.drawable.ic_transport_walk_20dp)
                return tintIcon(d, ContextCompat.getColor(context, R.color.icon_default))
            }
            TransportMode.TRAIN_INDEX -> {
                val d = ContextCompat.getDrawable(context, R.drawable.ic_transport_train_20dp)
                return tintIcon(d, color)
            }
            TransportMode.TRAM_INDEX -> {
                val d = ContextCompat.getDrawable(context, R.drawable.ic_transport_light_train_20dp)
                return tintIcon(d, color)
            }
            TransportMode.BOAT_INDEX -> {
                val d = ContextCompat.getDrawable(context, R.drawable.ic_transport_boat_20dp)
                return tintIcon(d, color)
            }
            else -> {
                // What to use when we don't know..
                ContextCompat.getDrawable(context, R.drawable.ic_transport_train_20dp)
            }
        }
        return tintIcon(drawable, color)
    }

    @JvmStatic
    @ColorRes
    fun getTextColorByRealtimeState(realTimeState: RealTimeState?): Int {
        return when (realTimeState) {
            RealTimeState.AHEAD_OF_SCHEDULE, RealTimeState.ON_TIME -> R.color.schedule_ahead
            RealTimeState.BEHIND_SCHEDULE -> R.color.schedule_late
            else -> R.color.body_text_1
        }
    }

    @JvmStatic
    fun setTextColorForTimeView(textView: TextView, leg: Leg, isDeparture: Boolean) {
        textView.setTextColor(
            ContextCompat.getColor(
                textView.context,
                getTextColorByRealtimeState(leg.realtimeState(isDeparture))
            )
        )
    }
}

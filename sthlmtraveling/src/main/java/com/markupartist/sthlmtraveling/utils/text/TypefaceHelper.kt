package com.markupartist.sthlmtraveling.utils.text

import android.graphics.Typeface

/**
 * Each call to Typeface.createFromAsset will load a new instance of the typeface into memory,
 * and this memory is not consistently get garbage collected
 * http://code.google.com/p/android/issues/detail?id=9904
 * (It states released but even on Lollipop you can see the typefaces accumulate even after
 * multiple GC passes)
 * You can detect this by running:
 * adb shell dumpsys meminfo com.your.packagenage
 * You will see output like:
 * Asset Allocations
 * zip:/data/app/com.your.packagenage-1.apk:/assets/Roboto-Medium.ttf: 125K
 * zip:/data/app/com.your.packagenage-1.apk:/assets/Roboto-Medium.ttf: 125K
 * zip:/data/app/com.your.packagenage-1.apk:/assets/Roboto-Medium.ttf: 125K
 * zip:/data/app/com.your.packagenage-1.apk:/assets/Roboto-Regular.ttf: 123K
 * zip:/data/app/com.your.packagenage-1.apk:/assets/Roboto-Medium.ttf: 125K
 * @author Aidan Follestad (afollestad), Kevin Barry (teslacoil)
 */
object TypefaceHelper {
    private val cache = HashMap<String, Typeface>()

    fun get(name: String?, style: Int): Typeface? {
        if (name == null) return null

        synchronized(cache) {
            return cache.getOrPut(name) {
                try {
                    Typeface.create(name, style)
                } catch (e: RuntimeException) {
                    return null
                }
            }
        }
    }
}
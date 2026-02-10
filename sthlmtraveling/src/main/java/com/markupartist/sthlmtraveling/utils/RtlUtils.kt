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

import java.util.Locale

// Based on http://stackoverflow.com/questions/18996183/identifyng-rtl-language-in-android
object RtlUtils {
    private val RTL_LANGUAGES = setOf(
        "ar", // Arabic
        "dv", // Divehi
        "fa", // Persian (Farsi)
        "ha", // Hausa
        "he", // Hebrew
        "iw", // Hebrew (old code)
        "ji", // Yiddish (old code)
        "ps", // Pashto, Pushto
        "ur", // Urdu
        "yi"  // Yiddish
    )

    @JvmStatic
    fun isRtl(locale: Locale?): Boolean {
        return locale != null && RTL_LANGUAGES.contains(locale.language)
    }

    @JvmStatic
    fun isRtl(s: CharSequence?): Boolean {
        if (s.isNullOrEmpty()) {
            return false
        }
        return when (Character.getDirectionality(s[0]).toInt()) {
            Character.DIRECTIONALITY_RIGHT_TO_LEFT.toInt(),
            Character.DIRECTIONALITY_RIGHT_TO_LEFT_ARABIC.toInt() -> true
            else -> false
        }
    }
}

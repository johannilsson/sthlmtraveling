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

package com.markupartist.sthlmtraveling.utils;

import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

// Based on http://stackoverflow.com/questions/18996183/identifyng-rtl-language-in-android
public class RtlUtils {

    private static final Set<String> RTL_LANGUAGES;

    static {
        Set<String> lang = new HashSet<String>();
        lang.add("ar"); // Arabic
        lang.add("dv"); // Divehi
        lang.add("fa"); // Persian (Farsi)
        lang.add("ha"); // Hausa
        lang.add("he"); // Hebrew
        lang.add("iw"); // Hebrew (old code)
        lang.add("ji"); // Yiddish (old code)
        lang.add("ps"); // Pashto, Pushto
        lang.add("ur"); // Urdu
        lang.add("yi"); // Yiddish
        RTL_LANGUAGES = Collections.unmodifiableSet(lang);
    }

    public static boolean isRtl(Locale locale) {
        return locale != null && RTL_LANGUAGES.contains(locale.getLanguage());
    }
}
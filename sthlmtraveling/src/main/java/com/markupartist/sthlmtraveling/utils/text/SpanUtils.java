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

package com.markupartist.sthlmtraveling.utils.text;

import android.content.Context;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.CharacterStyle;
import android.text.style.ReplacementSpan;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class SpanUtils {
    private SpanUtils() {
    }

    public static CharSequence createSpannable(CharSequence source, Pattern pattern, ReplacementSpan style) {
        SpannableStringBuilder sb = new SpannableStringBuilder(source);

        Matcher matcher = pattern.matcher(source);
        while (matcher.find()) {
            int start = matcher.start();
            int end = matcher.end();
            sb.setSpan(style, start, end, Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
        }

        return sb;
    }

    public static CharSequence createSpannable(Context context, int stringId, Pattern pattern, CharacterStyle... styles) {
        String string = context.getString(stringId);
        return createSpannable(string, pattern, styles);
    }

    public static CharSequence createSpannable(CharSequence source, Pattern pattern, CharacterStyle... styles) {
        SpannableStringBuilder spannableStringBuilder = new SpannableStringBuilder(source);
        Matcher matcher = pattern.matcher(source);
        while (matcher.find()) {
            int start = matcher.start();
            int end = matcher.end();
            applyStylesToSpannable(spannableStringBuilder, start, end, styles);
        }
        return spannableStringBuilder;
    }

    private static SpannableStringBuilder applyStylesToSpannable(SpannableStringBuilder source, int start, int end, CharacterStyle... styles) {
        for (CharacterStyle style : styles) {
            source.setSpan(CharacterStyle.wrap(style), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        return source;
    }
}
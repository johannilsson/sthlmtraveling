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
package com.markupartist.sthlmtraveling.utils.text

import android.content.Context
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.CharacterStyle
import android.text.style.ReplacementSpan
import java.util.regex.Pattern

object SpanUtils {
    @JvmStatic
    fun createSpannable(source: CharSequence, style: ReplacementSpan): CharSequence {
        val sb = SpannableStringBuilder(source)
        sb.setSpan(style, 0, source.length, Spanned.SPAN_INCLUSIVE_EXCLUSIVE)
        return sb
    }

    @JvmStatic
    fun createSpannable(
        source: CharSequence,
        pattern: Pattern,
        style: ReplacementSpan?
    ): CharSequence {
        val sb = SpannableStringBuilder(source)

        val matcher = pattern.matcher(source)
        while (matcher.find()) {
            val start = matcher.start()
            val end = matcher.end()
            sb.setSpan(style, start, end, Spanned.SPAN_INCLUSIVE_EXCLUSIVE)
        }

        return sb
    }

    fun createSpannable(
        context: Context,
        stringId: Int,
        pattern: Pattern,
        vararg styles: CharacterStyle?
    ): CharSequence {
        val string = context.getString(stringId)
        return createSpannable(string, pattern, *styles)
    }

    fun createSpannable(
        source: CharSequence,
        pattern: Pattern,
        vararg styles: CharacterStyle?
    ): CharSequence {
        val spannableStringBuilder = SpannableStringBuilder(source)
        val matcher = pattern.matcher(source)
        while (matcher.find()) {
            val start = matcher.start()
            val end = matcher.end()
            applyStylesToSpannable(spannableStringBuilder, start, end, *styles)
        }
        return spannableStringBuilder
    }

    private fun applyStylesToSpannable(
        source: SpannableStringBuilder,
        start: Int,
        end: Int,
        vararg styles: CharacterStyle?
    ): SpannableStringBuilder {
        for (style in styles) {
            source.setSpan(CharacterStyle.wrap(style), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        return source
    }
}
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

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Paint.FontMetricsInt
import android.graphics.RectF
import android.text.style.ReplacementSpan
import androidx.annotation.ColorInt

class RoundedBackgroundSpan(
    @field:ColorInt @param:ColorInt private val backgroundColor: Int,
    @field:ColorInt @param:ColorInt private val textColor: Int,
    private val padding: Int
) : ReplacementSpan() {

    override fun getSize(
        paint: Paint,
        text: CharSequence,
        start: Int,
        end: Int,
        fm: FontMetricsInt?
    ): Int {
        return paint.measureText(text.subSequence(start, end).toString()).toInt() + padding * 2
    }

    override fun draw(
        canvas: Canvas, text: CharSequence,
        start: Int, end: Int, x: Float, top: Int, y: Int, bottom: Int, paint: Paint
    ) {
        val width = paint.measureText(text.subSequence(start, end).toString())

        val newTop = (bottom - paint.textSize - paint.descent()).toInt()
        val rect = RectF(x, newTop.toFloat(), x + width + padding * 2, bottom.toFloat())
        paint.color = backgroundColor
        canvas.drawRoundRect(rect, padding.toFloat(), padding.toFloat(), paint)
        paint.color = textColor
        paint.isFakeBoldText = true

        val textY = bottom - paint.descent()
        canvas.drawText(text, start, end, x + padding, textY, paint)
    }
}

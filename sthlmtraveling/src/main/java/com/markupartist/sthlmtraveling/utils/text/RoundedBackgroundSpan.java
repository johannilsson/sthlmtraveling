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

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import androidx.annotation.ColorInt;
import android.text.style.ReplacementSpan;

public class RoundedBackgroundSpan extends ReplacementSpan {
    private int mPadding = 0;
    @ColorInt
    private int mBackgroundColor;
    @ColorInt
    private int mTextColor;

    public RoundedBackgroundSpan(@ColorInt int backgroundColor, @ColorInt int textColor, int  padding) {
        super();
        mBackgroundColor = backgroundColor;
        mTextColor = textColor;
        mPadding = padding;
    }

    @Override
    public int getSize(Paint paint, CharSequence text, int start, int end, Paint.FontMetricsInt fm) {
        return (int) paint.measureText(text.subSequence(start, end).toString()) + mPadding * 2;
    }

    @Override
    public void draw(Canvas canvas, CharSequence text,
                     int start, int end, float x, int top, int y, int bottom, Paint paint) {
        float width = paint.measureText(text.subSequence(start, end).toString());

        int newTop = (int) (bottom - paint.getTextSize() - paint.descent());
        RectF rect = new RectF(x, newTop, x + width + mPadding * 2, bottom);
        paint.setColor(mBackgroundColor);
        canvas.drawRoundRect(rect, mPadding, mPadding, paint);
        paint.setColor(mTextColor);
        paint.setFakeBoldText(true);

        float textY = bottom - paint.descent();
        canvas.drawText(text, start, end, x + mPadding, textY, paint);
    }
}

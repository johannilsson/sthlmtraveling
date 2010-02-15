/*
 * SL helps you search for trips with SL, Stockholm's public transport
 * company.  Note that the application is in Swedish.
 *
 *  Copyright (C) 2009  Johan Walles, johan.walles@gmail.com
 *  Copyright (C) 2009  Johan Nilsson, johan@markupartist.com
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.markupartist.sthlmtraveling.graphics;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.util.DisplayMetrics;

/**
 * Represent a marker with a label inside.
 * 
 * Marker logic written by Johan Walles. Wrapped in LabelMarker and small 
 * modifications by Johan Nilsson.
 */
public class LabelMarker {
    private String mLabel;
    private float mTextSize;
    private BitmapDrawable mMarker;

    /**
     * Constructs a new LabelMarker.
     * @param label the label
     * @param textSize the text size
     */
    public LabelMarker(String label, float textSize) {
        mLabel = label;
        mTextSize = textSize;
    }

    /**
     * Get the marker.
     * @return the marker
     */
    public BitmapDrawable getMarker() {
        if (mMarker == null) {
            mMarker = create();
        }
        return mMarker;
    }

    /**
     * Creates a {@link BitmapDrawable}.
     * @author johan.walles@gmail.com
     * @return the bitmap
     */
    public BitmapDrawable create() {
        // Measure the title size in pixels

        Paint paint = new Paint();
        paint.setTextSize(mTextSize);
        Rect textSize = new Rect();
        paint.getTextBounds(mLabel, 0, mLabel.length(), textSize);

        // Create a bitmap to draw on
        int arrowSize = 7;

        // Width of the black bubble outline
        int edgeWidth = 1;

        // The bubble's interior margin
        int mariginWidth = 10;
        Bitmap bitmap =
            Bitmap.createBitmap(
                textSize.width() + 2 * edgeWidth + 2 * mariginWidth,
                textSize.height() + 2 * edgeWidth + 2 * mariginWidth + arrowSize,
                Bitmap.Config.ARGB_4444);
        paint.setAntiAlias(true);

        // Create the bubble outline
        int top = edgeWidth;
        int left = edgeWidth;
        int bottom = edgeWidth + 2 * mariginWidth + textSize.height();
        int right = edgeWidth + 2 * mariginWidth + textSize.width();
        int arrowLeft = bitmap.getWidth() / 3;
        int arrowRight = arrowLeft + arrowSize;
        int arrowMiddle = (arrowLeft + arrowRight) / 2;
        int arrowBottom = bottom + arrowSize;
        Path bubblePath = new Path();
        bubblePath.moveTo(left + mariginWidth / 2, top);

        // Top + right arc
        bubblePath.arcTo(
            new RectF(right - mariginWidth, top, right, top + mariginWidth),
            270, 90);

        // Right + bottom arc
        bubblePath.arcTo(
            new RectF(right - mariginWidth, bottom - mariginWidth, right, bottom),
            0, 90);

        // Arrow
        bubblePath.lineTo(arrowRight, bottom);
        bubblePath.lineTo(arrowMiddle, arrowBottom);
        bubblePath.lineTo(arrowLeft, bottom);

        // Bottom + left arc
        bubblePath.arcTo(
            new RectF(left, bottom - mariginWidth, left + mariginWidth, bottom),
            90, 90);

        // Left + top arc
        bubblePath.arcTo(
            new RectF(left, top, left + mariginWidth, top + mariginWidth),
            180, 90);

        // Draw a white background
        Canvas canvas = new Canvas(bitmap);
        paint.setColor(0xffffffff);
        paint.setStyle(Paint.Style.FILL);
        canvas.drawPath(bubblePath, paint);

        // Stroke the background edge with black
        paint.setColor(0xff000000);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(edgeWidth);
        canvas.drawPath(bubblePath, paint);

        // Draw the text on the background
        paint.setStrokeWidth(0);
        canvas.drawText(mLabel,
            left + mariginWidth,
            left + mariginWidth - paint.getFontMetricsInt().ascent - 1,
            paint);

        // Align the marker so that the hot spot is at the pointer tip
        BitmapDrawable marker = new BitmapDrawable(bitmap);
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int hotX = arrowMiddle;
        int hotY = bitmap.getHeight();
        marker.setBounds(-hotX, -hotY, width - hotX, height - hotY);

        return marker;
    }
}

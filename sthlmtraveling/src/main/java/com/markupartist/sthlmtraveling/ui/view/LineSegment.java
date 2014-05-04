/*
 * Copyright (C) 2009-2014 Johan Nilsson <http://markupartist.com>
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

package com.markupartist.sthlmtraveling.ui.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

/**
 * Created by johan on 5/1/14.
 */
public class LineSegment extends View {
    private int mWidth;
    private int mHeight;

    public LineSegment(Context context) {
        super(context);
        init();
    }

    public LineSegment(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {

    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        mWidth = w;
        mHeight = h;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float width = (float) getWidth();
        float height = (float) getHeight();

        float radius;

        if (width > height) {
            radius = height / 3;
        } else {
            radius = width / 3;
        }

        Paint fillPaint = new Paint();
        fillPaint.setColor(Color.RED);
        fillPaint.setStrokeWidth(5);
        fillPaint.setStyle(Paint.Style.FILL);
        fillPaint.setAntiAlias(true);

        Paint fillPaint2 = new Paint();
        fillPaint2.setColor(Color.BLACK);
        fillPaint2.setStrokeWidth(5);
        fillPaint2.setStyle(Paint.Style.FILL);
        fillPaint2.setAntiAlias(true);

        float center_x, center_y;
        center_x = width / 2;
        center_y = height / 2;

//        final RectF oval = new RectF();
//        oval.set(center_x - radius, center_y - radius, center_x + radius, center_y + radius);

        // canvas.drawArc(oval, 90, 270, true, paint);
        //canvas.drawArc(oval, mStartAngle, mAngle, true, paint);

        canvas.drawRect(width / 2, 0, 10, height, fillPaint2);
        canvas.drawCircle(center_x, center_y, radius, fillPaint);

        //canvas.drawArc(oval, 0, 360, true, strokePaint);
    }
}

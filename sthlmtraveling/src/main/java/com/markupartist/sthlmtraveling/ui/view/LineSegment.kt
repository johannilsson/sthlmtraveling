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
package com.markupartist.sthlmtraveling.ui.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View

/**
 * Created by johan on 5/1/14.
 */
class LineSegment : View {
    private var mWidth = 0
    private var mHeight = 0

    constructor(context: Context?) : super(context) {
        init()
    }

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {
        init()
    }

    private fun init() {
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        mWidth = w
        mHeight = h
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val width = getWidth().toFloat()
        val height = getHeight().toFloat()

        val radius: Float

        if (width > height) {
            radius = height / 3
        } else {
            radius = width / 3
        }

        val fillPaint = Paint()
        fillPaint.setColor(Color.RED)
        fillPaint.setStrokeWidth(5f)
        fillPaint.setStyle(Paint.Style.FILL)
        fillPaint.setAntiAlias(true)

        val fillPaint2 = Paint()
        fillPaint2.setColor(Color.BLACK)
        fillPaint2.setStrokeWidth(5f)
        fillPaint2.setStyle(Paint.Style.FILL)
        fillPaint2.setAntiAlias(true)

        val center_x: Float
        val center_y: Float
        center_x = width / 2
        center_y = height / 2

        //        final RectF oval = new RectF();
//        oval.set(center_x - radius, center_y - radius, center_x + radius, center_y + radius);

        // canvas.drawArc(oval, 90, 270, true, paint);
        //canvas.drawArc(oval, mStartAngle, mAngle, true, paint);
        canvas.drawRect(width / 2, 0f, 10f, height, fillPaint2)
        canvas.drawCircle(center_x, center_y, radius, fillPaint)

        //canvas.drawArc(oval, 0, 360, true, strokePaint);
    }
}

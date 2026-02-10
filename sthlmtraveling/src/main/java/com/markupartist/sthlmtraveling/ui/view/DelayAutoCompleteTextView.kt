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
import android.os.Handler
import android.os.Message
import android.util.AttributeSet
import android.widget.ProgressBar
import androidx.appcompat.widget.AppCompatAutoCompleteTextView

/**
 * Created by johan on 27/10/14.
 */
class DelayAutoCompleteTextView(context: Context, attrs: AttributeSet?) :
    AppCompatAutoCompleteTextView(context, attrs) {
    private var mAutoCompleteDelay: Int = DEFAULT_AUTO_COMPLETE_DELAY
    private var mLoadingIndicator: ProgressBar? = null

    private val mHandler: Handler = object : Handler() {
        override fun handleMessage(msg: Message) {
            super@DelayAutoCompleteTextView.performFiltering(msg.obj as CharSequence?, msg.arg1)
        }
    }

    fun setProgressBar(progressBar: ProgressBar?) {
        mLoadingIndicator = progressBar
    }

    fun setFilterDelay(autoCompleteDelay: Int) {
        mAutoCompleteDelay = autoCompleteDelay
    }

    override fun performFiltering(text: CharSequence?, keyCode: Int) {
        if (mLoadingIndicator != null) {
            mLoadingIndicator!!.setVisibility(VISIBLE)
        }
        mHandler.removeMessages(MESSAGE_TEXT_CHANGED)
        mHandler.sendMessageDelayed(
            mHandler.obtainMessage(MESSAGE_TEXT_CHANGED, text),
            mAutoCompleteDelay.toLong()
        )
    }

    override fun onFilterComplete(count: Int) {
        if (mLoadingIndicator != null) {
            mLoadingIndicator!!.setVisibility(GONE)
        }
        super.onFilterComplete(count)
    }

    companion object {
        private const val MESSAGE_TEXT_CHANGED = 100
        private const val DEFAULT_AUTO_COMPLETE_DELAY = 600
    }
}
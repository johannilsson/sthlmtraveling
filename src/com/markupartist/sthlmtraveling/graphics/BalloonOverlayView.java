/***
 * Copyright (c) 2010 readyState Software Ltd
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License. You may obtain
 * a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 */

package com.markupartist.sthlmtraveling.graphics;

import com.markupartist.sthlmtraveling.R;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * A view representing a MapView marker information balloon.
 *
 * This class has a number of Android resource dependencies:
 * 
 * <ul>
 * <li>drawable/balloon_overlay_bg_selector.xml</li>
 * <li>drawable/balloon_overlay_focused.9.png</li>
 * <li>drawable/balloon_overlay_unfocused.9.png</li>
 * <li>layout/balloon_map_overlay.xml</li>
 * </ul>
 * 
 * Modified by Johan Nilsson.
 * 
 * <ul>
 * <li>Added bindTouchListener</li>
 * <li>Removed close button</li>
 * <li>Removed snippet</li>
 * <li>Added a OnTapBallonListener</li>
 * </ul>
 * @author Jeff Gilfelt
 * @author Johan Nilsson
 *
 */
public class BalloonOverlayView extends FrameLayout {
	private LinearLayout mLayout;
	private TextView mTitle;
	private OnTapBallonListener mOnTapBalloonListener;

	/**
	 * Create a new BalloonOverlayView.
	 * 
	 * @param context - The activity context.
	 * @param balloonBottomOffset - The bottom padding (in pixels) to be applied
	 * when rendering this view.
	 */
	public BalloonOverlayView(Context context, int balloonBottomOffset) {

		super(context);

		setPadding(10, 0, 10, balloonBottomOffset);
		mLayout = new LinearLayout(context);
		mLayout.setVisibility(VISIBLE);

		LayoutInflater inflater = (LayoutInflater) context
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View v = inflater.inflate(R.layout.balloon_map_overlay, mLayout);
		mTitle = (TextView) v.findViewById(R.id.balloon_item_title);

		FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
				LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		params.gravity = Gravity.NO_GRAVITY;

		addView(mLayout, params);

		bindTouchListener();
	}

	public void setOnTapBalloonListener(OnTapBallonListener listener) {
	    mOnTapBalloonListener = listener;
	}

	/**
	 * Sets the view data.
	 * 
	 * @param title the title 
	 * @param snippet the snippet
	 */
	public void setLabel(String title) {
		mLayout.setVisibility(VISIBLE);
		if (title != null) {
			mTitle.setVisibility(VISIBLE);
			mTitle.setText(title);
		} else {
			mTitle.setVisibility(GONE);
		}
	}

	private void bindTouchListener() {
	    View clickRegion = (View) findViewById(R.id.balloon_inner_layout);
	    clickRegion.setOnTouchListener(new OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                // If there's no listener no need to check for touch events.
                if (mOnTapBalloonListener == null) {
                    return false;
                }
                
                View l =  ((View) v.getParent()).findViewById(R.id.balloon_main_layout);
                Drawable d = l.getBackground();
                
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    int[] states = {android.R.attr.state_pressed};
                    if (d.setState(states)) {
                        d.invalidateSelf();
                    }
                    return true;
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    int newStates[] = {};
                    if (d.setState(newStates)) {
                        d.invalidateSelf();
                    }
                    onBallonTap();
                    return true;
                } else {
                    return false;
                }
            }
        });
	}

	private void onBallonTap() {
	    if (mOnTapBalloonListener != null) {
	        mOnTapBalloonListener.onTap();
	    }
	}

	public interface OnTapBallonListener {
	    public void onTap();
	}
}

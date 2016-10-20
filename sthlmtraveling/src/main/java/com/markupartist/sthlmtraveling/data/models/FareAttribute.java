/*
 * Copyright (C) 2009-2016 Johan Nilsson <http://markupartist.com>
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

package com.markupartist.sthlmtraveling.data.models;

import android.os.Parcel;
import android.os.Parcelable;

/**
 *
 */
public class FareAttribute implements Parcelable {
    private final boolean reduced;
    private final String text;
    private final String action;

    public FareAttribute(String action, boolean reduced, String text) {
        this.action = action;
        this.reduced = reduced;
        this.text = text;
    }

    protected FareAttribute(Parcel in) {
        reduced = in.readByte() != 0;
        text = in.readString();
        action = in.readString();
    }

    public static final Creator<FareAttribute> CREATOR = new Creator<FareAttribute>() {
        @Override
        public FareAttribute createFromParcel(Parcel in) {
            return new FareAttribute(in);
        }

        @Override
        public FareAttribute[] newArray(int size) {
            return new FareAttribute[size];
        }
    };

    public String getAction() {
        return action;
    }

    public boolean isReduced() {
        return reduced;
    }

    public String getText() {
        return text;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeByte((byte) (reduced ? 1 : 0));
        dest.writeString(text);
        dest.writeString(action);
    }
}

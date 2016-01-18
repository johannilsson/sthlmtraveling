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
public class RouteError implements Parcelable {
    private final String mode;
    private final String code;
    private final String description;

    public RouteError(String code, String mode, String description) {
        this.code = code;
        this.mode = mode;
        this.description = description;
    }

    protected RouteError(Parcel in) {
        mode = in.readString();
        code = in.readString();
        description = in.readString();
    }

    public static final Creator<RouteError> CREATOR = new Creator<RouteError>() {
        @Override
        public RouteError createFromParcel(Parcel in) {
            return new RouteError(in);
        }

        @Override
        public RouteError[] newArray(int size) {
            return new RouteError[size];
        }
    };

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public String getMode() {
        return mode;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mode);
        dest.writeString(code);
        dest.writeString(description);
    }
}

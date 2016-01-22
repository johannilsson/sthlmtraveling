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

import java.util.Collections;
import java.util.List;

/**
 *
 */
public class IntermediateResponse {
    private final List<IntermediateStops> result;

    public IntermediateResponse(List<IntermediateStops> result) {
        this.result = result;
    }

    public List<IntermediateStops> getResult() {
        return result;
    }

    public List<IntermediateStop> getStops(String reference) {
        if (result == null || result.isEmpty() || reference == null) {
            return Collections.emptyList();
        }
        for (IntermediateStops is : result) {
            if (reference.equals(is.getReference())) {
                return is.getStops();
            }
        }
        return Collections.emptyList();
    }
}

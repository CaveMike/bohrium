/*
 * Copyright (C) 2008 The Android Open Source Project
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
package com.mikecorrigan.bohrium.pubsub;

import android.os.Bundle;

import com.mikecorrigan.bohrium.common.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;

public class JSONCodec {
    private static final String TAG = JSONCodec.class.getSimpleName();

    public static JSONObject toJson(final Bundle bundle) {
        Log.v(TAG, "toJson");

        JSONObject j = new JSONObject();
        try {
            for (final String key : bundle.keySet()) {
                final Object value = bundle.get(key);
                Log.v(TAG, "key=" + key + ", value=" + value.toString());
                j.put(key, value.toString());
            }

            return j;
        } catch (JSONException e) {
            Log.e(TAG, "exception=" + e);
            Log.e(TAG, Log.getStackTraceString(e));
        }

        return null;
    }

    public static Bundle fromJson(JSONObject j) {
        Log.v(TAG, "fromJson");

        try {
            Bundle bundle = new Bundle();

            Iterator<String> i = j.keys();
            while (i.hasNext()) {
                final String key = i.next();
                final String value = j.getString(key);
                Log.v(TAG, "key=" + key + ", value=" + value);
                bundle.putString(key, value);
            }

            return bundle;
        } catch (JSONException e) {
            Log.e(TAG, "exception=" + e);
            Log.e(TAG, Log.getStackTraceString(e));
        }

        return null;
    }
}

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

import com.mikecorrigan.bohrium.common.Log;

import org.apache.http.cookie.Cookie;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class JSONTransaction extends Transaction {
    private static final String TAG = JSONTransaction.class.getSimpleName();

    public JSONTransaction(final String baseUriString, final Cookie authCookie, final String method, final String uriString, final JSONObject requestBody) {
        super(baseUriString, authCookie, method, uriString, (Object) requestBody);
    }

    protected String getMimeType() {
        return "application/json";
    }

    protected String encode(final Object object) {
        JSONObject jsonObject = (JSONObject) object;
        return jsonObject.toString();
    }

    protected Object decode(final String string) {
        try {
            if (string.charAt(0) == '[') {
                return new JSONArray(string);
            } else {
                return new JSONObject(string);
            }

        } catch (JSONException e) {
            Log.e(TAG, "Exception " + e);
            Log.e(TAG, Log.getStackTraceString(e));
            return null;
        }
    }
}

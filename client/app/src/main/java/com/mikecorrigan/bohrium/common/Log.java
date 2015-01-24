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
package com.mikecorrigan.bohrium.common;

public class Log {
    private static final String TAG = "bohrium";

    public static void e(final String tag, final String message) {
        android.util.Log.e(TAG + "." + tag, message);
    }

    public static void w(final String tag, final String message) {
        android.util.Log.w(TAG + "." + tag, message);
    }

    public static void i(final String tag, final String message) {
        android.util.Log.i(TAG + "." + tag, message);
    }

    public static void d(final String tag, final String message) {
        android.util.Log.d(TAG + "." + tag, message);
    }

    public static void v(final String tag, final String message) {
        android.util.Log.v(TAG + "." + tag, message);
    }

    public static void vc(final boolean enabled, final String tag, final String message) {
        if (enabled) {
            android.util.Log.v(TAG + "." + tag, message);
        }
    }

    public static void wtf(final String tag, final String message) {
        android.util.Log.wtf(TAG + "." + tag, message);
    }

    public static String getStackTraceString(Throwable th) {
        return android.util.Log.getStackTraceString(th);
    }
}

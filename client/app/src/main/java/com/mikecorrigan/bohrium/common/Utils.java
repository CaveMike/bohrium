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

import android.content.SharedPreferences;
import android.os.Bundle;

import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class Utils {
    //private static final String TAG = "." + Utils.class.getSimpleName();

    public static final String bundleToString(final Bundle bundle) {
        if (bundle == null) {
            return "<invalid bundle>";
        }

        StringBuilder sb = new StringBuilder();

        Set<String> keys = bundle.keySet();
        if (keys == null) {
            return "<invalid bundle>";
        }

        for (final String key : keys) {
            sb.append(key + "=" + bundle.get(key) + ", ");
        }

        return sb.toString();
    }


    public static void prefsToBundle(final SharedPreferences prefs, Bundle bundle) {
        final Map<String, ?> map = prefs.getAll();
        for (final String key : map.keySet()) {
            Object o = map.get(key);
            if (o instanceof String) {
                bundle.putString(key, (String) o);
            } else if (o instanceof Long) {
                bundle.putLong(key, (Long) o);
            }
        }
    }

    public static void bundleToPrefsEditor(final Bundle bundle, SharedPreferences.Editor editor) {
        for (final String key : bundle.keySet()) {
            Object o = bundle.get(key);
            if (o instanceof String) {
                editor.putString(key, (String) o);
            } else if (o instanceof Long) {
                editor.putLong(key, (Long) o);
            }
        }
    }

    public static boolean sleep(long timeInMs) {
        try {
            Thread.sleep(timeInMs);
            return false;
        } catch (InterruptedException e) {
            return true;
        }
    }

    public static Locale stringToLocale(final String localeString) {
        String[] parts = localeString.split("_");
        if (parts.length == 1) {
            return new Locale(parts[0]);
        } else if (parts.length == 2) {
            return new Locale(parts[0], parts[1]);
        } else if (parts.length == 3) {
            return new Locale(parts[0], parts[1], parts[2]);
        }

        return Locale.getDefault();
    }
}

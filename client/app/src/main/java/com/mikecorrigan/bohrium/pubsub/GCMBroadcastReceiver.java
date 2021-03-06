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

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.support.v4.content.WakefulBroadcastReceiver;

import com.mikecorrigan.bohrium.common.Log;
import com.mikecorrigan.bohrium.common.Utils;

public class GCMBroadcastReceiver extends WakefulBroadcastReceiver {
    private static final String TAG = GCMBroadcastReceiver.class.getSimpleName();

    @Override
    public final void onReceive(Context context, Intent intent) {
        Log.d(TAG, "onReceive: context=" + context + ", intent=" + intent + ", extras: " + Utils.bundleToString(intent.getExtras()));

        intent.setClass(context, RegistrationClient.class);
        startWakefulService(context, intent);

        setResult(Activity.RESULT_OK, null, null); // TODO: Is this needed?
    }
}

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

import android.content.Context;
import android.os.Bundle;
import android.provider.Settings;

import com.mikecorrigan.bohrium.common.Log;
import com.mikecorrigan.bohrium.common.Utils;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class PubSubClient implements RegistrationClient.IRegistrationStrategy {
    private static final String TAG = PubSubClient.class.getSimpleName();

    public static final String SERVICE_DEVICE = "device";
    public static final String SERVICE_USER = "user";
    public static final String SERVICE_PUBLISH = "publication";
    public static final String SERVICE_SUBSCRIBE = "subscription";

    // Android C2DM device type.
    private static final String C2DM_DEVICE_TYPE = "ac2dm";
    private static final String GCM_DEVICE_TYPE = "gcm";

    private final ITransactionService mTransactionService;
    private final String mDevId;

    private final Map<String, Service> mServices = new HashMap<>();

    private Bundle mDevice;
    private Bundle mUser;

    public PubSubClient(final ITransactionService transactionService, final Context context) {
        super();
        Log.v(TAG, "ctor");

        mTransactionService = transactionService;
        mDevId = getDevId(context);

        mServices.put(SERVICE_DEVICE, new Service(transactionService, "device", "dev_id"));
        mServices.put(SERVICE_USER, new Service(transactionService, "user", "user_id"));
        mServices.put(SERVICE_PUBLISH, new Service(transactionService, "publication", "pub_id"));
        mServices.put(SERVICE_SUBSCRIBE, new Service(transactionService, "subscription", "sub_id"));
    }

    public String getDevId() {
        return mDevId;
    }

    public Bundle getDevice() {
        return mDevice;
    }

    public Bundle getUser() {
        return mUser;
    }

    public Service getService(final String serviceName) {
        return mServices.get(serviceName);
    }

    private static String getDevId(final Context context) {
        if (context == null) {
            Log.e(TAG, "getDevId: invalid context");
            return null;
        }

        return Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
    }

    @Override
    public boolean register(final Bundle bundle) {
        Log.v(TAG, "register: bundle=" + Utils.bundleToString(bundle));

        // Create JSON request body.
        JSONObject j = new JSONObject();
        try {
            j.put("name", bundle.getString(RegistrationClient.APP_NAME));
            j.put("resource", bundle.getString(RegistrationClient.RESOURCE));
            j.put("type", GCM_DEVICE_TYPE);
            j.put("reg_id", bundle.getString(RegistrationClient.REG_ID));
            j.put("dev_id", mDevId);
        } catch (JSONException e) {
            Log.w(TAG, "Registration failure: URI failed=" + e);
            return false;
        }

        ITransaction transaction = mTransactionService.createTransaction("PUT", "device/" + mDevId + "/", j);
        transaction.run();
        if (transaction.getStatusCode() == 200) {
            // Save user ID.
            try {
                JSONObject responseBody = (JSONObject) transaction.getResponseBody();
                mDevice = JSONCodec.fromJson(responseBody);

                mUser = new Bundle();
                mUser.putString("user_id", responseBody.getString("user_id"));

            } catch (JSONException e) {
                Log.e(TAG, "Exception " + e);
                Log.e(TAG, Log.getStackTraceString(e));
            }
        }

        Log.e(TAG, "onRegisterComplete: statusCode=" + transaction.getStatusCode());
        return transaction.getStatusCode() == 200;
    }

    @Override
    public boolean unregister(final Bundle bundle) {
        Log.v(TAG, "unregister: bundle=" + Utils.bundleToString(bundle));

        ITransaction transaction = mTransactionService.createTransaction("DELETE", "device/" + mDevId + "/", null);
        transaction.run();

        Log.e(TAG, "onUnregisterComplete: statusCode=" + transaction.getStatusCode());
        return transaction.getStatusCode() == 200;
    }
}

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
import com.mikecorrigan.bohrium.common.Utils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.LinkedList;
import java.util.List;

class Adapter {
    private static final String TAG = Adapter.class.getSimpleName();

    private final ITransactionService mTransactionService;

    public Adapter(final ITransactionService transactionService) {
        Log.v(TAG, "ctor: server=" + transactionService);
        mTransactionService = transactionService;
    }

    public Bundle create(final String uriString, final Bundle bundle) {
        Log.v(TAG, "create: bundle=" + Utils.bundleToString(bundle));

        JSONObject j = JSONCodec.toJson(bundle);
        Log.v(TAG, "json=" + j);

        ITransaction transaction = mTransactionService.createTransaction("POST", uriString, j);
        mTransactionService.completeTransaction(transaction);

        if (transaction.getStatusCode() == 200) {
            JSONObject jsonBody = (JSONObject) transaction.getResponseBody();
            Bundle responseBundle = JSONCodec.fromJson(jsonBody);
            Log.v(TAG, "responseBundle=" + Utils.bundleToString(responseBundle));
            return responseBundle;
        }

        return null;
    }

    public Bundle createChild(final String parentKey, final Bundle bundle) {
        Log.v(TAG, "createChild: parentKey=" + parentKey + ", bundle=" + Utils.bundleToString(bundle));

        // TODO: implement

        return null;
    }

    public Bundle read(final String uriString) {
        Log.v(TAG, "read: uri=" + uriString);

        ITransaction transaction = mTransactionService.createTransaction("GET", uriString, null);
        mTransactionService.completeTransaction(transaction);

        if (transaction.getStatusCode() == 200) {
            JSONObject jsonBody = (JSONObject) transaction.getResponseBody();
            Bundle bundle = JSONCodec.fromJson(jsonBody);
            Log.v(TAG, "bundle=" + Utils.bundleToString(bundle));
            return bundle;
        }

        return null;
    }

    public List<Bundle> readAll(final String uriString) {
        Log.v(TAG, "readAll: uri=" + uriString);

        ITransaction transaction = mTransactionService.createTransaction("GET", uriString, null);
        mTransactionService.completeTransaction(transaction);

        try {
            if (transaction.getStatusCode() == 200) {
                List<Bundle> bundles = new LinkedList<>();

                JSONArray jsonBody = (JSONArray) transaction.getResponseBody();
                for (int i = 0; i < jsonBody.length(); i++) {
                    JSONObject jsonObj = (JSONObject) jsonBody.get(i);

                    Bundle bundle = JSONCodec.fromJson(jsonObj);
                    Log.v(TAG, "bundle=" + Utils.bundleToString(bundle));

                    bundles.add(bundle);
                }

                Log.v(TAG, "bundles=" + bundles);
                return bundles;
            }
        } catch (JSONException e) {
            Log.e(TAG, "exception=" + e);
            Log.e(TAG, Log.getStackTraceString(e));
        }

        return null;
    }

    public void update(final String uriString, final Bundle bundle) {
        Log.v(TAG, "update: uri=" + uriString + ", bundle=" + Utils.bundleToString(bundle));

        JSONObject j = JSONCodec.toJson(bundle);
        Log.v(TAG, "json=" + j);

        ITransaction transaction = mTransactionService.createTransaction("PUT", uriString, j);
        mTransactionService.completeTransaction(transaction);
    }

    public void updateAll(final String uriString, List<Bundle> bundles) {
        Log.v(TAG, "updateAll: uri=" + uriString + ", bundles=" + bundles);

        // TODO: Implement
    }

    public void delete(final String uriString) {
        Log.v(TAG, "delete: uri=" + uriString);

        ITransaction transaction = mTransactionService.createTransaction("DELETE", uriString, null);
        mTransactionService.completeTransaction(transaction);
    }
}

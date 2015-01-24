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

import java.util.List;

public class Service {
    private static final String TAG = Adapter.class.getSimpleName();

    private final String URI;
    private final String KEY_NAME;

    private final Adapter mAdapter;

    public Service(final ITransactionService server, final String uri, final String keyName) {
        Log.v(TAG, "ctor: server=" + server + ", uri=" + uri + ", keyName=" + keyName);

        URI = uri;
        KEY_NAME = keyName;

        mAdapter = new Adapter(server);
    }

    public Bundle create(final Bundle bundle) {
        Log.v(TAG, "create: bundle=" + Utils.bundleToString(bundle));
        return mAdapter.create(URI + "/", bundle);
    }

    public Bundle createChild(final String parentKey, final Bundle bundle) {
        Log.v(TAG, "createChild: parentKey=" + parentKey + ", bundle=" + Utils.bundleToString(bundle));
        return mAdapter.createChild(parentKey, bundle);
    }

    public Bundle read(final String key) {
        Log.v(TAG, "read: key=" + key);
        return mAdapter.read(URI + "/" + key + "/");
    }

    public List<Bundle> readAll() {
        Log.v(TAG, "readAll");
        return mAdapter.readAll(URI + "/");
    }

    public void update(final Bundle bundle) {
        Log.v(TAG, "update: bundle=" + Utils.bundleToString(bundle));
        mAdapter.update(URI + "/" + bundle.get(KEY_NAME) + "/", bundle);
    }

    public void updateAll(List<Bundle> bundles) {
        Log.v(TAG, "updateAll: bundles=" + bundles);
        mAdapter.updateAll(URI + "/", bundles);
    }

    public void delete(final Bundle bundle) {
        Log.v(TAG, "delete: bundle=" + Utils.bundleToString(bundle));
        delete(bundle.getString(KEY_NAME));
    }

    public void delete(final String key) {
        Log.v(TAG, "delete: key=" + key);
        mAdapter.delete(URI + "/" + key + "/");
    }

    public void deleteAll() {
        Log.v(TAG, "deleteAll");
        mAdapter.delete(URI + "/");
    }

    public Bundle message(final Bundle bundle) {
        Log.v(TAG, "message: bundle=" + Utils.bundleToString(bundle));
        return mAdapter.create(URI + "/" + bundle.get(KEY_NAME) + "/message/", bundle);
    }
}


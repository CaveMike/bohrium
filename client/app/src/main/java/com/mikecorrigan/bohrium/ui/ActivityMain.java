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
package com.mikecorrigan.bohrium.ui;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.mikecorrigan.bohrium.R;
import com.mikecorrigan.bohrium.common.Log;
import com.mikecorrigan.bohrium.common.Utils;
import com.mikecorrigan.bohrium.pubsub.PubSubClient;
import com.mikecorrigan.bohrium.pubsub.RegistrationClient;
import com.mikecorrigan.bohrium.pubsub.Service;

import java.util.List;

public class ActivityMain extends Activity implements RegistrationClient.Listener {
    private static final String TAG = ActivityMain.class.getSimpleName();

    private static final int LAUNCH_AUTH_INTENT_REQUEST_CODE = 0;
    private static final int LAUNCH_ACCOUNT_ACTIVITY_REQUEST_CODE = 1;

    private RegistrationClient mRegistrationClient;
    private PubSubClient mPubSubClient;
    private String mAccountName;

    private TextView mResultsTextView;

    // C2DMClient.Listener

    @Override
    public void onRegistrationNotification(String accountName, String state, String substate) {
        Log.v(TAG, "onRegistrationNotification: accountName=" + accountName
                + ", state=" + state + ", substate=" + substate);
        appendResults(accountName + ": " + state + "." + substate + "\n");
    }

    @Override
    public void onLaunchIntent(Intent intent) {
        Log.v(TAG, "onLaunchIntent: intent=" + intent);
        this.startActivityForResult(intent, LAUNCH_AUTH_INTENT_REQUEST_CODE);
    }

    @Override
    public void onMessage(Context context, Intent intent) {
        Log.v(TAG,
                "onMessage: from=" + intent.getStringExtra("from")
                        + ", sender=" + intent.getStringExtra("sender")
                        + ", message=" + intent.getStringExtra("message"));

        appendResults(Utils.bundleToString(intent.getExtras()));
        appendResults("\n");
    }

    private void appendResults(final String message) {
        if (mResultsTextView == null) {
        }

        if (getWindow().getDecorView().getHandler() == null) {
            return;
        }

        getWindow().getDecorView().getHandler().post(new Runnable() {
            @Override
            public void run() {
                if (mResultsTextView != null) {
                    mResultsTextView.append(message);
                }
            }
        });
    }

    private final ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder binder) {
            Log.v(TAG, "onServiceConnected");

            mRegistrationClient = RegistrationClient.getService(binder);
            mPubSubClient = new PubSubClient(mRegistrationClient /* ITransactionService */, mRegistrationClient /* Context */);

            mRegistrationClient.addListener(ActivityMain.this);
            mRegistrationClient.setRegistrationStrategy(mPubSubClient);
        }

        @Override
        public void onServiceDisconnected(ComponentName className) {
            Log.v(TAG, "onServiceDisconnected");
            mRegistrationClient.removeListener(ActivityMain.this);
            mRegistrationClient = null;
            mPubSubClient = null;
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "onCreate: bundle=" + Utils.bundleToString(savedInstanceState));
        super.onCreate(savedInstanceState);

        final String APP_NAME = getResources().getString(R.string.gae_name);
        final String SENDER_ID = getResources().getString(R.string.sender_id);
        final String RESOURCE = getResources().getString(R.string.resource);

        Bundle configuration = new Bundle();
        configuration.putString(RegistrationClient.APP_NAME, APP_NAME);
        configuration.putString(RegistrationClient.RESOURCE, RESOURCE);
        configuration.putString(RegistrationClient.SENDER_ID, SENDER_ID);

        Intent intent = new Intent(this, RegistrationClient.class);
        intent.setAction(Intent.ACTION_MAIN);
        intent.putExtras(configuration);
        startService(intent);

        setContentView(R.layout.activity_main);

        mResultsTextView = (TextView) findViewById(R.id.results);

        checkPlayServices();

        bindService(intent, mConnection, Context.BIND_AUTO_CREATE | BIND_DEBUG_UNBIND);
    }

    @Override
    public void onResume() {
        Log.i(TAG, "onResume");
        super.onResume();
        checkPlayServices();

        if (TextUtils.isEmpty(mAccountName)) {
            Intent intent = new Intent(this, ActivityAccounts.class);
            startActivityForResult(intent, LAUNCH_ACCOUNT_ACTIVITY_REQUEST_CODE);
        }
    }

    @Override
    public void onPause() {
        Log.i(TAG, "onPause");
        super.onPause();
    }

    @Override
    public void onDestroy() {
        Log.i(TAG, "onDestroy");
        super.onDestroy();

        unbindService(mConnection);
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        Log.v(TAG, "onRestoreInstanceState: savedInstanceState=" + Utils.bundleToString(savedInstanceState));

        savedInstanceState.putString(RegistrationClient.ACCOUNT_NAME, mAccountName);
        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        Log.v(TAG, "onRestoreInstanceState: savedInstanceState=" + Utils.bundleToString(savedInstanceState));

        super.onRestoreInstanceState(savedInstanceState);
        mAccountName = savedInstanceState.getString(RegistrationClient.ACCOUNT_NAME);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        Log.d(TAG, "onActivityResult: requestCode=" + requestCode
                + ", resultCode=" + resultCode + ", intent=" + intent);

        if (requestCode == LAUNCH_ACCOUNT_ACTIVITY_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                mAccountName = intent.getStringExtra(RegistrationClient.ACCOUNT_NAME);
                Log.d(TAG, "accountName=" + mAccountName);
                if (!TextUtils.isEmpty(mAccountName)) {
                    mRegistrationClient.register(mAccountName);
                }
            } else {
                finish();
            }
        } else if (requestCode == LAUNCH_AUTH_INTENT_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                mRegistrationClient.reregister();
            } else {
                Log.e(TAG, "Auth intent failed: requestCode=" + requestCode + ", resultCode="
                        + resultCode + ", intent=" + intent);
            }
            finish();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Log.v(TAG, "onCreateOptionsMenu: menu=" + menu);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.v(TAG, "onOptionsItemSelected: item=" + item);

        final String title = item.getTitle().toString();
        Log.v(TAG, "onOptionsItemSelected: title=" + title);

        if (title.equals(getString(R.string.accounts))) {
            startActivityForResult(new Intent(this, ActivityAccounts.class), LAUNCH_ACCOUNT_ACTIVITY_REQUEST_CODE);
            return true;
        } else if (title.equals(getString(R.string.get_devices))) {
            getAllDevices();
            return true;
        } else if (title.equals(getString(R.string.register_device))) {
            if (mRegistrationClient != null) {
                mRegistrationClient.reregister();
            }
            return true;
        } else if (title.equals(getString(R.string.unregister_device))) {
            if (mRegistrationClient != null) {
                mRegistrationClient.unregister();
            }
            return true;
        } else if (title.equals(getString(R.string.message_device))) {
            messageDevice("test message to my own device\n");
            return true;
        } else if (title.equals(getString(R.string.get_users))) {
            getAllUsers();
            return true;
        } else if (title.equals(getString(R.string.register_user))) {
            return true;
        } else if (title.equals(getString(R.string.unregister_user))) {
            return true;
        } else if (title.equals(getString(R.string.message_user))) {
            messageDevice("test message to my own user\n");
            return true;
        } else if (title.equals(getString(R.string.get_publications))) {
            getAllPublications();
            return true;
        } else if (title.equals(getString(R.string.create_publication))) {
            createPublication("test publication");
            return true;
        } else if (title.equals(getString(R.string.delete_publication))) {
            return true;
        } else if (title.equals(getString(R.string.publish_message))) {
            createSubscription("test message to my publication");
            return true;
        } else if (title.equals(getString(R.string.subscribe))) {
            createSubscription("test publication");
            return true;
        } else if (title.equals(getString(R.string.unsubscribe))) {
            return true;
        } else if (title.equals(getString(R.string.clear))) {
            if (mRegistrationClient != null) {
                mRegistrationClient.clear();
            }
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    /**
     * Check the device to make sure it has the Google Play Services APK. If
     * it doesn't, display a dialog that allows users to download the APK from
     * the Google Play Store or enable it in the device's system settings.
     */
    private boolean checkPlayServices() {
        Log.v(TAG, "checkPlayServices");

        final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;

        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
                GooglePlayServicesUtil.getErrorDialog(resultCode, this,
                        PLAY_SERVICES_RESOLUTION_REQUEST).show();
            } else {
                Log.i(TAG, "This device is not supported.");
                finish();
            }
            return false;
        }
        return true;
    }

    private void getAllDevices() {
        Log.v(TAG, "getAllDevices");

        if (mRegistrationClient == null) {
            Log.e(TAG, "invalid client");
            return;
        }

        new AsyncTask<Void, Void, List<Bundle>>() {
            @Override
            protected List<Bundle> doInBackground(Void... params) {
                final Service service = mPubSubClient.getService(PubSubClient.SERVICE_DEVICE);
                return service.readAll();
            }

            @Override
            protected void onPostExecute(List<Bundle> devices) {
                super.onPostExecute(devices);

                if (devices == null) {
                    return;
                }

                for (final Bundle device : devices) {
                    Log.v(TAG, "device=" + Utils.bundleToString(device));
                    mResultsTextView.append(Utils.bundleToString(device));
                    mResultsTextView.append("\n");
                }
            }
        }.execute();
    }

    private void getAllUsers() {
        Log.v(TAG, "getAllUsers");

        if (mRegistrationClient == null) {
            Log.e(TAG, "invalid client");
            return;
        }

        new AsyncTask<Void, Void, List<Bundle>>() {
            @Override
            protected List<Bundle> doInBackground(Void... params) {
                final Service service = mPubSubClient.getService(PubSubClient.SERVICE_USER);
                return service.readAll();
            }

            @Override
            protected void onPostExecute(List<Bundle> users) {
                super.onPostExecute(users);

                if (users == null) {
                    return;
                }

                for (final Bundle user : users) {
                    Log.v(TAG, "user=" + Utils.bundleToString(user));
                    mResultsTextView.append(Utils.bundleToString(user));
                    mResultsTextView.append("\n");
                }
            }
        }.execute();
    }

    private void messageDevice(final String messageString) {
        Log.v(TAG, "messageDevice: message=" + messageString);

        final Bundle message = new Bundle();
        message.putString("dev_id", mPubSubClient.getDevId());
        message.putString("message", messageString);

        new AsyncTask<Bundle, Void, Void>() {
            @Override
            protected Void doInBackground(Bundle... params) {
                final Service service = mPubSubClient.getService(PubSubClient.SERVICE_DEVICE);
                final Bundle message = params[0];
                service.message(message);

                return null;
            }
        }.execute(message);
    }

    private void messageUser(final String messageString) {
        Log.v(TAG, "messageUser: message=" + messageString);

        final Bundle myUser = mPubSubClient.getUser();
        if (myUser == null) {
            return;
        }
        final String userId = myUser.getString("user_id");

        final Bundle message = new Bundle();
        message.putString("user_id", userId);
        message.putString("message", messageString);

        new AsyncTask<Bundle, Void, Void>() {
            @Override
            protected Void doInBackground(Bundle... params) {
                final Service service = mPubSubClient.getService(PubSubClient.SERVICE_USER);
                final Bundle message = params[0];
                service.message(message);

                return null;
            }
        }.execute(message);
    }

    private void createPublication(final String topic) {
        Log.v(TAG, "createPublication: topic=" + topic);

        final Bundle message = new Bundle();
        message.putString("dev_id", mPubSubClient.getDevId());
        message.putString("topic", topic);

        new AsyncTask<Bundle, Void, Bundle>() {
            @Override
            protected Bundle doInBackground(Bundle... params) {
                final Service service = mPubSubClient.getService(PubSubClient.SERVICE_PUBLISH);
                final Bundle message = params[0];
                Bundle publication = service.create(message);
                Log.v(TAG, "publication=" + Utils.bundleToString(publication));

                return publication;
            }

            @Override
            protected void onPostExecute(Bundle publication) {
                super.onPostExecute(publication);

                if (publication == null) {
                    return;
                }

                mResultsTextView.append(Utils.bundleToString(publication));
                mResultsTextView.append("\n");
            }
        }.execute(message);
    }

    private void getAllPublications() {
        Log.v(TAG, "getAllPublications");

        if (mRegistrationClient == null) {
            Log.e(TAG, "invalid client");
            return;
        }

        new AsyncTask<Void, Void, List<Bundle>>() {
            @Override
            protected List<Bundle> doInBackground(Void... params) {
                final Service service = mPubSubClient.getService(PubSubClient.SERVICE_PUBLISH);
                return service.readAll();
            }

            @Override
            protected void onPostExecute(List<Bundle> publications) {
                super.onPostExecute(publications);

                if (publications == null) {
                    return;
                }

                for (final Bundle publication : publications) {
                    Log.v(TAG, "publication=" + Utils.bundleToString(publication));
                    mResultsTextView.append(Utils.bundleToString(publication));
                    mResultsTextView.append("\n");
                }
            }
        }.execute();
    }

    private void publish(final String pubId, final String messageString) {
        Log.v(TAG, "publish: message=" + messageString);

        final Bundle message = new Bundle();
        message.putString("pub_id", pubId);
        message.putString("message", messageString);

        new AsyncTask<Bundle, Void, Void>() {
            @Override
            protected Void doInBackground(Bundle... params) {
                final Service service = mPubSubClient.getService(PubSubClient.SERVICE_PUBLISH);
                final Bundle message = params[0];
                service.message(message);

                return null;
            }
        }.execute(message);
    }

    private void createSubscription(final String topic) {
        Log.v(TAG, "createPublication: topic=" + topic);

        final Bundle message = new Bundle();
        message.putString("dev_id", mPubSubClient.getDevId());
        message.putString("topic", topic);

        new AsyncTask<Bundle, Void, Bundle>() {
            @Override
            protected Bundle doInBackground(Bundle... params) {
                final Service service = mPubSubClient.getService(PubSubClient.SERVICE_SUBSCRIBE);
                final Bundle message = params[0];
                Bundle subscription = service.create(message);
                Log.v(TAG, "subscription=" + Utils.bundleToString(subscription));

                return subscription;
            }

            @Override
            protected void onPostExecute(Bundle subscription) {
                super.onPostExecute(subscription);

                if (subscription == null) {
                    return;
                }

                mResultsTextView.append(Utils.bundleToString(subscription));
                mResultsTextView.append("\n");
            }
        }.execute(message);
    }

}

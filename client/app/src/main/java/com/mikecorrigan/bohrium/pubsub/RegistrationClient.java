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

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;

import com.mikecorrigan.bohrium.common.Log;
import com.mikecorrigan.bohrium.common.Utils;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.client.params.HttpClientParams;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class RegistrationClient extends android.app.Service implements ITransactionService {
    private static final String TAG = RegistrationClient.class.getSimpleName();

    public interface Listener {
        public void onRegistrationNotification(String accountName, String state, String substate);

        public void onLaunchIntent(Intent intent);

        public void onMessage(Context context, Intent intent);
    }

    public interface IRegistrationStrategy {
        public boolean register(final Bundle bundle);

        public boolean unregister(final Bundle bundle);
    }

    // CONFIGURATION/PREFERENCES
    private static final String SHARED_PREFS = RegistrationClient.class.getSimpleName()
            .toUpperCase(Locale.ENGLISH) + "_PREFS";

    public static final String APP_NAME = "appName";
    public static final String RESOURCE = "resource";
    public static final String SENDER_ID = "senderId";

    public static final String C2DM_BACKOFF = "backoff";
    private static final long DEFAULT_BACKOFF = 30000;
    private static final long BACKOFF_MULTIPLIER = 2;
    private static final long MAX_BACKOFF = 600000;

    public static final String ACCOUNT_NAME = "accountName";

    public static final String ACCOUNT_TYPE = "accountType";
    public static final String DEFAULT_ACCOUNT_TYPE = "com.google";

    public static final String AUTH_TOKEN = "authToken";
    public static final String REG_ID = "regId";

    public static final String LAST_CHANGE = "lastChange";

    public static final String REGISTRATION_STATE = "state";
    public static final String REGISTRATION_STATE_ERROR = "ERROR";
    public static final String REGISTRATION_STATE_INVALID = "INVALID";
    public static final String REGISTRATION_STATE_REGISTERING = "REGISTERING";
    public static final String REGISTRATION_STATE_REGISTERED = "REGISTERED";
    public static final String REGISTRATION_STATE_UNREGISTERING = "UNREGISTERING";
    public static final String REGISTRATION_STATE_UNREGISTERED = "UNREGISTERED";

    public static final String REGISTRATION_SUBSTATE = "subState";
    public static final String REGISTRATION_SUBSTATE_NONE = "NONE";

    // REGISTRATION_STATE_REGISTERING
    public static final String REGISTRATION_SUBSTATE_PROMPTING_USER = "PROMPTING_USER";
    public static final String REGISTRATION_SUBSTATE_INVALIDATED_AUTH_TOKEN = "INVALIDATED_AUTH_TOKEN";
    public static final String REGISTRATION_SUBSTATE_HAVE_AUTH_TOKEN = "HAVE_AUTH_TOKEN";
    public static final String REGISTRATION_SUBSTATE_HAVE_AUTH_COOKIE = "HAVE_AUTH_COOKIE";
    public static final String REGISTRATION_SUBSTATE_HAVE_REG_ID = "HAVE_REG_ID";

    // REGISTRATION_STATE_ERROR
    public static final String REGISTRATION_SUBSTATE_ERROR_C2DM_NOT_FOUND = "ERROR_C2DM_NOT_FOUND";
    public static final String REGISTRATION_SUBSTATE_ERROR_REGISTER = "ERROR_REGISTER";
    //public static final String REGISTRATION_SUBSTATE_ERROR_REG_ID = "ERROR_REG_ID";
    public static final String REGISTRATION_SUBSTATE_ERROR_AUTH_COOKIE = "ERROR_AUTH_COOKIE";
    public static final String REGISTRATION_SUBSTATE_ERROR_AUTH_TOKEN = "ERROR_AUTH_TOKEN";
    public static final String REGISTRATION_SUBSTATE_ERROR_UNREGISTER = "ERROR_UNREGISTER";

    private static final int EVENT_INITIALIZE = 0;
    private static final int EVENT_UNINITIALIZE = 1;
    private static final int EVENT_REGISTER = 2;
    private static final int EVENT_REREGISTER = 3;
    private static final int EVENT_UNREGISTER = 4;
    private static final int EVENT_C2DM_REGISTRATION_RESPONSE = 5;
    private static final int EVENT_C2DM_MESSAGE = 6;
    private static final int EVENT_C2DM_RETRY_REGISTRATION = 7;
    private static final int EVENT_REGISTER_COMPLETE = 8;
    private static final int EVENT_UNREGISTER_COMPLETE = 9;
    private static final int EVENT_CLEAR = 10;

    // Cookie name for authorization.
    private static final String AUTH_COOKIE_NAME = "SACSID";

    // C2DM Request intents and extras.
    private static final String GSF_PACKAGE = "com.google.android.gsf";

    private static final String REQUEST_UNREGISTRATION_INTENT = "com.google.android.c2dm.intent.UNREGISTER";
    private static final String REQUEST_REGISTRATION_INTENT = "com.google.android.c2dm.intent.REGISTER";

    private static final String EXTRA_APPLICATION_PENDING_INTENT = "app";
    private static final String EXTRA_SENDER = "sender";

    // C2DM Response intents and extras.
    private static final String C2DM_INTENT_REGISTRATION = "com.google.android.c2dm.intent.REGISTRATION";

    public static final String EXTRA_REGISTRATION_ID = "registration_id";

    // Indicates an unregister response. Otherwise it is a register response.
    public static final String EXTRA_UNREGISTERED = "unregistered";

    // Error codes for the registration intent.
    public static final String EXTRA_ERROR = "error";
    //public static final String ERR_SERVICE_NOT_AVAILABLE = "SERVICE_NOT_AVAILABLE";
    //public static final String ERR_ACCOUNT_MISSING = "ACCOUNT_MISSING";
    //public static final String ERR_AUTHENTICATION_FAILED = "AUTHENTICATION_FAILED";
    //public static final String ERR_TOO_MANY_REGISTRATIONS = "TOO_MANY_REGISTRATIONS";
    //public static final String ERR_INVALID_PARAMETERS = "INVALID_PARAMETERS";
    //public static final String ERR_INVALID_SENDER = "INVALID_SENDER";
    //public static final String ERR_PHONE_REGISTRATION_ERROR = "PHONE_REGISTRATION_ERROR";

    private static final String C2DM_INTENT_RECEIVE = "com.google.android.c2dm.intent.RECEIVE";

    // Internal intent to trigger registration re-tries.
    private static final String C2DM_INTENT_RETRY = "com.google.android.c2dm.intent.RETRY";

    // STATE
    private final IBinder mBinder = new _Binder();
    private Handler mHandler;
    private HandlerThread mHandlerThread;
    private Listener mListener;
    private IRegistrationStrategy mRegistrationStrategy;

    private Bundle mConfiguration;

    private String mState = REGISTRATION_STATE_INVALID;
    private String mSubState = REGISTRATION_SUBSTATE_NONE;
    private boolean mNeedInvalidate;
    private Cookie mAuthCookie;

    public static RegistrationClient getService(IBinder binder) {
        Log.v(TAG, "getService: binder=" + binder);
        return ((RegistrationClient._Binder) binder).getService();
    }

    private class _Binder extends Binder {
        private RegistrationClient getService() {
            Log.v(TAG, "getService: this=" + RegistrationClient.this);
            return RegistrationClient.this;
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.v(TAG, "onStartCommand: intent=" + intent + ", extras=" + Utils.bundleToString(intent.getExtras()) + ", flags=" + flags + ", startId=" + startId);

        if (intent.getAction().equals(C2DM_INTENT_REGISTRATION)) {
            mHandler.sendMessage(mHandler.obtainMessage(EVENT_C2DM_REGISTRATION_RESPONSE, intent));
            GCMBroadcastReceiver.completeWakefulIntent(intent);
        } else if (intent.getAction().equals(C2DM_INTENT_RECEIVE)) {
            mHandler.sendMessage(mHandler.obtainMessage(EVENT_C2DM_MESSAGE, intent));
            GCMBroadcastReceiver.completeWakefulIntent(intent);
        } else if (intent.getAction().equals(C2DM_INTENT_RETRY)) {
            mHandler.sendMessage(mHandler.obtainMessage(EVENT_C2DM_RETRY_REGISTRATION, intent));
            GCMBroadcastReceiver.completeWakefulIntent(intent);
        } else if (intent.getAction().equals(Intent.ACTION_MAIN)) {
            if (mConfiguration == null) {
                mConfiguration = intent.getExtras();
            }

            mHandler.sendEmptyMessage(EVENT_INITIALIZE);
        } else if (intent.getAction().equals(Intent.ACTION_SHUTDOWN)) {
            mHandler.sendEmptyMessage(EVENT_UNINITIALIZE);
        }

        return RegistrationClient.START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.v(TAG, "onBind: intent=" + intent);
        return mBinder;
    }

    @Override
    public void onCreate() {
        Log.v(TAG, "onCreate: ");
        super.onCreate();

        mHandlerThread = new HandlerThread(TAG + "Thread");
        mHandlerThread.start();
        mHandler = new _Handler(mHandlerThread.getLooper());
    }

    @Override
    public void onDestroy() {
        Log.v(TAG, "onDestroy: ");
        super.onDestroy();

        if (mHandler != null) {
            mHandler.removeCallbacksAndMessages(null);
            mHandler = null;
        }

        if (mHandlerThread != null) {
            mHandlerThread.quitSafely();
            mHandlerThread = null;
        }
    }

    // THREADING: Called in the application's context.
    public void register(final String accountName) {
        Log.v(TAG, "register");

        if (accountName == null || accountName.length() == 0) {
            Log.e(TAG, "Invalid account name.");
            return;
        }

        mHandler.sendMessage(mHandler.obtainMessage(EVENT_REGISTER, accountName));
    }

    // THREADING: Called in the application's context.
    public boolean reregister() {
        Log.v(TAG, "reregister");

        if (TextUtils.isEmpty(getAccountName())) {
            Log.e(TAG, "account not selected");
            return false;
        }

        mHandler.sendEmptyMessage(EVENT_REREGISTER);
        return true;
    }

    // THREADING: Called in the application's context.
    public void unregister() {
        Log.v(TAG, "unregister");
        mHandler.sendEmptyMessage(EVENT_UNREGISTER);
    }

    // THREADING: Called in the application's context.
    public void clear() {
        mHandler.sendEmptyMessage(EVENT_UNREGISTER);
        mHandler.sendEmptyMessage(EVENT_CLEAR);
    }

    // THREADING: Called in the application's context.
    public String getBaseUrl() {
        return "https://" + mConfiguration.getString(APP_NAME) + ".appspot.com";
    }

    // THREADING: Called in the application's context.
    public Bundle getConfiguration() {
        return (Bundle) mConfiguration.clone();
    }

    // THREADING: Called in the application's context.
    // It is possible for mHandlerThread to modify mAccountName at
    // the same time, but that is ok. That is the nature of
    // asynchronous communications.
    private String getAccountName() {
        return mConfiguration.getString(ACCOUNT_NAME);
    }

    // THREADING: Called in the application's context.
    // It is possible for mHandlerThread to modify mState at
    // the same time, but that is ok. That is the nature of
    // asynchronous communications.
    public boolean isRegistered() {
        return mState.equals(REGISTRATION_STATE_REGISTERED);
    }

    private class _Handler extends Handler {
        public _Handler(Looper looper) {
            super(looper);
            Log.d(TAG, "_Handler: looper=" + looper);
        }

        @Override
        public void handleMessage(Message msg) {
            Log.d(TAG, "handleMessage: msg=" + msg);
            switch (msg.what) {
                // TODO: Implement a state-machine.
                case EVENT_INITIALIZE: {
                    handleInitialize();
                    break;
                }
                case EVENT_UNINITIALIZE: {
                    handleUninitialize();
                    break;
                }
                case EVENT_REGISTER: {
                    String accountName = (String) msg.obj;
                    handleRegister(accountName);
                    break;
                }
                case EVENT_REREGISTER: {
                    requestAuthToken();
                    break;
                }
                case EVENT_UNREGISTER: {
                    handleUnregister();
                    break;
                }
                case EVENT_C2DM_REGISTRATION_RESPONSE: {
                    Intent intent = (Intent) msg.obj;
                    c2dmHandleRegistrationResponse(RegistrationClient.this, intent);
                    break;
                }
                case EVENT_C2DM_MESSAGE: {
                    Intent intent = (Intent) msg.obj;
                    notifyMessage(RegistrationClient.this, intent);
                    break;
                }
                case EVENT_C2DM_RETRY_REGISTRATION: {
                    // Intent intent = (Intent) msg.obj;
                    c2dmRegister(RegistrationClient.this, mConfiguration.getString(SENDER_ID));
                    break;
                }
                case EVENT_REGISTER_COMPLETE: {
                    handleRegisterComplete();
                    break;
                }
                case EVENT_UNREGISTER_COMPLETE: {
                    handleUnregisterComplete();
                    break;
                }
                case EVENT_CLEAR: {
                    handleClear();
                    break;
                }
                default: {
                    Log.e(TAG, "Unknown message, " + msg);
                }
            }
        }
    }

    private void handleInitialize() {
        // Ensure that this is the first call to create().
        if (!mState.equals(REGISTRATION_STATE_INVALID)) {
            Log.e(TAG, "Initialization failed, already initialized.");
            return;
        }

        readPreferences();
    }

    private void handleUninitialize() {
        if (mState.equals(REGISTRATION_STATE_INVALID)) {
            Log.e(TAG, "Uninitialization failed, not initialized.");
            return;
        }

        mHandlerThread.quitSafely();
    }

    private void handleRegister(String accountName) {
        if (isRegistered()) {
            Log.i(TAG, "Registered, ignoring register.");
            return;
        }

        mState = REGISTRATION_STATE_REGISTERING;
        mSubState = REGISTRATION_SUBSTATE_NONE;
        mNeedInvalidate = true;
        mAuthCookie = null;

        mConfiguration.putString(ACCOUNT_NAME, accountName);
        mConfiguration.putString(AUTH_TOKEN, "");
        mConfiguration.putString(REG_ID, "");
        writePreferences();

        requestAuthToken();
    }

    private void handleUnregister() {
        if (!isRegistered()) {
            Log.i(TAG, "Not registered, ignoring unregister.");
            return;
        }

        c2dmUnregister(this);
    }

    private void handleRegisterComplete() {
        Log.v(TAG, "handleRegisterComplete");

        boolean result = mRegistrationStrategy != null ? mRegistrationStrategy.register(mConfiguration) : true;
        if (result) {
            setStateAndNotify(REGISTRATION_STATE_REGISTERED, REGISTRATION_SUBSTATE_NONE);
        } else {
            setStateAndNotify(REGISTRATION_STATE_ERROR, REGISTRATION_SUBSTATE_ERROR_REGISTER);
        }
    }

    private void handleUnregisterComplete() {
        Log.v(TAG, "handleUnregisterComplete");

        boolean result = mRegistrationStrategy != null ? mRegistrationStrategy.unregister(mConfiguration) : true;
        if (result) {
            mConfiguration.putString(REG_ID, "");
            setStateAndNotify(REGISTRATION_STATE_UNREGISTERED, REGISTRATION_SUBSTATE_NONE);
        } else {
            setStateAndNotify(REGISTRATION_STATE_ERROR, REGISTRATION_SUBSTATE_ERROR_UNREGISTER);
        }
    }

    private void handleClear() {
        Log.v(TAG, "handleClear");

        mState = REGISTRATION_STATE_REGISTERING;
        mSubState = REGISTRATION_SUBSTATE_NONE;
        mNeedInvalidate = true;
        mAuthCookie = null;

        mConfiguration.putString(ACCOUNT_NAME, "");
        mConfiguration.putString(AUTH_TOKEN, "");
        mConfiguration.putString(REG_ID, "");
        mConfiguration.putString(REGISTRATION_STATE, mState);
        mConfiguration.putString(REGISTRATION_SUBSTATE, mSubState);
        mConfiguration.putLong(LAST_CHANGE, 0);
        writePreferences();
    }

    // THREADING: Called in the context of mHandlerThread only.
    private void setStateAndNotify(String state, String substate) {
        mState = state;
        mSubState = substate;
        writePreferences();

        notifyListeners();
    }

    // THREADING: Called in the context of mHandlerThread only.
    private void notifyListeners() {
        Log.v(TAG, "notifyListeners: accountName=" + mConfiguration.getString(ACCOUNT_NAME) + ", mState="
                + mState + ", mSubState=" + mSubState);

        if (mListener != null) {
            mListener.onRegistrationNotification(mConfiguration.getString(ACCOUNT_NAME), mState, mSubState);
        }
    }

    // THREADING: Called in the context of mHandlerThread only.
    private void notifyLaunchIntent(Intent intent) {
        Log.v(TAG, "notifyLaunchIntent: intent=" + intent);

        if (mListener != null) {
            mListener.onLaunchIntent(intent);
        }
    }

    // THREADING: Called in the context of mHandlerThread only.
    private void notifyMessage(Context context, Intent intent) {
        Log.v(TAG, "notifyMessage: context=" + context + ", intent=" + intent);

        if (mListener != null) {
            mListener.onMessage(context, intent);
        }
    }

    // THREADING: Called in the context of mHandlerThread only.
    private void readPreferences() {
        Log.v(TAG, "readPreferences: name=" + SHARED_PREFS);

        SharedPreferences prefs = this.getSharedPreferences(SHARED_PREFS, Context.MODE_PRIVATE);
        Utils.prefsToBundle(prefs, mConfiguration);
    }

    // THREADING: Called in the context of mHandlerThread only.
    private void writePreferences() {
        Log.v(TAG, "writePreferences: name=" + SHARED_PREFS);

        SharedPreferences prefs = this.getSharedPreferences(SHARED_PREFS, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        Utils.bundleToPrefsEditor(mConfiguration, editor);
        editor.apply();
    }

    // THREADING: Called in the context of mHandlerThread only.
    private void clearPreferences() {
        Log.v(TAG, "clearPreferences: name=" + SHARED_PREFS);

        this.getSharedPreferences(SHARED_PREFS, Context.MODE_PRIVATE).edit().clear().commit();
    }

    // THREADING: Called in the application's context.
    public void addListener(Listener listener) {
        Log.v(TAG, "addListener: listener=" + listener);

        mListener = listener;
    }

    // THREADING: Called in the application's context.
    public void removeListener(Listener listener) {
        Log.v(TAG, "removeListener: listener=" + listener);

        mListener = null;
    }

    // THREADING: Called in the application's context.
    public void setRegistrationStrategy(final IRegistrationStrategy registrationStrategy) {
        mRegistrationStrategy = registrationStrategy;
    }

    // THREADING: Called in the application's context.
    @Override
    public ITransaction createTransaction(final String method, final String uriString, final Object requestBody) {
        Log.v(TAG, "createTransaction: method=" + method + ", uri=" + uriString + ", requestBody=" + requestBody);

        if (mAuthCookie == null) {
            Log.e(TAG, "createTransaction: auth cookie not set");
            return null;
        }

        return new JSONTransaction(getBaseUrl(), mAuthCookie, method, uriString, (JSONObject) requestBody);
    }

    // THREADING: Called in the application's worker-thread.
    @Override
    public void completeTransaction(final ITransaction transaction) {
        Log.v(TAG, "completeTransaction: transaction=" + transaction);

        transaction.run();
    }

    // Returns a list of account names of the specified type. If no appropriate
    // accounts are registered on the device, a zero-length list is returned.
    public static List<String> getAccounts(final Context context, final String accountType) {
        Log.v(TAG, "getAccountsByType: accountType=" + accountType);

        final AccountManager mgr = AccountManager.get(context);
        ArrayList<String> result = new ArrayList<>();
        final Account[] accounts = mgr.getAccounts();
        for (final Account account : accounts) {
            Log.v(TAG, "accountName=" + account.name);
            if (account.type.equals(accountType)) {
                result.add(account.name);
            }
        }

        return result;
    }

    private Account getAccount() {
        final AccountManager mgr = AccountManager.get(this);
        final Account[] accounts = mgr.getAccountsByType(mConfiguration.getString(ACCOUNT_TYPE, DEFAULT_ACCOUNT_TYPE));
        for (final Account account : accounts) {
            if (account.name.equals(mConfiguration.getString(ACCOUNT_NAME))) {
                return account;
            }
        }

        return null;
    }

    private void requestAuthToken() {
        Log.v(TAG, "requestAuthToken");

        final AccountManager mgr = AccountManager.get(this);
        final Account account = getAccount();
        if (account == null) {
            Log.e(TAG, "Failed to find account: accountType=" + mConfiguration.getString(ACCOUNT_TYPE, DEFAULT_ACCOUNT_TYPE)
                    + ", accountName=" + mConfiguration.getString(ACCOUNT_NAME));
            setStateAndNotify(REGISTRATION_STATE_ERROR, REGISTRATION_SUBSTATE_ERROR_AUTH_TOKEN);
            return;
        }

        mgr.getAuthToken(account, "ah", false, new AuthTokenCallback(), mHandler);
    }

    private class AuthTokenCallback implements AccountManagerCallback<Bundle> {
        @Override
        public void run(AccountManagerFuture<Bundle> future) {
            Log.i(TAG, "AuthTokenCallback");
            handleAuthToken(future);
        }
    }

    @SafeVarargs
    private final void handleAuthToken(AccountManagerFuture<Bundle>... tokens) {
        Log.v(TAG, "handleAuthToken");

        try {
            Bundle result = tokens[0].getResult();

            Intent intent = (Intent) result.get(AccountManager.KEY_INTENT);
            if (intent != null) {
                Log.i(TAG, "Launch activity before getting authToken: intent=" + intent);

                setStateAndNotify(REGISTRATION_STATE_REGISTERING, REGISTRATION_SUBSTATE_PROMPTING_USER);

                intent.setFlags(intent.getFlags() & ~Intent.FLAG_ACTIVITY_NEW_TASK);
                notifyLaunchIntent(intent);
                return;
            }

            String authToken = result.getString(AccountManager.KEY_AUTHTOKEN);
            if (mNeedInvalidate) {
                mNeedInvalidate = false;

                Log.i(TAG, "Invalidating token and starting over.");

                // Invalidate auth token.
                AccountManager mgr = AccountManager.get(this);
                mgr.invalidateAuthToken(mConfiguration.getString(ACCOUNT_TYPE, DEFAULT_ACCOUNT_TYPE), authToken);

                setStateAndNotify(REGISTRATION_STATE_REGISTERING, REGISTRATION_SUBSTATE_INVALIDATED_AUTH_TOKEN);

                // Initiate the request again.
                requestAuthToken();
                return;
            } else {
                Log.i(TAG, "Received authToken=" + authToken);
                mConfiguration.putString(AUTH_TOKEN, authToken);
                setStateAndNotify(REGISTRATION_STATE_REGISTERING, REGISTRATION_SUBSTATE_HAVE_AUTH_TOKEN);

                // Move on to the next step, request auth cookie.
                requestAuthCookie();
                return;
            }
        } catch (Exception e) {
            Log.e(TAG, "Exception " + e);
            Log.e(TAG, Log.getStackTraceString(e));
        }

        setStateAndNotify(REGISTRATION_STATE_ERROR, REGISTRATION_SUBSTATE_ERROR_AUTH_TOKEN);
    }

    private void requestAuthCookie() {
        DefaultHttpClient httpClient = new DefaultHttpClient();
        try {
            String continueURL = getBaseUrl();
            URI uri = new URI(getBaseUrl() + "/_ah/login?continue="
                    + URLEncoder.encode(continueURL, "UTF-8") + "&auth="
                    + mConfiguration.getString(AUTH_TOKEN));
            HttpGet method = new HttpGet(uri);

            final HttpParams getParams = new BasicHttpParams();
            HttpClientParams.setRedirecting(getParams, false);
            method.setParams(getParams);

            HttpResponse res = httpClient.execute(method);
            Header[] headers = res.getHeaders("Set-Cookie");
            int statusCode = res.getStatusLine().getStatusCode();
            Log.v(TAG, "statusCode=" + statusCode);
            if (statusCode != 302 || headers.length == 0) {
                Log.e(TAG, "Failed to get authCookie: statusCode=" + statusCode);
                setStateAndNotify(REGISTRATION_STATE_ERROR, REGISTRATION_SUBSTATE_ERROR_AUTH_COOKIE);
                return;
            }

            for (Cookie cookie : httpClient.getCookieStore().getCookies()) {
                Log.v(TAG, "cookie=" + cookie.getName());
                if (AUTH_COOKIE_NAME.equals(cookie.getName())) {
                    Log.i(TAG, "Received authCookie=" + cookie);
                    mAuthCookie = cookie;
                    setStateAndNotify(REGISTRATION_STATE_REGISTERING, REGISTRATION_SUBSTATE_HAVE_AUTH_COOKIE);

                    // Move on to the next step, register to C2DM.
                    c2dmRegister(this, mConfiguration.getString("senderId"));
                    return;
                }
            }
        } catch (IOException | URISyntaxException e) {
            Log.e(TAG, "Exception " + e);
            Log.e(TAG, Log.getStackTraceString(e));
        } finally {
            httpClient.getParams().setBooleanParameter(
                    ClientPNames.HANDLE_REDIRECTS, true);
        }

        setStateAndNotify(REGISTRATION_STATE_ERROR, REGISTRATION_SUBSTATE_ERROR_AUTH_COOKIE);
    }

    private void c2dmRegister(Context context, String senderId) {
        Log.d(TAG, "c2dmRegister: context=" + context + ", senderId=" + senderId);

        Intent intent = new Intent(REQUEST_REGISTRATION_INTENT);
        intent.setPackage(GSF_PACKAGE);
        intent.putExtra(EXTRA_APPLICATION_PENDING_INTENT, PendingIntent.getBroadcast(context, 0, new Intent(), 0));
        intent.putExtra(EXTRA_SENDER, senderId);
        ComponentName name = context.startService(intent);
        if (name == null) {
            // Service not found.
            setStateAndNotify(REGISTRATION_STATE_ERROR, REGISTRATION_SUBSTATE_ERROR_C2DM_NOT_FOUND);
        }
    }

    private void c2dmUnregister(Context context) {
        Log.d(TAG, "c2dmUnregister: context=" + context);

        Intent intent = new Intent(REQUEST_UNREGISTRATION_INTENT);
        intent.setPackage(GSF_PACKAGE);
        intent.putExtra(EXTRA_APPLICATION_PENDING_INTENT, PendingIntent.getBroadcast(context, 0, new Intent(), 0));
        ComponentName name = context.startService(intent);
        if (name == null) {
            // Service not found.
            setStateAndNotify(REGISTRATION_STATE_ERROR, REGISTRATION_SUBSTATE_ERROR_C2DM_NOT_FOUND);
        }
    }

    private void c2dmHandleRegistrationResponse(final Context context,
                                                Intent intent) {
        Log.d(TAG, "c2dmHandleRegistrationResponse: context=" + context + ", intent=" + intent + ", extras=" + Utils.bundleToString(intent.getExtras()));

        final String registrationId = intent
                .getStringExtra(EXTRA_REGISTRATION_ID);
        String error = intent.getStringExtra(EXTRA_ERROR);
        String unregistered = intent.getStringExtra(EXTRA_UNREGISTERED);

        Log.d(TAG, "handleRegistration: registrationId = " + registrationId
                + ", error = " + error + ", unregistered = " + unregistered);

        mConfiguration.putLong(LAST_CHANGE, System.currentTimeMillis());

        if (unregistered != null) {
            // Unregistered
            mConfiguration.putString(REG_ID, "");
            setStateAndNotify(REGISTRATION_STATE_UNREGISTERING, REGISTRATION_SUBSTATE_NONE);

            mHandler.sendEmptyMessage(EVENT_UNREGISTER_COMPLETE);
        } else if (error != null) {
            // Registration failed.
            Log.e(TAG, "Registration error " + error);

            mConfiguration.putString(REG_ID, "");
            setStateAndNotify(REGISTRATION_STATE_ERROR, error);

            if ("SERVICE_NOT_AVAILABLE".equals(error)) {
                long backoffTime = mConfiguration.getLong(C2DM_BACKOFF, DEFAULT_BACKOFF);

                // For this error, try again later.
                Log.d(TAG, "Scheduling registration retry, backoff = " + backoffTime);
                Intent retryIntent = new Intent(C2DM_INTENT_RETRY);
                PendingIntent retryPIntent = PendingIntent
                        .getBroadcast(context, 0 /* requestCode */, retryIntent,
                                0 /* flags */);

                AlarmManager am = (AlarmManager) context
                        .getSystemService(Context.ALARM_SERVICE);
                am.set(AlarmManager.ELAPSED_REALTIME, backoffTime, retryPIntent);

                // Next retry should wait longer.
                backoffTime *= BACKOFF_MULTIPLIER;
                if (backoffTime > MAX_BACKOFF) {
                    backoffTime = MAX_BACKOFF;

                    mConfiguration.putLong(C2DM_BACKOFF, backoffTime);
                }

                // Save the backoff time.
                writePreferences();
            }
        } else {
            mConfiguration.putString(REG_ID, registrationId);
            setStateAndNotify(REGISTRATION_STATE_REGISTERING, REGISTRATION_SUBSTATE_HAVE_REG_ID);

            mHandler.sendEmptyMessage(EVENT_REGISTER_COMPLETE);
        }
    }

    @Override
    public String toString() {
        return "configuration=" + Utils.bundleToString(mConfiguration)
                + ", state=" + mState + ", substate=" + mSubState
                + ", authCookie=" + mAuthCookie + ", needInvalidate=" + mNeedInvalidate;
    }
}

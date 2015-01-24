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

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.CookieStore;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.client.params.HttpClientParams;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.cookie.Cookie;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.BasicHttpContext;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;

public abstract class Transaction implements ITransaction {
    private static final String TAG = Transaction.class.getSimpleName();

    // State
    private final String mBaseUriString;
    private final Cookie mAuthCookie;
    // Request
    private final String mMethod;
    private final String mUriString;
    private final Object mRequestBody;
    // Response
    private int mStatusCode;
    private String mStatusReason;
    private Object mResponseBody;

    public Transaction(final String baseUriString, final Cookie authCookie, final String method, final String uriString, final Object requestBody) {
        Log.v(TAG, "ctor");
        // State
        mBaseUriString = baseUriString;
        mAuthCookie = authCookie;
        // Request
        mUriString = uriString;
        mMethod = method;
        mRequestBody = requestBody;
    }

    public String getMethod() {
        return mMethod;
    }

    public String getUriString() {
        return mUriString;
    }

    public Object getRequestBody() {
        return mRequestBody;
    }

    public int getStatusCode() {
        return mStatusCode;
    }

    public String getStatusReason() {
        return mStatusReason;
    }

    public Object getResponseBody() {
        return mResponseBody;
    }

    protected abstract String getMimeType();

    protected abstract String encode(final Object object);

    protected abstract Object decode(final String string);

    public void run() {
        Log.v(TAG, "send");

        mStatusCode = -1;
        mStatusReason = null;
        mResponseBody = null;

        Log.v(TAG, "baseUri=" + mBaseUriString);
        Log.v(TAG, "authCookie=" + mAuthCookie);
        Log.v(TAG, "method=" + mMethod);
        Log.v(TAG, "uri=" + mUriString);
        if (mRequestBody != null) {
            Log.v(TAG, "requestBody=" + mRequestBody);
        }

        // Encode URI.
        URI uri;
        try {
            uri = new URI(mBaseUriString + "/" + mUriString);
            Log.v(TAG, "uri=" + uri);
        } catch (URISyntaxException e) {
            Log.w(TAG, "Invalid URI, " + e);
            uri = null;
        }

        if (uri != null) {
            // Dispatch method.
            switch (mMethod) {
                case "GET": {
                    read(new HttpGet(uri));
                    break;
                }
                case "DELETE": {
                    read(new HttpDelete(uri));
                    break;
                }
                case "PUT": {
                    readWrite(new HttpPut(uri));
                    break;
                }
                case "POST": {
                    readWrite(new HttpPost(uri));
                    break;
                }
                default: {
                    Log.e(TAG, "Unsupported method, " + mMethod);
                    break;
                }
            }
        }

        Log.v(TAG, "statusCode=" + mStatusCode);
        Log.v(TAG, "statusReason=" + mStatusReason);
        Log.v(TAG, "responseBody=" + mResponseBody);
    }

    private int read(HttpRequestBase request) {
        Log.v(TAG, "read");

        CookieStore mCookieStore = new BasicCookieStore();
        mCookieStore.addCookie(mAuthCookie);

        DefaultHttpClient httpClient = new DefaultHttpClient();
        BasicHttpContext mHttpContext = new BasicHttpContext();
        mHttpContext.setAttribute(ClientContext.COOKIE_STORE, mCookieStore);

        try {
            final HttpParams getParams = new BasicHttpParams();
            HttpClientParams.setRedirecting(getParams, false);
            request.setParams(getParams);

            request.setHeader("Accept", getMimeType());

            HttpResponse response = httpClient.execute(request, mHttpContext);
            Log.d(TAG, "status=" + response.getStatusLine());

            // Read response body.
            HttpEntity responseEntity = response.getEntity();
            if (responseEntity != null) {
                InputStream is = responseEntity.getContent();
                BufferedReader reader = new BufferedReader(new InputStreamReader(is, "utf-8"), 8);
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                    sb.append("\n");
                }
                is.close();

                mStatusCode = response.getStatusLine().getStatusCode();
                mStatusReason = response.getStatusLine().getReasonPhrase();
                if (mStatusCode == 200) {
                    mResponseBody = decode(sb.toString());
                    Log.v(TAG, "mResponseBody=" + sb.toString());
                }
                return mStatusCode;
            }
        } catch (IOException e) {
            Log.e(TAG, "exception=" + e);
            Log.e(TAG, Log.getStackTraceString(e));
        } finally {
            httpClient.getParams().setBooleanParameter(ClientPNames.HANDLE_REDIRECTS, true);
        }

        return mStatusCode;
    }

    private int readWrite(HttpEntityEnclosingRequestBase request) {
        Log.v(TAG, "readWrite");

        CookieStore mCookieStore = new BasicCookieStore();
        mCookieStore.addCookie(mAuthCookie);

        DefaultHttpClient httpClient = new DefaultHttpClient();
        BasicHttpContext mHttpContext = new BasicHttpContext();
        mHttpContext.setAttribute(ClientContext.COOKIE_STORE, mCookieStore);

        // Encode request body.
        StringEntity requestEntity;
        try {
            requestEntity = new StringEntity(encode(mRequestBody));
        } catch (UnsupportedEncodingException e) {
            Log.e(TAG, "HTTP encoding failed=" + e);
            return mStatusCode;
        }

        try {
            final HttpParams getParams = new BasicHttpParams();
            HttpClientParams.setRedirecting(getParams, false);
            request.setParams(getParams);

            request.setHeader("Content-Type", getMimeType());
            request.setEntity(requestEntity);

            HttpResponse response = httpClient.execute(request, mHttpContext);
            Log.d(TAG, "status=" + response.getStatusLine());

            // Read response body.
            HttpEntity responseEntity = response.getEntity();
            if (responseEntity != null) {
                InputStream is = responseEntity.getContent();
                BufferedReader reader = new BufferedReader(new InputStreamReader(is, "utf-8"), 8);
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                    sb.append("\n");
                }
                is.close();

                mStatusCode = response.getStatusLine().getStatusCode();
                mStatusReason = response.getStatusLine().getReasonPhrase();
                if (mStatusCode == 200) {
                    mResponseBody = decode(sb.toString());
                    Log.v(TAG, "mResponseBody=" + sb.toString());
                }
                return mStatusCode;
            }
        } catch (IOException e) {
            Log.e(TAG, "exception=" + e);
            Log.e(TAG, Log.getStackTraceString(e));
        } finally {
            httpClient.getParams().setBooleanParameter(ClientPNames.HANDLE_REDIRECTS, true);
        }

        return mStatusCode;
    }
}


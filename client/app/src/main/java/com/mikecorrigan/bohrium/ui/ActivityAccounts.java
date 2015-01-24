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
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.mikecorrigan.bohrium.R;
import com.mikecorrigan.bohrium.common.Log;
import com.mikecorrigan.bohrium.pubsub.RegistrationClient;

import java.util.List;

public class ActivityAccounts extends Activity {
    private static final String TAG = ActivityAccounts.class.getSimpleName();

    private final Context mContext = this;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.v(TAG, "onCreate: savedInstanceState=" + savedInstanceState);
        super.onCreate(savedInstanceState);

        setContentView(R.layout.connect);
        setConnectScreenContent();
    }

    private void setConnectScreenContent() {
        List<String> accounts = RegistrationClient.getAccounts(mContext, RegistrationClient.DEFAULT_ACCOUNT_TYPE);

        if (accounts.size() == 0) {
            // Show a dialog and invoke the "Add Account" activity if requested
            AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
            builder.setMessage(R.string.needs_account);
            builder.setPositiveButton(R.string.add_account, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    // TODO: See if it is possible to get the activity result to determine success.
                    startActivity(new Intent(Settings.ACTION_ADD_ACCOUNT));
                }
            });
            builder.setNegativeButton(R.string.skip, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    setResult(RESULT_CANCELED);
                    finish();
                }
            });
            builder.setIcon(android.R.drawable.stat_sys_warning);
            builder.setTitle(R.string.attention);
            builder.show();
        } else {
            final ListView listView = (ListView) findViewById(R.id.select_account);
            if (listView != null) {
                listView.setAdapter(new ArrayAdapter<>(mContext, R.layout.activity_account, accounts));
                listView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
                listView.setItemChecked(0, true);
            }

            final Button connectButton = (Button) findViewById(R.id.connect);
            if (connectButton != null) {
                connectButton.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        int pos = listView.getCheckedItemPosition();
                        TextView account = (TextView) listView.getChildAt(pos);

                        Log.d(TAG, "ConnectButton: accountName=" + account.getText());

                        Intent resultIntent = new Intent();
                        resultIntent.putExtra(RegistrationClient.ACCOUNT_NAME, (String) account.getText());
                        setResult(Activity.RESULT_OK, resultIntent);

                        finish();
                    }
                });
            }

            final Button disconnectButton = (Button) findViewById(R.id.disconnect);
            if (disconnectButton != null) {
                disconnectButton.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Log.d(TAG, "DisconnectButton");

                        Intent resultIntent = new Intent();
                        resultIntent.putExtra(RegistrationClient.ACCOUNT_NAME, "");
                        setResult(Activity.RESULT_OK, resultIntent);

                        finish();
                    }
                });
            }
        }
    }
}

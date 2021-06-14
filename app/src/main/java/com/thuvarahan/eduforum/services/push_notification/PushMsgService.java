/*
 *  Copyright 2020. Huawei Technologies Co., Ltd. All rights reserved.
 *  This subclass is created by HMS Core Toolkit 
 *  and used to receive token information or messages returned by HMS server
 *
 */
package com.thuvarahan.eduforum.services.push_notification;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.huawei.hms.push.HmsMessageService;
import com.huawei.hms.push.RemoteMessage;
import com.thuvarahan.eduforum.CustomUtils;

import java.util.HashMap;

public class PushMsgService extends HmsMessageService {
    private static final String TAG = "PushService";
    // This method callback must be completed in 10 seconds. Otherwise, you need to start a new Job for callback processing.
    // extends HmsMessageService super class
    @Override
    public void onNewToken(String token) {
        Log.i(TAG, "received refresh token:" + token);
        // send the token to your app server.
        if (!TextUtils.isEmpty(token)) {
            refreshedTokenToServer(token);
        }
    }

    private void refreshedTokenToServer(String token) {
        Log.i(TAG, "sending token to server. token:" + token);
    }

    @Override
    public Context getApplicationContext() {
        return super.getApplicationContext();
    }

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        Log.i("sss", "aaaaaaa");

        if (remoteMessage != null) {
            if (!remoteMessage.getData().isEmpty()) {
                Log.d("HMS", "Payload" + remoteMessage.getData());
            }

            if (remoteMessage.getNotification() != null) {
                Log.d("HMS", "Message Notification Body: " + remoteMessage.getNotification().getBody());
            }
        }

    }
}

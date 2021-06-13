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

        /*FirebaseFirestore db = FirebaseFirestore.getInstance();
        HashMap<String, Object> userData = CustomUtils.getLocalUserData(getApplicationContext());
        if (userData != null && userData.containsKey("userID") && !userData.get("userID").toString().isEmpty()) {
            String userID = userData.get("userID").toString();
            db.collection("users").document(userID)
            .update("pushToken", token)
            .addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void aVoid) {
                    CustomUtils.saveLocalTokenData(getApplicationContext(), token);
                    Log.i(TAG, "sent token to server. token:" + token);
                    Toast.makeText(getApplicationContext(), "aaaaaaaaaaaahhhhhhh", Toast.LENGTH_LONG).show();
                }
            });
        }*/
    }

    @Override
    public Context getApplicationContext() {
        return super.getApplicationContext();
    }

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

    }
}

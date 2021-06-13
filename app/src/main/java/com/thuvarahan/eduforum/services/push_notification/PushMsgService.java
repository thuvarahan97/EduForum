/*
 *  Copyright 2020. Huawei Technologies Co., Ltd. All rights reserved.
 *  This subclass is created by HMS Core Toolkit 
 *  and used to receive token information or messages returned by HMS server
 *
 */
package com.thuvarahan.eduforum.services.push_notification;

import android.text.TextUtils;
import android.util.Log;

import com.huawei.hms.push.HmsMessageService;

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
}

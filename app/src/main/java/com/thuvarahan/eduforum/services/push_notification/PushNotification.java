package com.thuvarahan.eduforum.services.push_notification;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.text.Spanned;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.gson.JsonObject;
import com.huawei.hms.aaid.HmsInstanceId;
import com.huawei.hms.common.ApiException;
import com.squareup.okhttp.Callback;
import com.squareup.okhttp.Headers;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;
import com.squareup.okhttp.ResponseBody;
import com.thuvarahan.eduforum.CustomUtils;
import com.thuvarahan.eduforum.services.api.ApiClient;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class PushNotification {

    private static final String TAG = "PushNotification";
    private static final String appId = "104395687";

    public static void getToken(Context context) {
        new Thread() {
            @Override
            public void run() {
                try {
                    // read from agconnect-services.json
                    String token = HmsInstanceId.getInstance(context).getToken(appId, "HCM");
                    Log.i(TAG, "get token:" + token);
                    if (!TextUtils.isEmpty(token)) {
                        sendRegTokenToServer(context, token);
                    }
                } catch (ApiException e) {
                    Log.e(TAG, "get token failed, " + e);
                }
            }
        }.start();
    }

    public static void sendRegTokenToServer(Context context, String token) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        HashMap<String, Object> userData = CustomUtils.getLocalUserData(context);
        if (userData != null && userData.containsKey("userID") && !userData.get("userID").toString().isEmpty()) {
            String userID = userData.get("userID").toString();
            db.collection("users").document(userID)
            .update("pushToken", token)
            .addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void aVoid) {
                    CustomUtils.saveLocalTokenData(context, token);
                    Log.i(TAG, "sent token to server. token:" + token);
                    Toast.makeText(context, "aaaaaaaaaaaahhhhhhh", Toast.LENGTH_LONG).show();
                }
            });
        }
    }

    public static void getIntentData(Context context, Intent intent) {
        if (intent != null) {
            // You can use the following three lines of code to obtain the values for dotting statistics:
            String msgid = intent.getStringExtra("_push_msgid");
            String cmdType = intent.getStringExtra("_push_cmd_type");
            int notifyId = intent.getIntExtra("_push_notifyid", -1);
            Bundle bundle = intent.getExtras();
            if (bundle != null) {
                for (String key : bundle.keySet()) {
                    String content = bundle.getString(key);
                    Log.i(TAG, "receive data from push, key = " + key + ", content = " + content);
                }
            }
            Log.i(TAG, "receive data from push, msgId = " + msgid + ", cmd = " + cmdType + ", notifyId = " + notifyId);
        } else {
            Log.i(TAG, "intent is null");
        }
    }

    public static void sendNotification(String senderToken, String requestBody) {
        String url = "https://push-api.cloud.huawei.com/v1/"+ appId +"/messages:send";
        String auth = "Bearer" + " " + "CgB6e3x91Oi4+I9H1pxX7oPaTLLEIK6plh++1q3NiNFzD9B0MVwwFyZy2PYL2NzIbt89MwlEsA4/cbBHojkV3egF";

        MediaType JSON = MediaType.parse("application/json; charset=utf-8");
        RequestBody body = RequestBody.create(JSON, requestBody);

        OkHttpClient client = new OkHttpClient();
        client.setConnectTimeout(20, TimeUnit.SECONDS);
        client.setWriteTimeout(20, TimeUnit.SECONDS);
        client.setReadTimeout(30, TimeUnit.SECONDS);

        Request request = new Request.Builder()
                .url(url)
                .addHeader("Content-Type", "application/json")
                .addHeader("Authorization", auth)
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Request request, IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(Response response) throws IOException {
                try (ResponseBody responseBody = response.body()) {
                    if (response.isSuccessful()) {
                        /*Headers responseHeaders = response.headers();
                        for (int i = 0, size = responseHeaders.size(); i < size; i++) {
                            System.out.println(responseHeaders.name(i) + ": " + responseHeaders.value(i));
                        }*/
                        Log.i("Post request response: ", responseBody.string());
                    }
                }}
        });
    }

    public static String getNotificationRequestBody(String authorName, String postID, String receiverToken) {
        String notifTitle = "New answer";
        String notifBody = authorName + " has answered your question.";
//        List<String> msgTokens = new ArrayList<>();
//        msgTokens.add(receiverToken);

//        String[] msgTokens = new String[1];
//        msgTokens[0] = receiverToken;

        /*JSONObject data = new JSONObject();
        try {
            data.put("postID", postID);
        } catch (JSONException e) {
            e.printStackTrace();
        }*/

        JSONObject params = new JSONObject();
        try {
            params.put("postID", postID);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        JSONObject pushBody = new JSONObject();
        try {
            pushBody.put("params", params);
            pushBody.put("page", "/");
            pushBody.put("title", notifTitle);
            pushBody.put("description", notifBody);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        JSONObject data = new JSONObject();
        try {
            data.put("pushtype", 0);
            data.put("pushbody", pushBody);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        /*JSONObject notification = new JSONObject();
        try {
            notification.put("title", notifTitle);
            notification.put("body", notifBody);
        } catch (JSONException e) {
            e.printStackTrace();
        }*/

        JSONObject androidConfig = new JSONObject();
        try {
            androidConfig.put("fast_app_target", 1);
            androidConfig.put("collapse_key", -1);
            androidConfig.put("delivery_priority", "HIGH");
            androidConfig.put("ttl", "1448s");
            androidConfig.put("bi_tag", "Trump");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        JSONArray msgTokens = new JSONArray();
        msgTokens.put(receiverToken);

        JSONObject message = new JSONObject();
        try {
            message.put("data", data.toString());
//            message.put("notification", notification);
            message.put("android", androidConfig);
            message.put("token", msgTokens);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        JSONObject body = new JSONObject();
        try {
            body.put("message", message);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        System.out.println("jsonString: "+ body.toString());

        return body.toString();
    }

    private static Spanned getNotificationBody(String boldText, String normalText) {
        return Html.fromHtml("<b>" + boldText + "</b>" + normalText);
    }

}

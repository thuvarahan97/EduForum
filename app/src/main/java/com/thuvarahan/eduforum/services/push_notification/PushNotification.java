package com.thuvarahan.eduforum.services.push_notification;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Html;
import android.text.Spanned;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.huawei.hms.aaid.HmsInstanceId;
import com.huawei.hms.common.ApiException;
import com.squareup.okhttp.Callback;
import com.squareup.okhttp.FormEncodingBuilder;
import com.squareup.okhttp.Headers;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;
import com.squareup.okhttp.ResponseBody;
import com.thuvarahan.eduforum.CustomUtils;
import com.thuvarahan.eduforum.data.post.Post;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

public class PushNotification {

    private static final String TAG = "PushNotification";
    private static final String appId = "104395687";
    private static final String appSecret = "0858571074f2a139b4900f7f4336b411c1808e60cefd7978c773c3cd8038c409";

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

    public static void sendNotification(String receiverToken, String authorName, String postID) {
        String accessUrl = "https://oauth-login.cloud.huawei.com/oauth2/v3/token";
        String pushUrl = "https://push-api.cloud.huawei.com/v1/"+ appId +"/messages:send";

        MediaType JSON = MediaType.parse("application/json; charset=utf-8");

        String pushBody = getNotificationRequestBody(authorName, postID, receiverToken);

        if (pushBody != null && !pushBody.trim().isEmpty()) {
            RequestBody pushRequestBody = RequestBody.create(JSON, pushBody);

            RequestBody accessFormBody = new FormEncodingBuilder()
                    .add("grant_type", "client_credentials")
                    .add("client_id", appId)
                    .add("client_secret", appSecret)
                    .build();

            OkHttpClient client = new OkHttpClient();
            client.setConnectTimeout(20, TimeUnit.SECONDS);
            client.setWriteTimeout(20, TimeUnit.SECONDS);
            client.setReadTimeout(30, TimeUnit.SECONDS);

            Request request = new Request.Builder()
                    .url(accessUrl)
                    .addHeader("Content-Type", "application/x-www-form-urlencoded")
                    .addHeader("POST", "/oauth2/v3/token   HTTP/1.1")
                    .addHeader("Host", "oauth-login.cloud.huawei.com")
                    .post(accessFormBody)
                    .build();

            new Thread() {
                @Override
                public void run() {
                    client.newCall(request).enqueue(new Callback() {
                        @Override
                        public void onFailure(Request request, IOException e) {
                            e.printStackTrace();
                        }

                        @Override
                        public void onResponse(Response response) throws IOException {
                            if (response.isSuccessful()) {
                                ResponseBody responseBody = response.body();
                                String responseString = responseBody.string();
                                Log.i("Post request response: ", responseString);

                                JSONObject responseObj = null;
                                try {
                                    responseObj = new JSONObject(responseString);
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }

                                if (responseObj.has("access_token")) {
                                    String accessToken = null;
                                    try {
                                        accessToken = responseObj.getString("access_token");
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    }
                                    accessToken = accessToken.replaceAll("\'\'", "");
                                    String pushAuth = "Bearer" + " " + accessToken;

                                    Request request = new Request.Builder()
                                            .url(pushUrl)
                                            .addHeader("Content-Type", "application/json")
                                            .addHeader("Authorization", pushAuth)
                                            .post(pushRequestBody)
                                            .build();

                                    client.newCall(request).enqueue(new Callback() {
                                        @Override
                                        public void onFailure(Request request, IOException e) {
                                            e.printStackTrace();
                                        }

                                        @Override
                                        public void onResponse(Response response) throws IOException {
                                            if (response.isSuccessful()) {
                                                /*Headers responseHeaders = response.headers();
                                                for (int i = 0, size = responseHeaders.size(); i < size; i++) {
                                                    System.out.println(responseHeaders.name(i) + ": " + responseHeaders.value(i));
                                                }*/
                                                ResponseBody responseBody = response.body();
                                                Log.i("Post request response: ", responseBody.string());
                                            }
                                        }
                                    });
                                }
                            }
                        }
                    });
                }
            }.start();
        }
    }

    public static String getNotificationRequestBody(String authorName, String postID, String receiverToken) {
        String notifTitle = "New answer";
        String notifBody = authorName + " has answered your question.";
        String intent = getIntentParameter(postID);
        int intentType = 3;

        /*JSONObject data = new JSONObject();
        try {
            data.put("postID", postID);
        } catch (JSONException e) {
            e.printStackTrace();
        }*/

        /*JSONObject params = new JSONObject();
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
        }*/

        JSONObject notification = new JSONObject();
        try {
            notification.put("title", notifTitle);
            notification.put("body", notifBody);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        JSONObject clickAction = new JSONObject();
        try {
            clickAction.put("type", intentType);
//            clickAction.put("intent", intent);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        JSONObject androidNotification = new JSONObject();
        try {
            androidNotification.put("title", notifTitle);
            androidNotification.put("body", notifBody);
            androidNotification.put("click_action", clickAction);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        JSONObject androidConfig = new JSONObject();
        try {
//            androidConfig.put("fast_app_target", 1);
//            androidConfig.put("collapse_key", -1);
//            androidConfig.put("delivery_priority", "HIGH");
//            androidConfig.put("ttl", "1448s");
//            androidConfig.put("bi_tag", "Trump");
            androidConfig.put("notification", androidNotification);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        JSONArray msgTokens = new JSONArray();
        msgTokens.put(receiverToken);

        JSONObject message = new JSONObject();
        try {
            message.put("validate_only", false);
//            message.put("data", data.toString());
            message.put("notification", notification);
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

    private static Spanned getNotificationBodyMsg(String boldText, String normalText) {
        return Html.fromHtml("<b>" + boldText + "</b>" + normalText);
    }

    public static String getIntentParameter(String postID) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        // Define a URI scheme, for example, pushscheme://com.huawei.codelabpush/deeplink?.
        intent.setData(Uri.parse("pushscheme://com.huawei.codelabpush/deeplink?"));

        // Add parameters to the intent as required.
        intent.putExtra("id", postID);

        // The following flag is mandatory. If it is not added, duplicate messages may be displayed.
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        String intentUri = intent.toUri(Intent.URI_INTENT_SCHEME);

        // The value of intentUri will be assigned to the intent parameter in the message to be sent.
        Log.d("intentUri", intentUri);

        return intentUri;
    }


}

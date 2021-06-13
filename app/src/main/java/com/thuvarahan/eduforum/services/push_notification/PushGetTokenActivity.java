package com.thuvarahan.eduforum.services.push_notification;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.huawei.agconnect.config.AGConnectServicesConfig;
import com.huawei.hms.aaid.HmsInstanceId;
import com.huawei.hms.common.ApiException;
import com.thuvarahan.eduforum.CustomUtils;
import com.thuvarahan.eduforum.R;

import java.util.HashMap;

public class PushGetTokenActivity extends Activity implements View.OnClickListener {

    private static final String TAG = "PushGetTokenActivity";
    private TextView mtokenInforView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_push_get_token);
        mtokenInforView = findViewById(R.id.tokeninfo);

        findViewById(R.id.btn_get_token).setOnClickListener(this);
        findViewById(R.id.btn_delete_token).setOnClickListener(this);
        getIntentData(getIntent());
    }

    @Override
    public void onClick(View view) {
        int viewId = view.getId();
        switch (viewId) {
            case R.id.btn_get_token:
                getToken();
                break;
            case R.id.btn_delete_token:
                deleteToken();
                break;
            default:
                break;
        }
    }

    private void getToken() {
        new Thread() {
            @Override
            public void run() {
                try {
                    // read from agconnect-services.json
                    String appId = "104395687";
                    String token = HmsInstanceId.getInstance(getApplicationContext()).getToken(appId, "HCM");
                    Log.i(TAG, "get token:" + token);
                    // 1. After a token is obtained by using the getToken method, null judgment must be performed.
                    // 2. In the outer code segment of the getToken method, you need to add exception capture processing.
                    if (!TextUtils.isEmpty(token)) {
                        sendRegTokenToServer(token);
                    }

                    mtokenInforView.append("\n" +"get token:" + token);
                } catch (ApiException e) {
                    Log.e(TAG, "get token failed, " + e);
                    mtokenInforView.append("\n" +"get token failed, " + e);
                }
            }
        }.start();
    }

    private void sendRegTokenToServer(String token) {
        Toast.makeText(getApplicationContext(), token.toString(), Toast.LENGTH_LONG).show();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        HashMap<String, Object> userData = CustomUtils.getLocalUserData(getApplicationContext());
        if (userData != null && userData.containsKey("userID") && !userData.get("userID").toString().isEmpty()) {
            String userID = userData.get("userID").toString();
            db.collection("users").document(userID)
            .update("pushToken", token)
            .addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void aVoid) {
                    Log.i(TAG, "sent token to server. token:" + token);
                }
            });
        }
    }

    private void deleteToken() {
        new Thread() {
            @Override
            public void run() {
                try {
                    // read from agconnect-services.json
                    String appId = AGConnectServicesConfig.fromContext(PushGetTokenActivity.this).getString("client/app_id");
                    HmsInstanceId.getInstance(PushGetTokenActivity.this).deleteToken(appId, "HCM");
                    Log.i(TAG, "deleteToken success.");
                    mtokenInforView.append("\n" + "deleteToken success");
                } catch (ApiException e) {
                    Log.e(TAG, "deleteToken failed." + e);
                    mtokenInforView.append("\n" + "deleteToken failed." + e);
                }
            }
        }.start();
    }

    private void getIntentData(Intent intent) {
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
}

package com.thuvarahan.eduforum;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;

import com.thuvarahan.eduforum.data.login.LoginDataSource;
import com.thuvarahan.eduforum.data.login.LoginRepository;
import com.thuvarahan.eduforum.data.user.User;
import com.thuvarahan.eduforum.services.network_broadcast.NetworkChangeReceiver;
import com.thuvarahan.eduforum.services.push_notification.PushNotification;
import com.thuvarahan.eduforum.ui.login.LoginActivity;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                checkUserLoggedIn();
            }
        }, 1000);

        //-------------- Check Push Token -------------//
        String pushToken = CustomUtils.getLocalTokenData(getApplicationContext());
        if (pushToken == null || pushToken.isEmpty() || pushToken.trim().equals("")) {
            PushNotification.getToken(getApplicationContext());
        }

        //-------------- Register NetworkChangeReceiver------------//
        IntentFilter intentFilter = new IntentFilter("android.net.conn.CONNECTIVITY_CHANGE");
        this.registerReceiver(new NetworkChangeReceiver(), intentFilter);
    }

    //----------- Check User Logged-In -------------//
    private void checkUserLoggedIn() {
        LoginRepository loginRepository = LoginRepository.getInstance(new LoginDataSource());
        HashMap<String, Object> userData = CustomUtils.getLocalUserData(getApplicationContext());
        if (userData != null && userData.containsKey("userID") && !userData.get("userID").toString().isEmpty()) {
            String userID = userData.get("userID").toString();
            String displayName = userData.get("displayName").toString();
            String username = userData.get("username").toString();
            int userType = Integer.parseInt(userData.get("userType").toString());
            Date dateCreated = new Date();
            try {
                dateCreated = new SimpleDateFormat("dd MMM yyyy hh:mm a", Locale.ENGLISH).parse(userData.get("dateCreated").toString());
            } catch (ParseException e) {
                e.printStackTrace();
            }
            User currentUser = new User(userID, displayName, username, userType, dateCreated);
            loginRepository.setLoggedInUser(currentUser);
            Intent intent = new Intent(SplashActivity.this, MainActivity.class);
            startActivity(intent);
        } else {
            Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
            startActivity(intent);
        }
        finish();
        overridePendingTransition(0, 0);
    }
}
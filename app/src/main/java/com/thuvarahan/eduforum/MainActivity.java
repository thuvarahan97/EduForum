package com.thuvarahan.eduforum;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.thuvarahan.eduforum.data.login.LoginDataSource;
import com.thuvarahan.eduforum.data.login.LoginRepository;
import com.thuvarahan.eduforum.data.post.Post;
import com.thuvarahan.eduforum.data.user.User;
import com.thuvarahan.eduforum.services.network_broadcast.NetworkChangeReceiver;
import com.thuvarahan.eduforum.services.push_notification.PushNotification;
import com.thuvarahan.eduforum.ui.login.LoginActivity;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //----------- Check User Logged-In -------------//
        LoginRepository loginRepository = LoginRepository.getInstance(new LoginDataSource());
        HashMap<String, Object> userData = CustomUtils.getLocalUserData(getApplicationContext());
        if (userData != null && userData.containsKey("userID") && !userData.get("userID").toString().isEmpty()) {
            String userID = userData.get("userID").toString();
            String displayName = userData.get("displayName").toString();
            String username = userData.get("username").toString();
            Date dateCreated = new Date();
            try {
                dateCreated = new SimpleDateFormat("dd MMM yyyy hh:mm a", Locale.ENGLISH).parse(userData.get("dateCreated").toString());
            } catch (ParseException e) {
                e.printStackTrace();
            }
            User currentUser = new User(userID, displayName, username, dateCreated);
            loginRepository.setLoggedInUser(currentUser);
        } else {
            Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
            startActivity(intent);
            finish();
            return;
        }


        //-------------- Check Push Token -------------//
        String pushToken = CustomUtils.getLocalTokenData(getApplicationContext());
        if (pushToken == null || pushToken.isEmpty() || pushToken.trim().equals("")) {
            PushNotification.getToken(getApplicationContext());
        }


        //------------ Initialize Fragments -----------//
        BottomNavigationView navView = findViewById(R.id.nav_view);
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.navigation_home, R.id.navigation_profile, R.id.navigation_notifications)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        NavigationUI.setupWithNavController(navView, navController);
    }
}
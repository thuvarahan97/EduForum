package com.thuvarahan.eduforum.services.push_notification;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.thuvarahan.eduforum.R;


public class UsingIntentOpenActivityForMethodOne extends Activity {
    private static final String TAG = "UsingIntentOpenActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_using_intent_activity_link);
        getIntentData(getIntent());
    }

    // Customize a class. This activity class is only an example.
    private void getIntentData(Intent intent) {
        if (intent != null) {
            // Directly add parameters to the Intent. Obtain data as follows:
             String name = intent.getStringExtra("name");
             int age = intent.getIntExtra("age", -1);
            Log.i(TAG, "name " + name + ",age " + age);
            Toast.makeText(this, "name " + name + ",age " + age, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        getIntentData(intent);
    }
}

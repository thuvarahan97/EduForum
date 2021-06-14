package com.thuvarahan.eduforum.services.push_notification;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.thuvarahan.eduforum.MainActivity;
import com.thuvarahan.eduforum.PostActivity;
import com.thuvarahan.eduforum.R;
import com.thuvarahan.eduforum.data.post.Post;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


public class DeeplinkActivity extends Activity {
    private static final String TAG = "UsingIntentOpenActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_deeplink);
        getIntentData(getIntent());
    }

    // Customize a class. This activity class is only an example.
    private void getIntentData(Intent intent) {
        if (intent != null) {
            Log.i("Deeplink", "Intent received.");

            FirebaseFirestore db = FirebaseFirestore.getInstance();

            String postID = intent.getStringExtra("id");
            Log.i(TAG, "postID " + postID);

            if (postID != null && !postID.trim().isEmpty()) {
                db.collection("posts").document(postID).get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot result) {
                        if (result != null && result.exists()) {
                            String id = result.getId();
                            String title = Objects.requireNonNull(result.get("postTitle")).toString();
                            String body = Objects.requireNonNull(result.get("postBody")).toString();
                            DocumentReference authorRef = (DocumentReference) result.get("postAuthor");
                            Timestamp timestamp = (Timestamp) result.get("timestamp");
                            ArrayList<String> images = new ArrayList<>((List<String>) result.get("postImages"));

                            assert authorRef != null;
                            assert timestamp != null;

                            //-------- Go to Post Activity -------//
                            Post _post = new Post(id, title, body, authorRef.getPath(), timestamp, images);
                            Intent intent = new Intent(getApplicationContext(), PostActivity.class);
                            intent.putExtra("post", _post);
                            startActivity(intent);
                        } else {
                            finish();
                        }
                    }
                });
            }
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        getIntentData(intent);
    }

    /*@Override
    protected void onResume() {
        super.onResume();
        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }*/
}

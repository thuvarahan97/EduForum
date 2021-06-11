package com.thuvarahan.eduforum.data.post;

import android.text.format.DateFormat;

import com.google.firebase.Timestamp;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class Post implements Serializable {
    public String id;
    public String title;
    public String body;
    public String authorRef;
//    public String author;
    public Date timestamp;
    public ArrayList<String> images;
//    public ArrayList<Reply> replies;

    public Post(String id, String title, String body, String authorRef, Timestamp timestamp, ArrayList<String> images) {
        this.id = id;
        this.title = title;
        this.body = body;
        this.authorRef = authorRef;
//        this.author = "Unknown";
        this.timestamp = timestamp.toDate();
        this.images = images;
//        this.replies = replies;
    }

    /*public void fetchAuthor(FirebaseFirestore db) {
        db.document(authorRef).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    DocumentSnapshot doc = task.getResult();
                    if (doc.getData() != null && doc.getData().get("displayName") != null) {
                        author = doc.getData().get("displayName").toString();
                    }
                    else {
                        author = "Unknown";
                    }
                }
                else {
                    author = "Unknown";
                }
            }
        });
    }*/

    /*public void fetchReplies(FirebaseFirestore db) {
        String TAG = "Replies Fetch: ";

        db.collection("posts")
        .document(id)
        .collection("replies")
        .get()
        .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if (task.isSuccessful()) {
                    QuerySnapshot result = task.getResult();
                    if (result == null || result.isEmpty()) {
                        return;
                    }

                    replies.clear();

                    for (QueryDocumentSnapshot document : result) {
                        Log.d(TAG, document.getId() + " => " + document.getData());

                        Map<String, Object> data = document.getData();
                        String id = document.getId().toString();
                        String body = Objects.requireNonNull(data.get("replyBody")).toString();
                        DocumentReference author = (DocumentReference) data.get("replyAuthor");
                        Timestamp timestamp = (Timestamp) data.get("timestamp");

                        assert author != null;
                        assert timestamp != null;
                        Reply reply = new Reply(id, body, author, timestamp);
                        replies.add(reply);
                    }
                } else {
                    Log.w(TAG, "Error getting documents.", task.getException());
                }
            }
        });
    }*/
}

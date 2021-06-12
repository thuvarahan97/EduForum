package com.thuvarahan.eduforum.data.reply;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;

import java.util.Date;

public class Reply {
    public String id;
    public String body;
    public DocumentReference author;
    public Date timestamp;
    public String postID;

    public Reply(String id, String body, DocumentReference author, Timestamp timestamp, String postID) {
        this.id = id;
        this.body = body;
        this.author = author;
        this.timestamp = timestamp.toDate();
        this.postID = postID;
    }
}

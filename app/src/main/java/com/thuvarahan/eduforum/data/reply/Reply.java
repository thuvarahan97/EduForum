package com.thuvarahan.eduforum.data.reply;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;

import java.util.ArrayList;
import java.util.Date;

public class Reply {
    public String id;
    public String body;
    public DocumentReference author;
    public Date timestamp;
    public String postID;
    public ArrayList<String> images;

    public Reply(String id, String body, DocumentReference author, Timestamp timestamp, String postID, ArrayList<String> images) {
        this.id = id;
        this.body = body;
        this.author = author;
        this.timestamp = timestamp.toDate();
        this.postID = postID;
        this.images = images;
    }
}

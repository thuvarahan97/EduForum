package com.thuvarahan.eduforum.data.notification;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;

import java.util.Date;

public class Notification {
    public String id;
    public DocumentReference post;
    public DocumentReference author;
    public Date timestamp;
    public boolean isChecked;

    public Notification(String id, DocumentReference post, DocumentReference author, Timestamp timestamp, boolean isChecked) {
        this.id = id;
        this.post = post;
        this.author = author;
        this.timestamp = timestamp.toDate();
        this.isChecked = isChecked;
    }
}

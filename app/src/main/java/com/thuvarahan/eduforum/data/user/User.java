package com.thuvarahan.eduforum.data.user;

import java.util.Date;

public class User {
    private String userID;
    private String displayName;
    private String username;
    private Date dateCreated;

    public User(String userID, String displayName, String username, Date dateCreated) {
        this.userID = userID;
        this.displayName = displayName;
        this.username = username;
        this.dateCreated = dateCreated;
    }

    public String getUserID() {
        return userID;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getUsername() {
        return username;
    }

    public Date getDateCreated() {
        return dateCreated;
    }
}

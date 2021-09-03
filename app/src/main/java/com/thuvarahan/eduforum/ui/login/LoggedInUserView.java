package com.thuvarahan.eduforum.ui.login;

import com.thuvarahan.eduforum.utils.CustomUtils;

import java.util.Date;

/**
 * Class exposing authenticated user details to the UI.
 */
class LoggedInUserView {
    private String userID;
    private String displayName;
    private String username;
    private int userType;
    private String dateCreated;
    //... other data fields that may be accessible to the UI

    LoggedInUserView(String userID, String displayName, String username, int userType, Date dateCreated) {
        this.userID = userID;
        this.displayName = displayName;
        this.username = username;
        this.userType = userType;
        this.dateCreated = CustomUtils.formatTimestamp(dateCreated);
    }

    String getDisplayName() {
        return displayName;
    }

    String getUserID() {
        return userID;
    }

    String getUsername() {
        return username;
    }

    int getUserType() {
        return userType;
    }

    String getDateCreated() {
        return dateCreated;
    }
}
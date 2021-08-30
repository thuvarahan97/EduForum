package com.thuvarahan.eduforum.data.login;

import android.content.Context;
import android.content.Intent;

import androidx.activity.result.ActivityResultLauncher;

import com.thuvarahan.eduforum.data.login.model.LoggedInUser;
import com.thuvarahan.eduforum.data.user.User;
import com.thuvarahan.eduforum.interfaces.ILoginUserTask;

/**
 * Class that requests authentication and user information from the remote data source and
 * maintains an in-memory cache of login status and user credentials information.
 */
public class LoginRepository {

    private static volatile LoginRepository instance;

    private LoginDataSource dataSource;

    // If user credentials will be cached in local storage, it is recommended it be encrypted
    // @see https://developer.android.com/training/articles/keystore
    private User user = null;

    // private constructor : singleton access
    private LoginRepository(LoginDataSource dataSource) {
        this.dataSource = dataSource;
    }

    public static LoginRepository getInstance(LoginDataSource dataSource) {
        if (instance == null) {
            instance = new LoginRepository(dataSource);
        }
        return instance;
    }

    public boolean isLoggedIn() {
        return user != null;
    }

    public void logout() {
        int userType = user.getUserType();
        user = null;
        if (userType == 1) {
            dataSource.logout();
        } else if (userType == 2) {
            dataSource.logoutHwId();
        }
    }

    public User getUser() {
        return user;
    }

    public void setLoggedInUser(User user) {
        this.user = user;
        // If user credentials will be cached in local storage, it is recommended it be encrypted
        // @see https://developer.android.com/training/articles/keystore
    }

    public void login(String username, String password, ILoginUserTask userTask) {
        // handle login
        dataSource.login(username, password, new ILoginUserTask() {
            @Override
            public void onReturn(Result result) {
                if (result instanceof Result.Success) {
                    setLoggedInUser(((Result.Success<User>) result).getData());
                }
                userTask.onReturn(result);
            }
        });
    }

    public void loginHwId(Context context, ActivityResultLauncher<Intent> loginHwIdActivityResult, ILoginUserTask userTask) {
        // handle login
        dataSource.loginHwId(context, loginHwIdActivityResult, new ILoginUserTask() {
            @Override
            public void onReturn(Result result) {
                if (result instanceof Result.Success) {
                    setLoggedInUser(((Result.Success<User>) result).getData());
                }
                userTask.onReturn(result);
            }
        });
    }

    public void loginHwId(Context context, Intent data, ILoginUserTask userTask) {
        // handle login
        dataSource.loginHwId(context, data, new ILoginUserTask() {
            @Override
            public void onReturn(Result result) {
                if (result instanceof Result.Success) {
                    setLoggedInUser(((Result.Success<User>) result).getData());
                }
                userTask.onReturn(result);
            }
        });
    }
}
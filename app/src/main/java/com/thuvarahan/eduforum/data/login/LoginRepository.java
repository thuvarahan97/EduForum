package com.thuvarahan.eduforum.data.login;

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
        user = null;
        dataSource.logout();
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
}
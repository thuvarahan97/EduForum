package com.thuvarahan.eduforum.data.register;

import com.thuvarahan.eduforum.data.register.model.RegisteredUser;

/**
 * Class that requests authentication and user information from the remote data source and
 * maintains an in-memory cache of login status and user credentials information.
 */
public class RegisterRepository {

    private static volatile RegisterRepository instance;

    private RegisterDataSource dataSource;

    // If user credentials will be cached in local storage, it is recommended it be encrypted
    // @see https://developer.android.com/training/articles/keystore
    private RegisteredUser user = null;

    // private constructor : singleton access
    private RegisterRepository(RegisterDataSource dataSource) {
        this.dataSource = dataSource;
    }

    public static RegisterRepository getInstance(RegisterDataSource dataSource) {
        if(instance == null){
            instance = new RegisterRepository(dataSource);
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

    private void setLoggedInUser(RegisteredUser user) {
        this.user = user;
        // If user credentials will be cached in local storage, it is recommended it be encrypted
        // @see https://developer.android.com/training/articles/keystore
    }

    public Result<RegisteredUser> register(String displayName, String username, String password, String confirmPassword) {
        // handle login
        Result<RegisteredUser> result = dataSource.register(displayName, username, password, confirmPassword);
        if (result instanceof Result.Success) {
            setLoggedInUser(((Result.Success<RegisteredUser>) result).getData());
        }
        return result;
    }
}
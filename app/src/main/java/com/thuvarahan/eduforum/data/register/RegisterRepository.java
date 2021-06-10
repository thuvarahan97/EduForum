package com.thuvarahan.eduforum.data.register;

import com.thuvarahan.eduforum.data.register.model.RegisteredUser;
import com.thuvarahan.eduforum.data.user.User;
import com.thuvarahan.eduforum.interfaces.IRegisterUserTask;

/**
 * Class that requests authentication and user information from the remote data source and
 * maintains an in-memory cache of login status and user credentials information.
 */
public class RegisterRepository {

    private static volatile RegisterRepository instance;

    private RegisterDataSource dataSource;

    // If user credentials will be cached in local storage, it is recommended it be encrypted
    // @see https://developer.android.com/training/articles/keystore
    private User user = null;

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

    private void setRegisteredUser(User user) {
        this.user = user;
        // If user credentials will be cached in local storage, it is recommended it be encrypted
        // @see https://developer.android.com/training/articles/keystore
    }

    public void register(String displayName, String username, String password, String confirmPassword, IRegisterUserTask userTask) {
        // handle register
        dataSource.register(displayName, username, password, confirmPassword, new IRegisterUserTask() {
            @Override
            public void onReturn(Result result) {
                if (result instanceof Result.Success) {
                    setRegisteredUser(((Result.Success<User>) result).getData());
                }
                userTask.onReturn(result);
            }
        });
    }
}
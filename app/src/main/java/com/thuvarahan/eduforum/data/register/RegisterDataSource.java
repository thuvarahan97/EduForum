package com.thuvarahan.eduforum.data.register;

import com.thuvarahan.eduforum.data.register.model.RegisteredUser;

import java.io.IOException;

/**
 * Class that handles authentication w/ login credentials and retrieves user information.
 */
public class RegisterDataSource {

    public Result<RegisteredUser> register(String displayName, String username, String password, String confirmPassword) {

        try {
            // TODO: handle loggedInUser authentication
            RegisteredUser fakeUser =
                    new RegisteredUser(
                            java.util.UUID.randomUUID().toString(),
                            "Jane Doe");
            return new Result.Success<>(fakeUser);
        } catch (Exception e) {
            return new Result.Error(new IOException("Error logging in", e));
        }
    }

    public void logout() {
        // TODO: revoke authentication
    }
}
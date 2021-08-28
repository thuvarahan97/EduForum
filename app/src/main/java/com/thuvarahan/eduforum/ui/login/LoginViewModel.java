package com.thuvarahan.eduforum.ui.login;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import android.content.Context;
import android.util.Patterns;

import com.thuvarahan.eduforum.R;
import com.thuvarahan.eduforum.data.login.LoginRepository;
import com.thuvarahan.eduforum.data.login.Result;
import com.thuvarahan.eduforum.data.login.model.LoggedInUser;
import com.thuvarahan.eduforum.data.user.User;
import com.thuvarahan.eduforum.interfaces.ILoginUserTask;

public class LoginViewModel extends ViewModel {

    private MutableLiveData<LoginFormState> loginFormState = new MutableLiveData<>();
    private MutableLiveData<LoginResult> loginResult = new MutableLiveData<>();
    private LoginRepository loginRepository;

    LoginViewModel(LoginRepository loginRepository) {
        this.loginRepository = loginRepository;
    }

    LiveData<LoginFormState> getLoginFormState() {
        return loginFormState;
    }

    LiveData<LoginResult> getLoginResult() {
        return loginResult;
    }

    public void login(String username, String password) {
        // can be launched in a separate asynchronous job
        loginRepository.login(username, password, new ILoginUserTask() {
            @Override
            public void onReturn(Result result) {
                if (result instanceof Result.Success) {
                    User data = ((Result.Success<User>) result).getData();
                    loginResult.setValue(new LoginResult(new LoggedInUserView(data.getUserID(), data.getDisplayName(), data.getUsername(), data.getDateCreated())));
                } else if (result instanceof Result.NotVerified) {
                    loginResult.setValue(new LoginResult(R.string.login_not_verified));
                } else if (result instanceof Result.Invalid) {
                    loginResult.setValue(new LoginResult(R.string.login_invalid_credentials));
                }  else {
                    loginResult.setValue(new LoginResult(R.string.login_failed));
                }
            }
        });
    }

    public void loginHwId(Context context) {
        // can be launched in a separate asynchronous job
        loginRepository.loginHwId(context, new ILoginUserTask() {
            @Override
            public void onReturn(Result result) {
                if (result instanceof Result.Success) {
                    User data = ((Result.Success<User>) result).getData();
                    loginResult.setValue(new LoginResult(new LoggedInUserView(data.getUserID(), data.getDisplayName(), data.getUsername(), data.getDateCreated())));
                } else if (result instanceof Result.NotVerified) {
                    loginResult.setValue(new LoginResult(R.string.login_not_verified));
                } else if (result instanceof Result.Invalid) {
                    loginResult.setValue(new LoginResult(R.string.login_invalid_credentials));
                }  else {
                    loginResult.setValue(new LoginResult(R.string.login_failed));
                }
            }
        });
    }

    public void loginDataChanged(String username, String password) {
        if (!isUserNameValid(username)) {
            loginFormState.setValue(new LoginFormState(R.string.invalid_username, null));
        } else if (!isPasswordValid(password)) {
            loginFormState.setValue(new LoginFormState(null, R.string.invalid_password));
        } else {
            loginFormState.setValue(new LoginFormState(true));
        }
    }

    // A placeholder username validation check
    private boolean isUserNameValid(String username) {
        return username != null && !username.trim().isEmpty() && Patterns.EMAIL_ADDRESS.matcher(username).matches();
    }

    // A placeholder password validation check
    private boolean isPasswordValid(String password) {
        return password != null && !password.trim().isEmpty() && password.trim().length() > 5;
    }
}
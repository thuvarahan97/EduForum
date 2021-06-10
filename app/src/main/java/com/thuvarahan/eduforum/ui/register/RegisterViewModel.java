package com.thuvarahan.eduforum.ui.register;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import android.util.Patterns;

import com.thuvarahan.eduforum.data.register.RegisterRepository;
import com.thuvarahan.eduforum.data.register.Result;
import com.thuvarahan.eduforum.data.register.model.RegisteredUser;
import com.thuvarahan.eduforum.R;
import com.thuvarahan.eduforum.data.user.User;
import com.thuvarahan.eduforum.interfaces.IRegisterUserTask;

public class RegisterViewModel extends ViewModel {

    private MutableLiveData<RegisterFormState> registerFormState = new MutableLiveData<>();
    private MutableLiveData<RegisterResult> registerResult = new MutableLiveData<>();
    private RegisterRepository registerRepository;

    RegisterViewModel(RegisterRepository registerRepository) {
        this.registerRepository = registerRepository;
    }

    LiveData<RegisterFormState> getRegisterFormState() {
        return registerFormState;
    }

    LiveData<RegisterResult> getRegisterResult() {
        return registerResult;
    }

    public void register(String displayName, String username, String password, String confirmPassword) {
        // can be launched in a separate asynchronous job
        registerRepository.register(displayName, username, password, confirmPassword, new IRegisterUserTask() {
            @Override
            public void onReturn(Result result) {
                if (result instanceof Result.Success) {
                    User data = ((Result.Success<User>) result).getData();
                    registerResult.setValue(new RegisterResult(new RegisteredUserView(data.getDisplayName())));
                } else if (result instanceof Result.Failure) {
                    registerResult.setValue(new RegisterResult(R.string.register_duplicate_failed));
                } else {
                    registerResult.setValue(new RegisterResult(R.string.register_failed));
                }
            }
        });
    }

    public void registerDataChanged(String displayName, String username, String password, String confirmPassword) {
        if (!isDisplayNameValid(displayName)) {
            registerFormState.setValue(new RegisterFormState(R.string.invalid_display_name, null, null, null));
        } else if (!isUserNameValid(username)) {
            registerFormState.setValue(new RegisterFormState(null, R.string.invalid_username, null, null));
        } else if (!isPasswordValid(password)) {
            registerFormState.setValue(new RegisterFormState(null, null, R.string.invalid_password, null));
        } else if (!isConfirmPasswordValid(password, confirmPassword)) {
            registerFormState.setValue(new RegisterFormState(null, null, null, R.string.invalid_confirm_password));
        } else {
            registerFormState.setValue(new RegisterFormState(true));
        }
    }

    // A placeholder display name validation check
    private boolean isDisplayNameValid(String displayName) {
        return displayName != null && !displayName.trim().isEmpty();
    }

    // A placeholder username validation check
    private boolean isUserNameValid(String username) {
        return username != null && !username.trim().isEmpty() && Patterns.EMAIL_ADDRESS.matcher(username).matches();
    }

    // A placeholder password validation check
    private boolean isPasswordValid(String password) {
        return password != null && !password.trim().isEmpty() && password.trim().length() > 5;
    }

    // A placeholder confirm password validation check
    private boolean isConfirmPasswordValid(String password, String confirmPassword) {
        return confirmPassword != null && confirmPassword.equals(password);
    }
}
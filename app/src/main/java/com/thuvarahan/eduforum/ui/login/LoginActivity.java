package com.thuvarahan.eduforum.ui.login;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.huawei.hms.support.hwid.ui.HuaweiIdAuthButton;
import com.thuvarahan.eduforum.CustomUtils;
import com.thuvarahan.eduforum.MainActivity;
import com.thuvarahan.eduforum.R;
import com.thuvarahan.eduforum.services.push_notification.PushNotification;
import com.thuvarahan.eduforum.ui.register.RegisterActivity;

public class LoginActivity extends AppCompatActivity {

    private LoginViewModel loginViewModel;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        loginViewModel = new ViewModelProvider(this, new LoginViewModelFactory())
                .get(LoginViewModel.class);

        final EditText usernameEditText = findViewById(R.id.username);
        final EditText passwordEditText = findViewById(R.id.password);
        final Button loginButton = findViewById(R.id.btnLogin);
        final ProgressBar loadingProgressBar = findViewById(R.id.loading);
        final ConstraintLayout registerNav = findViewById(R.id.registerNav);
        final HuaweiIdAuthButton loginHwIdButton = findViewById(R.id.btnLoginHwId);

        loginViewModel.getLoginFormState().observe(this, new Observer<LoginFormState>() {
            @Override
            public void onChanged(@Nullable LoginFormState loginFormState) {
                if (loginFormState == null) {
                    return;
                }
                loginButton.setEnabled(loginFormState.isDataValid());
                if (loginFormState.getUsernameError() != null) {
                    usernameEditText.setError(getString(loginFormState.getUsernameError()));
                }
                if (loginFormState.getPasswordError() != null) {
                    passwordEditText.setError(getString(loginFormState.getPasswordError()));
                }
            }
        });

        loginViewModel.getLoginResult().observe(this, new Observer<LoginResult>() {
            @Override
            public void onChanged(@Nullable LoginResult loginResult) {
                if (loginResult == null) {
                    return;
                }
                toggleProgressBar(loadingProgressBar, false);
                if (loginResult.getError() != null) {
                    showLoginFailed(loginResult.getError());

                    setResult(Activity.RESULT_OK);
                }
                if (loginResult.getSuccess() != null) {
                    updateUiWithUser(loginResult.getSuccess());

                    setResult(Activity.RESULT_OK);

                    // Go to Main Activity
                    Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                    startActivity(intent);

                    //Complete and destroy login activity once successful
                    finish();
                }

            }
        });

        TextWatcher afterTextChangedListener = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // ignore
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // ignore
            }

            @Override
            public void afterTextChanged(Editable s) {
                loginViewModel.loginDataChanged(usernameEditText.getText().toString(), passwordEditText.getText().toString());
            }
        };
        usernameEditText.addTextChangedListener(afterTextChangedListener);
        passwordEditText.addTextChangedListener(afterTextChangedListener);
        /*passwordEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {

            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    *//*loginViewModel.login(usernameEditText.getText().toString(),
                            passwordEditText.getText().toString());*//*
                    return true;
                }
                return false;
            }
        });*/

        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleProgressBar(loadingProgressBar, true);
                loginViewModel.login(usernameEditText.getText().toString(), passwordEditText.getText().toString());
            }
        });

        registerNav.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), RegisterActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
            }
        });

        loginHwIdButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleProgressBar(loadingProgressBar, true);
                loginViewModel.loginHwId(LoginActivity.this, loginHwIdActivityResult);
            }
        });
    }

    private void toggleProgressBar(ProgressBar loadingProgressBar, boolean value) {
        if (value) {
            loadingProgressBar.setVisibility(View.VISIBLE);
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE, WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
        } else {
            loadingProgressBar.setVisibility(View.GONE);
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
        }
    }

    private void updateUiWithUser(LoggedInUserView model) {
        CustomUtils.saveLocalUserData(getApplicationContext(), model.getUserID(), model.getDisplayName(), model.getUsername(), model.getUserType(), model.getDateCreated());
        PushNotification.getToken(getApplicationContext());
        String welcome = getString(R.string.welcome) + " " + model.getDisplayName() + " !";
        Toast.makeText(getApplicationContext(), welcome, Toast.LENGTH_LONG).show();
    }

    private void showLoginFailed(@StringRes Integer errorString) {
        Toast.makeText(getApplicationContext(), errorString, Toast.LENGTH_LONG).show();
    }

    ActivityResultLauncher<Intent> loginHwIdActivityResult = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        Intent intent = result.getData();
                        loginViewModel.loginHwId(LoginActivity.this, intent);
                    } else {
                        loginViewModel.loginHwId(LoginActivity.this, (Intent) null);
                    }
                }
            });

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }
}
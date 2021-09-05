package com.thuvarahan.eduforum;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.google.android.gms.tasks.OnCanceledListener;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.thuvarahan.eduforum.interfaces.IProgressBarTask;
import com.thuvarahan.eduforum.services.network_broadcast.NetworkChangeReceiver;
import com.thuvarahan.eduforum.ui.login.LoginActivity;
import com.thuvarahan.eduforum.utils.CustomUtils;

public class ForgotPasswordActivity extends AppCompatActivity {

    View rootView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);
        rootView = findViewById(android.R.id.content);

        EditText etUsername = findViewById(R.id.username);
        ConstraintLayout loginNav = findViewById(R.id.loginNav);
        Button btnResetPassword = findViewById(R.id.btnResetPassword);
        btnResetPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String username = etUsername.getText().toString().trim();
                if (username != null && !username.isEmpty()) {
                    sendEmail(username);
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
                String username = etUsername.getText().toString().trim();
                btnResetPassword.setEnabled(username != null && !username.isEmpty() && isUserNameValid(username));
            }
        };
        etUsername.addTextChangedListener(afterTextChangedListener);

        loginNav.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
            }
        });
    }

    private boolean isUserNameValid(String username) {
        return username != null && !username.trim().isEmpty() && Patterns.EMAIL_ADDRESS.matcher(username).matches();
    }

    private void sendEmail(String email) {
        if (NetworkChangeReceiver.isOnline(ForgotPasswordActivity.this)) {
            Dialog progressDialog = CustomUtils.createProgressDialog(ForgotPasswordActivity.this);
            IProgressBarTask progressBarTask = new IProgressBarTask() {
                @Override
                public void onStart() {
                    if (progressDialog != null && !progressDialog.isShowing()) {
                        progressDialog.show();
                        CustomUtils.toggleWindowInteraction(ForgotPasswordActivity.this, false);
                    }
                }

                @Override
                public void onComplete() {
                    if (progressDialog != null && progressDialog.isShowing()) {
                        progressDialog.dismiss();
                        CustomUtils.toggleWindowInteraction(ForgotPasswordActivity.this, true);
                    }
                }
            };
            progressBarTask.onStart();
            FirebaseAuth.getInstance().sendPasswordResetEmail(email)
                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if (task.isSuccessful()) {
                                Toast.makeText(ForgotPasswordActivity.this, "An email has been sent to reset your password.", Toast.LENGTH_LONG).show();
                                Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
                                startActivity(intent);
                                finish();
                            } else {
                                Toast.makeText(ForgotPasswordActivity.this, "Failed to reset password.", Toast.LENGTH_SHORT).show();
                            }
                            progressBarTask.onComplete();
                        }
                    }).addOnCanceledListener(new OnCanceledListener() {
                @Override
                public void onCanceled() {
                    Toast.makeText(ForgotPasswordActivity.this, "Failed to reset password.", Toast.LENGTH_SHORT).show();
                    progressBarTask.onComplete();
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Toast.makeText(ForgotPasswordActivity.this, "Failed to reset password.", Toast.LENGTH_SHORT).show();
                    progressBarTask.onComplete();
                }
            });
        } else {
            Snackbar.make(rootView, "No internet connection!", Snackbar.LENGTH_LONG)
                    .setBackgroundTint(Color.RED)
                    .setTextColor(Color.WHITE)
                    .show();
        }
    }

    @Override
    public void onBackPressed() {
//        super.onBackPressed();
        Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }
}
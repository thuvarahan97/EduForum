package com.thuvarahan.eduforum.data.login;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.huawei.hms.common.ApiException;
import com.huawei.hms.support.account.AccountAuthManager;
import com.huawei.hms.support.account.request.AccountAuthParams;
import com.huawei.hms.support.account.request.AccountAuthParamsHelper;
import com.huawei.hms.support.account.result.AuthAccount;
import com.huawei.hms.support.account.service.AccountAuthService;
import com.thuvarahan.eduforum.data.user.User;
import com.thuvarahan.eduforum.interfaces.ILoginUserTask;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Class that handles authentication w/ login credentials and retrieves user information.
 */
public class LoginDataSource {

    private final String TAG = "User Account";

    // Firebase Auth
    private final FirebaseAuth mAuth = FirebaseAuth.getInstance();
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    // Huawei Account Auth
    private AccountAuthService mAuthService;

    public void login(String username, String password, ILoginUserTask userTask) {
        try {
            mAuth.signInWithEmailAndPassword(username, password)
                    .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if (task.isSuccessful()) {
                                // Sign in success, update UI with the signed-in user's information
                                Log.d(TAG, "signInWithEmail:success");
                                FirebaseUser user = mAuth.getCurrentUser();

                                if (user != null && user.isEmailVerified()) {
                                    db.collection("users")
                                            .document(user.getUid())
                                            .get()
                                            .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                                                @Override
                                                public void onSuccess(DocumentSnapshot documentSnapshot) {
                                                    Map<String, Object> data = documentSnapshot.getData();
                                                    if (data == null) {
                                                        Exception exception = task.getException();
                                                        Log.w(TAG, "signInWithEmail:failure", exception);
                                                        userTask.onReturn(new Result.Error(new IOException("Error logging in", exception)));
                                                        return;
                                                    }
                                                    String userID = user.getUid();
                                                    String displayName = data.get("displayName").toString();
                                                    String emailAddress = user.getEmail();
                                                    Date dateCreated = ((Timestamp) data.get("dateCreated")).toDate();

                                                    Log.d(TAG, "DocumentSnapshot retrieved with ID: " + user.getUid());
                                                    User loggedInUser = new User(userID, displayName, emailAddress, 1, dateCreated);
                                                    userTask.onReturn(new Result.Success<>(loggedInUser));
                                                }
                                            })
                                            .addOnFailureListener(new OnFailureListener() {
                                                @Override
                                                public void onFailure(@NonNull Exception e) {
                                                    Log.w(TAG, "Error retrieving document", e);
                                                    userTask.onReturn(new Result.Error(new IOException("Error logging in", e)));
                                                }
                                            });
                                } else {
                                    userTask.onReturn(new Result.NotVerified(new IOException("Error logging in", new Exception())));
                                }
                            } else {
                                // If sign in fails, display a message to the user.
                                Exception exception = task.getException();
                                Log.w(TAG, "signInWithEmail:failure", exception);

                                if (exception instanceof FirebaseAuthInvalidCredentialsException) {
                                    userTask.onReturn(new Result.Invalid(new IOException("Error logging in", exception)));
                                } else {
                                    userTask.onReturn(new Result.Error(new IOException("Error logging in", exception)));
                                }
                            }
                        }
                    });
        } catch (Exception e) {
            userTask.onReturn(new Result.Error(new IOException("Error logging in", e)));
        }
    }

    public void logout() {
        mAuth.signOut();
    }

    public void loginHwId(Context context, ActivityResultLauncher<Intent> loginHwIdActivityResult, ILoginUserTask userTask) {
        AccountAuthParams mAuthParam = new AccountAuthParamsHelper(AccountAuthParams.DEFAULT_AUTH_REQUEST_PARAM)
                .setEmail()
                .createParams();

        mAuthService = AccountAuthManager.getService(context, mAuthParam);

        com.huawei.hmf.tasks.Task<AuthAccount> task = mAuthService.silentSignIn();
        task.addOnSuccessListener(new com.huawei.hmf.tasks.OnSuccessListener<AuthAccount>() {
            @Override
            public void onSuccess(AuthAccount user) {
                Log.d(TAG, "signInWithHuaweiAccount:success");
                onLoginHwId(user, userTask);
            }
        });
        task.addOnFailureListener(new com.huawei.hmf.tasks.OnFailureListener() {
            @Override
            public void onFailure(Exception e) {
                if (e instanceof ApiException) {
                    ApiException apiException = (ApiException) e;
                    Intent signInIntent = mAuthService.getSignInIntent();
                    Log.d(TAG, "signInWithHuaweiAccount:failure " + e);
                    loginHwIdActivityResult.launch(signInIntent);
                }
            }
        });
    }

    public void loginHwId(Context context, Intent data, ILoginUserTask userTask) {
        Log.i(TAG, "signInWithHuaweiAccount:onActivitResult");
        if (data != null) {
            com.huawei.hmf.tasks.Task<AuthAccount> authAccountTask = AccountAuthManager.parseAuthResultFromIntent(data);
            if (authAccountTask.isSuccessful()) {
                // The sign-in is successful, and the authAccount object that contains the HUAWEI ID information is obtained.
                Log.d(TAG, "signInWithHuaweiAccount:success");
                AuthAccount user = authAccountTask.getResult();
                onLoginHwId(user, userTask);
            } else {
                // The sign-in fails. Find the failure cause from the status code. For more information, please refer to the "Error Codes" section in the API Reference.
                Log.e(TAG, "signInWithHuaweiAccount:failed : " + ((ApiException) authAccountTask.getException()).getStatusCode());
                ApiException apiException = (ApiException) authAccountTask.getException();
                userTask.onReturn(new Result.Error(new IOException("Error logging in", apiException)));
            }
        } else {
            Exception exception = new Exception();
            Log.w(TAG, "signInWithHuaweiAccount:failed", exception);
            userTask.onReturn(new Result.Error(new IOException("Error logging in", exception)));
        }
    }

    public void logoutHwId() {
        if (mAuthService == null) {
            return;
        }
        com.huawei.hmf.tasks.Task<Void> signOutTask = mAuthService.signOut();
        signOutTask.addOnSuccessListener(new com.huawei.hmf.tasks.OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                Log.i(TAG, "signOut_HwId: success");
            }
        }).addOnFailureListener(new com.huawei.hmf.tasks.OnFailureListener() {
            @Override
            public void onFailure(Exception e) {
                Log.i(TAG, "signOut_HwId: failure");
            }
        });
    }

    public void cancelAuthHwId() {
        if (mAuthService == null) {
            return;
        }
        com.huawei.hmf.tasks.Task<Void> task = mAuthService.cancelAuthorization();
        task.addOnSuccessListener(new com.huawei.hmf.tasks.OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                Log.i(TAG, "cancelAuthorization_HwId: success");
            }
        });
        task.addOnFailureListener(new com.huawei.hmf.tasks.OnFailureListener() {
            @Override
            public void onFailure(Exception e) {
                Log.i(TAG, "cancelAuthorization_HwId: failure: " + e.getClass().getSimpleName());
            }
        });
    }

    private void onLoginHwId(AuthAccount user, ILoginUserTask userTask) {
        String userID = user.getUnionId();
        String displayName = user.getDisplayName();
        String emailAddress = user.getEmail();
        String avatar = user.getAvatarUriString();
        Log.d(TAG, "User Union ID: " + userID);
        db.collection("users")
                .document(userID)
                .get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                        if (documentSnapshot.exists()) {
                            Map<String, Object> data = documentSnapshot.getData();
                            if (data == null) {
                                Exception exception = new Exception();
                                Log.w(TAG, "signIn:failed", exception);
                                userTask.onReturn(new Result.Error(new IOException("Error logging in", exception)));
                                return;
                            }
                            Date dateCreated = ((Timestamp) Objects.requireNonNull(data.get("dateCreated"))).toDate();

                            Log.d(TAG, "DocumentSnapshot retrieved with ID: " + userID);
                            User loggedInUser = new User(userID, displayName, emailAddress, 2, dateCreated);
                            userTask.onReturn(new Result.Success<>(loggedInUser));
                        } else {
                            Log.d(TAG, "createUserWithHuaweiAccount:success");

                            Map<String, Object> userData = new HashMap<>();
                            userData.put("displayName", displayName);
                            userData.put("emailAddress", emailAddress);
//                            userData.put("avatar", avatar);
                            userData.put("userType", 2);
                            userData.put("userPriority", 1);
                            userData.put("dateCreated", FieldValue.serverTimestamp());
                            userData.put("isActive", true);

                            db.collection("users")
                                    .document(userID)
                                    .set(userData)
                                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                                        @Override
                                        public void onSuccess(Void aVoid) {
                                            Log.d(TAG, "DocumentSnapshot added with ID: " + userID);
                                            User loggedInUser = new User(userID, displayName, emailAddress, 2, new Date());
                                            userTask.onReturn(new Result.Success<>(loggedInUser));
                                        }
                                    })
                                    .addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {
                                            Log.w(TAG, "Error adding document", e);
                                            userTask.onReturn(new Result.Error(new IOException("Error registering", e)));
                                        }
                                    });
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w(TAG, "Error retrieving document", e);
                        userTask.onReturn(new Result.Error(new IOException("Error logging in", e)));
                    }
                });
    }
}
package com.thuvarahan.eduforum.data.login;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.thuvarahan.eduforum.data.login.model.LoggedInUser;
import com.thuvarahan.eduforum.data.user.User;
import com.thuvarahan.eduforum.interfaces.ILoginUserTask;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Class that handles authentication w/ login credentials and retrieves user information.
 */
public class LoginDataSource {

    private final FirebaseAuth mAuth = FirebaseAuth.getInstance();
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public void login(String username, String password, ILoginUserTask userTask) {
        try {
            mAuth.signInWithEmailAndPassword(username, password)
            .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                @Override
                public void onComplete(@NonNull Task<AuthResult> task) {
                    String TAG = "Logging in user: ";
                    if (task.isSuccessful()) {
                        // Sign in success, update UI with the signed-in user's information
                        Log.d(TAG, "signInWithEmail:success");
                        FirebaseUser user = mAuth.getCurrentUser();

                        if (user.isEmailVerified()) {
                            db.collection("users")
                            .document(user.getUid())
                            .get()
                            .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                                @Override
                                public void onSuccess(DocumentSnapshot documentSnapshot) {
                                    Map<String, Object> data =  documentSnapshot.getData();
                                    String userID = user.getUid();
                                    String displayName = data.get("displayName").toString();
                                    String emailAddress = user.getEmail();
                                    Date dateCreated = ((Timestamp) data.get("dateCreated")).toDate();

                                    Log.d(TAG, "DocumentSnapshot retrieved with ID: " + user.getUid());
                                    User loggedInUser = new User(userID, displayName, emailAddress, dateCreated);
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
}
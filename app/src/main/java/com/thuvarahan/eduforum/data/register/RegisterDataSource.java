package com.thuvarahan.eduforum.data.register;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.thuvarahan.eduforum.data.user.User;
import com.thuvarahan.eduforum.interfaces.IRegisterUserTask;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Class that handles authentication w/ login credentials and retrieves user information.
 */
public class RegisterDataSource {

    private final FirebaseAuth mAuth = FirebaseAuth.getInstance();
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public void register(String displayName, String username, String password, String confirmPassword, IRegisterUserTask userTask) {
        try {
            mAuth.createUserWithEmailAndPassword(username, password)
            .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                @Override
                public void onComplete(@NonNull Task<AuthResult> task) {
                    String TAG = "Registering new user: ";
                    if (task.isSuccessful()) {
                        // Sign in success, update UI with the signed-in user's information
                        Log.d(TAG, "createUserWithEmail:success");
                        FirebaseUser user = mAuth.getCurrentUser();
                        user.sendEmailVerification();

                        Map<String, Object> userData = new HashMap<>();
                        userData.put("displayName", displayName);
                        userData.put("emailAddress", username);
                        userData.put("userType", 1);
                        userData.put("userPriority", 1);
                        userData.put("dateCreated", FieldValue.serverTimestamp());
                        userData.put("isActive", true);

                        db.collection("users")
                        .document(user.getUid())
                        .set(userData)
                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                Log.d(TAG, "DocumentSnapshot added with ID: " + user.getUid());
                                User registeredUser = new User(user.getUid(), displayName, username, 1, new Date());
                                userTask.onReturn(new Result.Success<>(registeredUser));
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Log.w(TAG, "Error adding document", e);
                                userTask.onReturn(new Result.Error(new IOException("Error registering", e)));
                            }
                        });
                    } else {
                        // If sign in fails, display a message to the user.
                        Exception exception = task.getException();
                        Log.w(TAG, "createUserWithEmail:failure", exception);

                        if (exception instanceof FirebaseAuthUserCollisionException) {
                            userTask.onReturn(new Result.Failure(new IOException("Error registering", exception)));
                        } else {
                            userTask.onReturn(new Result.Error(new IOException("Error registering", exception)));
                        }
                    }
                }
            });
        } catch (Exception e) {
            userTask.onReturn(new Result.Error(new IOException("Error registering", e)));
        }
    }
}
package com.thuvarahan.eduforum;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.thuvarahan.eduforum.data.login.LoginDataSource;
import com.thuvarahan.eduforum.data.login.LoginRepository;
import com.thuvarahan.eduforum.services.network_broadcast.NetworkChangeReceiver;

import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class NewPostActivity extends AppCompatActivity {

    EditText title;
    EditText body;
    ImageView image;
    AppCompatButton btnChoose;
    AppCompatButton btnAddPost;

    TextView profileDisplayName;
    TextView profileEmail;

    String currentUserID = "";

    Menu postMenu;

    View rootView;

    FirebaseFirestore db = FirebaseFirestore.getInstance();

    FirebaseStorage storage = FirebaseStorage.getInstance();
    StorageReference storageReference = storage.getReference();


    // Uri indicates, where the image will be picked from
    private Uri filePath;

    // request code
    private final int PICK_IMAGE_REQUEST = 22;
    private final int IMAGE_ACTIVITY_REQUEST_CODE = 25;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_post);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);

        rootView = findViewById(android.R.id.content);

        title = findViewById(R.id.input_title);
        body = findViewById(R.id.input_body);
        image = findViewById(R.id.post_img);
        btnChoose = findViewById(R.id.choose_image_btn);
        btnAddPost = findViewById(R.id.add_post_btn);

        profileDisplayName = findViewById(R.id.post_author_name);
        profileEmail = findViewById(R.id.post_author_email);

        LoginRepository loginRepository = LoginRepository.getInstance(new LoginDataSource());
        profileDisplayName.setText(loginRepository.getUser().getDisplayName());
        profileEmail.setText(loginRepository.getUser().getUsername());

        currentUserID = loginRepository.getUser().getUserID();

        btnChoose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectImage();
            }
        });

        btnAddPost.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(NewPostActivity.this.getCurrentFocus().getWindowToken(), 0);

                if (image.getDrawable() != null) {
                    uploadImage();
                } else {
                    savePost("");
                }
            }
        });

        image.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                image.setImageDrawable(null);
                image.setVisibility(View.GONE);
                return true;
            }
        });

        title.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                togglePostButton();
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        body.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                togglePostButton();
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    /*@Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.top_post_button, menu);
        postMenu = menu;
        postMenu.findItem(R.id.action_post).setEnabled(false);
        return true;
    }*/

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            /*case R.id.action_post:
                InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(rootView.getWindowToken(), 0);

                if (image.getDrawable() != null) {
                    uploadImage();
                }
                else {
                    savePost("");
                }
                return true;*/
        }
        return super.onOptionsItemSelected(item);
    }

    void togglePostButton() {
        String title = this.title.getText().toString();
        String body = this.body.getText().toString();

//        postMenu.findItem(R.id.action_post).setEnabled(title.trim().length() != 0 && body.trim().length() != 0);
        btnAddPost.setEnabled(title.trim().length() != 0 && body.trim().length() != 0);
    }

    void savePost(String imageUrl) {
        if (NetworkChangeReceiver.isOnline(NewPostActivity.this)) {
            final ProgressDialog progressDialog = new ProgressDialog(this, R.style.ProgressDialogSpinnerOnly);
            progressDialog.setCancelable(false);
            progressDialog.show();

            DocumentReference author = db.collection("users").document(currentUserID);

            Map<String, Object> post = new HashMap<>();
            post.put("postTitle", title.getText().toString().trim());
            post.put("postBody", body.getText().toString().trim());
            post.put("postAuthor", author);
            post.put("timestamp", FieldValue.serverTimestamp());
            post.put("canDisplay", true);

            ArrayList<String> images = new ArrayList<String>();
            if (!imageUrl.equals("")) {
                images.add(imageUrl);
            }
            post.put("postImages", images);

            String TAG = "Saving New Post: ";

            db.collection("posts")
                    .add(post)
                    .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                        @Override
                        public void onSuccess(DocumentReference documentReference) {
                            Log.d(TAG, "DocumentSnapshot added with ID: " + documentReference.getId());

                            progressDialog.dismiss();

                            Toast toast = Toast.makeText(getApplicationContext(), getResources().getString(R.string.question_add_success), Toast.LENGTH_LONG);
                            toast.show();

                            onBackPressed();
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Log.w(TAG, "Error adding document", e);

                            progressDialog.dismiss();

                            Snackbar snackbar = Snackbar.make(rootView, getResources().getString(R.string.question_add_failure), Snackbar.LENGTH_LONG)
                                    .setBackgroundTint(Color.RED)
                                    .setTextColor(Color.WHITE);
                            snackbar.show();
                        }
                    });
        } else {
            Snackbar.make(rootView, "No internet connection!", Snackbar.LENGTH_LONG)
                    .setBackgroundTint(Color.RED)
                    .setTextColor(Color.WHITE)
                    .show();
        }
    }

    // Select Image method
    private void selectImage() {
        // Defining Implicit Intent to mobile gallery
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityIfNeeded(
                Intent.createChooser(
                        intent,
                        "Select Image from here..."),
                PICK_IMAGE_REQUEST);
    }

    void uploadImage() {
        if (NetworkChangeReceiver.isOnline(NewPostActivity.this)) {
            if (image.getDrawable() != null) {
                final ProgressDialog progressDialog = new ProgressDialog(this, R.style.ProgressDialog);
                progressDialog.setTitle("Uploading Image...");
                progressDialog.setCancelable(false);
                progressDialog.show();

                Bitmap imageBitmap = ((BitmapDrawable) image.getDrawable()).getBitmap();
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                imageBitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
                byte[] imageData = baos.toByteArray();

                StorageReference ref = storageReference.child("post_images/" + UUID.randomUUID().toString() + ".jpg");
                ref.putBytes(imageData)
                        .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                            @Override
                            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                                taskSnapshot.getMetadata().getReference().getDownloadUrl()
                                        .addOnSuccessListener(new OnSuccessListener<Uri>() {
                                            @Override
                                            public void onSuccess(Uri uri) {
                                                progressDialog.dismiss();
                                                String imageUrl = uri.toString();
                                                savePost(imageUrl);
                                            }
                                        })
                                        .addOnFailureListener(new OnFailureListener() {
                                            @Override
                                            public void onFailure(@NonNull Exception e) {
                                                progressDialog.dismiss();
                                                Snackbar.make(rootView, getResources().getString(R.string.question_add_failure), Snackbar.LENGTH_LONG)
                                                        .setBackgroundTint(Color.RED)
                                                        .setTextColor(Color.WHITE)
                                                        .show();
                                            }
                                        });
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                progressDialog.dismiss();
                                Snackbar.make(rootView, getResources().getString(R.string.question_add_failure), Snackbar.LENGTH_LONG)
                                        .setBackgroundTint(Color.RED)
                                        .setTextColor(Color.WHITE)
                                        .show();
                            }
                        })
                        .addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                            @Override
                            public void onProgress(@NotNull UploadTask.TaskSnapshot taskSnapshot) {
                                double progress = (100.0 * taskSnapshot.getBytesTransferred() / taskSnapshot.getTotalByteCount());
                                progressDialog.setMessage((int) progress + "%" + " uploaded");
                            }
                        });
            }
        } else {
            Snackbar.make(rootView, "No internet connection!", Snackbar.LENGTH_LONG)
                    .setBackgroundTint(Color.RED)
                    .setTextColor(Color.WHITE)
                    .show();
        }
    }

    // Override onActivityResult method
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        //--------------- Choose Image Result --------------//
        if (requestCode == PICK_IMAGE_REQUEST) {
            if (resultCode == RESULT_OK && data != null && data.getData() != null) {
                Intent intent = new Intent(getApplicationContext(), ImageActivity.class);
                intent.putExtra("imagePath", data.getData().toString());
                startActivityIfNeeded(intent, IMAGE_ACTIVITY_REQUEST_CODE);
            } else {
                Toast.makeText(getApplicationContext(), getResources().getString(R.string.failed_to_load_image), Toast.LENGTH_LONG).show();
            }
        }

        //--------------- Image Crop Activity Result --------------//
        if (requestCode == IMAGE_ACTIVITY_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                String imagePath = data.getExtras().getString("imagePath");
                if (imagePath != null) {
                    try {
                        Bitmap bitmap = BitmapFactory.decodeStream(getApplicationContext().openFileInput(imagePath));
                        image.setImageBitmap(bitmap);
                        image.setVisibility(View.VISIBLE);
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                        Toast.makeText(getApplicationContext(), getResources().getString(R.string.failed_to_load_image), Toast.LENGTH_LONG).show();
                    }
                } else {
                    Toast.makeText(getApplicationContext(), getResources().getString(R.string.failed_to_load_image), Toast.LENGTH_LONG).show();
                }
            } else if (resultCode == RESULT_CANCELED) {
                Toast.makeText(getApplicationContext(), getResources().getString(R.string.failed_to_load_image), Toast.LENGTH_LONG).show();
            }
        }
    }

}
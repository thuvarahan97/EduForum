package com.thuvarahan.eduforum;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.squareup.picasso.Picasso;
import com.thuvarahan.eduforum.data.login.LoginDataSource;
import com.thuvarahan.eduforum.data.login.LoginRepository;
import com.thuvarahan.eduforum.data.post.Post;
import com.thuvarahan.eduforum.data.reply.Reply;
import com.thuvarahan.eduforum.data.user.User;
import com.thuvarahan.eduforum.interfaces.IAlertDialogTask;
import com.thuvarahan.eduforum.interfaces.IEditDialogTask;
import com.thuvarahan.eduforum.services.network_broadcast.NetworkChangeReceiver;
import com.thuvarahan.eduforum.services.push_notification.PushNotification;
import com.thuvarahan.eduforum.ui.posts_replies.RVRepliesAdapter;
import com.thuvarahan.eduforum.utils.CustomUtils;
import com.thuvarahan.eduforum.utils.LanguageTranslation;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class PostActivity extends AppCompatActivity {

    RVRepliesAdapter rvAdapter;

    View rootView;

    FirebaseFirestore db = FirebaseFirestore.getInstance();

    ArrayList<Reply> replies = new ArrayList<Reply>();

    private Post _post;
    private String postID;

    private TextView title;
    private TextView body;
    private TextView author;
    private TextView timestamp;
    private ImageView image;
    private TextView replies_count;
    private EditText etReplyBody;
    private AppCompatButton btnAddReply;
    private Button btnOptions;
    SwipeRefreshLayout swipeRefresh;
    LinearLayout bodyTranslated;

    String currentUserID = "";
    String currentUserDisplayName = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);

        rootView = findViewById(android.R.id.content);

        title = (TextView) findViewById(R.id.post_title);
        body = (TextView) findViewById(R.id.post_body);
        author = (TextView) findViewById(R.id.post_author_name);
        timestamp = (TextView) findViewById(R.id.post_timestamp);
        image = (ImageView) findViewById(R.id.post_img);
        replies_count = (TextView) findViewById(R.id.post_replies_count);
        etReplyBody = (EditText) findViewById(R.id.add_reply_body);
        btnAddReply = (AppCompatButton) findViewById(R.id.add_reply_btn);
        btnOptions = (Button) findViewById(R.id.post_options);
        swipeRefresh = (SwipeRefreshLayout) findViewById(R.id.swipe_refresh_2);
        bodyTranslated = (LinearLayout) findViewById(R.id.post_body_translated);

        _post = (Post) getIntent().getSerializableExtra("post");

        LoginRepository loginRepository = LoginRepository.getInstance(new LoginDataSource());
        User user = loginRepository.getUser();
        currentUserID = user.getUserID();
        currentUserDisplayName = user.getDisplayName();

        if (_post != null) {
            postID = _post.id;
            title.setText(_post.title);
            body.setText(_post.body);
            author.setText("");

            //---------------- Fetch Author's Name ---------------//
            db.document(_post.authorRef).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                    if (task.isSuccessful()) {
                        DocumentSnapshot doc = task.getResult();
                        if (doc != null && doc.getData() != null && doc.getData().get("displayName") != null) {
                            String authorVal = doc.getData().get("displayName").toString();
                            author.setText(authorVal);
                        } else {
                            author.setText(getResources().getString(R.string.unknown_author));
                        }
                    } else {
                        author.setText(getResources().getString(R.string.unknown_author));
                    }
                }
            });

            //---------------- Convert Date Format ---------------//
            String date = CustomUtils.formatTimestamp(_post.timestamp);
            timestamp.setText(date);

            //----------------- Display Image ------------//
            URL url = null;
            if (_post.images.size() > 0) {
                try {
                    url = new URL(_post.images.get(0));

                    Picasso.get().load(url.toString()).into(image);
                    image.setVisibility(View.VISIBLE);
                } catch (MalformedURLException e) {
                    image.setVisibility(View.GONE);
                    e.printStackTrace();
                }
            }

            //----------------- Display 0 Replies Count -----------------//
            replies_count.setText("");

            //----------------- Fetch All Replies -----------------//
            fetchReplies(this, db, postID);

            //----------------- Swipe Refresh - Fetch Replies ------------//
            swipeRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
                @Override
                public void onRefresh() {
                    recreate();
                }
            });

            //---------------- Add Reply -----------------//
            etReplyBody.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    toggleReplyButton();
                }

                @Override
                public void afterTextChanged(Editable s) {

                }
            });

            btnAddReply.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(PostActivity.this.getCurrentFocus().getWindowToken(), 0);

                    saveReply(_post.authorRef);
                }
            });

            //---------------- Enable/Disable options --------------//
            if (currentUserID.equals(db.document(_post.authorRef).getId())) {
                btnOptions.setEnabled(true);
                btnOptions.setVisibility(View.VISIBLE);
            } else {
                btnOptions.setEnabled(false);
                btnOptions.setVisibility(View.GONE);
            }

            btnOptions.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    showPostOptionsBottomSheetDialog(view.getContext(), _post);
                }
            });

            // Show body text translation
            try {
                if (body != null && !body.getText().toString().trim().isEmpty()) {
                    LanguageTranslation.getTranslatedText(body.getText().toString().trim(), new LanguageTranslation.ITranslationTask() {
                        @Override
                        public void onResult(boolean isTranslated, String text) {
                            if (isTranslated) {
                                bodyTranslated.setVisibility(View.VISIBLE);
                                View innerTranslationView = bodyTranslated.getChildAt(0);
                                TextView tvToggleTranslation = innerTranslationView.findViewById(R.id.tv_toggle_translation);
                                LinearLayout llTranslatedText = innerTranslationView.findViewById(R.id.ll_translatedText);
                                TextView tvTranslatedText = innerTranslationView.findViewById(R.id.tv_TranslatedText);

                                if (tvToggleTranslation != null && llTranslatedText != null && tvTranslatedText != null) {
                                    tvToggleTranslation.setOnClickListener(new View.OnClickListener() {
                                        @Override
                                        public void onClick(View v) {
                                            if (llTranslatedText.getVisibility() == View.GONE) {
                                                tvTranslatedText.setText(text);
                                                tvToggleTranslation.setText("Hide translation");
                                                llTranslatedText.setVisibility(View.VISIBLE);

                                            } else {
                                                tvToggleTranslation.setText("Show translation");
                                                llTranslatedText.setVisibility(View.GONE);
                                            }
                                        }
                                    });
                                }
                            } else {
                                bodyTranslated.setVisibility(View.GONE);
                            }
                        }
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            finish();
        }
    }

    public void fetchReplies(Context context, FirebaseFirestore db, String postID) {
        String TAG = "Replies Fetch: ";

        db.collection("posts")
                .document(postID)
                .collection("replies")
                .whereEqualTo("canDisplay", true)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @SuppressLint("SetTextI18n")
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            QuerySnapshot result = task.getResult();
                            if (result == null || result.isEmpty()) {
                                replies_count.setText("0" + getResources().getString(R.string.answers));
                                stopRefreshing();
                                return;
                            }

                            replies.clear();

                            if (result.size() == 1) {
                                replies_count.setText("1" + getResources().getString(R.string.answer));
                            } else {
                                replies_count.setText(result.size() + getResources().getString(R.string.answers));
                            }

                            for (QueryDocumentSnapshot document : result) {
                                Log.d(TAG, document.getId() + " => " + document.getData());

                                Map<String, Object> data = document.getData();
                                String id = document.getId().toString();
                                String body = Objects.requireNonNull(data.get("replyBody")).toString();
                                DocumentReference author = (DocumentReference) data.get("replyAuthor");
                                Timestamp timestamp = (Timestamp) data.get("timestamp");

                                assert author != null;
                                assert timestamp != null;
                                Reply reply = new Reply(id, body, author, timestamp, postID);
                                replies.add(reply);
                                showRecyclerView();
                                stopRefreshing();
                            }
                        } else {
                            Toast toast = Toast.makeText(context, "Unable to retrieve replies! Try again.", Toast.LENGTH_SHORT);
                            toast.show();
                            Log.w(TAG, "Error getting documents.", task.getException());
                            stopRefreshing();
                        }
                    }
                });
    }

    void showRecyclerView() {
        ArrayList<Reply> allreplies = this.replies;

        // set up the RecyclerView
        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.recycler_replies_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        rvAdapter = new RVRepliesAdapter(allreplies);
        recyclerView.setAdapter(rvAdapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    void toggleReplyButton() {
        String replyBody = this.etReplyBody.getText().toString();
        btnAddReply.setEnabled(replyBody.trim().length() != 0 && replyBody.trim().length() != 0);
    }

    void saveReply(String postAuthorRef) {
        if (NetworkChangeReceiver.isOnline(PostActivity.this)) {
            final ProgressDialog progressDialog = new ProgressDialog(PostActivity.this, R.style.ProgressDialogSpinnerOnly);
            progressDialog.setCancelable(false);
            progressDialog.show();

            DocumentReference replyAuthor = db.collection("users").document(currentUserID);

            Map<String, Object> reply = new HashMap<>();
            reply.put("replyBody", etReplyBody.getText().toString().trim());
            reply.put("replyAuthor", replyAuthor);
            reply.put("timestamp", FieldValue.serverTimestamp());
            reply.put("canDisplay", true);

            String TAG = "Saving New Reply: ";

            db.collection("posts")
                    .document(postID)
                    .collection("replies")
                    .add(reply)
                    .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                        @Override
                        public void onSuccess(DocumentReference documentReference) {
                            Log.d(TAG, "DocumentSnapshot added with ID: " + documentReference.getId());

                            etReplyBody.getText().clear();
                            etReplyBody.clearFocus();

                            DocumentReference postAuthor = db.document(postAuthorRef);

                            if (!postAuthor.getId().equals(currentUserID)) {
                                postAuthor.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                                    @Override
                                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                                        Map<String, Object> docData = documentSnapshot.getData();
                                        if (docData != null && docData.get("pushToken") != null && !docData.get("pushToken").toString().trim().isEmpty()) {
                                            String receiverToken = docData.get("pushToken").toString();
                                            System.out.println("ReceiverToken: " + receiverToken);

                                            //--------- Send Notification ----------//
                                            PushNotification.sendNotification(receiverToken, currentUserDisplayName, postID);

                                            //--------- Save Notification in Firestore --------//
                                            DocumentReference postRef = db.collection("posts").document(postID);

                                            Map<String, Object> notification = new HashMap<>();
                                            notification.put("post", postRef);
                                            notification.put("author", replyAuthor);
                                            notification.put("timestamp", FieldValue.serverTimestamp());
                                            notification.put("canDisplay", true);
                                            notification.put("isChecked", false);

                                            String TAG1 = "Save New Notification: ";

                                            postAuthor.collection("notifications")
                                                    .add(notification)
                                                    .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                                                        @Override
                                                        public void onSuccess(DocumentReference documentReference) {
                                                            Log.d(TAG1, "DocumentSnapshot added with ID: " + documentReference.getId());
                                                        }
                                                    });
                                        }
                                    }
                                });
                            }
                            progressDialog.dismiss();
                            fetchReplies(getApplicationContext(), db, postID);
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Log.w(TAG, "Error adding document", e);
                            progressDialog.dismiss();
                            Snackbar snackbar = Snackbar.make(rootView, getResources().getString(R.string.unable_to_add_answer), Snackbar.LENGTH_LONG)
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

    void stopRefreshing() {
        if (swipeRefresh.isRefreshing()) {
            swipeRefresh.setRefreshing(false);
        }
    }

    void showPostOptionsBottomSheetDialog(Context context, Post post) {
        final BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(context);
        bottomSheetDialog.setContentView(R.layout.post_options_bottom_sheet_dialog);

        LinearLayout edit = bottomSheetDialog.findViewById(R.id.post_option_edit);
        LinearLayout delete = bottomSheetDialog.findViewById(R.id.post_option_delete);

        DocumentReference postAuthorRef = db.document(post.authorRef);

        assert edit != null;
        assert delete != null;

        if (currentUserID.equals(postAuthorRef.getId())) {
            edit.setVisibility(View.VISIBLE);
            delete.setVisibility(View.VISIBLE);
        } else {
            edit.setVisibility(View.GONE);
            delete.setVisibility(View.GONE);
        }

        edit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showEditDialogBox(PostActivity.this, rootView, title.getText().toString(), body.getText().toString(), true, _post, null, new IEditDialogTask() {
                    @Override
                    public void onUpdated(String title, String body) {
                        _post.title = (title != null) ? title : "";
                        _post.body = body;
                        recreate();
                    }
                });
                bottomSheetDialog.dismiss();
            }
        });

        delete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                bottomSheetDialog.dismiss();

                CustomUtils.showAlertDialog(context,
                        context.getResources().getString(R.string.delete_question_alert_title),
                        context.getResources().getString(R.string.delete_question_alert_message),
                        context.getResources().getString(R.string.delete_question_alert_yes),
                        context.getResources().getString(R.string.no),
                        new IAlertDialogTask() {

                            @Override
                            public void onPressedYes(DialogInterface alertDialog) {
                                if (NetworkChangeReceiver.isOnline(context)) {
                                    final ProgressDialog progressDialog = new ProgressDialog(context, R.style.ProgressDialogSpinnerOnly);
                                    progressDialog.setCancelable(false);
                                    progressDialog.show();

                                    db.collection("posts").document(post.id)
                                            .update("canDisplay", false)
                                            .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                @Override
                                                public void onComplete(@NonNull Task<Void> task) {
                                                    if (task.isSuccessful()) {
                                                        Toast.makeText(context, context.getResources().getString(R.string.question_deleted), Toast.LENGTH_LONG).show();
                                                        finish();
                                                    } else {
                                                        Snackbar.make(rootView, context.getResources().getString(R.string.question_not_deleted), Snackbar.LENGTH_LONG)
                                                                .setBackgroundTint(Color.RED)
                                                                .setTextColor(Color.WHITE)
                                                                .show();
                                                    }
                                                    progressDialog.dismiss();
                                                }
                                            });
                                    alertDialog.dismiss();
                                } else {
                                    Snackbar.make(rootView, "No internet connection!", Snackbar.LENGTH_LONG)
                                            .setBackgroundTint(Color.RED)
                                            .setTextColor(Color.WHITE)
                                            .show();
                                }
                            }

                            @Override
                            public void onPressedNo(DialogInterface alertDialog) {
                                alertDialog.dismiss();
                            }
                        });
            }
        });

        bottomSheetDialog.show();
    }

    public static void showEditDialogBox(Context context, View view, String existingTitle, String existingBody, boolean isPost, Post post, Reply reply, IEditDialogTask editDialogTask) {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(context);
        LayoutInflater layoutInflater = LayoutInflater.from(context);
        final View alertView = layoutInflater.inflate(R.layout.content_editdialog, null);
        final TextView tvContentTitle = alertView.findViewById(R.id.content_title);
        final EditText etEditTitle = alertView.findViewById(R.id.input_title);
        final EditText etEditBody = alertView.findViewById(R.id.input_body);
        etEditTitle.setText((existingTitle != null) ? existingTitle : "");
        etEditBody.setText((existingBody != null) ? existingBody : "");
        if (isPost) {
            tvContentTitle.setText("Edit Question");
            etEditTitle.setVisibility(View.VISIBLE);
        } else {
            tvContentTitle.setText("Edit Answer");
            etEditTitle.setVisibility(View.GONE);
        }
        dialogBuilder.setView(alertView);
        dialogBuilder.setCancelable(false);
        dialogBuilder.setNegativeButton("Cancel", null);
        dialogBuilder.setPositiveButton("Apply", null);
        AlertDialog alertDialog = dialogBuilder.show();
        Button negativeButton = alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE);
        Button positiveButton = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE);
        negativeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(alertDialog.getCurrentFocus().getWindowToken(), 0);
                alertDialog.dismiss();
            }
        });
        positiveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(alertDialog.getCurrentFocus().getWindowToken(), 0);
                if (isPost) {
                    String editTitle = etEditTitle.getText().toString().trim();
                    String editBody = etEditBody.getText().toString().trim();
                    if (!editTitle.isEmpty() && !editBody.isEmpty()) {
                        if (NetworkChangeReceiver.isOnline(context)) {
                            applyPostEdit(context, view, editTitle, editBody, post, editDialogTask, new IEditDialogTask() {
                                @Override
                                public void onUpdated(String title, String body) {
                                    alertDialog.dismiss();
                                }
                            });
                        } else {
                            Snackbar.make(view, "No internet connection!", Snackbar.LENGTH_LONG)
                                    .setBackgroundTint(Color.RED)
                                    .setTextColor(Color.WHITE)
                                    .show();
                        }
                    } else {
                        Snackbar.make(view, context.getResources().getQuantityString(R.plurals.input_empty, 2), Snackbar.LENGTH_LONG)
                                .setBackgroundTint(Color.RED)
                                .setTextColor(Color.WHITE)
                                .show();
                    }
                } else {
                    String editBody = etEditBody.getText().toString().trim();
                    if (!editBody.isEmpty()) {
                        if (NetworkChangeReceiver.isOnline(context)) {
                            applyReplyEdit(context, view, editBody, reply, editDialogTask, new IEditDialogTask() {
                                @Override
                                public void onUpdated(String title, String body) {
                                    alertDialog.dismiss();
                                }
                            });
                        } else {
                            Snackbar.make(view, "No internet connection!", Snackbar.LENGTH_LONG)
                                    .setBackgroundTint(Color.RED)
                                    .setTextColor(Color.WHITE)
                                    .show();
                        }
                    } else {
                        Snackbar.make(view, context.getResources().getQuantityString(R.plurals.input_empty, 1), Snackbar.LENGTH_LONG)
                                .setBackgroundTint(Color.RED)
                                .setTextColor(Color.WHITE)
                                .show();
                    }
                }
            }
        });
    }

    private static void applyPostEdit(Context context, View view, String editTitle, String editBody, Post post, IEditDialogTask editDialogTask, IEditDialogTask alertDismissTask) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        final ProgressDialog progressDialog = new ProgressDialog(context, R.style.ProgressDialogSpinnerOnly);
        progressDialog.setCancelable(false);
        progressDialog.show();
        Map<String, Object> updateValues = new HashMap<>();
        updateValues.put("postTitle", editTitle);
        updateValues.put("postBody", editBody);
        updateValues.put("lastTimestamp", FieldValue.serverTimestamp());
        db.collection("posts").document(post.id)
                .update(updateValues)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            Toast.makeText(context, context.getResources().getString(R.string.question_updated), Toast.LENGTH_LONG).show();
                            alertDismissTask.onUpdated(editTitle, editBody);
                            editDialogTask.onUpdated(editTitle, editBody);
                        } else {
                            Snackbar.make(view, context.getResources().getString(R.string.question_not_updated), Snackbar.LENGTH_LONG)
                                    .setBackgroundTint(Color.RED)
                                    .setTextColor(Color.WHITE)
                                    .show();
                        }
                        progressDialog.dismiss();
                    }
                });
    }

    private static void applyReplyEdit(Context context, View view, String editBody, Reply reply, IEditDialogTask editDialogTask, IEditDialogTask alertDismissTask) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        final ProgressDialog progressDialog = new ProgressDialog(context, R.style.ProgressDialogSpinnerOnly);
        progressDialog.setCancelable(false);
        progressDialog.show();
        Map<String, Object> updateValues = new HashMap<>();
        updateValues.put("replyBody", editBody);
        updateValues.put("lastTimestamp", FieldValue.serverTimestamp());
        db.collection("posts").document(reply.postID)
                .collection("replies").document(reply.id)
                .update(updateValues)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            Toast.makeText(context, context.getResources().getString(R.string.answer_updated), Toast.LENGTH_LONG).show();
                            alertDismissTask.onUpdated(null, editBody);
                            editDialogTask.onUpdated(null, editBody);
                        } else {
                            Snackbar.make(view, context.getResources().getString(R.string.answer_not_updated), Snackbar.LENGTH_LONG)
                                    .setBackgroundTint(Color.RED)
                                    .setTextColor(Color.WHITE)
                                    .show();
                        }
                        progressDialog.dismiss();
                    }
                });
    }

}
package com.thuvarahan.eduforum;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextWatcher;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.content.res.ResourcesCompat;
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
import com.thuvarahan.eduforum.services.push_notification.PushNotification;
import com.thuvarahan.eduforum.ui.posts_replies.RVRepliesAdapter;

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

    String currentUserID = "";
    String currentUserDisplayName = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);

        rootView = findViewById(android.R.id.content).getRootView();

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
                    fetchReplies(getApplicationContext(), db, postID);
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
                    imm.hideSoftInputFromWindow(rootView.getWindowToken(), 0);

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

                /*Toast toast = Toast.makeText(getApplicationContext(), "Successfully saved!", Toast.LENGTH_LONG);
                TextView toastMessage = (TextView) toast.getView().findViewById(android.R.id.message);
                toastMessage.setTextColor(Color.GREEN);
                toast.show();*/

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

                        fetchReplies(getApplicationContext(), db, postID);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w(TAG, "Error adding document", e);
                        Snackbar snackbar = Snackbar.make(rootView, getResources().getString(R.string.unable_to_add_answer), Snackbar.LENGTH_LONG)
                                .setBackgroundTint(Color.RED)
                                .setTextColor(Color.WHITE);
                        snackbar.show();
                    }
                });
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
            public void onClick(View v) {
                showEditDialogBox(PostActivity.this, body.getText().toString());
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
                                                    Snackbar.make(view, context.getResources().getString(R.string.question_not_deleted), Snackbar.LENGTH_LONG)
                                                            .setBackgroundTint(Color.RED)
                                                            .setTextColor(Color.WHITE)
                                                            .show();
                                                }
                                                progressDialog.dismiss();
                                            }
                                        });
                                alertDialog.dismiss();
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

    private void showEditDialogBox(Context context, String existingBody) {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(context);
        final EditText etEditBody = new EditText(context);
        etEditBody.setText(existingBody);
        etEditBody.setTextColor(Color.BLACK);
        etEditBody.setHint("Write something...");
        etEditBody.setTextSize(16);
        etEditBody.setMaxHeight(350);
        etEditBody.setPadding(15, 15, 15, 15);
        etEditBody.setMinLines(5);
        etEditBody.setMaxLines(12);
        etEditBody.setGravity(Gravity.TOP | Gravity.START);
        etEditBody.setFilters(new InputFilter[]{new InputFilter.LengthFilter(1000)});
        etEditBody.setBackground(ResourcesCompat.getDrawable(getResources(), R.drawable.bg_single_post, null));
        FrameLayout container = new FrameLayout(context);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.leftMargin = 15;
        params.rightMargin = 15;
        params.topMargin = 15;
        params.bottomMargin = 15;
        etEditBody.setLayoutParams(params);
        container.addView(etEditBody);
        SpannableString title = new SpannableString("Edit Question");
        title.setSpan(new ForegroundColorSpan(ResourcesCompat.getColor(context.getResources(), R.color.black, null)), 0, title.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        dialogBuilder.setTitle(title);
        dialogBuilder.setView(container);
        dialogBuilder
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                    }
                })
                .setPositiveButton("Apply", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        String editBody = etEditBody.getText().toString().trim();
                        applyPostEdit(editBody);
                    }
                });
        dialogBuilder.show();
    }

    private void applyPostEdit(String editBody) {
        Toast.makeText(getApplicationContext(), "Copy is Clicked ", Toast.LENGTH_LONG).show();
    }

}
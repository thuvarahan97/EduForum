package com.thuvarahan.eduforum.ui.notifications;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.text.Html;
import android.text.Spanned;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.thuvarahan.eduforum.PostActivity;
import com.thuvarahan.eduforum.R;
import com.thuvarahan.eduforum.data.login.LoginDataSource;
import com.thuvarahan.eduforum.data.login.LoginRepository;
import com.thuvarahan.eduforum.data.notification.Notification;
import com.thuvarahan.eduforum.data.post.Post;
import com.thuvarahan.eduforum.interfaces.IProgressBarTask;
import com.thuvarahan.eduforum.utils.CustomUtils;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class RVNotificationsAdapter extends RecyclerView.Adapter<RVNotificationsAdapter.ViewHolder> {

    private List<Notification> mData;

    String currentUserID = "";

    FirebaseFirestore db = FirebaseFirestore.getInstance();

    // data is passed into the constructor
    public RVNotificationsAdapter(List<Notification> data) {
        this.mData = data;

        LoginRepository loginRepository = LoginRepository.getInstance(new LoginDataSource());
        if (loginRepository.getUser() != null) {
            currentUserID = loginRepository.getUser().getUserID();
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final ConstraintLayout notif;
        private final TextView body;
        private final TextView timestamp;
        private final Button btnOptions;

        public ViewHolder(View view) {
            super(view);
            notif = (ConstraintLayout) view.findViewById(R.id.notif);
            body = (TextView) view.findViewById(R.id.notif_body);
            timestamp = (TextView) view.findViewById(R.id.notif_timestamp);
            btnOptions = (Button) view.findViewById(R.id.notif_options);
        }
    }

    /// Create new views (invoked by the layout manager)
    @NotNull
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        // Create a new view, which defines the UI of the list item
        View view = LayoutInflater.from(viewGroup.getContext())
                .inflate(R.layout.notification_single, viewGroup, false);

        return new ViewHolder(view);
    }

    // binds the data to the TextView in each row
    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Notification _notification = getItem(position);
        holder.body.setText("");
        holder.timestamp.setText("");

        //------------ Set Notification Color ------------//
        if (_notification.isChecked) {
            holder.notif.setBackgroundColor(Color.WHITE);
        } else {
            holder.notif.setBackgroundColor(Color.rgb(210, 193, 227));
        }

        //---------------- Fetch Author's Name ---------------//
        _notification.author.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    DocumentSnapshot doc = task.getResult();
                    if (doc != null && doc.getData() != null && doc.getData().get("displayName") != null) {
                        String authorVal = doc.getData().get("displayName").toString();
                        holder.body.setText(getNotificationBody(authorVal, holder.itemView.getContext().getResources().getString(R.string.notif_body_answered), null));

                        _notification.post.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                            @SuppressLint("SetTextI18n")
                            @Override
                            public void onSuccess(DocumentSnapshot documentSnapshot) {
                                if (documentSnapshot != null) {
                                    String title = Objects.requireNonNull(documentSnapshot.get("postTitle")).toString();
                                    if (!title.trim().isEmpty()) {
                                        holder.body.setText(getNotificationBody(authorVal, holder.itemView.getContext().getResources().getString(R.string.notif_body_answered), title));
                                    }
                                }
                            }
                        });
                    } else {
                        removeItemAt(position);
                    }
                } else {
                    removeItemAt(position);
                }
            }
        });

        //---------------- Convert Date Format ---------------//
        String date = CustomUtils.formatTimestamp(_notification.timestamp);
        holder.timestamp.setText(date);

        //---------------- Clicking Notification ----------------//
        holder.notif.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Dialog progressDialog = CustomUtils.createProgressDialog((Activity) view.getContext());
                IProgressBarTask progressBarTask = new IProgressBarTask() {
                    @Override
                    public void onStart() {
                        if (progressDialog != null && !progressDialog.isShowing()) {
                            progressDialog.show();
                            CustomUtils.toggleWindowInteraction((Activity) view.getContext(), false);
                        }
                    }

                    @Override
                    public void onComplete() {
                        if (progressDialog != null && progressDialog.isShowing()) {
                            progressDialog.dismiss();
                            CustomUtils.toggleWindowInteraction((Activity) view.getContext(), true);
                        }
                    }
                };
                progressBarTask.onStart();

                _notification.post.get()
                        .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                            @Override
                            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                                if (task.isSuccessful()) {
                                    DocumentSnapshot result = task.getResult();
                                    if (result != null && result.exists()) {
                                        progressBarTask.onComplete();
                                        String id = _notification.post.getId();
                                        String title = Objects.requireNonNull(result.get("postTitle")).toString();
                                        String body = Objects.requireNonNull(result.get("postBody")).toString();
                                        DocumentReference authorRef = (DocumentReference) result.get("postAuthor");
                                        Timestamp timestamp = (Timestamp) result.get("timestamp");
                                        ArrayList<String> images = new ArrayList<>((List<String>) result.get("postImages"));
                                        boolean canDisplay = (boolean) result.get("canDisplay");

                                        assert authorRef != null;
                                        assert timestamp != null;

                                        //--------- Set notification as checked --------//
                                        db.collection("users").document(currentUserID)
                                                .collection("notifications")
                                                .document(_notification.id)
                                                .update("isChecked", true)
                                                .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                    @Override
                                                    public void onComplete(@NonNull Task<Void> task) {
                                                        holder.notif.setBackgroundColor(Color.WHITE);
                                                    }
                                                });

                                        //-------- Go to Post Activity -------//
                                        if (canDisplay) {
                                            Post _post = new Post(id, title, body, authorRef.getPath(), timestamp, images);
                                            Intent intent = new Intent(view.getContext(), PostActivity.class);
                                            intent.putExtra("post", _post);
                                            view.getContext().startActivity(intent);
                                        } else {
                                            Toast.makeText(view.getContext(), "Cannot open! Question has been deleted.", Toast.LENGTH_SHORT).show();
                                        }
                                    }
                                } else {
                                    progressBarTask.onComplete();
                                }
                            }
                        });
            }
        });

        //---------------- Clicking Options ------------------//
        holder.btnOptions.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showNotificationOptionsBottomSheetDialog(view.getContext(), _notification, position);
            }
        });
    }

    // total number of rows
    @Override
    public int getItemCount() {
        return mData.size();
    }

    // convenience method for getting data at click position
    Notification getItem(int id) {
        return mData.get(id);
    }

    public void removeItemAt(int position) {
        mData.remove(position);
        notifyItemRemoved(position);
    }

    void showNotificationOptionsBottomSheetDialog(Context context, Notification notification, int position) {
        final BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(context);
        bottomSheetDialog.setContentView(R.layout.notification_options_bottom_sheet_dialog);

        LinearLayout delete = bottomSheetDialog.findViewById(R.id.notification_option_remove);

        delete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                bottomSheetDialog.dismiss();

                View rootView = ((Activity) context).getWindow().getDecorView().findViewById(android.R.id.content);

                Dialog progressDialog = CustomUtils.createProgressDialog((Activity) context);
                IProgressBarTask progressBarTask = new IProgressBarTask() {
                    @Override
                    public void onStart() {
                        if (progressDialog != null && !progressDialog.isShowing()) {
                            progressDialog.show();
                            CustomUtils.toggleWindowInteraction((Activity) context, false);
                        }
                    }

                    @Override
                    public void onComplete() {
                        if (progressDialog != null && progressDialog.isShowing()) {
                            progressDialog.dismiss();
                            CustomUtils.toggleWindowInteraction((Activity) context, true);
                        }
                    }
                };
                progressBarTask.onStart();

                db.collection("users").document(currentUserID)
                        .collection("notifications").document(notification.id)
                        .update("canDisplay", false)
                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                if (task.isSuccessful()) {
                                    removeItemAt(position);
                                    Snackbar.make(rootView, context.getResources().getString(R.string.notification_removed), Snackbar.LENGTH_LONG)
                                            .show();
                                } else {
                                    Snackbar.make(rootView, context.getResources().getString(R.string.notification_not_removed), Snackbar.LENGTH_LONG)
                                            .setBackgroundTint(Color.RED)
                                            .setTextColor(Color.WHITE)
                                            .show();
                                }
                                progressBarTask.onComplete();
                            }
                        });
            }
        });

        bottomSheetDialog.show();
    }

    Spanned getNotificationBody(String boldText1, String normalText, String boldText2) {
        if (boldText2 == null) {
            return Html.fromHtml("<b>" + boldText1 + "</b> " + normalText + ".", Html.FROM_HTML_MODE_LEGACY);
        } else {
            return Html.fromHtml("<b>" + boldText1 + "</b> " + normalText + " <b><i>" + boldText2 + "</i></b>.", Html.FROM_HTML_MODE_LEGACY);
        }
    }

}

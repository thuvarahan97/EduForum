package com.thuvarahan.eduforum.ui.posts_replies;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.thuvarahan.eduforum.CustomUtils;
import com.thuvarahan.eduforum.R;
import com.thuvarahan.eduforum.data.login.LoginDataSource;
import com.thuvarahan.eduforum.data.login.LoginRepository;
import com.thuvarahan.eduforum.data.reply.Reply;
import com.thuvarahan.eduforum.interfaces.IAlertDialogTask;
import com.thuvarahan.eduforum.interfaces.IEditDialogTask;
import com.thuvarahan.eduforum.services.network_broadcast.NetworkChangeReceiver;

import org.jetbrains.annotations.NotNull;

import java.util.List;

import static com.thuvarahan.eduforum.PostActivity.showEditDialogBox;

public class RVRepliesAdapter extends RecyclerView.Adapter<RVRepliesAdapter.ViewHolder> {

    private List<Reply> mData;

    String currentUserID = "";

    FirebaseFirestore db = FirebaseFirestore.getInstance();

    // data is passed into the constructor
    public RVRepliesAdapter(List<Reply> data) {
        this.mData = data;

        LoginRepository loginRepository = LoginRepository.getInstance(new LoginDataSource());
        if (loginRepository.getUser() != null) {
            currentUserID = loginRepository.getUser().getUserID();
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView body;
        private final TextView author;
        private final TextView timestamp;
        private final Button btnOptions;

        public ViewHolder(View view) {
            super(view);
            // Define click listener for the ViewHolder's View

            body = (TextView) view.findViewById(R.id.reply_body);
            author = (TextView) view.findViewById(R.id.reply_author_name);
            timestamp = (TextView) view.findViewById(R.id.reply_timestamp);
            btnOptions = (Button) view.findViewById(R.id.reply_options);
        }
    }

    /// Create new views (invoked by the layout manager)
    @NotNull
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        // Create a new view, which defines the UI of the list item
        View view = LayoutInflater.from(viewGroup.getContext())
                .inflate(R.layout.reply_single, viewGroup, false);

        return new ViewHolder(view);
    }

    // binds the data to the TextView in each row
    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Reply _reply = getItem(position);
        holder.body.setText(_reply.body);
        holder.author.setText("");

        //---------------- Fetch Author's Name ---------------//
        _reply.author.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    DocumentSnapshot doc = task.getResult();
                    if (doc != null && doc.getData() != null && doc.getData().get("displayName") != null) {
                        String authorVal = doc.getData().get("displayName").toString();
                        holder.author.setText(authorVal);
                    } else {
                        holder.author.setText(holder.itemView.getContext().getResources().getString(R.string.unknown_author));
                    }
                } else {
                    holder.author.setText(holder.itemView.getContext().getResources().getString(R.string.unknown_author));
                }
            }
        });

        //---------------- Convert Date Format ---------------//
        String date = CustomUtils.formatTimestamp(_reply.timestamp);
        holder.timestamp.setText(date);

        //---------------- Enable/Disable options --------------//
        if (currentUserID.equals(_reply.author.getId())) {
            holder.btnOptions.setEnabled(true);
            holder.btnOptions.setVisibility(View.VISIBLE);
        } else {
            holder.btnOptions.setEnabled(false);
            holder.btnOptions.setVisibility(View.GONE);
        }

        //---------------- Clicking Options ------------------//
        holder.btnOptions.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showReplyOptionsBottomSheetDialog(view.getContext(), _reply, position);
            }
        });

    }

    // total number of rows
    @Override
    public int getItemCount() {
        return mData.size();
    }

    // convenience method for getting data at click position
    Reply getItem(int id) {
        return mData.get(id);
    }

    public void removeItemAt(int position) {
        mData.remove(position);
        notifyItemRemoved(position);
        notifyItemRangeChanged(position, mData.size());
    }

    public void replaceItemAt(int position, Reply reply) {
        mData.set(position, reply);
        notifyItemRemoved(position);
        notifyItemRangeChanged(position, mData.size());
    }

    void showReplyOptionsBottomSheetDialog(Context context, Reply reply, int position) {
        final BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(context);
        bottomSheetDialog.setContentView(R.layout.reply_options_bottom_sheet_dialog);

        LinearLayout edit = bottomSheetDialog.findViewById(R.id.reply_option_edit);
        LinearLayout delete = bottomSheetDialog.findViewById(R.id.reply_option_delete);

        DocumentReference replyAuthorRef = reply.author;

        assert edit != null;
        assert delete != null;

        if (currentUserID.equals(replyAuthorRef.getId())) {
            edit.setVisibility(View.VISIBLE);
            delete.setVisibility(View.VISIBLE);
        } else {
            edit.setVisibility(View.GONE);
            delete.setVisibility(View.GONE);
        }

        edit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                View rootView = ((Activity) context).getWindow().getDecorView().findViewById(android.R.id.content);
                showEditDialogBox(context, rootView, null, reply.body, false, null, reply, new IEditDialogTask() {
                    @Override
                    public void onUpdated(String title, String body) {
                        reply.body = body;
                        replaceItemAt(position, reply);
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
                        context.getResources().getString(R.string.delete_answer_alert_title),
                        context.getResources().getString(R.string.delete_answer_alert_message),
                        context.getResources().getString(R.string.delete_answer_alert_yes),
                        context.getResources().getString(R.string.no),
                        new IAlertDialogTask() {

                            @Override
                            public void onPressedYes(DialogInterface alertDialog) {
                                View rootView = ((Activity) context).getWindow().getDecorView().findViewById(android.R.id.content);
                                if (NetworkChangeReceiver.isOnline(context)) {
                                    final ProgressDialog progressDialog = new ProgressDialog(context, R.style.ProgressDialogSpinnerOnly);
                                    progressDialog.setCancelable(false);
                                    progressDialog.show();

                                    db.collection("posts").document(reply.postID)
                                            .collection("replies").document(reply.id)
                                            .update("canDisplay", false)
                                            .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                @Override
                                                public void onComplete(@NonNull Task<Void> task) {
                                                    if (task.isSuccessful()) {
                                                        removeItemAt(position);
                                                        Snackbar.make(rootView, context.getResources().getString(R.string.answer_deleted), Snackbar.LENGTH_LONG)
                                                                .show();
                                                    } else {
                                                        Snackbar.make(rootView, context.getResources().getString(R.string.answer_not_deleted), Snackbar.LENGTH_LONG)
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

}

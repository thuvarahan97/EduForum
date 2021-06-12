package com.thuvarahan.eduforum.ui.posts_replies;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatButton;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.squareup.picasso.Picasso;
import com.thuvarahan.eduforum.R;
import com.thuvarahan.eduforum.PostActivity;
import com.thuvarahan.eduforum.CustomUtils;
import com.thuvarahan.eduforum.data.login.LoginDataSource;
import com.thuvarahan.eduforum.data.login.LoginRepository;
import com.thuvarahan.eduforum.data.post.Post;
import com.thuvarahan.eduforum.interfaces.IAlertDialogTask;

import org.jetbrains.annotations.NotNull;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

public class RVPostsAdapter extends RecyclerView.Adapter<RVPostsAdapter.ViewHolder> {

    private List<Post> mData;

    String currentUserID = "";

    FirebaseFirestore db = FirebaseFirestore.getInstance();

    // data is passed into the constructor
    public RVPostsAdapter(List<Post> data) {
        this.mData = data;

        LoginRepository loginRepository = LoginRepository.getInstance(new LoginDataSource());
        currentUserID = loginRepository.getUser().getUserID();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView title;
        private final TextView body;
        private final TextView author;
        private final TextView timestamp;
        private final TextView replies_count;
        private final ImageView image;
        private final AppCompatButton view_btn;
        private final Button btnOptions;

        public ViewHolder(View view) {
            super(view);
            // Define click listener for the ViewHolder's View

            title = (TextView) view.findViewById(R.id.post_title);
            body = (TextView) view.findViewById(R.id.post_body);
            author = (TextView) view.findViewById(R.id.post_author_name);
            timestamp = (TextView) view.findViewById(R.id.post_timestamp);
            replies_count = (TextView) view.findViewById(R.id.post_replies_count);
            image = (ImageView) view.findViewById(R.id.post_img);
            view_btn = (AppCompatButton) view.findViewById(R.id.post_view_btn);
            btnOptions = (Button) view.findViewById(R.id.post_options);
        }
    }

    /// Create new views (invoked by the layout manager)
    @NotNull
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        // Create a new view, which defines the UI of the list item
        View view = LayoutInflater.from(viewGroup.getContext())
                .inflate(R.layout.post_single, viewGroup, false);

        return new ViewHolder(view);
    }

    // binds the data to the TextView in each row
    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Post _post = getItem(position);
        holder.title.setText(_post.title);
        holder.body.setText(_post.body);

        //---------------- Fetch Author's Name ---------------//
        holder.author.setText("");

        db.document(_post.authorRef).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    DocumentSnapshot doc = task.getResult();
                    if (doc != null && doc.getData() != null && doc.getData().get("displayName") != null) {
                        String authorVal = doc.getData().get("displayName").toString();
                        holder.author.setText(authorVal);
                    }
                    else {
                        holder.author.setText(holder.itemView.getContext().getResources().getString(R.string.unknown_author));
                    }
                }
                else {
                    holder.author.setText(holder.itemView.getContext().getResources().getString(R.string.unknown_author));
                }
            }
        });


        //---------------- Convert Date Format ---------------//
        String date = CustomUtils.formatTimestamp(_post.timestamp);
        holder.timestamp.setText(date);

        //----------------- Display Image ------------//
        URL url = null;
        if (_post.images.size() > 0) {
            try {
                url = new URL(_post.images.get(0));

                Picasso.get().load(url.toString()).into(holder.image);
                holder.image.setVisibility(View.VISIBLE);
            } catch (MalformedURLException e) {
                holder.image.setVisibility(View.GONE);
                e.printStackTrace();
            }
        }


        //---------------- Fetch Replies Count ---------------//
        holder.replies_count.setText("");

        db.collection("posts")
        .document(_post.id)
        .collection("replies")
        .whereEqualTo("canDisplay", true)
        .orderBy("timestamp", Query.Direction.DESCENDING)
        .get()
        .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if (task.isSuccessful()) {
                    QuerySnapshot result = task.getResult();
                    if (result == null || result.isEmpty()) {
                        holder.replies_count.setText("0" + holder.itemView.getContext().getResources().getString(R.string.answers));
                        return;
                    }

                    if (result.size() == 1) {
                        holder.replies_count.setText("1" + holder.itemView.getContext().getResources().getString(R.string.answer));
                    }
                    else {
                        holder.replies_count.setText(result.size() + holder.itemView.getContext().getResources().getString(R.string.answers));
                    }
                }
                else {
                    holder.replies_count.setText("0" + holder.itemView.getContext().getResources().getString(R.string.answers));
                }
            }
        });

        //---------------- Clicking View Post ----------------//
        holder.view_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(view.getContext(), PostActivity.class);
                intent.putExtra("post", _post);
                view.getContext().startActivity(intent);
            }
        });

        //---------------- Enable/Disable options --------------//
        if (currentUserID.equals(db.document(_post.authorRef).getId())) {
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
                showPostOptionsBottomSheetDialog(view.getContext(), _post, position);
            }
        });
    }

    // total number of rows
    @Override
    public int getItemCount() {
        return mData.size();
    }

    // convenience method for getting data at click position
    Post getItem(int id) {
        return mData.get(id);
    }

    public void removeItemAt(int position) {
        mData.remove(position);
        notifyItemRemoved(position);
        notifyItemRangeChanged(position, mData.size());
    }

    void showPostOptionsBottomSheetDialog(Context context, Post post, int position) {
        final BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(context);
        bottomSheetDialog.setContentView(R.layout.post_options_bottom_sheet_dialog);

        LinearLayout edit = bottomSheetDialog.findViewById(R.id.post_option_edit);
        LinearLayout delete = bottomSheetDialog.findViewById(R.id.post_option_delete);
        LinearLayout copyLink = bottomSheetDialog.findViewById(R.id.post_option_copy_link);

        DocumentReference postAuthorRef = db.document(post.authorRef);

        assert edit != null;
        assert delete != null;
        assert copyLink != null;

        if (currentUserID.equals(postAuthorRef.getId())) {
//            edit.setVisibility(View.VISIBLE);
            delete.setVisibility(View.VISIBLE);
        } else {
//            edit.setVisibility(View.GONE);
            delete.setVisibility(View.GONE);
        }

        /*edit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(context, "Copy is Clicked ", Toast.LENGTH_LONG).show();
                bottomSheetDialog.dismiss();
            }
        });*/

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
                                    removeItemAt(position);
                                    Snackbar.make(view, context.getResources().getString(R.string.question_deleted), Snackbar.LENGTH_LONG)
                                    .show();
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

        /*copyLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Uri link = CustomUtils.getAppLink();
                link = Uri.parse("https://eduforum.dra.agconnect.link/aQn");
                CustomUtils.copyTextToClipboard(view.getContext(), link.toString());
                Toast.makeText(context, link.getPath(), Toast.LENGTH_LONG).show();
                bottomSheetDialog.dismiss();
            }
        });*/

        bottomSheetDialog.show();
    }

    /*void showPostOptionsPopupMenu(Context context, Button btnOptions) {
        //creating a popup menu
        PopupMenu popup = new PopupMenu(context, btnOptions);
        //inflating menu from xml resource
        popup.inflate(R.menu.post_options_menu);
        //adding click listener
        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.post_option_edit:
                        //handle menu1 click
                        return true;
                    case R.id.post_option_delete:
                        //handle menu2 click
                        return true;
                    case R.id.post_option_copy_link:
                        //handle menu3 click
                        return true;
                    default:
                        return false;
                }
            }
        });
        //displaying the popup
        popup.show();
    }*/

}

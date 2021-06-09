package com.thuvarahan.eduforum.ui.post;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatButton;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.squareup.picasso.Picasso;
import com.thuvarahan.eduforum.R;
import com.thuvarahan.eduforum.PostActivity;
import com.thuvarahan.eduforum.CustomUtils;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

public class RVPostsAdapter extends RecyclerView.Adapter<RVPostsAdapter.ViewHolder> {

    private List<Post> mData;

    FirebaseFirestore db = FirebaseFirestore.getInstance();

    public static final int OPEN_NEW_ACTIVITY = 123456;

    // data is passed into the constructor
    public RVPostsAdapter(List<Post> data) {
        this.mData = data;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView title;
        private final TextView body;
        private final TextView author;
        private final TextView timestamp;
        private final TextView replies_count;
        private final ImageView image;
        private final AppCompatButton view_btn;

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
        }
    }

    /// Create new views (invoked by the layout manager)
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
        Post _post = mData.get(position);
        holder.title.setText(_post.title);
        holder.body.setText(_post.body);

        //---------------- Fetch Author's Name ---------------//
        holder.author.setText("");

        db.document(_post.authorRef).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    DocumentSnapshot doc = task.getResult();
                    if (doc.getData() != null && doc.getData().get("displayName") != null) {
                        String authorVal = doc.getData().get("displayName").toString();
                        holder.author.setText(authorVal);
                    }
                    else {
                        holder.author.setText("Unknown");
                    }
                }
                else {
                    holder.author.setText("Unknown");
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
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if (task.isSuccessful()) {
                    QuerySnapshot result = task.getResult();
                    if (result == null || result.isEmpty()) {
                        holder.replies_count.setText("0" + " replies");
                        return;
                    }

                    if (result.size() == 1) {
                        holder.replies_count.setText("1" + " reply");
                    }
                    else {
                        holder.replies_count.setText(result.size() + " replies");
                    }
                }
                else {
                    holder.replies_count.setText("0" + " replies");
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

}

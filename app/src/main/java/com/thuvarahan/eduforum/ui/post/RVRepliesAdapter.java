package com.thuvarahan.eduforum.ui.post;

import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.thuvarahan.eduforum.CustomUtils;
import com.thuvarahan.eduforum.R;

import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class RVRepliesAdapter extends RecyclerView.Adapter<RVRepliesAdapter.ViewHolder> {

    private List<Reply> mData;

    FirebaseFirestore db = FirebaseFirestore.getInstance();

    // data is passed into the constructor
    public RVRepliesAdapter(List<Reply> data) {
        this.mData = data;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView body;
        private final TextView author;
        private final TextView timestamp;

        public ViewHolder(View view) {
            super(view);
            // Define click listener for the ViewHolder's View

            body = (TextView) view.findViewById(R.id.reply_body);
            author = (TextView) view.findViewById(R.id.reply_author_name);
            timestamp = (TextView) view.findViewById(R.id.reply_timestamp);
        }
    }

    /// Create new views (invoked by the layout manager)
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
        Reply _reply = mData.get(position);
        holder.body.setText(_reply.body);
        holder.author.setText("");

        //---------------- Fetch Author's Name ---------------//
        _reply.author.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
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
        String date = CustomUtils.formatTimestamp(_reply.timestamp);
        holder.timestamp.setText(date);

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

}

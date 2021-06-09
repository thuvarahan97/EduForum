package com.thuvarahan.eduforum.ui.profile;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.thuvarahan.eduforum.NewPostActivity;
import com.thuvarahan.eduforum.R;
import com.thuvarahan.eduforum.ui.home.HomeViewModel;
import com.thuvarahan.eduforum.ui.post.Post;
import com.thuvarahan.eduforum.ui.post.RVPostsAdapter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class ProfileFragment extends Fragment {

    RVPostsAdapter rvAdapter;

    SwipeRefreshLayout swipeRefresh;

    FirebaseFirestore db = FirebaseFirestore.getInstance();

    ArrayList<Post> posts = new ArrayList<Post>();

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_home, container, false);

        FloatingActionButton fab = root.findViewById(R.id.newpost_fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(view.getContext(), NewPostActivity.class);
                view.getContext().startActivity(intent);
            }
        });

        swipeRefresh = root.findViewById(R.id.swipe_refresh);
        swipeRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                fetchData(getContext(), db);
            }
        });

        fetchData(getContext(), db);
        return root;
    }

    public void fetchData(Context context, FirebaseFirestore db) {
        String TAG = "Data Fetch: ";

        DocumentReference userRef = db.collection("users").document("LgxX9SfYHkDX4zKWmJ9Q");

        db.collection("posts")
                .whereEqualTo("postAuthor", userRef)
                .whereEqualTo("canDisplay", true)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            QuerySnapshot result = task.getResult();
                            if (result == null || result.isEmpty()) {
                                stopRefreshing();
                                return;
                            }

                            posts.clear();

                            for (QueryDocumentSnapshot document : result) {
                                Log.d(TAG, document.getId() + " => " + document.getData());

                                Map<String, Object> data = document.getData();
                                String id = document.getId().toString();
                                String title = Objects.requireNonNull(data.get("postTitle")).toString();
                                String body = Objects.requireNonNull(data.get("postBody")).toString();
                                DocumentReference authorRef = (DocumentReference) data.get("postAuthor");
                                Timestamp timestamp = (Timestamp) data.get("timestamp");
                                ArrayList<String> images = new ArrayList<>((List<String>) data.get("postImages"));

                                assert authorRef != null;
                                assert timestamp != null;
                                Post post = new Post(id, title, body, authorRef.getPath(), timestamp, images);
                                posts.add(post);
                                showRecyclerView();
                                stopRefreshing();
                            }
                        } else {
                            Snackbar snackbar = Snackbar.make(getView(),"Unable to retrieve posts! Try again.", Snackbar.LENGTH_LONG).setAction("Action", null);
                            snackbar.show();
                    /*Toast toast = Toast.makeText(context, "Unable to retrieve posts! Try again.", Toast.LENGTH_SHORT);
                    toast.show();*/
                            Log.w(TAG, "Error getting documents.", task.getException());
                            stopRefreshing();
                        }
                    }
                });

    }

    void showRecyclerView() {
        ArrayList<Post> allposts = this.posts;

        // set up the RecyclerView
        RecyclerView recyclerView = (RecyclerView) getView().findViewById(R.id.recycler_posts_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        rvAdapter = new RVPostsAdapter(allposts);
        recyclerView.setAdapter(rvAdapter);
    }

    void stopRefreshing() {
        if (swipeRefresh.isRefreshing()) {
            swipeRefresh.setRefreshing(false);
        }
    }

    @Override
    public void onResume() {
        fetchData(getContext(), db);
        super.onResume();
    }
}
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
import androidx.appcompat.widget.AppCompatButton;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.gms.tasks.OnCanceledListener;
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
import com.thuvarahan.eduforum.CustomUtils;
import com.thuvarahan.eduforum.NewPostActivity;
import com.thuvarahan.eduforum.R;
import com.thuvarahan.eduforum.data.login.LoginDataSource;
import com.thuvarahan.eduforum.data.login.LoginRepository;
import com.thuvarahan.eduforum.ui.login.LoginActivity;
import com.thuvarahan.eduforum.data.post.Post;
import com.thuvarahan.eduforum.ui.posts_replies.RVPostsAdapter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class ProfileFragment extends Fragment {

    RecyclerView recyclerView;
    RVPostsAdapter rvAdapter;
    SwipeRefreshLayout swipeRefresh;
    AppCompatButton btnLogout;
    TextView profileDisplayName;
    TextView profileEmail;
    TextView tvUnavailable;

    String currentUserID = "";

    FirebaseFirestore db = FirebaseFirestore.getInstance();

    ArrayList<Post> posts = new ArrayList<Post>();

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_profile, container, false);

        recyclerView = root.findViewById(R.id.recycler_posts_view);
        tvUnavailable = root.findViewById(R.id.text_unavailable);
        profileDisplayName = root.findViewById(R.id.profile_header_displayname);
        profileEmail = root.findViewById(R.id.profile_header_email);

        LoginRepository loginRepository = LoginRepository.getInstance(new LoginDataSource());
        profileDisplayName.setText(loginRepository.getUser().getDisplayName());
        profileEmail.setText(loginRepository.getUser().getUsername());

        currentUserID = loginRepository.getUser().getUserID();

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

        btnLogout = root.findViewById(R.id.btnLogout);
        btnLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loginRepository.logout();
                CustomUtils.clearLocalUserData(getContext());

                Intent intent = new Intent(getContext(), LoginActivity.class);
                startActivity(intent);
                getActivity().finish();
            }
        });

        fetchData(getContext(), db);
        return root;
    }

    public void fetchData(Context context, FirebaseFirestore db) {
        String TAG = "Data Fetch: ";

        DocumentReference userRef = db.collection("users").document(currentUserID);

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
                    if (result != null && !result.isEmpty()) {
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
                        }
                        stopRefreshing();
                        showRecyclerView();
                        toggleUnavailableText();
                    } else {
                        stopRefreshing();
                        toggleUnavailableText();
                    }
                } else {
                    Snackbar.make(getView(),getResources().getString(R.string.unable_to_retrieve_posts), Snackbar.LENGTH_LONG).show();
                    Log.w(TAG, "Error getting documents.", task.getException());
                    stopRefreshing();
                    toggleUnavailableText();
                }
            }
        })
        .addOnCanceledListener(new OnCanceledListener() {
            @Override
            public void onCanceled() {
                stopRefreshing();
                toggleUnavailableText();
            }
        });
    }

    void showRecyclerView() {
        ArrayList<Post> allposts = this.posts;

        if (allposts.size() > 0) {
            // set up the RecyclerView
            recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
            rvAdapter = new RVPostsAdapter(allposts);
            recyclerView.setAdapter(rvAdapter);
        }
    }

    void stopRefreshing() {
        if (swipeRefresh.isRefreshing()) {
            swipeRefresh.setRefreshing(false);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        fetchData(getContext(), db);
    }

    private void toggleUnavailableText() {
        if (posts.size() == 0) {
            tvUnavailable.setText(getResources().getString(R.string.you_havent_posted_anything));
            ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) tvUnavailable.getLayoutParams();
            params.setMargins(0, 150, 0, 150);
            tvUnavailable.setLayoutParams(params);
            tvUnavailable.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            tvUnavailable.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }
}
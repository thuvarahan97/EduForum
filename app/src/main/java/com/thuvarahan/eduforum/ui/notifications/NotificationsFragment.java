package com.thuvarahan.eduforum.ui.notifications;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
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
import com.thuvarahan.eduforum.NewPostActivity;
import com.thuvarahan.eduforum.R;
import com.thuvarahan.eduforum.data.login.LoginDataSource;
import com.thuvarahan.eduforum.data.login.LoginRepository;
import com.thuvarahan.eduforum.data.notification.Notification;
import com.thuvarahan.eduforum.data.post.Post;
import com.thuvarahan.eduforum.ui.posts_replies.RVPostsAdapter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class NotificationsFragment extends Fragment {

    RecyclerView recyclerView;
    RVNotificationsAdapter rvAdapter;
    SwipeRefreshLayout swipeRefresh;
    TextView tvUnavailable;

    FirebaseFirestore db = FirebaseFirestore.getInstance();

    ArrayList<Notification> notifications = new ArrayList<>();

    String currentUserID = "";

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_notifications, container, false);
        setHasOptionsMenu(true);

        LoginRepository loginRepository = LoginRepository.getInstance(new LoginDataSource());
        if (loginRepository.getUser() != null) {
            currentUserID = loginRepository.getUser().getUserID();
        }

        recyclerView = root.findViewById(R.id.recycler_notifications_view);
        tvUnavailable = root.findViewById(R.id.text_unavailable);
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
        String TAG = "Notifications Fetch: ";

        db.collection("users").document(currentUserID)
        .collection("notifications")
        .whereEqualTo("canDisplay", true)
        .orderBy("timestamp", Query.Direction.DESCENDING)
        .get()
        .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if (task.isSuccessful()) {
                    QuerySnapshot result = task.getResult();
                    if (result != null && !result.isEmpty()) {
                        notifications.clear();

                        for (QueryDocumentSnapshot document : result) {
                            Log.d(TAG, document.getId() + " => " + document.getData());

                            Map<String, Object> data = document.getData();
                            String id = document.getId();
                            DocumentReference post = (DocumentReference) data.get("post");
                            DocumentReference author = (DocumentReference) data.get("author");
                            Timestamp timestamp = (Timestamp) data.get("timestamp");
                            boolean isChecked = (boolean) data.get("isChecked");

                            assert post != null;
                            assert author != null;
                            assert timestamp != null;

                            Notification notification = new Notification(id, post, author, timestamp, isChecked);
                            notifications.add(notification);
                        }
                        stopRefreshing();
                        showRecyclerView();
                        toggleUnavailableText();
                    } else {
                        stopRefreshing();
                        toggleUnavailableText();
                    }
                } else {
                    Snackbar.make(getView(), getResources().getString(R.string.unable_to_retrieve_notifications), Snackbar.LENGTH_LONG).show();
                    Log.w(TAG, "Error getting documents.", task.getException());
                    stopRefreshing();
                    toggleUnavailableText();
                }
            }
        }).addOnCanceledListener(new OnCanceledListener() {
            @Override
            public void onCanceled() {
                stopRefreshing();
                toggleUnavailableText();
            }
        });
    }

    void showRecyclerView() {
        ArrayList<Notification> allnotifications = this.notifications;

        if (allnotifications.size() > 0) {
            recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
            rvAdapter = new RVNotificationsAdapter(allnotifications);
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
        if (notifications.size() == 0) {
            tvUnavailable.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            tvUnavailable.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_refresh) {
            swipeRefresh.post(new Runnable() {
                @Override
                public void run() {
                    if (!swipeRefresh.isRefreshing()) {
                        swipeRefresh.setRefreshing(true);
                        fetchData(getContext(), db);
                    }
                }
            });
        }
        return super.onOptionsItemSelected(item);
    }
}
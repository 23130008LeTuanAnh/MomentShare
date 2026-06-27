package com.example.momentshare.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.momentshare.R;
import com.example.momentshare.model.User;
import com.example.momentshare.repository.FriendRepository;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.List;

public class FriendListActivity extends AppCompatActivity {

    private ImageButton btnBack, btnAdd;
    private ProgressBar progressBar;
    private TextView txtNoFriend;
    private RecyclerView rvFriendList;

    private FriendAdapter adapter;
    private final List<User> friendList = new ArrayList<>();
    private FriendRepository friendRepository;
    private String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_friend_list);

        currentUserId = FirebaseAuth.getInstance().getCurrentUser() != null ?
                FirebaseAuth.getInstance().getCurrentUser().getUid() : "";

        if (currentUserId.isEmpty()) {
            finish();
            return;
        }

        friendRepository = new FriendRepository();
        initViews();
        setupEvents();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadFriendList();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBackFriendList);
        btnAdd = findViewById(R.id.btnAddFriend);
        progressBar = findViewById(R.id.progressBarFriendList);
        txtNoFriend = findViewById(R.id.txtNoFriend);
        rvFriendList = findViewById(R.id.rvFriendList);

        rvFriendList.setLayoutManager(new LinearLayoutManager(this));
        adapter = new FriendAdapter(this, friendList, false);
        rvFriendList.setAdapter(adapter);
    }

    private void setupEvents() {
        btnBack.setOnClickListener(v -> finish());
        btnAdd.setOnClickListener(v -> startActivity(new Intent(this, SearchFriendActivity.class)));
    }

    private void loadFriendList() {
        setLoading(true);
        friendRepository.getFriendList(currentUserId, new FriendRepository.UserListCallback() {
            @Override
            public void onSuccess(List<User> users) {
                setLoading(false);
                friendList.clear();
                friendList.addAll(users);
                adapter.notifyDataSetChanged();

                if (users.isEmpty()) {
                    txtNoFriend.setVisibility(View.VISIBLE);
                } else {
                    txtNoFriend.setVisibility(View.GONE);
                }
            }

            @Override
            public void onFailure(String errorMessage) {
                setLoading(false);
                Toast.makeText(FriendListActivity.this, "Lỗi: " + errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setLoading(boolean isLoading) {
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        rvFriendList.setVisibility(isLoading ? View.GONE : View.VISIBLE);
        txtNoFriend.setVisibility(View.GONE);
    }
}

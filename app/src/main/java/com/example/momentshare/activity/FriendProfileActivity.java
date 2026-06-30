package com.example.momentshare.activity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.momentshare.R;
import com.example.momentshare.model.User;
import com.example.momentshare.repository.FriendRepository;
import com.example.momentshare.repository.UserRepository;
import com.google.firebase.auth.FirebaseAuth;

public class FriendProfileActivity extends AppCompatActivity {

    public static final String EXTRA_FRIEND_ID = "extra_friend_id";

    private ImageView imgAvatar;
    private TextView txtName, txtUsername, txtBio;
    private Button btnChat, btnUnfriend;
    private ImageButton btnBack;

    private FriendRepository friendRepository;
    private UserRepository userRepository;
    private String currentUserId;
    private String friendId;
    private String friendName = "";
    private String friendAvatarUrl = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_friend_profile);

        friendId = getIntent().getStringExtra(EXTRA_FRIEND_ID);
        currentUserId = FirebaseAuth.getInstance().getCurrentUser() != null ?
                FirebaseAuth.getInstance().getCurrentUser().getUid() : "";

        if (friendId == null || currentUserId.isEmpty()) {
            finish();
            return;
        }

        friendRepository = new FriendRepository();
        userRepository = new UserRepository();

        initViews();
        loadFriendData();
        setupEvents();
    }

    private void initViews() {
        imgAvatar = findViewById(R.id.imgFriendProfileAvatar);
        txtName = findViewById(R.id.txtFriendProfileName);
        txtUsername = findViewById(R.id.txtFriendProfileUsername);
        txtBio = findViewById(R.id.txtFriendProfileBio);
        btnChat = findViewById(R.id.btnFriendProfileChat);
        btnUnfriend = findViewById(R.id.btnFriendProfileUnfriend);
        btnBack = findViewById(R.id.btnBackFriendProfile);
    }

    private void loadFriendData() {
        userRepository.getUserById(friendId, new UserRepository.UserCallback() {
            @Override
            public void onSuccess(User user) {
                friendName = user.getFullName() == null || user.getFullName().trim().isEmpty()
                        ? "Bạn bè"
                        : user.getFullName().trim();
                friendAvatarUrl = user.getAvatarUrl() == null ? "" : user.getAvatarUrl();

                txtName.setText(friendName);
                txtUsername.setText("@" + user.getUsername());
                txtBio.setText(user.getBio() == null || user.getBio().isEmpty() ? "Chưa có bio" : user.getBio());

                if (!friendAvatarUrl.isEmpty()) {
                    Glide.with(FriendProfileActivity.this).load(friendAvatarUrl).circleCrop().into(imgAvatar);
                }
            }

            @Override
            public void onFailure(String errorMessage) {
                Toast.makeText(FriendProfileActivity.this, "Lỗi: " + errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupEvents() {
        btnBack.setOnClickListener(v -> finish());

        btnChat.setOnClickListener(v -> {
            Intent intent = new Intent(FriendProfileActivity.this, ChatActivity.class);
            intent.putExtra(ChatActivity.EXTRA_FRIEND_ID, friendId);
            intent.putExtra(ChatActivity.EXTRA_FRIEND_NAME, friendName);
            intent.putExtra(ChatActivity.EXTRA_FRIEND_AVATAR, friendAvatarUrl);
            startActivity(intent);
        });

        btnUnfriend.setOnClickListener(v -> {
            friendRepository.unfriend(currentUserId, friendId, new FriendRepository.ActionCallback() {
                @Override
                public void onSuccess() {
                    Toast.makeText(FriendProfileActivity.this, "Đã hủy kết bạn", Toast.LENGTH_SHORT).show();
                    finish();
                }

                @Override
                public void onFailure(String errorMessage) {
                    Toast.makeText(FriendProfileActivity.this, "Lỗi: " + errorMessage, Toast.LENGTH_SHORT).show();
                }
            });
        });
    }
}

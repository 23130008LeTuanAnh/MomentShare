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
import com.example.momentshare.model.ChatRoom;
import com.example.momentshare.repository.ChatRepository;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.List;

/**
 * Danh sách các cuộc trò chuyện 1-1 của người dùng hiện tại.
 */
public class ChatListActivity extends AppCompatActivity {

    private ImageButton btnBackChatList;
    private ImageButton btnRefreshChatList;
    private ImageButton btnOpenFriendList;
    private TextView txtEmptyChatList;
    private ProgressBar progressBarChatList;
    private RecyclerView rvChatRooms;

    private ChatRepository chatRepository;
    private ChatRoomAdapter adapter;
    private final List<ChatRoom> rooms = new ArrayList<>();

    private String currentUserId = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_list);

        currentUserId = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : "";

        if (currentUserId.trim().isEmpty()) {
            finish();
            return;
        }

        chatRepository = new ChatRepository();
        initViews();
        setupEvents();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadChatRooms();
    }

    private void initViews() {
        btnBackChatList = findViewById(R.id.btnBackChatList);
        btnRefreshChatList = findViewById(R.id.btnRefreshChatList);
        btnOpenFriendList = findViewById(R.id.btnOpenFriendList);
        txtEmptyChatList = findViewById(R.id.txtEmptyChatList);
        progressBarChatList = findViewById(R.id.progressBarChatList);
        rvChatRooms = findViewById(R.id.rvChatRooms);

        rvChatRooms.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ChatRoomAdapter(this, rooms, currentUserId);
        rvChatRooms.setAdapter(adapter);
    }

    private void setupEvents() {
        btnBackChatList.setOnClickListener(v -> finish());
        btnRefreshChatList.setOnClickListener(v -> loadChatRooms());
        btnOpenFriendList.setOnClickListener(v -> startActivity(new Intent(this, FriendListActivity.class)));
    }

    private void loadChatRooms() {
        setLoading(true);
        chatRepository.loadChatRooms(currentUserId, new ChatRepository.ChatRoomListCallback() {
            @Override
            public void onSuccess(List<ChatRoom> result) {
                setLoading(false);
                rooms.clear();
                rooms.addAll(result);
                adapter.notifyDataSetChanged();
                txtEmptyChatList.setVisibility(rooms.isEmpty() ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onFailure(String errorMessage) {
                setLoading(false);
                Toast.makeText(ChatListActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setLoading(boolean isLoading) {
        progressBarChatList.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        rvChatRooms.setVisibility(isLoading ? View.GONE : View.VISIBLE);
        if (isLoading) txtEmptyChatList.setVisibility(View.GONE);
    }
}

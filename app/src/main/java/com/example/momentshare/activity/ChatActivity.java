package com.example.momentshare.activity;

import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.momentshare.R;
import com.example.momentshare.model.ChatMessage;
import com.example.momentshare.model.ChatRoom;
import com.example.momentshare.model.User;
import com.example.momentshare.repository.ChatRepository;
import com.example.momentshare.repository.UserRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;

/**
 * Màn hình nhắn tin 1-1 với một người bạn.
 */
public class ChatActivity extends AppCompatActivity {

    public static final String EXTRA_FRIEND_ID = "extra_friend_id";
    public static final String EXTRA_FRIEND_NAME = "extra_friend_name";
    public static final String EXTRA_FRIEND_AVATAR = "extra_friend_avatar";

    private ImageButton btnBackChat;
    private ImageView imgChatAvatar;
    private TextView txtChatTitle;
    private TextView txtChatSubtitle;
    private ProgressBar progressBarChat;
    private RecyclerView rvMessages;
    private EditText edtMessage;
    private Button btnSendMessage;

    private ChatRepository chatRepository;
    private UserRepository userRepository;
    private ChatMessageAdapter adapter;
    private ListenerRegistration messageRegistration;

    private final List<ChatMessage> messages = new ArrayList<>();

    private String currentUserId = "";
    private String friendId = "";
    private String roomId = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        currentUserId = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : "";
        friendId = getIntent().getStringExtra(EXTRA_FRIEND_ID);
        if (friendId == null) friendId = "";

        if (currentUserId.trim().isEmpty() || friendId.trim().isEmpty()) {
            Toast.makeText(this, "Không xác định được cuộc trò chuyện", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        chatRepository = new ChatRepository();
        userRepository = new UserRepository();

        initViews();
        setupEvents();
        loadFriendHeader();
        openChatRoom();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (messageRegistration != null) {
            messageRegistration.remove();
            messageRegistration = null;
        }
    }

    private void initViews() {
        btnBackChat = findViewById(R.id.btnBackChat);
        imgChatAvatar = findViewById(R.id.imgChatAvatar);
        txtChatTitle = findViewById(R.id.txtChatTitle);
        txtChatSubtitle = findViewById(R.id.txtChatSubtitle);
        progressBarChat = findViewById(R.id.progressBarChat);
        rvMessages = findViewById(R.id.rvMessages);
        edtMessage = findViewById(R.id.edtMessage);
        btnSendMessage = findViewById(R.id.btnSendMessage);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        rvMessages.setLayoutManager(layoutManager);

        adapter = new ChatMessageAdapter(messages, currentUserId);
        rvMessages.setAdapter(adapter);
    }

    private void setupEvents() {
        btnBackChat.setOnClickListener(v -> finish());
        btnSendMessage.setOnClickListener(v -> sendMessage());

        edtMessage.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                sendMessage();
                return true;
            }
            return false;
        });
    }

    private void loadFriendHeader() {
        String friendName = getIntent().getStringExtra(EXTRA_FRIEND_NAME);
        String friendAvatar = getIntent().getStringExtra(EXTRA_FRIEND_AVATAR);

        if (friendName != null && !friendName.trim().isEmpty()) {
            txtChatTitle.setText(friendName.trim());
        } else {
            txtChatTitle.setText("Đang tải...");
        }

        if (friendAvatar != null && !friendAvatar.trim().isEmpty()) {
            Glide.with(this)
                    .load(friendAvatar)
                    .circleCrop()
                    .placeholder(R.mipmap.ic_launcher)
                    .error(R.mipmap.ic_launcher)
                    .into(imgChatAvatar);
        } else {
            imgChatAvatar.setImageResource(R.mipmap.ic_launcher);
        }

        userRepository.getUserById(friendId, new UserRepository.UserCallback() {
            @Override
            public void onSuccess(User user) {
                txtChatTitle.setText(buildDisplayName(user));
                txtChatSubtitle.setText("Nhắn tin riêng tư với bạn bè");
                if (user.getAvatarUrl() != null && !user.getAvatarUrl().trim().isEmpty()) {
                    Glide.with(ChatActivity.this)
                            .load(user.getAvatarUrl())
                            .circleCrop()
                            .placeholder(R.mipmap.ic_launcher)
                            .error(R.mipmap.ic_launcher)
                            .into(imgChatAvatar);
                }
            }

            @Override
            public void onFailure(String errorMessage) {
                txtChatSubtitle.setText("Không tải được hồ sơ bạn bè");
            }
        });
    }

    private void openChatRoom() {
        setLoading(true);
        chatRepository.openOrCreateRoom(currentUserId, friendId, new ChatRepository.ChatRoomCallback() {
            @Override
            public void onSuccess(ChatRoom room) {
                setLoading(false);
                roomId = room.getRoomId();
                listenMessages();
            }

            @Override
            public void onFailure(String errorMessage) {
                setLoading(false);
                Toast.makeText(ChatActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    private void listenMessages() {
        if (roomId == null || roomId.trim().isEmpty()) return;

        if (messageRegistration != null) {
            messageRegistration.remove();
            messageRegistration = null;
        }

        messageRegistration = chatRepository.listenMessages(roomId, new ChatRepository.MessageListCallback() {
            @Override
            public void onSuccess(List<ChatMessage> result) {
                messages.clear();
                messages.addAll(result);
                adapter.notifyDataSetChanged();
                if (!messages.isEmpty()) {
                    rvMessages.scrollToPosition(messages.size() - 1);
                }
            }

            @Override
            public void onFailure(String errorMessage) {
                Toast.makeText(ChatActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void sendMessage() {
        String text = edtMessage.getText() == null ? "" : edtMessage.getText().toString().trim();
        if (text.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập tin nhắn", Toast.LENGTH_SHORT).show();
            return;
        }

        btnSendMessage.setEnabled(false);
        chatRepository.sendTextMessage(currentUserId, friendId, text, new ChatRepository.ActionCallback() {
            @Override
            public void onSuccess() {
                btnSendMessage.setEnabled(true);
                edtMessage.setText("");
            }

            @Override
            public void onFailure(String errorMessage) {
                btnSendMessage.setEnabled(true);
                Toast.makeText(ChatActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setLoading(boolean isLoading) {
        progressBarChat.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        btnSendMessage.setEnabled(!isLoading);
        edtMessage.setEnabled(!isLoading);
    }

    private String buildDisplayName(User user) {
        if (user == null) return "Bạn bè";
        if (user.getFullName() != null && !user.getFullName().trim().isEmpty()) return user.getFullName().trim();
        if (user.getUsername() != null && !user.getUsername().trim().isEmpty()) return "@" + user.getUsername().trim();
        return "Bạn bè";
    }
}

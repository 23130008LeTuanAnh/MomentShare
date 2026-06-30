package com.example.momentshare.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
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

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

public class FriendListActivity extends AppCompatActivity {

    private static final Pattern DIACRITICS_PATTERN = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");

    private ImageButton btnBack;
    private ImageButton btnAdd;
    private ImageButton btnRefresh;
    private ImageButton btnChatList;
    private ProgressBar progressBar;
    private TextView txtNoFriend;
    private TextView txtFriendCount;
    private EditText edtSearchFriendList;
    private RecyclerView rvFriendList;

    private FriendAdapter adapter;
    private final List<User> allFriends = new ArrayList<>();
    private final List<User> displayedFriends = new ArrayList<>();
    private FriendRepository friendRepository;
    private String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_friend_list);

        currentUserId = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : "";

        if (currentUserId.isEmpty()) {
            Toast.makeText(this, "Bạn cần đăng nhập để xem danh sách bạn bè", Toast.LENGTH_SHORT).show();
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
        btnRefresh = findViewById(R.id.btnRefreshFriendList);
        btnChatList = findViewById(R.id.btnChatListFromFriendList);
        progressBar = findViewById(R.id.progressBarFriendList);
        txtNoFriend = findViewById(R.id.txtNoFriend);
        txtFriendCount = findViewById(R.id.txtFriendCount);
        edtSearchFriendList = findViewById(R.id.edtSearchFriendList);
        rvFriendList = findViewById(R.id.rvFriendList);

        rvFriendList.setLayoutManager(new LinearLayoutManager(this));

        adapter = new FriendAdapter(this, displayedFriends, false);

        adapter.setOnFriendRemovedListener(removedUser -> {
            removeUserFromList(allFriends, removedUser);
            removeUserFromList(displayedFriends, removedUser);

            adapter.notifyDataSetChanged();
            updateEmptyState();
        });

        rvFriendList.setAdapter(adapter);
    }

    private void setupEvents() {
        btnBack.setOnClickListener(v -> finish());

        btnAdd.setOnClickListener(v ->
                startActivity(new Intent(this, SearchFriendActivity.class))
        );

        btnRefresh.setOnClickListener(v -> loadFriendList());

        btnChatList.setOnClickListener(v ->
                startActivity(new Intent(this, ChatListActivity.class))
        );

        edtSearchFriendList.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // Không cần xử lý trước khi text thay đổi.
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterFriendList(s == null ? "" : s.toString());
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {
                // Không cần xử lý sau khi text thay đổi.
            }
        });
    }

    private void loadFriendList() {
        setLoading(true);

        friendRepository.getFriendList(currentUserId, new FriendRepository.UserListCallback() {
            @Override
            public void onSuccess(List<User> users) {
                setLoading(false);

                allFriends.clear();

                if (users != null) {
                    allFriends.addAll(users);
                }

                filterFriendList(edtSearchFriendList.getText().toString());
            }

            @Override
            public void onFailure(String errorMessage) {
                setLoading(false);
                Toast.makeText(FriendListActivity.this, "Lỗi: " + errorMessage, Toast.LENGTH_SHORT).show();
                updateEmptyState();
            }
        });
    }

    private void filterFriendList(String keyword) {
        String normalizedKeyword = normalizeSearchText(keyword);

        displayedFriends.clear();

        if (normalizedKeyword.isEmpty()) {
            displayedFriends.addAll(allFriends);
        } else {
            for (User user : allFriends) {
                if (matchesKeyword(user, normalizedKeyword)) {
                    displayedFriends.add(user);
                }
            }
        }

        adapter.notifyDataSetChanged();
        updateEmptyState();
    }

    private boolean matchesKeyword(User user, String normalizedKeyword) {
        if (user == null) {
            return false;
        }

        String fullName = normalizeSearchText(user.getFullName());
        String username = normalizeSearchText(user.getUsername());
        String email = normalizeSearchText(user.getEmail());

        return fullName.contains(normalizedKeyword)
                || username.contains(normalizedKeyword)
                || email.contains(normalizedKeyword);
    }

    private void updateEmptyState() {
        String keyword = edtSearchFriendList == null
                ? ""
                : edtSearchFriendList.getText().toString().trim();

        txtFriendCount.setText(
                "Tổng: " + allFriends.size()
                        + " bạn bè | Đang hiển thị: "
                        + displayedFriends.size()
        );

        rvFriendList.setVisibility(displayedFriends.isEmpty() ? View.GONE : View.VISIBLE);
        txtNoFriend.setVisibility(displayedFriends.isEmpty() ? View.VISIBLE : View.GONE);

        if (allFriends.isEmpty()) {
            txtNoFriend.setText("Bạn chưa có người bạn nào. Bấm nút + để tìm và kết bạn.");
        } else if (!keyword.isEmpty()) {
            txtNoFriend.setText("Không tìm thấy bạn bè phù hợp với từ khóa: " + keyword);
        } else {
            txtNoFriend.setText("Không có bạn bè để hiển thị");
        }
    }

    private void setLoading(boolean isLoading) {
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);

        if (isLoading) {
            rvFriendList.setVisibility(View.GONE);
            txtNoFriend.setVisibility(View.GONE);
        }

        btnAdd.setEnabled(!isLoading);
        btnRefresh.setEnabled(!isLoading);
        btnChatList.setEnabled(!isLoading);
    }

    /**
     * Xóa user khỏi danh sách.
     *
     * Không dùng users.removeIf(...) vì removeIf yêu cầu API 24,
     * trong khi project đang để minSdk = 23.
     */
    private void removeUserFromList(List<User> users, User removedUser) {
        if (users == null || removedUser == null || removedUser.getUserId() == null) {
            return;
        }

        String removedUserId = removedUser.getUserId();

        for (int i = users.size() - 1; i >= 0; i--) {
            User user = users.get(i);

            if (user != null && removedUserId.equals(user.getUserId())) {
                users.remove(i);
            }
        }
    }

    private String normalizeSearchText(String value) {
        if (value == null) {
            return "";
        }

        String normalized = Normalizer.normalize(
                value.trim().toLowerCase(Locale.ROOT),
                Normalizer.Form.NFD
        );

        normalized = DIACRITICS_PATTERN.matcher(normalized).replaceAll("");
        normalized = normalized.replace('đ', 'd').replace('Đ', 'd');

        return normalized;
    }
}

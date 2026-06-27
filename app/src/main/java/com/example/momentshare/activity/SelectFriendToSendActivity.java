package com.example.momentshare.activity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.SparseBooleanArray;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.momentshare.R;
import com.example.momentshare.model.FriendUser;
import com.example.momentshare.repository.MomentRepository;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.List;

public class SelectFriendToSendActivity extends AppCompatActivity {

    public static final String EXTRA_IMAGE_URI = "extra_image_uri";
    public static final String EXTRA_CAPTION = "extra_caption";
    public static final String EXTRA_RECEIVER_IDS = "extra_receiver_ids";

    private ListView listFriends;
    private TextView txtEmpty;
    private Button btnContinue;

    private final ArrayList<FriendUser> friendCandidates = new ArrayList<>();
    private ArrayAdapter<String> adapter;

    private MomentRepository momentRepository;
    private Uri imageUri;
    private String caption;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_friend_send);

        momentRepository = new MomentRepository();

        initViews();
        readIntentData();
        setupEvents();
        loadFriends();
    }

    private void initViews() {
        listFriends = findViewById(R.id.listFriendsToSend);
        txtEmpty = findViewById(R.id.txtEmptyFriends);
        btnContinue = findViewById(R.id.btnContinueSend);

        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbarSelectFriend);
        toolbar.setNavigationIcon(android.R.drawable.ic_media_previous);
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void readIntentData() {
        String uriString = getIntent().getStringExtra(EXTRA_IMAGE_URI);
        caption = getIntent().getStringExtra(EXTRA_CAPTION);

        if (uriString == null || uriString.trim().isEmpty()) {
            Toast.makeText(this, "Thiếu ảnh", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        imageUri = Uri.parse(uriString);
        if (caption == null) {
            caption = "";
        }
    }

    private void setupEvents() {
        btnContinue.setOnClickListener(v -> handleContinue());
    }

    private void loadFriends() {
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : null;

        if (currentUserId == null) {
            Toast.makeText(this, "Bạn cần đăng nhập lại", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        momentRepository.loadSelectableFriends(currentUserId, new MomentRepository.FriendsCallback() {
            @Override
            public void onSuccess(@androidx.annotation.NonNull List<FriendUser> friends) {
                friendCandidates.clear();
                friendCandidates.addAll(friends);

                if (friendCandidates.isEmpty()) {
                    txtEmpty.setVisibility(View.VISIBLE);
                    listFriends.setVisibility(View.GONE);
                    btnContinue.setEnabled(false);
                    return;
                }

                txtEmpty.setVisibility(View.GONE);
                listFriends.setVisibility(View.VISIBLE);
                btnContinue.setEnabled(true);

                ArrayList<String> labels = new ArrayList<>();
                for (FriendUser friend : friendCandidates) {
                    labels.add(formatFriendLabel(friend));
                }

                adapter = new ArrayAdapter<>(
                        SelectFriendToSendActivity.this,
                        android.R.layout.simple_list_item_multiple_choice,
                        labels
                );

                listFriends.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
                listFriends.setAdapter(adapter);
            }

            @Override
            public void onFailure(@androidx.annotation.NonNull String errorMessage) {
                Toast.makeText(SelectFriendToSendActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private String formatFriendLabel(FriendUser friend) {
        String fullName = friend.getFullName() == null ? "" : friend.getFullName();
        String username = friend.getUsername() == null ? "" : friend.getUsername();

        if (username.isEmpty()) {
            return fullName;
        }
        if (fullName.isEmpty()) {
            return "@" + username;
        }
        return fullName + "  @" + username;
    }

    private void handleContinue() {
        SparseBooleanArray checked = listFriends.getCheckedItemPositions();
        ArrayList<String> receiverIds = new ArrayList<>();

        for (int i = 0; i < friendCandidates.size(); i++) {
            if (checked.get(i)) {
                receiverIds.add(friendCandidates.get(i).getUserId());
            }
        }

        if (receiverIds.isEmpty()) {
            Toast.makeText(this, "Vui lòng chọn ít nhất một bạn", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(this, SendMomentActivity.class);
        intent.putExtra(EXTRA_IMAGE_URI, imageUri.toString());
        intent.putExtra(EXTRA_CAPTION, caption);
        intent.putStringArrayListExtra(EXTRA_RECEIVER_IDS, receiverIds);
        startActivity(intent);
    }
}

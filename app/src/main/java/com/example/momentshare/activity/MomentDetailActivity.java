package com.example.momentshare.activity;

import android.app.AlertDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.momentshare.R;
import com.example.momentshare.model.NotificationModel;
import com.example.momentshare.model.Reaction;
import com.example.momentshare.model.User;
import com.example.momentshare.repository.AdminRepository;
import com.example.momentshare.repository.MomentRepository;
import com.example.momentshare.repository.ReactionRepository;
import com.example.momentshare.repository.UserRepository;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * MomentDetailActivity hiển thị chi tiết khoảnh khắc.
 *
 * Đã chỉnh:
 * - Không hiện Toast lỗi kỹ thuật FAILED_PRECONDITION khi tải reaction.
 * - Nếu reaction query lỗi do index/mạng, giao diện detail vẫn hiển thị ảnh bình thường.
 */
public class MomentDetailActivity extends AppCompatActivity {

    private static final String TAG = "MomentDetailActivity";

    public static final String EXTRA_MOMENT_ID = "extra_moment_id";
    public static final String EXTRA_SENDER_ID = "extra_sender_id";
    public static final String EXTRA_IMAGE_URL = "extra_image_url";
    public static final String EXTRA_CAPTION = "extra_caption";
    public static final String EXTRA_CREATED_AT = "extra_created_at";

    private ImageView imgDetailMoment;
    private TextView txtDetailSender;
    private TextView txtDetailCaption;
    private TextView txtDetailTime;
    private TextView txtSelectedReaction;
    private Button btnReportMoment; // Người 5 thực hiện: nút để user gửi báo cáo nội dung.

    private RecyclerView rvReactions;
    private ReactionAdapter reactionAdapter;
    private ReactionRepository reactionRepository;
    private AdminRepository adminRepository; // Người 5 thực hiện: dùng để tạo report cho Admin xử lý.
    private UserRepository userRepository;

    private String currentMomentId = "";
    private String currentSenderId = ""; // Người 5 thực hiện: lưu người gửi để không cho tự báo cáo ảnh của mình.
    private String currentUserId;
    private String selectedReaction = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            Toast.makeText(this, "Vui lòng đăng nhập lại", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        setContentView(R.layout.activity_moment_detail);

        currentMomentId = getIntent().getStringExtra(EXTRA_MOMENT_ID);

        ImageButton btnBack = findViewById(R.id.btnBackDetail);
        imgDetailMoment = findViewById(R.id.imgDetailMoment);
        txtDetailSender = findViewById(R.id.txtDetailSender);
        txtDetailCaption = findViewById(R.id.txtDetailCaption);
        txtDetailTime = findViewById(R.id.txtDetailTime);
        txtSelectedReaction = findViewById(R.id.txtSelectedReaction);
        btnReportMoment = findViewById(R.id.btnReportMoment);
        rvReactions = findViewById(R.id.rvReactions);

        reactionRepository = new ReactionRepository();
        userRepository = new UserRepository();
        adminRepository = new AdminRepository();

        btnBack.setOnClickListener(v -> finish());

        showMomentData();

        rvReactions.setLayoutManager(new LinearLayoutManager(this));
        reactionAdapter = new ReactionAdapter(new ArrayList<>());
        rvReactions.setAdapter(reactionAdapter);

        if (currentMomentId != null && !currentMomentId.isEmpty()) {
            loadReactions();
            loadMyReaction();
        }

        setupReportButton();

        setupReactionButton(R.id.btnLove, "\u2764");
        setupReactionButton(R.id.btnHaha, "\uD83D\uDE02");
        setupReactionButton(R.id.btnWow, "\uD83D\uDE2E");
        setupReactionButton(R.id.btnSad, "\uD83D\uDE22");
        setupReactionButton(R.id.btnLike, "\uD83D\uDC4D");

        markMomentAsViewed();
    }

    private void showMomentData() {
        String senderId = getIntent().getStringExtra(EXTRA_SENDER_ID);
        currentSenderId = senderId == null ? "" : senderId;

        String imageUrl = getIntent().getStringExtra(EXTRA_IMAGE_URL);
        String caption = getIntent().getStringExtra(EXTRA_CAPTION);
        long createdAt = getIntent().getLongExtra(EXTRA_CREATED_AT, 0L);

        if (senderId == null || senderId.isEmpty()) {
            txtDetailSender.setText("Unknown sender");
        } else {
            userRepository.getUserById(senderId, new UserRepository.UserCallback() {
                @Override
                public void onSuccess(User user) {
                    String fullName = user == null ? "" : user.getFullName();
                    txtDetailSender.setText(fullName == null || fullName.trim().isEmpty()
                            ? "Unknown sender"
                            : fullName);
                }

                @Override
                public void onFailure(String errorMessage) {
                    txtDetailSender.setText("Unknown sender");
                }
            });
        }

        txtDetailCaption.setText(caption == null || caption.isEmpty() ? "No caption" : caption);
        txtDetailTime.setText(createdAt == 0L ? "Just now" : formatTime(createdAt));

        if (imageUrl != null && !imageUrl.isEmpty()) {
            Glide.with(this)
                    .load(imageUrl)
                    .placeholder(R.drawable.ic_launcher_background)
                    .into(imgDetailMoment);
        }
    }

    /**
     * Người 5 thực hiện: nối chức năng user tạo báo cáo nội dung từ màn hình chi tiết ảnh.
     */
    private void setupReportButton() {
        btnReportMoment.setOnClickListener(v -> {
            if (currentMomentId == null || currentMomentId.trim().isEmpty()) {
                Toast.makeText(this, "Không xác định được ảnh cần báo cáo", Toast.LENGTH_SHORT).show();
                return;
            }

            if (currentUserId != null && currentUserId.equals(currentSenderId)) {
                Toast.makeText(this, "Bạn không thể báo cáo ảnh của chính mình", Toast.LENGTH_SHORT).show();
                return;
            }

            showReportReasonDialog();
        });
    }

    /**
     * Người 5 thực hiện: cho user chọn lý do báo cáo thay vì tạo report rỗng.
     */
    private void showReportReasonDialog() {
        String[] reasons = {
                "Nội dung phản cảm",
                "Spam hoặc quảng cáo",
                "Quấy rối hoặc xúc phạm",
                "Nội dung không phù hợp khác"
        };

        new AlertDialog.Builder(this)
                .setTitle("Báo cáo ảnh")
                .setItems(reasons, (dialog, which) -> submitReport(reasons[which]))
                .setNegativeButton("Hủy", null)
                .show();
    }

    /**
     * Người 5 thực hiện: lưu report vào collection reports để Admin xử lý trong ReportManagementActivity.
     */
    private void submitReport(String reason) {
        btnReportMoment.setEnabled(false);

        adminRepository.createReport(currentUserId, currentMomentId, reason, new AdminRepository.ActionCallback() {
            @Override
            public void onSuccess() {
                btnReportMoment.setEnabled(true);
                Toast.makeText(MomentDetailActivity.this, "Đã gửi báo cáo cho Admin", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(String errorMessage) {
                btnReportMoment.setEnabled(true);
                Toast.makeText(MomentDetailActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Tải danh sách reaction.
     *
     * Đã chỉnh:
     * - Không Toast lỗi kỹ thuật ra giao diện.
     * - Khi lỗi Firestore index/mạng, chỉ để danh sách reaction trống và ghi Logcat.
     */
    private void loadReactions() {
        reactionRepository.getReactionsForMoment(currentMomentId, new ReactionRepository.ReactionListCallback() {
            @Override
            public void onSuccess(List<Reaction> reactions) {
                reactionAdapter.updateData(reactions);

                boolean hasReacted = false;

                for (Reaction reaction : reactions) {
                    if (reaction.getUserId() != null && reaction.getUserId().equals(currentUserId)) {
                        selectedReaction = reaction.getEmoji() == null ? "" : reaction.getEmoji();
                        txtSelectedReaction.setText("Your reaction: " + selectedReaction);
                        hasReacted = true;
                        break;
                    }
                }

                if (!hasReacted) {
                    txtSelectedReaction.setText("Your reaction: none");
                }
            }

            @Override
            public void onError(Exception e) {
                Log.w(TAG, "Không tải được reactions, ẩn lỗi kỹ thuật khỏi UI.", e);
                reactionAdapter.updateData(new ArrayList<>());
                txtSelectedReaction.setText(selectedReaction.isEmpty()
                        ? "Your reaction: none"
                        : "Your reaction: " + selectedReaction);
            }
        });
    }

    private void setupReactionButton(int buttonId, String reactionEmoji) {
        View button = findViewById(buttonId);

        button.setOnClickListener(v -> {
            if (currentUserId.equals(currentSenderId)) {
                Toast.makeText(
                        this,
                        "Bạn không thể thả cảm xúc vào ảnh của chính mình",
                        Toast.LENGTH_SHORT
                ).show();
                return;
            }

            boolean isChangingReaction = !selectedReaction.isEmpty() && !selectedReaction.equals(reactionEmoji);

            reactionRepository.saveReaction(currentMomentId, currentUserId, reactionEmoji, new ReactionRepository.SaveReactionCallback() {
                @Override
                public void onSuccess() {
                    selectedReaction = reactionEmoji;
                    txtSelectedReaction.setText("Your reaction: " + selectedReaction);

                    Toast.makeText(
                            MomentDetailActivity.this,
                            isChangingReaction ? "Đã đổi cảm xúc" : "Đã thả cảm xúc",
                            Toast.LENGTH_SHORT
                    ).show();

                    loadReactions();
                    sendReactionNotification(reactionEmoji);
                }

                @Override
                public void onError(Exception e) {
                    Toast.makeText(MomentDetailActivity.this, "Lỗi khi thả cảm xúc: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private void sendReactionNotification(String reactionEmoji) {
        if (currentSenderId == null || currentSenderId.isEmpty()) return;
        if (currentUserId == null || currentUserId.equals(currentSenderId)) return;

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        String notificationId = db.collection("notifications")
                .document()
                .getId();

        NotificationModel notification = new NotificationModel();
        notification.setNotificationId(notificationId);
        notification.setUserId(currentSenderId);
        notification.setType("reaction");
        notification.setTitle("Reaction mới");
        notification.setMessage("Có người đã thả " + reactionEmoji + " vào khoảnh khắc của bạn.");
        notification.setRead(false);
        notification.setCreatedAt(Timestamp.now());

        db.collection("notifications")
                .document(notificationId)
                .set(notification);
    }

    private void loadMyReaction() {
        reactionRepository.getUserReaction(currentMomentId, currentUserId, new ReactionRepository.SaveUserReactionCallback() {
            @Override
            public void onSuccess(String emoji) {
                selectedReaction = emoji == null ? "" : emoji;

                if (selectedReaction.isEmpty()) {
                    txtSelectedReaction.setText("Your reaction: none");
                } else {
                    txtSelectedReaction.setText("Your reaction: " + selectedReaction);
                }
            }

            @Override
            public void onError(Exception e) {
                Log.w(TAG, "Không tải được reaction của user hiện tại, giữ giao diện ổn định.", e);
                selectedReaction = "";
                txtSelectedReaction.setText("Your reaction: none");
            }
        });
    }

    private void markMomentAsViewed() {
        if (currentMomentId == null || currentMomentId.isEmpty()) {
            return;
        }

        MomentRepository momentRepository = new MomentRepository(this);
        momentRepository.markMomentAsViewed(
                currentMomentId,
                currentUserId,
                new MomentRepository.ActionCallback() {
                    @Override
                    public void onSuccess() { }

                    @Override
                    public void onFailure(@NonNull String errorMessage) { }
                });
    }

    private String formatTime(long timeMillis) {
        SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
        return formatter.format(new Date(timeMillis));
    }
}

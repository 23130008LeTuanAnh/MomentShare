package com.example.momentshare.activity;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.momentshare.R;
import com.example.momentshare.model.Reaction;
import com.example.momentshare.repository.AdminRepository;
import com.example.momentshare.repository.ReactionRepository;
import com.google.firebase.auth.FirebaseAuth;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MomentDetailActivity extends AppCompatActivity {

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

    // Khai báo các thành phần mới cho Reaction
    private RecyclerView rvReactions;
    private ReactionAdapter reactionAdapter;
    private ReactionRepository reactionRepository;
    private AdminRepository adminRepository; // Người 5 thực hiện: dùng để tạo report cho Admin xử lý.

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

        // Lấy ID của bức ảnh hiện tại để truy vấn
        currentMomentId = getIntent().getStringExtra(EXTRA_MOMENT_ID);

        ImageButton btnBack = findViewById(R.id.btnBackDetail);
        imgDetailMoment = findViewById(R.id.imgDetailMoment);
        txtDetailSender = findViewById(R.id.txtDetailSender);
        txtDetailCaption = findViewById(R.id.txtDetailCaption);
        txtDetailTime = findViewById(R.id.txtDetailTime);
        txtSelectedReaction = findViewById(R.id.txtSelectedReaction);
        btnReportMoment = findViewById(R.id.btnReportMoment); // Người 5 thực hiện
        rvReactions = findViewById(R.id.rvReactions);

        btnBack.setOnClickListener(v -> finish());
        showMomentData();

        // Cấu hình RecyclerView cho danh sách Reaction
        rvReactions.setLayoutManager(new LinearLayoutManager(this));
        reactionAdapter = new ReactionAdapter(new ArrayList<>());
        rvReactions.setAdapter(reactionAdapter);

        // Khởi tạo Repository và tải danh sách cảm xúc
        reactionRepository = new ReactionRepository();

        // Người 5 thực hiện: AdminRepository tạo report khi user bấm Báo cáo ảnh.
        adminRepository = new AdminRepository();
        if (currentMomentId != null && !currentMomentId.isEmpty()) {
            loadReactions();
        }

        setupReportButton();

        setupReactionButton(R.id.btnLove, "\u2764");
        setupReactionButton(R.id.btnHaha, "\uD83D\uDE02");
        setupReactionButton(R.id.btnWow, "\uD83D\uDE2E");
        setupReactionButton(R.id.btnSad, "\uD83D\uDE22");
        setupReactionButton(R.id.btnLike, "\uD83D\uDC4D");
    }

    private void showMomentData() {
        String senderId = getIntent().getStringExtra(EXTRA_SENDER_ID);
        currentSenderId = senderId == null ? "" : senderId; // Người 5 thực hiện: giữ lại senderId để kiểm tra quyền báo cáo.
        String imageUrl = getIntent().getStringExtra(EXTRA_IMAGE_URL);
        String caption = getIntent().getStringExtra(EXTRA_CAPTION);
        long createdAt = getIntent().getLongExtra(EXTRA_CREATED_AT, 0L);

        txtDetailSender.setText(senderId == null || senderId.isEmpty() ? "Unknown sender" : senderId);
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

    // Hàm gọi Firebase để tải danh sách những người đã thả cảm xúc
    private void loadReactions() {
        reactionRepository.getReactionsForMoment(currentMomentId, new ReactionRepository.ReactionListCallback() {
            @Override
            public void onSuccess(List<Reaction> reactions) {
                // Cập nhật danh sách lên màn hình
                reactionAdapter.updateData(reactions);

                boolean hasReacted = false;
                for (Reaction r : reactions) {
                    if (r.getUserId().equals(currentUserId)) {
                        selectedReaction = r.getEmoji();
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
                Toast.makeText(MomentDetailActivity.this, "Lỗi tải cảm xúc: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupReactionButton(int buttonId, String reactionEmoji) {
        View button = findViewById(buttonId);
        button.setOnClickListener(v -> {
            if (currentMomentId == null || currentMomentId.isEmpty()) return;

            boolean isChangingReaction = !selectedReaction.isEmpty() && !selectedReaction.equals(reactionEmoji);

            reactionRepository.saveReaction(currentMomentId, currentUserId, reactionEmoji, new ReactionRepository.SaveReactionCallback() {
                @Override
                public void onSuccess() {
                    selectedReaction = reactionEmoji;
                    txtSelectedReaction.setText("Your reaction: " + selectedReaction);
                    Toast.makeText(MomentDetailActivity.this, isChangingReaction ? "Đã đổi cảm xúc" : "Đã thả cảm xúc", Toast.LENGTH_SHORT).show();

                    loadReactions();
                }

                @Override
                public void onError(Exception e) {
                    Toast.makeText(MomentDetailActivity.this, "Lỗi khi thả cảm xúc: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private String formatTime(long timeMillis) {
        SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
        return formatter.format(new Date(timeMillis));
    }
}

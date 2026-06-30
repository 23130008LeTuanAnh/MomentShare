package com.example.momentshare.activity;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.momentshare.R;
import com.example.momentshare.model.Moment;
import com.example.momentshare.repository.MomentRepository;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Màn hình lịch sử khoảnh khắc.
 *
 * Đúng theo đề tài MomentShare:
 * - Tab Đã nhận: hiển thị ảnh bạn bè gửi cho tài khoản hiện tại.
 * - Tab Đã gửi: hiển thị ảnh tài khoản hiện tại đã gửi cho bạn bè.
 * - Có tìm kiếm theo caption/momentId/senderId.
 * - Có lọc thời gian: tất cả, hôm nay, 7 ngày, 30 ngày.
 */
public class HistoryActivity extends AppCompatActivity implements HistoryAdapter.OnItemClickListener {

    private static final int TAB_RECEIVED = 0;
    private static final int TAB_SENT = 1;

    private ImageButton btnBack;
    private Button btnTabReceived;
    private Button btnTabSent;
    private EditText edtHistorySearch;
    private Spinner spinnerHistoryTime;
    private TextView txtHistoryHint;
    private TextView txtHistoryCount;
    private TextView txtEmptyHistory;
    private RecyclerView rvHistory;

    private HistoryAdapter historyAdapter;
    private MomentRepository momentRepository;
    private String currentUserId;

    private final List<Moment> sentMoments = new ArrayList<>();
    private final List<Moment> receivedMoments = new ArrayList<>();
    private final List<Moment> displayMoments = new ArrayList<>();

    private int selectedTab = TAB_RECEIVED;
    private int selectedTimeFilter = 0;
    private boolean sentLoaded = false;
    private boolean receivedLoaded = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            Toast.makeText(this, "Vui lòng đăng nhập lại", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        setContentView(R.layout.activity_history);

        momentRepository = new MomentRepository(this);

        initViews();
        setupRecyclerView();
        setupEvents();
        loadHistory();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Khi quay lại từ MomentDetail hoặc sau khi gửi ảnh, tải lại để dữ liệu mới nhất.
        if (currentUserId != null && momentRepository != null) {
            loadHistory();
        }
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBackHistory);
        btnTabReceived = findViewById(R.id.btnTabReceived);
        btnTabSent = findViewById(R.id.btnTabSent);
        edtHistorySearch = findViewById(R.id.edtHistorySearch);
        spinnerHistoryTime = findViewById(R.id.spinnerHistoryTime);
        txtHistoryHint = findViewById(R.id.txtHistoryHint);
        txtHistoryCount = findViewById(R.id.txtHistoryCount);
        txtEmptyHistory = findViewById(R.id.txtEmptyHistory);
        rvHistory = findViewById(R.id.rvHistory);

        ArrayAdapter<String> timeAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                new String[]{"Tất cả thời gian", "Hôm nay", "7 ngày gần đây", "30 ngày gần đây"}
        );
        timeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerHistoryTime.setAdapter(timeAdapter);
    }

    private void setupRecyclerView() {
        historyAdapter = new HistoryAdapter(displayMoments, this);
        rvHistory.setLayoutManager(new GridLayoutManager(this, 2));
        rvHistory.setHasFixedSize(true);
        rvHistory.setAdapter(historyAdapter);
    }

    private void setupEvents() {
        btnBack.setOnClickListener(v -> finish());

        btnTabReceived.setOnClickListener(v -> {
            if (selectedTab != TAB_RECEIVED) {
                selectedTab = TAB_RECEIVED;
                applyFilters();
            }
        });

        btnTabSent.setOnClickListener(v -> {
            if (selectedTab != TAB_SENT) {
                selectedTab = TAB_SENT;
                applyFilters();
            }
        });

        edtHistorySearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                applyFilters();
            }

            @Override
            public void afterTextChanged(Editable s) { }
        });

        spinnerHistoryTime.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedTimeFilter = position;
                applyFilters();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) { }
        });
    }

    private void loadHistory() {
        sentLoaded = false;
        receivedLoaded = false;
        txtEmptyHistory.setVisibility(View.GONE);
        txtHistoryCount.setText("Đang tải lịch sử...");

        momentRepository.getSentHistory(currentUserId, new MomentRepository.MomentListCallback() {
            @Override
            public void onSuccess(List<Moment> moments) {
                sentMoments.clear();
                if (moments != null) {
                    sentMoments.addAll(moments);
                }
                sortByNewest(sentMoments);
                sentLoaded = true;
                applyFilters();
            }

            @Override
            public void onError(Exception exception) {
                sentLoaded = true;
                Toast.makeText(
                        HistoryActivity.this,
                        "Lỗi tải ảnh đã gửi: " + buildError(exception),
                        Toast.LENGTH_SHORT
                ).show();
                applyFilters();
            }
        });

        momentRepository.getReceivedHistory(currentUserId, new MomentRepository.MomentListCallback() {
            @Override
            public void onSuccess(List<Moment> moments) {
                receivedMoments.clear();
                if (moments != null) {
                    receivedMoments.addAll(moments);
                }
                sortByNewest(receivedMoments);
                receivedLoaded = true;
                applyFilters();
            }

            @Override
            public void onError(Exception exception) {
                receivedLoaded = true;
                Toast.makeText(
                        HistoryActivity.this,
                        "Lỗi tải ảnh đã nhận: " + buildError(exception),
                        Toast.LENGTH_SHORT
                ).show();
                applyFilters();
            }
        });
    }

    private void applyFilters() {
        updateTabUi();

        if (!sentLoaded || !receivedLoaded) {
            return;
        }

        String keyword = edtHistorySearch.getText() == null
                ? ""
                : edtHistorySearch.getText().toString().trim().toLowerCase(Locale.ROOT);

        List<Moment> source = selectedTab == TAB_RECEIVED ? receivedMoments : sentMoments;
        displayMoments.clear();

        for (Moment moment : source) {
            if (moment == null) continue;
            if (!matchesTimeFilter(moment)) continue;
            if (!matchesKeyword(moment, keyword)) continue;
            displayMoments.add(moment);
        }

        sortByNewest(displayMoments);
        historyAdapter.notifyDataSetChanged();
        updateEmptyState();
    }

    private boolean matchesKeyword(Moment moment, String keyword) {
        if (keyword.isEmpty()) return true;

        String caption = safeText(moment.getCaption()).toLowerCase(Locale.ROOT);
        String senderId = safeText(moment.getSenderId()).toLowerCase(Locale.ROOT);
        String momentId = safeText(moment.getMomentId()).toLowerCase(Locale.ROOT);

        return caption.contains(keyword)
                || senderId.contains(keyword)
                || momentId.contains(keyword);
    }

    private boolean matchesTimeFilter(Moment moment) {
        if (selectedTimeFilter == 0) return true;

        Timestamp createdAt = moment.getCreatedAt();
        if (createdAt == null) return false;

        long createdMillis = createdAt.toDate().getTime();
        long nowMillis = System.currentTimeMillis();
        long oneDayMillis = 24L * 60L * 60L * 1000L;

        if (createdMillis > nowMillis) {
            return false;
        }

        if (selectedTimeFilter == 1) {
            return createdMillis >= startOfTodayMillis();
        }

        if (selectedTimeFilter == 2) {
            return nowMillis - createdMillis <= 7L * oneDayMillis;
        }

        if (selectedTimeFilter == 3) {
            return nowMillis - createdMillis <= 30L * oneDayMillis;
        }

        return true;
    }

    private long startOfTodayMillis() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }

    private void updateTabUi() {
        boolean receivedSelected = selectedTab == TAB_RECEIVED;

        btnTabReceived.setText("Đã nhận (" + receivedMoments.size() + ")");
        btnTabSent.setText("Đã gửi (" + sentMoments.size() + ")");

        tintTab(btnTabReceived, receivedSelected);
        tintTab(btnTabSent, !receivedSelected);

        txtHistoryHint.setText(receivedSelected
                ? "Các khoảnh khắc bạn bè đã gửi cho bạn"
                : "Các khoảnh khắc bạn đã gửi cho bạn bè");
    }

    private void tintTab(Button button, boolean selected) {
        int backgroundColor = selected ? Color.parseColor("#6F50B5") : Color.parseColor("#E5E7EB");
        int textColor = selected ? Color.WHITE : Color.parseColor("#374151");
        button.setBackgroundTintList(ColorStateList.valueOf(backgroundColor));
        button.setTextColor(textColor);
    }

    private void updateEmptyState() {
        int count = displayMoments.size();
        txtHistoryCount.setText(count + " khoảnh khắc phù hợp");

        if (count == 0) {
            txtEmptyHistory.setVisibility(View.VISIBLE);
            txtEmptyHistory.setText(selectedTab == TAB_RECEIVED
                    ? "Chưa có khoảnh khắc đã nhận phù hợp"
                    : "Chưa có khoảnh khắc đã gửi phù hợp");
        } else {
            txtEmptyHistory.setVisibility(View.GONE);
        }
    }

    private void sortByNewest(List<Moment> moments) {
        Collections.sort(moments, (m1, m2) -> {
            if (m1.getCreatedAt() == null && m2.getCreatedAt() == null) return 0;
            if (m1.getCreatedAt() == null) return 1;
            if (m2.getCreatedAt() == null) return -1;
            return m2.getCreatedAt().compareTo(m1.getCreatedAt());
        });
    }

    @Override
    public void onItemClick(Moment moment) {
        if (moment == null) return;

        Intent intent = new Intent(HistoryActivity.this, MomentDetailActivity.class);
        HomeFeedActivity.putMomentExtras(intent, moment);
        startActivity(intent);
    }

    private String safeText(String value) {
        return value == null ? "" : value.trim();
    }

    private String buildError(Exception exception) {
        if (exception == null || exception.getMessage() == null || exception.getMessage().trim().isEmpty()) {
            return "Không rõ lỗi";
        }
        return exception.getMessage();
    }
}

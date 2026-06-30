package com.example.momentshare.activity;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
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

import java.util.ArrayList;
import java.util.List;

public class SearchFriendActivity extends AppCompatActivity {

    private EditText edtSearch;
    private Button btnSearch;
    private ImageButton btnBack;
    private ProgressBar progressBar;
    private TextView txtNoResult;
    private TextView txtSearchHint;
    private TextView txtSearchCount;
    private RecyclerView rvResult;

    private FriendAdapter adapter;
    private final List<User> resultList = new ArrayList<>();
    private FriendRepository friendRepository;

    private final Handler searchHandler = new Handler(Looper.getMainLooper());
    private Runnable pendingSearchRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_friend);

        friendRepository = new FriendRepository();
        initViews();
        setupEvents();
        showInitialState();
    }

    private void initViews() {
        edtSearch = findViewById(R.id.edtSearchFriend);
        btnSearch = findViewById(R.id.btnSearchFriend);
        btnBack = findViewById(R.id.btnBackSearch);
        progressBar = findViewById(R.id.progressBarSearch);
        txtNoResult = findViewById(R.id.txtNoResult);
        txtSearchHint = findViewById(R.id.txtSearchHint);
        txtSearchCount = findViewById(R.id.txtSearchCount);
        rvResult = findViewById(R.id.rvSearchFriendResult);

        rvResult.setLayoutManager(new LinearLayoutManager(this));
        adapter = new FriendAdapter(this, resultList, true);
        rvResult.setAdapter(adapter);
    }

    private void setupEvents() {
        btnBack.setOnClickListener(v -> finish());
        btnSearch.setOnClickListener(v -> handleSearch(false));

        edtSearch.setSingleLine(true);
        edtSearch.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
        edtSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                handleSearch(false);
                return true;
            }
            return false;
        });

        edtSearch.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                scheduleAutoSearch(s == null ? "" : s.toString());
            }

            @Override
            public void afterTextChanged(android.text.Editable s) { }
        });
    }

    private void scheduleAutoSearch(String keyword) {
        if (pendingSearchRunnable != null) {
            searchHandler.removeCallbacks(pendingSearchRunnable);
        }

        String query = keyword == null ? "" : keyword.trim();
        if (query.length() < 2) {
            resultList.clear();
            adapter.notifyDataSetChanged();
            showInitialState();
            return;
        }

        pendingSearchRunnable = () -> handleSearch(true);
        searchHandler.postDelayed(pendingSearchRunnable, 500);
    }

    private void handleSearch(boolean isAutoSearch) {
        String query = edtSearch.getText().toString().trim();
        if (query.isEmpty()) {
            if (!isAutoSearch) {
                Toast.makeText(this, "Vui lòng nhập tên, username hoặc email", Toast.LENGTH_SHORT).show();
            }
            showInitialState();
            return;
        }

        setLoading(true);
        friendRepository.searchUsers(query, new FriendRepository.UserListCallback() {
            @Override
            public void onSuccess(List<User> users) {
                setLoading(false);
                resultList.clear();
                resultList.addAll(users);
                adapter.notifyDataSetChanged();
                showSearchResultState(query, users.size());
            }

            @Override
            public void onFailure(String errorMessage) {
                setLoading(false);
                Toast.makeText(SearchFriendActivity.this, "Lỗi: " + errorMessage, Toast.LENGTH_SHORT).show();
                showSearchResultState(query, 0);
            }
        });
    }

    private void setLoading(boolean isLoading) {
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        btnSearch.setEnabled(!isLoading);
        rvResult.setVisibility(isLoading ? View.GONE : View.VISIBLE);
        txtNoResult.setVisibility(View.GONE);
        txtSearchHint.setVisibility(isLoading ? View.GONE : txtSearchHint.getVisibility());
    }

    private void showInitialState() {
        progressBar.setVisibility(View.GONE);
        rvResult.setVisibility(View.GONE);
        txtNoResult.setVisibility(View.GONE);
        txtSearchCount.setVisibility(View.GONE);
        txtSearchHint.setVisibility(View.VISIBLE);
        txtSearchHint.setText("Nhập tên, username hoặc email để tìm bạn bè. Tài khoản bị khóa và tài khoản admin sẽ không hiển thị.");
    }

    private void showSearchResultState(String query, int count) {
        txtSearchHint.setVisibility(View.GONE);
        txtSearchCount.setVisibility(View.VISIBLE);
        txtSearchCount.setText("Tìm thấy " + count + " kết quả cho: " + query);
        rvResult.setVisibility(count > 0 ? View.VISIBLE : View.GONE);

        if (count == 0) {
            txtNoResult.setText("Không tìm thấy tài khoản phù hợp");
            txtNoResult.setVisibility(View.VISIBLE);
        } else {
            txtNoResult.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onDestroy() {
        if (pendingSearchRunnable != null) {
            searchHandler.removeCallbacks(pendingSearchRunnable);
        }
        super.onDestroy();
    }
}

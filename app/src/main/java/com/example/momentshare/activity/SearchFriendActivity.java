package com.example.momentshare.activity;

import android.os.Bundle;
import android.view.View;
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
    private RecyclerView rvResult;

    private FriendAdapter adapter;
    private final List<User> resultList = new ArrayList<>();
    private FriendRepository friendRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_friend);

        friendRepository = new FriendRepository();
        initViews();
        setupEvents();
    }

    private void initViews() {
        edtSearch = findViewById(R.id.edtSearchFriend);
        btnSearch = findViewById(R.id.btnSearchFriend);
        btnBack = findViewById(R.id.btnBackSearch);
        progressBar = findViewById(R.id.progressBarSearch);
        txtNoResult = findViewById(R.id.txtNoResult);
        rvResult = findViewById(R.id.rvSearchFriendResult);

        rvResult.setLayoutManager(new LinearLayoutManager(this));
        adapter = new FriendAdapter(this, resultList, true);
        rvResult.setAdapter(adapter);
    }

    private void setupEvents() {
        btnBack.setOnClickListener(v -> finish());
        btnSearch.setOnClickListener(v -> handleSearch());
    }

    private void handleSearch() {
        String query = edtSearch.getText().toString().trim();
        if (query.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập từ khóa tìm kiếm", Toast.LENGTH_SHORT).show();
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
                
                if (users.isEmpty()) {
                    txtNoResult.setVisibility(View.VISIBLE);
                } else {
                    txtNoResult.setVisibility(View.GONE);
                }
            }

            @Override
            public void onFailure(String errorMessage) {
                setLoading(false);
                Toast.makeText(SearchFriendActivity.this, "Lỗi: " + errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setLoading(boolean isLoading) {
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        btnSearch.setEnabled(!isLoading);
        rvResult.setVisibility(isLoading ? View.GONE : View.VISIBLE);
        txtNoResult.setVisibility(View.GONE);
    }
}

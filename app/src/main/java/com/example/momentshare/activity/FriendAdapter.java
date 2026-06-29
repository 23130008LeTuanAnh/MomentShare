package com.example.momentshare.activity;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.momentshare.R;
import com.example.momentshare.model.User;
import com.example.momentshare.repository.FriendRepository;
import com.google.firebase.auth.FirebaseAuth;

import java.util.List;

public class FriendAdapter extends RecyclerView.Adapter<FriendAdapter.FriendViewHolder> {

    private final Context context;
    private final List<User> userList;
    private final FriendRepository friendRepository;
    private final String currentUserId;
    private final boolean isSearchMode;

    public FriendAdapter(Context context, List<User> userList, boolean isSearchMode) {
        this.context = context;
        this.userList = userList;
        this.isSearchMode = isSearchMode;
        this.friendRepository = new FriendRepository();
        this.currentUserId = FirebaseAuth.getInstance().getCurrentUser() != null ?
                FirebaseAuth.getInstance().getCurrentUser().getUid() : "";
    }

    @NonNull
    @Override
    public FriendViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_friend, parent, false);
        return new FriendViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FriendViewHolder holder, int position) {
        User user = userList.get(position);
        holder.txtName.setText(user.getFullName());
        holder.txtUsername.setText("@" + user.getUsername());

        if (user.getAvatarUrl() != null && !user.getAvatarUrl().isEmpty()) {
            Glide.with(context).load(user.getAvatarUrl()).circleCrop().into(holder.imgAvatar);
        } else {
            holder.imgAvatar.setImageResource(R.mipmap.ic_launcher);
        }

        if (isSearchMode) {
            if (user.getUserId().equals(currentUserId)) {
                holder.btnAction.setVisibility(View.GONE);
            } else {
                holder.btnAction.setVisibility(View.VISIBLE);
                updateButtonState(holder.btnAction, user.getUserId());
            }
        } else {
            // Danh sách bạn bè (FriendListActivity)
            holder.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(context, FriendProfileActivity.class);
                intent.putExtra(FriendProfileActivity.EXTRA_FRIEND_ID, user.getUserId());
                context.startActivity(intent);
            });

            holder.btnAction.setText("Hủy kết bạn");
            holder.btnAction.setBackgroundColor(Color.RED);
            holder.btnAction.setTextColor(Color.WHITE);
            holder.btnAction.setOnClickListener(v -> {
                int curPos = holder.getAdapterPosition();
                if (curPos == RecyclerView.NO_POSITION) return;
                
                friendRepository.unfriend(currentUserId, user.getUserId(), new FriendRepository.ActionCallback() {
                    @Override
                    public void onSuccess() {
                        userList.remove(curPos);
                        notifyItemRemoved(curPos);
                        notifyItemRangeChanged(curPos, userList.size());
                    }

                    @Override
                    public void onFailure(String errorMessage) {
                        // Handle failure
                    }
                });
            });
        }
    }

    private void updateButtonState(Button btn, String otherId) {
        friendRepository.checkFriendshipStatus(currentUserId, otherId, new FriendRepository.StatusCallback() {
            @Override
            public void onSuccess(String status) {
                switch (status) {
                    case "accepted":
                        btn.setText("Bạn bè");
                        btn.setEnabled(false);
                        break;
                    case "pending":
                        btn.setText("Đã gửi");
                        btn.setEnabled(false);
                        break;
                    case "received_pending":
                        btn.setText("Phản hồi");
                        btn.setEnabled(true);
                        btn.setOnClickListener(v -> {
                            Intent intent = new Intent(context, FriendRequestActivity.class);
                            context.startActivity(intent);
                        });
                        break;
                    case "none":
                        btn.setText("Kết bạn");
                        btn.setEnabled(true);
                        btn.setOnClickListener(v -> {
                            btn.setEnabled(false);
                            friendRepository.sendFriendRequest(currentUserId, otherId, new FriendRepository.ActionCallback() {
                                @Override
                                public void onSuccess() {
                                    btn.setText("Đã gửi");
                                }

                                @Override
                                public void onFailure(String errorMessage) {
                                    btn.setEnabled(true);
                                }
                            });
                        });
                        break;
                }
            }

            @Override
            public void onFailure(String errorMessage) {
            }
        });
    }

    @Override
    public int getItemCount() {
        return userList.size();
    }

    public static class FriendViewHolder extends RecyclerView.ViewHolder {
        ImageView imgAvatar;
        TextView txtName, txtUsername;
        Button btnAction;

        public FriendViewHolder(@NonNull View itemView) {
            super(itemView);
            imgAvatar = itemView.findViewById(R.id.imgFriendAvatar);
            txtName = itemView.findViewById(R.id.txtFriendName);
            txtUsername = itemView.findViewById(R.id.txtFriendUsername);
            btnAction = itemView.findViewById(R.id.btnFriendAction);
        }
    }
}

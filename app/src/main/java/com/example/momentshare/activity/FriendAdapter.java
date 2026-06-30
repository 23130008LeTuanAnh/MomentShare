package com.example.momentshare.activity;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.momentshare.R;
import com.example.momentshare.model.User;
import com.example.momentshare.repository.FriendRepository;
import com.google.firebase.auth.FirebaseAuth;

import java.util.List;

/**
 * Adapter hiển thị user ở màn hình tìm kiếm bạn bè và danh sách bạn bè.
 */
public class FriendAdapter extends RecyclerView.Adapter<FriendAdapter.FriendViewHolder> {

    public interface OnFriendRemovedListener {
        void onFriendRemoved(User removedUser);
    }

    private final Context context;
    private final List<User> userList;
    private final FriendRepository friendRepository;
    private final String currentUserId;
    private final boolean isSearchMode;
    private OnFriendRemovedListener onFriendRemovedListener;

    public FriendAdapter(Context context, List<User> userList, boolean isSearchMode) {
        this.context = context;
        this.userList = userList;
        this.isSearchMode = isSearchMode;
        this.friendRepository = new FriendRepository();
        this.currentUserId = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : "";
    }

    public void setOnFriendRemovedListener(OnFriendRemovedListener listener) {
        this.onFriendRemovedListener = listener;
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
        String userId = user == null ? "" : safeText(user.getUserId(), "");
        holder.btnAction.setTag(userId);
        holder.btnMessage.setTag(userId);

        holder.txtName.setText(safeText(user == null ? null : user.getFullName(), "Người dùng MomentShare"));
        holder.txtUsername.setText("@" + safeText(user == null ? null : user.getUsername(), "unknown"));
        holder.txtEmail.setText(safeText(user == null ? null : user.getEmail(), "Chưa có email"));

        if (user != null && user.getAvatarUrl() != null && !user.getAvatarUrl().trim().isEmpty()) {
            Glide.with(context)
                    .load(user.getAvatarUrl())
                    .circleCrop()
                    .placeholder(R.mipmap.ic_launcher)
                    .error(R.mipmap.ic_launcher)
                    .into(holder.imgAvatar);
        } else {
            holder.imgAvatar.setImageResource(R.mipmap.ic_launcher);
        }

        holder.itemView.setOnClickListener(null);
        holder.btnAction.setOnClickListener(null);
        holder.btnMessage.setOnClickListener(null);
        holder.btnAction.setVisibility(View.VISIBLE);
        holder.btnMessage.setVisibility(isSearchMode ? View.GONE : View.VISIBLE);
        holder.btnAction.setEnabled(true);
        holder.btnMessage.setEnabled(true);

        if (isSearchMode) {
            setupSearchAction(holder, user);
        } else {
            setupFriendListAction(holder, user);
        }
    }

    private void setupSearchAction(@NonNull FriendViewHolder holder, User user) {
        String otherId = user == null ? "" : safeText(user.getUserId(), "");
        if (otherId.isEmpty() || otherId.equals(currentUserId)) {
            holder.btnAction.setVisibility(View.GONE);
            return;
        }

        holder.btnAction.setText("Đang kiểm tra...");
        holder.btnAction.setEnabled(false);
        holder.btnAction.setBackgroundColor(Color.parseColor("#9E9E9E"));
        holder.btnAction.setTextColor(Color.WHITE);

        friendRepository.checkFriendshipStatus(currentUserId, otherId, new FriendRepository.StatusCallback() {
            @Override
            public void onSuccess(String status) {
                if (!otherId.equals(holder.btnAction.getTag())) return;
                updateSearchButtonState(holder.btnAction, otherId, status);
            }

            @Override
            public void onFailure(String errorMessage) {
                if (!otherId.equals(holder.btnAction.getTag())) return;
                updateSearchButtonState(holder.btnAction, otherId, "none");
            }
        });
    }

    private void updateSearchButtonState(Button btn, String otherId, String status) {
        switch (status) {
            case "accepted":
                btn.setText("Bạn bè");
                btn.setEnabled(false);
                btn.setBackgroundColor(Color.parseColor("#9E9E9E"));
                btn.setTextColor(Color.WHITE);
                break;

            case "pending":
                btn.setText("Đã gửi");
                btn.setEnabled(false);
                btn.setBackgroundColor(Color.parseColor("#9E9E9E"));
                btn.setTextColor(Color.WHITE);
                break;

            case "received_pending":
                btn.setText("Phản hồi");
                btn.setEnabled(true);
                btn.setBackgroundColor(Color.parseColor("#FF9800"));
                btn.setTextColor(Color.WHITE);
                btn.setOnClickListener(v -> {
                    Intent intent = new Intent(context, FriendRequestActivity.class);
                    intent.putExtra(FriendRequestActivity.EXTRA_FILTER_SENDER_ID, otherId);
                    context.startActivity(intent);
                });
                break;

            case "self":
                btn.setVisibility(View.GONE);
                break;

            case "none":
            default:
                btn.setText("Kết bạn");
                btn.setEnabled(true);
                btn.setBackgroundColor(Color.parseColor("#7353C4"));
                btn.setTextColor(Color.WHITE);
                btn.setOnClickListener(v -> sendRequest(btn, otherId));
                break;
        }
    }

    private void sendRequest(Button btn, String otherId) {
        btn.setEnabled(false);
        btn.setText("Đang gửi...");

        friendRepository.sendFriendRequest(currentUserId, otherId, new FriendRepository.ActionCallback() {
            @Override
            public void onSuccess() {
                btn.setText("Đã gửi");
                btn.setEnabled(false);
                btn.setBackgroundColor(Color.parseColor("#9E9E9E"));
                btn.setTextColor(Color.WHITE);
                Toast.makeText(context, "Đã gửi lời mời kết bạn", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(String errorMessage) {
                btn.setEnabled(true);
                btn.setText("Kết bạn");
                btn.setBackgroundColor(Color.parseColor("#7353C4"));
                btn.setTextColor(Color.WHITE);
                Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupFriendListAction(@NonNull FriendViewHolder holder, User user) {
        holder.itemView.setOnClickListener(v -> {
            if (user == null || user.getUserId() == null) return;
            Intent intent = new Intent(context, FriendProfileActivity.class);
            intent.putExtra(FriendProfileActivity.EXTRA_FRIEND_ID, user.getUserId());
            context.startActivity(intent);
        });

        holder.btnMessage.setText("Nhắn tin");
        holder.btnMessage.setEnabled(true);
        holder.btnMessage.setBackgroundColor(Color.parseColor("#7353C4"));
        holder.btnMessage.setTextColor(Color.WHITE);
        holder.btnMessage.setOnClickListener(v -> openChat(user));

        holder.btnAction.setText("Hủy kết bạn");
        holder.btnAction.setEnabled(true);
        holder.btnAction.setBackgroundColor(Color.parseColor("#E53935"));
        holder.btnAction.setTextColor(Color.WHITE);
        holder.btnAction.setOnClickListener(v -> showUnfriendConfirmDialog(holder, user));
    }

    private void openChat(User user) {
        if (user == null || user.getUserId() == null || user.getUserId().trim().isEmpty()) {
            Toast.makeText(context, "Không xác định được bạn bè", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(context, ChatActivity.class);
        intent.putExtra(ChatActivity.EXTRA_FRIEND_ID, user.getUserId());
        intent.putExtra(ChatActivity.EXTRA_FRIEND_NAME, safeText(user.getFullName(), "Bạn bè"));
        intent.putExtra(ChatActivity.EXTRA_FRIEND_AVATAR, user.getAvatarUrl());
        context.startActivity(intent);
    }

    private void showUnfriendConfirmDialog(@NonNull FriendViewHolder holder, User user) {
        if (user == null || user.getUserId() == null || user.getUserId().trim().isEmpty()) {
            Toast.makeText(context, "Không xác định được bạn bè cần hủy", Toast.LENGTH_SHORT).show();
            return;
        }

        String displayName = safeText(user.getFullName(), safeText(user.getUsername(), "người này"));
        new AlertDialog.Builder(context)
                .setTitle("Hủy kết bạn")
                .setMessage("Bạn có chắc muốn hủy kết bạn với " + displayName + " không?")
                .setNegativeButton("Đóng", null)
                .setPositiveButton("Hủy kết bạn", (dialog, which) -> handleUnfriend(holder, user))
                .show();
    }

    private void handleUnfriend(@NonNull FriendViewHolder holder, User user) {
        int curPos = holder.getAdapterPosition();
        if (curPos == RecyclerView.NO_POSITION) return;

        holder.btnAction.setEnabled(false);
        holder.btnAction.setText("Đang hủy...");

        friendRepository.unfriend(currentUserId, user.getUserId(), new FriendRepository.ActionCallback() {
            @Override
            public void onSuccess() {
                if (onFriendRemovedListener != null) {
                    onFriendRemovedListener.onFriendRemoved(user);
                } else {
                    int removePos = holder.getAdapterPosition();
                    if (removePos != RecyclerView.NO_POSITION && removePos < userList.size()) {
                        userList.remove(removePos);
                        notifyItemRemoved(removePos);
                        notifyItemRangeChanged(removePos, userList.size());
                    }
                }
                Toast.makeText(context, "Đã hủy kết bạn", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(String errorMessage) {
                holder.btnAction.setEnabled(true);
                holder.btnAction.setText("Hủy kết bạn");
                Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public int getItemCount() {
        return userList == null ? 0 : userList.size();
    }

    private String safeText(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value.trim();
    }

    public static class FriendViewHolder extends RecyclerView.ViewHolder {
        ImageView imgAvatar;
        TextView txtName;
        TextView txtUsername;
        TextView txtEmail;
        Button btnAction;
        Button btnMessage;

        public FriendViewHolder(@NonNull View itemView) {
            super(itemView);
            imgAvatar = itemView.findViewById(R.id.imgFriendAvatar);
            txtName = itemView.findViewById(R.id.txtFriendName);
            txtUsername = itemView.findViewById(R.id.txtFriendUsername);
            txtEmail = itemView.findViewById(R.id.txtFriendEmail);
            btnAction = itemView.findViewById(R.id.btnFriendAction);
            btnMessage = itemView.findViewById(R.id.btnMessageFriend);
        }
    }
}

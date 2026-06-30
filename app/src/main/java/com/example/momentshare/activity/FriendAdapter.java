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
 * FriendAdapter hiển thị user trong màn hình tìm bạn và danh sách bạn bè.
 *
 * Đã chỉnh:
 * - Reset listener/nút trong onBindViewHolder để RecyclerView không giữ trạng thái cũ.
 * - Khi trạng thái là received_pending, nút Phản hồi mở đúng FriendRequestActivity.
 * - Truyền senderId vào FriendRequestActivity để màn hình lời mời ưu tiên hiển thị đúng người vừa tìm kiếm.
 */
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
        this.currentUserId = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : "";
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

        holder.itemView.setOnClickListener(null);
        holder.btnAction.setOnClickListener(null);
        holder.btnAction.setEnabled(false);
        holder.btnAction.setVisibility(View.VISIBLE);
        holder.btnAction.setText("Đang tải...");
        holder.btnAction.setTextColor(Color.WHITE);
        holder.btnAction.setBackgroundColor(Color.parseColor("#6F4DBF"));
        holder.btnAction.setTag(user.getUserId());

        holder.txtName.setText(safeText(user.getFullName(), "Người dùng MomentShare"));
        holder.txtUsername.setText("@" + safeText(user.getUsername(), "unknown"));

        if (user.getAvatarUrl() != null && !user.getAvatarUrl().trim().isEmpty()) {
            Glide.with(context)
                    .load(user.getAvatarUrl())
                    .circleCrop()
                    .placeholder(R.mipmap.ic_launcher)
                    .error(R.mipmap.ic_launcher)
                    .into(holder.imgAvatar);
        } else {
            holder.imgAvatar.setImageResource(R.mipmap.ic_launcher);
        }

        if (isSearchMode) {
            bindSearchMode(holder, user);
        } else {
            bindFriendListMode(holder, user);
        }
    }

    private void bindSearchMode(@NonNull FriendViewHolder holder, @NonNull User user) {
        holder.itemView.setOnClickListener(null);

        if (user.getUserId() == null || user.getUserId().equals(currentUserId)) {
            holder.btnAction.setVisibility(View.GONE);
            return;
        }

        holder.btnAction.setVisibility(View.VISIBLE);
        updateButtonState(holder.btnAction, user.getUserId());
    }

    private void bindFriendListMode(@NonNull FriendViewHolder holder, @NonNull User user) {
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, FriendProfileActivity.class);
            intent.putExtra(FriendProfileActivity.EXTRA_FRIEND_ID, user.getUserId());
            context.startActivity(intent);
        });

        holder.btnAction.setText("Hủy kết bạn");
        holder.btnAction.setEnabled(true);
        holder.btnAction.setBackgroundColor(Color.RED);
        holder.btnAction.setTextColor(Color.WHITE);

        holder.btnAction.setOnClickListener(v -> {
            int curPos = holder.getAdapterPosition();
            if (curPos == RecyclerView.NO_POSITION) return;

            User currentUser = userList.get(curPos);
            friendRepository.unfriend(currentUserId, currentUser.getUserId(), new FriendRepository.ActionCallback() {
                @Override
                public void onSuccess() {
                    userList.remove(curPos);
                    notifyItemRemoved(curPos);
                    notifyItemRangeChanged(curPos, userList.size());
                }

                @Override
                public void onFailure(String errorMessage) {
                    Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private void updateButtonState(Button btn, String otherId) {
        friendRepository.checkFriendshipStatus(currentUserId, otherId, new FriendRepository.StatusCallback() {
            @Override
            public void onSuccess(String status) {
                if (!otherId.equals(btn.getTag())) {
                    return;
                }

                switch (status) {
                    case "accepted":
                        btn.setText("Bạn bè");
                        btn.setEnabled(false);
                        btn.setBackgroundColor(Color.GRAY);
                        break;

                    case "pending":
                        btn.setText("Đã gửi");
                        btn.setEnabled(false);
                        btn.setBackgroundColor(Color.GRAY);
                        break;

                    case "received_pending":
                        btn.setText("Phản hồi");
                        btn.setEnabled(true);
                        btn.setBackgroundColor(Color.parseColor("#6F4DBF"));
                        btn.setOnClickListener(v -> {
                            Intent intent = new Intent(context, FriendRequestActivity.class);
                            intent.putExtra(FriendRequestActivity.EXTRA_TARGET_SENDER_ID, otherId);
                            context.startActivity(intent);
                        });
                        break;

                    case "none":
                    default:
                        btn.setText("Kết bạn");
                        btn.setEnabled(true);
                        btn.setBackgroundColor(Color.parseColor("#6F4DBF"));
                        btn.setOnClickListener(v -> {
                            btn.setEnabled(false);
                            friendRepository.sendFriendRequest(currentUserId, otherId, new FriendRepository.ActionCallback() {
                                @Override
                                public void onSuccess() {
                                    btn.setText("Đã gửi");
                                    btn.setBackgroundColor(Color.GRAY);
                                }

                                @Override
                                public void onFailure(String errorMessage) {
                                    btn.setEnabled(true);
                                    Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show();
                                }
                            });
                        });
                        break;
                }
            }

            @Override
            public void onFailure(String errorMessage) {
                if (!otherId.equals(btn.getTag())) {
                    return;
                }

                btn.setText("Lỗi");
                btn.setEnabled(false);
            }
        });
    }

    @Override
    public int getItemCount() {
        return userList.size();
    }

    private String safeText(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value.trim();
    }

    public static class FriendViewHolder extends RecyclerView.ViewHolder {
        ImageView imgAvatar;
        TextView txtName;
        TextView txtUsername;
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

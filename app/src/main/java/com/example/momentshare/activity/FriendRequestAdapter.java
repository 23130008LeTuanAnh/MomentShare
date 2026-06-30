package com.example.momentshare.activity;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.momentshare.R;
import com.example.momentshare.model.FriendRequest;
import com.example.momentshare.model.User;
import com.example.momentshare.repository.FriendRepository;
import com.example.momentshare.repository.UserRepository;

import java.util.List;

/**
 * FriendRequestAdapter hiển thị danh sách lời mời kết bạn.
 *
 * Đã chỉnh:
 * - Hiển thị placeholder "Đang tải..." trước khi lấy được thông tin người gửi.
 * - Nếu không lấy được user, vẫn hiện fallback thay vì để dòng trắng.
 * - Dùng currentRequest theo vị trí hiện tại để tránh sai item khi RecyclerView thay đổi dữ liệu.
 */
public class FriendRequestAdapter extends RecyclerView.Adapter<FriendRequestAdapter.RequestViewHolder> {

    private final Context context;
    private final List<FriendRequest> requestList;
    private final FriendRepository friendRepository;
    private final UserRepository userRepository;

    public FriendRequestAdapter(Context context, List<FriendRequest> requestList) {
        this.context = context;
        this.requestList = requestList;
        this.friendRepository = new FriendRepository();
        this.userRepository = new UserRepository();
    }

    @NonNull
    @Override
    public RequestViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_friend_request, parent, false);
        return new RequestViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RequestViewHolder holder, int position) {
        FriendRequest request = requestList.get(position);

        // Tránh giao diện trắng trong lúc đang tải user từ Firestore.
        holder.txtName.setText("Đang tải...");
        holder.txtUsername.setText("");
        holder.imgAvatar.setImageResource(R.mipmap.ic_launcher);

        userRepository.getUserById(request.getSenderId(), new UserRepository.UserCallback() {
            @Override
            public void onSuccess(User user) {
                String fullName = user == null ? "" : user.getFullName();
                String username = user == null ? "" : user.getUsername();

                if (fullName == null || fullName.trim().isEmpty()) {
                    fullName = "Người dùng MomentShare";
                }

                if (username == null || username.trim().isEmpty()) {
                    username = "unknown";
                }

                holder.txtName.setText(fullName);
                holder.txtUsername.setText("@" + username);

                String avatarUrl = user == null ? "" : user.getAvatarUrl();

                if (avatarUrl != null && !avatarUrl.trim().isEmpty()) {
                    Glide.with(context)
                            .load(avatarUrl)
                            .circleCrop()
                            .placeholder(R.mipmap.ic_launcher)
                            .error(R.mipmap.ic_launcher)
                            .into(holder.imgAvatar);
                } else {
                    holder.imgAvatar.setImageResource(R.mipmap.ic_launcher);
                }
            }

            @Override
            public void onFailure(String errorMessage) {
                holder.txtName.setText("Người dùng MomentShare");
                holder.txtUsername.setText("ID: " + safeText(request.getSenderId()));
                holder.imgAvatar.setImageResource(R.mipmap.ic_launcher);
            }
        });

        holder.btnAccept.setOnClickListener(v -> {
            int currentPos = holder.getAdapterPosition();
            if (currentPos == RecyclerView.NO_POSITION) return;

            FriendRequest currentRequest = requestList.get(currentPos);

            friendRepository.acceptFriendRequest(
                    currentRequest.getRequestId(),
                    currentRequest.getSenderId(),
                    currentRequest.getReceiverId(),
                    new FriendRepository.ActionCallback() {
                        @Override
                        public void onSuccess() {
                            requestList.remove(currentPos);
                            notifyItemRemoved(currentPos);
                            Toast.makeText(context, "Đã chấp nhận kết bạn", Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onFailure(String errorMessage) {
                            Toast.makeText(context, "Lỗi: " + errorMessage, Toast.LENGTH_SHORT).show();
                        }
                    }
            );
        });

        holder.btnReject.setOnClickListener(v -> {
            int currentPos = holder.getAdapterPosition();
            if (currentPos == RecyclerView.NO_POSITION) return;

            FriendRequest currentRequest = requestList.get(currentPos);

            friendRepository.rejectFriendRequest(
                    currentRequest.getRequestId(),
                    new FriendRepository.ActionCallback() {
                        @Override
                        public void onSuccess() {
                            requestList.remove(currentPos);
                            notifyItemRemoved(currentPos);
                            Toast.makeText(context, "Đã từ chối lời mời", Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onFailure(String errorMessage) {
                            Toast.makeText(context, "Lỗi: " + errorMessage, Toast.LENGTH_SHORT).show();
                        }
                    }
            );
        });
    }

    @Override
    public int getItemCount() {
        return requestList.size();
    }

    private String safeText(String value) {
        return value == null ? "" : value;
    }

    public static class RequestViewHolder extends RecyclerView.ViewHolder {
        ImageView imgAvatar;
        TextView txtName;
        TextView txtUsername;
        ImageButton btnAccept;
        ImageButton btnReject;

        public RequestViewHolder(@NonNull View itemView) {
            super(itemView);
            imgAvatar = itemView.findViewById(R.id.imgRequestAvatar);
            txtName = itemView.findViewById(R.id.txtRequestName);
            txtUsername = itemView.findViewById(R.id.txtRequestUsername);
            btnAccept = itemView.findViewById(R.id.btnAcceptRequest);
            btnReject = itemView.findViewById(R.id.btnRejectRequest);
        }
    }
}

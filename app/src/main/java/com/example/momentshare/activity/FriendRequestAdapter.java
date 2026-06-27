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

        userRepository.getUserById(request.getSenderId(), new UserRepository.UserCallback() {
            @Override
            public void onSuccess(User user) {
                holder.txtName.setText(user.getFullName());
                holder.txtUsername.setText("@" + user.getUsername());
                if (user.getAvatarUrl() != null && !user.getAvatarUrl().isEmpty()) {
                    Glide.with(context).load(user.getAvatarUrl()).circleCrop().into(holder.imgAvatar);
                } else {
                    holder.imgAvatar.setImageResource(R.mipmap.ic_launcher);
                }
            }

            @Override
            public void onFailure(String errorMessage) {
            }
        });

        holder.btnAccept.setOnClickListener(v -> {
            int currentPos = holder.getAdapterPosition();
            if (currentPos == RecyclerView.NO_POSITION) return;
            
            friendRepository.acceptFriendRequest(request.getRequestId(), request.getSenderId(), request.getReceiverId(), new FriendRepository.ActionCallback() {
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
            });
        });

        holder.btnReject.setOnClickListener(v -> {
            int currentPos = holder.getAdapterPosition();
            if (currentPos == RecyclerView.NO_POSITION) return;

            friendRepository.rejectFriendRequest(request.getRequestId(), new FriendRepository.ActionCallback() {
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
            });
        });
    }

    @Override
    public int getItemCount() {
        return requestList.size();
    }

    public static class RequestViewHolder extends RecyclerView.ViewHolder {
        ImageView imgAvatar;
        TextView txtName, txtUsername;
        ImageButton btnAccept, btnReject;

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

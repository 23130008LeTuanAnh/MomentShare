package com.example.momentshare.activity;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.momentshare.R;
import com.example.momentshare.model.ChatRoom;
import com.example.momentshare.model.User;
import com.example.momentshare.repository.UserRepository;
import com.google.firebase.Timestamp;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Adapter hiển thị danh sách phòng chat.
 */
public class ChatRoomAdapter extends RecyclerView.Adapter<ChatRoomAdapter.ChatRoomViewHolder> {

    private final Context context;
    private final List<ChatRoom> rooms;
    private final String currentUserId;
    private final UserRepository userRepository;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm dd/MM", Locale.getDefault());

    public ChatRoomAdapter(Context context, List<ChatRoom> rooms, String currentUserId) {
        this.context = context;
        this.rooms = rooms;
        this.currentUserId = currentUserId;
        this.userRepository = new UserRepository();
    }

    @NonNull
    @Override
    public ChatRoomViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_chat_room, parent, false);
        return new ChatRoomViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatRoomViewHolder holder, int position) {
        ChatRoom room = rooms.get(position);
        String otherUserId = room == null ? "" : room.getOtherUserId(currentUserId);
        holder.itemView.setTag(otherUserId);

        holder.txtChatRoomName.setText("Đang tải...");
        holder.txtChatRoomLastMessage.setText(buildLastMessage(room));
        holder.txtChatRoomTime.setText(formatTime(room == null ? null : room.getLastMessageAt()));
        holder.imgChatRoomAvatar.setImageResource(R.mipmap.ic_launcher);

        if (!otherUserId.trim().isEmpty()) {
            userRepository.getUserById(otherUserId, new UserRepository.UserCallback() {
                @Override
                public void onSuccess(User user) {
                    Object tag = holder.itemView.getTag();
                    if (tag == null || !otherUserId.equals(tag.toString())) return;

                    holder.txtChatRoomName.setText(buildDisplayName(user));
                    if (user.getAvatarUrl() != null && !user.getAvatarUrl().trim().isEmpty()) {
                        Glide.with(context)
                                .load(user.getAvatarUrl())
                                .circleCrop()
                                .placeholder(R.mipmap.ic_launcher)
                                .error(R.mipmap.ic_launcher)
                                .into(holder.imgChatRoomAvatar);
                    }

                    holder.itemView.setOnClickListener(v -> openChat(otherUserId, user));
                }

                @Override
                public void onFailure(String errorMessage) {
                    holder.txtChatRoomName.setText("Bạn bè");
                    holder.itemView.setOnClickListener(v -> openChat(otherUserId, null));
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return rooms == null ? 0 : rooms.size();
    }

    private void openChat(String friendId, User user) {
        Intent intent = new Intent(context, ChatActivity.class);
        intent.putExtra(ChatActivity.EXTRA_FRIEND_ID, friendId);
        if (user != null) {
            intent.putExtra(ChatActivity.EXTRA_FRIEND_NAME, buildDisplayName(user));
            intent.putExtra(ChatActivity.EXTRA_FRIEND_AVATAR, user.getAvatarUrl());
        }
        context.startActivity(intent);
    }

    private String buildLastMessage(ChatRoom room) {
        if (room == null || room.getLastMessage() == null || room.getLastMessage().trim().isEmpty()) {
            return "Chưa có tin nhắn";
        }
        if (currentUserId != null && currentUserId.equals(room.getLastMessageSenderId())) {
            return "Bạn: " + room.getLastMessage();
        }
        return room.getLastMessage();
    }

    private String formatTime(Timestamp timestamp) {
        if (timestamp == null) return "";
        Date date = timestamp.toDate();
        return dateFormat.format(date);
    }

    private String buildDisplayName(User user) {
        if (user == null) return "Bạn bè";
        if (user.getFullName() != null && !user.getFullName().trim().isEmpty()) return user.getFullName().trim();
        if (user.getUsername() != null && !user.getUsername().trim().isEmpty()) return "@" + user.getUsername().trim();
        return "Bạn bè";
    }

    public static class ChatRoomViewHolder extends RecyclerView.ViewHolder {
        ImageView imgChatRoomAvatar;
        TextView txtChatRoomName;
        TextView txtChatRoomLastMessage;
        TextView txtChatRoomTime;

        public ChatRoomViewHolder(@NonNull View itemView) {
            super(itemView);
            imgChatRoomAvatar = itemView.findViewById(R.id.imgChatRoomAvatar);
            txtChatRoomName = itemView.findViewById(R.id.txtChatRoomName);
            txtChatRoomLastMessage = itemView.findViewById(R.id.txtChatRoomLastMessage);
            txtChatRoomTime = itemView.findViewById(R.id.txtChatRoomTime);
        }
    }
}

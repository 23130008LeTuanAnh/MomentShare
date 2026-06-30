package com.example.momentshare.activity;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.momentshare.R;
import com.example.momentshare.model.ChatMessage;
import com.google.firebase.Timestamp;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Adapter hiển thị tin nhắn gửi đi và nhận về bằng 2 layout khác nhau.
 */
public class ChatMessageAdapter extends RecyclerView.Adapter<ChatMessageAdapter.MessageViewHolder> {

    private static final int TYPE_SENT = 1;
    private static final int TYPE_RECEIVED = 2;

    private final List<ChatMessage> messages;
    private final String currentUserId;
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm dd/MM", Locale.getDefault());

    public ChatMessageAdapter(List<ChatMessage> messages, String currentUserId) {
        this.messages = messages;
        this.currentUserId = currentUserId;
    }

    @Override
    public int getItemViewType(int position) {
        ChatMessage message = messages.get(position);
        if (message != null && currentUserId != null && currentUserId.equals(message.getSenderId())) {
            return TYPE_SENT;
        }
        return TYPE_RECEIVED;
    }

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int layoutId = viewType == TYPE_SENT
                ? R.layout.item_chat_message_sent
                : R.layout.item_chat_message_received;
        View view = LayoutInflater.from(parent.getContext()).inflate(layoutId, parent, false);
        return new MessageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        ChatMessage message = messages.get(position);
        holder.txtMessageText.setText(message == null || message.getText() == null ? "" : message.getText());
        holder.txtMessageTime.setText(formatTime(message == null ? null : message.getCreatedAt()));
    }

    @Override
    public int getItemCount() {
        return messages == null ? 0 : messages.size();
    }

    private String formatTime(Timestamp timestamp) {
        if (timestamp == null) return "";
        Date date = timestamp.toDate();
        return timeFormat.format(date);
    }

    public static class MessageViewHolder extends RecyclerView.ViewHolder {
        TextView txtMessageText;
        TextView txtMessageTime;

        public MessageViewHolder(@NonNull View itemView) {
            super(itemView);
            txtMessageText = itemView.findViewById(R.id.txtMessageText);
            txtMessageTime = itemView.findViewById(R.id.txtMessageTime);
        }
    }
}

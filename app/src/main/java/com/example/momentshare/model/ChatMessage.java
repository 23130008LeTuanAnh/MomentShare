package com.example.momentshare.model;

import com.google.firebase.Timestamp;

/**
 * ChatMessage đại diện cho một tin nhắn giữa hai bạn bè.
 *
 * Firestore:
 * chat_rooms/{roomId}/messages/{messageId}
 */
public class ChatMessage {

    private String messageId;
    private String roomId;
    private String senderId;
    private String receiverId;
    private String text;
    private boolean read;
    private Timestamp createdAt;

    public ChatMessage() {
    }

    public ChatMessage(String messageId, String roomId, String senderId,
                       String receiverId, String text, boolean read,
                       Timestamp createdAt) {
        this.messageId = messageId;
        this.roomId = roomId;
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.text = text;
        this.read = read;
        this.createdAt = createdAt;
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public String getRoomId() {
        return roomId;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }

    public String getSenderId() {
        return senderId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public String getReceiverId() {
        return receiverId;
    }

    public void setReceiverId(String receiverId) {
        this.receiverId = receiverId;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public boolean isRead() {
        return read;
    }

    public void setRead(boolean read) {
        this.read = read;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }
}

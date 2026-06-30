package com.example.momentshare.model;

import com.google.firebase.Timestamp;

import java.util.ArrayList;
import java.util.List;

/**
 * ChatRoom lưu thông tin phòng chat 1-1 giữa hai bạn bè.
 *
 * Firestore:
 * chat_rooms/{roomId}
 */
public class ChatRoom {

    private String roomId;
    private List<String> memberIds;
    private String userAId;
    private String userBId;
    private String lastMessage;
    private String lastMessageSenderId;
    private Timestamp lastMessageAt;
    private Timestamp createdAt;
    private Timestamp updatedAt;

    public ChatRoom() {
        memberIds = new ArrayList<>();
    }

    public String getRoomId() {
        return roomId;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }

    public List<String> getMemberIds() {
        return memberIds;
    }

    public void setMemberIds(List<String> memberIds) {
        this.memberIds = memberIds;
    }

    public String getUserAId() {
        return userAId;
    }

    public void setUserAId(String userAId) {
        this.userAId = userAId;
    }

    public String getUserBId() {
        return userBId;
    }

    public void setUserBId(String userBId) {
        this.userBId = userBId;
    }

    public String getLastMessage() {
        return lastMessage;
    }

    public void setLastMessage(String lastMessage) {
        this.lastMessage = lastMessage;
    }

    public String getLastMessageSenderId() {
        return lastMessageSenderId;
    }

    public void setLastMessageSenderId(String lastMessageSenderId) {
        this.lastMessageSenderId = lastMessageSenderId;
    }

    public Timestamp getLastMessageAt() {
        return lastMessageAt;
    }

    public void setLastMessageAt(Timestamp lastMessageAt) {
        this.lastMessageAt = lastMessageAt;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public Timestamp getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Timestamp updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getOtherUserId(String currentUserId) {
        if (currentUserId == null) return "";
        if (currentUserId.equals(userAId)) return userBId == null ? "" : userBId;
        if (currentUserId.equals(userBId)) return userAId == null ? "" : userAId;

        if (memberIds != null) {
            for (String id : memberIds) {
                if (id != null && !id.equals(currentUserId)) return id;
            }
        }
        return "";
    }
}

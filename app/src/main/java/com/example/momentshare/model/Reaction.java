package com.example.momentshare.model;

import com.google.firebase.Timestamp;

public class Reaction {
    private String reactionId;
    private String momentId;
    private String userId;
    private String emoji;
    private Timestamp createdAt;

    public Reaction() {}

    public Reaction(String reactionId, String momentId, String userId, String emoji, Timestamp createdAt) {
        this.reactionId = reactionId;
        this.momentId = momentId;
        this.userId = userId;
        this.emoji = emoji;
        this.createdAt = createdAt;
    }

    public String getReactionId() { return reactionId; }
    public void setReactionId(String reactionId) { this.reactionId = reactionId; }

    public String getMomentId() { return momentId; }
    public void setMomentId(String momentId) { this.momentId = momentId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getEmoji() { return emoji; }
    public void setEmoji(String emoji) { this.emoji = emoji; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
}

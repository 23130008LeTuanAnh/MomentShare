package com.example.momentshare.repository;

import androidx.annotation.NonNull;

import com.example.momentshare.model.Reaction;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

public class ReactionRepository {
    private static final String COLLECTION_REACTIONS = "reactions";
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public interface ReactionListCallback {
        void onSuccess(List<Reaction> reactions);
        void onError(Exception e);
    }

    public interface SaveReactionCallback {
        void onSuccess();
        void onError(Exception e);
    }
    public interface SaveUserReactionCallback {
        void onSuccess(String emoji);
        void onError(Exception e);
    }
    public interface ReactionCountCallback {
        void onSuccess(int count);
        void onError(Exception e);
    }
    public void getReactionsForMoment(@NonNull String momentId, @NonNull ReactionListCallback callback) {
        db.collection(COLLECTION_REACTIONS)
                .whereEqualTo("momentId", momentId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(snapshot -> {
                    List<Reaction> reactions = new ArrayList<>();
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        Reaction r = doc.toObject(Reaction.class);
                        if (r != null) {
                            if (r.getReactionId() == null) r.setReactionId(doc.getId());
                            reactions.add(r);
                        }
                    }
                    callback.onSuccess(reactions);
                })
                .addOnFailureListener(callback::onError);
    }

    public void saveReaction(String momentId, String userId, String emoji, SaveReactionCallback callback) {
        if (momentId == null || momentId.trim().isEmpty()
                || userId == null || userId.trim().isEmpty()
                || emoji == null || emoji.trim().isEmpty()) {
            callback.onError(new IllegalArgumentException("Thiếu dữ liệu reaction"));
            return;
        }

        String reactionId = momentId + "_" + userId;

        Reaction reaction = new Reaction(
                reactionId,
                momentId,
                userId,
                emoji,
                Timestamp.now()
        );

        db.collection(COLLECTION_REACTIONS)
                .document(reactionId)
                .set(reaction)
                .addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(callback::onError);
    }
    public void getUserReaction(String momentId, String userId, SaveUserReactionCallback callback) {
        if (momentId == null || userId == null) {
            callback.onSuccess("");
            return;
        }

        String reactionId = momentId + "_" + userId;

        db.collection(COLLECTION_REACTIONS)
                .document(reactionId)
                .get()
                .addOnSuccessListener(document -> {
                    if (!document.exists()) {
                        callback.onSuccess("");
                        return;
                    }

                    Reaction reaction = document.toObject(Reaction.class);
                    callback.onSuccess(reaction == null ? "" : reaction.getEmoji());
                })
                .addOnFailureListener(callback::onError);
    }


    public void countReactionsForMoment(@NonNull String momentId, @NonNull ReactionCountCallback callback) {
        db.collection(COLLECTION_REACTIONS)
                .whereEqualTo("momentId", momentId)
                .get()
                .addOnSuccessListener(snapshot -> callback.onSuccess(snapshot.size()))
                .addOnFailureListener(callback::onError);
    }
}
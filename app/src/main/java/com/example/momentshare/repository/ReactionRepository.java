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
        // Kiểm tra xem user này đã thả cảm xúc vào ảnh này chưa
        db.collection(COLLECTION_REACTIONS)
                .whereEqualTo("momentId", momentId)
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (!snapshot.isEmpty()) {
                        // Đã thả rồi -> Cập nhật lại emoji mới
                        String existingDocId = snapshot.getDocuments().get(0).getId();
                        db.collection(COLLECTION_REACTIONS).document(existingDocId)
                                .update("emoji", emoji, "createdAt", Timestamp.now())
                                .addOnSuccessListener(aVoid -> callback.onSuccess())
                                .addOnFailureListener(callback::onError);
                    } else {
                        // Chưa thả -> Tạo mới
                        String newId = db.collection(COLLECTION_REACTIONS).document().getId();
                        Reaction newReaction = new Reaction(newId, momentId, userId, emoji, Timestamp.now());
                        db.collection(COLLECTION_REACTIONS).document(newId)
                                .set(newReaction)
                                .addOnSuccessListener(aVoid -> callback.onSuccess())
                                .addOnFailureListener(callback::onError);
                    }
                })
                .addOnFailureListener(callback::onError);
    }
}
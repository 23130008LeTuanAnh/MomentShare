package com.example.momentshare.repository;

import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.momentshare.model.FriendUser;
import com.example.momentshare.model.Moment;
import com.example.momentshare.model.MomentReceiver;
import com.example.momentshare.model.NotificationModel;
import com.example.momentshare.model.User;
import com.example.momentshare.util.Constants;
import com.example.momentshare.util.UploadImageHelper;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class MomentRepository {

    public interface ActionCallback {
        void onSuccess();
        void onFailure(@NonNull String errorMessage);
    }

    public interface FriendsCallback {
        void onSuccess(@NonNull List<FriendUser> friends);
        void onFailure(@NonNull String errorMessage);
    }

    public interface SendMomentCallback {
        void onSuccess(@NonNull String momentId);
        void onFailure(@NonNull String errorMessage);
    }

    public interface IdListCallback {
        void onSuccess(@NonNull Set<String> ids);
        void onFailure(@NonNull String errorMessage);
    }
    public interface MomentListCallback {
        void onSuccess(List<Moment> moments);
        void onError(Exception exception);
    }

    public interface MomentCallback {
        void onSuccess(@Nullable Moment moment);
        void onError(Exception exception);
    }


    private static final String COLLECTION_FRIENDS = "friends";
    private static final String COLLECTION_MOMENTS = "moments";
    private static final String COLLECTION_MOMENT_RECEIVERS = "moment_receivers";
    private static final String COLLECTION_NOTIFICATIONS = "notifications";
    private static final String FIELD_SENDER_ID = "senderId";
    private static final String FIELD_RECEIVER_ID = "receiverId";
    private static final String FIELD_MOMENT_ID = "momentId";
    private static final String FIELD_CREATED_AT = "createdAt";
    private static final String FIELD_STATUS = "status";
    private static final String STATUS_ACTIVE = "active";
    private static final int DEFAULT_LIMIT = 30;

    private final FirebaseFirestore db;
    private final UploadImageHelper uploadImageHelper;

    public MomentRepository() {
        db = FirebaseFirestore.getInstance();
        uploadImageHelper = new UploadImageHelper();
    }

    public void loadSelectableFriends(@NonNull String currentUserId,
                                      @NonNull FriendsCallback callback) {

        loadFriendIdsByField(currentUserId, "userId", new IdListCallback() {
            @Override
            public void onSuccess(@NonNull Set<String> outgoingIds) {
                loadFriendIdsByField(currentUserId, "friendUserId", new IdListCallback() {
                    @Override
                    public void onSuccess(@NonNull Set<String> incomingIds) {
                        Set<String> mergedIds = new LinkedHashSet<>();
                        mergedIds.addAll(outgoingIds);
                        mergedIds.addAll(incomingIds);

                        loadUserProfilesByIds(currentUserId, mergedIds, callback);
                    }

                    @Override
                    public void onFailure(@NonNull String errorMessage) {
                        callback.onFailure(errorMessage);
                    }
                });
            }

            @Override
            public void onFailure(@NonNull String errorMessage) {
                callback.onFailure(errorMessage);
            }
        });
    }

    private void loadFriendIdsByField(@NonNull String currentUserId,
                                      @NonNull String fieldName,
                                      @NonNull IdListCallback callback) {

        db.collection(COLLECTION_FRIENDS)
                .whereEqualTo(fieldName, currentUserId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    Set<String> friendIds = new HashSet<>();

                    for (QueryDocumentSnapshot document : querySnapshot) {
                        String friendId;
                        if ("userId".equals(fieldName)) {
                            friendId = document.getString("friendUserId");
                        } else {
                            friendId = document.getString("userId");
                        }

                        if (friendId != null && !friendId.trim().isEmpty()
                                && !friendId.equals(currentUserId)) {
                            friendIds.add(friendId);
                        }
                    }

                    callback.onSuccess(friendIds);
                })
                .addOnFailureListener(e ->
                        callback.onFailure("Không tải được danh sách bạn bè: " + e.getMessage()));
    }

    private void loadUserProfilesByIds(@NonNull String currentUserId,
                                       @NonNull Set<String> friendIds,
                                       @NonNull FriendsCallback callback) {

        if (friendIds.isEmpty()) {
            callback.onSuccess(new ArrayList<>());
            return;
        }

        List<FriendUser> result = new ArrayList<>();
        int total = friendIds.size();

        final int[] completed = {0};

        for (String friendId : friendIds) {
            db.collection(Constants.COLLECTION_USERS)
                    .document(friendId)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        completed[0]++;

                        if (documentSnapshot.exists()) {
                            User user = documentSnapshot.toObject(User.class);

                            if (user != null
                                    && user.getUserId() != null
                                    && !user.getUserId().equals(currentUserId)
                                    && !Constants.STATUS_LOCKED.equals(user.getStatus())) {

                                result.add(new FriendUser(
                                        user.getUserId(),
                                        user.getFullName(),
                                        user.getUsername(),
                                        user.getEmail(),
                                        user.getAvatarUrl(),
                                        user.getStatus(),
                                        user.getCreatedAt()
                                ));
                            }
                        }

                        if (completed[0] == total) {
                            sortFriends(result);
                            callback.onSuccess(result);
                        }
                    })
                    .addOnFailureListener(e -> {
                        completed[0]++;
                        if (completed[0] == total) {
                            sortFriends(result);
                            callback.onSuccess(result);
                        }
                    });
        }
    }

    private void sortFriends(@NonNull List<FriendUser> result) {
        Collections.sort(result, (a, b) -> {
            String left = a.getFullName() == null ? "" : a.getFullName();
            String right = b.getFullName() == null ? "" : b.getFullName();
            if (left.isEmpty()) {
                left = a.getUsername() == null ? "" : a.getUsername();
            }
            if (right.isEmpty()) {
                right = b.getUsername() == null ? "" : b.getUsername();
            }
            return left.compareToIgnoreCase(right);
        });
    }

    public void sendMoment(@NonNull String senderId,
                           @NonNull Uri imageUri,
                           String caption,
                           @NonNull List<String> receiverIds,
                           @NonNull SendMomentCallback callback) {

        if (receiverIds.isEmpty()) {
            callback.onFailure("Vui lòng chọn ít nhất một người nhận");
            return;
        }

        uploadImageHelper.uploadMomentImage(senderId, imageUri, new UploadImageHelper.UploadCallback() {
            @Override
            public void onSuccess(@NonNull String downloadUrl) {
                createMomentRecords(senderId, downloadUrl, caption, receiverIds, callback);
            }

            @Override
            public void onFailure(@NonNull String errorMessage) {
                callback.onFailure(errorMessage);
            }
        });
    }

    private void createMomentRecords(@NonNull String senderId,
                                     @NonNull String imageUrl,
                                     String caption,
                                     @NonNull List<String> receiverIds,
                                     @NonNull SendMomentCallback callback) {

        DocumentReference momentRef = db.collection(COLLECTION_MOMENTS).document();
        String momentId = momentRef.getId();

        Moment moment = new Moment();
        moment.setMomentId(momentId);
        moment.setSenderId(senderId);
        moment.setImageUrl(imageUrl);
        moment.setCaption(caption == null ? "" : caption.trim());
        moment.setCreatedAt(Timestamp.now());
        moment.setStatus("active");

        var batch = db.batch();
        batch.set(momentRef, moment);

        for (String receiverId : receiverIds) {
            if (receiverId == null || receiverId.trim().isEmpty()) {
                continue;
            }

            DocumentReference receiverRef = db.collection(COLLECTION_MOMENT_RECEIVERS).document();
            MomentReceiver receiver = new MomentReceiver();
            receiver.setId(receiverRef.getId());
            receiver.setMomentId(momentId);
            receiver.setReceiverId(receiverId);
            receiver.setViewed(false);
            receiver.setViewedAt(null);

            batch.set(receiverRef, receiver);

            DocumentReference notificationRef = db.collection(COLLECTION_NOTIFICATIONS).document();
            NotificationModel notification = new NotificationModel();
            notification.setNotificationId(notificationRef.getId());
            notification.setUserId(receiverId);
            notification.setType("moment");
            notification.setTitle("Khoảnh khắc mới");
            notification.setMessage("Bạn vừa nhận được một khoảnh khắc mới.");
            notification.setRead(false);
            notification.setCreatedAt(Timestamp.now());

            batch.set(notificationRef, notification);
        }

        batch.commit()
                .addOnSuccessListener(unused -> callback.onSuccess(momentId))
                .addOnFailureListener(e ->
                        callback.onFailure("Không thể lưu khoảnh khắc: " + e.getMessage()));
    }
    public void getHomeFeed(@NonNull String currentUserId, @NonNull MomentListCallback callback) {
        db.collection(COLLECTION_MOMENT_RECEIVERS)
                .whereEqualTo(FIELD_RECEIVER_ID, currentUserId)
                .limit(DEFAULT_LIMIT)
                .get()
                .addOnSuccessListener(snapshot -> {
                    List<String> momentIds = new ArrayList<>();
                    for (DocumentSnapshot document : snapshot.getDocuments()) {
                        String momentId = document.getString(FIELD_MOMENT_ID);
                        if (momentId != null && !momentId.isEmpty()) {
                            momentIds.add(momentId);
                        }
                    }
                    loadMomentsByIds(momentIds, callback);
                })
                .addOnFailureListener(callback::onError);
    }

    public void getSentHistory(@NonNull String currentUserId, @NonNull MomentListCallback callback) {
        db.collection(COLLECTION_MOMENTS)
                .whereEqualTo(FIELD_SENDER_ID, currentUserId)
                .whereEqualTo(FIELD_STATUS, STATUS_ACTIVE)
                .orderBy(FIELD_CREATED_AT, Query.Direction.DESCENDING)
                .limit(DEFAULT_LIMIT)
                .get()
                .addOnSuccessListener(snapshot -> callback.onSuccess(mapMomentList(snapshot.getDocuments())))
                .addOnFailureListener(callback::onError);
    }

    public void getReceivedHistory(@NonNull String currentUserId, @NonNull MomentListCallback callback) {
        getHomeFeed(currentUserId, callback);
    }

    public void getMomentById(@NonNull String momentId, @NonNull MomentCallback callback) {
        db.collection(COLLECTION_MOMENTS)
                .document(momentId)
                .get()
                .addOnSuccessListener(document -> {
                    if (!document.exists()) {
                        callback.onSuccess(null);
                        return;
                    }
                    callback.onSuccess(mapMoment(document));
                })
                .addOnFailureListener(callback::onError);
    }

    private void loadMomentsByIds(List<String> momentIds, MomentListCallback callback) {
        if (momentIds.isEmpty()) {
            callback.onSuccess(new ArrayList<>());
            return;
        }

        List<Moment> result = new ArrayList<>();
        final int[] completedCount = {0};
        final boolean[] failed = {false};

        for (String momentId : momentIds) {
            db.collection(COLLECTION_MOMENTS)
                    .document(momentId)
                    .get()
                    .addOnSuccessListener(document -> {
                        if (failed[0]) return;
                        Moment moment = mapMoment(document);
                        if (moment != null && STATUS_ACTIVE.equals(moment.getStatus())) {
                            result.add(moment);
                        }
                        completedCount[0]++;
                        if (completedCount[0] == momentIds.size()) {
                            callback.onSuccess(result);
                        }
                    })
                    .addOnFailureListener(exception -> {
                        if (!failed[0]) {
                            failed[0] = true;
                            callback.onError(exception);
                        }
                    });
        }
    }
    private List<Moment> mapMomentList(List<DocumentSnapshot> documents) {
        List<Moment> moments = new ArrayList<>();
        for (DocumentSnapshot document : documents) {
            Moment moment = mapMoment(document);
            if (moment != null) moments.add(moment);
        }
        return moments;
    }

    @Nullable
    private Moment mapMoment(DocumentSnapshot document) {
        if (document == null || !document.exists()) return null;
        Moment moment = document.toObject(Moment.class);
        if (moment != null && (moment.getMomentId() == null || moment.getMomentId().isEmpty())) {
            moment.setMomentId(document.getId());
        }
        return moment;
    }
}

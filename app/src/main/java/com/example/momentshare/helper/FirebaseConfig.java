package com.example.momentshare.helper;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;

/**
 * FirebaseConfig cung cấp các instance Firebase dùng chung cho module Người 5.
 * Class này giúp Activity/Repository không phải khởi tạo lặp lại FirebaseAuth, Firestore và Storage.
 * Chỉ gom nơi lấy instance, không xử lý nghiệp vụ trực tiếp trong helper này.
 */
public class FirebaseConfig {

    private static FirebaseAuth auth;
    private static FirebaseFirestore firestore;
    private static FirebaseStorage storage;

    public static FirebaseAuth getAuth() {
        if (auth == null) {
            auth = FirebaseAuth.getInstance();
        }
        return auth;
    }

    public static FirebaseFirestore getFirestore() {
        if (firestore == null) {
            firestore = FirebaseFirestore.getInstance();
        }
        return firestore;
    }

    public static FirebaseStorage getStorage() {
        if (storage == null) {
            storage = FirebaseStorage.getInstance();
        }
        return storage;
    }

    private FirebaseConfig() {
    }
}

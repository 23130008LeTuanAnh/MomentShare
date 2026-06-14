package com.example.momentshare.repository;

import androidx.annotation.NonNull;

import com.example.momentshare.model.User;
import com.example.momentshare.util.Constants;
import com.example.momentshare.util.ValidationUtils;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

/**
 * AuthManager chịu trách nhiệm xử lý Firebase Authentication.
 *
 * File này thuộc phần Người 1 - Tài khoản, hồ sơ cá nhân.
 *
 * Nhiệm vụ chính:
 * - Đăng ký tài khoản bằng email và mật khẩu.
 * - Đăng nhập bằng email và mật khẩu.
 * - Kiểm tra trạng thái tài khoản active/locked trong Firestore.
 * - Đăng xuất khỏi tài khoản hiện tại.
 */
public class AuthManager {

    private final FirebaseAuth firebaseAuth;
    private final UserRepository userRepository;

    /**
     * Constructor khởi tạo FirebaseAuth và UserRepository.
     */
    public AuthManager() {
        firebaseAuth = FirebaseAuth.getInstance();
        userRepository = new UserRepository();
    }

    /**
     * Callback dùng cho đăng ký/đăng nhập/đăng xuất.
     */
    public interface AuthCallback {
        void onSuccess();

        void onFailure(String errorMessage);
    }

    /**
     * Kiểm tra người dùng đã đăng nhập hay chưa.
     *
     * @return true nếu FirebaseAuth đang có currentUser
     */
    public boolean isLoggedIn() {
        return firebaseAuth.getCurrentUser() != null;
    }

    /**
     * Lấy userId của tài khoản hiện tại.
     *
     * @return uid nếu đã đăng nhập, null nếu chưa đăng nhập
     */
    public String getCurrentUserId() {
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();

        if (currentUser == null) {
            return null;
        }

        return currentUser.getUid();
    }

    /**
     * Đăng xuất khỏi tài khoản hiện tại.
     */
    public void logout() {
        firebaseAuth.signOut();
    }

    /**
     * Đăng ký tài khoản mới.
     *
     * Quy trình:
     * 1. Kiểm tra username đã tồn tại chưa.
     * 2. Tạo tài khoản bằng Firebase Authentication.
     * 3. Lưu hồ sơ người dùng vào collection users trên Firestore.
     *
     * @param fullName họ tên người dùng
     * @param username username dùng để tìm kiếm/kết bạn
     * @param email email đăng nhập
     * @param password mật khẩu
     * @param callback callback báo thành công/thất bại
     */
    public void register(@NonNull String fullName,
                         @NonNull String username,
                         @NonNull String email,
                         @NonNull String password,
                         @NonNull AuthCallback callback) {

        String normalizedUsername = ValidationUtils.normalizeUsername(username);

        userRepository.checkUsernameExists(normalizedUsername, new UserRepository.BooleanCallback() {
            @Override
            public void onSuccess(boolean usernameExists) {
                if (usernameExists) {
                    callback.onFailure("Username đã được sử dụng");
                    return;
                }

                createFirebaseAccount(fullName, normalizedUsername, email, password, callback);
            }

            @Override
            public void onFailure(String errorMessage) {
                callback.onFailure(errorMessage);
            }
        });
    }

    /**
     * Tạo tài khoản bằng Firebase Authentication.
     */
    private void createFirebaseAccount(@NonNull String fullName,
                                       @NonNull String username,
                                       @NonNull String email,
                                       @NonNull String password,
                                       @NonNull AuthCallback callback) {

        firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    FirebaseUser firebaseUser = authResult.getUser();

                    if (firebaseUser == null) {
                        callback.onFailure("Không thể tạo tài khoản");
                        return;
                    }

                    createUserProfile(firebaseUser.getUid(), fullName, username, email, callback);
                })
                .addOnFailureListener(e ->
                        callback.onFailure("Đăng ký thất bại: " + e.getMessage()));
    }

    /**
     * Tạo hồ sơ người dùng trong Firestore sau khi FirebaseAuth tạo tài khoản thành công.
     */
    private void createUserProfile(@NonNull String userId,
                                   @NonNull String fullName,
                                   @NonNull String username,
                                   @NonNull String email,
                                   @NonNull AuthCallback callback) {

        User user = new User(
                userId,
                fullName,
                username,
                email,
                Constants.DEFAULT_AVATAR_URL,
                "",
                Constants.ROLE_USER,
                Constants.STATUS_ACTIVE,
                Timestamp.now()
        );

        userRepository.createUser(user, new UserRepository.ActionCallback() {
            @Override
            public void onSuccess() {
                callback.onSuccess();
            }

            @Override
            public void onFailure(String errorMessage) {
                callback.onFailure(errorMessage);
            }
        });
    }

    /**
     * Đăng nhập bằng email và mật khẩu.
     *
     * Sau khi FirebaseAuth đăng nhập thành công, hệ thống đọc users/{userId}
     * để kiểm tra tài khoản có bị khóa hay không.
     *
     * @param email email đăng nhập
     * @param password mật khẩu
     * @param callback callback báo thành công/thất bại
     */
    public void login(@NonNull String email,
                      @NonNull String password,
                      @NonNull AuthCallback callback) {

        firebaseAuth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    String userId = getCurrentUserId();

                    if (userId == null) {
                        callback.onFailure("Không thể xác định tài khoản đăng nhập");
                        return;
                    }

                    checkAccountStatus(userId, callback);
                })
                .addOnFailureListener(e ->
                        callback.onFailure("Đăng nhập thất bại: " + e.getMessage()));
    }

    /**
     * Kiểm tra trạng thái tài khoản sau khi đăng nhập.
     *
     * Nếu status = locked thì đăng xuất và không cho truy cập ứng dụng.
     */
    private void checkAccountStatus(@NonNull String userId,
                                    @NonNull AuthCallback callback) {

        userRepository.getUserById(userId, new UserRepository.UserCallback() {
            @Override
            public void onSuccess(User user) {
                if (Constants.STATUS_LOCKED.equals(user.getStatus())) {
                    logout();
                    callback.onFailure("Tài khoản đã bị khóa");
                    return;
                }

                callback.onSuccess();
            }

            @Override
            public void onFailure(String errorMessage) {
                logout();
                callback.onFailure(errorMessage);
            }
        });
    }
}
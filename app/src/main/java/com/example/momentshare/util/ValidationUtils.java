package com.example.momentshare.util;

import android.text.TextUtils;
import android.util.Patterns;

/**
 * ValidationUtils chứa các hàm kiểm tra dữ liệu nhập từ người dùng.
 *
 * File này phục vụ phần Người 1:
 * - Đăng ký tài khoản.
 * - Đăng nhập.
 * - Chỉnh sửa hồ sơ cá nhân.
 */
public class ValidationUtils {

    /**
     * Kiểm tra chuỗi có rỗng hoặc chỉ chứa khoảng trắng hay không.
     *
     * @param value chuỗi cần kiểm tra
     * @return true nếu chuỗi rỗng, false nếu có dữ liệu
     */
    public static boolean isEmpty(String value) {
        return value == null || value.trim().isEmpty();
    }

    /**
     * Kiểm tra email có đúng định dạng cơ bản hay không.
     *
     * @param email email người dùng nhập
     * @return true nếu email hợp lệ, false nếu sai định dạng
     */
    public static boolean isValidEmail(String email) {
        return !isEmpty(email) && Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches();
    }

    /**
     * Kiểm tra mật khẩu có đủ độ dài tối thiểu hay không.
     * Firebase Authentication thường yêu cầu mật khẩu tối thiểu 6 ký tự.
     *
     * @param password mật khẩu người dùng nhập
     * @return true nếu mật khẩu hợp lệ, false nếu quá ngắn hoặc rỗng
     */
    public static boolean isValidPassword(String password) {
        return !isEmpty(password) && password.length() >= 6;
    }

    /**
     * Kiểm tra mật khẩu và xác nhận mật khẩu có trùng nhau không.
     *
     * @param password mật khẩu
     * @param confirmPassword mật khẩu xác nhận
     * @return true nếu hai mật khẩu giống nhau
     */
    public static boolean isPasswordMatched(String password, String confirmPassword) {
        return !TextUtils.isEmpty(password) && password.equals(confirmPassword);
    }

    /**
     * Chuẩn hóa username trước khi lưu hoặc tìm kiếm.
     *
     * @param username username người dùng nhập
     * @return username đã trim và chuyển về chữ thường
     */
    public static String normalizeUsername(String username) {
        if (username == null) {
            return "";
        }
        return username.trim().toLowerCase();
    }

    /**
     * Constructor private để không cho tạo object ValidationUtils.
     */
    private ValidationUtils() {
    }
}
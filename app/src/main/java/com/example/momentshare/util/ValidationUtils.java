package com.example.momentshare.util;

import android.text.TextUtils;
import android.util.Patterns;

import java.util.Locale;

/**
 * ValidationUtils chứa các hàm kiểm tra dữ liệu nhập từ người dùng.
 *
 * File này phục vụ phần Người 1:
 * - Đăng ký tài khoản.
 * - Đăng nhập.
 * - Chỉnh sửa hồ sơ cá nhân.
 */
public class ValidationUtils {

    // Người 1 thực hiện: cấu hình validate username chặt hơn cho chức năng đăng ký/tìm kiếm.
    private static final int MIN_USERNAME_LENGTH = 3;
    private static final int MAX_USERNAME_LENGTH = 20;
    private static final String USERNAME_PATTERN = "^[a-z][a-z0-9_]{2,19}$";

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
     * Người 1 thực hiện: chuẩn hóa email trước khi đăng ký, đăng nhập hoặc tìm kiếm.
     *
     * Firebase Authentication không phân biệt hoa/thường khi đăng nhập, nhưng Firestore
     * query whereEqualTo() thì so khớp chính xác chuỗi. Vì vậy email cần được lưu
     * và tìm kiếm cùng một chuẩn chữ thường để tránh lỗi khi người dùng nhập Email viết hoa.
     *
     * @param email email người dùng nhập
     * @return email đã trim và chuyển về chữ thường
     */
    public static String normalizeEmail(String email) {
        if (email == null) {
            return "";
        }
        return email.trim().toLowerCase(Locale.ROOT);
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
     * Người 1 thực hiện: chuẩn hóa username trước khi lưu hoặc tìm kiếm.
     *
     * @param username username người dùng nhập
     * @return username đã trim và chuyển về chữ thường
     */
    public static String normalizeUsername(String username) {
        if (username == null) {
            return "";
        }
        return username.trim().toLowerCase(Locale.ROOT);
    }

    /**
     * Người 1 thực hiện: kiểm tra username theo quy tắc chặt hơn để dữ liệu dễ tìm kiếm và tránh ký tự lạ.
     *
     * Quy tắc:
     * - Dài từ 3 đến 20 ký tự.
     * - Bắt đầu bằng chữ cái.
     * - Chỉ gồm chữ thường không dấu, số và dấu gạch dưới.
     * - Không chứa khoảng trắng, dấu tiếng Việt hoặc ký tự đặc biệt.
     *
     * @param username username người dùng nhập
     * @return true nếu username hợp lệ
     */
    public static boolean isValidUsername(String username) {
        String normalizedUsername = normalizeUsername(username);
        return normalizedUsername.length() >= MIN_USERNAME_LENGTH
                && normalizedUsername.length() <= MAX_USERNAME_LENGTH
                && normalizedUsername.matches(USERNAME_PATTERN);
    }

    /**
     * Người 1 thực hiện: thông báo lỗi dùng chung cho các màn hình nhập username.
     *
     * @return nội dung lỗi validate username
     */
    public static String getUsernameErrorMessage() {
        return "Username phải dài 3-20 ký tự, bắt đầu bằng chữ cái, chỉ gồm chữ thường, số và dấu gạch dưới";
    }

    /**
     * Constructor private để không cho tạo object ValidationUtils.
     */
    private ValidationUtils() {
    }
}

package com.example.momentshare;

import com.example.momentshare.model.ReportModel;
import com.example.momentshare.util.Constants;
import com.example.momentshare.util.ValidationUtils;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Unit test thật cho các phần xử lý nghiệp vụ cơ bản của MomentShare.
 * File này thay thế test mặc định 2 + 2 = 4.
 */
public class ExampleUnitTest {

    @Test
    public void usernameValidation_acceptsValidUsername() {
        // Người 5 thực hiện: test validate username hợp lệ phục vụ chức năng đăng ký/tìm kiếm.
        assertTrue(ValidationUtils.isValidUsername("tuananh"));
        assertTrue(ValidationUtils.isValidUsername("tuan_anh01"));
    }

    @Test
    public void usernameValidation_rejectsInvalidUsername() {
        // Người 5 thực hiện: test các username sai quy tắc để tránh dữ liệu khó tìm kiếm trên Firestore.
        assertFalse(ValidationUtils.isValidUsername("12tuan"));
        assertFalse(ValidationUtils.isValidUsername("tuấnanh"));
        assertFalse(ValidationUtils.isValidUsername("tuan anh"));
        assertFalse(ValidationUtils.isValidUsername("ta"));
    }

    @Test
    public void normalizeEmail_convertsEmailToLowerCase() {
        // Người 5 thực hiện: test chuẩn hóa email để tránh lỗi query Firestore do khác chữ hoa/thường.
        assertEquals("letuananh@gmail.com", ValidationUtils.normalizeEmail("  LeTuanAnh@Gmail.Com  "));
    }

    @Test
    public void reportModel_storesUserReportDataCorrectly() {
        // Người 5 thực hiện: test model báo cáo nội dung mà user gửi cho Admin xử lý.
        ReportModel report = new ReportModel(
                "report01",
                "user01",
                "moment01",
                "Nội dung phản cảm",
                Constants.REPORT_STATUS_PENDING,
                null,
                "",
                null
        );

        assertEquals("report01", report.getReportId());
        assertEquals("user01", report.getReporterId());
        assertEquals("moment01", report.getMomentId());
        assertEquals("Nội dung phản cảm", report.getReason());
        assertEquals(Constants.REPORT_STATUS_PENDING, report.getStatus());
    }

    @Test
    public void constants_containsUserAndAdminRoles() {
        // Người 5 thực hiện: test hằng số quyền dùng cho Firestore Security Rules và Admin screen.
        assertEquals("USER", Constants.ROLE_USER);
        assertEquals("ADMIN", Constants.ROLE_ADMIN);
        assertEquals("pending", Constants.REPORT_STATUS_PENDING);
    }
}

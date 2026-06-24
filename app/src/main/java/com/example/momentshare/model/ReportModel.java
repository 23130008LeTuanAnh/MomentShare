package com.example.momentshare.model;

import com.google.firebase.Timestamp;

/**
 * ReportModel đại diện cho một báo cáo nội dung do người dùng gửi lên.
 * File này thuộc phần Người 5 - Báo cáo nội dung.
 * Admin dùng dữ liệu này để xem lý do báo cáo và xử lý khoảnh khắc vi phạm.
 */
public class ReportModel {

    private String reportId;
    private String reporterId;
    private String momentId;
    private String reason;
    private String status;
    private Timestamp createdAt;
    private String handledBy;
    private Timestamp handledAt;

    public ReportModel() {
    }

    public ReportModel(String reportId, String reporterId, String momentId, String reason,
                       String status, Timestamp createdAt, String handledBy, Timestamp handledAt) {
        this.reportId = reportId;
        this.reporterId = reporterId;
        this.momentId = momentId;
        this.reason = reason;
        this.status = status;
        this.createdAt = createdAt;
        this.handledBy = handledBy;
        this.handledAt = handledAt;
    }

    public String getReportId() {
        return reportId;
    }

    public void setReportId(String reportId) {
        this.reportId = reportId;
    }

    public String getReporterId() {
        return reporterId;
    }

    public void setReporterId(String reporterId) {
        this.reporterId = reporterId;
    }

    public String getMomentId() {
        return momentId;
    }

    public void setMomentId(String momentId) {
        this.momentId = momentId;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public String getHandledBy() {
        return handledBy;
    }

    public void setHandledBy(String handledBy) {
        this.handledBy = handledBy;
    }

    public Timestamp getHandledAt() {
        return handledAt;
    }

    public void setHandledAt(Timestamp handledAt) {
        this.handledAt = handledAt;
    }
}

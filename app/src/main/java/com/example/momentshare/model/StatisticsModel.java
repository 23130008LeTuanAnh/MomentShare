package com.example.momentshare.model;

/**
 * StatisticsModel gom các chỉ số quản trị để hiển thị trên Admin Dashboard và StatisticsActivity.
 *
 * Giữ constructor 5 tham số cũ để không làm hỏng code cũ.
 * Bổ sung các field chi tiết hơn cho màn hình thống kê.
 */
public class StatisticsModel {

    private int totalUsers;
    private int activeUsers;
    private int lockedUsers;

    private int totalMoments;
    private int activeMoments;
    private int hiddenMoments;

    private int totalReports;
    private int pendingReports;
    private int resolvedReports;
    private int ignoredReports;

    public StatisticsModel() {
    }

    public StatisticsModel(int totalUsers, int totalMoments, int totalReports,
                           int pendingReports, int lockedUsers) {
        this.totalUsers = totalUsers;
        this.totalMoments = totalMoments;
        this.totalReports = totalReports;
        this.pendingReports = pendingReports;
        this.lockedUsers = lockedUsers;
    }

    public StatisticsModel(int totalUsers, int activeUsers, int lockedUsers,
                           int totalMoments, int activeMoments, int hiddenMoments,
                           int totalReports, int pendingReports, int resolvedReports, int ignoredReports) {
        this.totalUsers = totalUsers;
        this.activeUsers = activeUsers;
        this.lockedUsers = lockedUsers;
        this.totalMoments = totalMoments;
        this.activeMoments = activeMoments;
        this.hiddenMoments = hiddenMoments;
        this.totalReports = totalReports;
        this.pendingReports = pendingReports;
        this.resolvedReports = resolvedReports;
        this.ignoredReports = ignoredReports;
    }

    public int getTotalUsers() {
        return totalUsers;
    }

    public void setTotalUsers(int totalUsers) {
        this.totalUsers = totalUsers;
    }

    public int getActiveUsers() {
        return activeUsers;
    }

    public void setActiveUsers(int activeUsers) {
        this.activeUsers = activeUsers;
    }

    public int getLockedUsers() {
        return lockedUsers;
    }

    public void setLockedUsers(int lockedUsers) {
        this.lockedUsers = lockedUsers;
    }

    public int getTotalMoments() {
        return totalMoments;
    }

    public void setTotalMoments(int totalMoments) {
        this.totalMoments = totalMoments;
    }

    public int getActiveMoments() {
        return activeMoments;
    }

    public void setActiveMoments(int activeMoments) {
        this.activeMoments = activeMoments;
    }

    public int getHiddenMoments() {
        return hiddenMoments;
    }

    public void setHiddenMoments(int hiddenMoments) {
        this.hiddenMoments = hiddenMoments;
    }

    public int getTotalReports() {
        return totalReports;
    }

    public void setTotalReports(int totalReports) {
        this.totalReports = totalReports;
    }

    public int getPendingReports() {
        return pendingReports;
    }

    public void setPendingReports(int pendingReports) {
        this.pendingReports = pendingReports;
    }

    public int getResolvedReports() {
        return resolvedReports;
    }

    public void setResolvedReports(int resolvedReports) {
        this.resolvedReports = resolvedReports;
    }

    public int getIgnoredReports() {
        return ignoredReports;
    }

    public void setIgnoredReports(int ignoredReports) {
        this.ignoredReports = ignoredReports;
    }
}

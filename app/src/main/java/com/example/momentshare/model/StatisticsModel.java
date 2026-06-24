package com.example.momentshare.model;

/**
 * StatisticsModel gom các chỉ số cơ bản để hiển thị trên Admin Dashboard.
 */
public class StatisticsModel {

    private int totalUsers;
    private int totalMoments;
    private int totalReports;
    private int pendingReports;
    private int lockedUsers;

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

    public int getTotalUsers() {
        return totalUsers;
    }

    public void setTotalUsers(int totalUsers) {
        this.totalUsers = totalUsers;
    }

    public int getTotalMoments() {
        return totalMoments;
    }

    public void setTotalMoments(int totalMoments) {
        this.totalMoments = totalMoments;
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

    public int getLockedUsers() {
        return lockedUsers;
    }

    public void setLockedUsers(int lockedUsers) {
        this.lockedUsers = lockedUsers;
    }
}

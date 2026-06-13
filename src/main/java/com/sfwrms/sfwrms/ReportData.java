package com.sfwrms.sfwrms;

import java.util.List;
import java.util.Map;

public class ReportData {
    private int totalDonations;
    private int deliveredDonations;
    private int mealsSaved;
    private double successRate;
    private List<Map<String, Object>> topDonors;
    private Map<String, Object> topDriver;
    private String period;

    public ReportData(int totalDonations, int deliveredDonations, int mealsSaved, double successRate) {
        this.totalDonations = totalDonations;
        this.deliveredDonations = deliveredDonations;
        this.mealsSaved = mealsSaved;
        this.successRate = successRate;
    }

    public ReportData(int totalDonations, int deliveredDonations, int mealsSaved, double successRate,
                      List<Map<String, Object>> topDonors, Map<String, Object> topDriver, String period) {
        this.totalDonations = totalDonations;
        this.deliveredDonations = deliveredDonations;
        this.mealsSaved = mealsSaved;
        this.successRate = successRate;
        this.topDonors = topDonors;
        this.topDriver = topDriver;
        this.period = period;
    }

    public int getTotalDonations() { return totalDonations; }
    public int getDeliveredDonations() { return deliveredDonations; }
    public int getMealsSaved() { return mealsSaved; }
    public double getSuccessRate() { return successRate; }
    public List<Map<String, Object>> getTopDonors() { return topDonors; }
    public Map<String, Object> getTopDriver() { return topDriver; }
    public String getPeriod() { return period; }
}

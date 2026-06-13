package com.sfwrms.sfwrms;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.util.List;
import java.util.Map;


public class ReportController {

    @FXML private ComboBox<String> cbPeriod;
    @FXML private Label lblTotal;
    @FXML private Label lblDelivered;
    @FXML private Label lblMeals;
    @FXML private Label lblRate;
    @FXML private Label lblError;
    @FXML private Button btnBack;
    
    // Top donors display
    @FXML private VBox topDonorsBox;
    @FXML private Label lblTopDonor1;
    @FXML private Label lblTopDonor2;
    @FXML private Label lblTopDonor3;
    @FXML private Label lblTopDonor4;
    @FXML private Label lblTopDonor5;
    
    // Top driver display
    @FXML private VBox topDriverBox;
    @FXML private Label lblTopDriverName;
    @FXML private Label lblTopDriverDeliveries;
    @FXML private Label lblTopDriverVehicle;

    @FXML
    public void initialize() {
        cbPeriod.getItems().addAll("Weekly", "Monthly", "Yearly");
        cbPeriod.setValue("Monthly");
    }

    @FXML
    public void handleGenerate() {
        String period = cbPeriod.getValue();
        if (period == null) { lblError.setText("Please select a period."); return; }
        lblError.setText("");

        // Delegate entirely to AppBackend (Facade, UC06)
        ReportData data = AppBackend.getInstance().generateReport(SessionManager.getRepId(), period);

        if (data == null) {
            lblError.setText("Failed to generate report. Check DB connection.");
            return;
        }

        lblTotal.setText(String.valueOf(data.getTotalDonations()));
        lblDelivered.setText(String.valueOf(data.getDeliveredDonations()));
        lblMeals.setText(String.valueOf(data.getMealsSaved()));
        lblRate.setText(String.format("%.1f%%", data.getSuccessRate()));
        
        // Display top donors
        displayTopDonors(data.getTopDonors());
        
        // Display top driver
        displayTopDriver(data.getTopDriver());
    }

    private void displayTopDonors(List<Map<String, Object>> topDonors) {
        Label[] donorLabels = {lblTopDonor1, lblTopDonor2, lblTopDonor3, lblTopDonor4, lblTopDonor5};
        
        if (topDonors == null || topDonors.isEmpty()) {
            for (Label lbl : donorLabels) {
                if (lbl != null) lbl.setText("—");
            }
            return;
        }
        
        int index = 0;
        for (Map<String, Object> donor : topDonors) {
            if (index < donorLabels.length && donorLabels[index] != null) {
                String name = (String) donor.get("name");
                Integer count = (Integer) donor.get("donationCount");
                Integer qty = (Integer) donor.get("totalQuantity");
                String org = (String) donor.get("organization");
                org = (org == null || org.isEmpty()) ? "" : " (" + org + ")";
                donorLabels[index].setText((index + 1) + ". " + name + org + " — " + count + " donations, " + qty + " kg");
            }
            index++;
        }
        
        // Fill remaining labels with dash
        for (int i = index; i < donorLabels.length; i++) {
            if (donorLabels[i] != null) donorLabels[i].setText("—");
        }
    }

    private void displayTopDriver(Map<String, Object> topDriver) {
        if (topDriver == null || topDriver.isEmpty()) {
            if (lblTopDriverName != null) lblTopDriverName.setText("—");
            if (lblTopDriverDeliveries != null) lblTopDriverDeliveries.setText("—");
            if (lblTopDriverVehicle != null) lblTopDriverVehicle.setText("—");
            return;
        }
        
        String name = (String) topDriver.get("name");
        Integer deliveries = (Integer) topDriver.get("completedDeliveries");
        String vehicle = (String) topDriver.get("vehicleInfo");
        
        if (lblTopDriverName != null) lblTopDriverName.setText(name != null ? name : "—");
        if (lblTopDriverDeliveries != null) lblTopDriverDeliveries.setText(deliveries != null ? deliveries + " deliveries" : "—");
        if (lblTopDriverVehicle != null) lblTopDriverVehicle.setText(vehicle != null ? vehicle : "—");
    }

    @FXML
    public void handleBack() throws IOException {
        SceneHelper.switchTo(btnBack, "ngo-dashboard.fxml", 1280, 800);
    }
}

package com.sfwrms.sfwrms;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;


public class NGODashboardController implements Initializable {

    @FXML private Label  lblUserName;
    @FXML private Button btnLogout;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        lblUserName.setText("Welcome, " + SessionManager.getUserName());
    }

    // ── UC05: View Total Meals Saved ──────────────────────────────────────────
    @FXML
    public void handleViewMealsSaved() {
        int meals = AppBackend.getInstance().getTotalMealsSaved();
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("UC05 — Total Meals Saved");
        alert.setHeaderText("🍽 Meals Saved (System-Wide)");
        alert.setContentText("Total meals saved so far: " + meals +
            "\n\n(Business rule: 2 kg of delivered food = 1 meal)");
        alert.showAndWait();
    }

    // ── UC06: Generate Report ─────────────────────────────────────────────────
    @FXML
    public void handleGenerateReport() throws IOException {
        SceneHelper.switchTo(btnLogout, "report-view.fxml", 1280, 800);
    }

    // ── UC07 + UC09: View Available Donations (with Accept/Reject) ───────────
    @FXML
    public void handleViewDonations() throws IOException {
        SceneHelper.switchTo(btnLogout, "ngo-donations.fxml", 1280, 800);
    }

    // ── UC09: View Assignments (existing accepted assignments) ────────────────
    @FXML
    public void handleViewAssignments() throws IOException {
        SceneHelper.switchTo(btnLogout, "ngo-assignments.fxml", 1280, 800);
    }

    // ── UC10: Assign Driver to Pickup ─────────────────────────────────────────
    @FXML
    public void handleAssignDriver() throws IOException {
        NGOAssignDriverController.selectedAssignment = null;
        SceneHelper.switchTo(btnLogout, "ngo-assign-driver.fxml", 1280, 800);
    }

    @FXML
    public void handleLogout() throws IOException {
        SessionManager.clear();
        SceneHelper.switchTo(btnLogout, "login-view.fxml", 1280, 800);
    }
}

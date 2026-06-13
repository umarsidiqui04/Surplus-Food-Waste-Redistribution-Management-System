package com.sfwrms.sfwrms;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;


public class DonorDashboardController implements Initializable {

    @FXML private Label  lblUserName;
    @FXML private Button btnLogout;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        lblUserName.setText("Welcome, " + SessionManager.getUserName());
    }

    // ── UC01: Log Surplus Food — navigate to Add Donation form ────────────────
    @FXML
    public void handleAddDonation() throws IOException {
        AddEditDonationController.currentDonation = null;
        SceneHelper.switchTo(btnLogout, "add-edit-donation.fxml", 1280, 800);
    }

    // ── UC02: Modify Donation — navigate to donations list in EDIT mode ───────
    @FXML
    public void handleEditDonation() throws IOException {
        DonorDonationsController.mode = "edit";
        SceneHelper.switchTo(btnLogout, "donor-donations.fxml", 1280, 800);
    }

    // ── UC03: Delete Donation — navigate to donations list in DELETE mode ──────
    @FXML
    public void handleDeleteDonation() throws IOException {
        DonorDonationsController.mode = "delete";
        SceneHelper.switchTo(btnLogout, "donor-donations.fxml", 1280, 800);
    }

    // ── UC04: Track Donation Status — navigate to donations list in TRACK mode ─
    @FXML
    public void handleTrackDonations() throws IOException {
        DonorDonationsController.mode = "track";
        SceneHelper.switchTo(btnLogout, "donor-donations.fxml", 1280, 800);
    }

    @FXML
    public void handleLogout() throws IOException {
        SessionManager.clear();
        SceneHelper.switchTo(btnLogout, "login-view.fxml", 1280, 800);
    }
}

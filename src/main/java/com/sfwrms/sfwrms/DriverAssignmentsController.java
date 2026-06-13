package com.sfwrms.sfwrms;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;

public class DriverAssignmentsController implements Initializable {

    /** Set by DriverDashboardController before navigation. */
    public static String mode = "accept";

    @FXML private Label  lblTitle;
    @FXML private VBox   noAssignmentBox;
    @FXML private Label  lblNoAssignment;
    @FXML private VBox   assignmentCard;
    @FXML private HBox   cardHeader;
    @FXML private Label  lblCardTitle;
    @FXML private Label  lblFoodInfo;
    @FXML private Label  lblDonorName;
    @FXML private Label  lblAddress;
    @FXML private Label  lblExpiry;
    @FXML private Button btnAction;
    @FXML private Button btnBack;
    @FXML private Label  lblStatus;

    private AssignmentModel currentAssignment;
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Configure mode-specific labels and colors
        switch (mode) {
            case "delivery" -> {
                lblTitle.setText("Confirm Delivery");
                lblCardTitle.setText("📦 Confirm Delivery");
                btnAction.setText("✅ Confirm Delivery");
                btnAction.setStyle("-fx-background-color:#27ae60; -fx-text-fill:white; " +
                    "-fx-font-weight:bold; -fx-font-size:15; -fx-background-radius:8; -fx-cursor:hand;");
                cardHeader.setStyle("-fx-background-color:#27ae60; -fx-background-radius:16 16 0 0; -fx-padding:16 24 16 24;");
            }
            case "pickup" -> {
                lblTitle.setText("Mark Pickup");
                lblCardTitle.setText("🚚 Confirm Pickup");
                btnAction.setText("📦 Mark Picked Up");
                btnAction.setStyle("-fx-background-color:#8e44ad; -fx-text-fill:white; " +
                    "-fx-font-weight:bold; -fx-font-size:15; -fx-background-radius:8; -fx-cursor:hand;");
                cardHeader.setStyle("-fx-background-color:#8e44ad; -fx-background-radius:16 16 0 0; -fx-padding:16 24 16 24;");
            }
            default -> { // "accept"
                lblTitle.setText("Accept Assignment");
                lblCardTitle.setText("📋 New Assignment");
                btnAction.setText("✔ Accept Assignment");
                btnAction.setStyle("-fx-background-color:#2980b9; -fx-text-fill:white; " +
                    "-fx-font-weight:bold; -fx-font-size:15; -fx-background-radius:8; -fx-cursor:hand;");
                cardHeader.setStyle("-fx-background-color:#2980b9; -fx-background-radius:16 16 0 0; -fx-padding:16 24 16 24;");
            }
        }

        loadAssignment();
    }

    private void loadAssignment() {
        List<AssignmentModel> list =
            AppBackend.getInstance().getDriverAssignments(SessionManager.getDriverId());

        // Filter based on mode — driver can only have one at a time
        switch (mode) {
            case "delivery" -> list.removeIf(a -> !"InProgress".equals(a.getStatus()));
            case "pickup"   -> list.removeIf(a -> !"Assigned".equals(a.getStatus()));
            default         -> list.removeIf(a -> !"Assigned".equals(a.getStatus())); // accept
        }

        if (list.isEmpty()) {
            // Show empty state
            noAssignmentBox.setVisible(true);
            noAssignmentBox.setManaged(true);
            assignmentCard.setVisible(false);
            assignmentCard.setManaged(false);
            currentAssignment = null;

            String emptyMsg = switch (mode) {
                case "delivery" -> "No in-progress deliveries. Pick up an order first.";
                case "pickup"   -> "No assignments awaiting pickup.";
                default         -> "No new assignments to accept.";
            };
            lblNoAssignment.setText(emptyMsg);
        } else {
            // Show the first (and ideally only) assignment as a card
            currentAssignment = list.get(0);
            noAssignmentBox.setVisible(false);
            noAssignmentBox.setManaged(false);
            assignmentCard.setVisible(true);
            assignmentCard.setManaged(true);

            lblFoodInfo.setText(currentAssignment.getFoodType() + " — " +
                currentAssignment.getQuantity() + " kg");
            lblDonorName.setText(currentAssignment.getDonorName());
            lblAddress.setText(currentAssignment.getDonationLocation());
            lblExpiry.setText(currentAssignment.getExpiryTime() != null
                ? currentAssignment.getExpiryTime().format(FMT)
                : "—");
            lblStatus.setText("");
        }
    }

    @FXML
    public void handleAction() {
        if (currentAssignment == null) {
            lblStatus.setText("No assignment available.");
            return;
        }

        switch (mode) {
            case "delivery" -> handleDelivery();
            case "pickup"   -> handlePickup();
            default         -> handleAccept();
        }
    }

    private void handleAccept() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
            "Accept this assignment?\n\n" +
            currentAssignment.getFoodType() + " — " + currentAssignment.getQuantity() + " kg\n" +
            "From: " + currentAssignment.getDonorName() + "\n" +
            "At: " + currentAssignment.getDonationLocation(),
            ButtonType.YES, ButtonType.NO);
        confirm.setTitle("Accept Assignment");
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                boolean ok = AppBackend.getInstance()
                    .acceptAssignmentByDriver(currentAssignment.getAssignmentId(),
                        SessionManager.getDriverId());
                if (ok) {
                    lblStatus.setText("✅ Assignment accepted! You can now Mark Pickup.");
                    lblStatus.setStyle("-fx-text-fill:#27ae60; -fx-font-size:13; -fx-font-weight:bold;");
                } else {
                    lblStatus.setText("❌ Accept failed.");
                    lblStatus.setStyle("-fx-text-fill:#e74c3c; -fx-font-size:13; -fx-font-weight:bold;");
                }
                loadAssignment();
            }
        });
    }

    private void handlePickup() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
            "Confirm you have collected:\n\n" +
            currentAssignment.getFoodType() + " — " + currentAssignment.getQuantity() + " kg\n" +
            "From: " + currentAssignment.getDonorName() + "\n" +
            "At: " + currentAssignment.getDonationLocation() + "?",
            ButtonType.YES, ButtonType.NO);
        confirm.setTitle("Confirm Pickup");
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                boolean ok = AppBackend.getInstance()
                    .markPickedUp(currentAssignment.getAssignmentId(),
                        currentAssignment.getDonationId());
                if (ok) {
                    lblStatus.setText("✅ Picked up! Status: InTransit. NGO notified.");
                    lblStatus.setStyle("-fx-text-fill:#27ae60; -fx-font-size:13; -fx-font-weight:bold;");
                } else {
                    lblStatus.setText("❌ Update failed.");
                    lblStatus.setStyle("-fx-text-fill:#e74c3c; -fx-font-size:13; -fx-font-weight:bold;");
                }
                loadAssignment();
            }
        });
    }

    private void handleDelivery() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
            "Confirm delivery of:\n\n" +
            currentAssignment.getFoodType() + " — " + currentAssignment.getQuantity() + " kg\n" +
            "to NGO?",
            ButtonType.YES, ButtonType.NO);
        confirm.setTitle("Confirm Delivery");
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                boolean ok = AppBackend.getInstance()
                    .confirmDelivery(currentAssignment.getAssignmentId(),
                        currentAssignment.getDonationId());
                if (ok) {
                    lblStatus.setText("✅ Delivery confirmed! NGO and donor notified.");
                    lblStatus.setStyle("-fx-text-fill:#27ae60; -fx-font-size:13; -fx-font-weight:bold;");
                } else {
                    lblStatus.setText("❌ Confirmation failed.");
                    lblStatus.setStyle("-fx-text-fill:#e74c3c; -fx-font-size:13; -fx-font-weight:bold;");
                }
                loadAssignment();
            }
        });
    }

    @FXML
    public void handleRefresh() {
        loadAssignment();
    }

    @FXML
    public void handleBack() throws IOException {
        SceneHelper.switchTo(btnBack, "driver-dashboard.fxml", 1280, 800);
    }
}

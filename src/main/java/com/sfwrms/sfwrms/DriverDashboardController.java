package com.sfwrms.sfwrms;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;


public class DriverDashboardController implements Initializable {

    @FXML private Label  lblUserName;
    @FXML private Label  lblNotifBadge;
    @FXML private Button btnLogout;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        lblUserName.setText("Welcome, " + SessionManager.getUserName());
        // Show unread notification count
        updateNotificationBadge();
    }

    private void updateNotificationBadge() {
        if (lblNotifBadge != null) {
            List<NotificationModel> notifs = AppBackend.getInstance()
                .getNotifications(SessionManager.getDriverId(), "Driver");
            long unread = notifs.stream().filter(n -> !n.isRead()).count();
            lblNotifBadge.setText(unread > 0 ? unread + " new" : "");
        }
    }

    // ── View Notifications — see pickup requests from NGOs ───────────────────
    @FXML
    public void handleViewNotifications() {
        List<NotificationModel> notifs = AppBackend.getInstance()
            .getNotifications(SessionManager.getDriverId(), "Driver");

        if (notifs.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Driver Notifications");
            alert.setHeaderText("📬 No Notifications");
            alert.setContentText("You have no notifications at this time.");
            alert.showAndWait();
            return;
        }

        StringBuilder sb = new StringBuilder();
        for (NotificationModel n : notifs) {
            String readTag = n.isRead() ? "  [Read]" : "  ★ NEW";
            sb.append("• ").append(n.getMessage())
              .append("\n  (").append(n.getSentAt() != null ?
                  n.getSentAt().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) : "")
              .append(readTag).append(")\n\n");

            // Mark as read
            if (!n.isRead()) {
                AppBackend.getInstance().markNotificationRead(n.getNotificationId());
            }
        }

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Driver Notifications");
        alert.setHeaderText("📬 Your Notifications (" + notifs.size() + ")");
        alert.setContentText(sb.toString());
        alert.getDialogPane().setPrefWidth(550);
        alert.showAndWait();

        updateNotificationBadge();
    }

    // ── Accept Assignment — driver acknowledges and accepts the assignment ─────
    @FXML
    public void handleAcceptAssignment() throws IOException {
        DriverAssignmentsController.mode = "accept";
        SceneHelper.switchTo(btnLogout, "driver-assignments.fxml", 1280, 800);
    }

    // ── UC11: Mark Picked Up — show assignments with pickup action ────────────
    @FXML
    public void handleMarkPickedUp() throws IOException {
        DriverAssignmentsController.mode = "pickup";
        SceneHelper.switchTo(btnLogout, "driver-assignments.fxml", 1280, 800);
    }

    // ── UC12: Confirm Delivery — show assignments with delivery action ────────
    @FXML
    public void handleConfirmDelivery() throws IOException {
        DriverAssignmentsController.mode = "delivery";
        SceneHelper.switchTo(btnLogout, "driver-assignments.fxml", 1280, 800);
    }

    @FXML
    public void handleLogout() throws IOException {
        SessionManager.clear();
        SceneHelper.switchTo(btnLogout, "login-view.fxml", 1280, 800);
    }
}

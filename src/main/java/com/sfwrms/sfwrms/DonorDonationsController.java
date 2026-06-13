package com.sfwrms.sfwrms;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.io.IOException;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;


public class DonorDonationsController implements Initializable {

    /** Set by DonorDashboardController before navigation. */
    public static String mode = "track";

    @FXML private Label                          lblTitle;
    @FXML private Label                          lblInstruction;
    @FXML private TableView<DonationModel>       tblDonations;
    @FXML private TableColumn<DonationModel, Integer>       colId;
    @FXML private TableColumn<DonationModel, String>        colFoodType;
    @FXML private TableColumn<DonationModel, Integer>       colQuantity;
    @FXML private TableColumn<DonationModel, String>        colStatus;
    @FXML private TableColumn<DonationModel, LocalDateTime> colExpiry;
    @FXML private TableColumn<DonationModel, String>        colLocation;
    @FXML private TableColumn<DonationModel, Double>        colUrgency;
    @FXML private Button btnAction;
    @FXML private Button btnBack;
    @FXML private Label  lblStatus;

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupColumns();

        switch (mode) {
            case "edit" -> {
                lblTitle.setText("UC02 — Modify Donation");
                lblInstruction.setText("Select a Pending donation from the table, then click 'Edit Selected' below.");
                btnAction.setText("✏ Edit Selected");
                btnAction.setStyle("-fx-background-color: #2980b9; -fx-text-fill: white; " +
                                   "-fx-font-weight: bold; -fx-background-radius: 5; -fx-cursor: hand;");
                loadPendingOnly();
            }
            case "delete" -> {
                lblTitle.setText("UC03 — Delete Donation");
                lblInstruction.setText("Select a Pending donation from the table, then click 'Delete Selected' below.");
                btnAction.setText("🗑 Delete Selected");
                btnAction.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; " +
                                   "-fx-font-weight: bold; -fx-background-radius: 5; -fx-cursor: hand;");
                loadPendingOnly();
            }
            default -> {  // "track"
                lblTitle.setText("UC04 — Track Donation Status");
                lblInstruction.setText("View the current status and urgency of all your donations.");
                btnAction.setVisible(false);
                btnAction.setManaged(false);
                loadAllDonations();
            }
        }
    }

    // ── Column setup ─────────────────────────────────────────────────────────────

    private void setupColumns() {
        colId.setCellValueFactory(new PropertyValueFactory<>("donationId"));
        colFoodType.setCellValueFactory(new PropertyValueFactory<>("foodType"));
        colQuantity.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        colLocation.setCellValueFactory(new PropertyValueFactory<>("location"));
        colUrgency.setCellValueFactory(new PropertyValueFactory<>("urgencyScore"));

        // Status column with colour coding
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); return; }
                setText(item);
                switch (item) {
                    case "Pending"   -> setStyle("-fx-text-fill: #e67e22; -fx-font-weight: bold;");
                    case "Assigned"  -> setStyle("-fx-text-fill: #2980b9; -fx-font-weight: bold;");
                    case "InTransit" -> setStyle("-fx-text-fill: #8e44ad; -fx-font-weight: bold;");
                    case "Delivered" -> setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");
                    default          -> setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
                }
            }
        });

        // Formatted expiry datetime
        colExpiry.setCellValueFactory(new PropertyValueFactory<>("expiryTime"));
        colExpiry.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(LocalDateTime item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.format(FMT));
            }
        });

        // Urgency colour coding — show "—" for Delivered donations
        colUrgency.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Double v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || v == null) { setText(null); setStyle(""); return; }
                // Check if the donation is Delivered — urgency is irrelevant
                DonationModel row = getTableView().getItems().get(getIndex());
                if ("Delivered".equals(row.getStatus())) {
                    setText("—");
                    setStyle("-fx-text-fill:#27ae60; -fx-font-weight:bold;");
                    return;
                }
                setText(String.format("%.1f", v));
                if (v >= 70)      setStyle("-fx-text-fill:#e74c3c; -fx-font-weight:bold;");
                else if (v >= 40) setStyle("-fx-text-fill:#e67e22; -fx-font-weight:bold;");
                else              setStyle("-fx-text-fill:#27ae60;");
            }
        });
    }

    // ── Data loading ─────────────────────────────────────────────────────────────

    private void loadAllDonations() {
        List<DonationModel> list =
            AppBackend.getInstance().getDonationsByDonor(SessionManager.getUserId());
        tblDonations.setItems(FXCollections.observableArrayList(list));
        lblStatus.setText("Total donations: " + list.size());
    }

    private void loadPendingOnly() {
        List<DonationModel> list =
            AppBackend.getInstance().getDonationsByDonor(SessionManager.getUserId());
        list.removeIf(d -> !"Pending".equals(d.getStatus()));
        tblDonations.setItems(FXCollections.observableArrayList(list));
        lblStatus.setText("Pending donations: " + list.size());
    }

    // ── Action handler (mode-dependent) ──────────────────────────────────────────

    @FXML
    public void handleAction() throws IOException {
        DonationModel selected = tblDonations.getSelectionModel().getSelectedItem();
        if (selected == null) {
            lblStatus.setText("⚠ Please select a donation from the table first.");
            return;
        }

        switch (mode) {
            case "edit" -> {
                AddEditDonationController.currentDonation = selected;
                SceneHelper.switchTo(btnBack, "add-edit-donation.fxml", 1280, 800);
            }
            case "delete" -> {
                Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                    "Delete donation #" + selected.getDonationId() + " — " + selected.getFoodType() + "?",
                    ButtonType.YES, ButtonType.NO);
                confirm.setTitle("Confirm Delete");
                confirm.showAndWait().ifPresent(btn -> {
                    if (btn == ButtonType.YES) {
                        boolean ok = AppBackend.getInstance().deleteDonation(selected.getDonationId());
                        lblStatus.setText(ok ? "✅ Donation deleted successfully."
                                             : "❌ Delete failed — status may have changed.");
                        loadPendingOnly();
                    }
                });
            }
        }
    }

    @FXML
    public void handleRefresh() {
        if ("track".equals(mode)) loadAllDonations();
        else loadPendingOnly();
        lblStatus.setText("Refreshed.");
    }

    @FXML
    public void handleBack() throws IOException {
        SceneHelper.switchTo(btnBack, "donor-dashboard.fxml", 1280, 800);
    }
}

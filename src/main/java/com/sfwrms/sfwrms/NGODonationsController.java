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
import java.util.*;


public class NGODonationsController implements Initializable {

    @FXML private TextField                        tfLocationFilter;
    @FXML private TextField                        tfFoodTypeFilter;
    @FXML private ComboBox<String>                 cbSort;
    @FXML private TableView<DonationModel>         tblDonations;
    @FXML private TableColumn<DonationModel, Integer>       colId;
    @FXML private TableColumn<DonationModel, String>        colFoodType;
    @FXML private TableColumn<DonationModel, Integer>       colQty;
    @FXML private TableColumn<DonationModel, String>        colLocation;
    @FXML private TableColumn<DonationModel, LocalDateTime> colExpiry;
    @FXML private TableColumn<DonationModel, LocalDateTime> colPrepTime;
    @FXML private TableColumn<DonationModel, Double>        colUrgency;
    @FXML private TableColumn<DonationModel, String>        colDecision;
    @FXML private Label  lblStatus;
    @FXML private Button btnBack;
    @FXML private Button btnAccept;
    @FXML private Button btnReject;

    /** Tracks rejected donation IDs for this session + loaded from DB. */
    private final Set<Integer> rejectedIds = new HashSet<>();
    /** Tracks accepted donation IDs for this session. */
    private final Set<Integer> acceptedIds = new HashSet<>();

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        cbSort.getItems().addAll("Urgency (Most Urgent First)", "Expiry (Soonest First)");
        cbSort.setValue("Urgency (Most Urgent First)");

        colId.setCellValueFactory(new PropertyValueFactory<>("donationId"));
        colFoodType.setCellValueFactory(new PropertyValueFactory<>("foodType"));
        colQty.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        colLocation.setCellValueFactory(new PropertyValueFactory<>("location"));
        colUrgency.setCellValueFactory(new PropertyValueFactory<>("urgencyScore"));

        colExpiry.setCellValueFactory(new PropertyValueFactory<>("expiryTime"));
        colExpiry.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(LocalDateTime v, boolean empty) {
                super.updateItem(v, empty);
                setText(empty || v == null ? null : v.format(FMT));
            }
        });
        colPrepTime.setCellValueFactory(new PropertyValueFactory<>("preparationTime"));
        colPrepTime.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(LocalDateTime v, boolean empty) {
                super.updateItem(v, empty);
                setText(empty || v == null ? null : v.format(FMT));
            }
        });

        // Colour urgency
        colUrgency.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Double v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || v == null) { setText(null); setStyle(""); return; }
                setText(String.format("%.1f", v));
                if (v >= 70) setStyle("-fx-text-fill:#e74c3c; -fx-font-weight:bold;");
                else if (v >= 40) setStyle("-fx-text-fill:#e67e22; -fx-font-weight:bold;");
                else setStyle("-fx-text-fill:#27ae60;");
            }
        });

        // Decision column — shows Rejected in red, Accepted in green
        colDecision.setCellValueFactory(new PropertyValueFactory<>("status"));
        colDecision.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String v, boolean empty) {
                super.updateItem(v, empty);
                if (empty) { setText(null); setStyle(""); return; }
                DonationModel row = getTableView().getItems().get(getIndex());
                int id = row.getDonationId();
                if (rejectedIds.contains(id)) {
                    setText("Rejected");
                    setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
                } else if (acceptedIds.contains(id) || "Assigned".equals(row.getStatus())) {
                    setText("Accepted");
                    setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");
                } else {
                    setText("");
                    setStyle("");
                }
            }
        });

        // Double-click for drill-down
        tblDonations.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) showDetails();
        });

        // Load rejected IDs from DB for this NGO
        loadRejectedFromDB();
        loadDonations();
    }

    /** Loads donation IDs that this NGO previously rejected (cancelled assignments). */
    private void loadRejectedFromDB() {
        List<AssignmentModel> assignments =
            AppBackend.getInstance().getAssignmentsForNGO(SessionManager.getNgoId());
        // Also check cancelled ones
        List<AssignmentModel> cancelled =
            AppBackend.getInstance().getCancelledAssignmentsForNGO(SessionManager.getNgoId());
        for (AssignmentModel a : cancelled) {
            rejectedIds.add(a.getDonationId());
        }
    }

    @FXML public void handleFilter() { loadDonations(); }

    @FXML
    public void handleClearFilter() {
        tfLocationFilter.clear();
        tfFoodTypeFilter.clear();
        cbSort.setValue("Urgency (Most Urgent First)");
        loadDonations();
    }

    @FXML public void handleViewDetails() { showDetails(); }

    /** NGO accepts a pending donation. */
    @FXML
    public void handleAccept() {
        DonationModel sel = tblDonations.getSelectionModel().getSelectedItem();
        if (sel == null) { lblStatus.setText("Select a donation first."); return; }
        if (rejectedIds.contains(sel.getDonationId())) {
            lblStatus.setText("You already rejected this donation."); return;
        }
        if (!"Pending".equals(sel.getStatus())) {
            lblStatus.setText("Only Pending donations can be accepted."); return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
            "Accept donation #" + sel.getDonationId() + " — " + sel.getFoodType() +
            " (" + sel.getQuantity() + " kg)?",
            ButtonType.YES, ButtonType.NO);
        confirm.setTitle("Accept Donation");
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                boolean ok = AppBackend.getInstance().acceptDonation(
                    sel.getDonationId(), SessionManager.getRepId(), SessionManager.getNgoId());
                if (ok) {
                    acceptedIds.add(sel.getDonationId());
                    lblStatus.setText("Donation accepted! Now assign a driver from Assign Driver.");
                } else {
                    lblStatus.setText("Accept failed — may have been taken.");
                }
                loadDonations();
            }
        });
    }

    /** NGO rejects a donation — shows Rejected in red in Decision column. */
    @FXML
    public void handleReject() {
        DonationModel sel = tblDonations.getSelectionModel().getSelectedItem();
        if (sel == null) { lblStatus.setText("Select a donation first."); return; }
        if (!"Pending".equals(sel.getStatus())) {
            lblStatus.setText("Only Pending donations can be rejected."); return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
            "Reject donation #" + sel.getDonationId() + " — " + sel.getFoodType() + "?",
            ButtonType.YES, ButtonType.NO);
        confirm.setTitle("Reject Donation");
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                boolean ok = AppBackend.getInstance().rejectDonation(sel.getDonationId());
                if (ok) {
                    rejectedIds.add(sel.getDonationId());
                    lblStatus.setText("Donation rejected.");
                } else {
                    lblStatus.setText("Reject failed.");
                }
                tblDonations.refresh(); // refresh to show Rejected in red
            }
        });
    }

    private void loadDonations() {
        String loc   = tfLocationFilter.getText().trim();
        String food  = tfFoodTypeFilter.getText().trim();
        String sort  = cbSort.getValue() != null && cbSort.getValue().startsWith("Expiry")
                       ? "expiry" : "urgency";
        List<DonationModel> list =
            AppBackend.getInstance().getPendingDonationsFiltered(loc, food, sort);
        tblDonations.setItems(FXCollections.observableArrayList(list));
        lblStatus.setText(list.isEmpty() ? "No donations available."
                                         : list.size() + " donation(s) available.");
    }

    private void showDetails() {
        DonationModel sel = tblDonations.getSelectionModel().getSelectedItem();
        if (sel == null) { lblStatus.setText("Select a row first."); return; }
        DonationModel d = AppBackend.getInstance().getDonationDetails(sel.getDonationId());
        if (d == null) return;
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Donation #" + d.getDonationId());
        alert.setHeaderText(d.getFoodType() + "  |  " + d.getQuantity() + " kg");
        alert.setContentText(
            "Location : " + d.getLocation() + "\n" +
            "Status   : " + d.getStatus() + "\n" +
            "Prepared : " + (d.getPreparationTime() != null ? d.getPreparationTime().format(FMT) : "—") + "\n" +
            "Expires  : " + (d.getExpiryTime() != null ? d.getExpiryTime().format(FMT) : "—") + "\n" +
            "Urgency  : " + String.format("%.1f", d.getUrgencyScore())
        );
        alert.showAndWait();
    }

    @FXML
    public void handleBack() throws IOException {
        SceneHelper.switchTo(btnBack, "ngo-dashboard.fxml", 1280, 800);
    }
}

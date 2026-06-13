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


public class NGOAssignmentsController implements Initializable {

    @FXML private TableView<AssignmentModel>           tblAssignments;
    @FXML private TableColumn<AssignmentModel, Integer>       colId;
    @FXML private TableColumn<AssignmentModel, String>        colFoodType;
    @FXML private TableColumn<AssignmentModel, Integer>       colQty;
    @FXML private TableColumn<AssignmentModel, String>        colLocation;
    @FXML private TableColumn<AssignmentModel, LocalDateTime> colExpiry;
    @FXML private TableColumn<AssignmentModel, String>        colDonor;
    @FXML private TableColumn<AssignmentModel, String>        colDriver;
    @FXML private TableColumn<AssignmentModel, String>        colStatus;
    @FXML private TableColumn<AssignmentModel, String>        colDelivery;
    @FXML private Label  lblStatus;
    @FXML private Button btnBack;

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        colId.setCellValueFactory(new PropertyValueFactory<>("assignmentId"));
        colFoodType.setCellValueFactory(new PropertyValueFactory<>("foodType"));
        colQty.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        colLocation.setCellValueFactory(new PropertyValueFactory<>("donationLocation"));
        colDonor.setCellValueFactory(new PropertyValueFactory<>("donorName"));
        colDriver.setCellValueFactory(new PropertyValueFactory<>("driverName"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colDelivery.setCellValueFactory(new PropertyValueFactory<>("deliveryStatus"));

        colExpiry.setCellValueFactory(new PropertyValueFactory<>("expiryTime"));
        colExpiry.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(LocalDateTime v, boolean empty) {
                super.updateItem(v, empty);
                setText(empty || v == null ? null : v.format(FMT));
            }
        });

        // Color status column
        // Status column — show human-readable progression
        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || v == null) { setText(null); setStyle(""); return; }
                AssignmentModel row = getTableView().getItems().get(getIndex());
                String display;
                String color;
                switch (v) {
                    case "Assigned" -> {
                        if ("Not Assigned".equals(row.getDriverName()) || row.getDriverId() == 0) {
                            display = "Pending"; color = "#e67e22";
                        } else {
                            display = "Driver Assigned"; color = "#2980b9";
                        }
                    }
                    case "InProgress" -> { display = "Picked Up"; color = "#8e44ad"; }
                    case "Completed"  -> { display = "Delivered"; color = "#27ae60"; }
                    default           -> { display = v; color = "#e74c3c"; }
                }
                setText(display);
                setStyle("-fx-text-fill:" + color + "; -fx-font-weight:bold;");
            }
        });

        // Color driver column
        colDriver.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || v == null) { setText(null); setStyle(""); return; }
                setText(v);
                if ("Not Assigned".equals(v)) {
                    setStyle("-fx-text-fill:#e74c3c; -fx-font-weight:bold;");
                } else {
                    setStyle("-fx-text-fill:#27ae60; -fx-font-weight:bold;");
                }
            }
        });

        loadAssignments();
    }

    private void loadAssignments() {
        List<AssignmentModel> list =
            AppBackend.getInstance().getAssignmentsForNGO(SessionManager.getNgoId());
        tblAssignments.setItems(FXCollections.observableArrayList(list));
        lblStatus.setText(list.isEmpty() ? "No active assignments." : list.size() + " assignment(s).");
    }

    @FXML public void handleRefresh() { loadAssignments(); lblStatus.setText("Refreshed."); }

    @FXML
    public void handleBack() throws IOException {
        SceneHelper.switchTo(btnBack, "ngo-dashboard.fxml", 1280, 800);
    }
}

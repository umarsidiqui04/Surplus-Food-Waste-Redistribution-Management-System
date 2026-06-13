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


public class NGOAssignDriverController implements Initializable {

    /** Set by the previous screen (NGOAssignmentsController). */
    public static AssignmentModel selectedAssignment = null;

    @FXML private Label              lblAssignmentInfo;

    // ── Assignments table ────────────────────────────────────────────────────
    @FXML private TableView<AssignmentModel> tblAssignments;
    @FXML private TableColumn<AssignmentModel, Integer>       colId;
    @FXML private TableColumn<AssignmentModel, String>        colFoodType;
    @FXML private TableColumn<AssignmentModel, Integer>       colQty;
    @FXML private TableColumn<AssignmentModel, String>        colLocation;
    @FXML private TableColumn<AssignmentModel, LocalDateTime> colExpiry;
    @FXML private TableColumn<AssignmentModel, String>        colCurrentDriver;

    // ── Available drivers table ──────────────────────────────────────────────
    @FXML private TableView<DriverInfo> tblDrivers;
    @FXML private TableColumn<DriverInfo, Integer> colDriverId;
    @FXML private TableColumn<DriverInfo, String>  colDriverName;
    @FXML private TableColumn<DriverInfo, String>  colVehicle;
    @FXML private TableColumn<DriverInfo, String>  colPhone;

    @FXML private Label                      lblStatus;
    @FXML private Button                     btnAssign;
    @FXML private Button                     btnBack;

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Setup assignments table
        colId.setCellValueFactory(new PropertyValueFactory<>("assignmentId"));
        colFoodType.setCellValueFactory(new PropertyValueFactory<>("foodType"));
        colQty.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        colLocation.setCellValueFactory(new PropertyValueFactory<>("donationLocation"));
        colCurrentDriver.setCellValueFactory(new PropertyValueFactory<>("driverName"));

        colExpiry.setCellValueFactory(new PropertyValueFactory<>("expiryTime"));
        colExpiry.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(LocalDateTime v, boolean empty) {
                super.updateItem(v, empty);
                setText(empty || v == null ? null : v.format(FMT));
            }
        });

        // Setup drivers table
        colDriverId.setCellValueFactory(new PropertyValueFactory<>("driverId"));
        colDriverName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colVehicle.setCellValueFactory(new PropertyValueFactory<>("vehicleInfo"));
        colPhone.setCellValueFactory(new PropertyValueFactory<>("phone"));

        loadAssignments();
        loadDrivers();

        // If a specific assignment was passed from UC09, pre-select it
        if (selectedAssignment != null) {
            lblAssignmentInfo.setText("Assigning driver for: " +
                selectedAssignment.getFoodType() + " — " +
                selectedAssignment.getQuantity() + " kg  (#" + selectedAssignment.getAssignmentId() + ")");
            // Pre-select in table
            tblAssignments.getItems().stream()
                .filter(a -> a.getAssignmentId() == selectedAssignment.getAssignmentId())
                .findFirst()
                .ifPresent(a -> tblAssignments.getSelectionModel().select(a));
        }
    }

    private void loadAssignments() {
        List<AssignmentModel> list =
            AppBackend.getInstance().getAssignmentsForNGO(SessionManager.getNgoId());
        // Only show Assigned (not yet picked up)
        list.removeIf(a -> !"Assigned".equals(a.getStatus()));
        tblAssignments.setItems(FXCollections.observableArrayList(list));
        lblStatus.setText(list.isEmpty() ? "No assignments to process." : list.size() + " assignment(s) need a driver.");
    }

    private void loadDrivers() {
        List<DriverInfo> drivers =
            AppBackend.getInstance().getAvailableDrivers(SessionManager.getNgoId());
        tblDrivers.setItems(FXCollections.observableArrayList(drivers));
        if (drivers.isEmpty()) {
            lblStatus.setText("No available drivers. All are on active assignments.");
        }
    }

    @FXML
    public void handleAssignDriver() {
        AssignmentModel sel = tblAssignments.getSelectionModel().getSelectedItem();
        DriverInfo      drv = tblDrivers.getSelectionModel().getSelectedItem();

        if (sel == null) { lblStatus.setText("⚠ Select an assignment from the top table."); return; }
        if (drv == null) { lblStatus.setText("⚠ Select a driver from the bottom table."); return; }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
            "Assign driver '" + drv.getName() + "' (" + drv.getVehicleInfo() + ")\n" +
            "to pickup: " + sel.getFoodType() + " — " + sel.getQuantity() + " kg?",
            ButtonType.YES, ButtonType.NO);
        confirm.setTitle("Confirm Driver Assignment");
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                boolean ok = AppBackend.getInstance().assignDriver(sel.getAssignmentId(), drv.getDriverId());
                if (ok) {
                    lblStatus.setText("✅ Driver '" + drv.getName() + "' assigned to assignment #" +
                                      sel.getAssignmentId() + ". Driver has been notified.");
                    selectedAssignment = null;
                    loadAssignments();
                    loadDrivers();
                } else {
                    lblStatus.setText("❌ Assignment failed — assignment may no longer be in 'Assigned' status.");
                }
            }
        });
    }

    @FXML public void handleRefresh() { loadAssignments(); loadDrivers(); }

    @FXML
    public void handleBack() throws IOException {
        selectedAssignment = null;
        SceneHelper.switchTo(btnBack, "ngo-dashboard.fxml", 1280, 800);
    }
}

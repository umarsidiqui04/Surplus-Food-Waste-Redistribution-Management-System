package com.sfwrms.sfwrms;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;

import java.io.IOException;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ResourceBundle;


public class AddEditDonationController implements Initializable {

    public static DonationModel currentDonation = null;

    @FXML private Label       lblTitle;
    @FXML private TextField   tfFoodType;
    @FXML private TextField   tfQuantity;
    @FXML private ComboBox<String> cbUnit;
    @FXML private DatePicker  dpPrepDate;
    @FXML private TextField   tfPrepTime;
    @FXML private DatePicker  dpExpiryDate;
    @FXML private TextField   tfExpiryTime;
    @FXML private TextField   tfLocation;
    @FXML private Label       lblError;
    @FXML private Button      btnSubmit;
    @FXML private Button      btnCancel;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Initialize unit dropdown
        cbUnit.getItems().addAll("kg", "pieces");
        cbUnit.setValue("kg");

        if (currentDonation != null) {
            // ── Edit mode — pre-fill with existing values ──────────────────────
            lblTitle.setText("UC02 — Edit Donation");
            btnSubmit.setText("Save Changes");
            btnCancel.setText("← Cancel");
            tfFoodType.setText(currentDonation.getFoodType());
            tfQuantity.setText(String.valueOf(currentDonation.getQuantity()));
            tfLocation.setText(currentDonation.getLocation());
            // Restore unit from quantityUnit field if available
            if (currentDonation.getQuantityUnit() != null && !currentDonation.getQuantityUnit().isEmpty()) {
                cbUnit.setValue(currentDonation.getQuantityUnit());
            }
            if (currentDonation.getPreparationTime() != null) {
                dpPrepDate.setValue(currentDonation.getPreparationTime().toLocalDate());
                tfPrepTime.setText(currentDonation.getPreparationTime()
                                       .format(DateTimeFormatter.ofPattern("HH:mm")));
            }
            if (currentDonation.getExpiryTime() != null) {
                dpExpiryDate.setValue(currentDonation.getExpiryTime().toLocalDate());
                tfExpiryTime.setText(currentDonation.getExpiryTime()
                                         .format(DateTimeFormatter.ofPattern("HH:mm")));
            }
        } else {
            // ── Add mode — sensible defaults ───────────────────────────────────
            lblTitle.setText("UC01 — Log Surplus Food");
            btnSubmit.setText("Submit Donation");
            dpPrepDate.setValue(LocalDate.now());
            tfPrepTime.setText("09:00");
            dpExpiryDate.setValue(LocalDate.now().plusDays(1));
            tfExpiryTime.setText("18:00");
        }
    }

    @FXML
    public void handleSubmit() throws IOException {
        lblError.setText("");

        // ── Input validation ───────────────────────────────────────────────────
        String foodType = tfFoodType.getText().trim();
        String qtyStr   = tfQuantity.getText().trim();
        String unit     = cbUnit.getValue();
        String location = tfLocation.getText().trim();

        if (foodType.isEmpty() || qtyStr.isEmpty() || location.isEmpty() || unit == null) {
            lblError.setText("All fields are required."); return;
        }

        int quantity;
        try {
            quantity = Integer.parseInt(qtyStr);
            if (quantity <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            lblError.setText("Quantity must be a positive whole number."); return;
        }

        LocalDateTime prepTime   = parseDateTime(dpPrepDate.getValue(), tfPrepTime.getText().trim());
        LocalDateTime expiryTime = parseDateTime(dpExpiryDate.getValue(), tfExpiryTime.getText().trim());

        if (prepTime == null || expiryTime == null) {
            lblError.setText("Enter times in HH:mm format (e.g. 14:30)."); return;
        }
        if (!expiryTime.isAfter(LocalDateTime.now())) {
            lblError.setText("Expiry time must be in the future."); return;
        }
        if (!expiryTime.isAfter(prepTime)) {
            lblError.setText("Expiry time must be after preparation time."); return;
        }

        // ── Delegate to AppBackend (Facade) ────────────────────────────────────
        boolean ok;
        if (currentDonation == null) {
            // UC01 — Create
            System.out.println("[UC01] Creating donation: donor=" + SessionManager.getUserId() +
                ", food=" + foodType + ", qty=" + quantity + ", expiry=" + expiryTime);
            ok = AppBackend.getInstance().createDonation(
                  SessionManager.getUserId(), foodType, quantity, unit, prepTime, expiryTime, location);
            if (ok) System.out.println("[UC01] Donation created successfully.");
            else    System.err.println("[UC01] Donation creation FAILED.");
        } else {
            // UC02 — Update
            System.out.println("[UC02] Updating donation #" + currentDonation.getDonationId());
            ok = AppBackend.getInstance().updateDonation(
                    currentDonation.getDonationId(), foodType, quantity, unit,
                    prepTime, expiryTime, location);
        }

        if (ok) {
            boolean wasAdd = (currentDonation == null);
            currentDonation = null;
            if (wasAdd) {
                // After adding, go back to hub
                SceneHelper.switchTo(btnCancel, "donor-dashboard.fxml", 1280, 800);
            } else {
                // After editing, go back to donations list
                DonorDonationsController.mode = "edit";
                SceneHelper.switchTo(btnCancel, "donor-donations.fxml", 1280, 800);
            }
        } else {
            lblError.setText("Save failed. Check: (1) DB connection, (2) expiry is future, " +
                             "(3) donation is still Pending. See console for details.");
        }
    }

    @FXML
    public void handleCancel() throws IOException {
        currentDonation = null;
        SceneHelper.switchTo(btnCancel, "donor-dashboard.fxml", 1280, 800);
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private LocalDateTime parseDateTime(LocalDate date, String timeStr) {
        if (date == null || timeStr == null || timeStr.isEmpty()) return null;
        try {
            LocalTime time = LocalTime.parse(timeStr, DateTimeFormatter.ofPattern("HH:mm"));
            return LocalDateTime.of(date, time);
        } catch (DateTimeParseException e) {
            return null;
        }
    }
}

package com.sfwrms.sfwrms;

import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.io.IOException;

public class LoginController {

    @FXML private RadioButton   rbDonor;
    @FXML private RadioButton   rbNGORep;
    @FXML private RadioButton   rbDriver;
    @FXML private TextField     tfEmail;
    @FXML private PasswordField pfPassword;
    @FXML private Label         lblEmail;
    @FXML private Label         lblError;
    @FXML private Button        btnLogin;

    @FXML
    public void handleRoleChange() {
        // Drivers log in with phone, others with email
        if (rbDriver.isSelected()) {
            lblEmail.setText("Phone Number");
            tfEmail.setPromptText("e.g. 0333-1112222");
        } else {
            lblEmail.setText("Email Address");
            tfEmail.setPromptText("you@example.com");
        }
        lblError.setText("");
    }

    @FXML
    public void handleLogin() {
        String emailOrPhone = tfEmail.getText().trim();
        String password     = pfPassword.getText();

        if (emailOrPhone.isEmpty() || password.isEmpty()) {
            lblError.setText("Please fill in all fields.");
            return;
        }

        String role = rbDonor.isSelected() ? "Donor"
                    : rbNGORep.isSelected() ? "NGORep"
                    : "Driver";

        User user = AppBackend.getInstance().login(emailOrPhone, password, role);

        if (user == null) {
            lblError.setText("Invalid credentials. Check your " +
                (role.equals("Driver") ? "phone number" : "email") + ", password, and role.");
            return;
        }

        SessionManager.setCurrentUser(user);

        try {
            switch (role) {
                case "Donor"  -> SceneHelper.switchTo(btnLogin, "donor-dashboard.fxml",  1280, 800);
                case "NGORep" -> SceneHelper.switchTo(btnLogin, "ngo-dashboard.fxml",    1280, 800);
                case "Driver" -> SceneHelper.switchTo(btnLogin, "driver-dashboard.fxml", 1280, 800);
            }
        } catch (IOException e) {
            lblError.setText("Failed to load dashboard.");
            e.printStackTrace();
        }
    }
}

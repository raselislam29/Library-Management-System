package com.lms.lmsfinal;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;

public class UserProfileContentController {

    @FXML private Label lblEmail;
    @FXML private Label lblRole;
    @FXML private Label lblJoined;

    @FXML private TextField firstNameField;
    @FXML private TextField lastNameField;
    @FXML private TextField displayNameField;

    @FXML private Label statusLabel;

    private final FirebaseService firebase = new FirebaseService();
    private final DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("MMM dd, yyyy");

    @FXML
    private void initialize() {
        loadProfile();
    }

    // --------------------------------------------------
    // Load profile from Firebase
    // --------------------------------------------------
    private void loadProfile() {
        String email = (Session.email == null || Session.email.isBlank())
                ? "user@example.com"
                : Session.email;

        lblEmail.setText(email);

        FirebaseService.UserProfile profile = firebase.getUserProfile(email);

        if (profile == null) {
            lblRole.setText("Member");
            lblJoined.setText("-");
            firstNameField.setText("");
            lastNameField.setText("");
            displayNameField.setText("");
            statusLabel.setText("Could not load profile details (using defaults).");
            return;
        }

        lblRole.setText(profile.getRole() == null ? "Member" : profile.getRole());

        Date joinDate = profile.getJoinDate();
        if (joinDate != null) {
            LocalDate ld = joinDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            lblJoined.setText(ld.format(dateFmt));
        } else {
            lblJoined.setText("-");
        }

        firstNameField.setText(nullToEmpty(profile.getFirstName()));
        lastNameField.setText(nullToEmpty(profile.getLastName()));
        displayNameField.setText(nullToEmpty(profile.getDisplayName()));

        statusLabel.setText("Profile loaded.");
    }

    private String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    // --------------------------------------------------
    // Save profile changes
    // --------------------------------------------------
    @FXML
    private void onSaveProfile() {
        String email = lblEmail.getText();
        if (email == null || email.isBlank()) {
            statusLabel.setText("No email found for this account.");
            return;
        }

        String first = firstNameField.getText() == null ? "" : firstNameField.getText().trim();
        String last  = lastNameField.getText() == null ? "" : lastNameField.getText().trim();
        String disp  = displayNameField.getText() == null ? "" : displayNameField.getText().trim();

        try {
            // Send empty strings as null so we don't store "" in Firestore
            firebase.updateUserProfileNames(
                    email,
                    first.isBlank() ? null : first,
                    last.isBlank() ? null : last,
                    disp.isBlank() ? null : disp
            );

            statusLabel.setText("Profile updated successfully.");
            showInfo("Profile updated.", "Your name settings have been saved.");
        } catch (Exception e) {
            e.printStackTrace();
            statusLabel.setText("Error updating profile: " + e.getMessage());
            showError("Error updating profile", e.getMessage());
        }
    }

    // --------------------------------------------------
    // Reset / reload from server
    // --------------------------------------------------
    @FXML
    private void onResetProfile() {
        loadProfile();
        statusLabel.setText("Profile reset to saved values.");
    }

    // --------------------------------------------------
    // Change password (send reset email)
    // --------------------------------------------------
    @FXML
    private void onChangePassword() {
        String email = lblEmail.getText();
        if (email == null || email.isBlank()) {
            statusLabel.setText("No email found to send reset link.");
            return;
        }

        FirebaseService.PasswordResetResult res = firebase.sendPasswordResetEmail(email);
        if (res.ok) {
            statusLabel.setText("Password reset email sent to " + email + ".");
            showInfo("Password reset email sent",
                    "Check your inbox for a reset link.\nIf you don't see it, check your spam folder.");
        } else {
            String msg = res.errorText == null ? "Unknown error" : res.errorText;
            statusLabel.setText("Failed to send reset email: " + msg);
            showError("Password reset failed", msg);
        }
    }

    // --------------------------------------------------
    // Small helpers for alerts
    // --------------------------------------------------
    private void showError(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.show();
    }

    private void showInfo(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.show();
    }
}

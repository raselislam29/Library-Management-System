package com.lms.lmsfinal;

import com.lms.lmsfinal.FirebaseService.UserSummary;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;

public class AdminUsersController {

    @FXML private TableView<UserSummary> usersTable;
    @FXML private TableColumn<UserSummary, String> colEmail;
    @FXML private TableColumn<UserSummary, String> colRole;

    @FXML private TextField tfEmail;
    @FXML private ChoiceBox<String> cbRole;
    @FXML private Label statusLabel;

    private final FirebaseService firebase = new FirebaseService();
    private final ObservableList<UserSummary> users = FXCollections.observableArrayList();

    @FXML
    private void initialize() {
        // Table columns
        colEmail.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("email"));
        colRole.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("role"));
        usersTable.setItems(users);

        // Roles
        if (cbRole != null) {
            cbRole.setItems(FXCollections.observableArrayList("MEMBER", "LIBRARIAN", "ADMIN"));
        }

        // Load data
        refresh();

        // Selection listener -> populate edit fields
        usersTable.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> {
            if (n != null) {
                tfEmail.setText(n.getEmail());
                if (cbRole != null) cbRole.setValue(n.getRole());
            }
        });
    }

    @FXML
    private void onSetRole() {
        try {
            String email = tfEmail.getText() == null ? "" : tfEmail.getText().trim();
            String role = (cbRole == null || cbRole.getValue() == null) ? "" : cbRole.getValue().trim();

            if (email.isBlank() || role.isBlank()) {
                status("Select a user and a role.");
                return;
            }

            firebase.setUserRoleByEmail(email, role);
            status("Role updated: " + email + " → " + role);
            refresh();

        } catch (Exception e) {
            status("Error: " + e.getMessage());
        }
    }

    @FXML private void onRefresh() { refresh(); }

    @FXML
    private void onBack() {
        // Go back to Admin Dashboard (change the FXML path if yours is different)
        LibraryApp.setScene("/com/lms/lmsfinal/AdminDashboardView.fxml", "Admin Dashboard", 1200, 760);
    }

    private void refresh() {
        users.setAll(firebase.getAllUsers());
    }

    private void status(String s) {
        if (statusLabel != null) statusLabel.setText(s);
    }
}

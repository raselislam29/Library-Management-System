package com.lms.lmsfinal;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;

public class AdminShellController {

    @FXML private Label welcomeLabel;
    @FXML private StackPane contentHost;
    @FXML private BorderPane root;

    @FXML
    private void initialize() {
        String email = (Session.email == null || Session.email.isBlank())
                ? "admin@example.com"
                : Session.email;

        welcomeLabel.setText("Welcome, " + email);

        // Apply current theme to this root
        ThemeManager.getInstance().applyThemeToRoot(root);

        // Default view
        showHome();
    }

    @FXML
    private void onLogout() {
        Session.clear();
        LibraryApp.setScene("/com/lms/lmsfinal/LoginView.fxml",
                "Login / Register", 1200, 760);
    }

    // ---------- Navigation targets ----------

    @FXML
    public void showHome() {
        swapCenter("/com/lms/lmsfinal/AdminHomeContent.fxml");
    }

    @FXML
    public void showBooks() {
        swapCenter("/com/lms/lmsfinal/AdminBooksContent.fxml");
    }

    @FXML
    public void showUsers() {
        swapCenter("/com/lms/lmsfinal/AdminUsersContent.fxml");
    }

    @FXML
    public void showBorrowers() {
        swapCenter("/com/lms/lmsfinal/AdminBorrowersContent.fxml");
    }

    // ⭐ NEW: Settings view (theme toggle etc.)
    @FXML
    public void showSettings() {
        swapCenter("/com/lms/lmsfinal/AdminSettingsContent.fxml");
    }

    // ---------- Helper to swap center content ----------

    private void swapCenter(String fxmlPath) {
        try {
            Node view = FXMLLoader.load(getClass().getResource(fxmlPath));

            // Apply current theme to newly loaded view
            ThemeManager.getInstance().applyThemeToNode(view);

            contentHost.getChildren().setAll(view);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

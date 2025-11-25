package com.lms.lmsfinal;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class UserShellController {

    @FXML private Label welcomeLabel;
    @FXML private Label clockLabel;
    @FXML private StackPane contentHost;
    @FXML private BorderPane root;   // MUST match fx:id="root" in UserShellView.fxml

    private final DateTimeFormatter fmt =
            DateTimeFormatter.ofPattern("hh:mm a • MMM dd, yyyy");

    @FXML
    private void initialize() {

        // Apply current theme to root when shell is created
        ThemeManager.getInstance().applyThemeToRoot(root);

        // Show user welcome
        String email = (Session.email == null || Session.email.isBlank())
                ? "user@example.com" : Session.email;
        welcomeLabel.setText("Welcome, " + email);

        // Live clock
        Timeline t = new Timeline(new KeyFrame(Duration.seconds(1), e ->
                clockLabel.setText(LocalDateTime.now().format(fmt))
        ));
        t.setCycleCount(Timeline.INDEFINITE);
        t.play();

        showHome();
    }

    @FXML
    private void onLogout() {
        Session.clear();
        LibraryApp.setScene("/com/lms/lmsfinal/LoginView.fxml",
                "Login / Register", 1200, 760);
    }

    @FXML public void showHome()    { swap("/com/lms/lmsfinal/UserHomeContent.fxml"); }
    @FXML public void showBrowse()  { swap("/com/lms/lmsfinal/UserBrowseContent.fxml"); }
    @FXML public void showBorrows() { swap("/com/lms/lmsfinal/UserBorrowsContent.fxml"); }

    // ⭐ Used by left nav + Settings button on top bar
    @FXML public void showProfile() {
        swap("/com/lms/lmsfinal/UserProfileContent.fxml");
    }

    private void swap(String fxml) {
        try {
            Parent view = FXMLLoader.load(getClass().getResource(fxml));

            // Make sure current theme is applied to shell root AND new view
            ThemeManager tm = ThemeManager.getInstance();
            tm.applyThemeToRoot(root);
            tm.applyThemeToRoot(view);

            contentHost.getChildren().setAll(view);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

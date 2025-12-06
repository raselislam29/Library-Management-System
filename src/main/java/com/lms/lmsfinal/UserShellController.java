package com.lms.lmsfinal;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
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
    @FXML private BorderPane root;   // must match fx:id="root"

    private final DateTimeFormatter fmt =
            DateTimeFormatter.ofPattern("hh:mm a • MMM dd, yyyy");

    @FXML
    private void initialize() {
        // Apply current theme
        ThemeManager.getInstance().applyThemeToRoot(root);

        String email = (Session.email == null || Session.email.isBlank())
                ? "user@example.com" : Session.email;
        welcomeLabel.setText("Welcome, " + email);

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

    @FXML public void showHome()          { swap("/com/lms/lmsfinal/UserHomeContent.fxml"); }
    @FXML public void showBrowse()        { swap("/com/lms/lmsfinal/UserBrowseContent.fxml"); }
    @FXML public void showBorrows()       { swap("/com/lms/lmsfinal/UserBorrowsContent.fxml"); }
    @FXML public void showProfile()       { swap("/com/lms/lmsfinal/UserProfileContent.fxml"); }

    // 🔔 NEW: notifications tab
    @FXML public void showNotifications() { swap("/com/lms/lmsfinal/UserNotificationsContent.fxml"); }

    private void swap(String fxml) {
        try {
            Node view = FXMLLoader.load(getClass().getResource(fxml));
            // apply theme to new view if needed
            ThemeManager.getInstance().applyThemeToRoot(root);
            contentHost.getChildren().setAll(view);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

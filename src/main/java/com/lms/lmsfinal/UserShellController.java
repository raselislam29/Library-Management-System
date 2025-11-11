package com.lms.lmsfinal;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class UserShellController {

    @FXML private Label welcomeLabel;
    @FXML private Label clockLabel;
    @FXML private StackPane contentHost;

    private final DateTimeFormatter fmt = DateTimeFormatter.ofPattern("hh:mm a • MMM dd, yyyy");

    @FXML
    private void initialize() {
        String email = (Session.email == null || Session.email.isBlank()) ? "user@example.com" : Session.email;
        welcomeLabel.setText("Welcome, " + email);

        // live clock
        Timeline t = new Timeline(new KeyFrame(Duration.seconds(1), e ->
                clockLabel.setText(LocalDateTime.now().format(fmt))));
        t.setCycleCount(Timeline.INDEFINITE);
        t.play();

        showHome();
    }

    @FXML private void onLogout() {
        Session.clear();
        LibraryApp.setScene("/com/lms/lmsfinal/LoginView.fxml", "Login / Register", 1200, 760);
    }

    @FXML public void showHome()    { swap("/com/lms/lmsfinal/UserHomeContent.fxml"); }
    @FXML public void showBrowse()  { swap("/com/lms/lmsfinal/UserBrowseContent.fxml"); }
    @FXML public void showBorrows() { swap("/com/lms/lmsfinal/UserBorrowsContent.fxml"); }

    private void swap(String fxml) {
        try {
            Node view = FXMLLoader.load(getClass().getResource(fxml));
            contentHost.getChildren().setAll(view);
        } catch (Exception e) { e.printStackTrace(); }
    }
}

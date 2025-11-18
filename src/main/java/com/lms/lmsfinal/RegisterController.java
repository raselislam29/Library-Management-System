package com.lms.lmsfinal;

import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.util.Duration;

public class RegisterController {

    @FXML private BorderPane root;
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmField;
    @FXML private Label errorLabel;
    @FXML private Button registerBtn;

    private final FirebaseService firebase = new FirebaseService();

    @FXML
    private void initialize() {
        if (errorLabel != null) errorLabel.setText("");
        applyFadeIn(root);
    }

    @FXML
    private void onRegister() {
        String email    = safe(emailField);
        String pass     = safe(passwordField);
        String confirm  = safe(confirmField);

        if (email.isBlank() || pass.isBlank() || confirm.isBlank()) {
            setError("All fields are required.");
            return;
        }

        if (!pass.equals(confirm)) {
            setError("Passwords do not match.");
            return;
        }

        if (pass.length() < 6) {
            setError("Password must be at least 6 characters.");
            return;
        }

        setBusy(true);

        Task<Void> task = new Task<>() {
            @Override protected Void call() {
                FirebaseService.SignUpResult res = firebase.signUpUser(email, pass);

                Platform.runLater(() -> setBusy(false));

                if (!res.ok) {
                    Platform.runLater(() -> setError(res.errorText != null ? res.errorText : "Registration failed."));
                    return null;
                }

                Platform.runLater(() -> {
                    new Alert(Alert.AlertType.INFORMATION,
                            "Successfully registered!\nPlease log in now.")
                            .showAndWait();
                    goLogin();
                });
                return null;
            }
        };
        new Thread(task, "register-thread").start();
    }

    @FXML
    private void goLogin() {
        LibraryApp.setScene("/com/lms/lmsfinal/LoginView.fxml", "Login", 1000, 700);
    }

    private void setBusy(boolean b) {
        if (registerBtn != null)    registerBtn.setDisable(b);
        if (emailField != null)     emailField.setDisable(b);
        if (passwordField != null)  passwordField.setDisable(b);
        if (confirmField != null)   confirmField.setDisable(b);
    }

    private void setError(String msg) {
        if (errorLabel != null) errorLabel.setText(msg == null ? "" : msg);
    }

    private String safe(TextField tf) {
        return (tf != null && tf.getText() != null) ? tf.getText().trim() : "";
    }

    private void applyFadeIn(Parent node) {
        if (node == null) return;
        node.setOpacity(0.0);
        FadeTransition ft = new FadeTransition(Duration.millis(350), node);
        ft.setFromValue(0.0);
        ft.setToValue(1.0);
        ft.play();
    }
}

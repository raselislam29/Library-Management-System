package com.lms.lmsfinal;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;

import java.util.Optional;

public class LoginController {

    @FXML private BorderPane root;
    @FXML private Label formTitle;
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;
    @FXML private Button loginButton;
    @FXML private Button registerButton;
    @FXML private Hyperlink toggleLink;

    private final FirebaseService firebase = new FirebaseService();
    private boolean registerMode = false;
    private boolean darkMode = true;

    @FXML
    private void initialize() {
        ensureThemeClass(root, "theme-dark", true);
        ensureThemeClass(root, "theme-light", false);
        setMode(false);
    }

    @FXML
    private void onLogin() {
        final String email = safe(emailField);
        final String pass  = safe(passwordField);
        if (email.isBlank() || pass.isBlank()) {
            setError("Please enter email and password.");
            return;
        }
        setBusy(true);

        Task<Void> task = new Task<>() {
            @Override protected Void call() {
                Optional<String> ok = firebase.validateLogin(email, pass);
                if (ok.isEmpty()) {
                    Platform.runLater(() -> {
                        setBusy(false);
                        setError("Invalid email or password.");
                    });
                    return null;
                }

                // Look up user role from Firestore
                User user = firebase.getUserByEmail(email);
                String role = (user != null && user.getUserRole() != null) ? user.getUserRole() : "MEMBER";

                Session.email = email;
                Session.role  = role;

                Platform.runLater(() -> {
                    setBusy(false);
                    setError("");
                    if (role.equalsIgnoreCase("ADMIN") || role.equalsIgnoreCase("LIBRARIAN")) {
                        LibraryApp.setScene("/com/lms/lmsfinal/AdminShellView.fxml", "Admin", 1200, 760);
                    } else {
                        LibraryApp.setScene("/com/lms/lmsfinal/UserShellView.fxml", "User", 1200, 760);

                    }
                });
                return null;
            }
        };
        new Thread(task).start();
    }

    @FXML
    private void onRegister() {
        final String email = safe(emailField);
        final String pass  = safe(passwordField);
        if (email.isBlank() || pass.isBlank()) {
            setError("Please enter email and password.");
            return;
        }
        setBusy(true);

        Task<Void> task = new Task<>() {
            @Override protected Void call() {
                FirebaseService.SignUpResult res = firebase.signUpUser(email, pass);
                Platform.runLater(() -> setBusy(false));
                if (!res.ok) {
                    Platform.runLater(() -> setError(res.errorText));
                    return null;
                }
                Platform.runLater(() -> {
                    new Alert(Alert.AlertType.INFORMATION,
                            "Successfully registered. Please log in.").showAndWait();
                    setMode(false);
                    passwordField.clear();
                    setError("");
                });
                return null;
            }
        };
        new Thread(task).start();
    }


    @FXML private void toggleMode() { setMode(!registerMode); }

    @FXML
    private void toggleTheme() {
        darkMode = !darkMode;
        ensureThemeClass(root, "theme-dark", darkMode);
        ensureThemeClass(root, "theme-light", !darkMode);
    }

    private void setMode(boolean register) {
        this.registerMode = register;
        registerButton.setManaged(register);
        registerButton.setVisible(register);
        loginButton.setManaged(!register);
        loginButton.setVisible(!register);

        if (toggleLink != null) {
            toggleLink.setText(register ? "Already have an account? Login." : "Need an account? Register here.");
        }
        if (formTitle != null) formTitle.setText(register ? "Create account" : "Sign in");
        setError("");
    }

    private void setBusy(boolean busy) {
        loginButton.setDisable(busy);
        registerButton.setDisable(busy);
        emailField.setDisable(busy);
        passwordField.setDisable(busy);
    }
    private void setError(String msg) { errorLabel.setText(msg == null ? "" : msg); }
    private String safe(TextField tf) { return (tf != null && tf.getText() != null) ? tf.getText().trim() : ""; }

    private void ensureThemeClass(Parent node, String clazz, boolean present) {
        if (node == null) return;
        var list = node.getStyleClass();
        if (present) { if (!list.contains(clazz)) list.add(clazz); } else { list.remove(clazz); }
    }
}

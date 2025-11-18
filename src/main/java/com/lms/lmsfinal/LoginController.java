package com.lms.lmsfinal;

import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.util.Duration;

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
    @FXML private Hyperlink forgotLink;

    private final FirebaseService firebase = new FirebaseService();
    private boolean registerMode = false;
    private boolean darkMode = true;

    @FXML
    private void initialize() {
        ensureThemeClass(root, "theme-dark", true);
        ensureThemeClass(root, "theme-light", false);
        setMode(false); // start in Sign In mode
        setError("");
        applyFadeIn(root);
    }

    // -------------------- LOGIN --------------------
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
                Optional<String> ok;
                try {
                    ok = firebase.validateLogin(email, pass);
                } catch (Exception ex) {
                    Platform.runLater(() -> {
                        setBusy(false);
                        setError("Network error: " + ex.getMessage());
                    });
                    return null;
                }

                if (ok.isEmpty()) {
                    Platform.runLater(() -> {
                        setBusy(false);
                        setError("Invalid email or password.");
                    });
                    return null;
                }

                // Check user role
                User user = firebase.getUserByEmail(email);
                String role = (user != null && user.getUserRole() != null)
                        ? user.getUserRole()
                        : "MEMBER";

                Session.email = email;
                Session.role  = role;

                Platform.runLater(() -> {
                    setBusy(false);
                    setError("");
                    if (role.equalsIgnoreCase("ADMIN") || role.equalsIgnoreCase("LIBRARIAN")) {
                        LibraryApp.setScene("/com/lms/lmsfinal/AdminShellView.fxml", "Admin Dashboard", 1200, 760);
                    } else {
                        LibraryApp.setScene("/com/lms/lmsfinal/UserShellView.fxml", "User Dashboard", 1200, 760);
                    }
                });
                return null;
            }
        };
        new Thread(task, "login-thread").start();
    }

    // -------------------- REGISTER (this button on Login view) --------------------
    @FXML
    private void onRegister() {
        // If you prefer a separate RegisterView.fxml:
        LibraryApp.setScene("/com/lms/lmsfinal/RegisterView.fxml", "Register", 1000, 700);
    }

    // -------------------- FORGOT PASSWORD --------------------
    @FXML
    private void onForgotPassword() {
        final String email = safe(emailField);
        if (email.isBlank()) {
            setError("Enter your email first, then click 'Forgot password?'.");
            return;
        }

        setBusy(true);

        Task<Void> task = new Task<>() {
            @Override protected Void call() {
                FirebaseService.PasswordResetResult res = firebase.sendPasswordResetEmail(email);
                Platform.runLater(() -> {
                    setBusy(false);
                    if (res.ok) {
                        new Alert(Alert.AlertType.INFORMATION,
                                "Password reset link sent to:\n" + email +
                                        "\n\nPlease check your inbox (and spam folder).")
                                .showAndWait();
                        setError("");
                    } else {
                        setError(res.errorText != null ? res.errorText : "Failed to send reset email.");
                    }
                });
                return null;
            }
        };
        new Thread(task, "forgot-pw-thread").start();
    }

    // -------------------- TOGGLE MODE (if you still want internal toggle) --------------------
    @FXML
    private void toggleMode() {
        // Instead of internal toggle, we move to Register screen:
        LibraryApp.setScene("/com/lms/lmsfinal/RegisterView.fxml", "Register", 1000, 700);
    }

    @FXML
    private void toggleTheme() {
        darkMode = !darkMode;
        ensureThemeClass(root, "theme-dark", darkMode);
        ensureThemeClass(root, "theme-light", !darkMode);
    }

    private void setMode(boolean register) {
        this.registerMode = register;
        // These are less important now if you're navigating to RegisterView.fxml,
        // but we keep them for compatibility.
        registerButton.setManaged(register);
        registerButton.setVisible(register);
        loginButton.setManaged(!register);
        loginButton.setVisible(!register);

        if (toggleLink != null) {
            toggleLink.setText(register
                    ? "Already have an account? Login."
                    : "Need an account? Register here.");
        }
        if (formTitle != null) formTitle.setText(register ? "Create account" : "Sign in");
        setError("");
    }

    private void setBusy(boolean busy) {
        if (loginButton != null)    loginButton.setDisable(busy);
        if (registerButton != null) registerButton.setDisable(busy);
        if (emailField != null)     emailField.setDisable(busy);
        if (passwordField != null)  passwordField.setDisable(busy);
        if (forgotLink != null)     forgotLink.setDisable(busy);
    }

    private void setError(String msg) {
        if (errorLabel != null)
            errorLabel.setText(msg == null ? "" : msg);
    }

    private String safe(TextField tf) {
        return (tf != null && tf.getText() != null)
                ? tf.getText().trim()
                : "";
    }

    private void ensureThemeClass(Parent node, String clazz, boolean present) {
        if (node == null) return;
        var list = node.getStyleClass();
        if (present) {
            if (!list.contains(clazz)) list.add(clazz);
        } else {
            list.remove(clazz);
        }
    }

    // -------------------- Fade-in animation --------------------
    private void applyFadeIn(Parent node) {
        if (node == null) return;
        node.setOpacity(0.0);
        FadeTransition ft = new FadeTransition(Duration.millis(350), node);
        ft.setFromValue(0.0);
        ft.setToValue(1.0);
        ft.play();
    }
}

package com.lms.lmsfinal;

import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;

public class AdminSettingsContentController {

    @FXML private ToggleButton toggleTheme;
    @FXML private Label themeStatus;

    @FXML
    private void initialize() {
        ThemeManager.Theme theme = ThemeManager.getInstance().getCurrentTheme();
        updateToggle(theme);
    }

    @FXML
    private void onToggleTheme() {
        if (toggleTheme.getScene() == null) return;
        Parent sceneRoot = toggleTheme.getScene().getRoot();
        if (sceneRoot == null) return;

        ThemeManager tm = ThemeManager.getInstance();
        ThemeManager.Theme newTheme =
                (tm.getCurrentTheme() == ThemeManager.Theme.DARK)
                        ? ThemeManager.Theme.LIGHT
                        : ThemeManager.Theme.DARK;

        tm.setTheme(newTheme, sceneRoot);
        updateToggle(newTheme);
    }

    private void updateToggle(ThemeManager.Theme theme) {
        boolean isDark = (theme == ThemeManager.Theme.DARK);
        if (toggleTheme != null) {
            toggleTheme.setSelected(isDark);
            toggleTheme.setText(isDark ? "Dark mode" : "Light mode");
        }
        if (themeStatus != null) {
            themeStatus.setText(isDark ? "Current theme: Dark" : "Current theme: Light");
        }
    }
}

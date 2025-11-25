package com.lms.lmsfinal;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.Node;
import javafx.scene.Parent;

public class UserSettingsContentController {

    @FXML private ToggleButton themeToggle;
    @FXML private Label statusLabel;

    @FXML
    private void initialize() {
        // Set toggle initial state based on current theme
        var theme = ThemeManager.getInstance().getCurrentTheme();
        boolean dark = (theme == ThemeManager.Theme.DARK);
        themeToggle.setSelected(dark);
        themeToggle.setText(dark ? "Dark" : "Light");
    }

    @FXML
    private void onToggleTheme() {
        // get root of scene
        Node anyNode = themeToggle;
        Parent root = anyNode.getScene() != null ? anyNode.getScene().getRoot() : null;
        if (root == null) return;

        boolean nowDark = themeToggle.isSelected();
        ThemeManager.Theme newTheme = nowDark
                ? ThemeManager.Theme.DARK
                : ThemeManager.Theme.LIGHT;

        ThemeManager.getInstance().setTheme(newTheme, root);

        themeToggle.setText(nowDark ? "Dark" : "Light");
        statusLabel.setText("Theme changed to " + (nowDark ? "Dark" : "Light") + " mode.");
    }
}

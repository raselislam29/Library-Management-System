package com.lms.lmsfinal;

import javafx.scene.Node;
import javafx.scene.Parent;

public class ThemeManager {

    public enum Theme { DARK, LIGHT }

    private static final ThemeManager INSTANCE = new ThemeManager();

    private Theme currentTheme = Theme.DARK;

    private ThemeManager() {}

    public static ThemeManager getInstance() {
        return INSTANCE;
    }

    public Theme getCurrentTheme() {
        return currentTheme;
    }

    /**
     * Set theme and immediately apply to the given root.
     */
    public void setTheme(Theme theme, Parent root) {
        this.currentTheme = theme;
        applyThemeToRoot(root);
    }

    /**
     * Toggle theme and apply to given root.
     */
    public void toggleTheme(Parent root) {
        if (currentTheme == Theme.DARK) {
            currentTheme = Theme.LIGHT;
        } else {
            currentTheme = Theme.DARK;
        }
        applyThemeToRoot(root);
    }

    /**
     * Apply the current theme to the given root node.
     * Here we do two things:
     *  1) update style classes (theme-dark / theme-light),
     *  2) set a basic background color inline so you SEE the change.
     */
    public void applyThemeToRoot(Parent root) {
        if (root == null) return;

        var styles = root.getStyleClass();
        styles.removeAll("theme-dark", "theme-light");

        if (currentTheme == Theme.DARK) {
            if (!styles.contains("theme-dark")) styles.add("theme-dark");
            // basic dark background so it is obviously different
            root.setStyle("-fx-background-color:#0f172a;");
        } else {
            if (!styles.contains("theme-light")) styles.add("theme-light");
            // basic light background
            root.setStyle("-fx-background-color:#f9fafb;");
        }
    }

    /**
     * Convenience: apply to any Node that is a Parent.
     */
    public void applyThemeToNode(Node node) {
        if (node instanceof Parent) {
            applyThemeToRoot((Parent) node);
        }
    }
}

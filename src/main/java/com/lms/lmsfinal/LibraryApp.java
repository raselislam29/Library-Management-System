package com.lms.lmsfinal;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.control.Alert;

import java.io.IOException;
import java.net.URL;

public class LibraryApp extends javafx.application.Application {

    public static Stage primaryStage;

    @Override
    public void start(Stage stage) throws IOException {
        primaryStage = stage;
        setScene("/com/lms/lmsfinal/LoginView.fxml", "Library Management System");
    }

    public static void setScene(String fxmlPath, String title) {
        setScene(fxmlPath, title, 1200, 760);
    }

    public static void setScene(String fxmlPath, String title, double width, double height) {
        try {
            System.out.println("[LibraryApp] Loading FXML: " + fxmlPath);
            URL fxmlUrl = LibraryApp.class.getResource(fxmlPath);
            if (fxmlUrl == null) {
                String msg = "FXML not found: " + fxmlPath;
                System.err.println(msg);
                new Alert(Alert.AlertType.ERROR, msg).showAndWait();
                return;
            }

            FXMLLoader loader = new FXMLLoader(fxmlUrl);
            Parent root = loader.load();
            Scene scene = (width > 0 && height > 0)
                    ? new Scene(root, width, height)
                    : new Scene(root);

            // ✅ Load user theme (from resources)
            addStylesheetIfExists(scene, "/com/lms/lmsfinal/styles/user-theme.css");

            // ✅ Load global stylesheet (from Java folder)
            try {
                URL globalCss = new URL("file:src/main/java/com/lms/lmsfinal/styles.css");
                scene.getStylesheets().add(globalCss.toExternalForm());
                System.out.println("[LibraryApp] Loaded global CSS from java folder");
            } catch (Exception ex) {
                System.err.println("[LibraryApp] Global CSS not found at /java path");
            }

            primaryStage.setScene(scene);
            primaryStage.setTitle(title);
            primaryStage.show();
        } catch (Exception e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Failed to load: " + e.getMessage()).showAndWait();
        }
    }

    private static void addStylesheetIfExists(Scene scene, String resourcePath) {
        URL cssUrl = LibraryApp.class.getResource(resourcePath);
        if (cssUrl != null) {
            scene.getStylesheets().add(cssUrl.toExternalForm());
            System.out.println("[LibraryApp] Attached CSS: " + resourcePath);
        } else {
            System.err.println("[LibraryApp] Missing CSS: " + resourcePath);
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}

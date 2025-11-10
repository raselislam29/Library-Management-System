package com.lms.lmsfinal;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;
import java.io.IOException;
import java.net.URL;

public class LibraryApp extends Application {
    public static Stage primaryStage;

    @Override
    public void start(Stage stage) {
        primaryStage = stage;
        setScene("/com/lms/lmsfinal/LoginView.fxml", "Login", 1000, 700);
    }

    public static void setScene(String fxmlResourcePath, String title) { setScene(fxmlResourcePath, title, -1, -1); }

    public static void setScene(String fxmlResourcePath, String title, double width, double height) {
        try {
            URL fxmlUrl = LibraryApp.class.getResource(fxmlResourcePath);
            if (fxmlUrl == null) {
                new Alert(Alert.AlertType.ERROR, "FXML not found: " + fxmlResourcePath).showAndWait();
                return;
            }
            FXMLLoader loader = new FXMLLoader(fxmlUrl);
            Parent root = loader.load();
            Scene scene = (width > 0 && height > 0) ? new Scene(root, width, height) : new Scene(root);
            URL css = LibraryApp.class.getResource("/com/lms/lmsfinal/styles.css");
            if (css != null) scene.getStylesheets().add(css.toExternalForm());
            primaryStage.setScene(scene);
            primaryStage.setTitle(title);
            primaryStage.show();
        } catch (IOException e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Failed to load view:\n" + e.getMessage()).showAndWait();
        }
    }

    public static void main(String[] args) { launch(args); }
}

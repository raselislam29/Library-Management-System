package com.lms.lmsfinal;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.chart.PieChart;
import javafx.scene.control.Label;

import java.util.List;

public class UserHomeContentController {

    @FXML private PieChart pie;
    @FXML private Label borrowedCount;
    @FXML private Label returnedCount;

    private final FirebaseService firebase = new FirebaseService();

    @FXML
    private void initialize() {
        // Mark as final so it can be used in the inner class
        final String userEmail = (Session.email == null || Session.email.isBlank())
                ? "user@example.com"
                : Session.email;

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() {
                List<UserBorrow> all = firebase.getBorrowsForUser(userEmail, null);
                long returned = all.stream().filter(UserBorrow::isReturned).count();
                long active = all.size() - returned;

                final long fReturned = returned;
                final long fActive = active;

                Platform.runLater(() -> {
                    borrowedCount.setText("Active: " + fActive);
                    returnedCount.setText("Returned: " + fReturned);
                    pie.getData().setAll(
                            new PieChart.Data("Borrowed", fActive),
                            new PieChart.Data("Returned", fReturned)
                    );
                });
                return null;
            }
        };

        new Thread(task).start();
    }
}

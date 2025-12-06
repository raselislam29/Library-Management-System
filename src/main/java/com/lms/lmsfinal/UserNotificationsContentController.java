package com.lms.lmsfinal;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

public class UserNotificationsContentController {

    @FXML private ListView<String> listView;
    @FXML private Label statusLabel;

    private final FirebaseService firebase = new FirebaseService();
    private final ObservableList<String> items = FXCollections.observableArrayList();

    @FXML
    private void initialize() {
        listView.setItems(items);
        loadNotifications();
    }

    private void loadNotifications() {
        statusLabel.setText("Loading notifications...");
        final String email = (Session.email == null || Session.email.isBlank())
                ? "user@example.com"
                : Session.email;

        Task<Void> t = new Task<>() {
            @Override
            protected Void call() {
                try {
                    // active, not returned borrows for this user
                    List<FirebaseService.AdminBorrow> active =
                            firebase.getActiveBorrowsForUser(email);

                    LocalDate today = LocalDate.now();
                    ObservableList<String> tmp = FXCollections.observableArrayList();

                    for (FirebaseService.AdminBorrow ab : active) {
                        if (ab.getDueDate() == null) continue;

                        LocalDate due = ab.getDueDate().toInstant()
                                .atZone(ZoneId.systemDefault())
                                .toLocalDate();

                        long days = java.time.temporal.ChronoUnit.DAYS.between(today, due);

                        String title = ab.getTitle() == null ? "(Unknown title)" : ab.getTitle();

                        if (days == 2) {
                            tmp.add("📘 \"" + title + "\" is due in 2 days (" + due + ").");
                        } else if (days == 1) {
                            tmp.add("📘 \"" + title + "\" is due tomorrow (" + due + ").");
                        } else if (days == 0) {
                            tmp.add("⏰ \"" + title + "\" is due today (" + due + ").");
                        } else if (days < 0) {
                            tmp.add("⚠️ \"" + title + "\" is overdue by "
                                    + Math.abs(days) + " day(s).");
                        }
                    }

                    Platform.runLater(() -> {
                        items.setAll(tmp);
                        if (tmp.isEmpty()) {
                            statusLabel.setText("No notifications right now.");
                        } else {
                            statusLabel.setText("You have " + tmp.size() + " notifications.");
                        }
                    });

                } catch (Exception e) {
                    e.printStackTrace();
                    Platform.runLater(() ->
                            statusLabel.setText("Error loading notifications: " + e.getMessage()));
                }
                return null;
            }
        };

        new Thread(t).start();
    }
}

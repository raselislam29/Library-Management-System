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

public class AdminNotificationsContentController {

    @FXML private ListView<String> overdueList;
    @FXML private ListView<String> lowStockList;
    @FXML private Label statusLabel;

    private final FirebaseService firebase = new FirebaseService();

    private final ObservableList<String> overdueItems   = FXCollections.observableArrayList();
    private final ObservableList<String> lowStockItems  = FXCollections.observableArrayList();

    @FXML
    private void initialize() {
        overdueList.setItems(overdueItems);
        lowStockList.setItems(lowStockItems);
        loadNotifications();
    }

    private void loadNotifications() {
        statusLabel.setText("Loading notifications...");

        Task<Void> t = new Task<>() {
            @Override
            protected Void call() {
                try {
                    // Overdue loans
                    List<FirebaseService.AdminBorrow> active = firebase.getActiveBorrows();
                    ObservableList<String> overdueTmp = FXCollections.observableArrayList();

                    LocalDate today = LocalDate.now();

                    for (FirebaseService.AdminBorrow ab : active) {
                        if (ab.getDueDate() == null) continue;

                        LocalDate due = ab.getDueDate().toInstant()
                                .atZone(ZoneId.systemDefault())
                                .toLocalDate();

                        long diff = java.time.temporal.ChronoUnit.DAYS.between(due, today);
                        if (diff > 0) {
                            String who = (ab.getStudentEmail() == null || ab.getStudentEmail().isBlank())
                                    ? "(unknown user)"
                                    : ab.getStudentEmail();
                            String title = ab.getTitle() == null ? "(Unknown title)" : ab.getTitle();
                            overdueTmp.add("⚠️ \"" + title + "\" for " + who +
                                    " is overdue by " + diff + " day(s).");
                        }
                    }

                    // Low stock
                    List<Book> low = firebase.getLowStockBooks(3);
                    ObservableList<String> lowTmp = FXCollections.observableArrayList();
                    for (Book b : low) {
                        String title = b.getTitle() == null ? "(Unknown title)" : b.getTitle();
                        lowTmp.add("📉 Low stock: \"" + title + "\" ("
                                + b.getAvailableCopies() + " available)");
                    }

                    Platform.runLater(() -> {
                        overdueItems.setAll(overdueTmp);
                        lowStockItems.setAll(lowTmp);

                        String msg = "Loaded " + overdueTmp.size() + " overdue, "
                                + lowTmp.size() + " low stock alerts.";
                        statusLabel.setText(msg);
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

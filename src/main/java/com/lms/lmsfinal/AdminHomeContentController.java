package com.lms.lmsfinal;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.chart.PieChart;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class AdminHomeContentController {

    @FXML private Label lblActiveLoans;
    @FXML private Label lblOverdueLoans;
    @FXML private Label lblBorrowers;
    @FXML private Label statusLabel;

    @FXML private PieChart loanPie;

    @FXML private TableView<FirebaseService.AdminBorrow> tblPreview;
    @FXML private TableColumn<FirebaseService.AdminBorrow, String> colStudent;
    @FXML private TableColumn<FirebaseService.AdminBorrow, String> colEmail;
    @FXML private TableColumn<FirebaseService.AdminBorrow, String> colIsbn;
    @FXML private TableColumn<FirebaseService.AdminBorrow, String> colTitle;
    @FXML private TableColumn<FirebaseService.AdminBorrow, String> colDue;
    @FXML private TableColumn<FirebaseService.AdminBorrow, String> colOver;

    private final FirebaseService firebase = new FirebaseService();
    private final ObservableList<FirebaseService.AdminBorrow> previewData =
            FXCollections.observableArrayList();

    @FXML
    private void initialize() {
        // --- Table columns ---
        colStudent.setCellValueFactory(new PropertyValueFactory<>("studentName"));
        colEmail.setCellValueFactory(new PropertyValueFactory<>("studentEmail"));
        colIsbn.setCellValueFactory(new PropertyValueFactory<>("isbn"));
        colTitle.setCellValueFactory(new PropertyValueFactory<>("title"));

        // due date as plain string using getDueDateStr()
        colDue.setCellValueFactory(cd ->
                new SimpleStringProperty(cd.getValue().getDueDateStr())
        );

        // overdue column: compute "X days" or "-"
        colOver.setCellValueFactory(cd -> {
            FirebaseService.AdminBorrow ab = cd.getValue();
            String text = "-";
            if (ab.getDueDate() != null) {
                LocalDate due = ab.getDueDate().toInstant()
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate();
                long diff = java.time.temporal.ChronoUnit.DAYS.between(due, LocalDate.now());
                if (diff > 0) {
                    text = diff + " days";
                }
            }
            return new SimpleStringProperty(text);
        });

        tblPreview.setItems(previewData);

        // Optional: show labels on the pie chart
        loanPie.setLabelsVisible(true);

        loadStats();
    }

    @FXML
    private void onRefresh() {
        loadStats();
    }

    private void loadStats() {
        statusLabel.setText("Loading...");
        Task<Void> t = new Task<>() {
            @Override
            protected Void call() {
                try {
                    // get all active (not returned) borrows
                    List<FirebaseService.AdminBorrow> list = firebase.getActiveBorrows();

                    int totalActive = list.size();

                    LocalDate today = LocalDate.now();
                    int overdueCount = 0;

                    for (FirebaseService.AdminBorrow ab : list) {
                        if (ab.getDueDate() != null) {
                            LocalDate due = ab.getDueDate().toInstant()
                                    .atZone(ZoneId.systemDefault())
                                    .toLocalDate();
                            if (due.isBefore(today)) {
                                overdueCount++;
                            }
                        }
                    }

                    // unique borrowers by email
                    Set<String> borrowers = list.stream()
                            .map(FirebaseService.AdminBorrow::getStudentEmail)
                            .filter(e -> e != null && !e.isBlank())
                            .collect(Collectors.toSet());

                    int borrowerCount = borrowers.size();

                    int onTime = totalActive - overdueCount;
                    if (onTime < 0) onTime = 0;

                    // show max 25 rows in preview
                    List<FirebaseService.AdminBorrow> preview = list.size() > 25
                            ? list.subList(0, 25)
                            : list;

                    // make final copies for lambda
                    final int fTotalActive = totalActive;
                    final int fOverdue     = overdueCount;
                    final int fBorrowers   = borrowerCount;
                    final int fOnTime      = onTime;
                    final List<FirebaseService.AdminBorrow> fPreview = preview;

                    Platform.runLater(() -> {
                        lblActiveLoans.setText(String.valueOf(fTotalActive));
                        lblOverdueLoans.setText(String.valueOf(fOverdue));
                        lblBorrowers.setText(String.valueOf(fBorrowers));

                        loanPie.getData().clear();
                        loanPie.getData().addAll(
                                new PieChart.Data("On Time", fOnTime),
                                new PieChart.Data("Overdue", fOverdue)
                        );

                        previewData.setAll(fPreview);

                        if (fTotalActive == 0) {
                            statusLabel.setText("No active loans right now.");
                        } else {
                            statusLabel.setText("Active: " + fTotalActive +
                                    " • Overdue: " + fOverdue +
                                    " • Borrowers: " + fBorrowers);
                        }
                    });

                } catch (Exception e) {
                    e.printStackTrace();
                    Platform.runLater(() ->
                            statusLabel.setText("Error loading stats: " + e.getMessage()));
                }
                return null;
            }
        };
        new Thread(t).start();
    }
}

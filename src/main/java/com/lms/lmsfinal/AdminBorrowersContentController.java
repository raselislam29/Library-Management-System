package com.lms.lmsfinal;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

public class AdminBorrowersContentController {

    // Active tab
    @FXML private TableView<FirebaseService.AdminBorrow> tblActive;
    @FXML private TableColumn<FirebaseService.AdminBorrow, String> colAStudent;
    @FXML private TableColumn<FirebaseService.AdminBorrow, String> colAEmail;
    @FXML private TableColumn<FirebaseService.AdminBorrow, String> colAIsbn;
    @FXML private TableColumn<FirebaseService.AdminBorrow, String> colATitle;
    @FXML private TableColumn<FirebaseService.AdminBorrow, String> colADue;
    @FXML private TableColumn<FirebaseService.AdminBorrow, String> colAOver;
    @FXML private Label lblActiveStatus;

    // Returned tab
    @FXML private TableView<FirebaseService.AdminBorrow> tblReturned;
    @FXML private TableColumn<FirebaseService.AdminBorrow, String> colRStudent;
    @FXML private TableColumn<FirebaseService.AdminBorrow, String> colREmail;
    @FXML private TableColumn<FirebaseService.AdminBorrow, String> colRIsbn;
    @FXML private TableColumn<FirebaseService.AdminBorrow, String> colRTitle;
    @FXML private TableColumn<FirebaseService.AdminBorrow, String> colRDue;
    @FXML private TableColumn<FirebaseService.AdminBorrow, String> colRReturnedAt;
    @FXML private TableColumn<FirebaseService.AdminBorrow, String> colRLate;
    @FXML private Label lblReturnedStatus;

    private final FirebaseService firebase = new FirebaseService();

    private final ObservableList<FirebaseService.AdminBorrow> activeList   = FXCollections.observableArrayList();
    private final ObservableList<FirebaseService.AdminBorrow> returnedList = FXCollections.observableArrayList();

    @FXML
    private void initialize() {
        // ---- Active columns ----
        colAStudent.setCellValueFactory(c -> new SimpleStringProperty(
                nullSafe(c.getValue().getStudentName())
        ));
        colAEmail.setCellValueFactory(c -> new SimpleStringProperty(
                nullSafe(c.getValue().getStudentEmail())
        ));
        colAIsbn.setCellValueFactory(c -> new SimpleStringProperty(
                nullSafe(c.getValue().getIsbn())
        ));
        colATitle.setCellValueFactory(c -> new SimpleStringProperty(
                nullSafe(c.getValue().getTitle())
        ));
        colADue.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getDueDateStr()
        ));
        colAOver.setCellValueFactory(c -> new SimpleStringProperty(
                computeOverdueText(c.getValue())
        ));

        tblActive.setItems(activeList);

        // ---- Returned columns ----
        colRStudent.setCellValueFactory(c -> new SimpleStringProperty(
                nullSafe(c.getValue().getStudentName())
        ));
        colREmail.setCellValueFactory(c -> new SimpleStringProperty(
                nullSafe(c.getValue().getStudentEmail())
        ));
        colRIsbn.setCellValueFactory(c -> new SimpleStringProperty(
                nullSafe(c.getValue().getIsbn())
        ));
        colRTitle.setCellValueFactory(c -> new SimpleStringProperty(
                nullSafe(c.getValue().getTitle())
        ));
        colRDue.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getDueDateStr()
        ));
        colRReturnedAt.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getReturnedAtStr()
        ));
        colRLate.setCellValueFactory(c -> new SimpleStringProperty(
                computeLateText(c.getValue())
        ));

        tblReturned.setItems(returnedList);

        // load initial data
        refreshData();
    }

    @FXML
    private void onRefreshAll() {
        refreshData();
    }

    private void refreshData() {
        lblActiveStatus.setText("Loading...");
        lblReturnedStatus.setText("Loading...");
        Task<Void> t = new Task<>() {
            @Override
            protected Void call() {
                try {
                    List<FirebaseService.AdminBorrow> active   = firebase.getActiveBorrows();
                    List<FirebaseService.AdminBorrow> returned = firebase.getReturnedBorrows();

                    Platform.runLater(() -> {
                        activeList.setAll(active);
                        returnedList.setAll(returned);

                        lblActiveStatus.setText("Active loans: " + active.size());
                        lblReturnedStatus.setText("Returned loans: " + returned.size());
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                    Platform.runLater(() -> {
                        lblActiveStatus.setText("Error: " + e.getMessage());
                        lblReturnedStatus.setText("Error: " + e.getMessage());
                    });
                }
                return null;
            }
        };
        new Thread(t).start();
    }

    private String nullSafe(String s) {
        return s == null ? "" : s;
    }

    private String computeOverdueText(FirebaseService.AdminBorrow ab) {
        if (ab.getDueDate() == null) return "-";
        LocalDate due = ab.getDueDate().toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDate();
        long diff = java.time.temporal.ChronoUnit.DAYS.between(due, LocalDate.now());
        return diff > 0 ? diff + " days" : "-";
    }

    private String computeLateText(FirebaseService.AdminBorrow ab) {
        if (ab.getDueDate() == null || ab.getReturnedAt() == null) return "-";

        LocalDate due = ab.getDueDate().toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDate();
        LocalDate ret = ab.getReturnedAt().toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDate();

        long diff = java.time.temporal.ChronoUnit.DAYS.between(due, ret);
        if (diff > 0) {
            return diff + " days late";
        } else {
            return "On time";
        }
    }
}

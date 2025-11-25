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
import javafx.scene.control.TextField;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

public class AdminBorrowersContentController {

    @FXML private TextField searchField;

    @FXML private TableView<FirebaseService.AdminBorrow> table;
    @FXML private TableColumn<FirebaseService.AdminBorrow, String> colStudent;
    @FXML private TableColumn<FirebaseService.AdminBorrow, String> colEmail;
    @FXML private TableColumn<FirebaseService.AdminBorrow, String> colIsbn;
    @FXML private TableColumn<FirebaseService.AdminBorrow, String> colTitle;
    @FXML private TableColumn<FirebaseService.AdminBorrow, String> colDue;
    @FXML private TableColumn<FirebaseService.AdminBorrow, String> colOver;

    @FXML private Label statusLabel;

    private final FirebaseService firebase = new FirebaseService();

    // Master list + filtered list
    private final ObservableList<FirebaseService.AdminBorrow> all     = FXCollections.observableArrayList();
    private final ObservableList<FirebaseService.AdminBorrow> filtered = FXCollections.observableArrayList();

    @FXML
    private void initialize() {
        // Column bindings
        colStudent.setCellValueFactory(cd ->
                new SimpleStringProperty(cd.getValue().getStudentName())
        );
        colEmail.setCellValueFactory(cd ->
                new SimpleStringProperty(cd.getValue().getStudentEmail())
        );
        colIsbn.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("isbn"));
        colTitle.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("title"));

        // Use getDueDateStr() for date string
        colDue.setCellValueFactory(cd ->
                new SimpleStringProperty(cd.getValue().getDueDateStr())
        );

        // Overdue column: "X days" or "-"
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

        table.setItems(filtered);

        // Live filtering when typing
        searchField.textProperty().addListener((obs, oldV, newV) -> applyFilter());

        // Load data initially
        refreshData();
    }

    @FXML
    private void onRefresh() {
        refreshData();
    }

    private void refreshData() {
        statusLabel.setText("Loading...");
        Task<Void> t = new Task<>() {
            @Override
            protected Void call() {
                try {
                    List<FirebaseService.AdminBorrow> list = firebase.getActiveBorrows();

                    Platform.runLater(() -> {
                        all.setAll(list);
                        applyFilter();
                        statusLabel.setText("Loaded " + list.size() + " active borrows.");
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                    Platform.runLater(() ->
                            statusLabel.setText("Error loading borrows: " + e.getMessage()));
                }
                return null;
            }
        };
        new Thread(t).start();
    }

    private void applyFilter() {
        String q = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase();
        filtered.setAll(
                all.filtered(ab ->
                        q.isEmpty()
                                || (ab.getStudentName()  != null && ab.getStudentName().toLowerCase().contains(q))
                                || (ab.getStudentEmail() != null && ab.getStudentEmail().toLowerCase().contains(q))
                                || (ab.getTitle()        != null && ab.getTitle().toLowerCase().contains(q))
                                || (ab.getIsbn()         != null && ab.getIsbn().toLowerCase().contains(q))
                )
        );
    }
}

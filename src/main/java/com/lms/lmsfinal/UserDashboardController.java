package com.lms.lmsfinal;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.util.List;

public class UserDashboardController {

    @FXML private Label userLabel;
    @FXML private TextField searchField, loanDaysField;
    @FXML private TableView<Book> booksTable;
    @FXML private TableColumn<Book, String> colTitle, colAuthor, colIsbn;
    @FXML private TableColumn<Book, Integer> colAvail;
    @FXML private Label statusLabel;

    private final FirebaseService firebase = new FirebaseService();
    private final ObservableList<Book> books = FXCollections.observableArrayList();

    @FXML
    private void initialize() {
        userLabel.setText(Session.email + " • " + Session.role);
        colTitle.setCellValueFactory(new PropertyValueFactory<>("title"));
        colAuthor.setCellValueFactory(new PropertyValueFactory<>("author"));
        colIsbn.setCellValueFactory(new PropertyValueFactory<>("isbn"));
        colAvail.setCellValueFactory(new PropertyValueFactory<>("availableCopies"));
        booksTable.setItems(books);
        refreshBooks();
    }

    @FXML private void onLogout() { Session.clear(); LibraryApp.setScene("/com/lms/lmsfinal/LoginView.fxml", "Login / Register", 1200, 760); }

    private void refreshBooks() {
        Task<Void> t = new Task<>() {
            @Override protected Void call() {
                List<Book> data = firebase.getBooks();
                Platform.runLater(() -> books.setAll(data));
                return null;
            }
        };
        new Thread(t).start();
    }

    @FXML private void onSearch() {
        String q = searchField.getText() == null ? "" : searchField.getText().trim();
        Task<Void> t = new Task<>() {
            @Override protected Void call() {
                List<Book> data = q.isEmpty() ? firebase.getBooks() : firebase.searchBooks(q);
                Platform.runLater(() -> books.setAll(data));
                return null;
            }
        };
        new Thread(t).start();
    }

    @FXML private void onReset() { searchField.clear(); refreshBooks(); }

    @FXML
    private void onBorrow() {
        Book b = booksTable.getSelectionModel().getSelectedItem();
        if (b == null) { status("Select a book."); return; }
        if (b.getAvailableCopies() <= 0) { status("No copies available."); return; }

        int days = 14;
        try { days = Integer.parseInt(loanDaysField.getText().trim()); } catch (Exception ignored) {}

        // open the borrow form dialog
        BorrowFormData form = BorrowFormController.open();
        if (form == null) { status("Cancelled."); return; }

        // call Firebase
        int finalDays = days;
        Task<Void> t = new Task<>() {
            @Override protected Void call() {
                try {
                    firebase.borrowBookWithStudentInfo(
                            b.getIsbn(),
                            Session.email,
                            finalDays,
                            form.getStudentName(),
                            form.getStudentId(),
                            form.getPhone(),
                            form.getStudentEmail()
                    );
                    Platform.runLater(() -> {
                        status("Borrowed: " + b.getTitle());
                        refreshBooks();
                    });
                } catch (Exception e) {
                    Platform.runLater(() -> status("Error: " + e.getMessage()));
                }
                return null;
            }
        };
        new Thread(t).start();
    }

    private void status(String s) { statusLabel.setText(s); }
}

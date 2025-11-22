package com.lms.lmsfinal;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

public class AdminDashboardController {

    @FXML private Label userLabel;

    // Books UI
    @FXML private TableView<Book> booksTable;
    @FXML private TableColumn<Book, String> colIsbn, colTitle, colAuthor, colPublisher;
    @FXML private TableColumn<Book, Integer> colTotal, colAvail;
    @FXML private TextField searchField;
    @FXML private Label statusBooks;

    // Borrows UI
    @FXML private TableView<BorrowRow> borrowsTable;
    @FXML private TableColumn<BorrowRow, String> bColName, bColEmail, bColIsbn, bColTitle, bColDue, bColOver;
    @FXML private Label statusBorrows;

    private final FirebaseService firebase = new FirebaseService();
    private final ObservableList<Book> books = FXCollections.observableArrayList();
    private final ObservableList<BorrowRow> borrows = FXCollections.observableArrayList();

    @FXML
    private void initialize() {
        userLabel.setText(Session.email + " • " + Session.role);

        // books table
        colIsbn.setCellValueFactory(new PropertyValueFactory<>("isbn"));
        colTitle.setCellValueFactory(new PropertyValueFactory<>("title"));
        colAuthor.setCellValueFactory(new PropertyValueFactory<>("author"));
        colPublisher.setCellValueFactory(new PropertyValueFactory<>("publisher"));
        colTotal.setCellValueFactory(new PropertyValueFactory<>("totalCopies"));
        colAvail.setCellValueFactory(new PropertyValueFactory<>("availableCopies"));
        booksTable.setItems(books);

        // borrows table
        bColName.setCellValueFactory(new PropertyValueFactory<>("studentName"));
        bColEmail.setCellValueFactory(new PropertyValueFactory<>("studentEmail"));
        bColIsbn.setCellValueFactory(new PropertyValueFactory<>("isbn"));
        bColTitle.setCellValueFactory(new PropertyValueFactory<>("title"));
        bColDue.setCellValueFactory(new PropertyValueFactory<>("dueDateStr"));
        bColOver.setCellValueFactory(new PropertyValueFactory<>("overdueStr"));
        borrowsTable.setItems(borrows);

        refreshBooks();
        refreshBorrows();
    }

    @FXML
    private void onLogout() {
        Session.clear();
        LibraryApp.setScene("/com/lms/lmsfinal/LoginView.fxml", "Login / Register", 1200, 760);
    }

    // ================= BOOKS (read-only on dashboard) =================

    @FXML
    private void onSearchBooks() {
        String q = searchField.getText() == null ? "" : searchField.getText().trim();
        Task<Void> t = new Task<>() {
            @Override
            protected Void call() {
                List<Book> data = q.isEmpty()
                        ? firebase.getBooks()
                        : firebase.searchBooks(q);
                Platform.runLater(() -> {
                    books.setAll(data);
                    statusBooks.setText(q.isEmpty()
                            ? "Showing all books"
                            : "Showing results for: \"" + q + "\"");
                });
                return null;
            }
        };
        new Thread(t).start();
    }

    @FXML
    private void onResetBooks() {
        searchField.clear();
        refreshBooks();
        statusBooks.setText("Showing all books");
    }

    private void refreshBooks() {
        Task<Void> t = new Task<>() {
            @Override
            protected Void call() {
                List<Book> data = firebase.getBooks();
                Platform.runLater(() -> books.setAll(data));
                return null;
            }
        };
        new Thread(t).start();
    }

    /**
     * Open the dedicated Manage Books screen.
     * Make sure AdminBooksView.fxml uses AdminBooksContentController.
     */
    @FXML
    private void openManageBooks() {
        // If your LibraryApp.setScene has a 4-arg overload, you can also do:
        // LibraryApp.setScene("/com/lms/lmsfinal/AdminBooksView.fxml", "Manage Books", 1000, 700);
        LibraryApp.setScene("/com/lms/lmsfinal/AdminBooksView.fxml", "Manage Books");
    }

    // ================= BORROWS =================

    public static class BorrowRow {
        private final String studentName;
        private final String studentEmail;
        private final String isbn;
        private final String title;
        private final String dueDateStr;
        private final String overdueStr;

        public BorrowRow(String studentName, String studentEmail, String isbn, String title,
                         String dueDateStr, String overdueStr) {
            this.studentName = studentName;
            this.studentEmail = studentEmail;
            this.isbn = isbn;
            this.title = title;
            this.dueDateStr = dueDateStr;
            this.overdueStr = overdueStr;
        }

        public String getStudentName()  { return studentName; }
        public String getStudentEmail() { return studentEmail; }
        public String getIsbn()         { return isbn; }
        public String getTitle()        { return title; }
        public String getDueDateStr()   { return dueDateStr; }
        public String getOverdueStr()   { return overdueStr; }
    }

    @FXML
    private void refreshBorrows() {
        Task<Void> t = new Task<>() {
            @Override
            protected Void call() {
                var list = firebase.getActiveBorrows(); // all not returned
                var rows = FXCollections.<BorrowRow>observableArrayList();

                for (var br : list) {
                    LocalDate due = br.getDueDate() == null
                            ? null
                            : br.getDueDate().toInstant()
                            .atZone(ZoneId.systemDefault())
                            .toLocalDate();

                    long overdueDays = 0;
                    if (due != null) {
                        overdueDays = java.time.temporal.ChronoUnit.DAYS.between(due, LocalDate.now());
                    }
                    String overdueStr = overdueDays > 0 ? overdueDays + " days" : "-";

                    rows.add(new BorrowRow(
                            br.getStudentName(),
                            br.getStudentEmail(),
                            br.getIsbn(),
                            br.getTitle(),
                            br.getDueDateStr(),
                            overdueStr
                    ));
                }

                Platform.runLater(() -> borrows.setAll(rows));
                return null;
            }
        };
        new Thread(t).start();
    }

    @FXML
    private void markReturned() {
        var sel = borrowsTable.getSelectionModel().getSelectedItem();
        if (sel == null) {
            statusBorrows.setText("Select a row.");
            return;
        }
        try {
            firebase.returnBook(sel.getIsbn(), sel.getStudentEmail());
            statusBorrows.setText("Marked returned.");
            refreshBorrows();
            refreshBooks();
        } catch (Exception e) {
            statusBorrows.setText("Error: " + e.getMessage());
        }
    }
}

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
    @FXML private TextField searchField, isbnField, titleField, authorField, publisherField, totalField, availableField;
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

    @FXML
    private void onSearchBooks() {
        String q = searchField.getText() == null ? "" : searchField.getText().trim();
        Task<Void> t = new Task<>() {
            @Override
            protected Void call() {
                List<Book> data = q.isEmpty() ? firebase.getBooks() : firebase.searchBooks(q);
                Platform.runLater(() -> books.setAll(data));
                return null;
            }
        };
        new Thread(t).start();
    }

    @FXML
    private void onResetBooks() {
        searchField.clear();
        refreshBooks();
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

    @FXML
    private void onAddBook() {
        try {
            Book b = readBookForm();
            firebase.addBook(b);
            statusBooks.setText("Added " + b.getTitle());
            refreshBooks();
        } catch (Exception e) {
            statusBooks.setText("Error: " + e.getMessage());
        }
    }

    @FXML
    private void onUpdateBook() {
        try {
            Book b = readBookForm();
            firebase.updateBook(b);
            statusBooks.setText("Updated " + b.getTitle());
            refreshBooks();
        } catch (Exception e) {
            statusBooks.setText("Error: " + e.getMessage());
        }
    }

    @FXML
    private void onDeleteBook() {
        Book b = booksTable.getSelectionModel().getSelectedItem();
        if (b == null) {
            statusBooks.setText("Select a book first.");
            return;
        }
        try {
            firebase.deleteBookByIsbn(b.getIsbn());
            statusBooks.setText("Deleted " + b.getTitle());
            refreshBooks();
        } catch (Exception e) {
            statusBooks.setText("Error: " + e.getMessage());
        }
    }

    private Book readBookForm() {
        String isbn = val(isbnField);
        String title = val(titleField);
        String author = val(authorField);
        String publisher = val(publisherField);
        int total = parseInt(val(totalField), 1);
        int avail = parseInt(val(availableField), total);

        // No category field in this form, so use a default or change later if needed
        String category = "General";

        // Match Book constructor: (isbn, title, author, category, publisher, total, available)
        return new Book(isbn, title, author, category, publisher, total, avail);
    }

    private String val(TextField tf) {
        return tf.getText() == null ? "" : tf.getText().trim();
    }

    private int parseInt(String s, int d) {
        try {
            return Integer.parseInt(s);
        } catch (Exception e) {
            return d;
        }
    }

    public static class BorrowRow {
        private final String studentName, studentEmail, isbn, title, dueDateStr, overdueStr;

        public BorrowRow(String studentName, String studentEmail, String isbn, String title,
                         String dueDateStr, String overdueStr) {
            this.studentName = studentName;
            this.studentEmail = studentEmail;
            this.isbn = isbn;
            this.title = title;
            this.dueDateStr = dueDateStr;
            this.overdueStr = overdueStr;
        }

        public String getStudentName() { return studentName; }
        public String getStudentEmail() { return studentEmail; }
        public String getIsbn() { return isbn; }
        public String getTitle() { return title; }
        public String getDueDateStr() { return dueDateStr; }
        public String getOverdueStr() { return overdueStr; }
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

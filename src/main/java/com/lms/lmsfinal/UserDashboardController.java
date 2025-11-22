package com.lms.lmsfinal;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

public class UserDashboardController {

    @FXML private Label userLabel;

    // Stats
    @FXML private Label lblActiveLoans;
    @FXML private Label lblTotalLoans;
    @FXML private Label lblOverdue;

    // Active loans table
    @FXML private TableView<UserBorrow> tblActive;
    @FXML private TableColumn<UserBorrow, String> colAIsbn;
    @FXML private TableColumn<UserBorrow, String> colATitle;
    @FXML private TableColumn<UserBorrow, String> colABorrowed;
    @FXML private TableColumn<UserBorrow, String> colADue;
    @FXML private Label statusActive;

    // Books table (browse/borrow)
    @FXML private TableView<Book> tblBooks;
    @FXML private TableColumn<Book, String> colBIsbn;
    @FXML private TableColumn<Book, String> colBTitle;
    @FXML private TableColumn<Book, String> colBAuthor;
    @FXML private TableColumn<Book, Integer> colBAvail;
    @FXML private TextField bookSearchField;
    @FXML private Label statusBooks;

    private final FirebaseService firebase = new FirebaseService();
    private final ObservableList<UserBorrow> activeBorrows = FXCollections.observableArrayList();
    private final ObservableList<Book> availableBooks = FXCollections.observableArrayList();

    // Formatter for strings like "Tue Nov 19 00:00:00 EST 2025"
    private static final DateTimeFormatter DATE_STR_FORMATTER =
            DateTimeFormatter.ofPattern("EEE MMM dd HH:mm:ss zzz yyyy", Locale.ENGLISH);

    @FXML
    private void initialize() {
        // Show user info
        userLabel.setText(Session.email + " • " + Session.role);

        // Setup active loans table
        colAIsbn.setCellValueFactory(new PropertyValueFactory<>("isbn"));
        colATitle.setCellValueFactory(new PropertyValueFactory<>("title"));
        colABorrowed.setCellValueFactory(new PropertyValueFactory<>("borrowedAt"));
        colADue.setCellValueFactory(new PropertyValueFactory<>("dueDate"));
        tblActive.setItems(activeBorrows);

        // Setup books table
        colBIsbn.setCellValueFactory(new PropertyValueFactory<>("isbn"));
        colBTitle.setCellValueFactory(new PropertyValueFactory<>("title"));
        colBAuthor.setCellValueFactory(new PropertyValueFactory<>("author"));
        colBAvail.setCellValueFactory(new PropertyValueFactory<>("availableCopies"));
        tblBooks.setItems(availableBooks);

        // Load initial data
        refreshDashboard();
    }

    @FXML
    private void onLogout() {
        Session.clear();
        LibraryApp.setScene("/com/lms/lmsfinal/LoginView.fxml", "Login / Register", 1200, 760);
    }

    // ===================== DASHBOARD REFRESH =====================

    private void refreshDashboard() {
        Task<Void> t = new Task<>() {
            private char[] finaloverdueCount;

            @Override
            protected Void call() {
                String email = Session.email;

                // Active borrows (returned = false)
                List<UserBorrow> active = firebase.getBorrowsForUser(email, false);

                // All borrows (for stats)
                List<UserBorrow> all = firebase.getBorrowsForUser(email, null);

                // Some available books (e.g. first 15)
                List<Book> books = firebase.getBooksLimit(15);

                // Compute stats
                int activeCount = active.size();
                int totalCount = all.size();
                int overdueCount = 0;

                LocalDate today = LocalDate.now();

                for (UserBorrow ub : active) {
                    try {
                        String dueStr = ub.getDueDate();
                        if (dueStr != null && !dueStr.isBlank()) {
                            // Parse "Tue Nov 19 00:00:00 EST 2025" → LocalDate
                            ZonedDateTime zdt = ZonedDateTime.parse(dueStr, DATE_STR_FORMATTER);
                            LocalDate due = zdt.toLocalDate();
                            if (due.isBefore(today)) {
                                overdueCount++;
                            }
                        }
                    } catch (Exception ignore) {
                        // If parsing fails, just skip that record
                    }
                }

                Platform.runLater(() -> {
                    activeBorrows.setAll(active);
                    availableBooks.setAll(books);

                    lblActiveLoans.setText(String.valueOf(activeCount));
                    lblTotalLoans.setText(String.valueOf(totalCount));
               
                    lblOverdue.setText(String.valueOf(finaloverdueCount));
                    statusActive.setText("Loaded " + activeCount + " active loans");
                    statusBooks.setText("Showing " + books.size() + " books");
                });

                return null;
            }
        };
        new Thread(t).start();
    }

    // ===================== BOOK SEARCH (USER SIDE) =====================

    @FXML
    private void onSearchBooksUser() {
        String q = bookSearchField.getText() == null ? "" : bookSearchField.getText().trim();
        Task<Void> t = new Task<>() {
            @Override
            protected Void call() {
                List<Book> list = q.isEmpty()
                        ? firebase.getBooksLimit(15)
                        : firebase.searchBooks(q);
                Platform.runLater(() -> {
                    availableBooks.setAll(list);
                    statusBooks.setText(q.isEmpty()
                            ? "Showing top " + list.size() + " books"
                            : "Search results for: \"" + q + "\"");
                });
                return null;
            }
        };
        new Thread(t).start();
    }

    @FXML
    private void onResetBooksUser() {
        bookSearchField.clear();
        onSearchBooksUser();
    }

    // ===================== BORROW FROM USER SIDE =====================

    @FXML
    private void onBorrowSelected() {
        Book sel = tblBooks.getSelectionModel().getSelectedItem();
        if (sel == null) {
            statusBooks.setText("Select a book to borrow.");
            return;
        }
        if (sel.getAvailableCopies() <= 0) {
            statusBooks.setText("No copies available for this book.");
            return;
        }

        try {
            // Simple borrow: use Session.email as user, default 14 days
            firebase.borrowBook(sel.getIsbn(), Session.email, 14);
            statusBooks.setText("Borrowed: " + sel.getTitle());
            // Refresh dashboard so new loan appears
            refreshDashboard();
        } catch (Exception e) {
            statusBooks.setText("Error: " + e.getMessage());
        }
    }
}

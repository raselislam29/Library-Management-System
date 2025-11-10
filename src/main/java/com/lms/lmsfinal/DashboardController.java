package com.lms.lmsfinal;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;

public class DashboardController {

    @FXML private Label welcomeUserLabel;
    @FXML private Label userEmailLabel;
    @FXML private Label userRoleLabel;
    @FXML private HBox adminControlsBox;

    // Recent borrows
    @FXML private TableView<BorrowRecord> borrowedBooksTable;
    @FXML private TableColumn<BorrowRecord, String> borrowedTitleColumn;
    @FXML private TableColumn<BorrowRecord, String> borrowedAuthorColumn;
    @FXML private TableColumn<BorrowRecord, String> borrowedDueDateColumn;

    // Catalog preview
    @FXML private TableView<Book> catalogPreviewTable;
    @FXML private TableColumn<Book, String> catalogTitleColumn;
    @FXML private TableColumn<Book, String> catalogAuthorColumn;
    @FXML private TableColumn<Book, String> catalogIsbnColumn;
    @FXML private TableColumn<Book, Integer> catalogAvailableColumn;

    // Borrow/Return controls
    @FXML private TextField loanDaysField;
    @FXML private Button borrowBtn;
    @FXML private Button returnBtn;
    @FXML private Label statusLabel;

    private final FirebaseService firebase = new FirebaseService();
    private final ObservableList<BorrowRecord> borrows = FXCollections.observableArrayList();
    private final ObservableList<Book> catalog = FXCollections.observableArrayList();

    @FXML
    private void initialize() {
        // Header
        String email = Session.email != null ? Session.email : "user@example.com";
        String role  = Session.role  != null ? Session.role  : "MEMBER";
        welcomeUserLabel.setText("Welcome,");
        userEmailLabel.setText(email);
        userRoleLabel.setText(role);

        boolean isStaff = role.equalsIgnoreCase("LIBRARIAN") || role.equalsIgnoreCase("ADMIN");
        if (adminControlsBox != null) {
            adminControlsBox.setManaged(isStaff);
            adminControlsBox.setVisible(isStaff);
        }

        // Table columns
        borrowedTitleColumn.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("title"));
        borrowedAuthorColumn.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("author"));
        borrowedDueDateColumn.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("dueDate"));
        borrowedBooksTable.setItems(borrows);

        catalogTitleColumn.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("title"));
        catalogAuthorColumn.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("author"));
        catalogIsbnColumn.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("isbn"));
        catalogAvailableColumn.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("availableCopies"));
        catalogPreviewTable.setItems(catalog);

        // Defaults
        if (loanDaysField != null) loanDaysField.setText("14");

        // Load data
        loadRecentBorrows(email, isStaff);
        loadCatalogPreview();
    }

    private void loadRecentBorrows(String email, boolean isStaff) {
        Task<Void> task = new Task<>() {
            @Override protected Void call() {
                java.util.List<BorrowRecord> list =
                        (email != null && !email.isBlank())
                                ? firebase.getRecentBorrowsForUser(email, 10)
                                : firebase.getRecentBorrowsAll(10);

                if ((list == null || list.isEmpty()) && isStaff) {
                    list = firebase.getRecentBorrowsAll(10);
                }

                final java.util.List<BorrowRecord> data = list;
                Platform.runLater(() -> {
                    borrows.setAll(data);
                });
                return null;
            }
        };
        new Thread(task).start();
    }

    private void loadCatalogPreview() {
        Task<Void> task = new Task<>() {
            @Override protected Void call() {
                var books = firebase.getBooksLimit(10);
                Platform.runLater(() -> catalog.setAll(books));
                return null;
            }
        };
        new Thread(task).start();
    }

    @FXML private void onBorrowSelected() {
        Book book = catalogPreviewTable.getSelectionModel().getSelectedItem();
        if (book == null) { setStatus("Select a book to borrow."); return; }
        String email = Session.email;
        if (email == null || email.isBlank()) { setStatus("You must be logged in."); return; }
        int days = parseInt(loanDaysField != null ? loanDaysField.getText() : "14", 14);

        setBusy(true);
        Task<Void> task = new Task<>() {
            @Override protected Void call() {
                try {
                    firebase.borrowBook(book.getIsbn(), email, days);
                    Platform.runLater(() -> setStatus("Borrowed: " + book.getTitle()));
                    // Refresh both lists
                    Platform.runLater(() -> loadCatalogPreview());
                    Platform.runLater(() -> loadRecentBorrows(email,
                            userRoleLabel.getText().equalsIgnoreCase("ADMIN")
                                    || userRoleLabel.getText().equalsIgnoreCase("LIBRARIAN")));
                } catch (Exception e) {
                    e.printStackTrace();
                    Platform.runLater(() -> setStatus("Error: " + e.getMessage()));
                } finally {
                    Platform.runLater(() -> setBusy(false));
                }
                return null;
            }
        };
        new Thread(task).start();
    }

    @FXML private void onReturnSelected() {
        BorrowRecord record = borrowedBooksTable.getSelectionModel().getSelectedItem();
        if (record == null) { setStatus("Select a borrowed record to return."); return; }
        String email = Session.email;
        if (email == null || email.isBlank()) { setStatus("You must be logged in."); return; }

        setBusy(true);
        Task<Void> task = new Task<>() {
            @Override protected Void call() {
                try {
                    firebase.returnBook(record.getIsbn(), email);
                    Platform.runLater(() -> setStatus("Returned: " + record.getTitle()));
                    Platform.runLater(() -> loadCatalogPreview());
                    Platform.runLater(() -> loadRecentBorrows(email,
                            userRoleLabel.getText().equalsIgnoreCase("ADMIN")
                                    || userRoleLabel.getText().equalsIgnoreCase("LIBRARIAN")));
                } catch (Exception e) {
                    e.printStackTrace();
                    Platform.runLater(() -> setStatus("Error: " + e.getMessage()));
                } finally {
                    Platform.runLater(() -> setBusy(false));
                }
                return null;
            }
        };
        new Thread(task).start();
    }

    @FXML private void onSignOutClick() {
        Session.clear();
        LibraryApp.setScene("/com/lms/lmsfinal/LoginView.fxml", "Login");
    }
    @FXML private void onManageUsersClick() { LibraryApp.setScene("/com/lms/lmsfinal/AdminUsersView.fxml", "Manage Users"); }
    @FXML private void onManageBooksClick() { LibraryApp.setScene("/com/lms/lmsfinal/AdminBooksView.fxml", "Manage Books"); }

    private void setBusy(boolean b) {
        if (borrowBtn != null) borrowBtn.setDisable(b);
        if (returnBtn != null) returnBtn.setDisable(b);
        if (loanDaysField != null) loanDaysField.setDisable(b);
    }

    private void setStatus(String s) { if (statusLabel != null) statusLabel.setText(s); }

    private int parseInt(String s, int def) {
        try { return Integer.parseInt(s.trim()); } catch (Exception e) { return def; }
    }
}

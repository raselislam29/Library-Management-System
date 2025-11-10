package com.lms.lmsfinal;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;

public class AdminBooksController {

    @FXML private TableView<Book> booksTable;
    @FXML private TableColumn<Book, String> colTitle;
    @FXML private TableColumn<Book, String> colAuthor;
    @FXML private TableColumn<Book, String> colIsbn;
    @FXML private TableColumn<Book, Integer> colAvail;

    @FXML private TextField tfIsbn, tfTitle, tfAuthor, tfPublisher, tfTotal, tfAvailable;
    @FXML private Label statusLabel;

    private final FirebaseService firebase = new FirebaseService();
    private final ObservableList<Book> books = FXCollections.observableArrayList();

    @FXML
    private void initialize() {
        colTitle.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("title"));
        colAuthor.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("author"));
        colIsbn.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("isbn"));
        colAvail.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("availableCopies"));
        booksTable.setItems(books);
        refresh();
        booksTable.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> {
            if (n != null) {
                tfIsbn.setText(n.getIsbn());
                tfTitle.setText(n.getTitle());
                tfAuthor.setText(n.getAuthor());
                tfPublisher.setText(n.getPublisher());
                tfTotal.setText(String.valueOf(n.getTotalCopies()));
                tfAvailable.setText(String.valueOf(n.getAvailableCopies()));
            }
        });
    }

    @FXML private void onAdd() {
        try {
            Book b = formToBook();
            firebase.addBook(b);
            status("Added " + b.getTitle());
            refresh();
            clearForm();
        } catch (Exception e) {
            error(e.getMessage());
        }
    }

    @FXML private void onUpdate() {
        try {
            Book b = formToBook();
            firebase.updateBook(b);
            status("Updated " + b.getTitle());
            refresh();
        } catch (Exception e) {
            error(e.getMessage());
        }
    }

    @FXML private void onDelete() {
        try {
            String isbn = tfIsbn.getText().trim();
            firebase.deleteBookByIsbn(isbn);
            status("Deleted ISBN " + isbn);
            refresh();
            clearForm();
        } catch (Exception e) {
            error(e.getMessage());
        }
    }

    @FXML private void onRefresh() { refresh(); }

    @FXML private void onBack() { LibraryApp.setScene("/com/lms/lmsfinal/DashboardView.fxml", "Dashboard"); }

    private void refresh() {
        books.setAll(firebase.getBooks());
    }

    private Book formToBook() {
        String isbn = tfIsbn.getText().trim();
        String title = tfTitle.getText().trim();
        String author = tfAuthor.getText().trim();
        String publisher = tfPublisher.getText().trim();
        int total = parseInt(tfTotal.getText());
        int avail = parseInt(tfAvailable.getText());
        return new Book(isbn, title, author, publisher, total, avail);
    }

    private int parseInt(String s) {
        try { return Integer.parseInt(s.trim()); } catch (Exception e) { return 0; }
    }

    private void clearForm() {
        tfIsbn.clear(); tfTitle.clear(); tfAuthor.clear(); tfPublisher.clear(); tfTotal.clear(); tfAvailable.clear();
    }

    private void status(String msg) { statusLabel.setText(msg); }
    private void error(String msg) { statusLabel.setText("Error: " + msg); }
}

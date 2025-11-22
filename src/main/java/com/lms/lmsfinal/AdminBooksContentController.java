package com.lms.lmsfinal;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

public class AdminBooksContentController {

    // Table + columns
    @FXML private TableView<Book> table;
    @FXML private TableColumn<Book, String> cIsbn;
    @FXML private TableColumn<Book, String> cTitle;
    @FXML private TableColumn<Book, String> cAuthor;
    @FXML private TableColumn<Book, String> cCatagory;   // note: FXML id is "cCatagory"
    @FXML private TableColumn<Book, String> cPublisher;
    @FXML private TableColumn<Book, Integer> cTotal;
    @FXML private TableColumn<Book, Integer> cAvail;

    // Editor fields
    @FXML private TextField fIsbn;
    @FXML private TextField fTitle;
    @FXML private TextField fAuthor;
    @FXML private TextField fCatagory;   // FXML spelling "Catagory"
    @FXML private TextField fPublisher;
    @FXML private TextField fTotal;
    @FXML private TextField fAvail;

    // Search + status
    @FXML private TextField searchField;
    @FXML private Label status;

    private final FirebaseService firebase = new FirebaseService();
    private final ObservableList<Book> books = FXCollections.observableArrayList();

    @FXML
    private void initialize() {
        // Bind columns to Book properties (names must match getters: getIsbn(), getTitle(), etc.)
        cIsbn.setCellValueFactory(new PropertyValueFactory<>("isbn"));
        cTitle.setCellValueFactory(new PropertyValueFactory<>("title"));
        cAuthor.setCellValueFactory(new PropertyValueFactory<>("author"));
        cCatagory.setCellValueFactory(new PropertyValueFactory<>("category")); // property name, not fx:id
        cPublisher.setCellValueFactory(new PropertyValueFactory<>("publisher"));
        cTotal.setCellValueFactory(new PropertyValueFactory<>("totalCopies"));
        cAvail.setCellValueFactory(new PropertyValueFactory<>("availableCopies"));

        table.setItems(books);
        refreshTable();

        // When user clicks a row, load it into the form
        table.getSelectionModel().selectedItemProperty().addListener((obs, old, b) -> {
            if (b != null) {
                fIsbn.setText(b.getIsbn());
                fTitle.setText(b.getTitle());
                fAuthor.setText(b.getAuthor());
                fCatagory.setText(b.getCategory());
                fPublisher.setText(b.getPublisher());
                fTotal.setText(String.valueOf(b.getTotalCopies()));
                fAvail.setText(String.valueOf(b.getAvailableCopies()));
            }
        });
    }

    // ===================== BUTTON HANDLERS =====================

    @FXML
    private void onAdd() {
        try {
            Book book = formToBook();
            firebase.addBook(book);
            setStatus("✅ Added: " + book.getTitle());
            refreshTable();
            clearForm();
        } catch (Exception e) {
            setError("Add failed: " + e.getMessage());
        }
    }

    @FXML
    private void onUpdate() {
        try {
            Book book = formToBook();
            firebase.updateBook(book);
            setStatus("✅ Updated: " + book.getTitle());
            refreshTable();
        } catch (Exception e) {
            setError("Update failed: " + e.getMessage());
        }
    }

    @FXML
    private void onDelete() {
        try {
            String isbn = fIsbn.getText().trim();
            if (isbn.isEmpty()) {
                setError("ISBN required to delete");
                return;
            }
            firebase.deleteBookByIsbn(isbn);
            setStatus("🗑 Deleted ISBN: " + isbn);
            refreshTable();
            clearForm();
        } catch (Exception e) {
            setError("Delete failed: " + e.getMessage());
        }
    }

    @FXML
    private void onSearch() {
        String q = searchField.getText();
        books.setAll(firebase.searchBooks(q));
        setStatus("🔍 Showing search results for: " + (q == null ? "" : q));
    }

    @FXML
    private void onReset() {
        searchField.clear();
        refreshTable();
        setStatus("🔄 Reset to all books");
    }

    // ===================== HELPERS =====================

    private void refreshTable() {
        books.setAll(firebase.getBooks());
    }

    private Book formToBook() {
        String isbn = fIsbn.getText().trim();
        String title = fTitle.getText().trim();
        String author = fAuthor.getText().trim();
        String category = fCatagory.getText().trim();
        String publisher = fPublisher.getText().trim();

        int total = parseInt(fTotal.getText());
        int avail = parseInt(fAvail.getText());

        // Use constructor that matches Book
        return new Book(isbn, title, author, category, publisher, total, avail);
    }

    private int parseInt(String text) {
        try {
            return Integer.parseInt(text.trim());
        } catch (Exception e) {
            return 0;
        }
    }

    private void clearForm() {
        fIsbn.clear();
        fTitle.clear();
        fAuthor.clear();
        fCatagory.clear();
        fPublisher.clear();
        fTotal.clear();
        fAvail.clear();
    }

    private void setStatus(String msg) {
        status.setText(msg);
    }

    private void setError(String msg) {
        status.setText("Error: " + msg);
    }
}

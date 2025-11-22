package com.lms.lmsfinal;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

public class AdminBooksContentController {

    @FXML private TableView<Book> table;
    @FXML private TableColumn<Book, String> cIsbn;
    @FXML private TableColumn<Book, String> cTitle;
    @FXML private TableColumn<Book, String> cAuthor;
    @FXML private TableColumn<Book, String> cCatagory;
    @FXML private TableColumn<Book, String> cPublisher;
    @FXML private TableColumn<Book, Integer> cTotal;
    @FXML private TableColumn<Book, Integer> cAvail;

    @FXML private TextField searchField;

    @FXML private TextField fIsbn;
    @FXML private TextField fTitle;
    @FXML private TextField fAuthor;
    @FXML private TextField fCatagory;
    @FXML private TextField fPublisher;
    @FXML private TextField fTotal;
    @FXML private TextField fAvail;

    @FXML private Label status;

    private final FirebaseService firebase = new FirebaseService();
    private final ObservableList<Book> data = FXCollections.observableArrayList();

    @FXML
    private void initialize() {

        // Table setup
        cIsbn.setCellValueFactory(new PropertyValueFactory<>("isbn"));
        cTitle.setCellValueFactory(new PropertyValueFactory<>("title"));
        cAuthor.setCellValueFactory(new PropertyValueFactory<>("author"));
        cCatagory.setCellValueFactory(new PropertyValueFactory<>("category"));
        cPublisher.setCellValueFactory(new PropertyValueFactory<>("publisher"));
        cTotal.setCellValueFactory(new PropertyValueFactory<>("totalCopies"));
        cAvail.setCellValueFactory(new PropertyValueFactory<>("availableCopies"));

        table.setItems(data);
        refresh();

        // Populate form when selecting a row
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

    // --------------------
    // SEARCH
    // --------------------

    @FXML
    private void onSearch() {
        String q = searchField.getText().trim().toLowerCase();
        if (q.isEmpty()) {
            refresh();
            return;
        }
        data.setAll(firebase.searchBooks(q));
    }

    @FXML
    private void onReset() {
        searchField.clear();
        refresh();
    }

    // --------------------
    // CRUD
    // --------------------

    @FXML
    private void onAdd() {
        try {
            Book b = readForm();
            firebase.addBook(b);
            status.setText("Added: " + b.getTitle());
            refresh();
            clearForm();
        } catch (Exception e) {
            status.setText("Error: " + e.getMessage());
        }
    }

    @FXML
    private void onUpdate() {
        try {
            Book b = readForm();
            firebase.updateBook(b);
            status.setText("Updated: " + b.getTitle());
            refresh();
        } catch (Exception e) {
            status.setText("Error: " + e.getMessage());
        }
    }

    @FXML
    private void onDelete() {
        try {
            String isbn = fIsbn.getText().trim();
            if (isbn.isEmpty()) {
                status.setText("ISBN required!");
                return;
            }
            firebase.deleteBookByIsbn(isbn);
            status.setText("Deleted ISBN: " + isbn);
            refresh();
            clearForm();
        } catch (Exception e) {
            status.setText("Error: " + e.getMessage());
        }
    }

    // --------------------
    // UTILS
    // --------------------

    @FXML
    private void onRefresh() {
        refresh();
    }

    private void refresh() {
        data.setAll(firebase.getBooks());
    }

    private Book readForm() {
        String isbn = safe(fIsbn);
        String title = safe(fTitle);
        String author = safe(fAuthor);
        String cat = safe(fCatagory);
        String pub = safe(fPublisher);

        int total = parse(fTotal, 1);
        int avail = parse(fAvail, total);

        return new Book(isbn, title, author, cat, pub, total, avail);
    }

    private String safe(TextField tf) {
        return tf.getText() == null ? "" : tf.getText().trim();
    }

    private int parse(TextField tf, int def) {
        try {
            return Integer.parseInt(tf.getText().trim());
        } catch (Exception e) {
            return def;
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
}

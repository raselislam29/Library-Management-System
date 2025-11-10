package com.lms.lmsfinal;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.util.List;

public class AdminBooksContentController {

    @FXML private TableView<Book> table;
    @FXML private TableColumn<Book, String> cIsbn, cTitle, cAuthor, cPublisher;
    @FXML private TableColumn<Book, Integer> cTotal, cAvail;

    @FXML private TextField searchField;
    @FXML private TextField fIsbn, fTitle, fAuthor, fPublisher, fTotal, fAvail;
    @FXML private Label status;

    @FXML private Button /* optional if present in your FXML */ addButton;
    @FXML private Button /* optional */ updateButton;
    @FXML private Button /* optional */ deleteButton;
    @FXML private Button /* optional */ searchButton;
    @FXML private Button /* optional */ resetButton;

    private final FirebaseService firebase = new FirebaseService();
    private final ObservableList<Book> rows = FXCollections.observableArrayList();

    @FXML
    private void initialize() {
        cIsbn.setCellValueFactory(new PropertyValueFactory<>("isbn"));
        cTitle.setCellValueFactory(new PropertyValueFactory<>("title"));
        cAuthor.setCellValueFactory(new PropertyValueFactory<>("author"));
        cPublisher.setCellValueFactory(new PropertyValueFactory<>("publisher"));
        cTotal.setCellValueFactory(new PropertyValueFactory<>("totalCopies"));
        cAvail.setCellValueFactory(new PropertyValueFactory<>("availableCopies"));
        table.setItems(rows);

        table.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> {
            if (n != null) {
                fIsbn.setText(n.getIsbn());
                fTitle.setText(n.getTitle());
                fAuthor.setText(n.getAuthor());
                fPublisher.setText(n.getPublisher());
                fTotal.setText(String.valueOf(n.getTotalCopies()));
                fAvail.setText(String.valueOf(n.getAvailableCopies()));
            }
        });

        // Initial load
        refresh();
    }

    // ----------------- Actions -----------------

    @FXML private void onSearch() {
        final String q = val(searchField);
        setBusy(true);
        runAsync(
                () -> q.isEmpty() ? firebase.getBooks() : firebase.searchBooks(q),
                books -> {
                    rows.setAll(books);
                    status("Loaded " + books.size() + " result(s).");
                }
        );
    }

    @FXML private void onReset() {
        searchField.clear();
        refresh();
    }

    @FXML private void onAdd() {
        Book b;
        try { b = readFormValidated(true); } catch (IllegalArgumentException ex) { status("Error: " + ex.getMessage()); return; }

        setBusy(true);
        runAsync(
                () -> {
                    firebase.addBook(b);                // block until write completes
                    return firebase.getBooks();         // read fresh list immediately
                },
                books -> {
                    rows.setAll(books);
                    status("Added: " + b.getTitle());
                    selectByIsbn(b.getIsbn());
                }
        );
    }

    @FXML private void onUpdate() {
        Book b;
        try { b = readFormValidated(false); } catch (IllegalArgumentException ex) { status("Error: " + ex.getMessage()); return; }

        setBusy(true);
        runAsync(
                () -> {
                    firebase.updateBook(b);
                    return firebase.getBooks();
                },
                books -> {
                    rows.setAll(books);
                    status("Updated: " + b.getTitle());
                    selectByIsbn(b.getIsbn());
                }
        );
    }

    @FXML private void onDelete() {
        Book sel = table.getSelectionModel().getSelectedItem();
        if (sel == null) { status("Select a book first."); return; }

        setBusy(true);
        runAsync(
                () -> {
                    firebase.deleteBookByIsbn(sel.getIsbn());
                    return firebase.getBooks();
                },
                books -> {
                    rows.setAll(books);
                    status("Deleted: " + sel.getTitle());
                }
        );
    }

    // ----------------- Helpers -----------------

    private void refresh() {
        setBusy(true);
        runAsync(
                firebase::getBooks,
                books -> {
                    rows.setAll(books);
                    status("Loaded " + books.size() + " book(s).");
                }
        );
    }

    /** Validate inputs and build Book. If adding, ISBN must be non-empty; always check totals. */
    private Book readFormValidated(boolean adding) {
        String isbn = val(fIsbn);
        String title = val(fTitle);
        String author = val(fAuthor);
        String publisher = val(fPublisher);
        int total = parseInt(val(fTotal), 0);
        int avail = parseInt(val(fAvail), 0);

        if (adding && isbn.isBlank()) throw new IllegalArgumentException("ISBN is required.");
        if (title.isBlank()) throw new IllegalArgumentException("Title is required.");
        if (total < 0) throw new IllegalArgumentException("Total copies cannot be negative.");
        if (avail < 0) throw new IllegalArgumentException("Available copies cannot be negative.");
        if (avail > total) throw new IllegalArgumentException("Available copies cannot exceed total copies.");

        return new Book(isbn, title, author, publisher, total, avail);
    }

    private String val(TextField t) { return (t == null || t.getText() == null) ? "" : t.getText().trim(); }
    private int parseInt(String s, int def) { try { return Integer.parseInt(s.trim()); } catch (Exception e) { return def; } }

    private void status(String s) { if (status != null) status.setText(s); }

    /** Run a background supplier and deliver result to UI; also clears busy state. */
    private <T> void runAsync(java.util.concurrent.Callable<T> work, java.util.function.Consumer<T> onUi) {
        Task<T> task = new Task<>() {
            @Override protected T call() throws Exception { return work.call(); }
        };
        task.setOnSucceeded(e -> {
            try { onUi.accept(task.getValue()); }
            finally { setBusy(false); }
        });
        task.setOnFailed(e -> {
            Throwable ex = task.getException();
            ex.printStackTrace();
            status("Error: " + (ex == null ? "Unknown" : ex.getMessage()));
            setBusy(false);
        });
        new Thread(task, "books-io").start();
    }

    /** Disable/enable controls while IO is running. */
    private void setBusy(boolean busy) {
        if (table != null) table.setDisable(busy);
        if (searchField != null) searchField.setDisable(busy);
        if (fIsbn != null) fIsbn.setDisable(busy);
        if (fTitle != null) fTitle.setDisable(busy);
        if (fAuthor != null) fAuthor.setDisable(busy);
        if (fPublisher != null) fPublisher.setDisable(busy);
        if (fTotal != null) fTotal.setDisable(busy);
        if (fAvail != null) fAvail.setDisable(busy);

        // If you wired these @FXML buttons, they’ll be toggled too. If not, no problem.
        if (addButton != null) addButton.setDisable(busy);
        if (updateButton != null) updateButton.setDisable(busy);
        if (deleteButton != null) deleteButton.setDisable(busy);
        if (searchButton != null) searchButton.setDisable(busy);
        if (resetButton != null) resetButton.setDisable(busy);
    }

    /** After reload, reselect the updated/added row by ISBN. */
    private void selectByIsbn(String isbn) {
        if (isbn == null || isbn.isBlank()) return;
        for (Book b : rows) {
            if (isbn.equals(b.getIsbn())) {
                table.getSelectionModel().select(b);
                table.scrollTo(b);
                break;
            }
        }
    }
}

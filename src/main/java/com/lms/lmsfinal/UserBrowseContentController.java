package com.lms.lmsfinal;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.util.List;

public class UserBrowseContentController {

    @FXML private TableView<Book> table;
    @FXML private TableColumn<Book, String> cTitle, cAuthor, cIsbn;
    @FXML private TableColumn<Book, Integer> cAvail;
    @FXML private TextField searchField, nameField, studentIdField, phoneField, studentEmailField, daysField;
    @FXML private Label status;

    private final FirebaseService firebase = new FirebaseService();
    private final ObservableList<Book> rows = FXCollections.observableArrayList();

    @FXML
    private void initialize() {
        cTitle.setCellValueFactory(new PropertyValueFactory<>("title"));
        cAuthor.setCellValueFactory(new PropertyValueFactory<>("author"));
        cIsbn.setCellValueFactory(new PropertyValueFactory<>("isbn"));
        cAvail.setCellValueFactory(new PropertyValueFactory<>("availableCopies"));
        table.setItems(rows);
        refresh();
    }

    @FXML private void onSearch() {
        String q = searchField.getText() == null ? "" : searchField.getText().trim();
        runAsync(() -> q.isEmpty() ? firebase.getBooks() : firebase.searchBooks(q),
                books -> rows.setAll(books));
    }

    @FXML private void onReset() { searchField.clear(); refresh(); }

    @FXML private void onBorrow() {
        Book sel = table.getSelectionModel().getSelectedItem();
        if (sel == null) { status("Select a book first."); return; }

        String email = Session.email;
        if (email == null || email.isBlank()) { status("You must be logged in."); return; }

        int days = parseInt(get(daysField), 14);

        String nm = get(nameField);
        String sid = get(studentIdField);
        String ph = get(phoneField);
        String smail = get(studentEmailField);

        if (nm.isBlank() || sid.isBlank() || smail.isBlank()) {
            status("Name, Student ID and Student Email are required."); return;
        }

        runAsync(() -> {
            firebase.borrowBookWithStudentInfo(sel.getIsbn(), email, days, nm, sid, ph, smail);
            return firebase.getBooks();
        }, books -> {
            rows.setAll(books);
            status("Borrowed: " + sel.getTitle());
        });
    }

    private void refresh() {
        runAsync(firebase::getBooks, books -> rows.setAll(books));
    }

    // helpers
    private void runAsync(java.util.concurrent.Callable<List<Book>> work,
                          java.util.function.Consumer<List<Book>> onUi) {
        Task<List<Book>> t = new Task<>() { @Override protected List<Book> call() throws Exception { return work.call(); } };
        t.setOnSucceeded(e -> { onUi.accept(t.getValue()); });
        t.setOnFailed(e -> { Throwable ex=t.getException(); ex.printStackTrace(); status("Error: "+(ex==null?"":ex.getMessage())); });
        new Thread(t).start();
    }
    private String get(TextField f) { return f.getText()==null?"":f.getText().trim(); }
    private int parseInt(String s, int def) { try { return Integer.parseInt(s); } catch (Exception e) { return def; } }
    private void status(String s) { if (status!=null) status.setText(s); }
}
